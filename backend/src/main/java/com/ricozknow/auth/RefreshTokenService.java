package com.ricozknow.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Refresh tokens are high-entropy random strings, not user-chosen secrets —
 * unlike passwords, they don't need Argon2's deliberate slowness (which would
 * just waste CPU on every refresh call). A fast SHA-256 hash of the token is
 * sufficient: the security property that matters here is "the plaintext
 * token is never stored," not "brute-forcing the hash is expensive," since
 * the token itself already has far more entropy than any password.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;

    @Value("${ricozknow.security.jwt.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    /** @return the plaintext token — this is the only moment it ever exists outside the client. */
    @Transactional
    public String issue(UUID tenantId, UUID userId) {
        String plaintext = generateToken();
        RefreshToken token = new RefreshToken();
        token.setTenantId(tenantId);
        token.setUserId(userId);
        token.setTokenHash(hash(plaintext));
        token.setExpiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        repository.save(token);
        return plaintext;
    }

    /**
     * Validates the presented token, revokes it, and issues a fresh one
     * (rotation) — see RefreshToken's class doc for why rotation matters.
     *
     * @return the new plaintext token and the user/tenant it belongs to
     */
    @Transactional
    public RotationResult rotate(String presentedToken) {
        RefreshToken existing = repository.findByTokenHash(hash(presentedToken))
                .filter(RefreshToken::isActive)
                .orElseThrow(() -> new AuthException("Invalid or expired refresh token"));

        existing.setRevokedAt(Instant.now());
        String newToken = issue(existing.getTenantId(), existing.getUserId());
        return new RotationResult(existing.getTenantId(), existing.getUserId(), newToken);
    }

    @Transactional
    public void revoke(String presentedToken) {
        repository.findByTokenHash(hash(presentedToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
        });
    }

    /** Revokes every active refresh token for a user — used on password reset / forced logout. */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.findAll().stream()
                .filter(t -> t.getUserId().equals(userId) && t.isActive())
                .forEach(t -> t.setRevokedAt(Instant.now()));
    }

    private String generateToken() {
        byte[] bytes = new byte[32]; // 256 bits of entropy
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(token.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record RotationResult(UUID tenantId, UUID userId, String newPlaintextToken) {
    }
}
