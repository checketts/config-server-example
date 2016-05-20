package com.github.checketts.cloud.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

// Needed at Bootstrap configuration time, see spring.factories
@Configuration
@ConditionalOnProperty(value = "spring.cloud.config.enabled")
public class ConfigClientBootstrapConfiguration {

    @Autowired
    public void configureCloudConfigRestTemplate(@Value("${config.client.secret}") String configClientSecret,
                                                 ConfigServicePropertySourceLocator locator) {
        RestTemplate template = new RestTemplate();
        template.getInterceptors().add(authInterceptor(configClientSecret));

        locator.setRestTemplate(template);
    }

    private ClientHttpRequestInterceptor authInterceptor(final String configClientSecret) {
        return (request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + configClientSecret);
            return execution.execute(request, body);
        };
    }
}
