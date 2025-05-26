package zone.cogni.semanticz.jena.federation.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import zone.cogni.semanticz.jena.federation.core.LocalSparqlServiceRegistry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSparqlServiceConfigurationLifecycleTest {

  @Test
  void lifecycleCallbacksAreInvoked() {
    // given
    LocalSparqlServiceRegistry registry;
    
    // when - start Spring application context
    try (ConfigurableApplicationContext ctx =
                 SpringApplication.run(LocalSparqlServiceConfiguration.class)) {

      registry = ctx.getBean(LocalSparqlServiceRegistry.class);
      
      // then - registry should be initialized during startup
      assertTrue(registry.isInitialized(), "initialize() should have run");
    }
    
    // when - context is closed (try-with-resources calls ctx.close())
    // then - registry should be shut down via destroy callback
    assertFalse(registry.isInitialized(), "shutdown() should have run");
  }
}