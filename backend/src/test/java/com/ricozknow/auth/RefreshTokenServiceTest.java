package com.ricozknow.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository);
        ReflectionTestUtils.setField(service, "refreshTokenTtlDays", 14L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void issueProducesAUniquePlaintextTokenNeverStoredInPlaintext() {
        String token = service.issue(tenantId, userId);

        assertThat(token).isNotBlank();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(token); // never stored as-is
        assertThat(captor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void rotateRevokesThePresentedTokenAndIssuesANewOne() {
        RefreshToken stored = activeToken();
        String plaintext = "the-plaintext-token";
        // Simulate the hash the service would have computed at issue time.
        String hash = (String) ReflectionTestUtils.invokeMethod(service, "hash", plaintext);
        stored.setTokenHash(hash);
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        RefreshTokenService.RotationResult result = service.rotate(plaintext);

        assertThat(stored.getRevokedAt()).isNotNull(); // old token is dead
        assertThat(result.newPlaintextToken()).isNotEqualTo(plaintext); // a genuinely new token
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void rotateRejectsAnAlreadyRevokedToken() {
        RefreshToken stored = activeToken();
        stored.setRevokedAt(java.time.Instant.now());
        String hash = (String) ReflectionTestUtils.invokeMethod(service, "hash", "stolen-token");
        stored.setTokenHash(hash);
        when(repository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        // This is the reuse-detection property: once a token has been rotated
        // away from (revoked), presenting it again — e.g. a thief replaying a
        // stolen token after the legitimate holder already refreshed — fails.
        assertThatThrownBy(() -> service.rotate("stolen-token")).isInstanceOf(AuthException.class);
    }

    @Test
    void rotateRejectsAnUnknownToken() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate("never-issued")).isInstanceOf(AuthException.class);
    }

    private RefreshToken activeToken() {
        RefreshToken token = new RefreshToken();
        token.setTenantId(tenantId);
        token.setUserId(userId);
        token.setExpiresAt(java.time.Instant.now().plusSeconds(3600));
        return token;
    }
}