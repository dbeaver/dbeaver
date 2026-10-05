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

import java.util.EnumMap;
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
    private static final String CONNECTION_VALUE =
        "(?:\\([^\\r\\n]*\\)|\\{[^\\r\\n}]*\\}|\\[[^\\r\\n\\]]*\\]|[^\\s\"'<>()\\[\\]{}])++";

    private static final Pattern QUOTED_JDBC_URL = Pattern.compile("(?i)(?<=\")jdbc:[^\\r\\n\"]+|(?<=')jdbc:[^\\r\\n']+");
    private static final Pattern JDBC_URL = Pattern.compile("(?i)\\bjdbc:[a-z0-9_.-]+:" + CONNECTION_VALUE);
    private static final Pattern SSH_CONNECTION = Pattern.compile(
        "(?i)(?<![\\w.@])(?:ssh://" + CONNECTION_VALUE + "|[\\w.*-]+@" + SSH_HOST + ")(?![\\w@])"
    );
    private static final Pattern SSH_CONTEXT_HOST = Pattern.compile(
        "(?i)(?:\\bSSH(?:SessionController)?\\b[^\\r\\n]*?\\b(?:host(?:name)?|to)"
            + "|\\bConnecting to)[\\h=:]+[\"']?(" + SSH_HOST + ")"
    );
    private static final Pattern SSH_HOST_PROPERTY = Pattern.compile(
        "(?i)\\b(?:ssh[._]?host(?:name)?|localHost|remoteHost)\\h*=\\h*[\"']?(" + SSH_HOST + ")"
    );
    private static final Pattern SSH_LOCAL_FORWARD = Pattern.compile("(?i)\\bport forward(?:ing)?\\h+(" + SSH_HOST + ")");
    private static final Pattern SSH_REMOTE_FORWARD = Pattern.compile("(?i)<-\\h*(" + SSH_HOST + ")");
    private static final Pattern IPV6 = Pattern.compile("(?i)(?<![\\w:.%])" + IPV6_ADDRESS + "(?![\\w:%]|\\.\\d)");
    private static final Pattern IPV4 = Pattern.compile("(?<![\\w.])" + IPV4_ADDRESS + "(?![\\w]|\\.\\d)");

    private final Map<ValueType, Map<String, String>> placeholders = new EnumMap<>(ValueType.class);

    @NotNull
    String sanitize(@NotNull String text) {
        // Replace complete connection strings before addresses contained in them.
        text = replace(text, QUOTED_JDBC_URL, ValueType.JDBC_URL, 0);
        text = replace(text, JDBC_URL, ValueType.JDBC_URL, 0);
        text = replace(text, SSH_CONNECTION, ValueType.SSH, 0);
        // IPv4-mapped IPv6 addresses must be treated as a single IPv6 value.
        text = replace(text, IPV6, ValueType.IPV6, 0);
        text = replace(text, IPV4, ValueType.IPV4, 0);
        text = replace(text, SSH_CONTEXT_HOST, ValueType.SSH_HOST, 1);
        text = replace(text, SSH_HOST_PROPERTY, ValueType.SSH_HOST, 1);
        text = replace(text, SSH_LOCAL_FORWARD, ValueType.SSH_HOST, 1);
        return replace(text, SSH_REMOTE_FORWARD, ValueType.SSH_HOST, 1);
    }

    @NotNull
    private String replace(@NotNull String text, @NotNull Pattern pattern, @NotNull ValueType type, int group) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();
        int end = 0;
        while (matcher.find()) {
            int valueEnd = matcher.end(group);
            if (pattern == JDBC_URL || pattern == SSH_CONNECTION) {
                // Sentence punctuation is not part of a connection string.
                while (valueEnd > matcher.start(group) && (text.charAt(valueEnd - 1) == '.' || text.charAt(valueEnd - 1) == ',')) {
                    valueEnd--;
                }
            }
            String value = text.substring(matcher.start(group), valueEnd);
            Map<String, String> values = placeholders.computeIfAbsent(type, key -> new HashMap<>());
            String placeholder = values.computeIfAbsent(value, key -> "[" + type + "_" + values.size() + "]");
            result.append(text, end, matcher.start(group)).append(placeholder);
            end = valueEnd;
        }
        return end == 0 ? text : result.append(text, end, text.length()).toString();
    }

    private enum ValueType {
        JDBC_URL, SSH, SSH_HOST, IPV6, IPV4
    }
}
