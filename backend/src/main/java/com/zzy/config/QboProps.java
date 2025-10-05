package com.zzy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "qbo")
public class QboProps {
    private String clientId, clientSecret, redirectUri, environment, realmId, companyZoneId;
}
