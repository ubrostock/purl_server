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
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import de.uni.rostock.ub.purl_server.PurlController;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;

@Controller
public class PurlInfoController {

    @Autowired
    PurlDAO purlDAO;

    @Autowired
    private MessageSource messages;

    private static Logger LOGGER = LoggerFactory.getLogger(PurlController.class);

    @RequestMapping(path = "/info/purl/**",
        method = RequestMethod.GET,
        produces = "!"
            + MediaType.APPLICATION_JSON_VALUE)
    public Object retrieveInfoPurl(HttpServletRequest request, @RequestParam(defaultValue = "") String format) {
        if ("json".equals(format)) {
            return retrieveJSONPurl(request);
        } else {
            ModelAndView mav = new ModelAndView("purlinfo");
            String servletContextPath = request.getServletContext().getContextPath();
            String purlPath = "/"
                + new AntPathMatcher().extractPathWithinPattern(servletContextPath + "/info/purl/**",
                    request.getRequestURI());
            Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
            if (op.isPresent()) {
                mav.addObject("purl", op.get());
                mav.addObject("purl_url",
                    ServletUriComponentsBuilder.fromCurrentContextPath().path(purlPath).build().toString());
            } else {
                try {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        messages.getMessage("purl_server.error.purl.notfound", null, Locale.getDefault()));
                } catch (NoSuchMessageException e) {
                    LOGGER.error(messages.getMessage("purl_server.error.sending.error", null, Locale.getDefault()), e);
                }
            }
            return mav;
        }
    }

    @RequestMapping(path = "/info/purl/**", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Purl> retrieveJSONPurl(HttpServletRequest request) {
        String purlPath = "/" + new AntPathMatcher().extractPathWithinPattern("/info/purl/**", request.getRequestURI());
        Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
        if (op.isEmpty()) {
            return new ResponseEntity<Purl>(HttpStatus.NOT_FOUND);
        }
        ResponseEntity<Purl> r = new ResponseEntity<Purl>(op.get(), HttpStatus.OK);
        return r;
    }

}
