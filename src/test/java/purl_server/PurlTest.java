/*
 * Copyright 2020 University Library, 18051 Rostock, Germany
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
package purl_server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.junit.Before;
import org.junit.Test;

import de.uni.rostock.ub.purl_server.PurlServerWebApp;
import de.uni.rostock.ub.purl_server.client.PurlClient;

public class PurlTest {
    private String host = "http://localhost:8080";
    private String adminHost = "http://localhost:8080/api/purl";
    private String purlPath = "/test/test312/";

    @Before
    public void init() {
        PurlServerWebApp.main(new String[] {});
        prepairPurlTest();
    }

    @Test
    public void test() {
        createPurlTest();
        checkCreatedPurlTest();
        modifyPurlTest();
        checkModifyPurlTest();
        deletePurlTest();
    }

    private void createPurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(adminHost + purlPath))
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers
                    .ofString("{\"type\":\"PARTIAL_302\",\"target\":\"http://matrikel.uni-rostock.de/id/1234674\"}"))
                .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("admin", PurlClient.sha1("admin").toCharArray());
                    }
                }).build()
                .send(request, BodyHandlers.ofString());
            assertEquals("Statuscode must be 201!", 201, response.statusCode());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void checkCreatedPurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(host + purlPath + "123"))
                .GET()
                .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, BodyHandlers.ofString());
            assertEquals("Statuscode must be 302!", 302, response.statusCode());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void modifyPurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(adminHost + purlPath))
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .PUT(
                    HttpRequest.BodyPublishers.ofString("{\"type\":\"PARTIAL_302\",\"target\":\"http://test333.de/\"}"))
                .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("admin", PurlClient.sha1("admin").toCharArray());
                    }
                }).build()
                .send(request, BodyHandlers.ofString());
            assertEquals("Statuscode must be 200!", 200, response.statusCode());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void checkModifyPurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(host + purlPath + "123"))
                .GET()
                .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                .build()
                .send(request, BodyHandlers.ofString());
            assertEquals("Statuscode must be 302!", 302, response.statusCode());
            assertEquals("Location header must contain 'http://test333.de/{path}'", "http://test333.de/123",
                response.headers().firstValue("Location").orElse(""));
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void deletePurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(adminHost + purlPath))
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .DELETE()
                .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("admin", PurlClient.sha1("admin").toCharArray());
                    }
                }).build()
                .send(request, BodyHandlers.ofString());
            assertEquals("Statuscode must be 200!", 200, response.statusCode());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void prepairPurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(adminHost + purlPath))
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .DELETE()
                .build();
            HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("admin", PurlClient.sha1("admin").toCharArray());
                    }
                }).build()
                .send(request, BodyHandlers.ofString());

        } catch (URISyntaxException | IOException | InterruptedException e) {
            // DO NOTHING
        }
    }
}
