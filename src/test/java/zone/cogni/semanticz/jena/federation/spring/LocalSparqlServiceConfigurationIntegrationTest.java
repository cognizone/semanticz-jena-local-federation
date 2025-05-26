package zone.cogni.semanticz.jena.federation.spring;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import zone.cogni.semanticz.jena.federation.core.LocalSparqlServiceRegistry;
import zone.cogni.semanticz.jena.federation.core.ServiceRegistry;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = LocalSparqlServiceConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class LocalSparqlServiceConfigurationIntegrationTest {

  @Autowired
  ServiceRegistry registry;

  @Test
  void registryBeanIsCreatedAndInitialized() {
    // given - Spring Boot application context is started with LocalSparqlServiceConfiguration
    // (handled by @SpringBootTest annotation)
    
    // when - registry bean is autowired by Spring
    // (handled by @Autowired annotation)
    
    // then - registry should be initialized
    assertTrue(((LocalSparqlServiceRegistry) registry).isInitialized());
  }
}