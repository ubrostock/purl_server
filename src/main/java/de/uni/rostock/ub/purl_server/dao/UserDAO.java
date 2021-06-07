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
package de.uni.rostock.ub.purl_server.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.dao.mapper.UserRowMapper;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.User;

@Service
public class UserDAO {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private MessageSource messages;

	private static Logger LOGGER = LoggerFactory.getLogger(UserDAO.class);
	/**
	 * Check if login and password are in the database
	 * @param login
	 * @param password
	 * @return true if login and password are in the database
	 */
	public boolean canLogin(String login, String password) {
		try (Formatter formatter = new Formatter();) {
			MessageDigest crypt = MessageDigest.getInstance("SHA-1");
			crypt.reset();
			crypt.update(password.getBytes(StandardCharsets.UTF_8));

			for (byte b : crypt.digest()) {
				formatter.format("%02x", b);
			}
			String result = formatter.toString();
			try {
				jdbcTemplate.queryForObject("SELECT id WHERE login = ? AND password = ? AND status <> 'DELETED';", Integer.class, login, result);
				return true;
			} catch (DataAccessException e) {
				return false;
			}
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error(messages.getMessage("purl_server.error.login", null, Locale.getDefault()));
		}
		return false;
	}

	/**
	 * Retrieve user by their login
	 * @param login
	 * @return the retrieved user
	 */
	public Optional<User> retrieveUser(String login) {
		try {
		    User u = jdbcTemplate.queryForObject("SELECT * FROM user WHERE login = ?;", new UserRowMapper(), login);
			return Optional.of(u);
		}catch(DataAccessException e) {
		    return Optional.empty();
		}
	}

	/**
	 * Retrieve user by their id
	 * @param id
	 * @return the retrieved user
	 */
	public Optional<User> retrieveUser(int id) {
		try {
		    User u = jdbcTemplate.queryForObject("SELECT * FROM user WHERE id = ?;", new UserRowMapper(), id);
			return Optional.of(u);
		} catch(DataAccessException e) {
		    return Optional.empty();
		}
	}

	/**
	 * Retrieve logins
	 * @return a list of all logins
	 */
	public List<String> retrieveLogins() {
		return jdbcTemplate.queryForList("SELECT login FROM user ORDER BY login;", String.class);
	}

	/**
	 * Search users
	 * @param fullName
	 * @param affiliation
	 * @param email
	 * @param login
	 * @param isTombstoned
	 * @return a list of founded users
	 */
	public List<User> searchUsers(String fullName, String affiliation, String email, String login, Boolean isTombstoned) {
		String paramFullName = "%";
		if(!StringUtils.isEmpty(fullName)) {
			paramFullName = "%" + fullName + "%";
		}
		String paramAffiliation = "%";
		if(!StringUtils.isEmpty(affiliation)) {
			paramAffiliation = "%" + affiliation + "%";
		}
		String paramEmail = "%";
		if(!StringUtils.isEmpty(email)) {
			paramEmail = "%" + email + "%";
		}
		String paramLogin = "%";
		if(!StringUtils.isEmpty(login)) {
			paramLogin = "%" + login + "%";
		}
		int paramStatus = 3;
		if(isTombstoned) {
			paramStatus = 10;
		}
		return jdbcTemplate.query("SELECT * FROM user WHERE (fullname LIKE ?) AND (affiliation LIKE ?) AND (email LIKE ?) AND (login LIKE ?) AND (status < ?)  LIMIT 50;", new UserRowMapper(), paramFullName, paramAffiliation, paramEmail, paramLogin, paramStatus);
	}

	/**
	 * Create a user
	 * @param userObject
	 */
	public void createUser(User userObject, User u) {
		if(u.isAdmin()) {
			jdbcTemplate.update(
					"INSERT INTO user (login, password_sha, admin, fullname, affiliation, email, comment, created, lastmodified, status) VALUES (?,?,?,?,   ?,?,?,NOW(),NOW(),   ?)",
					userObject.getLogin(), userObject.getPasswordSHA(), userObject.isAdmin(), userObject.getFullname(), userObject.getAffiliation(), userObject.getEmail(),
					userObject.getComment(), 1);
		} else {
			LOGGER.error(messages.getMessage("purl_server.error.user.create.user.unauthorized", null, Locale.getDefault()));
		}
		
	}

	/**
	 * Modify a user
	 * @param userObject
	 */
	public void modifyUser(User userObject, User u) {
		if(u.isAdmin()) {
			jdbcTemplate.update(
					"UPDATE user SET login = ?, fullname = ?, affiliation = ?, email = ?, comment = ?, lastmodified = NOW(), status = ? WHERE id = ?;",
					userObject.getLogin(), userObject.getFullname(), userObject.getAffiliation(), userObject.getEmail(), userObject.getComment(), Status.MODIFIED.name() ,userObject.getId());
		} else {
			LOGGER.error(messages.getMessage("purl_server.error.user.modify.user.unauthorized", null, Locale.getDefault()));
		}
	}

	/**
	 * Delete a user
	 * 
	 * @param id
	 */
	public void deleteUser(User userObject, User u) {
		if(u.isAdmin()) {
			jdbcTemplate.update("UPDATE user SET lastmodified = NOW(), status = ? WHERE id = ?;", Status.DELETED.name(), userObject.getId());
		} else {
			LOGGER.error(messages.getMessage("purl_server.error.user.delete.user.unauthorized", null, Locale.getDefault()));
		}
	}
}
