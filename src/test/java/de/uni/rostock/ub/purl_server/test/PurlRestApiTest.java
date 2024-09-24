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
package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import de.uni.rostock.ub.purl_server.client.PurlClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PurlRestApiTest extends PURLServerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    private static String host = "http://localhost:8080";
    private static String apiPath = "/api/purl";
    private static String purlPath = "/test/test312/";
    
    private static final String createBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + PurlClient.sha1(password);
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
    @Test
    @WithMockUser("user1")
    public void test() {
        createPurlTest();
        /*    checkCreatedPurlTest();
        modifyPurlTest();
        checkModifyPurlTest();
        deletePurlTest();*/
    }

    @Test
    public void createPurlNoPermissionTest() {
        try {
            MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.post(URI.create(apiPath + purlPath))
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .content("{\"type\":\"PARTIAL_302\",\"target\":\"http://matrikel.uni-rostock.de/id/1234674\"}");

            ResultActions result = mockMvc.perform(rb);
            result.andExpect(MockMvcResultMatchers.status().isUnauthorized());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    @Test
    public void getDomainTest() {
        try {
            MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.get(URI.create("/api/domain/test"))
                .header("Accept", "application/json");

            ResultActions result = mockMvc.perform(rb);
            result.andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("\"id\":12,\"path\":\"/test\"")));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    public static RequestPostProcessor authentication() {
        return request -> {
            request.addHeader("Authorization", createBasicAuthenticationHeader("John", "secr3t"));
            return request;
        };
    } 

    private void createPurlTest() {
        try {
            MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.post(URI.create(apiPath + purlPath))
                .header("Accept", "application/json")
                .header("Content-type", "application/json")
                .header("Authorization",  createBasicAuthenticationHeader("user1", "user1"))
                .content("{\"type\":\"PARTIAL_302\",\"target\":\"http://matrikel.uni-rostock.de/id/1234674\"}");
          //      .with(authentication());

            ResultActions result = mockMvc.perform(rb);
            result.andExpect(MockMvcResultMatchers.status().isCreated());
        } catch (Exception e) {
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
            assertEquals(302, response.statusCode(), "Statuscode must be 302!");
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void modifyPurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiPath + purlPath))
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
            assertEquals(200, response.statusCode(), "Statuscode must be 200!");
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
            assertEquals(302, response.statusCode(), "Statuscode must be 302!");
            assertEquals("http://test333.de/123", response.headers().firstValue("Location").orElse(""),
                "Location header must contain 'http://test333.de/{path}'");
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void deletePurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiPath + purlPath))
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
            assertEquals(200, response.statusCode(), "Statuscode must be 200!");
        } catch (URISyntaxException | IOException | InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private static void prepairPurlTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(apiPath + purlPath))
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
