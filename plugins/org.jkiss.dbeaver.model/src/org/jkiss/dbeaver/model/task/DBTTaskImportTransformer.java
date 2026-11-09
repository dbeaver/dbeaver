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
package org.jkiss.dbeaver.model.task;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

import java.util.Map;

/**
 * Optional capability of a task handler for relocating persisted task state during project import.
 * Implementations must not resolve database objects or execute tasks.
 */
public interface DBTTaskImportTransformer {
    String PROJECT_PROPERTY = "project";

    /**
     * Updates only references understood by this task type, preserving unknown state.
     * Project names embedded in resource/navigator paths are deliberately not handled yet.
     *
     * @param state mutable task properties in the same map representation used by task settings
     * @param projectNames mapping from original project names to destination project names
     * @return whether the state changed
     */
    boolean transformTaskState(@NotNull Map<String, Object> state, @NotNull Map<String, String> projectNames) throws DBException;

    /**
     * Remaps the direct string-valued {@code project} property of a task-state map in place.
     * Uses a case-sensitive lookup; does not traverse nested objects or rewrite embedded paths.
     *
     * @param object       mutable task-state map containing the project reference; null and non-map values are ignored
     * @param projectNames mapping from original project names to destination project names
     * @return {@code true} if the project property changed; {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    static boolean remapProjectReference(@Nullable Object object, @NotNull Map<String, String> projectNames) {
        if (!(object instanceof Map<?, ?> properties) || !(properties.get(PROJECT_PROPERTY) instanceof String oldName)) {
            return false;
        }
        String newName = projectNames.get(oldName);
        if (newName == null || newName.equals(oldName)) {
            return false;
        }
        // Task settings represent JSON objects as mutable maps with string keys and Object values.
        ((Map<String, Object>) properties).put(PROJECT_PROPERTY, newName);
        return true;
    }
}
