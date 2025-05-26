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
        assertEquals("urn", ServiceUriConstants.SERVICE_URI_SCHEME);
        assertEquals("urn:jena:service:", ServiceUriConstants.SERVICE_URI_PREFIX);
    }

    @Test
    void testCreateServiceUri() {
        assertEquals("urn:jena:service:test", ServiceUriConstants.createServiceUri("test"));
        assertEquals("urn:jena:service:my-service", ServiceUriConstants.createServiceUri("my-service"));
        assertEquals("urn:jena:service:vocab", ServiceUriConstants.createServiceUri("  vocab  "));
    }

    @Test
    void testCreateServiceUriWithInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> 
            ServiceUriConstants.createServiceUri(null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            ServiceUriConstants.createServiceUri(""));
        
        assertThrows(IllegalArgumentException.class, () -> 
            ServiceUriConstants.createServiceUri("   "));
    }

    @Test
    void testIsLocalServiceUri() {
        assertTrue(ServiceUriConstants.isLocalServiceUri("urn:jena:service:test"));
        assertTrue(ServiceUriConstants.isLocalServiceUri("urn:jena:service:my-service"));
        
        assertFalse(ServiceUriConstants.isLocalServiceUri("http://example.org/service"));
        assertFalse(ServiceUriConstants.isLocalServiceUri("urn:other:service:test"));
        assertFalse(ServiceUriConstants.isLocalServiceUri("urn:jena:other:test"));
        assertFalse(ServiceUriConstants.isLocalServiceUri(null));
        assertFalse(ServiceUriConstants.isLocalServiceUri(""));
    }

    @Test
    void testExtractServiceName() {
        assertEquals("test", ServiceUriConstants.extractServiceName("urn:jena:service:test"));
        assertEquals("my-service", ServiceUriConstants.extractServiceName("urn:jena:service:my-service"));
        assertEquals("complex.name-123", ServiceUriConstants.extractServiceName("urn:jena:service:complex.name-123"));
        
        assertNull(ServiceUriConstants.extractServiceName("http://example.org/service"));
        assertNull(ServiceUriConstants.extractServiceName("urn:other:service:test"));
        assertNull(ServiceUriConstants.extractServiceName(null));
        assertNull(ServiceUriConstants.extractServiceName(""));
    }

    @Test
    void testRoundTripConversion() {
        String serviceName = "my-test-service";
        String serviceUri = ServiceUriConstants.createServiceUri(serviceName);
        String extractedName = ServiceUriConstants.extractServiceName(serviceUri);
        
        assertEquals(serviceName, extractedName);
        assertTrue(ServiceUriConstants.isLocalServiceUri(serviceUri));
    }
}