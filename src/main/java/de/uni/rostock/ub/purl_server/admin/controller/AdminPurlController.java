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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.PurlValidateService;

@Controller
public class AdminPurlController {
    private static final String MODEL_ATTRIBUTE_DELETED = "deleted";

    private static final String MODEL_ATTRIBUTE_PURLS = "purls";

    private static final String MODEL_ATTRIBUTE_MORE_RESULTS = "moreResults";

    private static final String MODEL_ATTRIBUTE_TOMBSTONED = "tombstoned";

    private static final String MODEL_ATTRIBUTE_TARGET_URL = "targetURL";

    private static final String MODEL_ATTRIBUTE_PATH = "path";

    private static final String MODEL_ATTRIBUTE_ERRORS = "errors";

    private static final String MODEL_ATTRIBUTE_SUBMITTED = "submitted";

    private static final String MODEL_ATTRIBUTE_PURL = "purl";

    private static final String MODEL_ATTRIBUTE_FORM = "form";

    private static int LIMIT = 50;

    @Autowired
    PurlAccess purlAccess;

    @Autowired
    PurlValidateService purlValidateService;

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
    @GetMapping(path = "/admin/manager/purl/create")
    public String showPurlCreate(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "create");
        model.addAttribute(MODEL_ATTRIBUTE_PURL, new Purl());
        return "purlcreate";
    }

    /**
     * Create a purl
     * 
     * @param purl
     * @param model
     * @return the purl create page
     */
    @PostMapping(path = "/admin/manager/purl/create")
    public String createPurl(@ModelAttribute Purl purl, HttpServletRequest request, Locale locale, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "create");
        User u = purlAccess.retrieveCurrentUser();
        purl.setPath(purl.getPath().trim());
        List<String> errorList = purlValidateService.validateCreatePurl(purl, u, locale);
        if (errorList.isEmpty()) {
            Optional<Purl> newPurl = purlDAO.createPurl(purl, u);
            if (newPurl.isPresent()) {
                model.addAttribute(MODEL_ATTRIBUTE_PURL, newPurl.get());
            } else {
                model.addAttribute(MODEL_ATTRIBUTE_PURL, purl);
            }
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_PURL, purl);
            model.addAttribute(MODEL_ATTRIBUTE_ERRORS, errorList);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
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
    @GetMapping(path = "/admin/manager/purl/modify")
    public String showPurlModify(@RequestParam("id") int id, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "modify");
        model.addAttribute(MODEL_ATTRIBUTE_PURL, purlDAO.retrievePurl(id).get());
        return "purlmodify";
    }

    /**
     * Modify a purl
     * 
     * @param purl
     * @param model
     * @return the purl modify page
     */
    @PostMapping(path = "/admin/manager/purl/modify")
    public String modifyPurl(@ModelAttribute Purl purl, HttpServletRequest request, Locale locale, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "modify");
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = purlValidateService.validateModifyPurl(purl, u, locale);
        if (errorList.isEmpty()) {
            purlDAO.modifyPurl(purl, u);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_ERRORS, errorList);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_PURL, purl);
        return "purlmodify";
    }

    /**
     * Show the purl search page
     * 
     * @return the purl search page
     */
    @GetMapping(path = "/admin/manager/purl/search")
    public String showPurlSearch(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_PATH, "");
        model.addAttribute(MODEL_ATTRIBUTE_TARGET_URL, "");
        model.addAttribute(MODEL_ATTRIBUTE_TOMBSTONED, false);
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
    @PostMapping(path = "/admin/manager/purl/search")
    protected String purlSearch(@RequestParam(value = MODEL_ATTRIBUTE_PATH, required = false, defaultValue = "") String path,
        @RequestParam(value = MODEL_ATTRIBUTE_TARGET_URL, required = false, defaultValue = "") String url,
        @RequestParam(value = MODEL_ATTRIBUTE_TOMBSTONED, required = false, defaultValue = "false") Boolean isTombstoned,
        Model model) {
        List<Purl> purlList = purlDAO.searchPurls(path, url, isTombstoned, LIMIT + 1);
        model.addAttribute(MODEL_ATTRIBUTE_MORE_RESULTS, false);
        if (purlList.size() == LIMIT + 1) {
            purlList.remove(LIMIT);
            model.addAttribute(MODEL_ATTRIBUTE_MORE_RESULTS, true);
        }
        model.addAttribute(MODEL_ATTRIBUTE_PURLS, purlList);
        model.addAttribute(MODEL_ATTRIBUTE_PATH, path);
        model.addAttribute(MODEL_ATTRIBUTE_TARGET_URL, url);
        model.addAttribute(MODEL_ATTRIBUTE_TOMBSTONED, isTombstoned);
        return "purlsearch";
    }

    /**
     * Show the purl delete page
     * 
     * @param id
     * @param model
     * @return the purl delete page
    **/
    @GetMapping(path = "/admin/manager/purl/delete")
    public String showPurlDelete(@RequestParam("id") int id, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_PURL, purlDAO.retrievePurl(id).get());
        return "purldelete";
    }

    /**
     * Delete a purl
     * 
     * @param purl
     * @param model
     * @return the purl delete page
     */
    @PostMapping(path = "/admin/manager/purl/delete")
    public String deletePurl(@ModelAttribute Purl purl, HttpServletRequest request, Model model) {
        User u = purlAccess.retrieveCurrentUser();
        Purl deletePurl = purlDAO.retrievePurl(purl.getPath()).get();
        domainDAO.retrieveDomain(deletePurl).ifPresent(d -> {
            if (purlAccess.canModifyPurl(d, u)) {
                purlDAO.deletePurl(deletePurl, u);
                model.addAttribute(MODEL_ATTRIBUTE_DELETED, true);
                model.addAttribute(MODEL_ATTRIBUTE_PURL, deletePurl);
            } else {
                List<String> errorList = new ArrayList<>();
                errorList.add(
                    messages.getMessage("purl_server.error.user.delete.purl.unauthorized", null, Locale.getDefault()));
                model.addAttribute(MODEL_ATTRIBUTE_ERRORS, errorList);
            }
        });
        // TODO Was passiert im Fehlerfall, Domain not present.
        return "purldelete";
    }
}
