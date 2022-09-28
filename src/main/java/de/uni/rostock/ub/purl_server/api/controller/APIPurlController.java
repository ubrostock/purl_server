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
package de.uni.rostock.ub.purl_server.api.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.PurlServerResponse;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.PurlValidateService;

/**
 * Retrieve and modify PURLs
 */
@Controller
public class APIPurlController {
    @Autowired
    PurlAccess purlAccess;

    @Autowired
    PurlValidateService purlValidateService;

    @Autowired
    PurlDAO purlDAO;

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    private MessageSource messages;

    /**
     * Return a purl
     * 
     * @statuscode 200 if the purl was found
     * @statuscode 404 if the purl does not exist
     * @return the ResponseEntity object with the retrieved purl include the purl
     *         history
     */
    @GetMapping(path = "/api/purl/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Purl> retrievePurl(HttpServletRequest request) {
        String purlPath = retrievePurlPathFromRequest(request);
        Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
        if (op.isEmpty()) {
            return new ResponseEntity<Purl>(HttpStatus.NOT_FOUND);
        }
        ResponseEntity<Purl> r = new ResponseEntity<Purl>(op.get(), HttpStatus.OK);
        return r;
    }

    /**
     * Return a purl
     * 
     * @statuscode 200 if the purl was found
     * @statuscode 404 if the purl does not exist
     * @return the ResponseEntity object with the retrieved purl include the purl
     *         history
     */
    @GetMapping(path = "/api/purl/**",
        produces = "application/x-java-serialized-object")
    public ResponseEntity<Resource> retrievePurlAsHashMap(HttpServletRequest request) {
        String purlPath = retrievePurlPathFromRequest(request);
        Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
        if (op.isEmpty()) {
            return new ResponseEntity<Resource>(HttpStatus.NOT_FOUND);
        }

        try {
            ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .build();

            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> result = mapper.readValue(mapper.writeValueAsString(op.get()),
                LinkedHashMap.class);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(result);
            }
            ByteArrayResource res = new ByteArrayResource(baos.toByteArray());

            return new ResponseEntity<Resource>(res, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<Resource>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a purl
     * 
     * @param inputPurl
     * @statuscode 201 if the purl was created
     * @statuscode 401 if the user is unauthorized
     * @statuscode 404 if the purl does not exist
     * @statuscode 409 if the purl already exists
     * @return the ResponseEntity object with the created purl
     */
    @PostMapping(path = "/api/purl/**", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<? extends PurlServerResponse> createPurl(@RequestBody Purl inputPurl, Locale locale,
        HttpServletRequest request) {

        String purlPath = retrievePurlPathFromRequest(request);
        if (inputPurl == null) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.create", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.input.empty", null, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        inputPurl.setPath(purlPath);
        User u = purlAccess.retrieveUserFromRequest(request);
        List<String> errorList = purlValidateService.validateCreatePurl(inputPurl, u, locale);
        if (!errorList.isEmpty()) {
            // TODO Fehlerliste ausgeben als JSON
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                messages.getMessage("purl_server.error.purl.create", null, locale),
                errorList);
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }

        Optional<Domain> d = domainDAO.retrieveDomain(inputPurl);
        if (d.isPresent()) {
            if (purlAccess.canCreatePurl(d.get(), u) == false) {
                PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED,
                    messages.getMessage("purl_server.error.purl.create", null, locale),
                    List.of(messages.getMessage("purl_server.error.purl.create", new Object[] { u.getFullname() },
                        locale)));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.UNAUTHORIZED);
            }
        }
        inputPurl.setPath(purlPath);
        purlDAO.createPurl(inputPurl, u);

        return new ResponseEntity<Purl>(purlDAO.retrievePurlWithHistory(inputPurl.getPath()).get(), HttpStatus.CREATED);
    }

    /**
     * Modify a purl
     * 
     * @param inputPurl
     * @statuscode 200 if the purl was modified
     * @statuscode 401 if the user is unauthorized
     * @statuscode 404 if the purl does not exist
     * @return the ResponseEntity object with the modified purl
     */
    @PutMapping(path = "/api/purl/**")
    public ResponseEntity<? extends PurlServerResponse> modifyPurl(@RequestBody Purl inputPurl, Locale locale,
        HttpServletRequest request) {
        String purlPath = retrievePurlPathFromRequest(request);
        if (inputPurl == null) {
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                messages.getMessage("purl_server.error.purl.update", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.input.empty", null, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
        inputPurl.setPath(purlPath);
        User u = purlAccess.retrieveUserFromRequest(request);
        List<String> errorList = purlValidateService.validateModifyPurl(inputPurl, u, locale);
        if (!errorList.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                messages.getMessage("purl_server.error.purl.update", null, locale),
                errorList);
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
        Purl p = purlDAO.retrievePurl(purlPath).get();
        if (p == null) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.update", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.path", new Object[] { purlPath },
                    locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        if (ObjectUtils.isEmpty(inputPurl.getType())) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.update", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.type", null, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            p.setType(inputPurl.getType());
        }
        if (p.getStatus() == Status.DELETED) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.update", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.deleted", null, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        if (!StringUtils.hasText(inputPurl.getTarget())) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.update", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.target.empty", null, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            p.setTarget(inputPurl.getTarget());
        }
        Optional<Domain> d = domainDAO.retrieveDomain(inputPurl);
        if (d.isPresent()) {
            if (purlAccess.canModifyPurl(d.get(), u) == false) {
                PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED,
                    messages.getMessage("purl_server.error.purl.create", null, locale),
                    List.of(messages.getMessage("purl_server.error.user.create.unauthorized",
                        new Object[] { u.getFullname() }, locale)));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.UNAUTHORIZED);
            }
            purlDAO.modifyPurl(p, u);
        }
        // TODO was passiert, wenn D nicht da ist.
        return new ResponseEntity<Purl>(purlDAO.retrievePurlWithHistory(p.getPath()).get(), HttpStatus.OK);
    }

    /**
     * Delete a purl
     * 
     * @param inputPurl
     * @statuscode 200 if the purl was deleted
     * @statuscode 401 if the user is unauthorized
     * @statuscode 404 if the purl does not exist
     * @return the ResponseEntity
     */
    @DeleteMapping(path = "/api/purl/**")
    public ResponseEntity<? extends PurlServerResponse> deletePurl(Locale locale, HttpServletRequest request) {
        String purlPath = retrievePurlPathFromRequest(request);
        User u = purlAccess.retrieveUserFromRequest(request);
        Purl p = purlDAO.retrievePurl(purlPath).get();
        if (p == null) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.delete", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.path", new Object[] { purlPath },
                    locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        Optional<Domain> d = domainDAO.retrieveDomain(p);
        if (d.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.delete", null, locale),
                List.of(messages.getMessage("purl_server.error.purl.domain", new Object[] { purlPath },
                    locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            if (purlAccess.canModifyPurl(d.get(), u) == false) {
                PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED,
                    messages.getMessage("purl_server.error.purl.delete", null, locale),
                    List.of(messages.getMessage("purl_server.error.user.delete.unauthorized",
                        new Object[] { u.getFullname() }, locale)));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.UNAUTHORIZED);
            }
        }
        purlDAO.deletePurl(p, u);
        return new ResponseEntity<Purl>(HttpStatus.OK);
    }

    private String retrievePurlPathFromRequest(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getRequestURI().indexOf("/api/purl/") + 9);
    }
}
