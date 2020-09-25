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
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
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

@Controller
public class AdminDomainController {
    @Autowired
    PurlAccess purlAccess;

    @Autowired
    UserDAO userDAO;

    @Autowired
    DomainDAO domainDAO;

    /**
     * Show the domain create page
     * 
     * @param model
     * @return the domain create page
     */
    @RequestMapping(path = "/admin/manager/domain/create", method = RequestMethod.GET)
    public String showDomainCreate(Model model) {
        model.addAttribute("domain", new Domain());
        model.addAttribute("usersLogin", userDAO.searchUsers("", "", "", "", false));
        return "domaincreate";
    }

    /**
     * Create a Domain
     * 
     * @param domain
     * @param model
     * @return the domain create page
     */
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(path = "/admin/manager/domain/create", method = RequestMethod.POST, params = "submit")
    public String createDomain(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        cleanUpDomain(domain);
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = validateCreateDomain(domain, u);
        if (errorList.isEmpty()) {
            domainDAO.createDomain(domain, u);
            model.addAttribute("created", true);
        } else {
            model.addAttribute("errors", errorList);
        }
        model.addAttribute("domain", domain);
        model.addAttribute("usersLogin", userDAO.searchUsers("", "", "", "", false));
        return "domaincreate";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(path = "/admin/manager/domain/create", method = RequestMethod.POST, params = "addUser")
    public String createDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute("domain", domain);
        List<User> list = userDAO.searchUsers("", "", "", "", false);
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
    @RequestMapping(path = "/admin/manager/domain/modify", method = RequestMethod.GET)
    public String showDomainModify(@RequestParam("id") int id, Model model) {
        model.addAttribute("domain", domainDAO.retrieveDomainWithUser(id).get());
        model.addAttribute("usersLogin", userDAO.searchUsers("", "", "", "", false));
        return "domainmodify";
    }

    /**
     * Modify a domain
     * 
     * @param domain
     * @param model
     * @return the domain modify page
     */
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(path = "/admin/manager/domain/modify", method = RequestMethod.POST, params = "submit")
    public String modifyDomain(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        cleanUpDomain(domain);
        User u = purlAccess.retrieveCurrentUser();
        List<String> errorList = validateModifyDomain(domain, u);
        if (errorList.isEmpty()) {
            domainDAO.modifyDomain(domain, u);
            model.addAttribute("created", true);
        } else {
            model.addAttribute("errors", errorList);
        }
        model.addAttribute("domain", domain);
        model.addAttribute("usersLogin", userDAO.searchUsers("", "", "", "", false));
        return "domainmodify";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(path = "/admin/manager/domain/modify", method = RequestMethod.POST, params = "addUser")
    public String modifyDomainAddUser(@ModelAttribute Domain domain, HttpServletRequest request, Model model) {
        model.addAttribute("domain", domain);
        List<User> list = userDAO.searchUsers("", "", "", "", false);
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
        @RequestParam(value = "searchTombstonedDomain", required = false, defaultValue = "false") Boolean isTombstoned,
        Model model) {
        model.addAttribute("domains", domainDAO.searchDomains(path, name, login, isTombstoned));
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
                errorList.add("Not allowed to delete domain!");
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
            if (StringUtils.isEmpty(du.getUser().getLogin())) {
                it.remove();
            }
        }
    }

    public List<String> validateCreateDomain(Domain d, User u) {
        List<String> errorList = new ArrayList<>();
        domainDAO.retrieveDomain(d.getPath()).ifPresentOrElse(dd -> {
            errorList.add("Domain '" + dd.getPath() + "' does already exist!");
        }, () -> {
            errorList.addAll(validateDomain(d));
        });
        return errorList;
    }

    public List<String> validateModifyDomain(Domain d, User u) {
        List<String> errorList = new ArrayList<>();
        domainDAO.retrieveDomain(d.getPath()).ifPresentOrElse(dd -> {
            errorList.addAll(validateDomain(d));
        }, () -> {
            errorList.add("Domain '" + d.getPath() + "' does not exist!");
        });
        return errorList;
    }

    /**
     * Validate the domain
     * 
     * @param domain
     * @return the error list
     */
    private List<String> validateDomain(Domain domain) {
        List<String> errorList = new ArrayList<>();
        if (StringUtils.isEmpty(domain.getPath())) {
            errorList.add("Path can not be empty!");
        } else {
            if (domain.getPath().startsWith("/admin")) {
                errorList.add("Path can not start with \"/admin\"");
            }
            if (!domain.getPath().startsWith("/")) {
                errorList.add("Path must start with '/'!");
            }
            if (!domain.getPath().matches("/[a-zA-Z0-9]+")) {
                errorList.add("Path can only contain a-z, A-Z and 0-9!");
            }
        }
        if (StringUtils.isEmpty(domain.getName())) {
            errorList.add("Name can not be empty!");
        }
        List<String> logins = userDAO.retrieveLogins();
        for (DomainUser du : domain.getDomainUserList()) {
            if (!logins.contains(du.getUser().getLogin())) {
                errorList.add("User login does not exist.");
            }
        }
        return errorList;
    }
}
