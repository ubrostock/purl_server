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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.DomainUser;

@Service
public class DomainValidateService {

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    MessageSource messages;

    public List<String> validateCreateDomain(Domain d, Locale locale) {
        List<String> errorList = new ArrayList<>();
        domainDAO.retrieveDomain(d.getPath()).ifPresentOrElse(dd -> {
            errorList.add(messages.getMessage("purl_server.error.validate.domain.already.exist",
                new Object[] { dd.getPath() }, locale));
        }, () -> {
            errorList.addAll(validatePath(d, locale));
            errorList.addAll(validateDomain(d, locale));
        });
        return errorList;
    }

    public List<String> validateModifyDomain(Domain d, Locale locale) {
        List<String> errorList = new ArrayList<>();
        domainDAO.retrieveDomain(d.getId()).ifPresentOrElse(dd -> {
            errorList.addAll(validateDomain(d, locale));
        }, () -> {
            errorList.add(messages.getMessage("purl_server.error.validate.domain.exist", new Object[] { d.getPath() },
                locale));
        });
        return errorList;
    }

    private List<String> validatePath(Domain domain, Locale locale) {
        if (!StringUtils.hasText(domain.getPath())) {
            return List.of(messages.getMessage("purl_server.error.validate.domain.path.empty", null, locale));
        }

        List<String> errorList = new ArrayList<>();
        if (domain.getPath().startsWith("/admin")) {
            errorList.add(messages.getMessage("purl_server.error.validate.domain.path.start.admin", null, locale));
        } else if (!domain.getPath().matches("/" + Domain.REGEX_VALID_DOMAINS)) {
            errorList.add(
                messages.getMessage("purl_server.error.validate.domain.path.reserved", null, locale));
        }
        if (!domain.getPath().startsWith("/")) {
            errorList.add(
                messages.getMessage("purl_server.error.validate.domain.path.start", null, locale));
        }
        if (!domain.getPath().matches("/[-_a-zA-Z0-9]+")) {
            errorList.add(
                messages.getMessage("purl_server.error.validate.domain.path.match", null, locale));
        }
        return errorList;
    }

    /**
     * Validate the domain
     * 
     * @param domain
     * @return the error list
     */
    private List<String> validateDomain(Domain domain, Locale locale) {
        List<String> errorList = new ArrayList<>();
        if (!StringUtils.hasText(domain.getName())) {
            errorList
                .add(messages.getMessage("purl_server.error.validate.domain.name.empty", null, locale));
        }
        List<String> logins = userDAO.retrieveLogins();
        for (DomainUser du : domain.getDomainUserList()) {
            if (!logins.contains(du.getUser().getLogin())) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.user",
                    new Object[] { du.getUser().getLogin() }, locale));
            }
        }
        return errorList;
    }

}
