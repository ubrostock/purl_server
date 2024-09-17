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
package de.uni.rostock.ub.purl_server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

@Service
public class DomainDAO {
    private static final String SQL_SELECT_DOMAIN_BY_ID = "SELECT * FROM domain WHERE id = ?;";

    private static final String SQL_SELECT_DOMAIN_BY_PATH = "SELECT * FROM domain WHERE path = ?;";

    private static final String SQL_SELECT_DOMAIN_WITH_USER_BY_ID = "SELECT u.*, du.* FROM user u JOIN domainuser du ON u.id = du.user_id WHERE du.domain_id = ?;";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Retrieve a domain by their path
     * @param path
     * @return the retrieved domain
     */
    public Optional<Domain> retrieveDomain(String path) {
        try {
            return Optional
                .of(jdbcTemplate.queryForObject(SQL_SELECT_DOMAIN_BY_PATH, new DomainRowMapper(), path));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieve a domain with user by their path
     * @param id
     * @return the retrieved domain with user
     */
    public Optional<Domain> retrieveDomainWithUser(String path) {
        try {
            Domain d = jdbcTemplate.queryForObject(SQL_SELECT_DOMAIN_BY_PATH, new DomainRowMapper(), path);
            if (d != null) {
                List<DomainUser> list = jdbcTemplate.query(
                    SQL_SELECT_DOMAIN_WITH_USER_BY_ID,
                    new DomainUserRowMapper(), d.getId());
                d.getDomainUserList().addAll(list);
                return Optional.of(d);
            }
        } catch (DataAccessException e) {
            //do nothing
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
            return Optional
                .of(jdbcTemplate.queryForObject(SQL_SELECT_DOMAIN_BY_ID, new DomainRowMapper(), id));
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieve a domain with user by their id
     * @param id
     * @return the retrieved domain with user
     */
    public Optional<Domain> retrieveDomainWithUser(int id) {
        try {
            Domain d = jdbcTemplate.queryForObject(SQL_SELECT_DOMAIN_BY_ID, new DomainRowMapper(), id);
            if (d != null) {
                List<DomainUser> list = jdbcTemplate.query(
                    SQL_SELECT_DOMAIN_WITH_USER_BY_ID,
                    new DomainUserRowMapper(), d.getId());
                d.getDomainUserList().addAll(list);
                return Optional.of(d);
            }
        } catch (DataAccessException e) {
            //do nothing
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
    public List<Domain> searchDomains(String path, String name, String login, boolean isTombstoned, int limit) {
        String paramPath = StringUtils.hasText(path) ? "%" + path + "%" : "%";
        String paramName = StringUtils.hasText(name) ? "%" + name + "%" : "%";
        String paramLogin = StringUtils.hasText(login) ? "%" + login + "%" : "%";
        String paramStatus = isTombstoned ? "CREATED,MODIFIED,DELETED" : "CREATED,MODIFIED";

        List<Domain> domainList = Collections.emptyList();
        if (StringUtils.hasText(login)) {
            domainList = jdbcTemplate.query("SELECT d.* FROM domain d, user u, domainuser du "
                + " WHERE d.id = du.domain_id AND u.id = du.user_id AND (d.path LIKE ?) "
                + " AND INSTR(?, d.status) > 0"
                + " AND (d.name LIKE ?) AND (u.login LIKE ?) GROUP BY d.id ORDER BY d.path LIMIT ?;",
                new DomainRowMapper(), paramPath, paramStatus, paramName, paramLogin, limit);
        } else {
            domainList = jdbcTemplate.query("SELECT * FROM domain d "
                + " WHERE (d.path LIKE ?) "
                + " AND INSTR(?, d.status) > 0"
                + " AND (d.name LIKE ?) ORDER BY d.path LIMIT ?;", new DomainRowMapper(), paramPath, paramStatus,
                paramName, limit);
        }

        for (Domain d : domainList) {
            List<DomainUser> list = jdbcTemplate.query(
                SQL_SELECT_DOMAIN_WITH_USER_BY_ID,
                new DomainUserRowMapper(), d.getId());
            d.getDomainUserList().addAll(list);
        }
        return domainList;
    }

    /**
     * Create a domain
     * @param domain
     */
    public Optional<Domain> createDomain(Domain domain) {
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
        Number key = domainHolder.getKey();
        if (key != null) {
            domain.setId(key.intValue());
        }

        jdbcTemplate.update("DELETE FROM domainuser WHERE domain_id = ?", domain.getId());
        for (DomainUser du : domain.getDomainUserList()) {
            Integer userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE login = ?;", Integer.class,
                du.getUser().getLogin());
            if (userId != null) {
                jdbcTemplate.update(
                    "INSERT INTO domainuser (user_id, domain_id, can_create, can_modify) VALUES(?,?,?,?);",
                    userId, domain.getId(), du.isCanCreate(), du.isCanModify());
            }
        }
        return retrieveDomain(domain.getPath());
    }

    /**
     * Modify a domain
     * Does not update path, because the domain form does not submit disabled inputs.
     * Path can not be modified.
     * @param domain
     */
    public Optional<Domain> modifyDomain(Domain domain) {
        jdbcTemplate.update(
            "UPDATE domain SET name = ?, comment = ?, lastmodified = NOW(3), status = 2 WHERE id = ?;",
            domain.getName(), domain.getComment(), domain.getId());
        jdbcTemplate.update("DELETE FROM domainuser WHERE domain_id = ?", domain.getId());
        for (DomainUser du : domain.getDomainUserList()) {
            Integer userId = jdbcTemplate.queryForObject("SELECT id FROM user WHERE login = ?;", Integer.class,
                du.getUser().getLogin());
            if (userId != null) {
                jdbcTemplate.update(
                    "INSERT INTO domainuser (user_id, domain_id, can_create, can_modify) VALUES(?,?,?,?);",
                    userId, domain.getId(), du.isCanCreate(), du.isCanModify());
            }
        }
        return retrieveDomain(domain.getPath());
    }

    /**
     * Delete a domain
     * @param domain
     */
    public void deleteDomain(Domain domain) {
        jdbcTemplate.update("UPDATE domain SET lastmodified = NOW(3), status = ? WHERE id = ?", Status.DELETED.name(),
            domain.getId());
    }
}
