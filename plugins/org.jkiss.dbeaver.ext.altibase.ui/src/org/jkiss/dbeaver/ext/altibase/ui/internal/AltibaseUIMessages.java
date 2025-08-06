/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class AltibaseUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.altibase.ui.internal.AltibaseUIMessages"; //$NON-NLS-1$

    static {
        NLS.initializeMessages(BUNDLE_NAME, AltibaseUIMessages.class);
    }
    
    public static String editors_altibase_session_editor_confirm_action;
    public static String editors_altibase_session_editor_action_disconnect_session;
    
    public static String pref_page_altibase_explain_plan_legend;
    public static String pref_page_altibase_explain_plan_content;
    public static String pref_page_altibase_legend_misc;
    public static String pref_page_altibase_checkbox_enable_dbms_output;
    public static String pref_page_altibase_checkbox_plan_prefix_depth;
    
    public static String edit_altibase_typeset_manager_dialog_title;
    public static String edit_altibase_typeset_manager_container;
    public static String edit_altibase_typeset_manager_name;
    
    private AltibaseUIMessages() {
    }
}
