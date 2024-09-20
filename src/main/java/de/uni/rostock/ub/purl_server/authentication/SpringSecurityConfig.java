/*
 * Copyright 2020 University Library, 18051 Rostock, Germany
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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/* Migration Spring Security 5 to 6
 * ---------------------------------
 * https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter
 *  
 * Method Annotation:
 * @EnableGlobalMethodSecurity -> @EnableMethodSecurity
 * https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html#migration-enableglobalmethodsecurity
 * 
 * PasswordEncoder:
 * https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html#authentication-password-storage-configuration
 * (DelegatingPasswordEncoder by default)
 * 
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SpringSecurityConfig {
    @Autowired
    DataSource dataSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .logout(l -> l
                .logoutUrl("/admin/logout"))
            .formLogin(fl -> fl
                .loginPage("/admin/login")
                .defaultSuccessUrl("/admin/manager"))
            //fine tune authorization for urls of login pages
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/manager/**").authenticated()
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin/login/**").permitAll())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST).authenticated()
                .requestMatchers(HttpMethod.PUT).authenticated()
                .requestMatchers(HttpMethod.DELETE).authenticated()
                //required for resources from webjars, javascript and css files:
                .requestMatchers(HttpMethod.GET).permitAll())
            
            .httpBasic(Customizer.withDefaults());
            return http.build();
    }

    @Bean
    public UserDetailsManager users(DataSource dataSource) {
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);
        users.setUsersByUsernameQuery(
            "SELECT login,password_sha, (status = 'CREATED' OR status = 'MODIFIED') FROM `user` WHERE login = ?;");
        users.setAuthoritiesByUsernameQuery("SELECT * FROM (SELECT login, 'ROLE_USER' FROM `user` "
            + " UNION SELECT login, 'ROLE_ADMIN' FROM `user` WHERE admin = true) AS x WHERE x.login = ?;");
        return users;
    }
    
    @Bean
    //TODO remove and try to work with default DelegatingPasswordEncoder
    public static PasswordEncoder passwordEncoder() {
        //return NoOpPasswordEncoder.getInstance();
        //return new BCryptPasswordEncoder();
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    
}
