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
package org.jkiss.dbeaver.ext.h2.util;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.h2.internal.H2Constants;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public final class H2Utils {
    private H2Utils() {
    }

    @NotNull
    public static List<String> getUserAllowedClasses() {
        return CommonUtils.splitString(ModelPreferences.getPreferences().getString(H2Constants.PREF_ALLOWED_CLASSES), ',');
    }

    public static void setUserAllowedClasses(@NotNull List<String> allowedClasses) {
        var store = ModelPreferences.getPreferences();
        store.setValue(H2Constants.PREF_ALLOWED_CLASSES, String.join(",", allowedClasses));
        PrefUtils.savePreferenceStore(store);
    }

    public static void setDefaultUserAllowedClasses(@NotNull List<String> allowedClasses) {
        ModelPreferences.getPreferences().setDefault(H2Constants.PREF_ALLOWED_CLASSES, String.join(",", allowedClasses));
    }

    @NotNull
    public static List<String> getDefaultUserAllowedClasses() {
        return CommonUtils.splitString(
            ModelPreferences.getPreferences().getDefaultString(H2Constants.PREF_ALLOWED_CLASSES),
            ','
        );
    }

    @NotNull
    public static List<String> getSystemAllowedClasses() {
        return CommonUtils.splitString(System.getProperty("h2.allowedClasses"), ',');
    }

    public static void setSystemAllowedClasses(@NotNull List<String> allowedClasses) {
        // https://h2database.com/html/advanced.html#restricting_classes
        System.setProperty("h2.allowedClasses", String.join(",", allowedClasses));
    }

    public static boolean isClassLoadingRestricted() {
        return !getSystemAllowedClasses().contains("*");
    }

    public static boolean isJavaSourceDefinition(@NotNull String query) {
        int state = 0;
        int length = query.length();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i <= length; i++) {
            char ch = i < length ? query.charAt(i) : '\0';
            if (i < length && (Character.isLetterOrDigit(ch) || ch == '_')) {
                token.append(ch);
                continue;
            }
            if (!token.isEmpty()) {
                state = advanceJavaSourceState(state, token.toString());
                if (state == 4) {
                    return true;
                }
                token.setLength(0);
            }
            if (i >= length) {
                break;
            }
            if (ch == ';') {
                state = 0;
            } else if (ch == '-' && i + 1 < length && query.charAt(i + 1) == '-') {
                i = skipLineComment(query, i + 2);
            } else if (ch == '/' && i + 1 < length) {
                if (query.charAt(i + 1) == '*') {
                    i = skipBlockComment(query, i + 2);
                } else if (query.charAt(i + 1) == '/') {
                    i = skipLineComment(query, i + 2);
                }
            } else if (ch == '\'' || ch == '"' || ch == '`') {
                i = skipQuoted(query, i + 1, ch);
            } else if (ch == '[') {
                i = skipQuoted(query, i + 1, ']');
            }
        }
        return false;
    }

    private static int advanceJavaSourceState(int state, @NotNull String token) {
        return switch (state) {
            case 0 -> token.equalsIgnoreCase("CREATE") ? 1 : -1;
            case 1 -> {
                if (token.equalsIgnoreCase("ALIAS") || token.equalsIgnoreCase("TRIGGER")) {
                    yield 3;
                } else if (token.equalsIgnoreCase("OR")) {
                    yield 2;
                } else if (token.equalsIgnoreCase("FORCE")) {
                    yield 1;
                }
                yield -1;
            }
            case 2 -> token.equalsIgnoreCase("REPLACE") ? 1 : -1;
            case 3 -> {
                if (token.equalsIgnoreCase("AS")) {
                    yield 4;
                } else if (token.equalsIgnoreCase("FOR") || token.equalsIgnoreCase("CALL")) {
                    yield -1;
                }
                yield 3;
            }
            default -> -1;
        };
    }

    private static int skipLineComment(@NotNull String query, int offset) {
        int end = query.indexOf('\n', offset);
        return end < 0 ? query.length() : end;
    }

    private static int skipBlockComment(@NotNull String query, int offset) {
        int end = query.indexOf("*/", offset);
        return end < 0 ? query.length() : end + 1;
    }

    private static int skipQuoted(@NotNull String query, int offset, char quote) {
        for (int i = offset; i < query.length(); i++) {
            if (query.charAt(i) == quote) {
                if (i + 1 < query.length() && query.charAt(i + 1) == quote) {
                    i++;
                } else {
                    return i;
                }
            }
        }
        return query.length();
    }
}
