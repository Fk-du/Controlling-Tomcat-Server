package com.fkadu.Controlling_Tomcat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.openmbean.CompositeData;
import java.io.IOException;

@Service
public class TomcatJmxService {

    private final TomcatControlService tomcatControlService;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String JMX_URL;


    public TomcatJmxService(TomcatControlService tomcatControlService,
                            @Value("${JMX_URL}") String jmxUrl
                            ) {
        this.tomcatControlService = tomcatControlService;
        JMX_URL = jmxUrl;

    }

    private MBeanServerConnection connect() throws Exception {
        JMXServiceURL url = new JMXServiceURL(JMX_URL);
        JMXConnector connector = JMXConnectorFactory.connect(url);
        return connector.getMBeanServerConnection();
    }

    public String restartServer() {
        try {
            tomcatControlService.stopServer();
            Thread.sleep(5000); // Delay before restarting
            tomcatControlService.startServer();
            return "🔄 Server restarted.";
        } catch (Exception e) {
            return "❌ Failed to restart server: " + e.getMessage();
        }
    }

    public String getServerStatus() {
        try {
            MBeanServerConnection connection = connect();

            ObjectName serverObject = new ObjectName("Catalina:type=Server");
            String serverInfo = (String) connection.getAttribute(serverObject, "serverInfo");

            ObjectName runtimeObject = new ObjectName("java.lang:type=Runtime");
            Long uptimeMs = (Long) connection.getAttribute(runtimeObject, "Uptime");
            String uptime = formatDuration(uptimeMs);

            ObjectName threadPoolObject = new ObjectName("Catalina:name=\"http-nio-8080\",type=ThreadPool");
            Integer maxThreads = (Integer) connection.getAttribute(threadPoolObject, "maxThreads");
            Integer currentThreadsBusy = (Integer) connection.getAttribute(threadPoolObject, "currentThreadsBusy");

            ObjectName memoryObject = new ObjectName("java.lang:type=Memory");
            CompositeData heapMemoryUsage = (CompositeData) connection.getAttribute(memoryObject, "HeapMemoryUsage");
            long usedHeap = (Long) heapMemoryUsage.get("used");
            long maxHeap = (Long) heapMemoryUsage.get("max");

            return String.format("""
                    📊 Server is running.
                    Info: %s
                    Uptime: %s
                    Threads Busy: %d/%d
                    Heap Usage: %.1f MB / %.1f MB
                    """,
                    serverInfo, uptime, currentThreadsBusy, maxThreads, bytesToMB(usedHeap), bytesToMB(maxHeap));

        } catch (IOException e) {
            e.printStackTrace();
            return "🟥 Server is OFFLINE or unreachable.";
        } catch (Exception e) {
            return "⚠️ Failed to get server status: " + e.getMessage();
        }
    }

    private String formatDuration(long uptimeMs) {
        long seconds = uptimeMs / 1000 % 60;
        long minutes = uptimeMs / (1000 * 60) % 60;
        long hours = uptimeMs / (1000 * 60 * 60) % 24;
        long days = uptimeMs / (1000 * 60 * 60 * 24);
        return String.format("%d days %02d:%02d:%02d", days, hours, minutes, seconds);
    }

    private double bytesToMB(long bytes) {
        return bytes / 1024.0 / 1024.0;
    }

    // TODO: Replace with actual implementation of sending text to Telegram
    private void sendText(Long chatId, String message) {
        System.out.printf("Send to [%d]: %s%n", chatId, message);
    }
}
