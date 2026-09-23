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
 * SQL editor macros constants.
 */
public final class SQLMacrosConstants {

    public static final String MACROS_CONFIG_FILE = "macros.xml"; //$NON-NLS-1$

    /**
     * Placeholder inside a macro query which is replaced by the currently selected editor text on insert.
     */
    public static final String SELECTION_PLACEHOLDER = "$SELECTION$"; //$NON-NLS-1$

    /**
     * Prefix of the apply macro command ids. Each command corresponds to one shortcut slot (Ctrl+Alt+F1..Ctrl+Alt+F12).
     */
    public static final String APPLY_MACRO_COMMAND_PREFIX = "org.jkiss.dbeaver.ui.editors.sql.macro.apply."; //$NON-NLS-1$

    public static final int MACRO_KEY_COUNT = 12;

    private SQLMacrosConstants() {
    }

    /**
     * Returns the human-readable shortcut label for the macro slot with the specified index (0-based).
     */
    @NotNull
    public static String getShortcutLabel(int index) {
        return "Ctrl+Alt+F" + (index + 1); //$NON-NLS-1$
    }
}