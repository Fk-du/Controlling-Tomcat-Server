package com.fkadu.Controlling_Tomcat.service;

import com.fkadu.Controlling_Tomcat.model.User;
import com.fkadu.Controlling_Tomcat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;

    @Autowired
    private SessionService sessionService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,  MessageService messageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.messageService = messageService;
    }

    public String register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            return "❌ Username already taken!";
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.getRoles().add("ADMIN");

        userRepository.save(user);
        return "✅ User registered successfully! Type /login to login";
    }

//    public String registerAdmin(RegisterRequest request) {
//        if (userRepository.existsByUsername(request.getUsername())) {
//            return "❌ Username already taken!";
//        }
//        User user = new User();
//        user.setUsername(request.getUsername());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.getRoles().add("ADMIN");
//        userRepository.save(user);
//        return "✅ Admin registered successfully!";
//    }

    public Set<String> loginAndGetRoles(String chatId, String username, String password) {

        if (sessionService.isLoggedIn(chatId)) {

            messageService.sendText(chatId, "❌ You're already logged in. Use /logout to log out first.");
            return Set.of();
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                sessionService.login(chatId, username);
                return user.getRoles();
            }
        }
        return Set.of(); // Return empty set if login fails
    }

    public boolean logout(String chatId){
        if (sessionService.isLoggedIn(chatId)){
            sessionService.logout(chatId);
            return true;
        }
        return false;
    }

}


