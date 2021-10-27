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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
    @RequestMapping(path = "/admin/manager/domain/create", method = RequestMethod.GET)
    public String showDomainCreate(Model model) {
        model.addAttribute("form", "create");
        model.addAttribute("domain", new Domain());
        model.addAttribute("usersLogin", userDAO.retrieveActiveUsers());
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
    @RequestMapping(path = "/admin/manager/domain/create", method = RequestMethod.POST, params = "submit")
    public String createDomain(@ModelAttribute Domain domain, HttpServletRequest request, Locale locale, Model model) {
        model.addAttribute("form", "create");
        cleanUpDomain(domain);
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = domainValidateService.validateCreateDomain(domain, u, locale);
        if (errorList.isEmpty()) {
            Optional<Domain> newDomain = domainDAO.createDomain(domain, u);
            if (newDomain.isPresent()) {
                model.addAttribute("domain", newDomain.get());
            } else {
                model.addAttribute("domain", domain);
            }
            model.addAttribute("submitted", true);
            model.addAttribute("created", true);
        } else {
            model.addAttribute("domain", domain);
            model.addAttribute("errors", errorList);
            model.addAttribute("submitted", false);
        }
        model.addAttribute("usersLogin", userDAO.retrieveActiveUsers());
        return "domaincreate";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(path = "/admin/manager/domain/create", method = RequestMethod.POST, params = "addUser")
    public String createDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute("form", "create");
        model.addAttribute("domain", domain);
        List<User> list = userDAO.retrieveActiveUsers();
        list.add(new User());
        model.addAttribute("usersLogin", list);
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
    @RequestMapping(path = "/admin/manager/domain/modify", method = RequestMethod.GET)
    public String showDomainModify(@RequestParam("id") int id, Model model) {
        model.addAttribute("form", "modify");
        model.addAttribute("domain", domainDAO.retrieveDomainWithUser(id).get());
        model.addAttribute("usersLogin", userDAO.retrieveActiveUsers());
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
    @RequestMapping(path = "/admin/manager/domain/modify", method = RequestMethod.POST, params = "submit")
    public String modifyDomain(@ModelAttribute Domain domain, HttpServletRequest request, Locale locale, Model model) {
        model.addAttribute("form", "modify");
        cleanUpDomain(domain);
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = domainValidateService.validateModifyDomain(domain, u, locale);
        if (errorList.isEmpty()) {
            domainDAO.modifyDomain(domain, u);
            model.addAttribute("submitted", true);
        } else {
            model.addAttribute("errors", errorList);
            model.addAttribute("submitted", false);
        }
        model.addAttribute("domain", domain);
        model.addAttribute("usersLogin", userDAO.retrieveActiveUsers());
        return "domainmodify";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(path = "/admin/manager/domain/modify", method = RequestMethod.POST, params = "addUser")
    public String modifyDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute("form", "modify");
        model.addAttribute("domain", domain);
        List<User> list = userDAO.retrieveActiveUsers();
        list.add(new User());
        model.addAttribute("usersLogin", list);
        return "domainmodify";
    }

    /**
     * Show the domain search page
     * 
     * @return the domain search page
     */
    @RequestMapping(path = "/admin/manager/domain/search", method = RequestMethod.GET)
    public String showDomainSearch(Model model) {
        model.addAttribute("searchPath", "");
        model.addAttribute("searchName", "");
        model.addAttribute("searchUser", "");
        model.addAttribute("searchTombstonedDomain", false);
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
    @RequestMapping(path = "/admin/manager/domain/search", method = RequestMethod.POST)
    public String domainSearch(@RequestParam(value = "searchPath", required = false, defaultValue = "") String path,
        @RequestParam(value = "searchName", required = false, defaultValue = "") String name,
        @RequestParam(value = "searchUser", required = false, defaultValue = "") String login,
        @RequestParam(value = "searchTombstonedDomain", required = false, defaultValue = "false") boolean isTombstoned,
        Model model) {
        List<Domain> domainList = domainDAO.searchDomains(path, name, login, isTombstoned, LIMIT + 1);
        model.addAttribute("moreResults", false);
        if (domainList.size() == LIMIT + 1) {
            domainList.remove(LIMIT);
            model.addAttribute("moreResults", true);
        }
        model.addAttribute("domains", domainList);
        model.addAttribute("searchPath", path);
        model.addAttribute("searchName", name);
        model.addAttribute("searchUser", login);
        model.addAttribute("searchTombstonedDomain", isTombstoned);
        return "domainsearch";
    }

    /**
     * Show the domain delete page
     * 
     * @return the domain delete page
     */
    @RequestMapping(path = "/admin/manager/domain/delete", method = RequestMethod.GET)
    public String showDomainDelete(@RequestParam("id") int id, Model model) {
        model.addAttribute("domain", domainDAO.retrieveDomain(id).get());
        return "domaindelete";
    }

    /**
     * Delete a domain
     * 
     * @param domain
     * @param model
     * @return the domain delete page
     */
    @RequestMapping(path = "/admin/manager/domain/delete", method = RequestMethod.POST)
    public String deleteDomain(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        User u = purlAccess.retrieveCurrentUser();
        domainDAO.retrieveDomain(domain.getPath()).ifPresent(d -> {
            if (purlAccess.canModifyPurl(d, u)) {
                domainDAO.deleteDomain(domain.getPath(), u);
                model.addAttribute("deleted", true);
                model.addAttribute("domain", domain);
            } else {
                List<String> errorList = new ArrayList<>();
                errorList.add(messages.getMessage("purl_server.error.domain.delete", null, Locale.getDefault()));
                model.addAttribute("errors", errorList);
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
