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
package de.uni.rostock.ub.purl_server.validate;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.User;

@Service
public class UserValidateService {

    private static final String SHA_EMPTY_STRING = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    @Autowired
    UserDAO userDAO;

    @Autowired
    MessageSource messages;

    /**
     * Validate the user
     * 
     * @param user
     * @return the error list
     */
    public PurlServerError validateUser(User user, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.user.create", null, locale), null);
        cleanUp(user);
        if (userDAO.retrieveUser(user.getLogin()).isPresent()) {
            pse.getDetails().add(messages.getMessage("purl_server.error.validate.user.create.exists",
                new Object[] { user.getLogin() }, locale));
        }
        if (SHA_EMPTY_STRING.equals(user.getPasswordSHA())) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.user.create.password.empty", null, locale));
        }
        if (!StringUtils.hasText(user.getLogin())) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.user.create.username.empty", null, locale));
        }

        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    public PurlServerError validateModifyUser(User u, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.user.modify", null, locale), null);
        cleanUp(u);

        userDAO.retrieveUser(u.getLogin()).ifPresentOrElse(uu -> {
        }, () -> {
            pse.getDetails().add(messages.getMessage("purl_server.error.validate.domain.create_modify.user",
                new Object[] { String.valueOf(u.getLogin()) }, locale));
        });

        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    private void cleanUp(User u) {
        u.setAffiliation(u.getAffiliation() == null ? null : u.getAffiliation().strip());
        u.setComment(u.getComment() == null ? null : u.getComment().strip());
        u.setEmail(u.getEmail() == null ? null : u.getEmail().strip());
        u.setFullname(u.getFullname() == null ? null : u.getFullname().strip());
        u.setLogin(u.getLogin() == null ? null : u.getLogin().strip());
    }

}
