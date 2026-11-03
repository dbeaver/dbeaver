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
package org.jkiss.dbeaver.tools.transfer.task;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.task.DBTTaskImportTransformer;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

public class DTTaskHandlerTransferImportTest extends DBeaverUnitTest {
    private final DBTTaskImportTransformer handler = new DTTaskHandlerTransfer();

    @Test
    void remapsOnlyDatabaseProducerLocations() throws DBException {
        JsonObject state = JsonParser.parseString("""
            {
              "project": "Source",
              "producers": [
                {"type": "databaseTransferProducer", "project": "Source", "location": {
                  "type": "entity", "project": "Source", "entityId": "Source/connection/table",
                  "dataFilter": {"project": "Source"}
                }},
                {"type": "databaseTransferProducer", "location": {
                  "type": "query", "project": "OtherImported", "dataSource": "connection",
                  "query": "select 'Source' as project", "path": "Source/Scripts/query.sql"
                }},
                {"type": "databaseTransferProducer", "location": {"project": "External"}},
                {"type": "databaseTransferProducer", "location": {"project": "Same"}},
                {"type": "streamTransferProducer", "location": {"project": "Source"}},
                {"type": "customProducer", "location": {"project": "Source"}},
                {"type": ["databaseTransferProducer"], "location": {"project": "Source"}},
                {"location": {"project": "Source"}},
                null, 42
              ],
              "consumers": [{"type": "databaseTransferProducer", "location": {"project": "Source"}}],
              "configuration": {"project": "Source", "outputFolder": "Source/output"}
            }
            """).getAsJsonObject();
        JsonObject expected = state.deepCopy();
        expected.getAsJsonArray("producers").get(0).getAsJsonObject()
            .getAsJsonObject("location").addProperty("project", "Target");
        expected.getAsJsonArray("producers").get(1).getAsJsonObject()
            .getAsJsonObject("location").addProperty("project", "OtherTarget");
        Map<String, String> projectNames = Map.of("Source", "Target", "OtherImported", "OtherTarget", "Same", "Same");

        Assertions.assertTrue(handler.transformTaskState(state, projectNames));
        Assertions.assertEquals(expected, state);
        Assertions.assertFalse(handler.transformTaskState(state, projectNames));
        Assertions.assertEquals(expected, state);
    }

    @Test
    void leavesSameAndExternalProjectNamesUnchanged() throws DBException {
        JsonObject state = JsonParser.parseString("""
            {"producers": [
              {"type": "databaseTransferProducer", "location": {"project": "Source"}},
              {"type": "databaseTransferProducer", "location": {"project": "External"}},
              {"type": "databaseTransferProducer", "location": {"project": "source"}}
            ]}
            """).getAsJsonObject();
        JsonObject expected = state.deepCopy();

        Assertions.assertFalse(handler.transformTaskState(state, Map.of()));
        Assertions.assertFalse(handler.transformTaskState(state, Map.of("Source", "Source")));
        Assertions.assertEquals(expected, state);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"producers\": null}", "{\"producers\": {\"project\": \"Source\"}}",
        "{\"producers\": [null, 1, [], {\"type\": null}, {\"type\": {}}]}",
        """
        {"producers": [
          {"type": "databaseTransferProducer"},
          {"type": "databaseTransferProducer", "location": null},
          {"type": "databaseTransferProducer", "location": [{"project": "Source"}]},
          {"type": "databaseTransferProducer", "location": {"project": 42}},
          {"type": "databaseTransferProducer", "location": {"project": {"project": "Source"}}}
        ]}
        """
    })
    void preservesUnexpectedShapes(@NotNull String json) throws DBException {
        JsonObject state = JsonParser.parseString(json).getAsJsonObject();
        JsonObject expected = state.deepCopy();

        Assertions.assertFalse(handler.transformTaskState(state, Map.of("Source", "Target", "42", "Target")));
        Assertions.assertEquals(expected, state);
    }
}
