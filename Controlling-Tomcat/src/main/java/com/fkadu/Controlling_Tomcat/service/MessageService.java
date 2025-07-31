package com.fkadu.Controlling_Tomcat.service;

import com.fkadu.Controlling_Tomcat.bot.TelegramBot;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Service
public class MessageService {

    private final ObjectProvider<TelegramBot> botProvider;

    @Autowired
    public MessageService(ObjectProvider<TelegramBot> botProvider) {
        this.botProvider = botProvider;
    }

    public void sendText(String chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();

        try {
            botProvider.getObject().execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

