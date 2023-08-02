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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample application to demonstrate the use of the PURL server REST API.
 */
public class PurlClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurlClient.class);

    private static final String ERROR_MESSAGE_LOGIN = "Please login into PURL client!";

    private static final List<String> ALLOWED_SERIALIZED_CLASSES = Arrays.asList(
        LinkedHashMap.class.getName(), HashMap.class.getName(),
        ArrayList.class.getName(), Map.Entry[].class.getName(), Object[].class.getName(),
        String.class.getName(), Integer.class.getName(), Number.class.getName(), Boolean.class.getName());

    public enum PURLType {
        REDIRECT_302, PARTIAL_302, GONE_410;
    }

    private String apiURL = "";

    private Optional<HttpClient> httpClient = Optional.empty();

    private StringBuilder messageBuffer = new StringBuilder();

    private Integer lastStatus = null;

    public PurlClient(String baseURL) {
        this.apiURL = baseURL + "/api/purl";
        httpClient = Optional.empty();
    }

    public PurlClient login(String user, String password) {
        httpClient = Optional.of(HttpClient.newBuilder().authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String encrPasswd = sha1(password);
                return encrPasswd != null ? new PasswordAuthentication(user, encrPasswd.toCharArray()) : null;
            }
        }).build());
        return this;
    }

    public void logout() {
        httpClient = Optional.empty();
    }

    public boolean createPURL(PURL p) {
        if (httpClient.isPresent()) {
            String message = "";
            HttpRequest request = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(outputPURLPayloadAsJSON(p)))
                .header("Content-Type", "application/json")
                .uri(URI.create(apiURL + p.getPath()))
                .build();
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
            } catch (IOException e) {
                message = "Error creating a PURL!";
                LOGGER.error(message, e);
                messageBuffer.append(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            LOGGER.error(ERROR_MESSAGE_LOGIN);
            messageBuffer.append("\n" + ERROR_MESSAGE_LOGIN);
        }
        return false;
    }

    public boolean updatePURL(PURL p) {
        if (httpClient.isPresent()) {
            String message = "";
            HttpRequest request = HttpRequest.newBuilder()
                .PUT(BodyPublishers.ofString(outputPURLPayloadAsJSON(p)))
                .header("Content-Type", "application/json").uri(URI.create(apiURL + p.getPath()))
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

            } catch (IOException e) {
                message = "Error updating a PURL!";
                messageBuffer.append(message);
                LOGGER.error(message, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            LOGGER.error(ERROR_MESSAGE_LOGIN);
            messageBuffer.append("\n" + ERROR_MESSAGE_LOGIN);
        }
        return false;
    }
    
    public boolean deletePURL(PURL p) {
        if (httpClient.isPresent()) {
            String message = "";
            HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .header("Content-Type", "application/json").uri(URI.create(apiURL + p.getPath()))
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

            } catch (IOException e) {
                message = "Error updating a PURL!";
                messageBuffer.append(message);
                LOGGER.error(message, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            LOGGER.error(ERROR_MESSAGE_LOGIN);
            messageBuffer.append("\n" + ERROR_MESSAGE_LOGIN);
        }
        return false;
    }

    public String retrievePURLInfoAsJsonString(String path) {
        if (httpClient.isPresent()) {
            messageBuffer = new StringBuilder();
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiURL + path))
                .build();
            try {
                lastStatus = null;
                HttpResponse<String> response = httpClient.get().send(request, HttpResponse.BodyHandlers.ofString());
                lastStatus = response.statusCode();
                if (response.statusCode() == HttpServletResponse.SC_OK) {
                    return response.body();
                }
                if (response.statusCode() == HttpServletResponse.SC_NOT_FOUND) {
                    String message = "PURL " + path + " not found!";
                    LOGGER.info(message);
                    messageBuffer.append(message);
                }
            } catch (IOException e) {
                String message = "Error retrieving information about a PURL!";
                LOGGER.error(message, e);
                messageBuffer.append(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        } else {
            LOGGER.error(ERROR_MESSAGE_LOGIN);
            messageBuffer.append("\n" + ERROR_MESSAGE_LOGIN);
        }
        return null;
    }

    /**
     * returns the PURL information as LinkedHashMap
     * 
     * This prevents us from having a dependency to one particular JSON library
     * If you want to process the data as JSON, use retrievePURLInfoAsJsonString()
     * and process the result with a JSON parser of your choice.
     * 
     * @param purl
     * @return
     */
    public Map<String, Object> retrievePURLInfoAsMap(String path) {
        if (httpClient.isPresent()) {
            messageBuffer = new StringBuilder();
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .header("Accept", "application/x-java-serialized-object")
                .uri(URI.create(apiURL + path))
                .build();
            try {
                lastStatus = null;
                HttpResponse<InputStream> response = httpClient.get().send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
                lastStatus = response.statusCode();
                if (response.statusCode() == HttpServletResponse.SC_OK) {
                    return retrieveMapFromResponse(path, response);
                }
                if (response.statusCode() == HttpServletResponse.SC_NOT_FOUND) {
                    String message = "PURL " + path + " not found!";
                    LOGGER.info(message);
                    messageBuffer.append(message);
                }
            } catch (IOException e) {
                String message = "Error retrieving information about a PURL!";
                LOGGER.error(message, e);
                messageBuffer.append(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        } else {
            LOGGER.error(ERROR_MESSAGE_LOGIN);
            messageBuffer.append("\n" + ERROR_MESSAGE_LOGIN);
        }
        return null;
    }

    private LinkedHashMap<String, Object> retrieveMapFromResponse(String path, HttpResponse<InputStream> response)
        throws IOException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(response.body())) {
            objectInputStream.setObjectInputFilter(PurlClient::serializedHashMapFilter);
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) objectInputStream
                .readObject();
            return map;
        } catch (ClassNotFoundException e) {
            String message = "PURL " + path + " not found: " + e.getMessage();
            LOGGER.info(message);
            messageBuffer.append(message);
        }
        return null;
    }

    public boolean existsPURL(PURL p) {
        PURL retrievedPURL = buildPURL(retrievePURLInfoAsMap(Objects.requireNonNull(p).getPath()));
        return retrievedPURL != null;
    }

    public boolean existsIdenticalPURL(PURL p) {
        PURL retrievedPURL = buildPURL(retrievePURLInfoAsMap(Objects.requireNonNull(p).getPath()));
        return p.equals(retrievedPURL);
    }

    /**
     * Retrieve all messages of the PURL client and delete the message buffer.
     */
    public String callMessages() {
        String result = messageBuffer.toString();
        messageBuffer = new StringBuilder();
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

    /**
     * check if the serialized Java object is a LinkedHashMap
     * @param info
     * @return
     */
    static ObjectInputFilter.Status serializedHashMapFilter(ObjectInputFilter.FilterInfo info) {
        Class<?> serialClass = info.serialClass();
        if (serialClass != null) {
            return ALLOWED_SERIALIZED_CLASSES.contains(serialClass.getName())
                ? ObjectInputFilter.Status.ALLOWED
                : ObjectInputFilter.Status.REJECTED;
        }
        return ObjectInputFilter.Status.UNDECIDED;
    }

    public static PURL buildPURL(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        return new PURL(
            String.valueOf(Objects.requireNonNull(data.get("path"), "path cannot be null")),
            String.valueOf(Objects.requireNonNull(data.get("target"), "target cannot be null")),
            PURLType.valueOf(String.valueOf(Objects.requireNonNull(data.get("type"), "type cannot be null"))));
    }

    public static PURL buildPURL(String path, String target, PURLType type) {
        return new PURL(
            String.valueOf(Objects.requireNonNull(path, "path cannot be null")),
            String.valueOf(Objects.requireNonNull(target, "target cannot be null")),
            PURLType.valueOf(String.valueOf(Objects.requireNonNull(type, "type cannot be null"))));
    }

    public static String outputPURLPayloadAsJSON(PURL p) {
        return "{\"type\":\"" + p.getType().toString() + "\",\"target\":\"" + p.getTarget() + "\"}";
    }

    public static void main(String[] args) {
        PurlClient app = new PurlClient("http://localhost:8080").login("test", "test123");
        boolean result = app.createPURL(buildPURL("/test/google", "http://google.de/", PURLType.REDIRECT_302));
        if (!result) {
            System.out.println("FEHLER!!!");
        }
        System.out.println("Message: " + app.callMessages());
        System.out.println("HTTP Status: " + app.callStatusCode());

        result = app.updatePURL(buildPURL("/test/google", "http://google.com/", PURLType.REDIRECT_302));
        if (!result) {
            System.out.println("FEHLER!!!");
        }
        System.out.println("Message: " + app.callMessages());
        System.out.println("HTTP Status: " + app.callStatusCode());

        Map<String, Object> data = app.retrievePURLInfoAsMap("/test/google");
        System.out.println(data.keySet());
        System.out.println("REDIRECT_302".equals(data.get("type")));

        app.logout();
    }

    /**
     * PURL data as POJO
     * 
     * for Java17 this could be reimplemented as Record Class (https://openjdk.org/jeps/395)
     * 
     *  Q: static or not static nested class?
     *  A: "In effect, a static nested class is behaviorally a top-level class that has been nested 
     *      in another top-level class for packaging convenience."
     *      (https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html)
     */
    public static class PURL {
        private String path;

        private String target;

        private PURLType type;

        //empty no-arg constructor
        public PURL() {

        }

        public PURL(String path, String target, PURLType type) {
            super();
            this.path = path;
            this.target = target;
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public PURLType getType() {
            return type;
        }

        public void setType(PURLType type) {
            this.type = type;
        }

        /**
         * generated via IDE
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result + ((target == null) ? 0 : target.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        /**
         * generated via IDE
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PURL other = (PURL) obj;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            if (target == null) {
                if (other.target != null)
                    return false;
            } else if (!target.equals(other.target) || type != other.type)
                return false;
            return true;
        }
    }

}
