/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.jkiss.code.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps a separate, stable placeholder mapping for each diagnostic archive.
 */
final class DiagnosticInfoSanitizer {
    private static final String IPV4_OCTET = "(?:25[0-5]|2[0-4]\\d|1\\d{2}|0?\\d{1,2})";
    private static final String IPV4_ADDRESS = "(?:" + IPV4_OCTET + "\\.){3}" + IPV4_OCTET;
    private static final String IPV6_GROUP = "[0-9a-f]{1,4}";
    private static final String IPV6_ADDRESS = "(?:"
        + "(?:" + IPV6_GROUP + ":){6}" + IPV4_ADDRESS
        + "|(?:(?:" + IPV6_GROUP + ":){0,4}" + IPV6_GROUP + ")?::" + IPV4_ADDRESS
        + "|(?:(?:" + IPV6_GROUP + ":){0,3}" + IPV6_GROUP + ")?::" + IPV6_GROUP + ":" + IPV4_ADDRESS
        + "|(?:(?:" + IPV6_GROUP + ":){0,2}" + IPV6_GROUP + ")?::(?:" + IPV6_GROUP + ":){2}" + IPV4_ADDRESS
        + "|(?:(?:" + IPV6_GROUP + ":){0,1}" + IPV6_GROUP + ")?::(?:" + IPV6_GROUP + ":){3}" + IPV4_ADDRESS
        + "|(?:" + IPV6_GROUP + ")?::(?:" + IPV6_GROUP + ":){4}" + IPV4_ADDRESS
        + "|::(?:" + IPV6_GROUP + ":){5}" + IPV4_ADDRESS
        + "|(?:" + IPV6_GROUP + ":){7}" + IPV6_GROUP
        + "|(?:" + IPV6_GROUP + ":){1,7}:"
        + "|(?:" + IPV6_GROUP + ":){1,6}:" + IPV6_GROUP
        + "|(?:" + IPV6_GROUP + ":){1,5}(?::" + IPV6_GROUP + "){1,2}"
        + "|(?:" + IPV6_GROUP + ":){1,4}(?::" + IPV6_GROUP + "){1,3}"
        + "|(?:" + IPV6_GROUP + ":){1,3}(?::" + IPV6_GROUP + "){1,4}"
        + "|(?:" + IPV6_GROUP + ":){1,2}(?::" + IPV6_GROUP + "){1,5}"
        + "|" + IPV6_GROUP + ":(?:(?::" + IPV6_GROUP + "){1,6})"
        + "|:(?:(?::" + IPV6_GROUP + "){1,7}|:)"
        + ")(?:%[\\w-]+(?:\\.[\\w-]+)*)?";
    private static final String SSH_HOST = "(?:\\[" + IPV6_ADDRESS + "\\]|" + IPV6_ADDRESS
        + "|[\\w*](?:[\\w.*-]*[\\w*])?)(?::\\d+)?";

    // Complete connection strings and IPv4-mapped IPv6 addresses take precedence over their embedded addresses.
    private final Pattern sensitiveValuesPattern = Pattern.compile(
        "(?<JDBC>(?<=\")jdbc:[^\\r\\n\"]+|(?<=')jdbc:[^\\r\\n']+"
            + "|\\bjdbc:[a-z0-9_.-]+:(?:\\([^\\r\\n]*\\)|\\{[^\\r\\n}]*\\}|\\[[^\\r\\n\\]]*\\]|[^\\s\"'<>()\\[\\]{}])++)"
            + "|(?<SSH>(?<![\\w.@])(?:ssh://"
            + "(?:\\([^\\r\\n]*\\)|\\{[^\\r\\n}]*\\}|\\[[^\\r\\n\\]]*\\]|[^\\s\"'<>()\\[\\]{}])++"
            + "|[\\w.*-]+@" + SSH_HOST + ")(?![\\w@]))"
            + "|(?<IPV6>(?<![\\w:.%])" + IPV6_ADDRESS + "(?![\\w:%]|\\.\\d))"
            + "|(?<IPV4>(?<![\\w.])" + IPV4_ADDRESS + "(?![\\w]|\\.\\d))",
        Pattern.CASE_INSENSITIVE
    );

    private final Map<SensitiveValue, String> placeholders = new HashMap<>();
    private final Map<String, Integer> placeholderCounts = new HashMap<>();

    @NotNull
    String sanitize(@NotNull String text) {
        Matcher matcher = sensitiveValuesPattern.matcher(text);
        var groupNames = sensitiveValuesPattern.namedGroups().keySet();
        StringBuilder result = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            for (String groupName : groupNames) {
                if (matcher.group(groupName) == null) {
                    continue;
                }
                int valueEnd = matcher.end();
                boolean quoted = matcher.start() > 0
                    && (text.charAt(matcher.start() - 1) == '"' || text.charAt(matcher.start() - 1) == '\'');
                if (groupName.equals("SSH") || groupName.equals("JDBC") && !quoted) {
                    // Sentence punctuation is not part of a connection string.
                    while (valueEnd > matcher.start() && (text.charAt(valueEnd - 1) == '.' || text.charAt(valueEnd - 1) == ',')) {
                        valueEnd--;
                    }
                }
                SensitiveValue value = new SensitiveValue(groupName, text.substring(matcher.start(), valueEnd));
                String placeholder = placeholders.computeIfAbsent(value, key -> {
                    int index = placeholderCounts.merge(key.groupName(), 1, Integer::sum) - 1;
                    return "[" + key.groupName() + "_" + index + "]";
                });
                result.append(text, end, matcher.start()).append(placeholder);
                end = valueEnd;
                break;
            }
        }
        return end == 0 ? text : result.append(text, end, text.length()).toString();
    }

    private record SensitiveValue(@NotNull String groupName, @NotNull String value) {}
}
