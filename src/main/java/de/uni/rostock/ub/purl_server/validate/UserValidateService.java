package de.uni.rostock.ub.purl_server.validate;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.uni.rostock.ub.purl_server.model.User;

@Service
public class UserValidateService {

    private static String SHA_EMPTY_STRING = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
    
    /**
     * Validate the user
     * 
     * @param user
     * @return the error list
     */
    public List<String> validateUser(User user) {
        List<String> errorList = new ArrayList<>();
        if (SHA_EMPTY_STRING.equals(user.getPasswordSHA())) {
            errorList.add("Password can not be empty!");
        }
        if (StringUtils.isEmpty(user.getLogin())) {
            errorList.add("Username can not be empty!");
        }
        return errorList;
    }
}
