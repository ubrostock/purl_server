/*
 * Copyright 2020 University Library, 18051 Rostock, Germany
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
package de.uni.rostock.ub.purl_server.common;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.DomainUser;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.Type;
import de.uni.rostock.ub.purl_server.model.User;

@Component
public class PurlAccess {
    @Autowired
    DomainDAO domainDAO;

    @Autowired
    UserDAO userDAO;

    @Autowired
    PurlDAO purlDAO;
    
    @Autowired
    private MessageSource messages;

    public PurlAccess() {

    }

    /**
     * Check a user if he is allowed to create
     * @param d domain
     * @param u user which is loged in
     * @return true, if user is allowed to create
     */
    public boolean canCreatePurl(Domain d, User u) {
        if (d == null) {
            return false;
        }
        List<DomainUser> userList = domainDAO.retrieveDomainWithUser(d.getId()).orElseThrow().getDomainUserList();
        if (u.getStatus() != Status.DELETED) {
            for (DomainUser du : userList) {
                if (du.getUser().getId() == u.getId() && du.isCanCreate()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canCreatePurl(Purl p, User u) {
        Domain d = domainDAO.retrieveDomain(p).orElseThrow();
        return canCreatePurl(d, u);
    }

    /**
     * Check a user if he is allowed to modify
     * @param d
     * @param u user which is loged in
     * @return true, if user is allowed to modify
     */
    public boolean canModifyPurl(Domain d, User u) {
        if (d == null) {
            return false;
        }
        List<DomainUser> userList = domainDAO.retrieveDomainWithUser(d.getId()).orElseThrow().getDomainUserList();
        if (u.getStatus() != Status.DELETED) {
            for (DomainUser du : userList) {
                if (du.getUser().getId() == u.getId() && du.isCanModify()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canModifyPurl(Purl p, User u) {
        Domain d = domainDAO.retrieveDomain(p).orElseThrow();
        return canModifyPurl(d, u);
    }

    /**
     * Retrieve user from request only works for RestAPI 
     * where user is set via Basic Authentication
     * @param request
     * @return user from request
     */
    public User retrieveUserFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Basic")) {
            authorization = authorization.substring(6).trim();
            authorization = new String(Base64.getDecoder().decode(authorization));
            if (authorization.contains(":")) {
                String userName = authorization.substring(0, authorization.indexOf(":"));
                Optional<User> ou = userDAO.retrieveUser(userName);
                if (ou.isPresent()) {
                    return ou.get();
                }
            }
        }
        return null;
    }

    /**
     * retrieves the current logged in user from SecurityContext
     * Use this in other Controllers (e.g. when processing html forms)
     */
    public User retrieveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String userName = auth.getName(); //get logged in username
            Optional<User> ou = userDAO.retrieveUser(userName);
            if (ou.isPresent()) {
                return ou.get();
            }
        }
        return null;
    }

    /**
     * Validate a purl which want to be created
     * @param purl
     * @return the error list
     */
    public List<String> validateCreatePurl(Purl purl, User u) {
        List<String> errorList = new ArrayList<>();
        if (StringUtils.isEmpty(purl.getPath())) {
            errorList.add(messages.getMessage("purl_server.error.validate.purl.create.path", null, Locale.getDefault()));
            return errorList;
        }
        Optional<Purl> currentPurl = purlDAO.retrievePurl(purl.getPath());
        if (currentPurl.isEmpty()) {

        } else {
            if (currentPurl.get().getStatus() == Status.DELETED) {

            } else {
                if (currentPurl.get().getType() == Type.PARTIAL_302) {
                    if (purl.getPath().length() == currentPurl.get().getPath().length()) {
                        errorList.add(messages.getMessage("purl_server.error.validate.purl.create.exist", null, Locale.getDefault()));
                    }
                } else {
                    errorList.add(messages.getMessage("purl_server.error.validate.purl.create.exist", null, Locale.getDefault()));
                }
            }
        }

        errorList.addAll(validatePurl(purl));
        if (errorList.isEmpty()) {
            Domain d = domainDAO.retrieveDomain(purl).orElseThrow();
            if (d == null) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.exist", new Object[] {purl.getDomainPath()}, Locale.getDefault()));
            } else if (d.getStatus() == Status.DELETED) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.deleted", new Object[] {d.getPath()}, Locale.getDefault()));
            } else if (!canCreatePurl(d, u)) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.unauthorized", null, Locale.getDefault()));
            }
        }
        return errorList;
    }

    /**
     * Validate a purl which want to be modified
     * @param purl
     * @param u
     * @return the error list
     */
    public List<String> validateModifyPurl(Purl purl, User u) {
        List<String> errorList = new ArrayList<>();
        errorList.addAll(validatePurl(purl));
        if (errorList.isEmpty()) {
            domainDAO.retrieveDomain(purl).ifPresentOrElse(
                d -> {
                    if (!canModifyPurl(d, u)) {
                        errorList.add(messages.getMessage("purl_server.error.validate.user.modify.purl", null, Locale.getDefault()));
                    }
                }, () -> {
                    errorList.add(messages.getMessage("purl_server.error.validate.domain.exist", new Object[] {purl.getDomainPath()}, Locale.getDefault()));
                });

            if (purl.getStatus() == Status.DELETED) {
                errorList.add(messages.getMessage("purl_server.error.validate.user.modify.deleted.purl", null, Locale.getDefault()));
            }
        }
        return errorList;
    }

    /**
     * Validate a purl
     * @param purl
     * @return return a error list
     */
    private List<String> validatePurl(Purl purl) {
        List<String> errorList = new ArrayList<>();
        if (StringUtils.isEmpty(purl.getPath())) {
            errorList.add(messages.getMessage("purl_server.error.validate.purl.path.empty", null, Locale.getDefault()));
        } else {
            if (!purl.getPath().startsWith("/")) {
                errorList.add(messages.getMessage("purl_server.error.validate.purl.path.start", null, Locale.getDefault()));
            }
            if (!purl.getPath().matches("\\/[a-zA-Z0-9]+(\\/[a-zA-Z0-9]*)*")) {
                errorList.add(messages.getMessage("purl_server.error.validate.purl.path.match", null, Locale.getDefault()));
            }
        }
        if (StringUtils.isEmpty(purl.getTarget())) {
            errorList.add(messages.getMessage("purl_server.error.validate.purl.target.empty", null, Locale.getDefault()));
        } else {
            if (!purl.getTarget().startsWith("https://") && !purl.getTarget().startsWith("http://")) {
                errorList.add(messages.getMessage("purl_server.error.validate.purl.target.start", null, Locale.getDefault()));
            }
        }
        if (StringUtils.isEmpty(purl.getType())) {
            errorList.add(messages.getMessage("purl_server.error.validate.purl.type.target", null, Locale.getDefault()));
        }
        return errorList;
    }
}
