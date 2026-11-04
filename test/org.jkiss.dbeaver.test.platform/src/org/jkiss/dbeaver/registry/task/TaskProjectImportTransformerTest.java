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
package org.jkiss.dbeaver.registry.task;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.model.task.DBTTaskRegistry;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.registry.project.ProjectImportRegistry;
import org.jkiss.dbeaver.runtime.DBInterruptedException;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TaskProjectImportTransformerTest extends DBeaverUnitTest {
    private static final Map<String, String> PROJECT_NAMES = Map.of("Source", "Target", "Other", "OtherTarget");
    private final DBRProgressMonitor progress = new VoidProgressMonitor();
    private DBTTaskRegistry registry;
    private DBTTaskType scriptType;
    private TaskProjectImportTransformer transformer;

    @BeforeEach
    void setUp() throws DBException {
        registry = Mockito.mock(DBTTaskRegistry.class);
        scriptType = Mockito.mock(DBTTaskType.class);
        DBTTaskHandler handler = TaskRegistry.getInstance().getTaskType("scriptExecute").createHandler();
        Mockito.doReturn(handler.getClass()).when(scriptType).getHandlerClass();
        Mockito.when(scriptType.createHandler()).thenReturn(handler);
        Mockito.when(registry.getTaskType("scriptExecute")).thenReturn(scriptType);
        transformer = new TaskProjectImportTransformer(registry);
    }

    @Test
    void preservesTaskMetadataUnknownTasksAndJsonValues() throws DBException {
        String json = """
            {
              "##tasksFolders": {"Source": {"parent": "Other"}},
              "script": {
                "task": "scriptExecute", "label": "Source", "createTime": "original",
                "taskFolder": "Source", "future": 9007199254740993, "optional": null,
                "state": {"dataSources": [
                  {"project": "Source", "dataSource": "connection1"},
                  {"project": "Other", "dataSource": "connection2"},
                  {"project": "External", "dataSource": "connection3"}
                ], "scriptFiles": ["Source/Scripts/test.sql"], "query": "select 'Source'",
                   "limit": 9007199254740993, "fraction": 0.12345678901234567890123456789,
                   "emptyList": [], "emptyObject": {}, "emptyString": "", "nullableSetting": null}
              },
              "unknown": {"task": "unavailable", "state": {"project": "Source"}},
              "unsupported": {"task": "unsupported", "state": {"project": "Source"}},
              "future-format": [1, null, "Source"],
              "no-state": {"task": "scriptExecute"}
            }
            """;
        DBTTaskType unsupported = Mockito.mock(DBTTaskType.class);
        Mockito.doReturn(DBTTaskHandler.class).when(unsupported).getHandlerClass();
        Mockito.when(registry.getTaskType("unsupported")).thenReturn(unsupported);
        JsonObject expected = JsonParser.parseString(json).getAsJsonObject();
        var dataSources = expected.getAsJsonObject("script").getAsJsonObject("state").getAsJsonArray("dataSources");
        dataSources.get(0).getAsJsonObject().addProperty("project", "Target");
        dataSources.get(1).getAsJsonObject().addProperty("project", "OtherTarget");

        String result = transformer.transform(progress, json, PROJECT_NAMES);

        assertEquals(expected, JsonParser.parseString(result));
        assertTrue(result.contains("9007199254740993"));
        assertTrue(result.contains("0.12345678901234567890123456789"));
        Mockito.verify(unsupported, Mockito.never()).createHandler();
    }

    @Test
    void preservesOriginalContentsWhenNoReferencesChange() throws DBException {
        String contents = "{\"task\":{\"task\":\"scriptExecute\",\"state\":{\"project\":\"External\"}}}";
        assertSame(contents, transformer.transform(progress, contents, PROJECT_NAMES));
        assertSame(contents, transformer.transform(progress, contents, Map.of("Source", "Source")));
        assertSame(contents, transformer.transform(progress, contents, Map.of()));
    }

    @Test
    void acceptsEmptyConfigurationsSupportedByTaskLoading() throws DBException {
        // Gson's map adapter also accepts an empty array of key/value entries.
        for (String json : List.of("", "null", "{}", "[]")) {
            assertTrue(JSONUtils.parseMap(JSONUtils.GSON, new StringReader(json)).isEmpty());
            assertSame(json, transformer.transform(progress, json, PROJECT_NAMES));
        }
    }

    @Test
    void transformsStateWrittenAndReadUsingTaskSettingsSerialization() throws Exception {
        Map<String, Object> state = Map.of(
            "dataSources", List.of(
                Map.of("project", "Source", "dataSource", "connection1"),
                Map.of("project", "External", "dataSource", "connection2")),
            "scriptFiles", List.of("Source/Scripts/test.sql"),
            "ignoreErrors", false,
            "emptyString", ""
        );
        StringWriter buffer = new StringWriter();
        try (JsonWriter writer = JSONUtils.GSON.newJsonWriter(buffer)) {
            writer.beginObject();
            writer.name("script").beginObject();
            writer.name(TaskConstants.TAG_TASK).value("scriptExecute");
            JSONUtils.serializeProperties(writer, TaskConstants.TAG_STATE, state, true);
            writer.endObject();
            writer.endObject();
        }
        Map<String, Object> expected = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(buffer.toString()));
        Map<String, Object> expectedState = JSONUtils.getObject(JSONUtils.getObject(expected, "script"), TaskConstants.TAG_STATE);
        JSONUtils.getObjectList(expectedState, "dataSources").getFirst().put("project", "Target");

        String result = transformer.transform(progress, buffer.toString(), PROJECT_NAMES);

        Map<String, Object> imported = JSONUtils.parseMap(JSONUtils.GSON, new StringReader(result));
        assertEquals(expected, imported);
    }

    @Test
    void mapsNamesOnceRatherThanFollowingMappingChains() throws DBException {
        String contents = "{\"task\":{\"task\":\"scriptExecute\",\"state\":{\"project\":\"Source\"}}}";
        String result = transformer.transform(progress, contents, Map.of("Source", "Target", "Target", "Third"));
        assertEquals("Target", JsonParser.parseString(result).getAsJsonObject()
            .getAsJsonObject("task").getAsJsonObject("state").get("project").getAsString());
    }

    @Test
    void reportsMalformedConfigurationAndHandlerFailures() throws DBException {
        assertThrows(DBException.class, () -> transformer.transform(progress, "{", PROJECT_NAMES));
        assertThrows(DBException.class, () -> transformer.transform(progress, "[1]", PROJECT_NAMES));
        assertThrows(DBException.class, () -> transformer.transform(progress, "42", PROJECT_NAMES));
        Mockito.when(scriptType.createHandler()).thenThrow(new DBException("Cannot create handler"));
        String contents = "{\"task\":{\"task\":\"scriptExecute\",\"state\":{\"project\":\"Source\"}}}";
        assertThrows(DBException.class, () -> transformer.transform(progress, contents, PROJECT_NAMES));
    }

    @Test
    void honorsCancellation() {
        DBRProgressMonitor canceled = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(canceled.isCanceled()).thenReturn(true);
        String contents = "{\"task\":{}}";
        assertThrows(DBInterruptedException.class, () -> transformer.transform(canceled, contents, PROJECT_NAMES));
    }

    @Test
    void extensionTransformsOnlyTheProjectTaskConfiguration() throws Exception {
        ProjectImportRegistry imports = new ProjectImportRegistry();
        byte[] contents = "{\"task\":{\"task\":\"scriptExecute\",\"state\":{\"project\":\"Source\"}}}"
            .getBytes(StandardCharsets.UTF_8);
        try (InputStream source = new ByteArrayInputStream(contents);
             InputStream result = imports.transformResource(
                 progress, Path.of(".dbeaver/tasks.json"), source, StandardCharsets.UTF_8, PROJECT_NAMES)) {
            assertEquals("Target", JsonParser.parseString(new String(result.readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("task").getAsJsonObject("state").get("project").getAsString());
        }
        try (InputStream source = new ByteArrayInputStream(contents)) {
            assertSame(source, imports.transformResource(
                progress, Path.of("Scripts/tasks.json"), source, StandardCharsets.UTF_8, PROJECT_NAMES));
            assertArrayEquals(contents, source.readAllBytes());
        }
    }
}
