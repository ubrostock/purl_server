/*
 * Copyright 2020 University Library, 18051 Rostock, Germany
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

import de.uni.rostock.ub.purl_server.model.PurlHistory;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.Type;

public class PurlHistoryRowMapper implements RowMapper<PurlHistory> {
		@Override
		public PurlHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
			PurlHistory pH = new PurlHistory();
			pH.setId(rs.getInt("id"));
			pH.setPurlId(rs.getInt("purl_id"));
			pH.setUserId(rs.getInt("user_id"));
			pH.setUser(rs.getString("login"));
			pH.setType(Type.valueOf(rs.getString("type")));
			pH.setTarget(rs.getString("target"));
			pH.setLastmodified(rs.getTimestamp("modified").toLocalDateTime());
			pH.setStatus(Status.valueOf(rs.getString("status")));
			return pH;
		}
}
