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

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

/**
 * User-defined SQL macro.
 * <p>
 * A macro is a named SQL snippet. Its query may contain the
 * {@link SQLMacrosConstants#SELECTION_PLACEHOLDER} placeholder which is replaced by the currently
 * selected editor text on insert.
 * <p>
 * A macro may be assigned a key combination captured in the edit dialog; the combination is stored
 * in the {@link KeySequence#format()} form, e.g. {@code ALT+CTRL+F1}. A macro with no custom
 * combination is applicable from the Macros menu only.
 */
public class SQLMacro {

    @NotNull
    private final String id;

    @NotNull
    private String name;

    @NotNull
    private String query;

    @Nullable
    private String shortcut;

    @NotNull
    private MacroAction action;

    public SQLMacro(@NotNull String id, @NotNull String name, @NotNull String query) {
        this(id, name, query, null, MacroAction.INSERT);
    }

    public SQLMacro(
            @NotNull String id,
            @NotNull String name,
            @NotNull String query,
            @NotNull MacroAction action
    ) {
        this(id, name, query, null, action);
    }

    public SQLMacro(
            @NotNull String id,
            @NotNull String name,
            @NotNull String query,
            @Nullable String shortcut,
            @NotNull MacroAction action
    ) {
        this.id = id;
        this.name = name;
        this.query = query;
        this.shortcut = shortcut;
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
     * Returns the key sequence assigned to this macro in the {@link KeySequence#format()} form
     * or null if this macro has no shortcut.
     */
    @Nullable
    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(@Nullable String shortcut) {
        this.shortcut = shortcut;
    }

    /**
     * Returns true if this macro has a keyboard shortcut assigned.
     */
    public boolean hasShortcut() {
        return CommonUtils.isNotEmpty(shortcut);
    }

    /**
     * Returns the shortcut label of this macro or an empty string if no shortcut is assigned.
     */
    @NotNull
    public String getShortcutLabel() {
        return CommonUtils.isEmpty(shortcut) ? "" : shortcut; //$NON-NLS-1$
    }

    /**
     * Returns the shortcut key sequence of this macro or null if it has no shortcut
     * or the assigned sequence cannot be parsed.
     */
    @Nullable
    public KeySequence getShortcutKeySequence() {
        if (CommonUtils.isEmpty(shortcut)) {
            return null;
        }
        try {
            return KeySequence.getInstance(shortcut);
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return hasShortcut() ? name + " (" + getShortcutLabel() + ")" : name; //$NON-NLS-1$ //$NON-NLS-2$
    }
}