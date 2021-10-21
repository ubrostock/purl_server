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

import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;

@Controller
public class APIDomainController {
    @Autowired
    UserDAO userDAO;;

    @Autowired
    DomainDAO domainDAO;

    /**
     * Retrieve the domain
     * 
     * @statuscode 200 if the domain was found
     * @statuscode 404 if the domain does not exist return the ResponseEntity object
     *             with the retrieved domain
     */
    @RequestMapping("/api/domain/**")
    public ResponseEntity<Domain> retrieveDomain(HttpServletRequest request) {
        String domainName = "/"
            + new AntPathMatcher().extractPathWithinPattern("/api/purl/**", request.getRequestURI());
        AtomicReference<ResponseEntity<Domain>> r = new AtomicReference<>();
        domainDAO.retrieveDomain(domainName).ifPresentOrElse(d -> {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", request.getRequestURL().toString());
            r.set(new ResponseEntity<Domain>(d, headers, HttpStatus.OK));
        }, () -> {
            r.set(new ResponseEntity<Domain>(HttpStatus.NOT_FOUND));
        });

        return r.get();
    }
}
