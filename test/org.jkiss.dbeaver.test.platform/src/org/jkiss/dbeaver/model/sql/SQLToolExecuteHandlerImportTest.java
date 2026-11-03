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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreToolBaseVacuum;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public class SQLToolExecuteHandlerImportTest extends DBeaverUnitTest {
    private final SQLToolExecuteHandler<?, ?> handler = new PostgreToolBaseVacuum();

    @Test
    void remapsAllObjectProjectsAndPreservesUnrelatedState() {
        JsonObject state = JsonParser.parseString("""
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
            """).getAsJsonObject();
        JsonObject expected = state.deepCopy();
        expected.getAsJsonArray("objects").get(0).getAsJsonObject().addProperty("project", "Target");
        expected.getAsJsonArray("objects").get(1).getAsJsonObject().addProperty("project", "OtherTarget");
        expected.getAsJsonArray("objects").get(10).getAsJsonObject().addProperty("project", "Target");
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
        JsonObject state = JsonParser.parseString(json).getAsJsonObject();
        JsonObject expected = state.deepCopy();

        Assertions.assertFalse(handler.transformTaskState(
            state, Map.of("Source", "Target", "Same", "Same", "42", "Target", "true", "Target")));
        Assertions.assertEquals(expected, state);
    }

    @Test
    void preservesReferencesForEmptyAndIdentityMappings() {
        JsonObject state = JsonParser.parseString("{\"objects\": [{\"project\": \"Source\"}]}").getAsJsonObject();
        JsonObject expected = state.deepCopy();

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
        JsonObject expected = JsonParser.parseString(json).getAsJsonObject();
        expected.getAsJsonObject("tableVacuum").getAsJsonObject("state")
            .getAsJsonArray("objects").get(0).getAsJsonObject().addProperty("project", "Target");
        expected.getAsJsonObject("databaseVacuum").getAsJsonObject("state")
            .getAsJsonArray("objects").get(0).getAsJsonObject().addProperty("project", "OtherTarget");

        ProjectImportRegistry imports = new ProjectImportRegistry();
        try (InputStream source = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
             InputStream result = imports.transformResource(
                 new VoidProgressMonitor(), Path.of(".dbeaver/tasks.json"), source,
                 Map.of("Source", "Target", "OtherImported", "OtherTarget"))
        ) {
            Assertions.assertEquals(expected, JsonParser.parseString(new String(result.readAllBytes(), StandardCharsets.UTF_8)));
        }
    }
}
