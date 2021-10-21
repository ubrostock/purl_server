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

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.User;

@Controller
public class APIUserController {
    @Autowired
    UserDAO userDAO;

    /**
     * Retrieve the user by login
     * 
     * @param login
     * @param request
     * @statuscode 200 if the user was found
     * @statuscode 404 if the user does not exist
     * @return the ResponseEntity object with the retrieved user
     */
    @RequestMapping("/api/user/{login}")
    public ResponseEntity<User> retrieveUser(@PathVariable("login") String login, HttpServletRequest request) {
        Optional<User> ou = userDAO.retrieveUser(login);
        if (ou.isEmpty()) {
            return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", request.getRequestURL().append("/").append(ou.get().getId()).toString());
        ResponseEntity<User> r = new ResponseEntity<User>(ou.get(), headers, HttpStatus.OK);
        return r;
    }
}
