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
package org.jkiss.dbeaver.tasks.ui.internal;

import org.eclipse.osgi.util.NLS;

public class TaskUIMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.tasks.ui.internal.TaskUIMessages"; //$NON-NLS-1$

	public static String edit_task_config_dialog_title_edit_task;
	public static String edit_task_config_dialog_title_create_task;
	public static String edit_task_config_dialog_label_type;
	public static String edit_task_config_dialog_label_name;
	public static String edit_task_config_dialog_label_descr;
	
	public static String edit_task_variabl_dialog_title_task_variables;
	public static String edit_task_variabl_dialog_column_variable;
	public static String edit_task_variabl_dialog_column_value;
	
	public static String task_config_wizard_group_task_label;
	public static String task_config_wizard_button_save_task;
	public static String task_config_wizard_link_open_tasks_view;
	public static String task_config_wizard_button_variables;
	
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
	
	public static String task_config_wizard_stub_title_create_task;
	public static String task_config_wizard_stub_page_name_void;
	
	public static String task_processor_ui_message_task_completed;
	
	public static String db_tasks_selector_dialog;
	
	public static String db_tasks_tree_text_tasks_type;
	public static String db_tasks_tree_column_controller_tasks;
	public static String db_tasks_tree_column_controller_add_name;
	public static String db_tasks_tree_column_controller_add_descr_name;
	public static String db_tasks_tree_column_controller_add_name_created;
	public static String db_tasks_tree_column_controller_add_descr_create_time;
	public static String db_tasks_tree_column_controller_add_name_last_run;
	public static String db_tasks_tree_column_controller_add_descr_start_time;
	public static String db_tasks_tree_column_controller_add_name_last_duration;
	public static String db_tasks_tree_column_controller_add_descr_run_duration;
	public static String db_tasks_tree_column_controller_add_name_last_result;
	public static String db_tasks_tree_column_controller_add_descr_last_result;
	public static String db_tasks_tree_column_cell_text_success;
	public static String db_tasks_tree_column_controller_add_name_next_run;
	public static String db_tasks_tree_column_controller_add_descr_next_run;
	public static String db_tasks_tree_column_controller_add_name_description;
	public static String db_tasks_tree_column_controller_add_descr_task_description;
	public static String db_tasks_tree_column_controller_add_name_type;
	public static String db_tasks_tree_column_controller_add_descr_task_type;
	public static String db_tasks_tree_column_controller_add_name_category;
	public static String db_tasks_tree_column_controller_add_descr_category;
	public static String db_tasks_tree_column_controller_add_name_project;
	public static String db_tasks_tree_column_controller_add_descr_project;
	
	public static String db_tasks_view_filtered_tree_text_error_message;
	public static String db_tasks_view_column_controller_add_name_time;
	public static String db_tasks_view_column_controller_add_descr_start_time;
	public static String db_tasks_view_column_controller_add_name_duration;
	public static String db_tasks_view_column_controller_add_descr_task_duration;
	public static String db_tasks_view_column_controller_add_name_result;
	public static String db_tasks_view_column_controller_add_descr_task_result;
	public static String db_tasks_view_cell_text_success;
	public static String db_tasks_view_context_menu_command_delete_task;
	public static String db_tasks_view_adapter_label_database_tasks;
	public static String db_tasks_view_run_log_view;
	public static String db_tasks_view_run_log_delete;
	public static String db_tasks_view_run_log_confirm_remove;
	public static String db_tasks_view_run_log_confirm_delete_task;
	public static String db_tasks_view_clear_run_log_clear;
	public static String db_tasks_view_clear_run_log_confirm_clear;
	public static String db_tasks_view_clear_run_log_confirm_delete_log;
	public static String db_tasks_view_open_run_log_folder_open;
	
	public static String task_handler_copy_name_dialog_enter_task;
	
	public static String task_handler_delete_confirm_title_delete_task;
	public static String task_handler_delete_confirm_question_delete_task;
	public static String task_handler_delete_confirm_title_delete_tasks;
	public static String task_handler_delete_confirm_question_delete_tasks;
	
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, TaskUIMessages.class);
	}

	private TaskUIMessages() {
	}
}
