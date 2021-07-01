package de.uni.rostock.ub.purl_server.validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.model.User;

@Service
public class UserValidateService {

    private static String SHA_EMPTY_STRING = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
    
    @Autowired
    MessageSource messages;
    
    /**
     * Validate the user
     * 
     * @param user
     * @return the error list
     */
    public List<String> validateUser(User user) {
        List<String> errorList = new ArrayList<>();
        if (SHA_EMPTY_STRING.equals(user.getPasswordSHA())) {
            errorList.add(messages.getMessage("purl_server.error.validate.user.password.empty", null, Locale.getDefault()));
        }
        if (StringUtils.isEmpty(user.getLogin())) {
            errorList.add(messages.getMessage("purl_server.error.validate.user.username.empty", null, Locale.getDefault()));
        }
        return errorList;
    }
}
