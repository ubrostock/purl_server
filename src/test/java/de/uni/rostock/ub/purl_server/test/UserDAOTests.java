package de.uni.rostock.ub.purl_server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import de.uni.rostock.ub.purl_server.dao.UserDAO;
import de.uni.rostock.ub.purl_server.model.Status;
import de.uni.rostock.ub.purl_server.model.User;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class UserDAOTests extends PURLServerBaseTest {
    @Autowired
    UserDAO userDAO;
    
    @Test
    void testRetrieveUserByLogin() {
        Optional<User> u = userDAO.retrieveUser("user5");
        assertTrue(u.isPresent());
    }
    
    @Test
    void testRetrieveUserWithoutLogin() {
        Optional<User> u = userDAO.retrieveUser("");
        assertTrue(u.isEmpty());
    }
    
    @Test
    void testRetrieveUserById() {
        Optional<User> u = userDAO.retrieveUser(201);
        assertTrue(u.isPresent());
    }
    
    @Test
    void testRetrieveLogins() {
        List<String> logins = userDAO.retrieveLogins();
        assertFalse(logins.isEmpty());
    }
    
    @Test
    void testSearchUsers() {
        List<User> l = userDAO.searchUsers("Test", null, null, null, false, 5);
        assertFalse(l.isEmpty());
    }
    
    @Test
    void testRetrieveActiveUsers() {
        List<User> l = userDAO.retrieveActiveUsers();
        assertFalse(l.isEmpty());
    }
    
   /* @Test
    void testCreateUser() {
        User u = createTestUser(202, "Create Test User 2").get();
        u.setAffiliation("");
        u.setPasswordSHA("");
        u.setAdmin(false);
        u.setCreated(Instant.now());
        u.setLastmodified(Instant.now());
        u.setEmail("");
        u.setFullname("");
        u.setComment("");
        userDAO.createUser(u);
        User createdUser = userDAO.retrieveUser(202).get();
        assertEquals(Status.CREATED , createdUser.getStatus());
        
    }*/
    /*
    @Test
    public void modifyUser() {
        User u = userDAO.retrieveUser(301).get();
        userDAO.modifyUser(u);
        User uModified = userDAO.retrieveUser(301).get();
        assertEquals(Status.MODIFIED, uModified.getStatus());
    }
    */
    @Test
    public void modifyUser() {
        User u = createTestUser(303, "User303").get();
        u.setAffiliation("");
        u.setPasswordSHA("");
        u.setAdmin(false);
        u.setCreated(Instant.now());
        u.setLastmodified(Instant.now());
        u.setEmail("");
        u.setFullname("");
        u.setComment("");
        userDAO.createUser(u);
        User createdUser = userDAO.retrieveUser(303).get();
        userDAO.modifyUser(createdUser);
        User modifiedUser = userDAO.retrieveUser(303).get();
        assertEquals(Status.MODIFIED, modifiedUser.getStatus());
    }
    

}
