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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.PurlServerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "domain", description = "Domain API")
@RestController
public class APIDomainController {
    @Autowired
    UserDAO userDAO;

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    private MessageSource messages;

    @GetMapping(path = "/api/domain/{path}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the domain")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = Domain.class))),
        @ApiResponse(responseCode = "404",
            description = "Not Found! A domain with the given path does not exist.",
            content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<? extends PurlServerResponse> retrieveDomain(@PathVariable("path") String path,
        Locale locale) {
        Optional<Domain> oDomain = domainDAO.retrieveDomain(StringUtils.prependIfMissing(path, "/"));
        if (oDomain.isEmpty()) {
            PurlServerError e = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.api.domain.notfound", null, locale),
                List.of());
            return new ResponseEntity<PurlServerError>(e, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Domain>(oDomain.get(), HttpStatus.OK);
    }
}
