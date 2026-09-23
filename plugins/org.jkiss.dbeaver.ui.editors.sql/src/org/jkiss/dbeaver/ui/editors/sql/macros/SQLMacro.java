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

/**
 * User-defined SQL macro.
 * <p>
 * A macro is a named SQL snippet assigned to a shortcut slot (Ctrl+Alt+F1..Ctrl+Alt+F12).
 * Its query may contain the {@link SQLMacrosConstants#SELECTION_PLACEHOLDER}
 * placeholder which is replaced by the currently selected editor text on insert.
 */
public class SQLMacro {

    @NotNull
    private final String id;

    @NotNull
    private String name;

    @NotNull
    private String query;

    private int shortcutIndex;

    @NotNull
    private MacroAction action;

    public SQLMacro(@NotNull String id, @NotNull String name, @NotNull String query, int shortcutIndex) {
        this(id, name, query, shortcutIndex, MacroAction.INSERT);
    }

    public SQLMacro(
            @NotNull String id,
            @NotNull String name,
            @NotNull String query,
            int shortcutIndex,
            @NotNull MacroAction action
    ) {
        this.id = id;
        this.name = name;
        this.query = query;
        this.shortcutIndex = shortcutIndex;
        this.action = action;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getQuery() {
        return query;
    }

    public void setQuery(@NotNull String query) {
        this.query = query;
    }

    public int getShortcutIndex() {
        return shortcutIndex;
    }

    public void setShortcutIndex(int shortcutIndex) {
        this.shortcutIndex = shortcutIndex;
    }

    /**
     * Returns the action performed when this macro is applied.
     */
    @NotNull
    public MacroAction getAction() {
        return action;
    }

    public void setAction(@NotNull MacroAction action) {
        this.action = action;
    }

    /**
     * Returns the human-readable shortcut label assigned to this macro, e.g. {@code Ctrl+Alt+F1}.
     */
    @NotNull
    public String getShortcutLabel() {
        return SQLMacrosConstants.getShortcutLabel(shortcutIndex);
    }

    @Override
    public String toString() {
        return name + " (" + getShortcutLabel() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}