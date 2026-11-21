package com.checkup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may open which page. Every request passes through these rules BEFORE any controller
 * runs, so a controller can't forget to check.
 *
 * Rules are checked top to bottom; the first match wins.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers("/", "/login", "/register", "/careers", "/css/**", "/error").permitAll()
                // Development-only database browser (turned off in production in step 7)
                .requestMatchers("/h2-console/**").permitAll()
                // Each area is locked to one role
                .requestMatchers("/patient/**").hasRole("PATIENT")
                .requestMatchers("/doctor/**").hasRole("DOCTOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Anything else needs a login (deny-by-default)
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")              // our own page instead of Spring's default
                .usernameParameter("email")       // the form field holding the login name
                .defaultSuccessUrl("/dashboard", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/?loggedOut"))
            // CSRF protection stays ON for the whole site: every form carries a secret token,
            // so another website can't submit forms as you. The H2 console can't send the
            // token, and it isn't part of the real app, so it's the only exception.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            // The H2 console draws itself in frames; allow frames from our own site only.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * Hashes passwords with BCrypt (stored as "{bcrypt}$2a$10$..."). BCrypt is slow on
     * purpose and salts every hash, so leaked hashes are very hard to crack, and two people
     * with the same password still get different hashes.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
