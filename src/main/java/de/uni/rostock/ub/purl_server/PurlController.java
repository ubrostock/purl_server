/*
 * Copyright 2020 University Library, 18051 Rostock, Germany
 *
 * This file is part of the application "PURL Server".
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
package de.uni.rostock.ub.purl_server;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Type;

@Controller
public class PurlController {

    @Autowired
    PurlDAO purlDAO;

    @Autowired
    private MessageSource messages;
    
    private static Logger LOGGER = LoggerFactory.getLogger(PurlController.class);
    
    /**
     * Resolve the Purl
     * 
     * @param request the HttpServletRequest
     * @param resp    the HttpServletResponse
     * @param domain  the PathVariable
     * @return redirect to the target URL
     */
    @RequestMapping(path = "/{domain:(?!admin)(?!api)(?!webjars).*}/**", method = RequestMethod.GET)
    public String resolvePurl(HttpServletRequest request, HttpServletResponse resp, @PathVariable String domain) {
        String path = request.getServletPath();
        Optional<Purl> op = purlDAO.retrievePurl(path);
        if (op.isPresent()) {
            Purl p = op.get();
            if (p.getType() == Type.PARTIAL) {
                String restPath = path.substring(p.getPath().length());
                return "redirect:" + p.getTarget() + restPath;
            }
            return "redirect:" + p.getTarget();
        } else {
            try {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Purl not found!");
            } catch (IOException e) {
                LOGGER.error(messages.getMessage("purl_server.error.sending.error", null, Locale.getDefault()), e);
            }
            return null;
        }
    }
}