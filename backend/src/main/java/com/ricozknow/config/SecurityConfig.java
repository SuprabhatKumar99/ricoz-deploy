package com.ricozknow.config;

import com.ricozknow.auth.JwtAuthFilter;
import com.ricozknow.tenant.TenantResolverFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Arrays;
import java.util.List;
/**
 * The frontend is never trusted as the authorization boundary (spec section 3);
 * every rule below is enforced server-side regardless of what Angular route
 * guards already checked. Coarse-grained rules live in the filter chain below;
 * finer-grained per-action rules (e.g. "categories are publicly readable but
 * only EDITOR/ADMIN can write") use @PreAuthorize on individual controller
 * methods, enabled here.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${ricozknow.security.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    private final JwtAuthFilter jwtAuthFilter;
    private final TenantResolverFilter tenantResolverFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // bearer-token auth for everything except the refresh
                                                        // cookie (Phase 8), which mitigates CSRF via
                                                        // SameSite=Strict instead — see AuthController
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/register", "/api/v1/auth/logout",
                                "/api/v1/tenants/lookup").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/portal/**").permitAll() // anonymous customer portal reads/feedback
                        .requestMatchers("/api/v1/search/**", "/api/v1/categories/**").permitAll() // public customer portal reads; visibility enforced in service layer
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/agent/**").hasAnyRole("ADMIN", "AGENT_VIEWER")
                        .requestMatchers("/api/v1/articles/*/review").hasAnyRole("ADMIN", "REVIEWER")
                        .requestMatchers("/api/v1/articles/**").hasAnyRole("ADMIN", "EDITOR", "REVIEWER")
                        .anyRequest().authenticated())
                // Order matters: JWT auth first (populates SecurityContext), then tenant
                // resolution (which trusts an authenticated principal's tenantId over hostname).
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantResolverFilter, JwtAuthFilter.class).exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Authentication required"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Access denied"
                    );
                })
        );


        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .toList()
        );

        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));

        configuration.setExposedHeaders(List.of(
            "Authorization"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}