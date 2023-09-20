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
package de.uni.rostock.ub.purl_server.controller;

import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.Purl;

@Controller
public class PurlController {

    @Autowired
    PurlDAO purlDAO;

    @Autowired
    private MessageSource messages;

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(PurlController.class);

    /**
     * Resolve the Purl
     * 
     * swagger cannot handle '/' in url parameters
     * there is no option to not url-encode them.
     * https://github.com/OAI/OpenAPI-Specification/issues/892
     * 
     * @param request the HttpServletRequest
     * @param resp    the HttpServletResponse
     * @param domain  the PathVariable
     * @return redirect to the target URL
     */
    @GetMapping(path = "/{domain:" + Domain.REGEX_VALID_DOMAINS + "}/{*path}")
    public String resolvePurl(@PathVariable String domain, @PathVariable("path") String path,
        HttpServletRequest request) {
        String purl = "/" + domain + path;
        Optional<Purl> op = purlDAO.retrievePurl(purl);
        if (op.isPresent()) {
            Purl p = op.get();
            switch (p.getType()) {
                case PARTIAL_302:
                    String restPath = purl.substring(p.getPath().length());
                    return "redirect:" + calcRedirectWithParams(request, p.getTarget(), restPath);
                case REDIRECT_302:
                    return "redirect:" + calcRedirectWithParams(request, p.getTarget(), "");
                case GONE_410:
                    throw new ResponseStatusException(HttpStatus.GONE,
                        messages.getMessage(
                            "purl_server.error.purl_deleted", new Object[] { ServletUriComponentsBuilder
                                .fromCurrentContextPath().path("/info/purl" + path).build().toString() },
                            Locale.getDefault()));
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.purl.notfound", null, Locale.getDefault()));
        }
        return null;
    }

    private String calcRedirectWithParams(HttpServletRequest request, String path, String restPath) {
        String redirect = path + restPath;
        String queryString = request.getQueryString();
        if (StringUtils.hasText(queryString)) {
            if (redirect.contains("?")) {
                redirect = redirect + "&" + queryString;
            } else {
                redirect = redirect + "?" + queryString;
            }
        }
        return redirect;
    }
}
