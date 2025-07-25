package com.fkadu.Controlling_Tomcat.bot;

import com.fkadu.Controlling_Tomcat.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final AuthService authService;

    @Autowired
    private TomcatService tomcatService;

    @Autowired
    private MenuService menuService;

    private final TomcatJmxService tomcatJmxService;
    private final TomcatControlService tomcatControlService;

    private final Map<String, String> userStates = new HashMap<>();
    private final Map<String, String> tempUsernames = new HashMap<>();
    private final Map<String, Boolean> loggedInUsers = new HashMap<>();

    public TelegramBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken,
            AuthService authService,
            TomcatJmxService tomcatJmxService,
            TomcatControlService tomcatControlService) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.authService = authService;
        this.tomcatJmxService = tomcatJmxService;
        this.tomcatControlService = tomcatControlService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();
            onTextCommand(chatId, text);
        }

        if (update.hasCallbackQuery()) {
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            String callback = update.getCallbackQuery().getData();

            if (!loggedInUsers.getOrDefault(chatId, false)) {
                sendText(chatId, "🔒 Please /login first.");
                return;
            }

            String result = null;

            // App Control
            if (callback.startsWith("start:")) {
                String path = callback.substring("start:".length());
                result = tomcatService.startApp(path);
            } else if (callback.startsWith("stop:")) {
                String path = callback.substring("stop:".length());
                result = tomcatService.stopApp(path);
            } else if (callback.equals("serverinfo")) {
                result = tomcatService.getServerInfo();
            }

            // Monitoring Controls
            else if (callback.equals("monitoring:start")) {
                result = tomcatControlService.startServer();
            } else if (callback.equals("monitoring:stop")) {
                result = tomcatControlService.stopServer();
            } else if (callback.equals("monitoring:restart")) {
                result = tomcatJmxService.restartServer();
            } else if (callback.equals("monitoring:status")) {
                result = tomcatJmxService.getServerStatus();
            }

            // If a recognized action occurred, send result and show full menu again
            if (result != null) {
                sendText(chatId, result);
                sendFullMenu(chatId); // 👈 always show both panels again
            }
        }
    }

    private void sendFullMenu(String chatId) {
        InlineKeyboardMarkup appMenu = menuService.createAppMenu();
        InlineKeyboardMarkup monitoringMenu = menuService.createMonitoringMenu();

        List<List<InlineKeyboardButton>> combined = new ArrayList<>();
        combined.addAll(appMenu.getKeyboard());
        combined.addAll(monitoringMenu.getKeyboard());

        InlineKeyboardMarkup fullMenu = new InlineKeyboardMarkup();
        fullMenu.setKeyboard(combined);

        sendMessageWithKeyboard(chatId, "📋 Tomcat Control Panel (Apps + Monitoring-Server):", fullMenu); // ✅ chatId as String
    }


    private void onTextCommand(String chatId, String text) {
        switch (text.toLowerCase()) {
            case "/start" -> sendText(chatId, "Welcome! Use /register or /login to proceed.");
            case "/register" -> {
                userStates.put(chatId, "register_username");
                sendText(chatId, "Enter username to register:");
            }
            case "/login" -> {
                userStates.put(chatId, "login_username");
                sendText(chatId, "Enter your username to login:");
            }
            case "/menu" -> {
                if (!loggedInUsers.getOrDefault(chatId, false)) {
                    sendText(chatId, "🔒 Please /login first.");
                    return;
                }

                // Combine both app menu and monitoring menu buttons
                InlineKeyboardMarkup appMenu = menuService.createAppMenu();
                InlineKeyboardMarkup monitoringMenu = menuService.createMonitoringMenu();

                // Merge both button lists
                List<List<InlineKeyboardButton>> combinedButtons = new ArrayList<>();
                combinedButtons.addAll(appMenu.getKeyboard());
                combinedButtons.addAll(monitoringMenu.getKeyboard());

                InlineKeyboardMarkup combinedMenu = new InlineKeyboardMarkup();
                combinedMenu.setKeyboard(combinedButtons);

                sendMessageWithKeyboard(chatId, "📋 Tomcat Control Panel (Apps + Monitoring):", combinedMenu);
            }
            default -> handleUserInput(chatId, text);
        }
    }

    private void handleUserInput(String chatId, String input) {
        String state = userStates.get(chatId);

        if (state == null) {
            sendText(chatId, "Type /start to begin.");
            return;
        }

        switch (state) {
            case "register_username" -> {
                tempUsernames.put(chatId, input);
                userStates.put(chatId, "register_password");
                sendText(chatId, "Enter password:");
            }
            case "register_password" -> {
                String username = tempUsernames.remove(chatId);
                String result = authService.registerUser(username, input);
                sendText(chatId, result);
                if (result.contains("success")) {
                    loggedInUsers.put(chatId, true);
                }
                userStates.remove(chatId);
            }
            case "login_username" -> {
                tempUsernames.put(chatId, input);
                userStates.put(chatId, "login_password");
                sendText(chatId, "Enter password:");
            }
            case "login_password" -> {
                String username = tempUsernames.remove(chatId);
                boolean authenticated = authService.authenticate(username, input);
                if (authenticated) {
                    sendText(chatId, "Login successful!");
                    loggedInUsers.put(chatId, true);
                    sendFullMenu(chatId);
                } else {
                    sendText(chatId, "Invalid username or password.");
                }
                userStates.remove(chatId);
            }
            default -> sendText(chatId, "Unknown command. Type /start to begin.");
        }
    }

    private void sendText(String chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithKeyboard(String chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();
        try {
            execute(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
