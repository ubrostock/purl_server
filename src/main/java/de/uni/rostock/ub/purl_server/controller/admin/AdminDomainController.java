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
package de.uni.rostock.ub.purl_server.controller.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.DomainUser;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.DomainValidateService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AdminDomainController {
    private static final int LIMIT = 50;

    private static final String MODEL_ATTRIBUTE_CREATED = "created";

    private static final String MODEL_ATTRIBUTE_DELETED = "deleted";

    private static final String MODEL_ATTRIBUTE_DOMAIN = "domain";

    private static final String MODEL_ATTRIBUTE_DOMAINS = "domains";

    private static final String MODEL_ATTRIBUTE_ERROR = "error";

    private static final String MODEL_ATTRIBUTE_FORM = "form";

    private static final String MODEL_ATTRIBUTE_MORE_RESULTS = "moreResults";

    private static final String MODEL_ATTRIBUTE_SEARCH_NAME = "searchName";

    private static final String MODEL_ATTRIBUTE_SEARCH_PATH = "searchPath";

    private static final String MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_DOMAIN = "searchTombstonedDomain";

    private static final String MODEL_ATTRIBUTE_SEARCH_USER = "searchUser";

    private static final String MODEL_ATTRIBUTE_SUBMITTED = "submitted";

    private static final String MODEL_ATTRIBUTE_USERS_LOGIN = "usersLogin";

    private static final String MODEL_VALUE_FORM_CREATE = "create";

    private static final String MODEL_VALUE_FORM_MODIFY = "modify";

    private static final String MODEL_VIEW_DOMAINCREATE = "domaincreate";

    private static final String MODEL_VIEW_DOMAINDELETE = "domaindelete";

    private static final String MODEL_VIEW_DOMAINMODIFY = "domainmodify";

    private static final String MODEL_VIEW_DOMAINSEARCH = "domainsearch";

    @Autowired
    PurlAccess purlAccess;

    @Autowired
    DomainValidateService domainValidateService;

    @Autowired
    UserDAO userDAO;

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    private MessageSource messages;

    /**
     * Show the domain create page
     * 
     * @param model
     * @return the domain create page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/admin/manager/domain/create")
    public String showDomainCreate(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, PurlServerError.createErrorOk());
        model.addAttribute(MODEL_ATTRIBUTE_FORM, MODEL_VALUE_FORM_CREATE);
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, new Domain());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return MODEL_VIEW_DOMAINCREATE;
    }

    /**
     * Create a Domain
     * 
     * @param domain
     * @param model
     * @return the domain create page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/domain/create", params = "submit")
    public String createDomain(@ModelAttribute Domain domain, HttpServletRequest request, Locale locale, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, MODEL_VALUE_FORM_CREATE);
        cleanUpDomain(domain);
        PurlServerError pse = domainValidateService.validateCreateDomain(domain, locale);
        if (pse.isOk()) {
            Optional<Domain> newDomain = domainDAO.createDomain(domain);
            if (newDomain.isPresent()) {
                model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, newDomain.get());
            } else {
                model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
            }
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);
            model.addAttribute(MODEL_ATTRIBUTE_CREATED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, pse);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return MODEL_VIEW_DOMAINCREATE;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/domain/create", params = "addUser")
    public String createDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, PurlServerError.createErrorOk());
        model.addAttribute(MODEL_ATTRIBUTE_FORM, MODEL_VALUE_FORM_CREATE);
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
        List<User> list = userDAO.retrieveActiveUsers();
        list.add(new User());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, list);
        return MODEL_VIEW_DOMAINCREATE;
    }

    /**
     * Show the domain modify page
     * 
     * @param id
     * @param model
     * @return the domain modify page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/admin/manager/domain/modify")
    public String showDomainModify(@RequestParam("id") int id, Locale locale, Model model) {
        PurlServerError pse = PurlServerError.createErrorOk();
        model.addAttribute(MODEL_ATTRIBUTE_FORM, MODEL_VALUE_FORM_MODIFY);
        domainDAO.retrieveDomainWithUser(id).ifPresentOrElse(d -> {
            model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, d);
        }, () -> {
            pse.getDetails().add(messages.getMessage("purl_server.error.validate.domain.modify.exist", new Object[] { String.valueOf(id) }, locale));
        });
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, pse);
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return MODEL_VIEW_DOMAINMODIFY;
    }

    /**
     * Modify a domain
     * 
     * @param domain
     * @param model
     * @return the domain modify page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/domain/modify", params = "submit")
    public String modifyDomain(@ModelAttribute Domain domain, HttpServletRequest request, Locale locale, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, MODEL_VALUE_FORM_MODIFY);
        cleanUpDomain(domain);
        PurlServerError pse = domainValidateService.validateModifyDomain(domain, locale);
        if (pse.isOk()) {
            domainDAO.modifyDomain(domain);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, pse);
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domainDAO.retrieveDomainWithUser(domain.getId()).get());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return MODEL_VIEW_DOMAINMODIFY;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/domain/modify", params = "addUser")
    public String modifyDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, PurlServerError.createErrorOk());
        model.addAttribute(MODEL_ATTRIBUTE_FORM, MODEL_VALUE_FORM_MODIFY);
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
        List<User> list = userDAO.retrieveActiveUsers();
        list.add(new User());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, list);
        return MODEL_VIEW_DOMAINMODIFY;
    }

    /**
     * Show the domain search page
     * 
     * @return the domain search page
     */
    @GetMapping(path = "/admin/manager/domain/search")
    public String showDomainSearch(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_PATH, "");
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_NAME, "");
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_USER, "");
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_DOMAIN, false);
        return MODEL_VIEW_DOMAINSEARCH;
    }

    /**
     * Search domains
     * 
     * @param path
     * @param name
     * @param login
     * @param isTombstoned
     * @param model
     * @return the domain search page with the model addtribute "domains"
     */
    @PostMapping(path = "/admin/manager/domain/search")
    public String domainSearch(
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_PATH, required = false, defaultValue = "") String path,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_NAME, required = false, defaultValue = "") String name,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_USER, required = false, defaultValue = "") String login,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_DOMAIN,
            required = false,
            defaultValue = "false") boolean isTombstoned,
        Model model) {
        List<Domain> domainList = domainDAO.searchDomains(path, name, login, isTombstoned, LIMIT + 1);
        model.addAttribute(MODEL_ATTRIBUTE_MORE_RESULTS, false);
        if (domainList.size() == LIMIT + 1) {
            domainList.remove(LIMIT);
            model.addAttribute(MODEL_ATTRIBUTE_MORE_RESULTS, true);
        }
        model.addAttribute(MODEL_ATTRIBUTE_DOMAINS, domainList);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_PATH, path);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_NAME, name);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_USER, login);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_DOMAIN, isTombstoned);
        return MODEL_VIEW_DOMAINSEARCH;
    }

    /**
     * Show the domain delete page
     * 
     * @return the domain delete page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/admin/manager/domain/delete")
    public String showDomainDelete(@RequestParam("id") int id, Locale locale, Model model) {
        domainDAO.retrieveDomain(id).ifPresentOrElse(d -> {
            model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, d);
        }, () -> {
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, List.of(
                messages.getMessage("purl_server.error.validate.domain.modify.exist", new Object[] { String.valueOf(id) }, locale)));
        });
        return MODEL_VIEW_DOMAINDELETE;
    }

    /**
     * Delete a domain
     * 
     * @param domain
     * @param model
     * @return the domain delete page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/domain/delete")
    public String deleteDomain(@ModelAttribute Domain domain, HttpServletRequest request, Locale locale, Model model) {
        List<String> errorList = domainValidateService.validateDeleteDomain(domain, locale);
        if (errorList.isEmpty()) {
            domainDAO.deleteDomain(domain);
            model.addAttribute(MODEL_ATTRIBUTE_DELETED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, errorList);
            model.addAttribute(MODEL_ATTRIBUTE_DELETED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domainDAO.retrieveDomain(domain.getId()).get());
        return MODEL_VIEW_DOMAINDELETE;
    }

    /**
     * Clean up the domain Remove users without a login name
     * 
     * @param domain
     */
    private void cleanUpDomain(Domain domain) {
        Iterator<DomainUser> it = domain.getDomainUserList().iterator();
        while (it.hasNext()) {
            DomainUser du = it.next();
            if (!StringUtils.hasText(du.getUser().getLogin())) {
                it.remove();
            }
        }
    }
}
