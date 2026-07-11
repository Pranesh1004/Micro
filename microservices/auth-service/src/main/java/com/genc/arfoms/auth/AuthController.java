package com.genc.arfoms.auth;

import com.genc.arfoms.auth.model.User;
import com.genc.arfoms.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.genc.arfoms.auth.exception.PasswordIncorrectException;
import com.genc.arfoms.auth.exception.UserAlreadyExistsException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        logger.info("Login attempt for user: {}", username);

        if (username == null || password == null) {
            logger.warn("Login failed: Username or password missing.");
            throw new IllegalArgumentException("Username and password required");
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            User user = userOpt.get();
            String token = jwtUtils.generateToken(username, user.getRole());
            logger.info("Login successful for user: {}, role: {}", username, user.getRole());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole(),
                    "name", user.getName()
            ));
        }

        logger.warn("Login failed: Invalid credentials for user: {}", username);
        throw new PasswordIncorrectException("Invalid credentials");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User newUser) {
        logger.info("Registration attempt for username: {}", newUser.getUsername());
        if (newUser.getUsername() == null || newUser.getPassword() == null || newUser.getRole() == null || newUser.getName() == null) {
            logger.warn("Registration failed: Missing required fields.");
            throw new IllegalArgumentException("All fields are required (username, password, name, role)");
        }

        if (userRepository.findByUsername(newUser.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username '{}' already exists.", newUser.getUsername());
            throw new UserAlreadyExistsException("Username already exists");
        }

        // Standardize roles for consistency in the frontend mapping
        String role = newUser.getRole().toLowerCase().trim();
        switch (role) {
            case "admin":
            case "reservation agent":
            case "ground staff":
            case "flight dispatcher":
            case "crew scheduler":
            case "loyalty manager":
                newUser.setRole(role);
                break;
            default:
                logger.warn("Registration failed: Invalid role '{}' specified.", role);
                throw new IllegalArgumentException("Invalid role specified");
        }

        User savedUser = userRepository.save(newUser);
        logger.info("User '{}' registered successfully with ID: {}", savedUser.getUsername(), savedUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully",
                "userId", savedUser.getId()
        ));
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        logger.info("Validating token...");
        jwtUtils.validateToken(token);
        logger.info("Token validated successfully.");
        return "Token is valid";
    }
}
