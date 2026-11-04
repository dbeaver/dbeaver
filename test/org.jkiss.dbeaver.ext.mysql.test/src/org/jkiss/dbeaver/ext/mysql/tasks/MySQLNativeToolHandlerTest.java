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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.task.DBTTaskImportTransformer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MySQLNativeToolHandlerTest extends DBeaverUnitTest {

    @Test
    void remapsOnlyRootProjectThroughInheritedHandler() throws DBException {
        DBTTaskImportTransformer handler = new MySQLScriptExecuteHandler();
        String json = """
            {
              "project": "Source", "dataSource": "connection",
              "databaseObjects": ["Source/connection/database"],
              "inputFile": "Source/backup.sql", "outputFolder": "Source/backups",
              "scriptFiles": ["Source/Scripts/query.sql"], "query": "select 'Source' as project",
              "options": {"project": "Source"}, "dataSources": [{"project": "Source"}]
            }
            """;
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        expected.put("project", "Target");

        Assertions.assertTrue(handler.transformTaskState(state, Map.of("Source", "Target")));
        Assertions.assertEquals(expected, state);
        Assertions.assertFalse(handler.transformTaskState(state, Map.of("Source", "Target")));
        Assertions.assertEquals(expected, state);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"project\": null}", "{\"project\": 42}", "{\"project\": false}",
        "{\"project\": [\"Source\"]}", "{\"project\": {\"project\": \"Source\"}}",
        "{\"project\": \"Same\"}", "{\"project\": \"External\"}", "{\"project\": \"source\"}"
    })
    void preservesSameExternalAndNonStringProjects(@NotNull String json) throws DBException {
        DBTTaskImportTransformer handler = new MySQLScriptExecuteHandler();
        Map<String, Object> state = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json));

        Assertions.assertFalse(handler.transformTaskState(state, Map.of()));
        Assertions.assertFalse(handler.transformTaskState(state, Map.of(
            "Source", "Target", "Same", "Same", "42", "Target", "false", "Target")));
        Assertions.assertEquals(expected, state);
    }

    @Test
    void createCredentialsFileEscapesPasswordForMySQLOptionFile() throws IOException {
        Path credentialsFile = MySQLNativeToolHandler.createCredentialsFile(
            "root",
            "Süper#Secret;\"quote'\\path"
        );

        try {
            Assertions.assertEquals(
                "[client]\nuser=root\npassword=\"Süper#Secret;\\\"quote'\\\\path\"",
                Files.readString(credentialsFile)
            );
        } finally {
            Files.deleteIfExists(credentialsFile);
        }
    }
}
