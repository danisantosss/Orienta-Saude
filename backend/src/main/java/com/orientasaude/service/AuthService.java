package com.orientasaude.service;

import com.orientasaude.dto.request.LoginRequest;
import com.orientasaude.dto.request.RegisterRequest;
import com.orientasaude.dto.response.AuthResponse;
import com.orientasaude.exception.EmailAlreadyRegisteredException;
import com.orientasaude.model.User;
import com.orientasaude.repository.UserRepository;
import com.orientasaude.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for user registration and authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new user.
     * Checks that the email is unique and stores the password with BCrypt.
     */
    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .active(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * Authenticates a user and returns the JWT.
     * Returns a generic message on invalid credentials (does not reveal whether the email exists).
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!user.getActive()) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
    }
}
