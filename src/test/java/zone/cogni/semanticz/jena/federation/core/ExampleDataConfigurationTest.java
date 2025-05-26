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
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class demonstrating how to register local data sources
 * with the ServiceRegistry for use in SPARQL federation.
 */
class ExampleDataConfigurationTest {

    private static final Logger log = LoggerFactory.getLogger(ExampleDataConfigurationTest.class);

    private ServiceRegistry serviceRegistry;

    @BeforeEach
    void setUp() {
        serviceRegistry = new LocalSparqlServiceRegistry();
    }

    @Test
    void testRegisterExampleData() {
        try {
            registerVocabularyModel();
            registerTdbDataset();
            registerPersonsModel();
            
            // Verify all services are registered
            assertEquals(3, serviceRegistry.getRegisteredServices().size());
            assertTrue(serviceRegistry.isRegistered(ServiceUriConstants.createServiceUri("vocabulary")));
            assertTrue(serviceRegistry.isRegistered(ServiceUriConstants.createServiceUri("example-tdb")));
            assertTrue(serviceRegistry.isRegistered(ServiceUriConstants.createServiceUri("persons")));
        } catch (Exception e) {
            fail("Failed to register example data: " + e.getMessage());
        }
    }

    private void registerVocabularyModel() {
        log.info("Creating and registering vocabulary model");
        
        Model vocabModel = ModelFactory.createDefaultModel();
        
        // Add some vocabulary data
        Resource conceptClass = vocabModel.createResource("http://example.org/vocab#Concept");
        conceptClass.addProperty(RDF.type, RDFS.Class);
        conceptClass.addProperty(RDFS.label, "Concept");
        conceptClass.addProperty(RDFS.comment, "A general concept in our vocabulary");

        Property hasCategory = vocabModel.createProperty("http://example.org/vocab#hasCategory");
        hasCategory.addProperty(RDF.type, RDF.Property);
        hasCategory.addProperty(RDFS.label, "has category");
        hasCategory.addProperty(RDFS.domain, conceptClass);

        String vocabularyServiceUri = ServiceUriConstants.createServiceUri("vocabulary");
        serviceRegistry.registerModel(vocabularyServiceUri, vocabModel);
        log.info("Registered vocabulary model at {}", vocabularyServiceUri);
    }

    private void registerTdbDataset() throws IOException {
        log.info("Creating and registering TDB2 dataset");
        
        // Create temporary directory for TDB
        Path tdbPath = Paths.get("target", "example-tdb");
        Files.createDirectories(tdbPath);
        
        Dataset tdbDataset = TDB2Factory.connectDataset(tdbPath.toString());
        
        // Add some sample data to TDB
        tdbDataset.executeWrite(() -> {
            Model defaultModel = tdbDataset.getDefaultModel();
            
            Resource person1 = defaultModel.createResource("http://example.org/data#person1");
            person1.addProperty(RDF.type, defaultModel.createResource("http://example.org/vocab#Person"));
            person1.addProperty(RDFS.label, "John Doe");
            person1.addProperty(defaultModel.createProperty("http://example.org/vocab#hasCategory"), "researcher");

            Resource person2 = defaultModel.createResource("http://example.org/data#person2");
            person2.addProperty(RDF.type, defaultModel.createResource("http://example.org/vocab#Person"));
            person2.addProperty(RDFS.label, "Jane Smith");
            person2.addProperty(defaultModel.createProperty("http://example.org/vocab#hasCategory"), "developer");
        });

        String tdbServiceUri = ServiceUriConstants.createServiceUri("example-tdb");
        serviceRegistry.registerDataset(tdbServiceUri, tdbDataset);
        log.info("Registered TDB dataset at {}", tdbServiceUri);
    }

    private void registerPersonsModel() {
        log.info("Creating and registering persons model");
        
        Model personsModel = ModelFactory.createDefaultModel();
        
        // Add some additional person data
        Resource person3 = personsModel.createResource("http://example.org/data#person3");
        person3.addProperty(RDF.type, personsModel.createResource("http://example.org/vocab#Person"));
        person3.addProperty(RDFS.label, "Alice Johnson");
        person3.addProperty(SKOS.prefLabel, "Alice Johnson");
        person3.addProperty(personsModel.createProperty("http://example.org/vocab#hasCategory"), "manager");

        Resource person4 = personsModel.createResource("http://example.org/data#person4");
        person4.addProperty(RDF.type, personsModel.createResource("http://example.org/vocab#Person"));
        person4.addProperty(RDFS.label, "Bob Wilson");
        person4.addProperty(SKOS.prefLabel, "Bob Wilson");
        person4.addProperty(personsModel.createProperty("http://example.org/vocab#hasCategory"), "analyst");

        String personsServiceUri = ServiceUriConstants.createServiceUri("persons");
        serviceRegistry.registerModel(personsServiceUri, personsModel);
        log.info("Registered persons model at {}", personsServiceUri);
    }
}