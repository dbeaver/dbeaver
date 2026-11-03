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

import com.google.gson.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProjectImportTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.model.task.DBTTaskImportTransformer;
import org.jkiss.dbeaver.model.task.DBTTaskRegistry;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBInterruptedException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TaskProjectImportTransformer implements DBPProjectImportTransformer {
    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    private final DBTTaskRegistry taskRegistry;

    public TaskProjectImportTransformer() {
        this(TaskRegistry.getInstance());
    }

    public TaskProjectImportTransformer(@NotNull DBTTaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    @NotNull
    @Override
    public byte[] transform(
        @NotNull DBRProgressMonitor monitor,
        @NotNull byte[] contents,
        @NotNull Map<String, String> projectNames
    ) throws DBException {
        if (projectNames.entrySet().stream().allMatch(entry -> entry.getKey().equals(entry.getValue()))) {
            return contents;
        }
        JsonObject configuration;
        try {
            configuration = GSON.fromJson(new String(contents, StandardCharsets.UTF_8), JsonObject.class);
        } catch (JsonParseException e) {
            throw new DBException("Cannot transform imported task configuration", e);
        }
        if (configuration == null) {
            return contents;
        }
        boolean changed = false;
        // Transform the JSON tree rather than loading tasks: unavailable task types and unknown fields must survive.
        for (Map.Entry<String, JsonElement> entry : configuration.entrySet()) {
            if (monitor.isCanceled()) {
                throw new DBInterruptedException("Project import canceled");
            }
            if (entry.getKey().startsWith(TaskConstants.TASKS_FOLDERS_TAG) || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject task = entry.getValue().getAsJsonObject();
            JsonElement typeId = task.get(TaskConstants.TAG_TASK);
            JsonElement state = task.get(TaskConstants.TAG_STATE);
            if (typeId == null || !typeId.isJsonPrimitive() || !typeId.getAsJsonPrimitive().isString()
                || state == null || !state.isJsonObject()
            ) {
                continue;
            }
            DBTTaskType type = taskRegistry.getTaskType(typeId.getAsString());
            Class<? extends DBTTaskHandler> handlerClass = type == null ? null : type.getHandlerClass();
            if (handlerClass != null && DBTTaskImportTransformer.class.isAssignableFrom(handlerClass)) {
                DBTTaskImportTransformer transformer = (DBTTaskImportTransformer) type.createHandler();
                changed |= transformer.transformTaskState(state.getAsJsonObject(), projectNames);
            }
        }
        return changed ? GSON.toJson(configuration).getBytes(StandardCharsets.UTF_8) : contents;
    }
}
