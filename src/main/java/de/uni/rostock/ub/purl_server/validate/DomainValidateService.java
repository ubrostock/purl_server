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

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.DomainUser;
import de.uni.rostock.ub.purl_server.model.PurlServerError;

@Service
public class DomainValidateService {

    @Autowired
    DomainDAO domainDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    MessageSource messages;

    public PurlServerError validateCreateDomain(Domain d, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.domain.create", null, locale), null);
        cleanUp(d);
        domainDAO.retrieveDomain(d.getPath()).ifPresentOrElse(dd -> {
            pse.getDetails().add(messages.getMessage("purl_server.error.validate.domain.create.already.exist",
                new Object[] { dd.getPath() }, locale));
        }, () -> {
            validatePath(pse, d, locale);
            validateDomain(pse, d, locale);
        });

        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    public PurlServerError validateModifyDomain(Domain d, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.domain.modify", null, locale), null);
        cleanUp(d);
        domainDAO.retrieveDomain(d.getPath()).ifPresentOrElse(dd -> {
            validateDomain(pse, d, locale);
        }, () -> {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.domain.modify.exist", new Object[] { d.getPath() },
                    locale));
        });
        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    public PurlServerError validateDeleteDomain(Domain d, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.domain.delete", null, locale), null);
        domainDAO.retrieveDomain(d.getId()).ifPresentOrElse(dd -> {
        }, () -> {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.domain.modify.exist", new Object[] { d.getPath() },
                    locale));
        });
        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    private void validatePath(PurlServerError pse, Domain domain, Locale locale) {
        if (!StringUtils.hasText(domain.getPath())) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.domain.create.path.empty", null, locale));
            return;
        }

        if (!domain.getPath().matches("/" + Domain.REGEX_VALID_DOMAINS)) {
            pse.getDetails().add(
                messages.getMessage("purl_server.error.validate.domain.create.path.reserved", null, locale));
        }
        if (!domain.getPath().startsWith("/")) {
            pse.getDetails().add(
                messages.getMessage("purl_server.error.validate.domain.create.path.start", null, locale));
        }
        if (!domain.getPath().matches("/[-_a-zA-Z0-9]+")) {
            pse.getDetails().add(
                messages.getMessage("purl_server.error.validate.domain.create.path.match", null, locale));
        }
    }

    /**
     * Validate the domain
     * 
     * @param domain
     * @return the error list
     */
    private void validateDomain(PurlServerError pse, Domain domain, Locale locale) {
        if (!StringUtils.hasText(domain.getName())) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.domain.create_modify.name.empty", null, locale));
        }
        List<String> logins = userDAO.retrieveLogins();
        for (DomainUser du : domain.getDomainUserList()) {
            if (!logins.contains(du.getUser().getLogin())) {
                pse.getDetails().add(messages.getMessage("purl_server.error.validate.domain.create_modify.user",
                    new Object[] { du.getUser().getLogin() }, locale));
            }
        }
    }

    private void cleanUp(Domain domain) {
        domain.setComment(domain.getComment() == null ? null : domain.getComment().strip());
        domain.setName(domain.getName() == null ? null : domain.getName().strip());
        domain.setPath(domain.getPath() == null ? null : domain.getPath().strip());
    }
}
