package com.fkadu.Controlling_Tomcat.service;

import com.fkadu.Controlling_Tomcat.dto.RegisterRequest;
import com.fkadu.Controlling_Tomcat.model.User;
import com.fkadu.Controlling_Tomcat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public String registerUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return "❌ Username already taken!";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.getRoles().add("USER");

        userRepository.save(user);
        return "✅ User registered successfully!";
    }

    public String adminRegister(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return "❌ Username already taken!";
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.getRoles().add("ADMIN");

        userRepository.save(user);
        return "✅ Admin registered successfully!";
    }

    public boolean authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(false);
    }

    public Set<String> getUserRoles(String username) {
        return userRepository.findByUsername(username)
                .map(User::getRoles)
                .orElse(Set.of());
    }
}
