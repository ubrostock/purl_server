/*
 * Copyright 2021 University Library, 18051 Rostock, Germany
 *
 * This file is part of the application "PURL Server".
 * https://github.com/ubrostock/purl_server
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni.rostock.ub.purl_server.authentication;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

@Controller
public class LoginController {
    private static final String MODEL_ATTRIBUTE_ERROR_MESSAGE = "errorMessage";

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private static final String SQL_UPDATE_RESET_TOKEN = "UPDATE user SET password_reset_token = ? WHERE login = ?;";

    private static final String SQL_SELECT_FOR_EMAIL
        = "SELECT login, email, fullname, password_reset_token FROM `user` WHERE login = ?;";

    private static final String SQL_UPATE_PASSWORD
        = "UPDATE user SET password_sha = ?, password_reset_token=null WHERE password_reset_token = ?;";

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MessageSource messages;

    @Value("${purl_server.mail.from}")
    private String mailFrom;

    @GetMapping(value = "/admin/login")
    public ModelAndView login() {
        return new ModelAndView("login/login");
    }

    @PostMapping(value = "/admin/login")
    public ModelAndView login(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("login/login");
        HttpSession session = request.getSession(false);
        if (session != null) {
            AuthenticationException ex = (AuthenticationException) session
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (ex != null) {
                StringBuilder sbErrorMessage = new StringBuilder(ex.getMessage());
                LOGGER.error(ex.getMessage());
                Throwable t = ex;
                while (t.getCause() != null) {
                    t = t.getCause();
                    sbErrorMessage.append("<br />").append(t.getMessage());
                }
                mav.addObject(MODEL_ATTRIBUTE_ERROR_MESSAGE, sbErrorMessage.toString());
            }
        }
        return mav;
    }

    @GetMapping(value = "/admin/login/password")
    public String password() {
        return "login/password_request";
    }

    @GetMapping(value = "/admin/login/password", params = "token")
    public String passwordForm() {
        return "login/password_form";
    }

    @PostMapping(value = "/admin/login/password", params = "do_set_password")
    public ModelAndView resetPassword(@RequestParam(value = "password_reset_token", required = true) String token,
        @RequestParam(value = "password_sha1", required = true) String passwordSha1, Locale locale) {
        ModelAndView mav = new ModelAndView();
        if (validateToken(token, mav, locale)) {
            jdbcTemplate.update(SQL_UPATE_PASSWORD, passwordEncoder.encode(passwordSha1), token);
            mav.setViewName("login/password_success");
        } else {
            mav.setViewName("login/login");
        }
        return mav;
    }

    @PostMapping(value = "/admin/login/password", params = "do_password_reset")
    public ModelAndView passwordReset(@RequestParam(value = "login", required = true) String userid, Locale locale) {
        createToken(userid);
        final AtomicBoolean found = new AtomicBoolean(false);
        jdbcTemplate.query(SQL_SELECT_FOR_EMAIL, new RowCallbackHandler() {
            @Override
            public void processRow(ResultSet rs) throws SQLException {
                sendEmail(rs.getString("login"), rs.getString("fullname"), rs.getString("email"),
                    rs.getString("password_reset_token"), locale);
                found.set(true);
            }
        }, userid);
        if (found.get()) {
            return new ModelAndView("login/email_success");
        } else {
            ModelAndView mav = new ModelAndView("login/password_request");
            mav.addObject(MODEL_ATTRIBUTE_ERROR_MESSAGE,
                context.getMessage("purl_server.login.error_message.unknown_user", null, locale));
            return mav;
        }
    }

    private boolean validateToken(String token, ModelAndView mav, Locale locale) {
        Integer x = jdbcTemplate.queryForObject("SELECT COUNT(*) from `user` WHERE password_reset_token = ?",
            Integer.class, token);
        if (x != null && x == 1 && token.contains("_")) {
            String timeString = token.substring(0, token.indexOf("_"));
            long time = Long.parseLong(timeString);
            if (time < System.currentTimeMillis()) {
                mav.addObject(MODEL_ATTRIBUTE_ERROR_MESSAGE,
                    context.getMessage("purl_server.password.error_message.invalid_time", null, locale));
                return false;
            } else {
                return true;
            }
        } else {
            mav.addObject(MODEL_ATTRIBUTE_ERROR_MESSAGE,
                context.getMessage("purl_server.password.error_message.invalid_token", null, locale));
            return false;
        }
    }

    private void sendEmail(String login, String name, String email, String token, Locale locale) {
        try {
            UriComponents uriComponents = MvcUriComponentsBuilder.fromMethodName(LoginController.class, "password")
                .queryParam("login", login)
                .queryParam("token", token).build();
            URI uri = uriComponents.encode().toUri();
            MimeMessageHelper mailHelper = new MimeMessageHelper(mailSender.createMimeMessage(), true);
            mailHelper.setFrom(mailFrom);
            mailHelper.setTo(email);
            mailHelper
                .setSubject(context.getMessage("purl_server.email.password_reset.subject", null, locale));
            mailHelper.setText(context.getMessage("purl_server.email.password_reset.body", null, locale)
                + "\n" + uri.toString());
            mailSender.send(mailHelper.getMimeMessage());
        } catch (MessagingException e) {
            LOGGER.error(messages.getMessage("purl_server.error.login.email", null, locale), e);
        }
    }

    private void createToken(String username) {
        String token = Long.toString(System.currentTimeMillis() + 10 * 60 * 1000) + "_" + UUID.randomUUID().toString();
        jdbcTemplate.update(SQL_UPDATE_RESET_TOKEN, token, username);
    }

}
