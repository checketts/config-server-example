package com.github.checketts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class GreeterController {

    private final GreeterProperties greeterProperties;

    @Autowired
    public GreeterController(GreeterProperties greeterProperties) {
        this.greeterProperties = greeterProperties;
    }

    @RequestMapping("/")
    public String root() {
        return "Hello " + greeterProperties.getName() + " - from " + greeterProperties.getFrom().stream().collect(Collectors.joining(", "));
    }
}
