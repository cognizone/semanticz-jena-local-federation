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
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSparqlServiceRegistryTest {

    private LocalSparqlServiceRegistry registry;
    private Model testModel;
    private Dataset testDataset;

    @BeforeEach
    void setUp() {
        registry = new LocalSparqlServiceRegistry();
        
        // Create test model with some data
        testModel = ModelFactory.createDefaultModel();
        Resource person = testModel.createResource("http://example.org/person1");
        person.addProperty(RDF.type, testModel.createResource("http://example.org/Person"));
        person.addProperty(RDFS.label, "Test Person");

        // Create test dataset with some data
        testDataset = DatasetFactory.createTxnMem();
        testDataset.executeWrite(() -> {
            Model defaultModel = testDataset.getDefaultModel();
            Resource company = defaultModel.createResource("http://example.org/company1");
            company.addProperty(RDF.type, defaultModel.createResource("http://example.org/Company"));
            company.addProperty(RDFS.label, "Test Company");
        });
    }

    @AfterEach
    void tearDown() {
        if (registry.isInitialized()) {
            registry.shutdown();
        }
        if (testDataset != null) {
            testDataset.close();
        }
    }

    @Test
    void testRegisterAndUnregisterModel() {
        String serviceUri = ServiceUriConstants.createServiceUri("test-model");
        
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getModelCount());
        
        registry.registerModel(serviceUri, testModel);
        
        assertTrue(registry.isRegistered(serviceUri));
        assertEquals(1, registry.getModelCount());
        assertTrue(registry.getRegisteredServices().contains(serviceUri));
        
        assertTrue(registry.unregisterService(serviceUri));
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getModelCount());
    }

    @Test
    void testRegisterAndUnregisterDataset() {
        String serviceUri = ServiceUriConstants.createServiceUri("test-dataset");
        
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getDatasetCount());
        
        registry.registerDataset(serviceUri, testDataset);
        
        assertTrue(registry.isRegistered(serviceUri));
        assertEquals(1, registry.getDatasetCount());
        assertTrue(registry.getRegisteredServices().contains(serviceUri));
        
        assertTrue(registry.unregisterService(serviceUri));
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getDatasetCount());
    }

    @Test
    void testDuplicateRegistrationThrowsException() {
        String serviceUri = ServiceUriConstants.createServiceUri("test-duplicate");
        
        registry.registerModel(serviceUri, testModel);
        
        assertThrows(IllegalArgumentException.class, () -> 
            registry.registerModel(serviceUri, testModel));
        
        assertThrows(IllegalArgumentException.class, () -> 
            registry.registerDataset(serviceUri, testDataset));
    }

    @Test
    void testNullArgumentsThrowException() {
        assertThrows(NullPointerException.class, () -> 
            registry.registerModel(null, testModel));
        
        assertThrows(NullPointerException.class, () -> 
            registry.registerModel("test", null));
        
        assertThrows(NullPointerException.class, () -> 
            registry.registerDataset(null, testDataset));
        
        assertThrows(NullPointerException.class, () -> 
            registry.registerDataset("test", null));
    }

    @Test
    void testInitializeAndShutdown() {
        assertFalse(registry.isInitialized());
        
        registry.initialize();
        assertTrue(registry.isInitialized());
        
        // Test that double initialization is safe
        registry.initialize();
        assertTrue(registry.isInitialized());
        
        registry.shutdown();
        assertFalse(registry.isInitialized());
        
        // Test that double shutdown is safe
        registry.shutdown();
        assertFalse(registry.isInitialized());
    }

    @Test
    void testClear() {
        String modelUri = ServiceUriConstants.createServiceUri("test-model");
        String datasetUri = ServiceUriConstants.createServiceUri("test-dataset");
        
        registry.registerModel(modelUri, testModel);
        registry.registerDataset(datasetUri, testDataset);
        
        assertEquals(1, registry.getModelCount());
        assertEquals(1, registry.getDatasetCount());
        assertEquals(2, registry.getRegisteredServices().size());
        
        registry.clear();
        
        assertEquals(0, registry.getModelCount());
        assertEquals(0, registry.getDatasetCount());
        assertEquals(0, registry.getRegisteredServices().size());
    }

    @Test
    void testFederatedQueryWithModel() {
        // Register the test model
        String serviceUri = ServiceUriConstants.createServiceUri("test-persons");
        registry.initialize();
        registry.registerModel(serviceUri, testModel);

        // Create a primary model (can be empty for this test)
        Model primaryModel = ModelFactory.createDefaultModel();

        // Query that uses the federated service
        String queryString = String.format("""
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            
            SELECT ?person ?label
            WHERE {
              SERVICE <%s> {
                ?person a <http://example.org/Person> ;
                        rdfs:label ?label .
              }
            }
            """, serviceUri);

        try (QueryExecution qExec = QueryExecutionFactory.create(queryString, primaryModel)) {
            ResultSet results = qExec.execSelect();
            ResultSetRewindable rewindable = ResultSetFactory.copyResults(results);

            // Optional: debug output
            ResultSetFormatter.out(System.out, rewindable);
            rewindable.reset();

            // Assertions
            assertTrue(rewindable.hasNext(), "Expected at least one result");

            QuerySolution solution = rewindable.next();
            assertEquals("http://example.org/person1", solution.getResource("person").getURI());
            assertEquals("Test Person", solution.getLiteral("label").getString());

            // Assert no more results
            assertFalse(rewindable.hasNext(), "Expected only one result");
        }
    }

    @Test
    void testFederatedQueryWithDataset() {
        // Register the test dataset
        String serviceUri = ServiceUriConstants.createServiceUri("test-companies");
        registry.initialize();
        registry.registerDataset(serviceUri, testDataset);

        // Create a primary model
        Model primaryModel = ModelFactory.createDefaultModel();

        // Query that uses the federated service
        String queryString = String.format("""
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        
        SELECT ?company ?label
        WHERE {
          SERVICE <%s> {
            ?company a <http://example.org/Company> ;
                     rdfs:label ?label .
          }
        }
        """, serviceUri);

        try (QueryExecution qExec = QueryExecutionFactory.create(queryString, primaryModel)) {
            ResultSet results = qExec.execSelect();
            ResultSetRewindable rewindable = ResultSetFactory.copyResults(results);

            // Optional: debug output
            ResultSetFormatter.out(System.out, rewindable);
            rewindable.reset();

            // Assertions
            assertTrue(rewindable.hasNext(), "Expected at least one result");

            QuerySolution solution = rewindable.next();
            assertEquals("http://example.org/company1", solution.getResource("company").getURI());
            assertEquals("Test Company", solution.getLiteral("label").getString());

            // Assert no more results
            assertFalse(rewindable.hasNext(), "Expected only one result");
        }
    }

    @Test
    void testUnregisteredServiceUri() {
        registry.initialize();
        
        Model primaryModel = ModelFactory.createDefaultModel();
        
        // Query with unregistered service URI should not cause errors
        String queryString = """
            SELECT ?s ?p ?o
            WHERE {
              SERVICE <http://example.org/nonexistent> {
                ?s ?p ?o .
              }
            }
            """;
        
        // For HTTP services, this will try to make an actual HTTP call and fail
        // For our local registry, unregistered URN services should be ignored
        assertThrows(org.apache.jena.sparql.engine.http.QueryExceptionHTTP.class, () -> {
            try (QueryExecution qExec = QueryExecutionFactory.create(queryString, primaryModel)) {
                ResultSet results = qExec.execSelect();
                results.hasNext(); // Force execution
            }
        });
    }
}