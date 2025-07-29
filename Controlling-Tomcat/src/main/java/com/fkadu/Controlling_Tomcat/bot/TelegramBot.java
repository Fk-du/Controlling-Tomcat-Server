package com.fkadu.Controlling_Tomcat.bot;

import com.fkadu.Controlling_Tomcat.service.*;
import com.fkadu.Controlling_Tomcat.utils.TelegramBotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String botToken;
    private final UserService userService;


    @Autowired
    private TomcatService tomcatService;

    @Autowired
    private MenuService menuService;

    private final TomcatJmxService tomcatJmxService;
    private final TomcatControlService tomcatControlService;
    private final TelegramBotUtils telegramBotUtils;

    private final Map<String, String> userStates = new HashMap<>();
    private final Map<String, String> tempUsernames = new HashMap<>();
    private final Map<String, Boolean> loggedInUsers = new HashMap<>();
    private final Map<Long, String> userFilePaths = new HashMap<>();

    public TelegramBot(
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.token}") String botToken, UserService userService,
            TomcatJmxService tomcatJmxService,
            TomcatControlService tomcatControlService, TelegramBotUtils telegramBotUtils) {
        this.botUsername = botUsername;
        this.botToken = botToken;
        this.userService = userService;
        this.tomcatJmxService = tomcatJmxService;
        this.tomcatControlService = tomcatControlService;
        this.telegramBotUtils = telegramBotUtils;
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
        // Handle incoming text messages
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();
            onTextCommand(chatId, text);
        }

        // Handle uploaded .war files
        if (update.hasMessage() && update.getMessage().hasDocument()) {
            Long chatIdLong = update.getMessage().getChatId();
            String chatId = chatIdLong.toString();
            Document document = update.getMessage().getDocument();

            if ("awaiting_war_file_upload".equals(userStates.get(chatId))) {
                if (document.getFileName().endsWith(".war")) {
                    userStates.remove(chatId);
                    sendText(chatIdLong.toString(), "⏳ Uploading WAR file...");

                    try {
                        String fileId = document.getFileId();
                        String filePath = telegramBotUtils.downloadFile(fileId);

                        // 🧠 Save the file path temporarily
                        userFilePaths.put(chatIdLong, filePath);

                        // ⌨️ Ask the user for the app name
                        sendText(chatIdLong.toString(), "✅ WAR file uploaded. Now send the app name (e.g., SampleWebApp):");
                        userStates.put(chatId, "awaiting_app_name");

                    } catch (Exception e) {
                        sendText(chatIdLong.toString(), "❌ Failed to download WAR file: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    sendText(chatIdLong.toString(), "⚠️ Invalid file type. Please upload a valid `.war` file.");
                }
            }
        }


        // Handle button presses (callbacks)
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

            // WAR Deployment Trigger
            else if (callback.equals("deploy_war")) {
                userStates.put(chatId, "awaiting_war_file_upload");
                sendText(chatId, "📤 Please upload a `.war` file to deploy.");
            }

            // If an action was processed, send result and menu
            if (result != null) {
                sendText(chatId, result);
                sendFullMenu(chatId);
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
                String result = userService.register(username, input);
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
                var roles = userService.loginAndGetRoles(username, input);

                if (!roles.isEmpty()) {
                    loggedInUsers.put(chatId, true);
                    userStates.remove(chatId);

                    if (roles.contains("ADMIN")) {
                        sendText(chatId, "✅ Login successful! Welcome, admin.\n\n🛠 Opening control panel...");
                        sendFullMenu(chatId);
                    } else if (roles.contains("USER")) {
                        SendMessage message = new SendMessage();
                        message.setChatId(chatId);
                        message.setText("👋 Welcome! Here's your application status:");

                        InlineKeyboardMarkup userMenu = menuService.createUserAppMenu();
                        message.setReplyMarkup(userMenu);

                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            e.printStackTrace(); // or use proper logging
                        }
                    }
                    else {
                        sendText(chatId, "✅ Login successful, but role unrecognized.");
                    }
                } else {
                    sendText(chatId, "❌ Invalid username or password.");
                    userStates.remove(chatId);
                }
            }

            case "deploy_war" -> {
                userStates.put(chatId, "awaiting_war_file_upload");
                sendText(chatId, "📤 Please upload your `.war` file to deploy.");
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
