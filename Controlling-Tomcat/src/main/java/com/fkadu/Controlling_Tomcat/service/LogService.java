package com.fkadu.Controlling_Tomcat.service;

import com.fkadu.Controlling_Tomcat.model.LogEntry;
import com.fkadu.Controlling_Tomcat.repository.LogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {
    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void saveLog(String level, String message) {
        LogEntry logEntry = new LogEntry(level, message);
        logRepository.save(logEntry);
    }

    public List<LogEntry> getLatestLogs() {
        return logRepository.findTop100ByOrderByTimestampDesc();
    }
}

