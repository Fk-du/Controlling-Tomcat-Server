package com.fkadu.Controlling_Tomcat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class TomcatControlService {

    private final String tomcatsBinPath;

    public TomcatControlService(@Value("${tomcat.bin.path}") String tomcatsBinPath) {
        this.tomcatsBinPath = tomcatsBinPath;
    }


    public String startServer() {
        return runBatchScript("startup.bat");
    }

    public String stopServer() {
        return runBatchScript("shutdown.bat");
    }

    private String runBatchScript(String scriptName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptName);
            pb.directory(new File(tomcatsBinPath));
            Process process = pb.start();
            process.waitFor();
            return scriptName + " executed.";
        } catch (Exception e) {
            return "Error executing " + scriptName + ": " + e.getMessage();
        }
    }
}
