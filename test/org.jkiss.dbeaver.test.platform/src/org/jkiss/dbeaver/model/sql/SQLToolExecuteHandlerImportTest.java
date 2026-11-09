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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreToolBaseVacuum;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteHandler;
import org.jkiss.dbeaver.registry.project.ProjectImportRegistry;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public class SQLToolExecuteHandlerImportTest extends DBeaverUnitTest {
    private final SQLToolExecuteHandler<?, ?> handler = new PostgreToolBaseVacuum();

    @Test
    void remapsAllObjectProjectsAndPreservesUnrelatedState() {
        String json = """
            {
              "project": "Source",
              "objects": [
                {"project": "Source", "objectId": "Source/connection/schema/table",
                 "options": {"project": "Source"}},
                {"project": "OtherImported", "objectId": "OtherImported/connection/schema/table"},
                {"project": "External"}, {"project": "Same"}, {"project": "source"},
                null, false, 42, "Source", [],
                {"project": "Source", "objectId": "Source/connection/schema/anotherTable"}
              ],
              "query": "select 'Source' as project",
              "scriptFiles": ["Source/Scripts/query.sql", "resource:/Source/query.sql"],
              "options": {"project": "Source", "objects": [{"project": "OtherImported"}]}
            }
            """;
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        JSONUtils.getObjectList(expected, "objects").get(0).put("project", "Target");
        JSONUtils.getObjectList(expected, "objects").get(1).put("project", "OtherTarget");
        JSONUtils.getObjectList(expected, "objects").get(10).put("project", "Target");
        Map<String, String> projectNames = Map.of("Source", "Target", "OtherImported", "OtherTarget", "Same", "Same");

        Assertions.assertTrue(handler.transformTaskState(state, projectNames));
        Assertions.assertEquals(expected, state);
        Assertions.assertFalse(handler.transformTaskState(state, projectNames));
        Assertions.assertEquals(expected, state);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"objects\": null}", "{\"objects\": []}", "{\"objects\": \"Source\"}",
        "{\"objects\": 42}", "{\"objects\": false}", "{\"objects\": {\"project\": \"Source\"}}",
        """
        {"project": "Source", "objects": [null, false, 42, "Source", [], {},
          {"project": null}, {"project": 42}, {"project": true},
          {"project": ["Source"]}, {"project": {"project": "Source"}}]}
        """,
        "{\"objects\": [{\"project\": \"External\"}, {\"project\": \"Same\"}, {\"project\": \"source\"}]}"
    })
    void preservesUnmappedNamesAndMalformedShapes(@NotNull String json) {
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));

        Assertions.assertFalse(handler.transformTaskState(
            state, Map.of("Source", "Target", "Same", "Same", "42", "Target", "true", "Target")));
        Assertions.assertEquals(expected, state);
    }

    @Test
    void preservesReferencesForEmptyAndIdentityMappings() {
        String json = "{\"objects\": [{\"project\": \"Source\"}]}";
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));

        Assertions.assertFalse(handler.transformTaskState(state, Map.of()));
        Assertions.assertFalse(handler.transformTaskState(state, Map.of("Source", "Source")));
        Assertions.assertEquals(expected, state);
    }

    @Test
    void importsRegisteredSQLToolsThroughProjectImportExtension() throws Exception {
        String json = """
            {
              "tableVacuum": {"task": "pgToolTableVacuum", "label": "Source",
                "state": {"objects": [{"project": "Source", "objectId": "Source/connection/schema/table"}],
                  "project": "Source", "query": "select 'Source'"}},
              "databaseVacuum": {"task": "pgToolDatabaseVacuum",
                "state": {"objects": [{"project": "OtherImported", "objectId": "OtherImported/connection"}],
                  "options": {"project": "OtherImported"}}}
            }
            """;
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        JSONUtils.getObjectList(JSONUtils.getObject(JSONUtils.getObject(expected, "tableVacuum"), "state"), "objects")
            .get(0).put("project", "Target");
        JSONUtils.getObjectList(JSONUtils.getObject(JSONUtils.getObject(expected, "databaseVacuum"), "state"), "objects")
            .get(0).put("project", "OtherTarget");

        ProjectImportRegistry imports = new ProjectImportRegistry();
        try (InputStream source = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
             InputStream result = imports.transformResource(
                 new VoidProgressMonitor(), Path.of(".dbeaver/tasks.json"), source, StandardCharsets.UTF_8,
                 Map.of("Source", "Target", "OtherImported", "OtherTarget"))
        ) {
            Assertions.assertEquals(expected, JSONUtils.parseMap(JSONUtils.GSON, new InputStreamReader(result, StandardCharsets.UTF_8)));
        }
    }
}
