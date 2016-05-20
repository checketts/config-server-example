package com.github.checketts.config.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.encryption.CipherEnvironmentEncryptor;
import org.springframework.cloud.config.server.encryption.EnvironmentEncryptor;
import org.springframework.cloud.config.server.encryption.TextEncryptorLocator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@Component
public class SanitizeEnforcingEnvironmentEncryptor implements EnvironmentEncryptor {

    public static final String DECRYPT_WITHHELD_MSG =
            "Decrypted value withheld because key is not registered for sanitization";
    private final static Logger LOG = LoggerFactory.getLogger(SanitizeEnforcingEnvironmentEncryptor.class);

    private final CipherEnvironmentEncryptor delegate;
    private final KeySanitizationUtil keySanitizationUtil;

    @Autowired
    public SanitizeEnforcingEnvironmentEncryptor(TextEncryptorLocator textEncryptorLocator,
                                                 KeySanitizationUtil keySanitizationUtil) {
        this.delegate = new CipherEnvironmentEncryptor(textEncryptorLocator);
        this.keySanitizationUtil = keySanitizationUtil;
    }

    @Override
    public Environment decrypt(Environment environment) {
        Environment sanitizedEnv = new Environment(environment.getName(),
                environment.getProfiles(), environment.getLabel(), environment.getVersion());
        for (PropertySource source : environment.getPropertySources()) {
            Map<Object, Object> map = new LinkedHashMap<>(source.getSource());
            for (Map.Entry<Object, Object> entry : new LinkedHashSet<>(map.entrySet())) {
                Object key = entry.getKey();
                String name = key.toString();
                String value = entry.getValue().toString();
                if (value.startsWith("{cipher}") && !keySanitizationUtil.shouldSanitize(name)) {
                    LOG.warn("Not decrypting value because key is not registered for sanitization. key={}", key);
                    map.remove(key);
                    map.put(name, DECRYPT_WITHHELD_MSG);
                }
            }
            sanitizedEnv.add(new PropertySource(source.getName(), map));
        }
        return delegate.decrypt(sanitizedEnv);
    }
}
