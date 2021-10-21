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

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurlClient {
    private static Logger LOGGER = LoggerFactory.getLogger(PurlClient.class);

    public static String TYPE_302 = "FOUND_302";

    public static String TYPE_PARTIAL_302 = "PARTIAL_302";

    public static String TYPE_410 = "GONE_410";

    private String baiscURL = "";

    private HttpClient httpClient = null;
    
    private String message = "";

    public static void main(String[] args) {
        PurlClient app = new PurlClient("http://localhost:8080");
  //      app.createPURL("/nureintest/testneu12345", "http://google.de/", TYPE_PARTIAL_302);
        app.login("admin", "admin");
  //      app.createPURL("/test/bcd", "http://google.de/", TYPE_PARTIAL_302);
  //      app.createPURL("/nureintest/testneu12345", "http://google.de/", TYPE_PARTIAL_302);
  //      app.updatePURL("/nureintest/testneu12345", "http://test1", TYPE_PARTIAL_302);
        app.createPURL("/web/ub/test", "http://google.de/", TYPE_302);
        app.createPURL("/keinedomain/123", "http://google.de/", TYPE_302);
        boolean result = app.createPURL("/keinedomain/123", "http://google.de/", TYPE_302);
        if(!result) {
        	System.out.println("Fehler " + app.getMessage());
        }
        app.logout();
    }

    public PurlClient(String basicURL) {
        this.baiscURL = basicURL + "/api/purl";
        httpClient = HttpClient.newBuilder().build();
    }

    public void login(String user, String password) {
        httpClient = HttpClient.newBuilder().authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, sha1(password).toCharArray());
            }
        }).build();
    }

    public void logout() {
        httpClient = HttpClient.newBuilder().build();
    }

    public boolean createPURL(String purl, String target, String type) {
        message = "";
    	String purlJson = "{\"type\":\"" + type + "\",\"target\":\"" + target + "\"}";
        HttpRequest request = HttpRequest.newBuilder().POST(BodyPublishers.ofString(purlJson))
            .header("Content-Type", "application/json").uri(URI.create(baiscURL + purl)).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == HttpServletResponse.SC_CREATED) {
                return true;
            }
            message = response.body();
            LOGGER.info(message);
        } catch (IOException | InterruptedException e) {
            message = "Error creating a PURL!";
        	LOGGER.error(message, e);
        }
        return false;
    }

    public boolean updatePURL(String purl, String target, String type) {
        message = "";
    	String purlJson = "{\"type\":\"" + type + "\",\"target\":\"" + target + "\"}";
        HttpRequest request = HttpRequest.newBuilder().PUT(BodyPublishers.ofString(purlJson))
            .header("Content-Type", "application/json").uri(URI.create(baiscURL + purl))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == HttpServletResponse.SC_OK) {
                return true;
            }
            message = response.body();
            LOGGER.info(message);
        } catch (IOException | InterruptedException e) {
        	message = "Error updating a PURL!";
            LOGGER.error(message, e);
        }
        return false;
    }

    public String showPURL(String purl) {
       message = "";
       HttpRequest request = HttpRequest.newBuilder().GET()
            .uri(URI.create(baiscURL + purl)).build();
       try {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if(response.statusCode() == HttpServletResponse.SC_OK) {
            return response.body();
        }
        if(response.statusCode() == HttpServletResponse.SC_NOT_FOUND) {
        	message = "PURL " + purl + " not found!";
			LOGGER.info(message);
        }
       } catch (IOException | InterruptedException e) {
    	   message = "Error displaying a PURL!";
           LOGGER.error(message, e);
       }
       return null;
    }
    
    public String getMessage() {
    	return message;
    }
    
    private static String sha1(String plain) {
        try {
            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(plain.getBytes());
            return String.format("%040x", new BigInteger(1, sha1));
        } catch (NoSuchAlgorithmException e) {
            // will never happen
        }
        return null;
    }
}
