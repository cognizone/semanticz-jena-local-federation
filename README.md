# Semanticz Jena Local Federation

A Spring Boot library component for SPARQL federation on local Jena Models and TDB datasets. This library enables efficient SPARQL queries across multiple local data sources using custom SERVICE URIs, without requiring external HTTP calls.

## Features

- **Framework-agnostic core**: Can be used in any Java application
- **Spring Boot integration**: Auto-configuration for Spring Boot applications
- **Thread-safe**: Concurrent registration and querying support
- **Local SERVICE URIs**: Use `urn:jena:service:*` URIs for federation
- **Multiple data source types**: Support for both Jena Models and Datasets
- **Easy registration**: Simple API for registering local data sources

## Requirements

- **Java**: 17+
- **Apache Jena**: 5.3.0+
- **Spring Boot**: 3.4+ (for Spring integration)
- **Spring Framework**: 6.2+ (for Spring integration)

## Quick Start

### 1. Add Dependency

Add this library to your Spring Boot application:

#### Maven
```xml
<dependency>
    <groupId>zone.cogni.semanticz</groupId>
    <artifactId>semanticz-jena-local-federation</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Gradle
```gradle
implementation 'zone.cogni.semanticz:semanticz-jena-local-federation:1.0.0-SNAPSHOT'
```

### 2. Automatic Configuration

The library provides **automatic Spring Boot configuration** through `LocalSparqlServiceConfiguration`. When Jena ARQ is detected on the classpath, the configuration automatically:

- Creates a `LocalSparqlServiceRegistry` bean
- Initializes the registry with Jena's ServiceExecutorRegistry during startup
- Properly shuts down the registry when the application context closes
- Ensures `JenaSystem.init()` is called for proper Jena initialization

**No additional configuration is required** - the registry bean is ready to use as soon as your Spring Boot application starts.

### 3. Register Data Sources

Inject the `ServiceRegistry` bean and register your local data sources:

```java
@Configuration
public class MyDataConfiguration {
    
    private final ServiceRegistry serviceRegistry;
    
    public MyDataConfiguration(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }
    
    @PostConstruct
    public void registerData() {
        // Register a vocabulary model
        Model vocabModel = loadVocabularyModel();
        serviceRegistry.registerModel("urn:jena:service:vocabulary", vocabModel);
        
        // Register a TDB dataset
        Dataset tdbDataset = TDB2Factory.connectDataset("/path/to/tdb");
        serviceRegistry.registerDataset("urn:jena:service:my-data", tdbDataset);
    }
    
    private Model loadVocabularyModel() {
        // Your vocabulary loading logic here
        Model model = ModelFactory.createDefaultModel();
        // ... populate model
        return model;
    }
}
```

#### Alternative: Using ServiceUriConstants

For consistent URI management, use the provided constants:

```java
@PostConstruct
public void registerData() {
    // Using ServiceUriConstants for consistent URI formatting
    String vocabUri = ServiceUriConstants.createServiceUri("vocabulary");
    String dataUri = ServiceUriConstants.createServiceUri("my-data");
    
    serviceRegistry.registerModel(vocabUri, loadVocabularyModel());
    serviceRegistry.registerDataset(dataUri, loadTdbDataset());
}
```

### 4. Use Federation in SPARQL Queries

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

## Architecture Overview

### Core Components

The library consists of two main layers:

1. **Framework-agnostic Core** (`zone.cogni.semanticz.jena.federation.core`)
   - `ServiceRegistry` interface - Core API for registering local data sources
   - `LocalSparqlServiceRegistry` - Thread-safe implementation that integrates with Jena's ServiceExecutorRegistry
   - `ServiceUriConstants` - Utilities for consistent SERVICE URI management

2. **Spring Boot Integration** (`zone.cogni.semanticz.jena.federation.spring`)
   - `LocalSparqlServiceConfiguration` - Auto-configuration that creates and manages the registry bean

### How It Works

1. **Registration**: Local Jena Models and Datasets are registered with custom SERVICE URIs (e.g., `urn:jena:service:my-data`)
2. **Integration**: The registry registers itself with Jena's global ServiceExecutorRegistry
3. **Query Processing**: When SPARQL queries contain SERVICE clauses with registered URIs, the registry intercepts them
4. **Local Execution**: Sub-queries are executed locally on the registered data sources without HTTP overhead
5. **Result Federation**: Results are returned to the main query engine for federation with other data

## Non-Spring Usage

For applications not using Spring Boot:

```java
LocalSparqlServiceRegistry registry = new LocalSparqlServiceRegistry();
registry.initialize();

Model myModel = ModelFactory.createDefaultModel();
registry.registerModel("urn:jena:service:my-model", myModel);

// Use in SPARQL queries as shown above

registry.shutdown(); // Clean up when done
```

## License

Licensed under the Apache License, Version 2.0. See LICENSE file for details.