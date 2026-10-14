package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO returned after a successful login.
 * Contains the JWT and its expiration time.
 */
@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private long expiresIn;
}
