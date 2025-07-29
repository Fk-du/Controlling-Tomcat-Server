package com.fkadu.Controlling_Tomcat.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class TelegramBotUtils {

    @Value("${telegram.bot.token}")
    private  String BOT_TOKEN;



    public String downloadFile(String fileId) throws Exception {
        String getFileUrl = "https://api.telegram.org/bot" + BOT_TOKEN + "/getFile?file_id=" + fileId;
        String filePath;

        HttpURLConnection conn = (HttpURLConnection) new URL(getFileUrl).openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();

        InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
        String json = new String(is.readAllBytes());

        if (responseCode != 200) {
            throw new IOException("Telegram getFile failed: " + json);
        }

        filePath = json.split("\"file_path\":\"")[1].split("\"")[0];

        String fileUrl = "https://api.telegram.org/file/bot" + BOT_TOKEN + "/" + filePath;
        String outputFilePath = "C:/temp/" + filePath.substring(filePath.lastIndexOf('/') + 1);

        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }

        return outputFilePath;
    }


}
