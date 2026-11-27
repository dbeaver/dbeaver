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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

public final class DuckDBExtensionSettings {
    private static final Set<String> UNSAFE_SETTINGS = Set.of(
        "allow_unsigned_extensions", "allow_extensions_metadata_mismatch");

    private DuckDBExtensionSettings() {
    }

    @NotNull
    public static List<String> getUnsafeSettings(@Nullable String url, @NotNull Properties properties) {
        var values = new TreeMap<String, String>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        properties.forEach((name, value) -> values.put(name.toString(), value.toString()));
        List<String> settings = new ArrayList<>();
        values.forEach((name, value) -> addSetting(settings, name, value));
        if (url != null) {
            String[] parts = url.split(";");
            for (int index = 1; index < parts.length; index++) {
                int equals = parts[index].indexOf('=');
                if (equals >= 0) {
                    addSetting(settings, parts[index].substring(0, equals).trim(), parts[index].substring(equals + 1).trim());
                }
            }
        }
        // keep both sources: URL precedence and duplicate-name handling differ between driver versions
        boolean unsafe = settings.stream().anyMatch(setting -> {
            String value = setting.substring(setting.indexOf('=') + 1).trim();
            return !"false".equalsIgnoreCase(value) && !"0".equals(value);
        });
        settings.sort(String::compareTo);
        return unsafe ? List.copyOf(settings) : List.of();
    }

    private static void addSetting(@NotNull List<String> settings, @NotNull String name, @NotNull String value) {
        if (UNSAFE_SETTINGS.contains(name.toLowerCase(Locale.ENGLISH))) {
            settings.add(name + "=" + value);
        }
    }
}
