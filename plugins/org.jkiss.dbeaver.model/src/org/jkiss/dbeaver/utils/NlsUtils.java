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
package org.jkiss.dbeaver.utils;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.xml.XMLUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NlsUtils {
    private static final Log log = Log.getLog(NlsUtils.class);
    private static final Pattern MESSAGE_PLACEHOLDER = Pattern.compile("%(!?#?)([a-zA-Z_][a-zA-Z_0-9]*)");

    private NlsUtils() {
    }

    /** Escapes string contents without adding quotes, for single/double-quoted strings and template literals. */
    @NotNull
    private static String escapeJavaScriptString(@NotNull String message) {
        StringBuilder escaped = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\'' -> escaped.append("\\u0027");
                case '`' -> escaped.append("\\u0060");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\u2028' -> escaped.append("\\u2028");
                case '\u2029' -> escaped.append("\\u2029");
                case '$' -> {
                    if (i + 1 < message.length() && message.charAt(i + 1) == '{') {
                        escaped.append("\\u0024");
                    } else {
                        escaped.append(c);
                    }
                }
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    /**
     * Replaces %field_name placeholders with public static String fields from an NLS bundle.
     * Use %!field_name for XML/HTML encoding, %#field_name for JavaScript string encoding,
     * or %!#field_name for XML/HTML encoding followed by JavaScript string encoding.
     * Unresolved keys are logged and left as-is. Replacements are not processed recursively.
     */
    @NotNull
    public static String formatNlsMessages(@NotNull String text, @NotNull Class<? extends NLS> nlsBundle) {
        return MESSAGE_PLACEHOLDER.matcher(text).replaceAll(match -> {
            String name = match.group(2);
            try {
                Field field = nlsBundle.getField(name);
                if (field.getType() != String.class || !Modifier.isStatic(field.getModifiers())) {
                    log.debug("Not an NLS message: " + nlsBundle.getName() + '.' + name);
                    return Matcher.quoteReplacement(match.group());
                }
                String value = (String) field.get(null);
                if (value == null) {
                    log.debug("Uninitialized NLS message: " + nlsBundle.getName() + '.' + name);
                    return Matcher.quoteReplacement(match.group());
                }
                String encoding = match.group(1);
                if (encoding.contains("!")) {
                    value = XMLUtils.escapeXml(value);
                }
                if (encoding.contains("#")) {
                    value = escapeJavaScriptString(value);
                }
                return Matcher.quoteReplacement(value);
            } catch (ReflectiveOperationException e) {
                log.debug("Cannot resolve NLS message: " + nlsBundle.getName() + '.' + name, e);
                return Matcher.quoteReplacement(match.group());
            }
        });
    }
}
