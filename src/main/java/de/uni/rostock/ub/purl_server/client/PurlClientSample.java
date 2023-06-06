/*
 * Copyright 2021 University Library, 18051 Rostock, Germany
 *
 * This file is part of the application "PURL Server".
 * https://github.com/ubrostock/purl_server
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni.rostock.ub.purl_server.client;

import de.uni.rostock.ub.purl_server.client.PurlClient.PURL;

public class PurlClientSample {
   
    public static void main(String[] args) {
        PurlClient client = new PurlClient("http://localhost:8080").login("admin",  "admin");

        PURL p = PurlClient.buildPURL("/test/with/trailing1/slash1/", "http://www.google.de",  PurlClient.PURLType.PARTIAL_302);
        client.createPURL(p);
        
        client.logout();
    }

}
