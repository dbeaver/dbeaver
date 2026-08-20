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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.util.Collection;
import java.util.Map;

/**
 * YUML utils
 * */
public class YumlUtils {

    @NotNull
    public static String toYaml(@NotNull Map<String, Object> model) {
        StringBuilder yaml = new StringBuilder();
        appendYamlMap(yaml, model, 0);
        return yaml.toString();
    }

    private static void appendYamlMap(@NotNull StringBuilder yaml, @NotNull Map<?, ?> map, int indent) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            indent(yaml, indent).append(entry.getKey()).append(':');
            appendYamlValue(yaml, entry.getValue(), indent + 2);
        }
    }

    private static void appendYamlList(@NotNull StringBuilder yaml, @NotNull Collection<?> collection, int indent) {
        if (collection.isEmpty()) {
            indent(yaml, indent).append("[]\n");
            return;
        }
        for (Object value : collection) {
            if (value instanceof Map<?, ?> map) {
                appendYamlMapItem(yaml, map, indent);
            } else if (value instanceof Collection<?> childCollection) {
                indent(yaml, indent).append("-\n");
                appendYamlList(yaml, childCollection, indent + 2);
            } else {
                indent(yaml, indent).append("- ").append(yamlScalar(value)).append('\n');
            }
        }
    }

    private static void appendYamlMapItem(@NotNull StringBuilder yaml, @NotNull Map<?, ?> map, int indent) {
        if (map.isEmpty()) {
            indent(yaml, indent).append("- {}\n");
            return;
        }
        var entries = map.entrySet().iterator();
        Map.Entry<?, ?> firstEntry = entries.next();
        indent(yaml, indent).append("- ").append(firstEntry.getKey()).append(':');
        appendYamlValue(yaml, firstEntry.getValue(), indent + 4);
        while (entries.hasNext()) {
            Map.Entry<?, ?> entry = entries.next();
            indent(yaml, indent + 2).append(entry.getKey()).append(':');
            appendYamlValue(yaml, entry.getValue(), indent + 4);
        }
    }

    private static void appendYamlValue(@NotNull StringBuilder yaml, @Nullable Object value, int indent) {
        if (value instanceof Map<?, ?> map) {
            yaml.append('\n');
            appendYamlMap(yaml, map, indent);
        } else if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                yaml.append(" []\n");
            } else {
                yaml.append('\n');
                appendYamlList(yaml, collection, indent);
            }
        } else {
            yaml.append(' ').append(yamlScalar(value)).append('\n');
        }
    }

    @NotNull
    public static String yamlScalar(@Nullable Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return JSONUtils.GSON.toJson(value.toString());
    }

    @NotNull
    private static StringBuilder indent(@NotNull StringBuilder yaml, int indent) {
        return yaml.append(" ".repeat(indent));
    }
}
