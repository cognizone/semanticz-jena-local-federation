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

package zone.cogni.semanticz.jena.federation.spring;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zone.cogni.semanticz.jena.federation.core.LocalSparqlServiceRegistry;
import zone.cogni.semanticz.jena.federation.core.ServiceRegistry;

/**
 * Spring Boot auto-configuration for the Local SPARQL Service Registry.
 * 
 * This configuration automatically creates and manages a ServiceRegistry bean
 * when Spring Boot and Jena ARQ are on the classpath.
 */
@Configuration
@ConditionalOnClass({org.apache.jena.query.Query.class})
public class LocalSparqlServiceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LocalSparqlServiceConfiguration.class);

    /**
     * Creates a ServiceRegistry bean that will be automatically initialized
     * and shut down with the Spring application context.
     */
    @Bean
    public ServiceRegistry serviceRegistry() {
        log.info("Creating LocalSparqlServiceRegistry bean");
        return new LocalSparqlServiceRegistry();
    }

    /**
     * Wrapper bean to manage the lifecycle of the ServiceRegistry.
     */
    @Bean
    public ServiceRegistryLifecycleManager serviceRegistryLifecycleManager(ServiceRegistry serviceRegistry) {
        return new ServiceRegistryLifecycleManager(serviceRegistry);
    }

    /**
     * Manages the initialization and shutdown of the ServiceRegistry.
     */
    public static class ServiceRegistryLifecycleManager {
        private static final Logger log = LoggerFactory.getLogger(ServiceRegistryLifecycleManager.class);
        
        private final ServiceRegistry serviceRegistry;

        public ServiceRegistryLifecycleManager(ServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        @PostConstruct
        public void initialize() {
            log.info("Initializing ServiceRegistry");
            serviceRegistry.initialize();
            log.info("ServiceRegistry initialized successfully");
        }

        @PreDestroy
        public void shutdown() {
            log.info("Shutting down ServiceRegistry");
            serviceRegistry.shutdown();
            log.info("ServiceRegistry shut down successfully");
        }

        public ServiceRegistry getServiceRegistry() {
            return serviceRegistry;
        }
    }
}