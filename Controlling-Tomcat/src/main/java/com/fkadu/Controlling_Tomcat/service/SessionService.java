package com.fkadu.Controlling_Tomcat.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionService {
    private final Map<String, String> activeSession = new ConcurrentHashMap<>();

    public boolean isLoggedIn(String chatId){
        return activeSession.containsKey(chatId);
    }

    public void login(String chatId, String userName){
        activeSession.put(chatId, userName);
    }

    public void logout(String chatId){
        activeSession.remove(chatId);
    }

    public String getUsername(String chatId){
        return activeSession.get(chatId);
    }

}
