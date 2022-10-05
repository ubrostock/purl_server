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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Type;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "purl info", description = "Public PURL information")
@RestController
public class PurlInfoController {

    @Autowired
    PurlDAO purlDAO;

    @Autowired
    private MessageSource messages;

    @GetMapping(path = "/info/purl/**",
        produces = "!"
            + MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the information from a PURL")
    public Object retrieveInfoPurl(HttpServletRequest request, @RequestParam(defaultValue = "") String format) {
        if ("json".equals(format)) {
            return retrieveJSONPurl(request);
        } else {
            ModelAndView mav = new ModelAndView("purlinfo");
            String purlPath = retrievePurlPathFromRequest(request);
            Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
            if (op.isPresent()) {
                mav.addObject("purl", op.get());
                mav.addObject("purl_url",
                    ServletUriComponentsBuilder.fromCurrentContextPath().path(purlPath).build().toString());
                if (op.get().getType() == Type.PARTIAL_302) {
                    mav.addObject("purl_target_suffix", purlPath.substring(op.get().getPath().length()));
                }
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    messages.getMessage("purl_server.error.purl.notfound", null, "Not found!", Locale.getDefault()));
            }
            return mav;
        }
    }
    
    @GetMapping(path = "/info/purl/**", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the information from a domain as json")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                     description = "OK",
                     content = @Content(schema = @Schema(implementation = Purl.class))),
        @ApiResponse(responseCode = "404",
                     description = "Not Found! The PURL does not exist.",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    public ResponseEntity<Purl> retrieveJSONPurl(HttpServletRequest request) {
        String purlPath = retrievePurlPathFromRequest(request);
        Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
        if (op.isEmpty()) {
            return new ResponseEntity<Purl>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Purl>(op.get(), HttpStatus.OK);
    }

    private String retrievePurlPathFromRequest(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getRequestURI().indexOf("/info/purl/") + 10);
    }

}
