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

import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.Type;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.PurlValidateService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class PurlValidateTests extends PURLServerBaseTest {

    @Autowired
    PurlValidateService purlValidateService;

    @Autowired
    MessageSource messages;

    @Test
    void testValidateCreatePurl() {
        Purl p = createTestPurl("/test/123", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertTrue(pse.isOk());
    }

    @Test
    void testValidateCreatePurlEmptyUser() {
        Purl p = createTestPurl("/test/123", "http://example.com", Type.REDIRECT_302);
        Optional<User> u =  Optional.empty();
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.user.unknown",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlPurlNull() {
        Purl p = null;
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.api.purl.input.empty",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlPathAndTargetEmpty() {
        Purl p = createTestPurl("  ", "  ", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String messagePath = messages.getMessage("purl_server.error.validate.purl.create_modify.path.empty",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(messagePath), "Fehler bei ValidateCreatePurl");
        String messageTarget = messages.getMessage("purl_server.error.validate.purl.create_modify.target.empty",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(messageTarget));
    }
    
    @Test
    void testValidateCreatePurlPathStartsWith() {
        Purl p = createTestPurl("test", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.path.start",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlPathMatches() {
        Purl p = createTestPurl("/²³$%]{{()=?", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.path.match",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlTargetStartsWith() {
        Purl p = createTestPurl("/test", "ftp://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.target.start",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlTypeEmpty() {
        Purl p = createTestPurl("/test", "http://example.com", null);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.type.target.empty",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlDomainNotPresent() {
        Purl p = createTestPurl("/xxx/1234", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.domain.modify.exist",
            new Object[] { p.getDomainPath() }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlDomainStatusDeleted() {
        Purl p = createTestPurl("/deleted/1234", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.domain.create.deleted",
            new Object[] { "/deleted" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlCanCreatePurlUserCreateFalse() {
        Purl p = createTestPurl("/test/1234", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(102, "user2");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.domain.create.unauthorized",
            new Object[] { "/test" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlCanCreatePurUserNotInDomain() {
        Purl p = createTestPurl("/test/1234", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(103, "user3");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.domain.create.unauthorized",
            new Object[] { "/test" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlExist() {
        Purl p = createTestPurl("/test/redirect1", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.path.exist",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateCreatePurlPartOfPartial() {
        Purl p = createTestPurl("/test/test123", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateCreatePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create.path.partial",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateModifyPurl() {
        Purl p = createTestPurl("/test/123", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateModifyPurl(p, u, Locale.getDefault());
        assertTrue(pse.isOk());
    }
    
    @Test
    void testValidateModifyPurlUserEmpty() {
        Purl p = createTestPurl("/test/123", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = Optional.empty();
        PurlServerError pse = purlValidateService.validateModifyPurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.user.unknown",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateModifyPurlPurlNull() {
        Purl p = null;
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateModifyPurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.api.purl.input.empty",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateModifyPurlDomainNotPresent() {
        Purl p = createTestPurl("/xxx/1234", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateModifyPurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.modify.domain.exist",
            new Object[] { p.getDomainPath() }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateModifyPurlCanModifyPurl() {
        Purl p = createTestPurl("/modify/1234", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(104, "user4");
        PurlServerError pse = purlValidateService.validateModifyPurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.modify.unauthorized",
            new Object[] { "user4" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateModifyPurlExist() {
        Purl p = createTestPurl("/test/redirect1", "http://example.com", Type.REDIRECT_302);
        p.setId(9999);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateModifyPurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.path.exist",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateDeletePurl() {
        Purl p = createTestPurl("/test/123", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(101, "user1");
        PurlServerError pse = purlValidateService.validateDeletePurl(p, u, Locale.getDefault());
        assertTrue(pse.isOk());
    }
    
    @Test
    void testValidateDeletePurlUserEmpty() {
        Purl p = createTestPurl("/test/123", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = Optional.empty();
        PurlServerError pse = purlValidateService.validateDeletePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.validate.purl.create_modify.user.unknown",
            null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }
    
    @Test
    void testValidateDeletePurlCanModifyPurl() {
        Purl p = createTestPurl("/modify/1234", "http://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(104, "user4");
        PurlServerError pse = purlValidateService.validateDeletePurl(p, u, Locale.getDefault());
        assertFalse(pse.isOk(), "Fehler bei ValidateCreatePurl");
        String message = messages.getMessage("purl_server.error.api.purl.delete.unauthorized",
            new Object[] { "user4" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

}
