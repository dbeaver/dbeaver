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
package org.jkiss.dbeaver.ui.editors.sql.macros;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * What should happen when a macro is applied.
 */
public enum MacroAction {

    /**
     * Insert the macro text into the editor only.
     */
    INSERT("insert"),

    /**
     * Insert the macro text into the editor and execute it right away.
     */
    INSERT_AND_EXECUTE("insert-execute"),

    /**
     * Execute the macro query without inserting it into the editor.
     */
    EXECUTE_ONLY("execute");

    @NotNull
    private final String id;

    MacroAction(@NotNull String id) {
        this.id = id;
    }

    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Resolves the action by its persistence id.
     * <p>
     * For backward compatibility, a legacy {@code execute="true"} configuration
     * (which had the meaning of insert-and-execute) is mapped to {@link #INSERT_AND_EXECUTE}.
     */
    @NotNull
    public static MacroAction byId(@Nullable String id, boolean legacyExecuteImmediately) {
        if (id != null) {
            for (MacroAction action : values()) {
                if (action.id.equals(id)) {
                    return action;
                }
            }
        }
        return legacyExecuteImmediately ? INSERT_AND_EXECUTE : INSERT;
    }
}