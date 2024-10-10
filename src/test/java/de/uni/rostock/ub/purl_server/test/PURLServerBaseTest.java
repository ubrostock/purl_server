package de.uni.rostock.ub.purl_server.test;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
import de.uni.rostock.ub.purl_server.model.Purl;
import de.uni.rostock.ub.purl_server.model.Type;
import de.uni.rostock.ub.purl_server.model.User;
import de.uni.rostock.ub.purl_server.validate.DomainValidateService;

/**
 * BaseTest class for PURLServer
 * 
 * initializes the GreenMail SMTP-Server
 */
public class PURLServerBaseTest {
    @Autowired
    DomainValidateService domainValidateService;

    @Autowired
    DomainDAO domainDAO;

    private GreenMail greenMail;

    @Value("${spring.mail.port}")
    private int smtpPort;

    @BeforeEach
    public void setup() {
        // Start GreenMail SMTP server
        greenMail = new GreenMail(new ServerSetup(smtpPort, null, "smtp"));
        greenMail.start();
    }

    @AfterEach
    public void tearDown() {
        greenMail.stop();
    }
    
    public Purl createTestPurl(String path, String target, Type type) {
        Purl p = new Purl();
        p.setPath(path);
        p.setTarget(target);
        p.setType(type);
        return p;
    }
    
    public Optional<User> createTestUser(int id, String login) {
        Optional<User> u = Optional.of(new User());
        u.get().setLogin(login);
        u.get().setId(id);
        return u;
    }
}
