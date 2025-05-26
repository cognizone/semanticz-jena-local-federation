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

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import java.util.Set;

/**
 * Core interface for registering and managing local Jena data sources
 * that can be accessed via custom SERVICE URIs in SPARQL queries.
 * 
 * This interface is framework-agnostic and can be used in any Java application.
 */
public interface ServiceRegistry {

    /**
     * Registers a Jena Dataset to be accessible via a custom SERVICE URI.
     *
     * @param serviceUri The custom URI (e.g., "urn:jena:service:my-tdb-data"). Must be unique.
     * @param dataset    The Jena Dataset instance. Must not be null.
     * @throws IllegalArgumentException if serviceUri or dataset is null, or if URI is already registered.
     */
    void registerDataset(String serviceUri, Dataset dataset);

    /**
     * Registers a Jena Model to be accessible via a custom SERVICE URI.
     *
     * @param serviceUri The custom URI (e.g., "urn:jena:service:my-vocab"). Must be unique.
     * @param model      The Jena Model instance. Must not be null.
     * @throws IllegalArgumentException if serviceUri or model is null, or if URI is already registered.
     */
    void registerModel(String serviceUri, Model model);

    /**
     * Removes a previously registered service URI.
     *
     * @param serviceUri The URI to remove.
     * @return true if a mapping was removed, false otherwise.
     */
    boolean unregisterService(String serviceUri);

    /**
     * Checks if a service URI is registered.
     *
     * @param serviceUri The URI to check.
     * @return true if the URI is registered, false otherwise.
     */
    boolean isRegistered(String serviceUri);

    /**
     * Gets all registered service URIs.
     *
     * @return An immutable set of all registered service URIs.
     */
    Set<String> getRegisteredServices();

    /**
     * Clears all registered services.
     */
    void clear();

    /**
     * Initializes the registry and activates it for handling SERVICE calls.
     * This typically involves registering with Jena's ServiceExecutorRegistry.
     */
    void initialize();

    /**
     * Shuts down the registry and deactivates it from handling SERVICE calls.
     * This typically involves unregistering from Jena's ServiceExecutorRegistry.
     */
    void shutdown();
}