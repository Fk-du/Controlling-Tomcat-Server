package com.fkadu.Controlling_Tomcat.bot;

import com.fkadu.Controlling_Tomcat.model.LogEntry;
import com.fkadu.Controlling_Tomcat.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final UserService userService;


    @Autowired
    private TomcatService tomcatService;

    @Autowired
    private MenuService menuService;

    private final TomcatJmxService tomcatJmxService;
    private final TomcatControlService tomcatControlService;
    private final LogService logService;

    private final Map<Long, Boolean> loginStates = new HashMap<>();
    private final Map<String, String> tempUsernames = new HashMap<>();
    private final Map<String, Boolean> loggedInUsers = new HashMap<>();

    private final Map<Long, String> registrationStates = new HashMap<>();
    private final Map<Long, Contact> contactBuffer = new HashMap<>();
    private final Map<Long, String> passwordBuffer = new HashMap<>();



    public TelegramBot(
            UserService userService,
            TomcatJmxService tomcatJmxService,
            TomcatControlService tomcatControlService,
            LogService logService,
            SessionService sessionService) {
        this.logService = logService;

        this.botUsername = botUsername;
        this.botToken = botToken;
        this.userService = userService;
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
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            Long chatId = msg.getChatId();

            if (msg.hasText()) {
                String text = msg.getText().trim();

                if (text.equalsIgnoreCase("/start")) {
                    if (!userService.existsBychatId(chatId)) {
                        sendText(chatId.toString(), "ℹ️ 👋 Welcome! You are not registered yet. Use /register to sign up.");
                    } else {
                        sendText(chatId.toString(), "ℹ️ 👋 Welcome! You are already registered. Use /login to continue.");
                    }
                    return;
                }

                if (text.equalsIgnoreCase("/register")) {
                    if (userService.existsBychatId(chatId)) {
                        logService.saveLog("INFO", "User with chatId " + chatId + " started registration.");
                        sendText(chatId.toString(), "ℹ️ You are already registered.");
                    } else {
                        sendRequestContactButton(chatId);
                        registrationStates.put(chatId, "awaiting_contact");
                    }
                    return;
                }

                if ("awaiting_password".equals(registrationStates.get(chatId))) {
                    if (text.length() < 4) {
                        sendText(chatId.toString(), "⚠️ Password too short. Try again:");
                        return;
                    }
                    removePassword(text, update, chatId);

                    passwordBuffer.put(chatId, text);
                    registrationStates.put(chatId, "awaiting_password_confirm");
                    sendText(chatId.toString(), "🔁 Please retype your password to confirm:");
                    return;
                }

                if ("awaiting_password_confirm".equals(registrationStates.get(chatId))) {
                    String originalPassword = passwordBuffer.get(chatId);
                    removePassword(text, update, chatId);
                    if (!text.equals(originalPassword)) {
                        sendText(chatId.toString(), "❌ Passwords do not match. Please retype your password:");

                        return;
                    }

                    Contact contact = contactBuffer.get(chatId);
                    if (contact == null) {
                        sendText(chatId.toString(), "❗ Contact info missing. Please /register again.");
                        registrationStates.remove(chatId);
                        return;
                    }

                    userService.registerUser(contact, chatId, text);

                    // Cleanup
                    registrationStates.remove(chatId);
                    contactBuffer.remove(chatId);
                    passwordBuffer.remove(chatId);

                    KeyboardButton loginButton = new KeyboardButton("🔐 Login");
                    KeyboardRow row = new KeyboardRow();
                    row.add(loginButton);

                    ReplyKeyboardMarkup loginKeyboard = ReplyKeyboardMarkup.builder()
                            .keyboard(List.of(row))
                            .resizeKeyboard(true)
                            .oneTimeKeyboard(true)
                            .build();

                    SendMessage doneMessage = SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("✅ Registration complete.")
                            .replyMarkup(loginKeyboard)
                            .build();

                    try {
                        execute(doneMessage);
                    } catch (TelegramApiException e) {
                        logService.saveLog("ERROR", "Exception: " + e.getMessage());
                        e.printStackTrace();
                    }

                    logService.saveLog("INFO", "User with chatId " + chatId + " completed registration.");
                    return;
                }

                if (text.equalsIgnoreCase("/login") || text.equals("🔐 Login")) {
                    if (!userService.existsBychatId(chatId)) {
                        logService.saveLog("INFO", "User with chatId " + chatId + " initiated login.");
                        sendText(chatId.toString(), "❌ You are not registered. Use /register first.");
                        return;
                    }
                    if (loggedInUsers.getOrDefault(chatId.toString(), false)) {
                        sendText(chatId.toString(), "✅ You are already logged in.");
                        return;
                    }

                    loginStates.put(chatId, true);
                    sendText(chatId.toString(), "🔐 Please enter your password:");
                    return;
                }

                if (loginStates.getOrDefault(chatId, false)) {
                    String password = text;
                    boolean success = userService.verifyPassword(chatId, password);

                    if (success) {
                        loggedInUsers.put(chatId.toString(), true);
                        loginStates.remove(chatId);

                        removePassword(text, update, chatId);

                        KeyboardRow row1 = new KeyboardRow();
                        row1.add(new KeyboardButton("Server"));
                        row1.add(new KeyboardButton("Apps"));

                        KeyboardRow row2 = new KeyboardRow();
                        row2.add(new KeyboardButton("Log"));
                        row2.add(new KeyboardButton("Logout"));

                        ReplyKeyboardMarkup mainMenuKeyboard = ReplyKeyboardMarkup.builder()
                                .keyboard(List.of(row1, row2))
                                .resizeKeyboard(true)
                                .oneTimeKeyboard(false)
                                .build();

                        logService.saveLog("INFO", "User with chatId " + chatId + " logged in successfully.");

                        sendTextWithKeyboard(chatId.toString(), "✅ Login successful. Welcome!", mainMenuKeyboard);

                    } else {
                        int passwordMessageId = update.getMessage().getMessageId(); // get the message ID

                        // Delete the password message
                        DeleteMessage delete = new DeleteMessage(chatId.toString(), passwordMessageId);
                        try {
                            execute(delete);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sendText(chatId.toString(), "❌ Incorrect password. Try again.");
                        logService.saveLog("WARN", "Failed login attempt for chatId " + chatId);
//                        loginStates.remove(chatId);
                    }
                    return;
                }

                if (text.equalsIgnoreCase("/logout") || text.equalsIgnoreCase("Logout")) {
                    logService.saveLog("INFO", "User with chatId " + chatId + " logged out.");
                    loggedInUsers.remove(chatId.toString());

                    KeyboardButton loginButton = new KeyboardButton("🔐 Login");
                    KeyboardRow row = new KeyboardRow();
                    row.add(loginButton);

                    ReplyKeyboardMarkup loginKeyboard = ReplyKeyboardMarkup.builder()
                            .keyboard(List.of(row))
                            .resizeKeyboard(true)
                            .oneTimeKeyboard(true)
                            .build();

                    sendTextWithKeyboard(chatId.toString(), "🔒 You have been logged out.", loginKeyboard);
                    return;
                }

                boolean isLoggedIn = loggedInUsers.containsKey(chatId.toString());



                if (text.equalsIgnoreCase("Apps")) {
                    if (!isLoggedIn) {
                        sendText(chatId.toString(), "❗ Please login to access the Apps menu.");
                        return;
                    }
                    sendCreateAppMenu(chatId);
                } else if (text.equalsIgnoreCase("Server")) {
                    if (!isLoggedIn) {
                        sendText(chatId.toString(), "❗ Please login to access the Server menu.");
                        return;
                    }
                    sendCreateMonitoringMenu(chatId);
                } else if (text.equalsIgnoreCase("Log") || text.equals("📜 Log")) {
                    if (!isLoggedIn) {
                        sendText(chatId.toString(), "❗ Please login to view logs.");
                        return;
                    }
                    sendLatestLogs(chatId.toString());
                } else

                sendText(chatId.toString(), "❓ Unrecognized command. Try /register or /login.");
                return;
            }

    // Handle contact sharing
            if (msg.hasContact()) {
                Contact contact = msg.getContact();
                String normalizedPhone = userService.normalizePhone(contact.getPhoneNumber());

                if (!userService.isAllowedPhone(normalizedPhone)) {
                    sendText(chatId.toString(), "❌ You are not authorized to register.");
                    return;
                }

                contactBuffer.put(chatId, contact);
                registrationStates.put(chatId, "awaiting_password");
                sendText(chatId.toString(), "📌 Enter a password to complete your registration:");
                return;
            }

        }

        // Handle callback queries (button presses)
        if (update.hasCallbackQuery()) {
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            String callback = update.getCallbackQuery().getData();

            if (!loggedInUsers.getOrDefault(chatId, false)) {
                sendText(chatId, "🔒 Please /login first.");
                return;
            }

            String result = null;

            // Tomcat app controls
            if (callback.startsWith("start:")) {
                String appName = callback.substring("start:".length());
                logService.saveLog("INFO", "User with chatId " + chatId + " started app: " + appName);
                result = tomcatService.startApp(callback.substring("start:".length()));
            } else if (callback.startsWith("stop:")) {
                String appName = callback.substring("start:".length());
                logService.saveLog("INFO", "User with chatId " + chatId + " started app: " + appName);
                result = tomcatService.stopApp(callback.substring("stop:".length()));
            }

            // Monitoring controls
            else if (callback.equals("monitoring:start")) {
                logService.saveLog("INFO", "User with chatId " + chatId + " Started the Server.");
                result = tomcatControlService.startServer();
            } else if (callback.equals("monitoring:stop")) {
                logService.saveLog("INFO", "User with chatId " + chatId + " Stoped the Server.");
                result = tomcatControlService.stopServer();
            } else if (callback.equals("monitoring:restart")) {
                logService.saveLog("INFO", "User with chatId " + chatId + " Restarted the Server.");
                result = tomcatJmxService.restartServer();
            } else if (callback.equals("monitoring:status")) {
                logService.saveLog("INFO", "User with chatId " + chatId + " checked status of the Server.");
                result = tomcatJmxService.getServerStatus();
            } else {
                result = "❓ Unknown action.";
            }

            if (result != null) {
                sendText(chatId, result);
            }


        }
    }


    private void sendCreateAppMenu(Long chatId) {
        InlineKeyboardMarkup appMenu = menuService.createAppMenu();

        sendMessageWithKeyboard(Long.toString(chatId), "Tomcat Apps Control Panel:", appMenu);
    }

    private void sendCreateMonitoringMenu(Long chatId) {
        InlineKeyboardMarkup monitoringMenu = menuService.createMonitoringMenu();

        sendMessageWithKeyboard(Long.toString(chatId), "Tomcat Monitoring Control Panel:", monitoringMenu);
    }


    public void sendText(String chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            execute(msg);
        } catch (Exception e) {
            logService.saveLog("ERROR", "Exception: " + e.getMessage());
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
            logService.saveLog("ERROR", "Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendRequestContactButton(Long chatId) {
        KeyboardButton contactButton = KeyboardButton.builder()
                .text("📱 Share Contact")
                .requestContact(true)
                .build();

        KeyboardRow row = new KeyboardRow();
        row.add(contactButton);

        ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();


        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Please share your contact to register.")
                .replyMarkup(markup)
                .build();


        try {
            execute(message);
            registrationStates.put(chatId, "awaiting_contact");
        } catch (TelegramApiException e) {
            logService.saveLog("ERROR", "Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendTextWithKeyboard(String chatId, String message, ReplyKeyboardMarkup keyboard) {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .replyMarkup(keyboard)
                .build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            logService.saveLog("ERROR", "Exception: " + e.getMessage());
            e.printStackTrace(); // Log properly in production
        }
    }


    private void sendLatestLogs(String chatId) {
        List<LogEntry> logs = logService.getLatestLogs();

        if (logs.isEmpty()) {
            sendText(chatId, "No logs found.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (LogEntry log : logs) {
            sb.append("[").append(log.getTimestamp()).append("] ")
                    .append(log.getLevel()).append(" - ")
                    .append(log.getMessage()).append("\n");
        }

        String message = sb.toString();

        if (message.length() > 4000) {
            message = message.substring(message.length() - 4000);
        }

        sendText(chatId, "📜 Latest 100 Logs:\n" + message);
    }

    private void removePassword(String text, Update update, Long chatId){
        String password = text;
        int passwordMessageId = update.getMessage().getMessageId(); // get the message ID

        // Delete the password message
        DeleteMessage delete = new DeleteMessage(chatId.toString(), passwordMessageId);
        try {
            execute(delete);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
