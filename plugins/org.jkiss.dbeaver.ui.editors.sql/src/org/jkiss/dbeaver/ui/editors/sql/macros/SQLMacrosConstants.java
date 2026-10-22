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

import org.jkiss.dbeaver.ui.editors.sql.SQLEditorContributions;

/**
 * SQL editor macros constants.
 * <p>
 * There is a single apply-macro command
 * ({@link #APPLY_MACRO_COMMAND_ID}) whose {@link #MACRO_ID_PARAMETER} parameter identifies the
 * macro to apply, so the number of macros is not limited by the number of commands or shortcuts.
 * A macro either keeps the custom key combination assigned by the user in the edit dialog or has
 * no key at all and is applied from the Macros menu only.
 */
public final class SQLMacrosConstants {

    public static final String MACROS_CONFIG_FILE = "macros.xml"; //$NON-NLS-1$

    /**
     * Placeholder inside a macro query which is replaced by the currently selected editor text on insert.
     */
    public static final String SELECTION_PLACEHOLDER = "$SELECTION$"; //$NON-NLS-1$

    /**
     * Id of the apply macro command. The macro to apply is specified by the {@link #MACRO_ID_PARAMETER}
     * command parameter, so there is no limit on the number of macros.
     */
    public static final String APPLY_MACRO_COMMAND_ID = "org.jkiss.dbeaver.ui.editors.sql.macro.apply"; //$NON-NLS-1$

    /**
     * Name of the command parameter which contains the id of the macro to apply.
     */
    public static final String MACRO_ID_PARAMETER = "macroId"; //$NON-NLS-1$

    /**
     * Scheme used by the user-defined macro key bindings.
     */
    public static final String DEFAULT_SCHEME_ID = "org.eclipse.ui.defaultAcceleratorConfiguration"; //$NON-NLS-1$

    /**
     * Key binding context of the focused SQL script editor. User-defined macro bindings are registered in this context.
     */
    public static final String SQL_EDITOR_SCRIPT_FOCUSED_CONTEXT_ID = SQLEditorContributions.SQL_EDITOR_CONTROL_CONTEXT;

    private SQLMacrosConstants() {
    }
}