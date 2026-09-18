package com.ricozknow.auth;

import com.ricozknow.auth.dto.LoginRequest;
import com.ricozknow.auth.dto.LoginResponse;
import com.ricozknow.auth.dto.LoginResult;
import com.ricozknow.auth.dto.RegisterRequest;
import com.ricozknow.auth.dto.RegisterResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * The refresh token is set as an httpOnly, Secure, SameSite=Strict cookie —
 * never returned in a JSON body — so it's inaccessible to JavaScript (no XSS
 * exfiltration path) for a credential that's long-lived and thus a higher-
 * value target than the short-lived access token. SameSite=Strict means the
 * cookie is only ever sent on same-site requests, which is the CSRF
 * mitigation for these two endpoints specifically (the rest of the API stays
 * CSRF-exempt because it's pure bearer-token auth with no cookies involved —
 * see SecurityConfig's csrf().disable() and its comment).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "rk_refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;

    @Value("${ricozknow.security.jwt.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    @Value("${ricozknow.security.jwt.refresh-cookie-secure}")
    private boolean refreshCookieSecure;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        LoginResult result = authService.login(request, clientIp(httpRequest));
        setRefreshCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.response());
    }

    /**
     * Silent refresh: the frontend calls this when an access token is about
     * to expire (or has just expired), using the httpOnly cookie the browser
     * sends automatically — no refresh token ever touches frontend JavaScript.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String presented = readRefreshCookie(httpRequest);
        if (presented == null) {
            throw new AuthException("No refresh token presented");
        }
        LoginResult result = authService.refresh(presented);
        setRefreshCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(readRefreshCookie(httpRequest));
        clearRefreshCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(refreshCookieSecure) // requires HTTPS; see ricozknow.security.jwt.refresh-cookie-secure
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofDays(refreshTokenTtlDays))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Strict").path(REFRESH_COOKIE_PATH).maxAge(0).build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    // Password reset flow (request + confirm) lands alongside email delivery
    // infrastructure in a later phase; endpoints intentionally omitted here.
}