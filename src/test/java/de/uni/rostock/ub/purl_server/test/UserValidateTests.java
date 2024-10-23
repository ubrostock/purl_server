package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;

import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.UserValidateService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class UserValidateTests extends PURLServerBaseTest {

    @Autowired
    UserValidateService userValidateService;

    @Autowired
    MessageSource messages;

    @Test
    void testValidateUser() {
        Optional<User> u = createTestUser(199, "userNeu");
        PurlServerError pse = userValidateService.validateUser(u.get(), Locale.getDefault());
        assertTrue(pse.isOk());
    }

    @Test
    void testValidateUserExists() {
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = userValidateService.validateUser(u.get(), Locale.getDefault());
        assertFalse(pse.isOk());
        String message = messages.getMessage("purl_server.error.validate.user.create.exists", new Object[] { "user1" },
            Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateUserPasswordEmpty() {
        Optional<User> u = createTestUser(105, "user5");
        // SHA_EMPTY_STRING
        u.get().setPasswordSHA("da39a3ee5e6b4b0d3255bfef95601890afd80709");
        PurlServerError pse = userValidateService.validateUser(u.get(), Locale.getDefault());
        assertFalse(pse.isOk());
        String message
            = messages.getMessage("purl_server.error.validate.user.create.password.empty", null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateUserNameEmpty() {
        Optional<User> u = createTestUser(109, "");
        PurlServerError pse = userValidateService.validateUser(u.get(), Locale.getDefault());
        assertFalse(pse.isOk());
        String message
            = messages.getMessage("purl_server.error.validate.user.create.username.empty", null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateModifyUser() {
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = userValidateService.validateModifyUser(u.get(), Locale.getDefault());
        assertTrue(pse.isOk());
    }
    
    @Test
    void testValidateModifyUserNameEmpty() {
        Optional<User> u = createTestUser(199, "");
        PurlServerError pse = userValidateService.validateModifyUser(u.get(), Locale.getDefault());
        assertFalse(pse.isOk());
        String message = messages.getMessage("purl_server.error.validate.domain.create_modify.user", new Object[] { "" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

}
