package com.github.checketts;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("greeter")
public class GreeterProperties {
    private String name;
    private List<String> from = new ArrayList<>();

    public GreeterProperties() {
        name = "Spencer";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFrom() {
        return from;
    }
}
