package com.fkadu.Controlling_Tomcat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TomcatService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${tomcat.manager.url}")
    private String tomcatUrl;

    @Value("${tomcat.manager.username}")
    private String managerUsername;

    @Value("${tomcat.manager.password}")
    private String managerPassword;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(managerUsername, managerPassword);
        return headers;
    }

    private String makeTomcatRequest(String endpoint) {
        String fullUrl = tomcatUrl + endpoint;
        HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    public List<String> listApps() {
        try {
            String response = makeTomcatRequest("/text/list");
            return parseAppList(response);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<String> listRawAppLines() {
        try {
            String response = makeTomcatRequest("/text/list");
            System.out.println("Raw Tomcat response:\n" + response);

            List<String> lines = new ArrayList<>();
            for (String line : response.split("\n")) {
                if (line.startsWith("/") && !line.contains("manager") && !line.contains("docs")) {
                    lines.add(line.trim());
                }
            }
            return lines;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private List<String> parseAppList(String response) {
        List<String> apps = new ArrayList<>();
        if (response == null) return apps;

        for (String line : response.split("\n")) {
            line = line.trim();
            if (line.startsWith("/")) {
                String[] parts = line.split(":");
                if (parts.length > 0) {
                    apps.add(parts[0].trim());
                }
            }
        }
        return apps;
    }

    public String startApp(String path) {
        return handleCommand("/text/start?path=" + path, "starting");
    }

    public String stopApp(String path) {
        return handleCommand("/text/stop?path=" + path, "stopping");
    }

    public String getServerInfo() {
        return handleCommand("/text/serverinfo", "getting server info");
    }

    private String handleCommand(String endpoint, String action) {
        try {
            return makeTomcatRequest(endpoint);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error " + action + ": " + e.getMessage();
        }
    }
}
