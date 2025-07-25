package com.fkadu.Controlling_Tomcat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TomcatJmxService {

    private final TomcatControlService tomcatControlService;

    private final String jmxUrl = "service:jmx:rmi:///jndi/rmi://10.6.9.21:9010/jmxrmi";

    private MBeanServerConnection connect() throws Exception {
        JMXServiceURL url = new JMXServiceURL(jmxUrl);
        JMXConnector connector = JMXConnectorFactory.connect(url);
        MBeanServerConnection connection = connector.getMBeanServerConnection();

        // Print all available MBeans
        Set<ObjectName> names = connection.queryNames(null, null);
        for (ObjectName name : names) {
            System.out.println("MBean: " + name.getCanonicalName());
        }

        return connection;
    }

    public String restartServer() {
        try {
            tomcatControlService.stopServer();
            Thread.sleep(5000); // Wait 2 seconds before restart
            tomcatControlService.startServer();
            return "🔄 Server restarted.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed to restart server: " + e.getMessage();
        }
    }

    public String getServerStatus() {
        try {
            MBeanServerConnection connection = connect();

            // Server Info
            ObjectName serverObject = new ObjectName("Catalina:type=Server");
            String serverInfo = (String) connection.getAttribute(serverObject, "serverInfo");

            // Uptime
            ObjectName runtimeObject = new ObjectName("java.lang:type=Runtime");
            Long uptimeMs = (Long) connection.getAttribute(runtimeObject, "Uptime");
            String uptime = formatDuration(uptimeMs);

            // Connector Thread Pool (e.g. http-nio-8080)
            ObjectName threadPoolObject = new ObjectName("Catalina:name=\"http-nio-8080\",type=ThreadPool");
            Integer maxThreads = (Integer) connection.getAttribute(threadPoolObject, "maxThreads");
            Integer currentThreadsBusy = (Integer) connection.getAttribute(threadPoolObject, "currentThreadsBusy");

            // Memory Usage
            ObjectName memoryObject = new ObjectName("java.lang:type=Memory");
            javax.management.openmbean.CompositeData heapMemoryUsage =
                    (javax.management.openmbean.CompositeData) connection.getAttribute(memoryObject, "HeapMemoryUsage");
            long usedHeap = (Long) heapMemoryUsage.get("used");
            long maxHeap = (Long) heapMemoryUsage.get("max");

            // Format output
            return String.format("📊 Server is running.\n" +
                            "Info: %s\n" +
                            "Uptime: %s\n" +
                            "Threads Busy: %d/%d\n" +
                            "Heap Usage: %.1f MB / %.1f MB",
                    serverInfo, uptime, currentThreadsBusy, maxThreads, bytesToMB(usedHeap), bytesToMB(maxHeap));

        } catch (IOException e) {
            return "🟥 Server is OFFLINE or unreachable.";
        } catch (Exception e) {
            e.printStackTrace();
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

}
