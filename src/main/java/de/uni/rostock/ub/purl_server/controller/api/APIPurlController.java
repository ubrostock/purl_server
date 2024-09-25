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
package de.uni.rostock.ub.purl_server.controller.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Retrieve and modify PURLs
 */
@Tag(name = "purl", description = "PURL API")
@RestController
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
     * catch-all pattern in REST APIs with access to the captured path segments through a @PathVariable
     * https://spring.io/blog/2020/06/30/url-matching-with-pathpattern-in-spring-mvc#pathpattern
     * 
     * swagger cannot handle '/' in url parameters
     * there is no option to not url-encode them.
     * https://github.com/OAI/OpenAPI-Specification/issues/892
     * 
     * @return the ResponseEntity object with the retrieved purl include the purl
     *         history
     */
    @GetMapping(path = "/api/purl/{*path}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the PURL with PURL history")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Purl.class))),
        @ApiResponse(responseCode = "404",
            description = "Not Found! The PURL does not exist.",
            content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Purl> retrievePurl(@PathVariable("path") String path) {
        Optional<Purl> op = purlDAO.retrievePurlWithHistory(path);
        if (op.isEmpty()) {
            return new ResponseEntity<Purl>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Purl>(op.get(), HttpStatus.OK);
    }

    /**
     * 
     * @return the ResponseEntity object with the retrieved purl include the purl
     *         history
     */
    @GetMapping(path = "/api/purl/{*path}",
        produces = "application/x-java-serialized-object")
    @Operation(summary = "Get the PURL as HashMap with the PURL history")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Purl.class))),
        @ApiResponse(responseCode = "404",
            description = "Not Found! The PURL does not exist.",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500",
            description = "Internal server error.",
            content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Resource> retrievePurlAsHashMap(@PathVariable("path") String path) {
        Optional<Purl> op = purlDAO.retrievePurlWithHistory(path);
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
     * @param inputPurl
     * @return the ResponseEntity object with the created purl
     */
    @PostMapping(path = "/api/purl/{*path}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a PURL")
    @ApiResponses({
        @ApiResponse(responseCode = "201",
            description = "Created",
            content = @Content(schema = @Schema(implementation = Purl.class))),
        @ApiResponse(responseCode = "401",
            description = "User unauthorized to create a PURL",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404",
            description = "Not Found! The PURL does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409",
            description = "PURL already exists",
            content = @Content(schema = @Schema(hidden = true))),
    })
    public ResponseEntity<? extends PurlServerResponse> createPurl(@PathVariable("path") String path,
        @RequestBody Purl inputPurl, Locale locale,
        HttpServletRequest request) {
        String msgErrorPurlCreate = messages.getMessage("purl_server.error.api.purl.create", null, locale);
        if (inputPurl == null) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                msgErrorPurlCreate,
                List.of(messages.getMessage("purl_server.error.api.purl.input.empty", null, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        inputPurl.setPath(path);
        Optional<User> u = purlAccess.retrieveUserFromRequest(request);
        if(u.isPresent()) {
        List<String> errorList = purlValidateService.validateCreatePurl(inputPurl, u.get(), locale);
        if (!errorList.isEmpty()) {
            // TODO Fehlerliste ausgeben als JSON
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                msgErrorPurlCreate, errorList);
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
        }

        Optional<Domain> d = domainDAO.retrieveDomain(inputPurl);
        if (d.isPresent() && u.isPresent()) {
          if(!purlAccess.canCreatePurl(d.get(), u.orElse(null))) {
            PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED,
                msgErrorPurlCreate,
                List.of(messages.getMessage("purl_server.error.api.purl.create.unauthorized",
                    new Object[] { u.get().getFullname() }, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.UNAUTHORIZED);
          }
          inputPurl.setPath(path);
          purlDAO.createPurl(inputPurl, u.get());
        }

        Optional<Purl> oPurl = purlDAO.retrievePurlWithHistory(inputPurl.getPath());
        if (oPurl.isPresent()) {
            return new ResponseEntity<Purl>(oPurl.get(), HttpStatus.CREATED);
        } else {
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                msgErrorPurlCreate, List.of());
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
    }

    /**
     * @param inputPurl
     * @return the ResponseEntity object with the modified purl
     */
    @PutMapping(path = "/api/purl/{*path}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a PURL")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "Modified",
            content = @Content(schema = @Schema(implementation = Purl.class))),
        @ApiResponse(responseCode = "401",
            description = "User unauthorized to modify a PURL!",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404",
            description = "Not Found! The PURL does not exist.",
            content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<? extends PurlServerResponse> modifyPurl(@PathVariable("path") String path,
        @RequestBody Purl inputPurl, Locale locale,
        HttpServletRequest request) {
        String msgErrorPurlUpdate = messages.getMessage("purl_server.error.api.purl.modify", null, locale);
        if (inputPurl == null) {
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                msgErrorPurlUpdate,
                List.of(messages.getMessage("purl_server.error.api.purl.input.empty", null, locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
        inputPurl.setPath(path);
        Optional<User> u = purlAccess.retrieveUserFromRequest(request);
        List<String> errorList = purlValidateService.validateModifyPurl(inputPurl, u.orElse(null), locale);
        if (!errorList.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                msgErrorPurlUpdate,
                errorList);
            return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
        }
        Optional<Purl> oPurl = purlDAO.retrievePurl(path);
        if (oPurl.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                msgErrorPurlUpdate,
                List.of(messages.getMessage("purl_server.error.api.purl.path", new Object[] { path },
                    locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            Purl p = oPurl.get();
            if (ObjectUtils.isEmpty(inputPurl.getType())) {
                PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                    msgErrorPurlUpdate,
                    List.of(messages.getMessage("purl_server.error.api.purl.type", null, locale)));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
            } else {
                p.setType(inputPurl.getType());
            }
            if (p.getStatus() == Status.DELETED) {
                PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                    msgErrorPurlUpdate,
                    List.of(messages.getMessage("purl_server.error.api.purl.deleted", null, locale)));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
            }
            if (!StringUtils.hasText(inputPurl.getTarget())) {
                PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                    msgErrorPurlUpdate,
                    List.of(messages.getMessage("purl_server.error.api.purl.target.empty", null, locale)));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
            } else {
                p.setTarget(inputPurl.getTarget());
            }
            Optional<Domain> d = domainDAO.retrieveDomain(inputPurl);
            if (d.isPresent() && u.isPresent()) {
                if (!purlAccess.canModifyPurl(d.get(), u.get())) {
                    PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED,
                        msgErrorPurlUpdate,
                        List.of(messages.getMessage("purl_server.error.api.purl.modify.unauthorized",
                            new Object[] { u.get().getFullname() }, locale)));
                    return new ResponseEntity<PurlServerError>(e, HttpStatus.UNAUTHORIZED);
                }
                purlDAO.modifyPurl(p, u.get());
            }
            // TODO was passiert, wenn D nicht da ist.
            Optional<Purl> oPurlWithHistory = purlDAO.retrievePurlWithHistory(p.getPath());
            if (oPurlWithHistory.isPresent()) {
                return new ResponseEntity<Purl>(oPurlWithHistory.get(), HttpStatus.OK);
            } else {
                PurlServerError e = new PurlServerError(HttpStatus.CONFLICT,
                    msgErrorPurlUpdate, List.of());
                return new ResponseEntity<PurlServerError>(e, HttpStatus.CONFLICT);
            }

        }
    }

    /**
     * @param inputPurl
     * @return the ResponseEntity
     */
    @DeleteMapping(path = "/api/purl/{*path}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a PURL")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "Deleted",
            content = @Content(schema = @Schema(implementation = Purl.class))),
        @ApiResponse(responseCode = "401",
            description = "User unauthorized to delete a PURL!",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404",
            description = "Not Found! The PURL does not exist.",
            content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<? extends PurlServerResponse> deletePurl(@PathVariable("path") String path, Locale locale,
        HttpServletRequest request) {
        Optional<User> u = purlAccess.retrieveUserFromRequest(request);
        String msgErrorPurlDelete = messages.getMessage("purl_server.error.api.purl.delete", null, locale);

        Optional<Purl> oPurl = purlDAO.retrievePurl(path);
        if (oPurl.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                msgErrorPurlDelete,
                List.of(messages.getMessage("purl_server.error.api.purl.path", new Object[] { path },
                    locale)));
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        } else {
            Purl p = oPurl.get();
            Optional<Domain> d = domainDAO.retrieveDomain(p);
            if (d.isEmpty() || u.isEmpty()) {
                PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                    msgErrorPurlDelete,
                    List.of(messages.getMessage("purl_server.error.api.purl.domain", new Object[] { path },
                        locale)));
                return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
            } else {
                if (!purlAccess.canModifyPurl(d.get(), u.get())) {
                    PurlServerError e = new PurlServerError(HttpStatus.UNAUTHORIZED,
                        msgErrorPurlDelete,
                        List.of(messages.getMessage("purl_server.error.api.purl.delete.unauthorized",
                            new Object[] { u.get().getFullname() }, locale)));
                    return new ResponseEntity<PurlServerError>(e, HttpStatus.UNAUTHORIZED);
                }
            }
            purlDAO.deletePurl(p, u.get());
            return new ResponseEntity<Purl>(HttpStatus.OK);
        }
    }

}
