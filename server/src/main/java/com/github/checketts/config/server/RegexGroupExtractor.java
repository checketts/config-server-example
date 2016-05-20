package com.github.checketts.config.server;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility bean useful in areas like SpEL WebSecurityExpressions
 */
@Component
public class RegexGroupExtractor {

    private final ConcurrentMap<String, Pattern> precompiledPatterns = new ConcurrentHashMap<>();

    /**
     * Extracts the string obtained by matching the supplied regex and capture group and prepending a prefix.
     *
     * @param toMatch      The string to extract from
     * @param regex        The regex to match against that includes the capture group to extract
     * @param groupIndex   The capture group index
     * @param returnPrefix A prefix to prepend to the extracted string.
     * @return the string obtained by matching the supplied regex and capture group and prepending a prefix.
     */
    public String extract(String toMatch, String regex, int groupIndex, String returnPrefix) {
        Pattern groupingPattern = pattern(regex);
        Matcher groupingMatcher = groupingPattern.matcher(toMatch);
        if (groupingMatcher.matches()) {
            if (groupIndex <= groupingMatcher.groupCount()) {
                return returnPrefix + groupingMatcher.group(groupIndex);
            }
        }
        throw new IllegalArgumentException(String.format("String to extract not found. toMatch=%s, regex=%s, groupIndex=%s,",
                toMatch, regex, groupIndex));
    }

    /**
     * Returns a precompiled {@link Pattern} for the supplied regex, if available, otherwise caches and returns a new one
     */
    private Pattern pattern(String regex) {
        if (precompiledPatterns.containsKey(regex)) {
            return precompiledPatterns.get(regex);
        }
        Pattern result = Pattern.compile(regex);
        precompiledPatterns.put(regex, result);
        return result;
    }
}
