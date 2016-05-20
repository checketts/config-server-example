package com.github.checketts.config.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceConfig {
    private Map<String, DevicesWithDefaults> deviceDefinitions = new HashMap<>();

    public Map<String, DevicesWithDefaults> getDeviceDefinitions() {
        return deviceDefinitions;
    }

    public void setDeviceDefinitions(Map<String, DevicesWithDefaults> deviceDefinitions) {
        this.deviceDefinitions = deviceDefinitions;
    }


    public static class DevicesWithDefaults {

        private Map<String, String> defaults = new HashMap<>();
        private Map<String, DevicesWithDefaults> subtypes = new HashMap<>();

        private List<Map<String, String>> devices = new ArrayList<>();

        public Map<String, String> getDefaults() {
            return defaults;
        }

        public void setDefaults(Map<String, String> defaults) {
            this.defaults = defaults;
        }

        public List<Map<String, String>> getDevices() {
            return devices;
        }

        public void setDevices(List<Map<String, String>> devices) {
            this.devices = devices;
        }

        public Map<String, DevicesWithDefaults> getSubtypes() {
            return subtypes;
        }

        public void setSubtypes(Map<String, DevicesWithDefaults> subtypes) {
            this.subtypes = subtypes;
        }
    }

}