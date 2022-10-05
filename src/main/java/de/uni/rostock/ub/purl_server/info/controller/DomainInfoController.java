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
package de.uni.rostock.ub.purl_server.info.controller;

import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "domain info", description = "Public domain information")
@RestController
public class DomainInfoController {

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    private MessageSource messages;

    @GetMapping(path = "/info/domain/**",
        produces = "!"
            + MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the information from a domain")
    public Object retrieveInfoDomain(HttpServletRequest request, @RequestParam(defaultValue = "") String format) {
        if ("json".equals(format)) {
            return retrieveJSONDomain(request);
        } else {
            ModelAndView mav = new ModelAndView("domaininfo");
            String domainPath = retrieveDomainPathFromRequest(request);
            Optional<Domain> op = domainDAO.retrieveDomainWithUser(domainPath);
            if (op.isPresent()) {
                mav.addObject("domain", op.get());
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    messages.getMessage("purl_server.error.domain.notfound", null, "Domain not found!",
                        Locale.getDefault()));
            }
            return mav;
        }
    }

    @GetMapping(path = "/info/domain/**", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the information from a domain as json")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "OK",
                     content = @Content(schema = @Schema(implementation = Domain.class))),
        @ApiResponse(responseCode = "404",
                     description = "Not Found! A domain with the given path does not exist.",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Domain> retrieveJSONDomain(HttpServletRequest request) {
        String domainPath = retrieveDomainPathFromRequest(request);
        Optional<Domain> op = domainDAO.retrieveDomainWithUser(domainPath);
        if (op.isEmpty()) {
            return new ResponseEntity<Domain>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Domain>(op.get(), HttpStatus.OK);
    }

    private String retrieveDomainPathFromRequest(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getRequestURI().indexOf("/info/domain/") + 12);
    }

}
