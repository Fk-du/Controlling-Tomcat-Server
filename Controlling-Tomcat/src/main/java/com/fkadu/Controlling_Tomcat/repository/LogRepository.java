package com.fkadu.Controlling_Tomcat.repository;

import com.fkadu.Controlling_Tomcat.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findTop100ByOrderByTimestampDesc();
}

