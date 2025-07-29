package com.fkadu.Controlling_Tomcat.service;

import com.fkadu.Controlling_Tomcat.dto.RegisterRequest;
import com.fkadu.Controlling_Tomcat.model.User;
import com.fkadu.Controlling_Tomcat.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(String username, String password) {
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

    public String registerAdmin(RegisterRequest request) {
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

    public Set<String> loginAndGetRoles(String username, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user.getRoles(); // This returns a Set<String>
            }
        }
        return Set.of(); // Return empty set if login fails
    }

}
