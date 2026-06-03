package br.com.ecowinds.service;

import br.com.ecowinds.dto.login.LoginRequest;
import br.com.ecowinds.dto.login.RegisterRequest;
import br.com.ecowinds.dto.login.TokenResponse;
import br.com.ecowinds.model.User;
import br.com.ecowinds.repository.UserRepository;
import br.com.ecowinds.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenResponse login(LoginRequest loginRequest) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var springUser = (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        User authenticatedUser = userRepository.findByEmail(springUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco de dados."));

        String token = jwtService.generateToken(authenticatedUser);

        return new TokenResponse(token);
    }

    public void register (RegisterRequest registerRequest) {
        Optional<User> existingUser = userRepository.findByEmail(registerRequest.email());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("This email is already in use.");
        }

        String encryptedPassword = passwordEncoder.encode(registerRequest.password());

        User newUser = new User(
                registerRequest.name(),
                registerRequest.email(),
                encryptedPassword,
                registerRequest.role()
        );

        userRepository.save(newUser);
    }
}
