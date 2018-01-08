/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug.ui.internal;

import org.eclipse.osgi.util.NLS;

public class PostgreDebugUIMessages extends NLS {

    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.postgresql.debug.ui.internal.PostgreDebugUIMessages"; //$NON-NLS-1$
    
    public static String PgSqlLaunchShortcut_e_editor_empty;
    public static String PgSqlLaunchShortcut_e_selection_empty;
    public static String PgSqlLaunchShortcut_select_procedure_message;
    public static String PgSqlLaunchShortcut_select_procedure_title;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, PostgreDebugUIMessages.class);
    }

    private PostgreDebugUIMessages()
    {
    }
}
