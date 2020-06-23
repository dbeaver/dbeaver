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
package org.jkiss.dbeaver.tools.transfer.internal;

import org.eclipse.osgi.util.NLS;

public class DTMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.tools.transfer.internal.DTMessages"; //$NON-NLS-1$

	public static String data_transfer_wizard_init_column_description;
	public static String data_transfer_wizard_init_column_exported;
	public static String data_transfer_wizard_init_description;
	public static String data_transfer_wizard_init_name;
	public static String data_transfer_wizard_init_title;
	public static String data_transfer_wizard_job_container_name;
	public static String data_transfer_wizard_job_name;
	public static String data_transfer_wizard_job_task_export;
	public static String data_transfer_wizard_job_task_export_table_data;
	public static String data_transfer_wizard_job_task_retrieve;
	public static String data_transfer_wizard_output_checkbox_compress;
	public static String data_transfer_wizard_output_checkbox_split_files;
	public static String data_transfer_wizard_output_checkbox_split_files_tip;
	public static String data_transfer_wizard_output_checkbox_new_connection;
	public static String data_transfer_wizard_output_checkbox_open_folder;
	public static String data_transfer_wizard_output_checkbox_select_row_count;
	public static String data_transfer_wizard_output_checkbox_selected_columns_only;
	public static String data_transfer_wizard_output_checkbox_selected_rows_only;
	public static String data_transfer_wizard_output_combo_extract_type_item_by_segments;
	public static String data_transfer_wizard_output_combo_extract_type_item_single_query;
	public static String data_transfer_wizard_output_description;
	public static String data_transfer_wizard_output_dialog_directory_message;
	public static String data_transfer_wizard_output_dialog_directory_text;
	public static String data_transfer_wizard_output_group_general;
	public static String data_transfer_wizard_output_group_progress;
	public static String data_transfer_wizard_output_label_copy_to_clipboard;
	public static String data_transfer_wizard_output_label_use_single_file;
	public static String data_transfer_wizard_output_label_use_single_file_tip;
	public static String data_transfer_wizard_output_label_directory;
	public static String data_transfer_wizard_output_label_encoding;
	public static String data_transfer_wizard_output_label_timestamp_pattern;
	public static String data_transfer_wizard_output_label_extract_type;
	public static String data_transfer_wizard_output_label_file_name_pattern;
	public static String data_transfer_wizard_output_label_insert_bom;
	public static String data_transfer_wizard_output_label_insert_bom_tooltip;
	public static String data_transfer_wizard_output_label_max_threads;
	public static String data_transfer_wizard_output_label_segment_size;
	public static String data_transfer_wizard_output_name;
	public static String data_transfer_wizard_output_title;
	public static String data_transfer_wizard_settings_binaries_item_inline;
	public static String data_transfer_wizard_settings_binaries_item_save_to_file;
	public static String data_transfer_wizard_settings_binaries_item_set_to_null;
	public static String data_transfer_wizard_settings_button_edit;
	public static String data_transfer_wizard_settings_description;
	public static String data_transfer_wizard_settings_group_exporter;
	public static String data_transfer_wizard_settings_group_general;
	public static String data_transfer_wizard_settings_label_binaries;
	public static String data_transfer_wizard_settings_label_encoding;
	public static String data_transfer_wizard_settings_label_formatting;
	public static String data_transfer_wizard_settings_listbox_formatting_item_default;
	public static String data_transfer_wizard_settings_name;
	public static String data_transfer_wizard_settings_title;
	public static String data_transfer_wizard_page_input_files_name;
	public static String data_transfer_wizard_page_input_files_title;
	public static String data_transfer_wizard_page_input_files_description;
	public static String data_transfer_wizard_page_preview_name;
	public static String data_transfer_wizard_page_preview_title;
	public static String data_transfer_wizard_page_preview_description;
	public static String data_transfer_wizard_producers_title;
	public static String data_transfer_wizard_producers_description;
	public static String data_transfer_wizard_mappings_name;
	public static String data_transfer_wizard_settings_group_input_files;
	public static String data_transfer_wizard_settings_group_importer;
	public static String data_transfer_wizard_settings_group_column_mappings;
	public static String data_transfer_wizard_settings_group_preview;
	public static String data_transfer_wizard_settings_group_preview_table;
	public static String data_transfer_wizard_settings_group_preview_columns;
	public static String data_transfer_wizard_settings_column_mapping_type;
	public static String data_transfer_db_consumer_target_container;
	public static String data_transfer_db_consumer_choose_container;
	public static String data_transfer_db_consumer_auto_assign;
	public static String data_transfer_db_consumer_existing_table;
	public static String data_transfer_db_consumer_new_table;
	public static String data_transfer_db_consumer_column_mappings;
	public static String data_transfer_db_consumer_ddl;

	public static String sql_script_task_title;
	public static String sql_script_task_page_settings_title;
	public static String sql_script_task_page_settings_description;
	public static String sql_script_task_page_settings_group_files;
	public static String sql_script_task_page_settings_group_connections;
	public static String sql_script_task_page_settings_group_script;
	public static String sql_script_task_page_settings_option_ignore_errors;
	public static String sql_script_task_page_settings_option_dump_results;
	public static String sql_script_task_page_settings_option_auto_commit;
	public static String database_consumer_settings_option_use_transactions;
	public static String database_consumer_settings_option_commit_after;
	public static String database_consumer_settings_option_transfer_auto_generated_columns;
	public static String database_consumer_settings_option_truncate_before_load;

	public static String data_transfer_settings_title_find_producer;
	public static String data_transfer_settings_message_find_data_producer;
	public static String data_transfer_settings_title_find_consumer;
	public static String data_transfer_settings_message_find_data_consumer;
	public static String data_transfer_settings_title_configuration_error;
	public static String data_transfer_settings_message_error_reading_task_configuration;
	public static String database_consumer_settings_title_init_connection;
	public static String database_consumer_settings_message_error_connecting;
	public static String database_mapping_container_title_attributes_read_failed;
	public static String database_mapping_container_message_get_attributes_from;
	public static String database_transfer_consumer_task_error_occurred_during_data_load;
	public static String stream_transfer_consumer_title_run_process;
	public static String stream_transfer_consumer_message_error_running_process;



	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DTMessages.class);
	}

	private DTMessages() {
	}
}
