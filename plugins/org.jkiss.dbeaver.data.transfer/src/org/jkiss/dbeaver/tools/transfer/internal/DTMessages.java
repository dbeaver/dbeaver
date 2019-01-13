/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

	public static String data_transfer_wizard_name;
	public static String data_transfer_wizard_final_column_source;
	public static String data_transfer_wizard_final_column_source_container;
	public static String data_transfer_wizard_final_column_target;
	public static String data_transfer_wizard_final_column_target_container;
	public static String data_transfer_wizard_final_description;
	public static String data_transfer_wizard_final_group_tables;
	public static String data_transfer_wizard_final_name;
	public static String data_transfer_wizard_final_title;
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
	public static String data_transfer_wizard_output_label_directory;
	public static String data_transfer_wizard_output_label_encoding;
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
	public static String data_transfer_db_consumer_existing_table;
	public static String data_transfer_db_consumer_new_table;
	public static String data_transfer_db_consumer_column_mappings;
	public static String data_transfer_db_consumer_ddl;

    static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DTMessages.class);
	}

	private DTMessages() {
	}
}
