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

import org.apache.jena.sparql.ARQConstants;
import org.apache.jena.sparql.service.ServiceExecutorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zone.cogni.semanticz.jena.federation.core.LocalSparqlServiceRegistry;
import zone.cogni.semanticz.jena.federation.core.ServiceRegistry;

/**
 * Spring Boot auto-configuration for the Local SPARQL Service Registry.
 * <p>
 * This configuration automatically creates and manages a ServiceRegistry bean
 * when Spring Boot and Jena ARQ are on the classpath.
 */
@Configuration
@ConditionalOnClass(org.apache.jena.query.Query.class)
public class LocalSparqlServiceConfiguration {

  static {
    // THIS LINE IS CRITICAL – kick off Jena’s own
    // SystemARQ / ARQConstants / FunctionRegistry wiring
    org.apache.jena.sys.JenaSystem.init();
  }

  @Bean(initMethod="initialize", destroyMethod="shutdown")
  public LocalSparqlServiceRegistry serviceRegistry() {
    return new LocalSparqlServiceRegistry();
  }

}