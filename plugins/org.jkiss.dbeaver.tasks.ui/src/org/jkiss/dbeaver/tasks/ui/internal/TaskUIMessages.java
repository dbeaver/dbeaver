/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class TaskUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages"; //$NON-NLS-1$

    public static String edit_task_config_dialog_title_edit_task;
    public static String edit_task_config_dialog_title_create_task;
    public static String edit_task_config_dialog_label_type;
    public static String edit_task_config_dialog_label_name;
    public static String edit_task_config_dialog_label_descr;
    public static String edit_task_config_dialog_task_folders_label_name;

    public static String edit_task_variabl_dialog_title_task_variables;
    public static String edit_task_variabl_dialog_column_task;
    public static String edit_task_variabl_dialog_column_variable;
    public static String edit_task_variabl_dialog_column_value;

    public static String task_config_wizard_group_task_label;
    public static String task_config_wizard_button_save_task;
    public static String task_config_wizard_link_open_tasks_view;
    public static String task_config_wizard_button_variables;
    public static String task_config_wizard_button_variables_configure;
    public static String task_config_wizard_button_variables_prompt;
    public static String task_config_wizard_button_variables_prompt_tip;

    public static String task_config_wizard_dialog_button_save;

    public static String task_config_wizard_page_settings_create_task;
    public static String task_config_wizard_page_settings_edit_task;
    public static String task_config_wizard_page_settings_title_task_prop;
    public static String task_config_wizard_page_settings_descr_set_task;
    public static String task_config_wizard_page_settings_config;

    public static String task_config_wizard_page_task_title_new_task_prop;
    public static String task_config_wizard_page_task_label_task_type;
    public static String task_config_wizard_page_task_control_label_category;
    public static String task_config_wizard_page_task_control_label_type;
    public static String task_config_wizard_page_task_control_label_task_info;
    public static String task_config_wizard_page_task_text_label_name;
    public static String task_config_wizard_page_task_control_label_descr;
    public static String task_config_wizard_page_task_text_label_task_id;
    public static String task_config_wizard_page_task_create_folder_label;
    public static String task_config_wizard_page_task_no_task_types_available;

    public static String task_config_wizard_stub_title_create_task;
    public static String task_config_wizard_stub_page_name_void;

    public static String task_processor_ui_message_task_completed;

    public static String task_configuration_wizard_page_task_error_message_enter_task_name;
    public static String task_configuration_wizard_page_task_already_exists;
    public static String task_configuration_wizard_page_task_enter_type;
    public static String task_configuration_wizard_page_settings_fill_parameters;
    public static String task_configuration_wizard_dialog_configuration_error;
    public static String task_execute_handler_tool_error_title;
    public static String task_execute_handler_tool_error_message;
    public static String task_execute_handler_tool_error_project_message;
    public static String task_execute_handler_tool_error_apply_message;
    public static String task_execute_handler_tool_warn_readonly_title;
    public static String task_execute_handler_tool_warn_readonly_message;

    public static String task_config_wizard_page_task_advanced_label;
    public static String task_config_wizard_page_task_max_exec_time;
    public static String task_config_wizard_page_task_max_exec_time_descr;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, TaskUIMessages.class);
    }

    private TaskUIMessages() {
    }
}
