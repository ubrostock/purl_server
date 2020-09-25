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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.dao.mapper.DomainRowMapper;
import de.uni.rostock.ub.purl_server.dao.mapper.DomainUserRowMapper;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.DomainUser;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.User;

@Service
public class DomainDAO {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static Logger LOGGER = LoggerFactory.getLogger(DomainDAO.class);
	/**
	 * Retrieve a domain by their path
	 * @param path
	 * @return the retrieved domain
	 */
	public Optional<Domain> retrieveDomain(String path) {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM domain WHERE path = ?;", new DomainRowMapper(), path));
		} catch (DataAccessException e) {
			LOGGER.debug("Error retrieving domain from database!", e);
		}
		return Optional.empty();
	}
	public Optional<Domain> retrieveDomain(Purl p) {
			return retrieveDomain(p.getDomainPath());
	}
	/**
	 * Retrieve a domain by their id
	 * @param id
	 * @return the retrieved domain
	 */
	public Optional<Domain> retrieveDomain(int id) {
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM domain WHERE id = ?;", new DomainRowMapper(), id));
		} catch (DataAccessException e) {
			LOGGER.error("Error retrieving domain from database!", e);
		}
		return Optional.empty();
	}
	
	/**
	 * Retrieve a domain with user by their id
	 * @param id
	 * @return the retrieved domain with user
	 */
	public Optional<Domain> retrieveDomainWithUser(int id) {
		try {
			Domain d = jdbcTemplate.queryForObject("SELECT * FROM domain WHERE id = ?;", new DomainRowMapper(), id);
			List<DomainUser> list = jdbcTemplate.query("SELECT u.*, du.* FROM user u JOIN domainuser du ON u.id = du.user_id WHERE du.domain_id = ?;", new Object[] {d.getId()}, new DomainUserRowMapper());
			d.getDomainUserList().addAll(list);
			return Optional.of(d);
		} catch (DataAccessException e) {
			LOGGER.error("Error retrieving domain from database!", e);
		}
		return Optional.empty();
	}

	/**
	 * Search domains
	 * @param path
	 * @param name
	 * @param login
	 * @param isTombstoned
	 * @return a list of founded domains
	 */
	public List<Domain> searchDomains(String path, String name, String login, Boolean isTombstoned) {
		String paramPath = "%";
		if(!StringUtils.isEmpty(path)) {
			paramPath = "%" + path + "%";
		}
		String paramName = "%";
		if(!StringUtils.isEmpty(name)) {
			paramName = "%" + name + "%";
		}
		String paramLogin = "%";
		if(!StringUtils.isEmpty(login)) {
			paramLogin = "%" + login + "%";
		}
		int paramStatus = 3;
		if(isTombstoned) {
			paramStatus = 10;
		}
		List<Domain> domainList = jdbcTemplate.query("SELECT d.*, u.* FROM user u, domain d, domainuser du WHERE du.user_id = du.domain_id AND (path LIKE ?) AND (d.status < ?) AND (name LIKE ?) AND (login LIKE ?) GROUP BY d.id ORDER BY d.path LIMIT 50;", new DomainRowMapper(), paramPath, paramStatus, paramName,paramLogin );
		for(Domain d: domainList){
		List<DomainUser> list = jdbcTemplate.query("SELECT u.*, du.* FROM user u JOIN domainuser du ON u.id = du.user_id WHERE du.domain_id = ?;", new Object[] {d.getId()}, new DomainUserRowMapper());
		d.getDomainUserList().addAll(list);
		}
		return domainList;
	}

	/**
	 * Create a domain
	 * @param domain
	 */
	public void createDomain(Domain domain, User u) {
		if(u.isAdmin()) {
			KeyHolder domainHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {
				@Override
				public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
					PreparedStatement ps = con.prepareStatement(
							"INSERT INTO domain (id, path, name, comment, created, lastmodified, status) VALUES(?,?,?,?,?   ,?,?);",
							Statement.RETURN_GENERATED_KEYS);
					ps.setNull(1, Types.INTEGER);
					ps.setString(2, domain.getPath());
					ps.setString(3, domain.getName());
					ps.setString(4, domain.getComment());
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					ps.setTimestamp(5, timestamp);
					ps.setTimestamp(6, timestamp);
					ps.setInt(7, 1);
					return ps;
				}
			}, domainHolder);
			domain.setId(domainHolder.getKey().intValue());

			jdbcTemplate.update("DELETE FROM domainuser WHERE domain_id = ?", domain.getId());
			for (DomainUser du : domain.getDomainUserList()) {
				int userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE login = ?;", Integer.class, du.getUser().getLogin());
				jdbcTemplate.update("INSERT INTO domainuser (user_id, domain_id, can_create, can_modify) VALUES(?,?,?,?);", userId, domain.getId(),
						du.isCanCreate(), du.isCanModify());
			}
		} else {
			LOGGER.error("User not allowed to create domains!");
		}
		
	}

	/**
	 * Modify a domain
	 * @param domain
	 */
	public void modifyDomain(Domain domain, User u) {
		if(u.isAdmin()) {
			jdbcTemplate.update("UPDATE domain SET path = ?, name = ?, comment = ?, lastmodified = NOW(), status = 2 WHERE id = ?;", domain.getPath(),
					domain.getName(), domain.getComment(), domain.getId());
			jdbcTemplate.update("DELETE FROM domainuser WHERE domain_id = ?", domain.getId());
			for (DomainUser du : domain.getDomainUserList()) {
				int userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE login = ?;", Integer.class, du.getUser().getLogin());
				jdbcTemplate.update("INSERT INTO domainuser (user_id, domain_id, can_create, can_modify) VALUES(?,?,?,?);", userId, domain.getId(),
						du.isCanCreate(), du.isCanModify());
			}
		} else {
			LOGGER.error("User not allowed to modify domains!");
		}
	}
	
	/**
	 * Delete a domain
	 * @param domain
	 */
	public void deleteDomain(String path, User u) {
		if(u.isAdmin()) {
			jdbcTemplate.update("UPDATE domain SET lastmodified = NOW(), status = ? WHERE path = ?", Status.DELETED.name(), path);
		} else {
			LOGGER.error("User not allowed to delete domains!");
		}
		
	}
}
