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

/**
 * This is a sample application to demonstrate the use of the PURL server REST API.
 */
import java.io.IOException;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurlClient {
    private static Logger LOGGER = LoggerFactory.getLogger(PurlClient.class);

    public enum PURLType {
        REDIRECT_302, PARTIAL_302, GONE_410;
    }

    private String apiURL = "";

    private Optional<HttpClient> httpClient = Optional.empty();

    private StringBuffer messageBuffer = new StringBuffer();

    private Integer lastStatus = null;

    public PurlClient(String baseURL) {
        this.apiURL = baseURL + "/api/purl";
        httpClient = Optional.empty();
    }

    public PurlClient login(String user, String password) {
        httpClient = Optional.of(HttpClient.newBuilder().authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, sha1(password).toCharArray());
            }
        }).build());
        return this;
    }

    public void logout() {
        httpClient = Optional.empty();
    }

    public boolean createPURL(String purl, String target, PURLType type) {
        if (httpClient.isPresent()) {
            String message = "";
            String purlJson = "{\"type\":\"" + type.toString() + "\",\"target\":\"" + target + "\"}";
            HttpRequest request = HttpRequest.newBuilder().POST(BodyPublishers.ofString(purlJson))
                .header("Content-Type", "application/json")
                .uri(URI.create(apiURL + purl)).build();
            try {
                lastStatus = null;
                HttpResponse<String> response = httpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();
                if (response.statusCode() == HttpServletResponse.SC_CREATED) {
                    return true;
                }
                message = response.body();
                LOGGER.info(message);
                messageBuffer.append(message);
            } catch (IOException | InterruptedException e) {
                message = "Error creating a PURL!";
                LOGGER.error(message, e);
                messageBuffer.append(message);
            }
        } else {
            String errorMmessage = "Please login into PURL client!";
            LOGGER.error(errorMmessage);
            messageBuffer.append("\n" + errorMmessage);
        }
        return false;
    }

    public boolean updatePURL(String purl, String target, PURLType type) {
        if (httpClient.isPresent()) {
            String message = "";
            String purlJson = "{\"type\":\"" + type.toString() + "\",\"target\":\"" + target + "\"}";
            HttpRequest request = HttpRequest.newBuilder().PUT(BodyPublishers.ofString(purlJson))
                .header("Content-Type", "application/json").uri(URI.create(apiURL + purl))
                .build();
            try {
                lastStatus = null;
                HttpResponse<String> response = httpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();
                message = response.body();
                LOGGER.info(message);
                messageBuffer.append(message);
                if (response.statusCode() == HttpServletResponse.SC_OK) {
                    return true;
                }

            } catch (IOException | InterruptedException e) {
                message = "Error updating a PURL!";
                messageBuffer.append(message);
                LOGGER.error(message, e);
            }
        } else {
            String errorMmessage = "Please login into PURL client!";
            LOGGER.error(errorMmessage);
            messageBuffer.append("\n" + errorMmessage);
        }
        return false;
    }

    public String infoPURL(String purl) {
        if (httpClient.isPresent()) {
            messageBuffer = new StringBuffer();
            HttpRequest request = HttpRequest.newBuilder().GET()
                .uri(URI.create(apiURL + purl)).build();
            try {
                lastStatus = null;
                HttpResponse<String> response = httpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();
                if (response.statusCode() == HttpServletResponse.SC_OK) {
                    return response.body();
                }
                if (response.statusCode() == HttpServletResponse.SC_NOT_FOUND) {
                    String message = "PURL " + purl + " not found!";
                    LOGGER.info(message);
                    messageBuffer.append(message);
                }
            } catch (IOException | InterruptedException e) {
                String message = "Error retrieving information about a PURL!";
                LOGGER.error(message, e);
                messageBuffer.append(message);
            }
            return null;
        } else {
            String errorMmessage = "Please login into PURL client!";
            LOGGER.error(errorMmessage);
            messageBuffer.append("\n" + errorMmessage);
        }
        return null;
    }

    /**
     * Retrieve all messages of the PURL client and delete the message buffer.
     */
    public String callMessages() {
        String result = messageBuffer.toString();
        messageBuffer = new StringBuffer();
        return result;
    }

    /**
     * Retrieve the last statusCode, which was sent from the PURL server to the client.
     */
    public Integer callStatusCode() {
        Integer result = lastStatus;
        lastStatus = null;
        return result;
    }

    /**
     * Calculate SHA1-Checksum of a String.
     * The method is used to send the password in an encrypted form.
     * @param plain
     * @return
     */
    private static String sha1(String plainText) {
        try {
            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(plainText.getBytes());
            return String.format("%040x", new BigInteger(1, sha1));
        } catch (NoSuchAlgorithmException e) {
            // will never happen
            return null;
        }
    }

    public static void main(String[] args) {
        PurlClient app = new PurlClient("http://localhost:8080").login("test", "test123");
        //      app.createPURL("/test/bcd", "http://google.de/", TYPE_PARTIAL_302);
        //      app.createPURL("/nureintest/testneu12345", "http://google.de/", TYPE_PARTIAL_302);
        //      app.updatePURL("/nureintest/testneu12345", "http://test1", TYPE_PARTIAL_302);

        boolean result = app.createPURL("/test/google", "http://google.de/", PURLType.REDIRECT_302);
        if (!result) {
            System.out.println("FEHLER!!!");
        }
        System.out.println("Message: " + app.callMessages());
        System.out.println("HTTP Status: " + app.callStatusCode());

        result = app.updatePURL("/test/google", "http://google.com/", PURLType.REDIRECT_302);
        if (!result) {
            System.out.println("FEHLER!!!");
        }
        System.out.println("Message: " + app.callMessages());
        System.out.println("HTTP Status: " + app.callStatusCode());

        app.logout();
    }

}
