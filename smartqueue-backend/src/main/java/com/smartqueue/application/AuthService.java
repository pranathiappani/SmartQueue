package com.smartqueue.application;

import com.smartqueue.domain.Role;
import com.smartqueue.domain.User;
import com.smartqueue.repository.UserRepository;
import com.smartqueue.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(String username, String rawPassword, String email, String phone, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User(username, passwordEncoder.encode(rawPassword), role != null ? role : Role.USER);
        user.setEmail(email);
        user.setPhone(phone);
        userRepository.save(user);
    }

    public String loginAndGenerateToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return jwtUtil.generateToken(user.getUsername(), user.getRole().name());
    }
}
