package com.fkadu.Controlling_Tomcat.repository;

import com.fkadu.Controlling_Tomcat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String username);

    boolean existsBychatId(Long chatId);

    Optional<User> findByChatId(Long chatId);
}
