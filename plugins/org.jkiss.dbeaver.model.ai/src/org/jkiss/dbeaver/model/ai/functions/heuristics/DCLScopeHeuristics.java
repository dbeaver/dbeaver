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
package org.jkiss.dbeaver.model.ai.functions.heuristics;

import org.jkiss.code.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DCLScopeHeuristics extends SQLScopeHeuristics {
    @NotNull
    @Override
    public List<ObjectReference> extractReferences(@NotNull String sql) {
        Map<String, ObjectReference> references = new LinkedHashMap<>();
        List<String> tokens = tokenize(sql);
        for (int i = 1; i < tokens.size(); i++) {
            if (!tokens.get(i).equalsIgnoreCase("ON") && !tokens.get(i).equalsIgnoreCase("SCHEMA")) {
                continue;
            }
            String type = tokens.get(i).equalsIgnoreCase("SCHEMA") ? "SCHEMA" : "OBJECT";
            int start = i + 1;
            if (start < tokens.size() && Set.of("ALL", "TABLES", "SEQUENCES", "FUNCTIONS", "PROCEDURES", "ROUTINES", "TYPES")
                .contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
                continue;
            }
            if (start < tokens.size() && Set.of("OBJECT", "TABLE", "VIEW", "SCHEMA", "DATABASE", "SEQUENCE", "FUNCTION", "PROCEDURE", "TYPE")
                .contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
                type = tokens.get(start++).toUpperCase(Locale.ROOT);
            }
            if (start + 1 < tokens.size() && tokens.get(start).equals(":") && tokens.get(start + 1).equals(":")) {
                start += 2;
            }
            Name name = readName(tokens, start);
            while (name != null) {
                references.put(name.text(), new ObjectReference(name.text(), type, false));
                start = name.end();
                if (start < tokens.size() && tokens.get(start).equals("(")) {
                    start = skipParentheses(tokens, start);
                }
                if (start >= tokens.size() || !tokens.get(start).equals(",")) {
                    break;
                }
                name = readName(tokens, start + 1);
            }
        }
        return List.copyOf(references.values());
    }
}
