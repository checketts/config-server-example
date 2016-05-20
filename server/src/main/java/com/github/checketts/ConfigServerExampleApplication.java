package com.github.checketts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;

import java.io.File;

@SpringBootApplication
@EnableConfigServer
@EnableConfigurationProperties
public class ConfigServerExampleApplication {

    public static void main(String[] args) {

        //Create a cross platform consistent variable to reference the parent directory
        System.setProperty("user.parent.dir", new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath());

        SpringApplication.run(ConfigServerExampleApplication.class, args);
    }


}
