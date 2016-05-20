package com.github.checketts.config.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.server.encryption.EncryptionController;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;

/**
 * Encapsulates details of interacting with Spring Cloud Config server endpoints, to avoid dependencies on server classes.
 * Currently simply delegates to Spring Cloud Config server classes because it is embedded,
 * but when it is no longer embedded then this class should be the only bean to change (probably to use RestTemplate).
 */
@Component
public class ConfigServerClient {

    public static final String DEVICES_PREFIX = "devices_";
    private static final Logger LOG = LoggerFactory.getLogger(ConfigServerClient.class);

    private final EncryptionController encryptionController;
    private final EncryptProperties encryptProperties;

    private PropertySourceLocator propertySourceLocator;

    @Autowired
    public ConfigServerClient(EncryptionController encryptionController,
//                              ConfigServicePropertySourceLocator propertySourceLocator,
                              EncryptProperties encryptProperties) {
        this.encryptionController = encryptionController;
        this.propertySourceLocator = propertySourceLocator;
        this.encryptProperties = encryptProperties;
    }

    public String encrypt(String toEncrypt) {
        return encryptionController.encrypt(toEncrypt, MediaType.TEXT_PLAIN);
    }

    public String decrypt(String toDecrypt) {
        return encryptionController.decrypt(toDecrypt, MediaType.TEXT_PLAIN);
    }

    /**
     * Gets the property source in the same way Spring Cloud Config client code does for the current app, except the
     * caller chooses the app, profile, and label. Also the result is unrelated to the current app's PropertySources.
     * <p>
     * If {applicationName} is the default ("application"), an extra property source is requested for a
     * profile of "devices_{environmentId}".
     *
     * @see org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration
     * @see org.springframework.cloud.config.client.ConfigServicePropertySourceLocator
     */
    public EnumerablePropertySource<?> getPropertySource(String applicationName, String environmentId) {
        return getPropertySource(applicationName, environmentId, "master");
    }

    public EnumerablePropertySource<?> getPropertySource(String applicationName, String environmentId, String branch) {
        EnumerablePropertySource<?> result = newPropertySource(applicationName, environmentId, branch);
        if ("application".equals(applicationName)) {
            String profile = DEVICES_PREFIX + environmentId;
            EnumerablePropertySource<?> deviceSource = newPropertySource(applicationName, profile, branch);
            CompositePropertySource deviceSourceWithUniqueName = new CompositePropertySource("devicesSource");
            deviceSourceWithUniqueName.addPropertySource(deviceSource);

            CompositePropertySource composite = new CompositePropertySource("envPlusDevices");
            composite.addPropertySource(result);
            composite.addPropertySource(deviceSourceWithUniqueName);
            result = composite;
        }
        return result;
    }

    private EnumerablePropertySource<?> newPropertySource(String applicationName, String environmentId, String branch) {
        throw new UnsupportedOperationException("Property source Locator is not wired up properly, check constructor");
//        ConfigurableEnvironment environment = newEnvironment(applicationName, environmentId, branch);
//
//        PropertySource<?> source = propertySourceLocator.locate(environment);
//        LOG.info("getPropertySource(). env={}, source={}", environment, source);
//        if (null == source) {
//            source = new MapPropertySource("emptyPropertySource", Collections.emptyMap());
//        }
//        return (EnumerablePropertySource<?>)source;
    }

    private ConfigurableEnvironment newEnvironment(String applicationName, String environmentId, String branch) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(ConfigClientProperties.PREFIX + ".name", applicationName);
        map.put(ConfigClientProperties.PREFIX + ".profile", environmentId);
        map.put(ConfigClientProperties.PREFIX + ".label", branch);

        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("profiles", map));
        return environment;

    }

    public String toEncryptedValue(String value, Optional<String> environmentId, Optional<String> serviceOrDeviceId) {
        String keyAlias = toKeyAlias(environmentId, serviceOrDeviceId);
        if (!encryptProperties.getKeys().containsKey(keyAlias)) {
            keyAlias = toKeyAlias(environmentId, Optional.empty());
            if (!encryptProperties.getKeys().containsKey(keyAlias)) {
                throw new IllegalStateException("Encryption key not found for environment=" + environmentId + ", svc/dvc=" + serviceOrDeviceId);
            }
        }
        return "'" + toEncryptedValue(Optional.of(keyAlias), value) + "'";
    }

    public String toEncryptedValue(Optional<String> keyAlias, String value) {
        String aliasPrefixedValue = keyAlias.isPresent() ? String.format("{key:%s}%s", keyAlias.get(), value) : value;
        return "{cipher}" + encrypt(aliasPrefixedValue) + "";
    }


    private String toKeyAlias(Optional<String> environmentId, Optional<String> serviceOrDeviceId) {
        if (environmentId.isPresent()) {
            if (serviceOrDeviceId.isPresent()) {
                return environmentId.get() + "_" + serviceOrDeviceId.get() + "_v1";
            }
            return environmentId.get() + "_v1";
        }
        throw new IllegalStateException("environmentId expected for secure properties");
    }

}
