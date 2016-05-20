package com.github.checketts.config.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@Component
public class KeySanitizationUtil {

    //Copied from org.springframework.boot.actuate.endpoint.Sanitizer to maintain parity
    private static final String[] REGEX_PARTS = {"*", "$", "^", "+"};
    private final List<Pattern> sanitizedKeyPatterns;

    @Autowired
    public KeySanitizationUtil(
            @Value("#{'${endpoints.env.keys-to-sanitize}'.split(',')}") List<String> keysToSanitize) {
        this.sanitizedKeyPatterns = keysToSanitize.stream().map(KeySanitizationUtil::getPattern).collect(toList());
    }

    @SuppressWarnings("squid:UnusedPrivateMethod") //False positive from Sonar as it is used in constructor
    //Copied from org.springframework.boot.actuate.endpoint.Sanitizer to maintain parity
    private static Pattern getPattern(String value) {
        if (isRegex(value)) {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
    }

    private static boolean isRegex(String value) {
        for (String part : REGEX_PARTS) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldSanitize(String key) {
        return sanitizedKeyPatterns.stream().anyMatch(pattern -> pattern.matcher(key).matches());

    }
}
