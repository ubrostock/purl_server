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

import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.User;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setAdmin(rs.getBoolean("admin"));
        u.setFullname(rs.getString("fullname"));
        u.setAffiliation(rs.getString("affiliation"));
        u.setEmail(rs.getString("email"));
        u.setLogin(rs.getString("login"));
        u.setPasswordSHA(rs.getString("password_sha"));
        u.setComment(rs.getString("comment"));
        u.setCreated(rs.getTimestamp("created").toLocalDateTime());
        u.setLastmodified(rs.getTimestamp("lastmodified").toLocalDateTime());
        u.setStatus(Status.valueOf(rs.getString("status")));
        return u;
    }
}
