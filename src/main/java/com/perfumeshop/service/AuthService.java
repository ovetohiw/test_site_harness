package com.perfumeshop.service;

import com.perfumeshop.domain.user.User;
import com.perfumeshop.dto.auth.AuthRequest;
import com.perfumeshop.dto.auth.AuthResponse;
import com.perfumeshop.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String login = normalizeLogin(request.login());
        if (userRepository.existsByLogin(login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login already taken");
        }

        User user = new User(login, passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return issueToken(login);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String login = normalizeLogin(request.login());
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return issueToken(login);
    }

    private AuthResponse issueToken(String login) {
        String token = jwtService.createToken(login);
        return AuthResponse.bearer(token, jwtService.expirationMs(), login);
    }

    private static String normalizeLogin(String login) {
        return login.trim().toLowerCase();
    }
}
