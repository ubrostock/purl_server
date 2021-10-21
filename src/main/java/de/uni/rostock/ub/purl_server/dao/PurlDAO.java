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
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.mapper.PurlHistoryRowMapper;
import de.uni.rostock.ub.purl_server.dao.mapper.PurlRowMapper;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.PurlHistory;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.Type;
import de.uni.rostock.ub.purl_server.model.User;

@Service
public class PurlDAO {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    PurlAccess purlAccess;

    @Autowired
    private MessageSource messages;

    private static Logger LOGGER = LoggerFactory.getLogger(PurlDAO.class);

    /**
     * Retireve a purl by their path
     * 
     * @param path
     * @return the retrieved purl
     */
    public Optional<Purl> retrievePurl(String path) {
        try {
            Purl p = jdbcTemplate.queryForObject("SELECT * FROM purl WHERE path=?;", new Object[] { path },
                new PurlRowMapper());
            domainDAO.retrieveDomainWithUser(p.getDomainId()).ifPresent(d -> p.setDomain(d));
            return Optional.of(p);
        } catch (DataAccessException e) {
            try {
                Purl p2 = jdbcTemplate.queryForObject(
                    "SELECT * FROM purl p WHERE LOCATE(path, ?) = 1 AND type = 'partial_302' ORDER BY LENGTH(path) DESC LIMIT 1;",
                    new Object[] { path }, new PurlRowMapper());
                return Optional.of(p2);
            } catch (DataAccessException e2) {
                return Optional.empty();
            }
        }
    }

    /**
     * Retrieve a purl by their id
     * 
     * @param id
     * @return the retrieved purl
     */
    public Optional<Purl> retrievePurl(int id) {
        try {
            Purl p = jdbcTemplate.queryForObject("SELECT * FROM purl WHERE id=?;", new Object[] { id },
                new PurlRowMapper());
            domainDAO.retrieveDomain(p.getDomainId()).ifPresent(d -> p.setDomain(d));
            return Optional.of(p);
        } catch (DataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Search purls
     * 
     * @param path
     * @param target
     * @param isTombstoned
     * @return a list of founded purls
     */
    public List<Purl> searchPurls(String path, String target, boolean isTombstoned, int limit) {
        String paramPath = "%";
        if (StringUtils.hasText(path)) {
            paramPath = "%" + path + "%";
        }
        String paramTarget = "%";
        if (StringUtils.hasText(target)) {
            paramTarget = "%" + target + "%";
        }
        int paramStatus = 3;
        if (isTombstoned) {
            paramStatus = 10;
        }
        List<Purl> purlList = jdbcTemplate.query(
            "SELECT * FROM purl WHERE (path LIKE ?) AND (status < ?) AND (target LIKE ?) LIMIT ?;", new PurlRowMapper(),
            paramPath, paramStatus, paramTarget, limit);
        for (Purl p : purlList) {
            domainDAO.retrieveDomain(p.getDomainId()).ifPresent(d -> p.setDomain(d));
        }
        return purlList;
    }

    /**
     * Retrieve a purl with their history by path
     * 
     * @param path
     * @return the retrieved purl with their history
     */
    public Optional<Purl> retrievePurlWithHistory(String path) {
        Optional<Purl> p = retrievePurl(path);
        p.ifPresent(x -> {
            List<PurlHistory> purlHistoryList = jdbcTemplate.query(
                "SELECT ph.*, u.login FROM purlhistory ph JOIN user u ON ph.user_id = u.id WHERE ph.purl_id = ? ORDER BY modified DESC;",
                new PurlHistoryRowMapper(), x.getId());
            x.setPurlHistory(purlHistoryList);
        });
        return p;
    }

    /**
     * Create a purl
     * 
     * @param p
     */
    public Optional<Purl> createPurl(Purl p, User u) {
        int domainId = jdbcTemplate.queryForObject("SELECT id FROM domain WHERE path = ?;", Integer.class,
            p.getDomainPath());
        KeyHolder purlId = new GeneratedKeyHolder();

        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO purl (path, domain_id, type, target, created, lastmodified, status) VALUES(?,?,?,?,NOW(), NOW(),?) ON DUPLICATE KEY UPDATE type = ?, target = ?, lastmodified = NOW(), status = ?;",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, p.getPath());
                ps.setInt(2, domainId);
                ps.setString(3, p.getType().name());
                ps.setString(4, p.getTarget());
                ps.setString(5, Status.CREATED.name());
                ps.setString(6, p.getType().name());
                ps.setString(7, p.getTarget());
                ps.setString(8, Status.CREATED.name());
                return ps;
            }
        }, purlId);
        jdbcTemplate.update(
            "INSERT INTO purlhistory (purl_id, user_id, type, target, modified, status) VALUES(?,?,?,?,NOW(),? );",
            purlId.getKey().intValue(), u.getId(), p.getType().name(),
            p.getTarget(), Status.CREATED.name());

        return retrievePurl(p.getPath());
    }

    /**
     * Modify a purl
     * 
     * @param purl
     */
    public Optional<Purl> modifyPurl(Purl p, User u) {

        jdbcTemplate.update(
            "UPDATE purl SET path = ?, target = ?, lastmodified = NOW(), status = ?, type = ? WHERE id = ?;",
            p.getPath(), p.getTarget(),
            Status.MODIFIED.name(), p.getType().name(), p.getId());
        jdbcTemplate.update(
            "INSERT INTO purlhistory (purl_id, user_id, type, target, modified, status) VALUES(?,?,?,?,NOW(),? );",
            p.getId(), u.getId(), p.getType().name(),
            p.getTarget(), Status.MODIFIED.name());
        return retrievePurl(p.getPath());
    }

    /**
     * Delete a purl
     * 
     * @param path
     */
    public void deletePurl(Purl p, User u) {
        jdbcTemplate.update("UPDATE purl SET lastmodified = NOW(), status = ?, type = ? WHERE path = ?",
            Status.DELETED.name(), Type.GONE_410.name(), p.getPath());
        p.setType(Type.GONE_410);
        jdbcTemplate.update(
            "INSERT INTO purlhistory (purl_id, user_id, type, target, modified, status) VALUES(?,?,?,?NOW(),? );",
            p.getId(), u.getId(), p.getType().name(), p.getTarget(),
            Status.DELETED.name());
    }
}
