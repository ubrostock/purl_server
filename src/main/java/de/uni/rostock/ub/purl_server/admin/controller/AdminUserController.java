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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.UserValidateService;

@Controller
public class AdminUserController {
	private static int LIMIT = 50;
	
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
	@RequestMapping(path = "/admin/manager/user/create", method = RequestMethod.GET)
	public String showUserCreate(Model model) {
		model.addAttribute("form", "create");
		model.addAttribute("user", new User());
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
	@RequestMapping(path = "/admin/manager/user/create", method = RequestMethod.POST)
	public String createUser(@ModelAttribute User user, HttpServletRequest request, Model model) {
		model.addAttribute("form", "create");
		if (userDAO.retrieveUser(user.getLogin()).isPresent()) {
			model.addAttribute("errors", Arrays.asList(messages.getMessage("purl_server.error.validate.user.exists",
					new Object[] { user.getLogin() }, Locale.getDefault())));
		} else {
			List<String> errorList = userValidateService.validateUser(user);
			if (errorList.isEmpty()) {
				User loginUser = purlAccess.retrieveCurrentUser();
				userDAO.createUser(user, loginUser);
				model.addAttribute("submitted", true);

			} else {
				model.addAttribute("errors", errorList);
				model.addAttribute("submitted", null);
			}
		}
		model.addAttribute("user", user);
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
	@RequestMapping(path = "/admin/manager/user/modify", method = RequestMethod.GET)
	public String showUserModify(@RequestParam("id") int id, Model model) {
		model.addAttribute("form", "modify");
		model.addAttribute("user", userDAO.retrieveUser(id).get());
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
	@RequestMapping(path = "/admin/manager/user/modify", method = RequestMethod.POST)
	public String modifyUser(@ModelAttribute User user, HttpServletRequest request, Model model) {
		model.addAttribute("form", "modify");
		User loginUser = purlAccess.retrieveCurrentUser();
		List<String> errorList = userValidateService.validateUser(user);
		if (errorList.isEmpty()) {
			userDAO.modifyUser(user, loginUser);
			model.addAttribute("submitted", true);
		} else {
			model.addAttribute("errors", errorList);
			model.addAttribute("submitted", false);
		}
		model.addAttribute("user", user);
		return "usermodify";
	}

	/**
	 * Show the user search page
	 * 
	 * @return the user search page
	 */
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@RequestMapping(path = "/admin/manager/user/search", method = RequestMethod.GET)
	public String showUserSearch(Model model) {
		model.addAttribute("searchFullName", "");
		model.addAttribute("searchAffiliation", "");
		model.addAttribute("searchEMailAddress", "");
		model.addAttribute("searchLogin", "");
		model.addAttribute("searchTombstonedUser", false);
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
	@RequestMapping(path = "/admin/manager/user/search", method = RequestMethod.POST)
	protected String userSearch(
			@RequestParam(value = "searchFullName", required = false, defaultValue = "") String fullName,
			@RequestParam(value = "searchAffiliation", required = false, defaultValue = "") String affiliation,
			@RequestParam(value = "searchEMailAddress", required = false, defaultValue = "") String email,
			@RequestParam(value = "searchLogin", required = false, defaultValue = "") String login,
			@RequestParam(value = "searchTombstonedUser", required = false, defaultValue = "false") boolean isTombstoned,
			Model model) {
		List<User> userList = userDAO.searchUsers(login, fullName, affiliation, email, isTombstoned, LIMIT + 1);
    	model.addAttribute("moreResults", false);
    	if(userList.size() == LIMIT + 1) {
    		userList.remove(LIMIT);
    		model.addAttribute("moreResults", true);
    	}
		model.addAttribute("users", userList);
		model.addAttribute("searchFullName", fullName);
		model.addAttribute("searchAffiliation", affiliation);
		model.addAttribute("searchEMailAddress", email);
		model.addAttribute("searchLogin", login);
		model.addAttribute("searchTombstonedUser", isTombstoned);
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
	@RequestMapping(path = "/admin/manager/user/delete", method = RequestMethod.GET)
	public String showUserDelete(@RequestParam("id") int id, Model model) {
		model.addAttribute("user", userDAO.retrieveUser(id).get());
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
	@RequestMapping(path = "/admin/manager/user/delete", method = RequestMethod.POST)
	public String deleteUser(@ModelAttribute User user, HttpServletRequest request, Model model) {
		User loginUser = purlAccess.retrieveCurrentUser();
		userDAO.deleteUser(user, loginUser);
		model.addAttribute("deleted", true);
		model.addAttribute("user", user);
		return "userdelete";
	}

}
