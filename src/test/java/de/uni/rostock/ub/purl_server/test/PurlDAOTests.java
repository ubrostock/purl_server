package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.DomainUser;
import de.uni.rostock.ub.purl_server.model.Purl;
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
        assertTrue(purl.get().getDomain().getDomainUserList().stream().anyMatch(du -> du.getUser().getLogin().equals(u.get().getLogin())));
    }
    
}
