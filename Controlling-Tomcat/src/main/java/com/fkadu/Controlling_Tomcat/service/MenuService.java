package com.fkadu.Controlling_Tomcat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final TomcatService tomcatService;

    public InlineKeyboardMarkup createAppMenu() {
        List<String> rawApps = tomcatService.listRawAppLines(); // new method to return raw app lines
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String line : rawApps) {
            if (!line.contains(":")) continue; // skip invalid lines

            String[] parts = line.split(":");
            if (parts.length < 4) continue;

            String path = parts[0];           // e.g., /sample
            String status = parts[1];         // e.g., running or stopped

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(buildButton(path, "noop"));
            row.add(buildButton("🔴 Stop", "stop:" + path));
            row.add(buildButton("🟢 Start", "start:" + path));

            String statusSymbol = status.equalsIgnoreCase("running") ? "✅ Running" : "⛔ Stopped";
            row.add(buildButton(statusSymbol, "status:" + path));

            rows.add(row);
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup createMonitoringMenu() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                InlineKeyboardButton.builder().text("🟢 Start Server").callbackData("monitoring:start").build(),
                InlineKeyboardButton.builder().text("🔴 Stop Server").callbackData("monitoring:stop").build()
        ));
        rows.add(List.of(
                InlineKeyboardButton.builder().text("🔄 Restart Server").callbackData("monitoring:restart").build(),
                InlineKeyboardButton.builder().text("📊 Server Status").callbackData("monitoring:status").build()
        ));

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public void sendMonitoringMenu(String chatId, TelegramLongPollingBot bot) {
        InlineKeyboardMarkup keyboard = createMonitoringMenu();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("🛠 Server Monitoring Panel")
                .replyMarkup(keyboard)
                .build();

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardButton buildButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}
