package de.uni.rostock.ub.purl_server.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import de.uni.rostock.ub.purl_server.dao.DomainDAO;
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
}
