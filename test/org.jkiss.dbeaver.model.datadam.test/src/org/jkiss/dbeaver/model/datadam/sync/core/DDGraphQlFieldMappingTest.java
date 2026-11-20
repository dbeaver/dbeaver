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
package org.jkiss.dbeaver.model.datadam.sync.core;

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.jkiss.code.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashSet;
import java.util.Set;

class DDGraphQlFieldMappingTest {

    @Test
    void sharedProjectFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(DDShareClient.PROJECT_FIELDS, DDSharedProject.class);
    }

    private static void assertSelectionMatchesRecord(
        @NotNull String selectionSet, @NotNull Class<? extends Record> recordClass
    ) {
        Set<String> expected = new LinkedHashSet<>();
        for (RecordComponent component : recordClass.getRecordComponents()) {
            expected.add(component.getName());
        }
        Assertions.assertEquals(expected, topLevelResponseKeys(selectionSet), recordClass.getSimpleName());
    }

    @NotNull
    private static Set<String> topLevelResponseKeys(@NotNull String selectionSet) {
        Set<String> keys = new LinkedHashSet<>();
        int depth = 0;
        for (String rawLine : selectionSet.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (depth == 0) {
                String head = line.split("\\{", 2)[0].trim();
                if (!head.isEmpty() && !head.equals("}")) {
                    int colon = head.indexOf(':');
                    keys.add(colon < 0 ? head : head.substring(0, colon).trim());
                }
            }
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
            }
        }
        return keys;
    }

}
