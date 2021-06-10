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
package de.uni.rostock.ub.purl_server.admin.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.common.PurlValidate;
import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.User;

@Controller
public class AdminPurlController {
    @Autowired
    PurlAccess purlAccess;
    
    @Autowired
    PurlValidate purlValidate;

    @Autowired
    PurlDAO purlDAO;

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    private MessageSource messages;
    
    /**
     * Show the purl create page
     * 
     * @param model
     * @return the purl create page
     */
    @RequestMapping(path = "/admin/manager/purl/create", method = RequestMethod.GET)
    public String showPurlCreate(Model model) {
        model.addAttribute("purl", new Purl());
        return "purlcreate";
    }

    /**
     * Create a purl
     * 
     * @param purl
     * @param model
     * @return the purl create page
     */
    @RequestMapping(path = "/admin/manager/purl/create", method = RequestMethod.POST)
    public String createPurl(@ModelAttribute Purl purl, HttpServletRequest request, Model model) {
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = purlValidate.validateCreatePurl(purl, u);
        if (errorList.isEmpty()) {
            Optional<Purl> newPurl = purlDAO.createPurl(purl, u);
            if(newPurl.isPresent()) {
                model.addAttribute("purl", newPurl.get());
            } else {
                model.addAttribute("purl", purl); 
            }
            model.addAttribute("created", true);
        } else {
            model.addAttribute("purl", purl); 
            model.addAttribute("errors", errorList);
            model.addAttribute("created", false);
        }
        return "purlcreate";
    }

    /**
     * Show the purl modify page
     * 
     * @param id
     * @param model
     * @return the purl modify page
     */
    @RequestMapping(path = "/admin/manager/purl/modify", method = RequestMethod.GET)
    public String showPurlModify(@RequestParam("id") int id, Model model) {
        model.addAttribute("purl", purlDAO.retrievePurl(id).get());
        return "purlmodify";
    }

    /**
     * Modify a purl
     * 
     * @param purl
     * @param model
     * @return the purl modify page
     */
    @RequestMapping(path = "/admin/manager/purl/modify", method = RequestMethod.POST)
    public String modifyPurl(@ModelAttribute Purl purl, HttpServletRequest request, Model model) {
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = purlValidate.validateModifyPurl(purl, u);
        if (errorList.isEmpty()) {
            purlDAO.modifyPurl(purl, u);
            model.addAttribute("created", true);
        } else {
            model.addAttribute("errors", errorList);
            model.addAttribute("created", false);
        }
        model.addAttribute("purl", purl);
        return "purlmodify";
    }

    /**
     * Show the purl search page
     * 
     * @return the purl search page
     */
    @RequestMapping(path = "/admin/manager/purl/search", method = RequestMethod.GET)
    public String showPurlSearch(Model model) {
        model.addAttribute("path", "");
        model.addAttribute("targetURL", "");
        model.addAttribute("tombstoned", "");
        return "purlsearch";
    }

    /**
     * Search purls
     * 
     * @param path
     * @param url
     * @param isTombstoned
     * @param model
     * @return the purl search page with the model addtribute "purls"
     */
    @RequestMapping(path = "/admin/manager/purl/search", method = RequestMethod.POST)
    protected String purlSearch(@RequestParam(value = "path", required = false, defaultValue = "") String path,
        @RequestParam(value = "targetURL", required = false, defaultValue = "") String url,
        @RequestParam(value = "tombstoned", required = false, defaultValue = "false") Boolean isTombstoned,
        Model model) {
        model.addAttribute("purls", purlDAO.searchPurls(path, url, isTombstoned));
        model.addAttribute("path", path);
        model.addAttribute("targetURL", url);
        model.addAttribute("tombstoned", isTombstoned);
        return "purlsearch";
    }

    /**
     * Show the purl delete page
     * 
     * @param id
     * @param model
     * @return the purl delete page
    **/
    @RequestMapping(path = "/admin/manager/purl/delete", method = RequestMethod.GET)
    public String showPurlDelete(@RequestParam("id") int id, Model model) {
        model.addAttribute("purl", purlDAO.retrievePurl(id).get());
        return "purldelete";
    }

    /**
     * Delete a purl
     * 
     * @param purl
     * @param model
     * @return the purl delete page
     */
    @RequestMapping(path = "/admin/manager/purl/delete", method = RequestMethod.POST)
    public String deletePurl(@ModelAttribute Purl purl, HttpServletRequest request, Model model) {
        User u = purlAccess.retrieveCurrentUser();
        Purl deletePurl = purlDAO.retrievePurl(purl.getPath()).get();
        domainDAO.retrieveDomain(deletePurl).ifPresent(d -> {
            if (purlAccess.canModifyPurl(d, u)) {
                purlDAO.deletePurl(deletePurl, u);
                model.addAttribute("deleted", true);
                model.addAttribute("purl", deletePurl);
            } else {
                List<String> errorList = new ArrayList<>();
                errorList.add(messages.getMessage("purl_server.error.user.delete.purl.unauthorized", null, Locale.getDefault()));
                model.addAttribute("errors", errorList);
            }
        });
        // TODO Was passiert im Fehlerfall, Domain not present.
        return "purldelete";
    }
}
