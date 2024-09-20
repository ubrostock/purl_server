package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.validate.DomainValidateService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class DomainUnitTests {
    @Autowired
    DomainValidateService domainValidateService;

    @Autowired
    DomainDAO domainDAO;

    @Test
    public void testDomainCreate() throws Exception {
        Domain domain = new Domain();
        domain.setName("Test Domain 1");
        domain.setPath("/test1");
        List<String> errorList = domainValidateService.validateCreateDomain(domain, Locale.GERMAN);
        assertTrue(errorList.isEmpty(), "Domain invalid: " + String.join(", ", errorList));

        Optional<Domain> newDomain = domainDAO.createDomain(domain);
        assertTrue(newDomain.isPresent(), "Domain wurde nicht erstellt");
        assertEquals("Test Domain 1", newDomain.get().getName());

        //Test auf Liste mit Inhalt in beliebiger Reihenfolge
        // Assertions.assertThat(expectedUserList).containsExactlyInAnyOrderElementsOf(actualUserList);
    }

}
