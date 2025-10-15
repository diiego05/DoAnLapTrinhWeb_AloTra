package com.alotra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    public String getClientId() { return clientId; }
    public String getApiKey() { return apiKey; }
    public String getChecksumKey() { return checksumKey; }
}
