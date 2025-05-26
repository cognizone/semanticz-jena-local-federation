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
        // given
        String serviceUri = ServiceUriConstants.createServiceUri("test-model");
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getModelCount());
        
        // when - register model
        registry.registerModel(serviceUri, testModel);
        
        // then - verify registration
        assertTrue(registry.isRegistered(serviceUri));
        assertEquals(1, registry.getModelCount());
        assertTrue(registry.getRegisteredServices().contains(serviceUri));
        
        // when - unregister model
        boolean unregistered = registry.unregisterService(serviceUri);
        
        // then - verify unregistration
        assertTrue(unregistered);
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getModelCount());
    }

    @Test
    void testRegisterAndUnregisterDataset() {
        // given
        String serviceUri = ServiceUriConstants.createServiceUri("test-dataset");
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getDatasetCount());
        
        // when - register dataset
        registry.registerDataset(serviceUri, testDataset);
        
        // then - verify registration
        assertTrue(registry.isRegistered(serviceUri));
        assertEquals(1, registry.getDatasetCount());
        assertTrue(registry.getRegisteredServices().contains(serviceUri));
        
        // when - unregister dataset
        boolean unregistered = registry.unregisterService(serviceUri);
        
        // then - verify unregistration
        assertTrue(unregistered);
        assertFalse(registry.isRegistered(serviceUri));
        assertEquals(0, registry.getDatasetCount());
    }

    @Test
    void testDuplicateRegistrationThrowsException() {
        // given
        String serviceUri = ServiceUriConstants.createServiceUri("test-duplicate");
        registry.registerModel(serviceUri, testModel);
        
        // when/then - attempt duplicate model registration
        assertThrows(IllegalArgumentException.class, () -> 
            registry.registerModel(serviceUri, testModel));
        
        // when/then - attempt dataset registration on same URI
        assertThrows(IllegalArgumentException.class, () -> 
            registry.registerDataset(serviceUri, testDataset));
    }

    @Test
    void testNullArgumentsThrowException() {
        // given - registry is set up in @BeforeEach
        
        // when/then - test null service URI for model
        assertThrows(NullPointerException.class, () -> 
            registry.registerModel(null, testModel));
        
        // when/then - test null model
        assertThrows(NullPointerException.class, () -> 
            registry.registerModel("test", null));
        
        // when/then - test null service URI for dataset
        assertThrows(NullPointerException.class, () -> 
            registry.registerDataset(null, testDataset));
        
        // when/then - test null dataset
        assertThrows(NullPointerException.class, () -> 
            registry.registerDataset("test", null));
    }

    @Test
    void testInitializeAndShutdown() {
        // given
        assertFalse(registry.isInitialized());
        
        // when - initialize registry
        registry.initialize();
        
        // then - verify initialization
        assertTrue(registry.isInitialized());
        
        // when - initialize again (should be safe)
        registry.initialize();
        
        // then - still initialized
        assertTrue(registry.isInitialized());
        
        // when - shutdown registry
        registry.shutdown();
        
        // then - verify shutdown
        assertFalse(registry.isInitialized());
        
        // when - shutdown again (should be safe)
        registry.shutdown();
        
        // then - still shut down
        assertFalse(registry.isInitialized());
    }

    @Test
    void testClear() {
        // given
        String modelUri = ServiceUriConstants.createServiceUri("test-model");
        String datasetUri = ServiceUriConstants.createServiceUri("test-dataset");
        registry.registerModel(modelUri, testModel);
        registry.registerDataset(datasetUri, testDataset);
        
        assertEquals(1, registry.getModelCount());
        assertEquals(1, registry.getDatasetCount());
        assertEquals(2, registry.getRegisteredServices().size());
        
        // when
        registry.clear();
        
        // then
        assertEquals(0, registry.getModelCount());
        assertEquals(0, registry.getDatasetCount());
        assertEquals(0, registry.getRegisteredServices().size());
    }

    @Test
    void testFederatedQueryWithModel() {
        // given
        String serviceUri = ServiceUriConstants.createServiceUri("test-persons");
        registry.initialize();
        registry.registerModel(serviceUri, testModel);
        Model primaryModel = ModelFactory.createDefaultModel();

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

        // when
        try (QueryExecution qExec = QueryExecutionFactory.create(queryString, primaryModel)) {
            ResultSet results = qExec.execSelect();
            ResultSetRewindable rewindable = ResultSetFactory.copyResults(results);

            // Optional: debug output
            ResultSetFormatter.out(System.out, rewindable);
            rewindable.reset();

            // then
            assertTrue(rewindable.hasNext(), "Expected at least one result");

            QuerySolution solution = rewindable.next();
            assertEquals("http://example.org/person1", solution.getResource("person").getURI());
            assertEquals("Test Person", solution.getLiteral("label").getString());

            assertFalse(rewindable.hasNext(), "Expected only one result");
        }
    }

    @Test
    void testFederatedQueryWithDataset() {
        // given
        String serviceUri = ServiceUriConstants.createServiceUri("test-companies");
        registry.initialize();
        registry.registerDataset(serviceUri, testDataset);
        Model primaryModel = ModelFactory.createDefaultModel();

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

        // when
        try (QueryExecution qExec = QueryExecutionFactory.create(queryString, primaryModel)) {
            ResultSet results = qExec.execSelect();
            ResultSetRewindable rewindable = ResultSetFactory.copyResults(results);

            // Optional: debug output
            ResultSetFormatter.out(System.out, rewindable);
            rewindable.reset();

            // then
            assertTrue(rewindable.hasNext(), "Expected at least one result");

            QuerySolution solution = rewindable.next();
            assertEquals("http://example.org/company1", solution.getResource("company").getURI());
            assertEquals("Test Company", solution.getLiteral("label").getString());

            assertFalse(rewindable.hasNext(), "Expected only one result");
        }
    }

    @Test
    void testUnregisteredServiceUri() {
        // given
        registry.initialize();
        Model primaryModel = ModelFactory.createDefaultModel();
        
        String queryString = """
            SELECT ?s ?p ?o
            WHERE {
              SERVICE <http://example.org/nonexistent> {
                ?s ?p ?o .
              }
            }
            """;
        
        // when/then - HTTP services should fail with QueryExceptionHTTP
        assertThrows(org.apache.jena.sparql.engine.http.QueryExceptionHTTP.class, () -> {
            try (QueryExecution qExec = QueryExecutionFactory.create(queryString, primaryModel)) {
                ResultSet results = qExec.execSelect();
                results.hasNext(); // Force execution
            }
        });
    }
}