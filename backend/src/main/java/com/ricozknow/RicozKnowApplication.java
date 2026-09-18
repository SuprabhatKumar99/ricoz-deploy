package com.ricozknow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RicozKnow modular monolith.
 *
 * Business capabilities live under com.ricozknow.<module> (auth, tenant, user, role,
 * article, category, publishing, search, analytics, asset, agent, audit).
 * Modules communicate through service interfaces / domain events, not by reaching
 * into each other's repositories directly.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class RicozKnowApplication {

    public static void main(String[] args) {
        SpringApplication.run(RicozKnowApplication.class, args);
    }
}
