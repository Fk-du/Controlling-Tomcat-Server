package com.fkadu.Controlling_Tomcat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "allowed")
public class AllowedPhonesConfig {

    @Value("${allowed.phones}")
    private Set<String> phones;

    public Set<String> getPhones() {
        return phones;
    }

    public void setPhones(Set<String> phones) {
        this.phones = phones;
    }
}
