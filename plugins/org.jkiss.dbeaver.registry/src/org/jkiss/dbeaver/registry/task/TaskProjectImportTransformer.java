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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProjectImportTransformer;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTaskHandler;
import org.jkiss.dbeaver.model.task.DBTTaskImportTransformer;
import org.jkiss.dbeaver.model.task.DBTTaskRegistry;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.runtime.DBInterruptedException;

import java.io.StringReader;
import java.util.Map;

public class TaskProjectImportTransformer implements DBPProjectImportTransformer {
    private static final Log log = Log.getLog(TaskProjectImportTransformer.class);

    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        // Import must not round unrelated numeric settings while remapping project references.
        .setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER)
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
    public String transform(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String contents,
        @NotNull Map<String, String> projectNames
    ) throws DBException {
        if (projectNames.entrySet().stream().allMatch(entry -> entry.getKey().equals(entry.getValue()))) {
            return contents;
        }
        Map<String, Object> configuration;
        try {
            configuration = JSONUtils.parseMap(GSON, new StringReader(contents));
        } catch (JsonParseException e) {
            throw new DBException("Cannot transform imported task configuration", e);
        }
        boolean changed = false;
        // Use the task settings representation without instantiating tasks, so unknown task types survive import.
        for (Map.Entry<String, Object> entry : configuration.entrySet()) {
            if (monitor.isCanceled()) {
                throw new DBInterruptedException("Project import canceled");
            }
            if (entry.getKey().startsWith(TaskConstants.TASKS_FOLDERS_TAG) || !(entry.getValue() instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> task = JSONUtils.getObject(configuration, entry.getKey());
            if (!(task.get(TaskConstants.TAG_TASK) instanceof String typeId)
                || !(task.get(TaskConstants.TAG_STATE) instanceof Map<?, ?>)
            ) {
                continue;
            }
            Map<String, Object> state = JSONUtils.getObject(task, TaskConstants.TAG_STATE);
            DBTTaskType type = taskRegistry.getTaskType(typeId);
            if (type == null) {
                log.warn("Cannot remap project references for task '" + entry.getKey()
                    + "': task type '" + typeId + "' is unavailable");
                continue;
            }
            Class<? extends DBTTaskHandler> handlerClass = type.getHandlerClass();
            if (handlerClass == null) {
                log.warn("Cannot remap project references for task '" + entry.getKey()
                    + "': handler for task type '" + typeId + "' is unavailable");
            } else if (DBTTaskImportTransformer.class.isAssignableFrom(handlerClass)) {
                DBTTaskImportTransformer transformer = (DBTTaskImportTransformer) type.createHandler();
                changed |= transformer.transformTaskState(state, projectNames);
            } else {
                log.debug("Skipping project reference remapping for task '" + entry.getKey()
                    + "': task type '" + typeId + "' does not support import transformation");
            }
        }
        // As in LocalTaskController, serialize the parsed map directly. Unlike saving live task settings,
        // importing must also preserve unknown properties, nulls, and empty collections.
        return changed ? GSON.toJson(configuration) : contents;
    }
}
