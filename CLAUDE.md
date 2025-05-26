# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

- **Build**: `./gradlew build`
- **Test**: `./gradlew test`
- **Run tests for specific class**: `./gradlew test --tests "ClassName"`
- **Quality checks**: `./gradlew qualityCheck` (runs PMD, Jacoco, dependency check)
- **Clean build**: `./gradlew clean build`

## Testing

The project uses JUnit 5 (`junit.jupiter`). Test files are located in `src/test/java/`.

## Code Quality

- PMD static analysis is configured with minimum priority 5
- Jacoco code coverage reports are generated
- OWASP dependency vulnerability checking is enabled
- Java 17 is required

## Architecture Overview

This is a Spring Boot library that enables SPARQL federation on local Jena Models and TDB datasets using custom SERVICE URIs.

### Core Components

**`ServiceRegistry` interface**: Framework-agnostic API for registering Jena data sources
- Register Models/Datasets with custom URIs (e.g., `urn:jena:service:my-data`)
- Thread-safe concurrent registration and querying

**`LocalSparqlServiceRegistry`**: Core implementation that acts as both ServiceRegistry and Jena ServiceExecutor
- Manages thread-safe Maps of registered Models and Datasets
- Integrates with Jena's ServiceExecutorRegistry for SPARQL SERVICE clause handling
- Only supports SELECT queries in SERVICE blocks

**Spring Integration**: `LocalSparqlServiceConfiguration` provides auto-configuration
- Creates and manages LocalSparqlServiceRegistry bean with proper lifecycle
- Ensures `JenaSystem.init()` is called for proper Jena initialization

### Service URI Pattern

- Standard pattern: `urn:jena:service:{name}` (defined in `ServiceUriConstants`)
- Custom URIs are allowed but generate warnings
- URIs must be unique across both Models and Datasets

### Federation Flow

1. Register local data sources with custom SERVICE URIs
2. Use SERVICE clauses in SPARQL queries to access registered data
3. LocalSparqlServiceRegistry intercepts matching SERVICE URIs
4. Executes sub-queries locally on registered Models/Datasets
5. Returns results to the main query engine for federation

This enables efficient SPARQL federation without HTTP overhead for local data sources.