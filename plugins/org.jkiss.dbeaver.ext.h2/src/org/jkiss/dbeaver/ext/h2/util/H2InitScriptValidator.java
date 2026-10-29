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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

public final class H2InitScriptValidator {
    private static final String INIT_PROPERTY = "INIT";

    private final Set<Approval> approvedScripts = new HashSet<>();

    public synchronized void validate(
        @Nullable String url,
        @NotNull Properties properties,
        @NotNull Predicate<String> confirmation
    ) throws DBCException {
        for (String script : getInitScripts(url, properties)) {
            validateScript(url, script, confirmation);
        }
    }

    public synchronized void validateScript(
        @Nullable String url,
        @NotNull String script,
        @NotNull Predicate<String> confirmation
    ) throws DBCException {
        if (script.isBlank()) {
            return;
        }
        Approval approval = new Approval(url, script);
        if (!approvedScripts.contains(approval)) {
            if (!confirmation.test(script)) {
                throw new DBCException("H2 initialization SQL execution was not approved. Connection cancelled");
            }
            approvedScripts.add(approval);
        }
    }

    @NotNull
    private static Set<String> getInitScripts(@Nullable String url, @NotNull Properties properties) {
        Set<String> scripts = new LinkedHashSet<>();
        for (var entry : properties.entrySet()) {
            if (INIT_PROPERTY.equals(entry.getKey().toString().toUpperCase(Locale.ENGLISH))) {
                addScript(scripts, entry.getValue().toString());
            }
        }
        int separator = url == null ? -1 : url.indexOf(';');
        if (separator >= 0) {
            StringBuilder setting = new StringBuilder();
            // h2 escapes the next character, including separators and characters in property names
            for (int index = separator + 1; index < url.length(); index++) {
                char character = url.charAt(index);
                if (character == ';') {
                    addUrlSetting(scripts, setting.toString());
                    setting.setLength(0);
                } else if (character == '\\' && index + 1 < url.length()) {
                    setting.append(url.charAt(++index));
                } else {
                    setting.append(character);
                }
            }
            addUrlSetting(scripts, setting.toString());
        }
        return scripts;
    }

    private static void addUrlSetting(@NotNull Set<String> scripts, @NotNull String setting) {
        int equals = setting.indexOf('=');
        if (equals >= 0 && INIT_PROPERTY.equals(setting.substring(0, equals).toUpperCase(Locale.ENGLISH))) {
            addScript(scripts, setting.substring(equals + 1));
        }
    }

    private static void addScript(@NotNull Set<String> scripts, @NotNull String script) {
        if (!script.isBlank()) {
            scripts.add(script);
        }
    }

    private record Approval(@Nullable String url, @NotNull String script) {
    }
}
