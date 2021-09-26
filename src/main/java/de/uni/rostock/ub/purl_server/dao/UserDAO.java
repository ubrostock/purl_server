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

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
	public List<User> searchUsers(String login, String fullName, String affiliation, String email, boolean isTombstoned) {
		String paramFullName = "%";
		if(StringUtils.hasText(fullName)) {
			paramFullName = "%" + fullName + "%";
		}
		String paramAffiliation = "%";
		if(StringUtils.hasText(affiliation)) {
			paramAffiliation = "%" + affiliation + "%";
		}
		String paramEmail = "%";
		if(StringUtils.hasText(email)) {
			paramEmail = "%" + email + "%";
		}
		String paramLogin = "%";
		if(StringUtils.hasText(login)) {
			paramLogin = "%" + login + "%";
		}
	    String sqlStatus = " AND (status='CREATED' OR status='MODIFIED')";
	    if(isTombstoned) {
	            sqlStatus = "";
	    }
		return jdbcTemplate.query("SELECT * FROM user WHERE (login LIKE ?) AND (fullname LIKE ?) AND (affiliation LIKE ?) AND (email LIKE ?)"
		    + sqlStatus
		    + " ORDER BY login LIMIT 50;", new UserRowMapper(), paramLogin, paramFullName, paramAffiliation, paramEmail);
	}
	
	/**
	 * List all currently active Users.
	 * @return list of users
	 */
	public List<User> retrieveActiveUsers() {
	    return jdbcTemplate.query("SELECT * FROM user WHERE (status='CREATED' OR status='MODIFIED') ORDER BY login;",
            new UserRowMapper());
	}

	/**
	 * Create a user
	 * @param userObject
	 */
	public void createUser(User userObject, User u) {
			jdbcTemplate.update(
					"INSERT INTO user (login, password_sha, admin, fullname, affiliation, email, comment, created, lastmodified, status) VALUES (?,?,?,?,   ?,?,?,NOW(),NOW(),   ?)",
					userObject.getLogin(), userObject.getPasswordSHA(), userObject.isAdmin(), userObject.getFullname(), userObject.getAffiliation(), userObject.getEmail(),
					userObject.getComment(), Status.CREATED.name());
	}

	/**
	 * Modify a user
	 * @param userObject
	 */
	public void modifyUser(User userObject, User u) {
			jdbcTemplate.update(
					"UPDATE user SET login = ?, fullname = ?, affiliation = ?, email = ?, comment = ?, lastmodified = NOW(), status = ? WHERE id = ?;",
					userObject.getLogin(), userObject.getFullname(), userObject.getAffiliation(), userObject.getEmail(), userObject.getComment(), Status.MODIFIED.name() ,userObject.getId());
	}

	/**
	 * Delete a user
	 * 
	 * @param id
	 */
	public void deleteUser(User userObject, User u) {
		jdbcTemplate.update("UPDATE user SET lastmodified = NOW(), status = ? WHERE id = ?;", Status.DELETED.name(), userObject.getId());
	}
}
