package com.github.checketts.config.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConfigurationProperties("config.server.encrypt")
@Component
@RefreshScope
public class EncryptProperties {
    /**
     * Symmetric keys by alias. As a stronger alternative consider using a keystore.
     */
    private Map<String, String> keys;

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }

}
