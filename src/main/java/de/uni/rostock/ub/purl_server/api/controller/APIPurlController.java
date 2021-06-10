/*
 * Copyright 2020 University Library, 18051 Rostock, Germany
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.common.PurlValidate;
import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.PurlServerResponse;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.User;

@Controller
public class APIPurlController {
    @Autowired
    PurlAccess purlAccess;

    @Autowired
    PurlValidate purlValidate;
    
    @Autowired
    PurlDAO purlDAO;

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    UserDAO userDAO;
    
    @Autowired
    private MessageSource messages;

    /**
     * Retrieve and modify PURLs
     * 
     * @statuscode 200 if the purl was found
     * @statuscode 404 if the purl does not exist
     * @return the ResponseEntity object with the retrieved purl include the purl
     *         history
     */
    @RequestMapping(path = "/api/purl/**", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Purl> retrievePurl(HttpServletRequest request) {
        String purlPath = "/" + new AntPathMatcher().extractPathWithinPattern("/api/purl/**", request.getRequestURI());
        Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
        if (op.isEmpty()) {
            return new ResponseEntity<Purl>(HttpStatus.NOT_FOUND);
        }
        ResponseEntity<Purl> r = new ResponseEntity<Purl>(op.get(), HttpStatus.OK);
        return r;
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
    @RequestMapping(path = "/api/purl/**", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<? extends PurlServerResponse> createPurl(@RequestBody Purl inputPurl,
        HttpServletRequest request) {
        String purlPath = "/" + new AntPathMatcher().extractPathWithinPattern("/api/purl/**", request.getRequestURI());
        if (inputPurl == null) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND.value(), messages.getMessage("purl_server.error.purl.create", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.input.empty", null, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        inputPurl.setPath(purlPath);
        User u = purlAccess.retrieveUserFromRequest(request);
        List<String> errorList = purlValidate.validateCreatePurl(inputPurl, u);
        if (!errorList.isEmpty()) {
            // TODO Fehlerliste ausgeben als JSON
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT.value(), messages.getMessage("purl_server.error.purl.create", null, Locale.getDefault()),
                errorList);
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }

        Optional<Domain> d = domainDAO.retrieveDomain(inputPurl);
        if (d.isPresent()) {
            if (purlAccess.canCreatePurl(d.get(), u) == false) {
                PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED.value(),
                    messages.getMessage("purl_server.error.purl.create", null, Locale.getDefault()),
                    List.of(messages.getMessage("purl_server.error.purl.create", new Object[]{u.getFullname()}, Locale.getDefault())));
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
    @RequestMapping(path = "/api/purl/**", method = RequestMethod.PUT)
    public ResponseEntity<? extends PurlServerResponse> modifyPurl(@RequestBody Purl inputPurl,
        HttpServletRequest request) {
        String purlPath = "/" + new AntPathMatcher().extractPathWithinPattern("/api/purl/**", request.getRequestURI());
        if (inputPurl == null) {
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT.value(), messages.getMessage("purl_server.error.purl.update", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.input.empty", null, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
        inputPurl.setPath(purlPath);
        User u = purlAccess.retrieveUserFromRequest(request);
        List<String> errorList = purlValidate.validateModifyPurl(inputPurl, u);
        if (!errorList.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT.value(), messages.getMessage("purl_server.error.purl.update", null, Locale.getDefault()),
                errorList);
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
        Purl p = purlDAO.retrievePurl(purlPath).get();
        if (p == null) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND.value(), messages.getMessage("purl_server.error.purl.update", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.path", new Object[] {purlPath}, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        if (StringUtils.isEmpty(inputPurl.getType())) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND.value(), messages.getMessage("purl_server.error.purl.update", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.type", null, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            p.setType(inputPurl.getType());
        }
        if (p.getStatus() == Status.DELETED) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND.value(), messages.getMessage("purl_server.error.purl.update", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.deleted", null, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        if (StringUtils.isEmpty(inputPurl.getTarget())) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND.value(), messages.getMessage("purl_server.error.purl.update", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.target.empty", null, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            p.setTarget(inputPurl.getTarget());
        }
        Optional<Domain> d = domainDAO.retrieveDomain(inputPurl);
        if (d.isPresent()) {
            if (purlAccess.canModifyPurl(d.get(), u) == false) {
                PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED.value(),
                    messages.getMessage("purl_server.error.purl.create", null, Locale.getDefault()),
                    List.of(messages.getMessage("purl_server.error.user.create.unauthorized", new Object[] {u.getFullname()}, Locale.getDefault())));
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
    @RequestMapping(path = "/api/purl/**", method = RequestMethod.DELETE)
    public ResponseEntity<? extends PurlServerResponse> deletePurl(HttpServletRequest request) {
        String purlPath = "/" + new AntPathMatcher().extractPathWithinPattern("/api/purl/**", request.getRequestURI());
        User u = purlAccess.retrieveUserFromRequest(request);
        Purl p = purlDAO.retrievePurl(purlPath).get();
        if (p == null) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND.value(), messages.getMessage("purl_server.error.purl.delete", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.path", new Object[] {purlPath}, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        Optional<Domain> d = domainDAO.retrieveDomain(p);
        if (d.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND.value(), messages.getMessage("purl_server.error.purl.delete", null, Locale.getDefault()),
                List.of(messages.getMessage("purl_server.error.purl.domain", new Object[] {purlPath}, Locale.getDefault())));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            if (purlAccess.canModifyPurl(d.get(), u) == false) {
                PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED.value(),
                    messages.getMessage("purl_server.error.purl.delete", null, Locale.getDefault()),
                    List.of(messages.getMessage("purl_server.error.user.delete.unauthorized", new Object[] {u.getFullname()}, Locale.getDefault())));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.UNAUTHORIZED);
            }
        }
        purlDAO.deletePurl(p, u);
        return new ResponseEntity<Purl>(HttpStatus.OK);
    }
}
