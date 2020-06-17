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
package org.jkiss.dbeaver.tools.transfer.ui.internal;

import org.eclipse.osgi.util.NLS;

public class DTUIMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages"; //$NON-NLS-1$

	public static String data_transfer_wizard_name;
	public static String data_transfer_wizard_final_column_source;
	public static String data_transfer_wizard_final_column_source_container;
	public static String data_transfer_wizard_final_column_target;
	public static String data_transfer_wizard_final_column_target_container;
	public static String data_transfer_wizard_final_description;
	public static String data_transfer_wizard_final_group_tables;
	public static String data_transfer_wizard_final_group_objects;
	public static String data_transfer_wizard_final_group_settings_source;
	public static String data_transfer_wizard_final_group_settings_target;
	public static String data_transfer_wizard_final_name;
	public static String data_transfer_wizard_final_title;

	public static String database_consumer_wizard_name;
	public static String database_consumer_wizard_title;
	public static String database_consumer_wizard_description;
	public static String database_consumer_wizard_transfer_checkbox_label;
	public static String database_consumer_wizard_transfer_checkbox_tooltip;
	public static String database_consumer_wizard_truncate_checkbox_label;
	public static String database_consumer_wizard_performance_group_label;
	public static String database_consumer_wizard_transactions_checkbox_label;
	public static String database_consumer_wizard_commit_spinner_label;
	public static String database_consumer_wizard_general_group_label;
	public static String database_consumer_wizard_table_checkbox_label;
	public static String database_consumer_wizard_final_message_checkbox_label;
	public static String database_consumer_wizard_truncate_checkbox_title;
	public static String database_consumer_wizard_truncate_checkbox_question;
	
	public static String columns_mapping_dialog_shell_text;
	public static String columns_mapping_dialog_composite_label_text_source_container;
	public static String columns_mapping_dialog_composite_label_text_source_entity;
	public static String columns_mapping_dialog_composite_label_text_target_container;
	public static String columns_mapping_dialog_composite_label_text_target_entity;
	public static String columns_mapping_dialog_column_source_text;
	public static String columns_mapping_dialog_column_source_type_text;
	public static String columns_mapping_dialog_column_target_text;
	public static String columns_mapping_dialog_column_target_type_text;
	public static String columns_mapping_dialog_cell_text_existing;
	public static String columns_mapping_dialog_cell_text_new;
	public static String columns_mapping_dialog_cell_text_skip;
	public static String columns_mapping_dialog_column_type_text_mapping;

	public static String database_consumer_page_mapping_name_and_title;
	public static String database_consumer_page_mapping_description;
	public static String database_consumer_page_mapping_column_source_text;
	public static String database_consumer_page_mapping_column_target_text;
	public static String database_consumer_page_mapping_column_mapping_text;
	public static String database_consumer_page_mapping_node_title;
	public static String database_consumer_page_mapping_table_name;
	public static String database_consumer_page_mapping_label_hint;
	public static String database_consumer_page_mapping_monitor_task;
	public static String database_consumer_page_mapping_sqlviewer_title;

	public static String database_producer_page_extract_settings_name_and_title;
	public static String database_producer_page_extract_settings_description;
	public static String database_producer_page_extract_settings_threads_num_text_tooltip;
	public static String database_producer_page_extract_settings_new_connection_checkbox_tooltip;
	public static String database_producer_page_extract_settings_row_count_checkbox_tooltip;
	public static String database_producer_page_extract_settings_text_fetch_size_label;
	public static String database_producer_page_extract_settings_text_fetch_size_tooltip;

	public static String database_producer_page_input_objects_name;
	public static String database_producer_page_input_objects_title;
	public static String database_producer_page_input_objects_description;
	public static String database_producer_page_input_objects_item_text_none;
	public static String database_producer_page_input_objects_node_select_table;
	public static String database_producer_page_input_objects_node_select_target;
	public static String database_producer_page_input_objects_node_select_source;

	public static String stream_consumer_page_output_checkbox_execute_process;
	public static String stream_consumer_page_output_label_maximum_file_size;
	public static String stream_consumer_page_output_label_show_finish_message;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DTUIMessages.class);
	}

	private DTUIMessages() {
	}
}
