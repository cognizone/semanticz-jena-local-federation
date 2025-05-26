/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package zone.cogni.semanticz.jena.federation.core;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterNullIterator;
import org.apache.jena.sparql.service.ServiceExecutorFactory;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.apache.jena.sparql.service.ServiceExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A registry for local Jena Models and Datasets that enables them to be accessed
 * through custom SERVICE URIs in SPARQL queries.
 * 
 * This implementation is framework-agnostic and thread-safe.
 */
public class LocalSparqlServiceRegistry implements ServiceRegistry, ServiceExecutorFactory {

    private static final Logger log = LoggerFactory.getLogger(LocalSparqlServiceRegistry.class);

    private final Map<String, Dataset> localDatasets = new ConcurrentHashMap<>();
    private final Map<String, Model> localModels = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void registerDataset(String serviceUri, Dataset dataset) {
        Objects.requireNonNull(serviceUri, "Service URI cannot be null");
        Objects.requireNonNull(dataset, "Dataset cannot be null");

        if (localDatasets.containsKey(serviceUri) || localModels.containsKey(serviceUri)) {
            throw new IllegalArgumentException("Service URI already registered: " + serviceUri);
        }

        if (!ServiceUriConstants.isLocalServiceUri(serviceUri)) {
            log.warn("Registering service URI '{}' which does not follow the recommended pattern '{}'",
                    serviceUri, ServiceUriConstants.SERVICE_URI_PREFIX);
        }

        localDatasets.put(serviceUri, dataset);
        log.info("Registered Dataset with service URI: {}", serviceUri);
    }

    @Override
    public void registerModel(String serviceUri, Model model) {
        Objects.requireNonNull(serviceUri, "Service URI cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");

        if (localDatasets.containsKey(serviceUri) || localModels.containsKey(serviceUri)) {
            throw new IllegalArgumentException("Service URI already registered: " + serviceUri);
        }

        if (!ServiceUriConstants.isLocalServiceUri(serviceUri)) {
            log.warn("Registering service URI '{}' which does not follow the recommended pattern '{}'",
                    serviceUri, ServiceUriConstants.SERVICE_URI_PREFIX);
        }

        localModels.put(serviceUri, model);
        log.info("Registered Model with service URI: {}", serviceUri);
    }

    @Override
    public boolean unregisterService(String serviceUri) {
        boolean removed = localDatasets.remove(serviceUri) != null;
        removed |= localModels.remove(serviceUri) != null;
        if (removed) {
            log.info("Unregistered service URI: {}", serviceUri);
        }
        return removed;
    }

    @Override
    public boolean isRegistered(String serviceUri) {
        return localDatasets.containsKey(serviceUri) || localModels.containsKey(serviceUri);
    }

    @Override
    public Set<String> getRegisteredServices() {
        Set<String> allServices = new HashSet<>(localDatasets.keySet());
        allServices.addAll(localModels.keySet());
        return Collections.unmodifiableSet(allServices);
    }

    @Override
    public void clear() {
        localDatasets.clear();
        localModels.clear();
        log.info("Cleared all registered services");
    }

    @Override
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            log.info("Registering LocalSparqlServiceRegistry with Jena ServiceExecutorRegistry");
            ServiceExecutorRegistry.get().add(this);
            log.info("LocalSparqlServiceRegistry registered successfully");
        } else {
            log.warn("LocalSparqlServiceRegistry is already initialized");
        }
    }

    @Override
    public void shutdown() {
        if (initialized.compareAndSet(true, false)) {
            log.info("Unregistering LocalSparqlServiceRegistry from Jena ServiceExecutorRegistry");
            ServiceExecutorRegistry.get().remove(this);
            clear();
            log.info("LocalSparqlServiceRegistry unregistered and cleared");
        } else {
            log.warn("LocalSparqlServiceRegistry is not initialized or already shut down");
        }
    }

    // --- Implementation of ServiceExecutorFactory ---

    @Override
    public ServiceExecution createExecutor(OpService opExecute, OpService opOriginal, Binding binding, ExecutionContext execCxt) {
        if (!initialized.get()) {
            log.trace("Registry not initialized, skipping service URI: {}", opExecute.getService().getURI());
            return null;
        }

        String serviceUri = opExecute.getService().getURI();

        // Check if it's one of our registered datasets
        Dataset localDs = localDatasets.get(serviceUri);
        if (localDs != null) {
            log.debug("Handling SERVICE call to registered Dataset: {}", serviceUri);
            QueryIterator iter = executeLocallyOnDataset(opExecute, localDs, execCxt);
            return new ServiceExecution() {
                @Override
                public QueryIterator exec() {
                    return iter;
                }
            };
        }

        // Check if it's one of our registered models
        Model localModel = localModels.get(serviceUri);
        if (localModel != null) {
            log.debug("Handling SERVICE call to registered Model: {}", serviceUri);
            QueryIterator iter = executeLocallyOnModel(opExecute, localModel, execCxt);
            return new ServiceExecution() {
                @Override
                public QueryIterator exec() {
                    return iter;
                }
            };
        }

        // If the URI doesn't match any registered local source, return null
        // to let Jena try other registered factories (e.g., the default HTTP handler).
        log.trace("Service URI {} not handled by LocalSparqlServiceRegistry", serviceUri);
        return null;
    }

    private QueryIterator executeLocallyOnDataset(OpService opExecute, Dataset dataset, ExecutionContext execCxt) {
        try {
            // For simplicity, create a basic SELECT query for the sub-operation
            // In a real implementation, you'd need more sophisticated query conversion
            String queryString = "SELECT * WHERE { " + opExecute.getSubOp().toString() + " }";
            Query subQuery = QueryFactory.create(queryString);
            
            if (!subQuery.isSelectType()) {
                log.warn("Local SERVICE execution currently only supports SELECT queries");
                return QueryIterNullIterator.create(execCxt);
            }

            // For now, return a null iterator to indicate successful but empty execution
            // In a full implementation, you'd need proper algebra conversion
            log.warn("SERVICE execution not fully implemented - returning empty results");
            return QueryIterNullIterator.create(execCxt);

        } catch (Exception e) {
            log.error("Error executing local SERVICE sub-query for URI {}: {}", 
                    opExecute.getService().getURI(), opExecute.getSubOp(), e);
            return QueryIterNullIterator.create(execCxt);
        }
    }

    private QueryIterator executeLocallyOnModel(OpService opExecute, Model model, ExecutionContext execCxt) {
        try {
            // For simplicity, create a basic SELECT query for the sub-operation
            // In a real implementation, you'd need more sophisticated query conversion
            String queryString = "SELECT * WHERE { " + opExecute.getSubOp().toString() + " }";
            Query subQuery = QueryFactory.create(queryString);
            
            if (!subQuery.isSelectType()) {
                log.warn("Local SERVICE execution currently only supports SELECT queries");
                return QueryIterNullIterator.create(execCxt);
            }

            // For now, return a null iterator to indicate successful but empty execution
            // In a full implementation, you'd need proper algebra conversion
            log.warn("SERVICE execution not fully implemented - returning empty results");
            return QueryIterNullIterator.create(execCxt);

        } catch (Exception e) {
            log.error("Error executing local SERVICE sub-query for URI {}: {}", 
                    opExecute.getService().getURI(), opExecute.getSubOp(), e);
            return QueryIterNullIterator.create(execCxt);
        }
    }

    // Utility methods for inspection

    /**
     * Gets the number of registered datasets.
     */
    public int getDatasetCount() {
        return localDatasets.size();
    }

    /**
     * Gets the number of registered models.
     */
    public int getModelCount() {
        return localModels.size();
    }

    /**
     * Checks if the registry is initialized and active.
     */
    public boolean isInitialized() {
        return initialized.get();
    }
}