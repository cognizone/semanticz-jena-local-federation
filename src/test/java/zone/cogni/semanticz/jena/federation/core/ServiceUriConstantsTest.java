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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceUriConstantsTest {

    @Test
    void testConstants() {
        // given - constants are defined in ServiceUriConstants
        
        // when/then - verify constant values
        assertEquals("urn", ServiceUriConstants.SERVICE_URI_SCHEME);
        assertEquals("urn:jena:service:", ServiceUriConstants.SERVICE_URI_PREFIX);
    }

    @Test
    void testCreateServiceUri() {
        // given - various service names
        
        // when/then - create URIs and verify format
        assertEquals("urn:jena:service:test", ServiceUriConstants.createServiceUri("test"));
        assertEquals("urn:jena:service:my-service", ServiceUriConstants.createServiceUri("my-service"));
        assertEquals("urn:jena:service:vocab", ServiceUriConstants.createServiceUri("  vocab  "));
    }

    @Test
    void testCreateServiceUriWithInvalidInput() {
        // given - invalid service name inputs
        
        // when/then - null input should throw exception
        assertThrows(IllegalArgumentException.class, () -> 
            ServiceUriConstants.createServiceUri(null));
        
        // when/then - empty input should throw exception
        assertThrows(IllegalArgumentException.class, () -> 
            ServiceUriConstants.createServiceUri(""));
        
        // when/then - whitespace-only input should throw exception
        assertThrows(IllegalArgumentException.class, () -> 
            ServiceUriConstants.createServiceUri("   "));
    }

    @Test
    void testIsLocalServiceUri() {
        // given - various URI formats
        
        // when/then - valid local service URIs should return true
        assertTrue(ServiceUriConstants.isLocalServiceUri("urn:jena:service:test"));
        assertTrue(ServiceUriConstants.isLocalServiceUri("urn:jena:service:my-service"));
        
        // when/then - non-local URIs should return false
        assertFalse(ServiceUriConstants.isLocalServiceUri("http://example.org/service"));
        assertFalse(ServiceUriConstants.isLocalServiceUri("urn:other:service:test"));
        assertFalse(ServiceUriConstants.isLocalServiceUri("urn:jena:other:test"));
        assertFalse(ServiceUriConstants.isLocalServiceUri(null));
        assertFalse(ServiceUriConstants.isLocalServiceUri(""));
    }

    @Test
    void testExtractServiceName() {
        // given - various service URIs
        
        // when/then - extract names from valid local service URIs
        assertEquals("test", ServiceUriConstants.extractServiceName("urn:jena:service:test"));
        assertEquals("my-service", ServiceUriConstants.extractServiceName("urn:jena:service:my-service"));
        assertEquals("complex.name-123", ServiceUriConstants.extractServiceName("urn:jena:service:complex.name-123"));
        
        // when/then - invalid URIs should return null
        assertNull(ServiceUriConstants.extractServiceName("http://example.org/service"));
        assertNull(ServiceUriConstants.extractServiceName("urn:other:service:test"));
        assertNull(ServiceUriConstants.extractServiceName(null));
        assertNull(ServiceUriConstants.extractServiceName(""));
    }

    @Test
    void testRoundTripConversion() {
        // given
        String serviceName = "my-test-service";
        
        // when
        String serviceUri = ServiceUriConstants.createServiceUri(serviceName);
        String extractedName = ServiceUriConstants.extractServiceName(serviceUri);
        
        // then
        assertEquals(serviceName, extractedName);
        assertTrue(ServiceUriConstants.isLocalServiceUri(serviceUri));
    }
}