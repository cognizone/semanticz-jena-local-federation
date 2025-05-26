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

/**
 * Constants for SERVICE URI schemes and prefixes used in local SPARQL federation.
 */
public final class ServiceUriConstants {

    private ServiceUriConstants() {
        // Utility class
    }

    /**
     * The URN scheme used for local service URIs.
     */
    public static final String SERVICE_URI_SCHEME = "urn";

    /**
     * The standard prefix for local Jena service URIs.
     */
    public static final String SERVICE_URI_PREFIX = SERVICE_URI_SCHEME + ":jena:service:";

    /**
     * Creates a service URI with the standard prefix.
     *
     * @param serviceName The name of the service (e.g., "vocabulary", "my-tdb")
     * @return A properly formatted service URI
     */
    public static String createServiceUri(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }
        return SERVICE_URI_PREFIX + serviceName.trim();
    }

    /**
     * Checks if a URI follows the standard local service URI pattern.
     *
     * @param uri The URI to check
     * @return true if the URI starts with the standard prefix, false otherwise
     */
    public static boolean isLocalServiceUri(String uri) {
        return uri != null && uri.startsWith(SERVICE_URI_PREFIX);
    }

    /**
     * Extracts the service name from a properly formatted service URI.
     *
     * @param serviceUri The service URI
     * @return The service name, or null if the URI doesn't follow the standard pattern
     */
    public static String extractServiceName(String serviceUri) {
        if (isLocalServiceUri(serviceUri)) {
            return serviceUri.substring(SERVICE_URI_PREFIX.length());
        }
        return null;
    }
}