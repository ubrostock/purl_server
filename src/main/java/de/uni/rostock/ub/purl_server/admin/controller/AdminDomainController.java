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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
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
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.DomainValidateService;

@Controller
public class AdminDomainController {
    private static final String MODEL_ATTRIBUTE_CREATED = "created";

    private static final String MODEL_ATTRIBUTE_SUBMITTED = "submitted";

    private static final String MODEL_ATTRIBUTE_MORE_RESULTS = "moreResults";

    private static final String MODEL_ATTRIBUTE_ERRORS = "errors";

    private static final String MODEL_ATTRIBUTE_DELETED = "deleted";

    private static final String MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_DOMAIN = "searchTombstonedDomain";

    private static final String MODEL_ATTRIBUTE_SEARCH_USER = "searchUser";

    private static final String MODEL_ATTRIBUTE_SEARCH_NAME = "searchName";

    private static final String MODEL_ATTRIBUTE_SEARCH_PATH = "searchPath";

    private static final String MODEL_ATTRIBUTE_DOMAINS = "domains";

    private static final String MODEL_ATTRIBUTE_USERS_LOGIN = "usersLogin";

    private static final String MODEL_ATTRIBUTE_DOMAIN = "domain";

    private static final String MODEL_ATTRIBUTE_FORM = "form";

    private static int LIMIT = 50;

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
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "create");
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, new Domain());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return "domaincreate";
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
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "create");
        cleanUpDomain(domain);
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = domainValidateService.validateCreateDomain(domain, u, locale);
        if (errorList.isEmpty()) {
            Optional<Domain> newDomain = domainDAO.createDomain(domain, u);
            if (newDomain.isPresent()) {
                model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, newDomain.get());
            } else {
                model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
            }
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);
            model.addAttribute(MODEL_ATTRIBUTE_CREATED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
            model.addAttribute(MODEL_ATTRIBUTE_ERRORS, errorList);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return "domaincreate";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/domain/create", params = "addUser")
    public String createDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "create");
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
        List<User> list = userDAO.retrieveActiveUsers();
        list.add(new User());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, list);
        return "domaincreate";
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
    public String showDomainModify(@RequestParam("id") int id, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "modify");
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domainDAO.retrieveDomainWithUser(id).get());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return "domainmodify";
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
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "modify");
        cleanUpDomain(domain);
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = domainValidateService.validateModifyDomain(domain, u, locale);
        if (errorList.isEmpty()) {
            domainDAO.modifyDomain(domain, u);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_ERRORS, errorList);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, userDAO.retrieveActiveUsers());
        return "domainmodify";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/domain/modify", params = "addUser")
    public String modifyDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "modify");
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
        List<User> list = userDAO.retrieveActiveUsers();
        list.add(new User());
        model.addAttribute(MODEL_ATTRIBUTE_USERS_LOGIN, list);
        return "domainmodify";
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
        return "domainsearch";
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
    public String domainSearch(@RequestParam(value = MODEL_ATTRIBUTE_SEARCH_PATH, required = false, defaultValue = "") String path,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_NAME, required = false, defaultValue = "") String name,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_USER, required = false, defaultValue = "") String login,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_DOMAIN, required = false, defaultValue = "false") boolean isTombstoned,
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
        return "domainsearch";
    }

    /**
     * Show the domain delete page
     * 
     * @return the domain delete page
     */
    @GetMapping(path = "/admin/manager/domain/delete")
    public String showDomainDelete(@RequestParam("id") int id, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domainDAO.retrieveDomain(id).get());
        return "domaindelete";
    }

    /**
     * Delete a domain
     * 
     * @param domain
     * @param model
     * @return the domain delete page
     */
    @PostMapping(path = "/admin/manager/domain/delete")
    public String deleteDomain(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        User u = purlAccess.retrieveCurrentUser();
        domainDAO.retrieveDomain(domain.getPath()).ifPresent(d -> {
            if (purlAccess.canModifyPurl(d, u)) {
                domainDAO.deleteDomain(domain.getPath(), u);
                model.addAttribute(MODEL_ATTRIBUTE_DELETED, true);
                model.addAttribute(MODEL_ATTRIBUTE_DOMAIN, domain);
            } else {
                List<String> errorList = new ArrayList<>();
                errorList.add(messages.getMessage("purl_server.error.domain.delete", null, Locale.getDefault()));
                model.addAttribute(MODEL_ATTRIBUTE_ERRORS, errorList);
            }
        });
        return "domaindelete";
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
