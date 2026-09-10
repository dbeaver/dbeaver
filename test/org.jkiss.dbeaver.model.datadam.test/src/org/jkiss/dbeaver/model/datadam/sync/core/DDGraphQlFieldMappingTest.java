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

import com.dbeaver.datadam.share.api.model.DDConfiguration;
import com.dbeaver.datadam.share.api.model.DDConfigurationPart;
import com.dbeaver.datadam.share.api.model.DDConfigurationSummary;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectFile;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.model.DDUpdateConfigurationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashSet;
import java.util.Set;

class DDGraphQlFieldMappingTest {

    @Test
    void configurationSummaryFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(DDSyncApi.CONFIGURATION_FIELDS, DDConfigurationSummary.class);
    }

    @Test
    void configurationFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(DDSyncApi.CONFIGURATION_WITH_PARTS_FIELDS, DDConfiguration.class);
    }

    @Test
    void configurationPartFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(
            extractBlock(DDSyncApi.CONFIGURATION_WITH_PARTS_FIELDS, "parts"), DDConfigurationPart.class);
    }

    @Test
    void updateConfigurationResultFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(
            extractBlock(DDSyncApi.MUTATION_UPDATE_CONFIGURATION, "updateConfiguration("),
            DDUpdateConfigurationResult.class);
    }

    @Test
    void sharedProjectFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(DDProjectSyncApi.PROJECT_FIELDS, DDSharedProject.class);
    }

    @Test
    void sharedProjectFileFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(DDProjectSyncApi.PROJECT_FILE_FIELDS, DDSharedProjectFile.class);
    }

    @Test
    void sharedProjectConfigurationFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(
            extractBlock(DDProjectSyncApi.QUERY_PULL_PROJECT_CONFIGURATION, "pullProjectConfiguration("),
            DDSharedProjectConfiguration.class);
    }

    @Test
    void sharedProjectRevisionFieldsMatchResponseKeys() {
        assertSelectionMatchesRecord(
            extractBlock(DDProjectSyncApi.MUTATION_PUSH_PROJECT_CONFIGURATION, "pushProjectConfiguration("),
            DDSharedProjectRevision.class);
    }

    private static void assertSelectionMatchesRecord(String selectionSet, Class<? extends Record> recordClass) {
        Set<String> expected = new LinkedHashSet<>();
        for (RecordComponent component : recordClass.getRecordComponents()) {
            expected.add(component.getName());
        }
        Assertions.assertEquals(expected, topLevelResponseKeys(selectionSet), recordClass.getSimpleName());
    }

    private static Set<String> topLevelResponseKeys(String selectionSet) {
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

    private static String extractBlock(String source, String marker) {
        int markerIndex = source.indexOf(marker);
        int braceIndex = source.indexOf('{', markerIndex);
        int depth = 0;
        int i = braceIndex;
        for (; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
        }
        return source.substring(braceIndex + 1, i);
    }
}
