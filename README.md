2# Semanticz Jena Local Federation

A Spring Boot library component for SPARQL federation on local Jena Models and TDB datasets. This library enables efficient SPARQL queries across multiple local data sources using custom SERVICE URIs, without requiring external HTTP calls.

## Features

- **Framework-agnostic core**: Can be used in any Java application
- **Spring Boot integration**: Auto-configuration for Spring Boot applications
- **Thread-safe**: Concurrent registration and querying support
- **Local SERVICE URIs**: Use `urn:jena:service:*` URIs for federation
- **Multiple data source types**: Support for both Jena Models and Datasets
- **Easy registration**: Simple API for registering local data sources

## Quick Start

### 1. Add Dependency

Add this library to your Spring Boot application:

```xml
<dependency>
    <groupId>zone.cogni.semanticz</groupId>
    <artifactId>semanticz-jena-local-federation</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Register Data Sources

```java
@Configuration
public class MyDataConfiguration {
    
    @Autowired
    private ServiceRegistry serviceRegistry;
    
    @PostConstruct
    public void registerData() {
        // Register a vocabulary model
        Model vocabModel = loadVocabularyModel();
        serviceRegistry.registerModel("urn:jena:service:vocabulary", vocabModel);
        
        // Register a TDB dataset
        Dataset tdbDataset = TDB2Factory.connectDataset("/path/to/tdb");
        serviceRegistry.registerDataset("urn:jena:service:my-data", tdbDataset);
    }
}
```

### 3. Use Federation in SPARQL Queries

#### Basic Federation Query
```java
String basicQuery = """
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX vocab: <http://example.org/vocab#>
    
    SELECT ?person ?label ?category
    WHERE {
      SERVICE <urn:jena:service:example-tdb> {
        ?person a vocab:Person ;
                rdfs:label ?label ;
                vocab:hasCategory ?category .
      }
    }
    """;

try (QueryExecution qExec = QueryExecutionFactory.create(basicQuery, primaryModel)) {
    ResultSet results = qExec.execSelect();
    ResultSetFormatter.out(System.out, results);
}
```

#### Cross-Service Federation with Joins
```java
String joinQuery = """
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX vocab: <http://example.org/vocab#>
    
    SELECT ?person ?label ?category ?vocabLabel
    WHERE {
      # Query the TDB dataset for persons
      SERVICE <urn:jena:service:example-tdb> {
        ?person a vocab:Person ;
                rdfs:label ?label ;
                vocab:hasCategory ?category .
      }
      
      # Get vocabulary information from the vocabulary service
      SERVICE <urn:jena:service:vocabulary> {
        vocab:hasCategory rdfs:label ?vocabLabel .
      }
    }
    """;
```

#### Optional Federation
```java
String optionalQuery = """
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    PREFIX vocab: <http://example.org/vocab#>
    
    SELECT ?person ?label ?category ?prefLabel
    WHERE {
      # Query the TDB dataset for persons
      SERVICE <urn:jena:service:example-tdb> {
        ?person a vocab:Person ;
                rdfs:label ?label ;
                vocab:hasCategory ?category .
      }
      
      # Optionally get preferred labels from the persons service
      OPTIONAL {
        SERVICE <urn:jena:service:persons> {
          ?person skos:prefLabel ?prefLabel .
        }
      }
    }
    """;
```

#### Union Across Multiple Services
```java
String unionQuery = """
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX vocab: <http://example.org/vocab#>
    
    SELECT ?person ?label ?source
    WHERE {
      {
        SERVICE <urn:jena:service:example-tdb> {
          ?person a vocab:Person ;
                  rdfs:label ?label .
        }
        BIND("TDB" AS ?source)
      }
      UNION
      {
        SERVICE <urn:jena:service:persons> {
          ?person a vocab:Person ;
                  rdfs:label ?label .
        }
        BIND("Persons Model" AS ?source)
      }
    }
    """;
```

#### Filtered Federation
```java
String filteredQuery = """
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX vocab: <http://example.org/vocab#>
    
    SELECT ?person ?label
    WHERE {
      SERVICE <urn:jena:service:example-tdb> {
        ?person a vocab:Person ;
                rdfs:label ?label ;
                vocab:hasCategory ?category .
        FILTER(?category = "researcher")
      }
    }
    """;
```

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.