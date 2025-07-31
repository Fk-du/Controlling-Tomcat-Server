package com.fkadu.Controlling_Tomcat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;


import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final TomcatService tomcatService;

    public InlineKeyboardMarkup createAppMenu() {
        List<String> rawApps = tomcatService.listRawAppLines(); // method returns raw app lines
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

//        // ➕ Add Deploy WAR row
//        List<InlineKeyboardButton> deployRow = new ArrayList<>();
//        deployRow.add(buildButton("📦 Deploy WAR", "deploy_war"));
//        rows.add(deployRow);

        return new InlineKeyboardMarkup(rows);
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

    public InlineKeyboardMarkup createUserAppMenu() {
        List<String> rawApps = tomcatService.listRawAppLines(); // e.g., /sample:running:...:...
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String line : rawApps) {
            if (!line.contains(":")) continue;

            String[] parts = line.split(":");
            if (parts.length < 2) continue;

            String path = parts[0].trim();           // e.g., /sample
            String status = parts[1].trim();         // e.g., running

            String appName = path.startsWith("/") ? path.substring(1) : path;
            if (appName.isBlank()) continue; // 🚨 Skip empty app names

            String statusText = status.equalsIgnoreCase("running") ? "✅ Running" : "⛔ Stopped";

            List<InlineKeyboardButton> row = List.of(
                    buildButton(appName, "noop:" + appName),
                    buildButton(statusText, "status:" + appName)
            );
            rows.add(row);
        }

        return new InlineKeyboardMarkup(rows);
    }

    private InlineKeyboardButton buildButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}
