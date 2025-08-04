package com.fkadu.Controlling_Tomcat.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "logs")
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp;

    private String level;

    @Column(columnDefinition = "TEXT")
    private String message;

    // Constructors, getters and setters

    public LogEntry() {
        this.timestamp = OffsetDateTime.now();
    }

    public LogEntry(String level, String message) {
        this.timestamp = OffsetDateTime.now();
        this.level = level;
        this.message = message;
    }

    // getters/setters below ...

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
