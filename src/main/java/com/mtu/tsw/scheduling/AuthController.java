package com.mtu.tsw.scheduling;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mtu.tsw.scheduling.exceptions.ExceptionMapper.*;
import com.mtu.tsw.scheduling.security.JwtService;
import com.mtu.tsw.scheduling.user.UserEntity;
import com.mtu.tsw.scheduling.user.UserRepository;

@RestController
@RequestMapping("/api/auth")
/**
 * Controller for API endpoints related to authentication. Note that these endpoints are not protected by the security filter, and can be accessed by an unauthorized user.
 */
public class AuthController {

    public record AuthResponse(String token) {}
    public record AuthRequest(String username, String password) {}

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public AuthController(JwtService jwtService, UserDetailsService user, UserRepository userRepo, PasswordEncoder encoder) {
        this.jwtService = jwtService;
        this.userDetailsService = user;
        this.encoder = encoder;
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        
        var details = userDetailsService.loadUserByUsername(req.username);
        
        if (details == null || !encoder.matches(req.password, details.getPassword())) {
            throw new InvalidAuthException("Invalid credentials");
        }

        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(req.username)));
    }

    @PostMapping("/create")
    public ResponseEntity<AuthResponse> create(@RequestBody AuthRequest req) {

        // username must be a string with at least 3 alphanumeric or '.' characters
        if (req.username == null || req.username.length() < 3 || !req.username.matches("^[a-zA-Z0-9.]+$")) {
            throw new MalformedArgumentException("Malformed username");
        }

        // password must be a string with at least 8 characters
        if (req.password == null || req.password.length() < 8) {
            throw new MalformedArgumentException("Malformed password");
        }

        var details = userDetailsService.loadUserByUsername(req.username);

        if (details != null) {
            throw new ResourceConflictException("Username taken");
        }

        userRepo.save(new UserEntity(req.username, encoder.encode(req.password)));

        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(req.username)));
    }

}
