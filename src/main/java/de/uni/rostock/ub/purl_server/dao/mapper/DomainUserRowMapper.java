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
    public DomainUser mapRow(ResultSet rs3, int rowNum) throws SQLException {
        DomainUser du = new DomainUser();
        
        User u = new User();
        u.setId(rs3.getInt("user.id"));
        u.setAdmin(rs3.getBoolean("user.admin"));
        u.setFullname(rs3.getString("user.fullname"));
        u.setAffiliation(rs3.getString("user.affiliation"));
        u.setEmail(rs3.getString("user.email"));
        u.setLogin(rs3.getString("user.login"));
        u.setPasswordSHA(rs3.getString("user.password_sha"));
        u.setComment(rs3.getString("user.comment"));
        u.setCreated(rs3.getTimestamp("user.created").toInstant());
        u.setLastmodified(rs3.getTimestamp("user.lastmodified").toInstant());
        u.setStatus(Status.valueOf(rs3.getString("user.status")));
        
        du.setUser(u);
        du.setId(rs3.getInt("domainuser.id"));
        du.setCanCreate(rs3.getBoolean("domainuser.can_create"));
        du.setCanModify(rs3.getBoolean("domainuser.can_modify"));

        return du;
    }
}
