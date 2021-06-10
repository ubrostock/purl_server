package de.uni.rostock.ub.purl_server.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.Type;
import de.uni.rostock.ub.purl_server.model.User;

@Component
public class PurlValidate {
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
    public List<String> validateCreatePurl(Purl purl, User u) {
        List<String> errorList = new ArrayList<>();
        if (StringUtils.isEmpty(purl.getPath())) {
            errorList.add(messages.getMessage("purl_server.error.validate.purl.create.path.empty", null, Locale.getDefault()));
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
            } else if (!purlAccess.canCreatePurl(d, u)) {
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
                    if (!purlAccess.canModifyPurl(d, u)) {
                        errorList.add(messages.getMessage("purl_server.error.validate.user.modify.purl.unauthorized", null, Locale.getDefault()));
                    }
                }, () -> {
                    errorList.add(messages.getMessage("purl_server.error.validate.domain.exist", new Object[] {purl.getDomainPath()}, Locale.getDefault()));
                });

            if (purl.getStatus() == Status.DELETED) {
                errorList.add(messages.getMessage("purl_server.error.validate.user.modify.deleted.purl.unauthorized", null, Locale.getDefault()));
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
            errorList.add(messages.getMessage("purl_server.error.validate.purl.type.target.empty", null, Locale.getDefault()));
        }
        return errorList;
    }
}
