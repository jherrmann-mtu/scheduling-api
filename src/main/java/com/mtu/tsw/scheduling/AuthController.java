package com.mtu.tsw.scheduling;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mtu.tsw.scheduling.exceptions.ExceptionMapper.InvalidAuthException;
import com.mtu.tsw.scheduling.security.JwtService;

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
    private final PasswordEncoder encoder;

    public AuthController(JwtService jwtService, UserDetailsService user, PasswordEncoder encoder) {
        this.jwtService = jwtService;
        this.userDetailsService = user;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
        
        var details = userDetailsService.loadUserByUsername(req.username);
        
        if (details == null || !encoder.matches(req.password, details.getPassword())) {
            throw new InvalidAuthException("Invalid credentials");
        }

        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(req.username)));
    }
    
}
