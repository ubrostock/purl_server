package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.model.Domain;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.Type;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class DomainDAOTests extends PURLServerBaseTest {
    @Autowired
    DomainDAO domainDAO;

    @Test
    void testDomainDAOCreate() {
        Domain domain = createTestDomain("/test1", "Test Domain 1");
        Optional<Domain> newDomain = domainDAO.createDomain(domain);
        assertTrue(newDomain.isPresent(), "Domain wurde nicht erstellt");
        assertEquals(domain.getName(), newDomain.get().getName());
    }

    @Test
    void testRetrieveDomainString() {
        Optional<Domain> domain = domainDAO.retrieveDomain("/test");
        assertTrue(domain.isPresent(), "Domain wurde nicht abgerufen.");
    }

    @Test
    void testRetrieveDomainWithUser() {
        Optional<Domain> domain = domainDAO.retrieveDomainWithUser(12);
        assertTrue(domain.isPresent(), "Domain wurde nicht abgerufen.");
        assertTrue(domain.get().getDomainUserList().size() > 0, "Keine Benutzer in der Domain");
    }

    @Test
    void testRetrieveDomainPurl() {
        Purl purl = createTestPurl("/test/test123", "https://example.com", Type.PARTIAL_302);
        Optional<Domain> domain = domainDAO.retrieveDomain(purl);
        assertTrue(domain.isPresent(), "Domain wurde nicht abgerufen");
    }

    @Test
    void testRetrieveDomainId() {
        Optional<Domain> domain = domainDAO.retrieveDomain(12);
        assertTrue(domain.isPresent(), "Domain wurde nicht abgerufen.");
    }

    @Test
    void testModifyDomain() {
        Optional<Domain> domain = domainDAO.retrieveDomain("/test");
        String domainNameNeu = "Test Domain modified";
        domain.get().setName(domainNameNeu);
        domain = domainDAO.modifyDomain(domain.get());
        assertTrue(domain.get().getName().equals(domainNameNeu), "Domain wurde nicht bearbeitet.");
    }

    @Test
    void testDomainDAODelete() {
        Optional<Domain> domain = domainDAO.retrieveDomain(12);
        domainDAO.deleteDomain(domain.get());
        domain = domainDAO.retrieveDomain(12);
        assertTrue(domain.get().getStatus().equals(Status.DELETED), "Domain wurde nicht gelöscht");
    }

}
