package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;

import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.DomainUser;
import de.uni.rostock.ub.purl_server.model.PurlServerError;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.DomainValidateService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class DomainValidateTests extends PURLServerBaseTest {

    @Autowired
    DomainValidateService domainValidateService;

    @Autowired
    MessageSource messages;

    @Test
    void testValidateCreateDomain() {
        Domain d = createTestDomain("/testNeu", "Test Domain");
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertTrue(pse.isOk());
    }

    @Test
    void testValidateCreateDomainExists() {
        Domain d = createTestDomain("/test", "Test Domain");
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message = messages.getMessage("purl_server.error.validate.domain.create.already.exist",
            new Object[] { "/test" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateCreateDomainPathEmpty() {
        Domain d = createTestDomain("", "Empty Path Domain");
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message
            = messages.getMessage("purl_server.error.validate.domain.create.path.empty", null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateCreateDomainPathNotMatchValidDomains() {
        Domain d = createTestDomain("/api", "Not Valid Domain");
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message
            = messages.getMessage("purl_server.error.validate.domain.create.path.reserved", null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateCreateDomainStartWithoutSlash() {
        Domain d = createTestDomain("noslash", "No slash Domain");
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message
            = messages.getMessage("purl_server.error.validate.domain.create.path.start", null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateCreateDomainPathValid() {
        Domain d = createTestDomain("/!%&", "No valid path Domain");
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message
            = messages.getMessage("purl_server.error.validate.domain.create.path.match", null, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateCreateDomainNameEmpty() {
        Domain d = createTestDomain("/nonamedomain", "");
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message = messages.getMessage("purl_server.error.validate.domain.create_modify.name.empty", null,
            Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateCreateDomainValidDomainUser() {
        Domain d = createTestDomain("/wrongUserDomain", "Wrong user domain");
        DomainUser du = new DomainUser();
        User u = new User();
        u.setLogin("WrongUserForThisDomain");
        du.setUser(u);
        d.getDomainUserList().add(du);
        PurlServerError pse = domainValidateService.validateCreateDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message = messages.getMessage("purl_server.error.validate.domain.create_modify.user",
            new Object[] { "WrongUserForThisDomain" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateModifyDomain() {
        Domain d = createTestDomain("/test", "Test Domain");
        PurlServerError pse = domainValidateService.validateModifyDomain(d, Locale.getDefault());
        assertTrue(pse.isOk());
    }

    @Test
    void testValidateModifyDomainNotExists() {
        Domain d = createTestDomain("/domainNotExists", "No Domain");
        PurlServerError pse = domainValidateService.validateModifyDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message = messages.getMessage("purl_server.error.validate.domain.modify.exist",
            new Object[] { "/domainNotExists" }, Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

    @Test
    void testValidateDeleteDomain() {
        Domain d = createTestDomain("/test", "Test Domain");
        PurlServerError pse = domainValidateService.validateDeleteDomain(d, Locale.getDefault());
        assertFalse(pse.isOk());
        String message = messages.getMessage("purl_server.error.validate.domain.modify.exist", new Object[] { "/test" },
            Locale.getDefault());
        assertTrue(pse.getDetails().contains(message));
    }

}
