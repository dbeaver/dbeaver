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
package org.jkiss.dbeaver.tools.sql.task;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.task.DBTTaskImportTransformer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.StringReader;
import java.util.Map;

public class SQLScriptExecuteHandlerTest extends DBeaverUnitTest {
    private final DBTTaskImportTransformer handler = new SQLScriptExecuteHandler();

    @Test
    void remapsLegacyProject() throws DBException {
        String json = """
            {"project": "Source", "dataSourceContainer": "connection",
             "scriptFiles": ["Source/Scripts/query.sql"], "options": {"project": "Source"}}
            """;
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        expected.put("project", "Target");

        Assertions.assertTrue(handler.transformTaskState(state, Map.of("Source", "Target")));
        Assertions.assertEquals(expected, state);
    }

    @Test
    void remapsAllDataSourcesEvenWhenLegacyProjectChanges() throws DBException {
        String json = """
            {
              "project": "Source",
              "dataSources": [
                {"project": "Source", "dataSource": "Source/connection", "options": {"project": "Source"}},
                {"project": "OtherImported", "dataSource": "other"},
                {"project": "External"}, {"project": "Same"}, {"project": "source"},
                null, 42, {"project": 42}, {"project": null}, {"project": {"project": "Source"}}
              ],
              "scriptFiles": ["Source/Scripts/query.sql", "resource:/Source/query.sql"],
              "query": "select 'Source' as project", "options": {"project": "Source"}
            }
            """;
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        expected.put("project", "Target");
        JSONUtils.getObjectList(expected, "dataSources").get(0).put("project", "Target");
        JSONUtils.getObjectList(expected, "dataSources").get(1).put("project", "OtherTarget");
        Map<String, String> projectNames = Map.of(
            "Source", "Target", "OtherImported", "OtherTarget", "Same", "Same", "42", "Target");

        Assertions.assertTrue(handler.transformTaskState(state, projectNames));
        Assertions.assertEquals(expected, state);
        Assertions.assertFalse(handler.transformTaskState(state, projectNames));
        Assertions.assertEquals(expected, state);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"project\": null}", "{\"dataSources\": null}",
        "{\"dataSources\": {\"project\": \"Source\"}}",
        "{\"project\": 42, \"dataSources\": [null, false, [], {}, {\"project\": [\"Source\"]}]}",
        "{\"project\": \"Same\", \"dataSources\": [{\"project\": \"External\"}, {\"project\": \"source\"}]}"
    })
    void preservesUnmappedNamesAndUnexpectedShapes(@NotNull String json) throws DBException {
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));

        Assertions.assertFalse(handler.transformTaskState(state, Map.of()));
        Assertions.assertFalse(handler.transformTaskState(state, Map.of("Source", "Target", "Same", "Same", "42", "Target")));
        Assertions.assertEquals(expected, state);
    }
}
