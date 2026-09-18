package com.ricozknow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    /**
     * Argon2id, per spec section 6 (secure password hashing).
     * Parameters follow OWASP's current baseline recommendation for Argon2id
     * (19 MiB memory, 2 iterations, 1 degree of parallelism, 32-byte hash, 16-byte salt)
     * as a starting point — tune against real infra during Phase 7 load testing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }
}
