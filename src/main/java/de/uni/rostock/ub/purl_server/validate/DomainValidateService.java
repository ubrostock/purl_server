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
import de.uni.rostock.ub.purl_server.model.User;

@Service
public class DomainValidateService {
    
    @Autowired
    DomainDAO domainDAO;
    
    @Autowired
    UserDAO userDAO;
    
    @Autowired
    MessageSource messages;
    
    public List<String> validateCreateDomain(Domain d, User u) {
        List<String> errorList = new ArrayList<>();
        domainDAO.retrieveDomain(d.getPath()).ifPresentOrElse(dd -> {
            errorList.add(messages.getMessage("purl_server.error.validate.domain.already.exist", new Object[] {dd.getPath()}, Locale.getDefault()));
        }, () -> {
            errorList.addAll(validateDomain(d));
        });
        return errorList;
    }

    public List<String> validateModifyDomain(Domain d, User u) {
        List<String> errorList = new ArrayList<>();
        domainDAO.retrieveDomain(d.getPath()).ifPresentOrElse(dd -> {
            errorList.addAll(validateDomain(d));
        }, () -> {
            errorList.add(messages.getMessage("purl_server.error.validate.domain.exist", new Object[] {d.getPath()}, Locale.getDefault()));
        });
        return errorList;
    }

    /**
     * Validate the domain
     * 
     * @param domain
     * @return the error list
     */
    private List<String> validateDomain(Domain domain) {
        List<String> errorList = new ArrayList<>();
        if (StringUtils.isEmpty(domain.getPath())) {
            errorList.add(messages.getMessage("purl_server.error.validate.domain.path.empty", null, Locale.getDefault()));
        } else {
            if (domain.getPath().startsWith("/admin")) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.path.start.admin", null, Locale.getDefault()));
            }
            if (!domain.getPath().startsWith("/")) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.path.start", null, Locale.getDefault()));
            }
            if (!domain.getPath().matches("/[-a-zA-Z0-9_]+")) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.path.match", null, Locale.getDefault()));
            }
        }
        if (StringUtils.isEmpty(domain.getName())) {
            errorList.add(messages.getMessage("purl_server.error.validate.domain.name.empty", null, Locale.getDefault()));
        }
        List<String> logins = userDAO.retrieveLogins();
        for (DomainUser du : domain.getDomainUserList()) {
            if (!logins.contains(du.getUser().getLogin())) {
                errorList.add(messages.getMessage("purl_server.error.validate.domain.user", null, Locale.getDefault()));
            }
        }
        return errorList;
    }
}
