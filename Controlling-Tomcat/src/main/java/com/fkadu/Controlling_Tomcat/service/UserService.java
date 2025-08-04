package com.fkadu.Controlling_Tomcat.service;

import com.fkadu.Controlling_Tomcat.config.AllowedPhonesConfig;
import com.fkadu.Controlling_Tomcat.model.User;
import com.fkadu.Controlling_Tomcat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Contact;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final AllowedPhonesConfig allowedPhonesConfig;

    @Autowired
    private SessionService sessionService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, MessageService messageService, AllowedPhonesConfig allowedPhonesConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.messageService = messageService;
        this.allowedPhonesConfig = allowedPhonesConfig;
    }

    public boolean existsBychatId(Long chatId){
        return userRepository.existsBychatId(chatId);
    }

    public boolean isAllowedPhone(String phone){
        Set<String> allowedPhones = allowedPhonesConfig.getPhones();
        return allowedPhones.contains(phone);
    }

    public void registerUser(Contact contact, Long chatId, String password){

        User user = new User();
        user.setChatId(chatId);
        user.setFirstName(contact.getFirstName());
        user.setLastName(contact.getLastName());
        user.setPhone(normalizePhone(contact.getPhoneNumber()));
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
    }

    public boolean verifyPassword(Long chatId, String rawPassword) {
        Optional<User> optional = userRepository.findByChatId(chatId);
        if (optional.isEmpty()) return false;

        User user = optional.get();
        return passwordEncoder.matches(rawPassword, user.getPassword()); // if you're using BCrypt
    }


    public String normalizePhone(String phone){
        if (phone.startsWith("0")) return "+251" + phone.substring(1);
        return phone.startsWith("+")? phone : "+" + phone;
    }

}


