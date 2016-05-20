package com.github.checketts.config.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.config.ConfigServerProperties;
import org.springframework.cloud.config.server.encryption.EnvironmentEncryptor;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.cloud.config.server.environment.EnvironmentEncryptorEnvironmentRepository;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by clintchecketts on 10/20/15.
 */
@RestController
@RequestMapping(method = RequestMethod.GET, value = "/config")
public class FilteringEnvironmentController extends EnvironmentController {

    private static final Logger LOG = LoggerFactory.getLogger(FilteringEnvironmentController.class);
    private static final Pattern devicePattern = Pattern.compile("(deviceDefinitions.[\\w\\.]+).devices\\[\\d*\\]");
    private static final Pattern superPattern = Pattern.compile("(deviceDefinitions.[\\w\\.]+).subtypes\\.[\\w_]+");

    @Autowired
    public FilteringEnvironmentController(EnvironmentRepository repository,
                                          EnvironmentEncryptor environmentEncryptor,
                                          ConfigServerProperties configServerProperties) {
        super(encrypted(repository, environmentEncryptor, configServerProperties));
    }

    private static EnvironmentEncryptorEnvironmentRepository encrypted(EnvironmentRepository repository,
                                                                       EnvironmentEncryptor environmentEncryptor,
                                                                       ConfigServerProperties configServerProperties) {
        EnvironmentEncryptorEnvironmentRepository encrypted =
                new EnvironmentEncryptorEnvironmentRepository(repository, environmentEncryptor);
        encrypted.setOverrides(configServerProperties.getOverrides());
        return encrypted;
    }

    //@VisibleForTesting
    static boolean isRequiredDefault(String key, List<String> deviceTypes) {
        for (String deviceType : deviceTypes) {
            if (key.startsWith(deviceType + ".defaults")) {
                return true;
            }
        }
        return false;
    }

    private static String getTypeFromDevice(String deviceString) {
        Matcher typeMatcher = devicePattern.matcher(deviceString);
        if (typeMatcher.matches()) {
            return typeMatcher.group(1);
        } else {
            return null;
        }
    }

    //@VisibleForTesting
    static List<String> getDeviceTypeAndSuperTypes(String prefix) {
        List<String> deviceTypes = new ArrayList<>();
        String device = getTypeFromDevice(prefix);
        if (device == null) {
            LOG.warn("Invalid device property. Device Properties should end with devices[\\d] and start with deviceDefinitions. was [{}]", prefix);
            return deviceTypes;
        }
        //add the device itself
        deviceTypes.add(device);

        //start looking for super types
        String superType = device;
        while (superType.length() > 0) {
            Matcher superMatcher = superPattern.matcher(superType);
            if (superMatcher.matches()) {
                superType = superMatcher.group(1);
                deviceTypes.add(superType);
            } else {
                //done finding devices
                break;
            }
        }
        return deviceTypes;
    }

    @Override
    public Environment labelled(@PathVariable String name, @PathVariable String profiles,
                                @PathVariable String label) {
        Environment env = super.labelled(name, profiles, label);

        addEnvironmentRepoMetadata(env);

        List<String> serviceDevices = findAssignedDevices(env);
        if (!"application".equals(name)) {
            removeUnassignedDeviceDefinitions(env, serviceDevices);
        }
        return env;
    }

    private void addEnvironmentRepoMetadata(Environment env) {
        String version = null == env.getVersion() ? "unknown" : env.getVersion();
        env.addFirst(new PropertySource("environment-repository-metadata", Collections.singletonMap(
                "cloud.config.environment.repository.version", version)));
    }

    private void removeUnassignedDeviceDefinitions(Environment env, List<String> serviceDevices) {
        List<DeviceDefinitionPart> deviceDefinitons = new ArrayList<>();
        List<String> assignedServiceDevicePrefixes = new ArrayList<>();
        List<String> unassignedDevices = new ArrayList<>();

        env.getPropertySources().stream().forEach(propSource -> {
            Iterator<? extends Map.Entry<?, ?>> it = propSource.getSource().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = it.next();
                String key = (String) entry.getKey();
                if (key.startsWith("deviceDefinitions")) {
                    it.remove();
                    deviceDefinitons.add(new DeviceDefinitionPart(key, entry.getValue(), propSource));
                    if (key.endsWith(".id")) {
                        if (serviceDevices.contains(entry.getValue())) {
                            assignedServiceDevicePrefixes.add(key.replace(".id", ""));
                        } else {
                            unassignedDevices.add(entry.getValue().toString());
                        }
                    }
                }
            }
        });

        LOG.debug("Removing properties for devices not assigned to service. allowed={}, removed={}", serviceDevices,
                unassignedDevices);

        assignedServiceDevicePrefixes.stream().forEach(prefix -> {
            List<String> deviceTypes = getDeviceTypeAndSuperTypes(prefix);
            deviceDefinitons.stream()
                    .filter(def -> def.key.startsWith(prefix) || isRequiredDefault(def.key, deviceTypes))
                    .forEach(def -> {
                        Map<String, Object> sourceMap = (Map<String, Object>) def.source.getSource();
                        sourceMap.put(def.key, def.value);
                    });
        });
    }

    private List<String> findAssignedDevices(Environment env) {
        List<String> devices = new ArrayList();

        env.getPropertySources().stream().forEach(propSource -> {
            propSource.getSource().entrySet().stream()
                    .filter(entry -> ((String) entry.getKey()).startsWith("devices["))
                    .forEach(entry -> devices.add((String) entry.getValue()));
        });

        return devices;
    }

    private static class DeviceDefinitionPart {
        String key;
        Object value;
        PropertySource source;

        public DeviceDefinitionPart(String key, Object value, PropertySource source) {
            this.key = key;
            this.value = value;
            this.source = source;
        }
    }
}
