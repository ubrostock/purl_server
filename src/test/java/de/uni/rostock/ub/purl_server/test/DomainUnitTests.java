package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.validate.DomainValidateService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public class DomainUnitTests extends PURLServerBaseTest {
    @Autowired
    DomainValidateService domainValidateService;

    @Autowired
    DomainDAO domainDAO;
    
    @Test
    public void testDomainValidate() throws Exception {
        Domain domain = new Domain();
        domain.setName("Test Domain 1");
        domain.setPath("/test1");
        List<String> errorList = null;
        errorList = domainValidateService.validateCreateDomain(domain, Locale.GERMAN);
        assertTrue(errorList.isEmpty(), "Domain invalid: " + String.join(", ", errorList));
        
        domain.setPath("/test 1");
        errorList = domainValidateService.validateCreateDomain(domain, Locale.GERMAN);
        assertFalse(errorList.isEmpty(), "Domain error not detected - given path is invalid");
        assertTrue(errorList.get(0).startsWith("Pfad darf nur"), "Domain error message for invalid path not created");

        // Hinweis/Beispiel: Test auf Liste mit Inhalt in beliebiger Reihenfolge
        // Assertions.assertThat(expectedUserList).containsExactlyInAnyOrderElementsOf(actualUserList);
    }

    @Test
    public void testDomainDAOCreate() throws Exception {
        Domain domain = new Domain();
        domain.setName("Test Domain 1");
        domain.setPath("/test1");

        Optional<Domain> newDomain = domainDAO.createDomain(domain);
        assertTrue(newDomain.isPresent(), "Domain wurde nicht erstellt");
        assertEquals(domain.getName(), newDomain.get().getName());
    }

}
