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

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.UserValidateService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AdminUserController {
    private static final String MODEL_ATTRIBUTE_USERS = "users";

    private static final String MODEL_ATTRIBUTE_DELETED = "deleted";

    private static final String MODEL_ATTRIBUTE_MORE_RESULTS = "moreResults";

    private static final String MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_USER = "searchTombstonedUser";

    private static final String MODEL_ATTRIBUTE_SEARCH_LOGIN = "searchLogin";

    private static final String MODEL_ATTRIBUTE_SEARCH_E_MAIL_ADDRESS = "searchEMailAddress";

    private static final String MODEL_ATTRIBUTE_SEARCH_AFFILIATION = "searchAffiliation";

    private static final String MODEL_ATTRIBUTE_SEARCH_FULL_NAME = "searchFullName";

    private static final String MODEL_ATTRIBUTE_SUBMITTED = "submitted";

    private static final String MODEL_ATTRIBUTE_ERROR = "error";

    private static final String MODEL_ATTRIBUTE_USER = "user";

    private static final String MODEL_ATTRIBUTE_FORM = "form";

    private static final int LIMIT = 50;

    @Autowired
    PurlAccess purlAccess;

    @Autowired
    UserValidateService userValidateService;

    @Autowired
    UserDAO userDAO;

    @Autowired
    MessageSource messages;

    /**
     * Show the user create page
     * 
     * @param model
     * @return the user create page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/admin/manager/user/create")
    public String showUserCreate(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, PurlServerError.createErrorOk());
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "create");
        model.addAttribute(MODEL_ATTRIBUTE_USER, new User());
        return "usercreate";
    }

    /**
     * Create a User
     * 
     * @param user
     * @param model
     * @return the user create page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/user/create")
    public String createUser(@ModelAttribute User user, HttpServletRequest request, Locale locale, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "create");
        PurlServerError pse = userValidateService.validateUser(user, locale);
        if (pse.isOk()) {
            userDAO.createUser(user);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);

        } else {
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, pse);
        model.addAttribute(MODEL_ATTRIBUTE_USER, user);
        return "usercreate";
    }

    /**
     * Show the user modify page
     * 
     * @param id
     * @param model
     * @return the user modify page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/admin/manager/user/modify")
    public String showUserModify(@RequestParam("id") int id, Locale locale, Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "modify");
        userDAO.retrieveUser(id).ifPresentOrElse(u -> {
            model.addAttribute(MODEL_ATTRIBUTE_USER, u);
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, PurlServerError.createErrorOk());
        }, () -> {
            PurlServerError pse = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.api.user.modify", null, locale), List.of(
                    messages.getMessage("purl_server.error.validate.domain.create_modify.user",
                        new Object[] { String.valueOf(id) }, locale)));
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, pse);
        });

        return "usermodify";
    }

    /**
     * Modify a user
     * 
     * @param user
     * @param model
     * @return the user modify page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/user/modify")
    public String modifyUser(@ModelAttribute User user, HttpServletRequest request, Locale locale, Model model) {
        PurlServerError pse = userValidateService.validateModifyUser(user, locale);
        if (pse.isOk()) {
            userDAO.modifyUser(user);
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, true);
        } else {
            model.addAttribute(MODEL_ATTRIBUTE_SUBMITTED, false);
        }
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, PurlServerError.createErrorOk());
        model.addAttribute(MODEL_ATTRIBUTE_FORM, "modify");
        model.addAttribute(MODEL_ATTRIBUTE_USER, userDAO.retrieveUser(user.getId()).get());
        return "usermodify";
    }

    /**
     * Show the user search page
     * 
     * @return the user search page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/admin/manager/user/search")
    public String showUserSearch(Model model) {
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_FULL_NAME, "");
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_AFFILIATION, "");
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_E_MAIL_ADDRESS, "");
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_LOGIN, "");
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_USER, false);
        return "usersearch";
    }

    /**
     * Search a user
     * 
     * @param fullName
     * @param affiliation
     * @param email
     * @param login
     * @param isTombstoned
     * @param model
     * @return the user search page with the model addtribute "users"
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/user/search")
    protected String userSearch(
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_FULL_NAME, required = false, defaultValue = "") String fullName,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_AFFILIATION,
            required = false,
            defaultValue = "") String affiliation,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_E_MAIL_ADDRESS, required = false, defaultValue = "") String email,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_LOGIN, required = false, defaultValue = "") String login,
        @RequestParam(value = MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_USER,
            required = false,
            defaultValue = "false") boolean isTombstoned,
        Model model) {
        List<User> userList = userDAO.searchUsers(login, fullName, affiliation, email, isTombstoned, LIMIT + 1);
        model.addAttribute(MODEL_ATTRIBUTE_MORE_RESULTS, false);
        if (userList.size() == LIMIT + 1) {
            userList.remove(LIMIT);
            model.addAttribute(MODEL_ATTRIBUTE_MORE_RESULTS, true);
        }
        model.addAttribute(MODEL_ATTRIBUTE_USERS, userList);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_FULL_NAME, fullName);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_AFFILIATION, affiliation);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_E_MAIL_ADDRESS, email);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_LOGIN, login);
        model.addAttribute(MODEL_ATTRIBUTE_SEARCH_TOMBSTONED_USER, isTombstoned);
        return "usersearch";
    }

    /**
     * Show the user delete page
     * 
     * @param id
     * @param model
     * @return the user delete page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(path = "/admin/manager/user/delete")
    public String showUserDelete(@RequestParam("id") int id, Locale locale, Model model) {
        userDAO.retrieveUser(id).ifPresentOrElse(u -> {
            model.addAttribute(MODEL_ATTRIBUTE_USER, u);
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, PurlServerError.createErrorOk());
        }, () -> {
            PurlServerError pse = new PurlServerError(HttpStatus.NOT_FOUND,
                messages.getMessage("purl_server.error.api.user.delete", null, locale), List.of(
                    messages.getMessage("purl_server.error.validate.user.create.exists",
                        new Object[] { String.valueOf(id) }, locale)));
            model.addAttribute(MODEL_ATTRIBUTE_ERROR, pse);
        });
        return "userdelete";
    }

    /**
     * Delete a user
     * 
     * @param user
     * @param model
     * @return the user delete page
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/admin/manager/user/delete")
    public String deleteUser(@ModelAttribute User user, HttpServletRequest request, Model model) {
        PurlServerError pse = PurlServerError.createErrorOk();
        userDAO.deleteUser(user);
        User dbUser = userDAO.retrieveUser(user.getId()).get();
        model.addAttribute(MODEL_ATTRIBUTE_DELETED, true);
        model.addAttribute(MODEL_ATTRIBUTE_USER, dbUser);
        model.addAttribute(MODEL_ATTRIBUTE_ERROR, pse);
        return "userdelete";
    }

}
