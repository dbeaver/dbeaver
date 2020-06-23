/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tasks.ui.sql.internal;

import org.eclipse.osgi.util.NLS;

public class TasksSQLUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.tasks.ui.sql.internal.TSQLUIMessages"; //$NON-NLS-1$

    public static String sql_tool_task_object_selector_dialog_title;
    public static String sql_tool_task_wizard_page_settings_name;
    public static String sql_tool_task_wizard_page_settings_title;
    public static String sql_tool_task_wizard_page_settings_description;
    public static String sql_tool_task_wizard_page_settings_group_label_objects;
    public static String sql_tool_task_wizard_page_settings_tool_item_text_add_string;
    public static String sql_tool_task_wizard_page_settings_tool_item_text_remove_string;
    public static String sql_tool_task_wizard_page_settings_tool_item_text_move_script_up;
    public static String sql_tool_task_wizard_page_settings_tool_item_text_move_script_down;
    public static String sql_tool_task_wizard_page_settings_group_label_settings;
    public static String sql_tool_task_wizard_page_settings_sql_panel_name;
    public static String sql_tool_task_wizard_page_settings_dialog_button_label_copy;
    public static String sql_tool_task_wizard_page_status_name;
    public static String sql_tool_task_wizard_page_status_title;
    public static String sql_tool_task_wizard_page_status_description;
    public static String sql_tool_task_wizard_page_status_activate_page_title;
    public static String sql_tool_task_wizard_page_status_message_console_name_tool_log;
    public static String sql_tool_task_wizard_page_status_update_job_name_update_tool;
    public static String sql_tool_task_wizard_page_status_dummy_load_service_name;
    public static String sql_tool_task_wizard_page_settings_title_sql_preview_error;
    public static String sql_tool_task_wizard_page_settings_message_sql_preview_panel;
    public static String sql_tool_task_wizard_message_error_running_task;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, TasksSQLUIMessages.class);
    }

    private TasksSQLUIMessages() {
    }
}
