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
package de.uni.rostock.ub.purl_server.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import de.uni.rostock.ub.purl_server.model.DomainUser;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.User;

public class DomainUserRowMapper implements RowMapper<DomainUser> {
    @Override
    public DomainUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        DomainUser du = new DomainUser();
        
        User u = new User();
        u.setId(rs.getInt("u.id"));
        u.setAdmin(rs.getBoolean("u.admin"));
        u.setFullname(rs.getString("u.fullname"));
        u.setAffiliation(rs.getString("u.affiliation"));
        u.setEmail(rs.getString("u.email"));
        u.setLogin(rs.getString("u.login"));
        u.setPasswordSHA(rs.getString("u.password_sha"));
        u.setComment(rs.getString("u.comment"));
        u.setCreated(rs.getTimestamp("u.created").toInstant());
        u.setLastmodified(rs.getTimestamp("u.lastmodified").toInstant());
        u.setStatus(Status.valueOf(rs.getString("u.status")));
        
        du.setUser(u);
        du.setId(rs.getInt("du.id"));
        du.setCanCreate(rs.getBoolean("du.can_create"));
        du.setCanModify(rs.getBoolean("du.can_modify"));

        return du;
    }
}
