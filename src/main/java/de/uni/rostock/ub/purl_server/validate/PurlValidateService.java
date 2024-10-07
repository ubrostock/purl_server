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
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.Type;
import de.uni.rostock.ub.purl_server.model.User;

@Service
public class PurlValidateService {
    @Autowired
    DomainDAO domainDAO;

    @Autowired
    PurlDAO purlDAO;

    @Autowired
    MessageSource messages;

    @Autowired
    PurlAccess purlAccess;

    /**
     * Validate a purl which want to be created
     * @param purl
     * @return the error list
     */
    public PurlServerError validateCreatePurl(Purl purl, Optional<User> u, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.purl.create", null, locale), null);
        if (u.isEmpty()) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.purl.create_modify.user.unknown",
                    null, locale));
            pse.setStatus(HttpStatus.CONFLICT);
            return pse;
        }
        if (purl == null) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.api.purl.input.empty", null, locale));
            pse.setStatus(HttpStatus.NOT_FOUND);
            return pse;
        }
        cleanUp(purl);
        if (!StringUtils.hasText(purl.getPath())) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.purl.create.path.empty", null, locale));
        }
        validatePurl(pse, purl, locale);
        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
            return pse;
        }

        Optional<Domain> d = domainDAO.retrieveDomain(purl);
        if (d.isPresent()) {
            if (d.get().getStatus() == Status.DELETED) {
                pse.getDetails().add(messages.getMessage("purl_server.error.validate.domain.create.deleted",
                    new Object[] { d.get().getPath() }, locale));
                pse.setStatus(HttpStatus.CONFLICT);
                return pse;
            } else if (!purlAccess.canCreatePurl(d.get(), u.get())) {
                pse.getDetails().add(messages.getMessage("purl_server.error.validate.domain.create.unauthorized",
                    new Object[] { d.get().getPath() }, locale));
                pse.setStatus(HttpStatus.UNAUTHORIZED);
                return pse;
            }
        } else {
            pse.getDetails().add(messages.getMessage("purl_server.error.validate.domain.modify.exist",
                new Object[] { purl.getDomainPath() }, locale));
            pse.setStatus(HttpStatus.CONFLICT);
            return pse;
        }

        Optional<Purl> currentPurl = purlDAO.retrievePurl(purl.getPath());
        if (!currentPurl.isEmpty() && currentPurl.get().getStatus() != Status.DELETED) {
            if (currentPurl.get().getType() == Type.PARTIAL_302) {
                if (purl.getPath().length() == currentPurl.get().getPath().length()) {
                    pse.getDetails()
                        .add(messages.getMessage("purl_server.error.validate.purl.create_modify.path.exist", null,
                            locale));
                    pse.setStatus(HttpStatus.CONFLICT);
                    return pse;
                }
            } else {
                pse.getDetails()
                    .add(messages.getMessage("purl_server.error.validate.purl.create_modify.path.exist", null, locale));
                pse.setStatus(HttpStatus.CONFLICT);
                return pse;
            }
        }

        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    /**
     * Validate a purl which want to be modified
     * @param purl
     * @param u
     * @return the error list
     */
    public PurlServerError validateModifyPurl(Purl purl, Optional<User> u, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.purl.modify", null, locale), null);
        if (u.isEmpty()) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.purl.create_modify.user.unknown",
                    null, locale));
            pse.setStatus(HttpStatus.CONFLICT);
            return pse;
        }
        if (purl == null) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.api.purl.input.empty", null, locale));
            pse.setStatus(HttpStatus.NOT_FOUND);
            return pse;
        }
        
        
        cleanUp(purl);
        validatePurl(pse, purl, locale);

        if (pse.getDetails().isEmpty()) {
            domainDAO.retrieveDomain(purl).ifPresentOrElse(
                d -> {
                    if (!purlAccess.canModifyPurl(d, u.get())) {
                        pse.getDetails()
                            .add(messages.getMessage("purl_server.error.validate.purl.modify.unauthorized",
                                new Object[] { u.get().getLogin() }, locale));
                    }
                }, () -> {
                    pse.getDetails().add(messages.getMessage("purl_server.error.validate.purl.modify.domain.exist",
                        new Object[] { purl.getDomainPath() }, locale));
                });
            purlDAO.retrievePurl(purl.getPath()).ifPresent(
                p -> {
                    if (purl.getId() != -1 && purl.getId() != p.getId()) {
                        pse.getDetails()
                            .add(messages.getMessage("purl_server.error.validate.purl.create_modify.path.exist",
                                null, locale));
                    }
                });
        }

        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    /**
     * Validate a purl which want to be deleted
     * @param purl
     * @param u
     * @return the error list
     */
    public PurlServerError validateDeletePurl(Purl purl, Optional<User> u, Locale locale) {
        PurlServerError pse = new PurlServerError(HttpStatus.OK,
            messages.getMessage("purl_server.error.api.purl.delete", null, locale), null);

        if (u.isEmpty()) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.purl.create_modify.user.unknown",
                    null, locale));
            pse.setStatus(HttpStatus.CONFLICT);
            return pse;
        }

        purlDAO.retrievePurl(purl.getId()).ifPresent(deletePurl -> {
            domainDAO.retrieveDomain(deletePurl).ifPresent(d -> {
                if (!purlAccess.canModifyPurl(d, u.get())) {
                    pse.getDetails().add(
                        messages.getMessage("purl_server.error.api.purl.delete.unauthorized",
                            new Object[] { u.get().getLogin() }, locale));
                }
            });
        });

        if (!pse.getDetails().isEmpty()) {
            pse.setStatus(HttpStatus.CONFLICT);
        }
        return pse;
    }

    /**
     * Validate a purl
     * @param purl
     * @return return a error list
     */
    private void validatePurl(PurlServerError pse, Purl purl, Locale locale) {
        if (!StringUtils.hasText(purl.getPath())) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.purl.create_modify.path.empty", null, locale));
        } else {
            if (!purl.getPath().startsWith("/")) {
                pse.getDetails()
                    .add(messages.getMessage("purl_server.error.validate.purl.create_modify.path.start", null, locale));
            }
            if (!purl.getPath().matches("[a-zA-Z0-9_\\-\\/]*")) {
                pse.getDetails()
                    .add(messages.getMessage("purl_server.error.validate.purl.create_modify.path.match", null, locale));
            }
        }
        if (!StringUtils.hasText(purl.getTarget())) {
            pse.getDetails()
                .add(messages.getMessage("purl_server.error.validate.purl.create_modify.target.empty", null, locale));
        } else {
            if (!purl.getTarget().startsWith("https://") && !purl.getTarget().startsWith("http://")) {
                pse.getDetails().add(
                    messages.getMessage("purl_server.error.validate.purl.create_modify.target.start", null, locale));
            }
        }
        if (ObjectUtils.isEmpty(purl.getType())) {
            pse.getDetails().add(
                messages.getMessage("purl_server.error.validate.purl.create_modify.type.target.empty", null, locale));
        }
    }

    private void cleanUp(Purl p) {
        p.setPath(p.getPath() == null ? null : p.getPath().strip());
        p.setTarget(p.getTarget() == null ? null : p.getTarget().strip());
    }

}
