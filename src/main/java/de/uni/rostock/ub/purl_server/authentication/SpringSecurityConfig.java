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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    DataSource dataSource;

    @Configuration
    @Order(1)
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**").csrf().disable()
                .authorizeRequests(auth -> auth.antMatchers(HttpMethod.POST).authenticated()
                    .antMatchers(HttpMethod.PUT).authenticated().antMatchers(HttpMethod.DELETE).authenticated()
                    .antMatchers(HttpMethod.GET).permitAll())
                .httpBasic();

        }
    }

    @Configuration
    @Order(2)
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.logout(l -> l.logoutUrl("/admin/logout"))
                .formLogin().loginPage("/admin/login").defaultSuccessUrl("/admin/manager/")
                .and().antMatcher("/admin/**")
                .authorizeRequests(auth -> auth.antMatchers("/admin/manager/**").authenticated()
                    .antMatchers("/admin/login").permitAll().antMatchers("/admin/login/**").permitAll());
                
        }
    }

    @Configuration
    @Order(3)
    public static class DefaultWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests(authorize -> authorize.anyRequest().permitAll());
        }
    }

    @Autowired
    @SuppressWarnings("deprecation")
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        /*
        //in-memory authentication 
        String password = "admin";
        byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(password.getBytes());
        String sha1Hex = String.format("%040x", new BigInteger(1, sha1));
        String bcryptedPassword = new BCryptPasswordEncoder().encode(sha1Hex);
        auth.inMemoryAuthentication().withUser("admin").password("{bcrypt}" +
            bcryptedPassword).roles("USER");
        */

        auth.jdbcAuthentication().dataSource(dataSource).passwordEncoder(NoOpPasswordEncoder.getInstance())
            .usersByUsernameQuery(
                "SELECT login,password_sha, (status = 'CREATED' OR status = 'MODIFIED') FROM user WHERE login = ?;")
            .authoritiesByUsernameQuery(
                "SELECT * FROM (SELECT login, 'ROLE_USER' FROM user UNION SELECT login, 'ROLE_ADMIN' FROM user WHERE admin = true) AS x WHERE x.login = ?;");
    }

}
