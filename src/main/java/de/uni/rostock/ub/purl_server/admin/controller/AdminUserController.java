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

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
	@Autowired
	PurlAccess purlAccess;
	
	@Autowired
	UserValidateService userValidateService;
	
	@Autowired
	UserDAO userDAO;

	/**
	 * Show the user create page
	 * 
	 * @param model
	 * @return the user create page
	 */
	@RequestMapping(path = "/admin/manager/user/create", method = RequestMethod.GET)
	public String showUserCreate(Model model) {
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
	@RequestMapping(path = "/admin/manager/user/create", method = RequestMethod.POST)
	public String createUser(@ModelAttribute User user, HttpServletRequest request, Model model) {
		User loginUser = purlAccess.retrieveCurrentUser();
		List<String> errorList = userValidateService.validateUser(user);
		if (loginUser.isAdmin()) {
			try {

			} catch (DuplicateKeyException e) {
				errorList.add("user exist in database");
			}
		} else {
			errorList.add("Not allowed to create user!");
		}
		if (errorList.isEmpty()) {
			userDAO.createUser(user, loginUser);
			model.addAttribute("created", true);

		} else {
			model.addAttribute("errors", errorList);
			model.addAttribute("created", false);
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
	@RequestMapping(path = "/admin/manager/user/modify", method = RequestMethod.GET)
	public String showUserModify(@RequestParam("id") int id, Model model) {
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
	@RequestMapping(path = "/admin/manager/user/modify", method = RequestMethod.POST)
	public String modifyUser(@ModelAttribute User user, HttpServletRequest request, Model model) {
		User loginUser = purlAccess.retrieveCurrentUser();
		List<String> errorList = userValidateService.validateUser(user);
		if (loginUser.isAdmin() || loginUser.getLogin().equals(user.getLogin())) {
			if (errorList.isEmpty()) {
				userDAO.modifyUser(user, loginUser);
				model.addAttribute("created", true);
			}
		} else {
			errorList.add("Not allowed to modify user!");
		}
		if (!errorList.isEmpty()) {
			model.addAttribute("errors", errorList);
			model.addAttribute("created", false);
		}
		model.addAttribute("user", user);
		return "usermodify";
	}

	/**
	 * Show the user search page
	 * 
	 * @return the user search page
	 */
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
	@RequestMapping(path = "/admin/manager/user/search", method = RequestMethod.POST)
	protected String userSearch(
			@RequestParam(value = "searchFullName", required = false, defaultValue = "") String fullName,
			@RequestParam(value = "searchAffiliation", required = false, defaultValue = "") String affiliation,
			@RequestParam(value = "searchEMailAddress", required = false, defaultValue = "") String email,
			@RequestParam(value = "searchLogin", required = false, defaultValue = "") String login,
			@RequestParam(value = "searchTombstonedUser", required = false, defaultValue = "false") Boolean isTombstoned,
			Model model) {
		model.addAttribute("users", userDAO.searchUsers(fullName, affiliation, email, login, isTombstoned));
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

	@RequestMapping(path = "/admin/manager/user/delete", method = RequestMethod.POST)
	public String deleteUser(@ModelAttribute User user, HttpServletRequest request, Model model) {
		User loginUser = purlAccess.retrieveCurrentUser();
		if (loginUser.isAdmin()) {
			userDAO.deleteUser(user, loginUser);
			model.addAttribute("deleted", true);
			model.addAttribute("user", user);
		} else {
			List<String> errorList = new ArrayList<>();
			errorList.add("Not allowed to delete user!");
			model.addAttribute("errors", errorList);
		}
		return "userdelete";
	}

	
}
