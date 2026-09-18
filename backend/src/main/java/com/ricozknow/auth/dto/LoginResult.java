package com.ricozknow.auth.dto;

/**
 * The refresh token travels as an httpOnly cookie, never in the JSON body —
 * see AuthController. This record only exists to carry both pieces from
 * AuthService to the controller in one call.
 */
public record LoginResult(LoginResponse response, String refreshToken) {
}
