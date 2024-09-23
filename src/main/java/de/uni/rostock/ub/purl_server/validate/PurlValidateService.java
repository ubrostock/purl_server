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
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.common.PurlAccess;
import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.Purl;
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
    public List<String> validateCreatePurl(Purl purl, User u, Locale locale) {
        if (!StringUtils.hasText(purl.getPath())) {
            return List.of(
                messages.getMessage("purl_server.error.validate.purl.create.path.empty", null, locale));
        }
        List<String> l = validatePurl(purl, locale);
        if (!l.isEmpty()) {
            return l;
        }

        Optional<Domain> d = domainDAO.retrieveDomain(purl);
        if (d.isPresent()) {
            if (d.get().getStatus() == Status.DELETED) {
                return List.of(messages.getMessage("purl_server.error.validate.domain.deleted",
                    new Object[] { d.get().getPath() }, locale));
            } else if (!purlAccess.canCreatePurl(d.get(), u)) {
                return List.of(messages.getMessage("purl_server.error.validate.domain.unauthorized",
                    new Object[] { d.get().getPath() }, locale));
            }
        } else {
            return List.of(messages.getMessage("purl_server.error.validate.domain.exist",
                new Object[] { purl.getDomainPath() }, locale));
        }

        Optional<Purl> currentPurl = purlDAO.retrievePurl(purl.getPath());
        if (!currentPurl.isEmpty() && currentPurl.get().getStatus() != Status.DELETED) {
            if (currentPurl.get().getType() == Type.PARTIAL_302) {
                if (purl.getPath().length() == currentPurl.get().getPath().length()) {
                    return List.of(messages.getMessage("purl_server.error.validate.purl.path.exist", null,
                        locale));
                }
            } else {
                return List.of(
                    messages.getMessage("purl_server.error.validate.purl.path.exist", null, locale));
            }
        }
        return List.of();
    }

    /**
     * Validate a purl which want to be modified
     * @param purl
     * @param u
     * @return the error list
     */
    public List<String> validateModifyPurl(Purl purl, User u, Locale locale) {
        List<String> errorList = new ArrayList<>();
        errorList.addAll(validatePurl(purl, locale));
        if (errorList.isEmpty()) {
            domainDAO.retrieveDomain(purl).ifPresentOrElse(
                d -> {
                    if (!purlAccess.canModifyPurl(d, u)) {
                        errorList.add(messages.getMessage("purl_server.error.validate.user.modify.purl.unauthorized",
                            null, locale));
                    }
                }, () -> {
                    errorList.add(messages.getMessage("purl_server.error.validate.domain.exist",
                        new Object[] { purl.getDomainPath() }, locale));
                });
            purlDAO.retrievePurl(purl.getPath()).ifPresent(
                p -> {
                    if (purl.getId() != -1 && purl.getId() != p.getId()) {
                        errorList.add(messages.getMessage("purl_server.error.validate.purl.path.exist", null, locale));
                    }
                });
        }
        return errorList;
    }

    /**
     * Validate a purl which want to be deleted
     * @param purl
     * @param u
     * @return the error list
     */
    public List<String> validateDeletePurl(Purl purl, User u, Locale locale) {
        List<String> errorList = new ArrayList<>();
        purlDAO.retrievePurl(purl.getId()).ifPresent(deletePurl -> {
            domainDAO.retrieveDomain(deletePurl).ifPresent(d -> {
                if (!purlAccess.canModifyPurl(d, u)) {
                    errorList.add(
                        messages.getMessage("purl_server.error.purl.delete.unauthorized", new Object[] { u.getLogin() },
                            Locale.getDefault()));
                }
            });
        });

        return errorList;
    }

    /**
     * Validate a purl
     * @param purl
     * @return return a error list
     */
    private List<String> validatePurl(Purl purl, Locale locale) {
        List<String> errorList = new ArrayList<>();
        if (!StringUtils.hasText(purl.getPath())) {
            errorList.add(messages.getMessage("purl_server.error.validate.purl.path.empty", null, locale));
        } else {
            if (!purl.getPath().startsWith("/")) {
                errorList
                    .add(messages.getMessage("purl_server.error.validate.purl.path.start", null, locale));
            }
            if (!purl.getPath().matches("[a-zA-Z0-9_\\-\\/]*")) {
                errorList
                    .add(messages.getMessage("purl_server.error.validate.purl.path.match", null, locale));
            }
        }
        if (!StringUtils.hasText(purl.getTarget())) {
            errorList
                .add(messages.getMessage("purl_server.error.validate.purl.target.empty", null, locale));
        } else {
            if (!purl.getTarget().startsWith("https://") && !purl.getTarget().startsWith("http://")) {
                errorList.add(
                    messages.getMessage("purl_server.error.validate.purl.target.start", null, locale));
            }
        }
        if (ObjectUtils.isEmpty(purl.getType())) {
            errorList.add(
                messages.getMessage("purl_server.error.validate.purl.type.target.empty", null, locale));
        }
        return errorList;
    }
}
