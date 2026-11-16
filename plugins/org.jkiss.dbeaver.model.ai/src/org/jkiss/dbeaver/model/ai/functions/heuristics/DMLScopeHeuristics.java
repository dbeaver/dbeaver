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

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DMLScopeHeuristics extends SQLScopeHeuristics {
    @NotNull
    @Override
    public List<ObjectReference> extractReferences(@NotNull String sql) {
        Map<String, ObjectReference> references = referencedObjects(sql,
            Set.of("FROM", "JOIN", "UPDATE", "INTO", "USING", "TABLE", "CALL", "COPY"));
        List<String> tokens = tokenize(sql);
        if (!tokens.isEmpty() && tokens.getFirst().equalsIgnoreCase("CALL")) {
            Name name = readName(tokens, 1);
            if (name != null) {
                references.put(name.text(), new ObjectReference(name.text(), "PROCEDURE", false));
            }
        }
        return List.copyOf(references.values());
    }
}
