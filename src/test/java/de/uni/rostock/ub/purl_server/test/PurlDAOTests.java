package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.Type;
import de.uni.rostock.ub.purl_server.model.User;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class PurlDAOTests extends PURLServerBaseTest {

    @Autowired
    PurlDAO purlDAO;

    @Test
    void testRetrievePurlRedirect() {
        Optional<Purl> purl = purlDAO.retrievePurl("/test/redirect1");
        assertTrue(purl.isPresent());
    }

    @Test
    void testRetrievePurlWrongPathRedirect() {
        Optional<Purl> purl = purlDAO.retrievePurl("/test/redirect12");
        assertTrue(purl.isEmpty());
    }

    @Test
    void testRetrievePurlPartial() {
        Optional<Purl> purl = purlDAO.retrievePurl("/test/test123");
        assertTrue(purl.isPresent());
    }

    @Test
    void testRetrievePurlPartialPath() {
        Optional<Purl> purl = purlDAO.retrievePurl("/purlDAO/123");
        assertEquals("https://example.com", purl.get().getTarget());
        assertEquals("/purlDAO", purl.get().getDomain().getPath());
        Optional<User> u = createTestUser(105, "user5");
        assertTrue(purl.get().getDomain().getDomainUserList().stream()
            .anyMatch(du -> du.getUser().getLogin().equals(u.get().getLogin())));
    }

    @Test
    void testRetrievePurlPartialPathPartial() {
        Optional<Purl> purl = purlDAO.retrievePurl("/purlDAO/12345");
        assertEquals("https://example.com", purl.get().getTarget());
        assertEquals("/purlDAO", purl.get().getDomain().getPath());
        Optional<User> u = createTestUser(105, "user5");
        assertTrue(purl.get().getDomain().getDomainUserList().stream()
            .anyMatch(du -> du.getUser().getLogin().equals(u.get().getLogin())));
    }

    @Test
    void testRetrievePurlById() {
        Optional<Purl> purl = purlDAO.retrievePurl(1);
        assertTrue(purl.isPresent());
        purl = purlDAO.retrievePurl(9999);
        assertTrue(purl.isEmpty());
    }

    @Test
    void testCreatePurl() {
        Purl p = createTestPurl("/purlDAO/daoCreatePurl", "https://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(105, "user5");
        Optional<Purl> createdPurl = purlDAO.createPurl(p, u.get());
        assertTrue(createdPurl.isPresent());
        Optional<Purl> createdPurlWithHistory = purlDAO.retrievePurlWithHistory("/purlDAO/daoCreatePurl");
        assertTrue(createdPurlWithHistory.get().getPurlHistory().get(0).getStatus().equals(Status.CREATED));

    }

    @Test
    void testModifyPurl() {
        Purl p = createTestPurl("/purlDAO/modified", "https://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(105, "user5");
        Optional<Purl> createdPurl = purlDAO.createPurl(p, u.get());
        createdPurl.get().setTarget("https://example.com/modified");
        Optional<Purl> modifiedPurl = purlDAO.modifyPurl(createdPurl.get(), u.get());
        assertEquals("https://example.com/modified", modifiedPurl.get().getTarget());
        Optional<Purl> modifiedPurlWithHistory = purlDAO.retrievePurlWithHistory(modifiedPurl.get().getPath());
        assertEquals("https://example.com", modifiedPurlWithHistory.get().getPurlHistory().get(1).getTarget());
        assertEquals("https://example.com/modified", modifiedPurlWithHistory.get().getPurlHistory().get(0).getTarget());
    }

    @Test
    void testDeletePurl() {
        Purl p = createTestPurl("/purlDAO/delete", "https://example.com", Type.REDIRECT_302);
        Optional<User> u = createTestUser(105, "user5");
        Optional<Purl> createdPurl = purlDAO.createPurl(p, u.get());
        purlDAO.deletePurl(createdPurl.get(), u.get());
        Optional<Purl> deletedPurlWithHistory = purlDAO.retrievePurlWithHistory("/purlDAO/delete");
        assertTrue(deletedPurlWithHistory.get().getStatus().equals(Status.DELETED));
        assertTrue(deletedPurlWithHistory.get().getPurlHistory().get(0).getStatus().equals(Status.DELETED));
        assertTrue(deletedPurlWithHistory.get().getPurlHistory().get(1).getStatus().equals(Status.CREATED));
    }

    @Test
    void testSearchPurls() {
        List<Purl> purls = purlDAO.searchPurls("DAO", "example", true, 5);
        assertFalse(purls.isEmpty());
    }

}
