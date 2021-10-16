/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.json.internal;

import org.eclipse.osgi.util.NLS;

public class JSONEditorMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.json.internal.JSONEditorMessages"; //$NON-NLS-1$

    public static String JSONEditorPart_title;
    public static String JSONPanelEditor_e_load_json;
    public static String JSONPanelEditor_e_save_json;
    public static String JSONPanelEditor_subtask_prime_task;
    public static String JSONPanelEditor_task_prime;
    public static String JSONPanelEditor_task_read_json;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, JSONEditorMessages.class);
    }

    private JSONEditorMessages()
    {
    }
}
