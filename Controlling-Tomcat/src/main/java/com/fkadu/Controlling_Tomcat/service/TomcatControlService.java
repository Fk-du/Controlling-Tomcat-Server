package com.fkadu.Controlling_Tomcat.service;

import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class TomcatControlService {

    private static final String tomcatBinPath = "C:\\Program Files\\Apache Software Foundation\\Tomcat 11.0\\bin";

    public String startServer() {
        return runBatchScript("startup.bat");
    }

    public String stopServer() {
        return runBatchScript("shutdown.bat");
    }

    private String runBatchScript(String scriptName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptName);
            pb.directory(new File(tomcatBinPath));
            Process process = pb.start();
            process.waitFor();
            return scriptName + " executed.";
        } catch (Exception e) {
            return "Error executing " + scriptName + ": " + e.getMessage();
        }
    }
}
