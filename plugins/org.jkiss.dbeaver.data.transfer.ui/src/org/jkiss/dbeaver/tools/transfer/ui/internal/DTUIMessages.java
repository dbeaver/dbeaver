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
package org.jkiss.dbeaver.tools.transfer.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

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
    public static String database_consumer_wizard_truncate_checkbox_description;
    public static String database_consumer_wizard_disable_referential_integrity_label;
    public static String database_consumer_wizard_disable_referential_integrity_tip_start;
    public static String database_consumer_wizard_performance_group_label;
    public static String database_consumer_wizard_transactions_checkbox_label;
    public static String database_consumer_wizard_commit_spinner_label;
    public static String database_consumer_wizard_general_group_label;
    public static String database_consumer_wizard_table_checkbox_label;
    public static String database_consumer_wizard_final_message_checkbox_label;
    public static String database_consumer_wizard_truncate_checkbox_title;
    public static String database_consumer_wizard_truncate_checkbox_question;
    public static String database_consumer_wizard_checkbox_multi_insert_label;
    public static String database_consumer_wizard_checkbox_multi_insert_description;
    public static String database_consumer_wizard_spinner_multi_insert_batch_size;
    public static String database_consumer_wizard_checkbox_multi_insert_skip_bind_values_label;
    public static String database_consumer_wizard_checkbox_multi_insert_skip_bind_values_description;
    public static String database_consumer_wizard_disable_import_batches_label;
    public static String database_consumer_wizard_disable_import_batches_description;
    public static String database_consumer_wizard_ignore_duplicate_rows_label;
    public static String database_consumer_wizard_ignore_duplicate_rows_tip;
    public static String database_consumer_wizard_use_bulk_load_label;
    public static String database_consumer_wizard_use_bulk_load_description;
    public static String database_consumer_wizard_on_duplicate_key_insert_method_text;
    public static String database_consumer_wizard_link_label_replace_method_wiki;
    public static String database_consumer_wizard_label_replace_method_not_supported;

    public static String columns_mapping_dialog_shell_text;
    public static String columns_mapping_dialog_composite_label_text_source_container;
    public static String columns_mapping_dialog_composite_label_text_source_entity;
    public static String columns_mapping_dialog_composite_label_text_target_container;
    public static String columns_mapping_dialog_composite_label_text_target_entity;
    public static String columns_mapping_dialog_composite_button_reconnect;
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
    public static String database_consumer_page_mapping_column_transformer_text;
    public static String database_consumer_page_mapping_column_transformer_tip;
    public static String database_consumer_page_mapping_node_title;
    public static String database_consumer_page_mapping_table_name;
    public static String database_consumer_page_mapping_label_hint;
    public static String database_consumer_page_mapping_monitor_task;
    public static String database_consumer_page_mapping_sqlviewer_title;
    public static String database_consumer_page_mapping_sqlviewer_nonsql_tables_message;
    public static String database_consumer_page_mapping_recreate_confirm_title;
    public static String database_consumer_page_mapping_recreate_confirm_tip;

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

    public static String stream_consumer_page_output_label_maximum_file_size;
    public static String stream_consumer_page_output_label_show_finish_message;
    public static String stream_consumer_page_output_tooltip_output_directory_pattern;
    public static String stream_consumer_page_output_tooltip_output_file_name_pattern;
    public static String stream_consumer_page_output_label_results;
    public static String stream_consumer_page_output_variables_hint_label;
    public static String stream_consumer_page_settings_title;
    public static String stream_consumer_page_settings_description;
    public static String stream_consumer_page_settings_item_text_none;
    public static String stream_consumer_page_warning_not_enough_sources_chosen;
    public static String stream_consumer_page_mapping_title;
    public static String stream_consumer_page_mapping_label_configure;
    public static String stream_consumer_page_mapping_button_configure;
    public static String stream_consumer_page_mapping_name_column_name;
    public static String stream_consumer_page_mapping_mapping_column_name;
    public static String stream_consumer_page_mapping_label_error_no_columns_selected_text;

    public static String data_transfer_task_configurator_group_label_export_tables;
    public static String data_transfer_task_configurator_group_label_import_into;
    public static String data_transfer_task_configurator_table_column_text_object;
    public static String data_transfer_task_configurator_table_column_text_data_source;
    public static String data_transfer_task_configurator_dialog_button_label_add_table;
    public static String data_transfer_task_configurator_tables_title_choose_source;
    public static String data_transfer_task_configurator_tables_title_choose_target;
    public static String data_transfer_task_configurator_dialog_button_label_add_query;
    public static String data_transfer_task_configurator_sql_query_title;
    public static String data_transfer_task_configurator_dialog_button_label_remove;
    public static String data_transfer_task_configurator_confirm_action_title;
    public static String data_transfer_task_configurator_confirm_action_question;

    public static String sql_script_task_configuration_wizard_default_window_title;
    public static String sql_script_task_data_source_selection_dialog_column_description_script;
    public static String sql_script_task_data_source_selection_dialog_column_description_script_data_source;
    public static String sql_script_task_page_settings_tool_item_text_add_script;
    public static String sql_script_task_page_settings_tool_item_text_remove_script;
    public static String sql_script_task_page_settings_tool_item_text_move_script_up;
    public static String sql_script_task_page_settings_tool_item_text_move_script_down;
    public static String sql_script_task_page_settings_tool_item_text_add_data_source;
    public static String sql_script_task_page_settings_tool_item_text_remove_data_source;
    public static String sql_script_task_page_settings_tool_item_text_move_data_source_up;
    public static String sql_script_task_page_settings_tool_item_text_move_data_source_down;
    public static String sql_script_task_selector_dialog_column_description_script;
    public static String sql_script_task_selector_dialog_column_description_script_data_source;

    public static String data_transfer_handler_title_data_transfer_error;
    public static String data_transfer_handler_message_data_transfer_error;
    public static String database_consumer_page_mapping_title_error_mapping_table;
    public static String database_consumer_page_mapping_message_error_mapping_target_table;
    public static String database_consumer_page_mapping_title_mapping_error;
    public static String database_consumer_page_mapping_message_error_setting_target_table;
    public static String database_consumer_page_mapping_message_error_auto_mapping_source_table;
    public static String database_consumer_page_mapping_message_error_mapping_existing_table;
    public static String database_consumer_page_mapping_message_error_mapping_new_table;
    public static String database_consumer_page_mapping_title_target_DDL;
    public static String database_consumer_page_mapping_message_error_generating_target_DDL;
    public static String database_consumer_page_mapping_title_target_table;
    public static String database_consumer_page_mapping_message_error_generating_target_table;
    public static String database_producer_page_input_objects_title_assign_error;
    public static String database_producer_page_input_objects_message_error_reading_container_objects;
    public static String database_producer_page_input_objects_error_message_auto_assign_failed;
    public static String stream_producer_column_mapping_error_title;
    public static String stream_producer_column_mapping_error_message;
    public static String stream_producer_select_input_file;
    public static String stream_producer_page_input_files_hint;
    public static String stream_producer_page_preview_title_load_entity_meta;
    public static String stream_producer_page_preview_message_entity_attributes;
    public static String stream_producer_page_preview_title_preview_data;
    public static String stream_producer_page_preview_message_preview_data;
    public static String data_transfer_page_final_title_error_initializing_transfer_pipe;
    public static String data_transfer_page_final_message_error_initializing_data_transfer_pipe;
    public static String data_transfer_task_configurator_title_error_opening_data_source;
    public static String data_transfer_task_configurator_message_error_while_opening_data_source;
    public static String data_transfer_wizard_message_init_data_transfer;
    public static String data_transfer_error_source_not_specified;
    public static String data_transfer_error_target_not_specified;
    public static String data_transfer_error_no_objects_selected;

    public static String stream_producer_page_preview_error_message_no_entities_specified;
    public static String stream_producer_page_preview_error_message_wrong_input_object;
    public static String stream_producer_page_preview_error_message_set_mappings_for_all_columns;
    public static String database_consumer_page_mapping_error_message_set_target_container;
    public static String database_consumer_page_mapping_error_message_set_all_tables_mappings;
    public static String database_consumer_page_mapping_error_no_schema_changes_title;
    public static String database_consumer_page_mapping_error_no_schema_changes_info;
    public static String database_consumer_page_mapping_error_schema_save_title;
    public static String database_consumer_page_mapping_error_schema_save_info;
    public static String database_consumer_page_mapping_create_target_object_confirmation_title;
    public static String database_consumer_page_mapping_create_target_object_confirmation_question;
    public static String sql_script_task_page_settings_error_message_you_must_select_script_execute;
    public static String sql_script_task_page_settings_error_message_you_must_select_connection;

    public static String page_configure_metadata_title;
    public static String page_configure_table_properties_tab_title;
    public static String page_configure_table_DDL_button_execute;
    public static String page_configure_table_DDL_button_copy;
    public static String page_configure_table_properties_text;
    public static String page_configure_table_properties_no_properties;
    public static String page_configure_table_properties_info_text;

    public static String value_format_selector_value;
    public static String value_format_selector_display;
    public static String value_format_selector_editable;
    public static String value_format_selector_database_native;

    public static String data_transfer_event_processor_execute_command_command;
    public static String data_transfer_event_processor_execute_command_working_directory;
    public static String data_transfer_event_processor_execute_command_working_directory_title;

    public static String pref_data_transfer_wizard_title;
    public static String pref_data_transfer_wizard_reconnect_to_database;
    public static String pref_data_transfer_options_title;
    public static String pref_data_transfer_options_fallback_directory;
    public static String pref_data_transfer_options_fallback_directory_tip;
    public static String pref_data_transfer_mapping_group;
    public static String pref_data_transfer_info_label_mapping;
    public static String pref_data_transfer_name_case_label;
    public static String pref_data_transfer_replacing_combo_label;
    public static String pref_data_transfer_replacing_combo_tip;
    public static String pref_data_transfer_spanner_max_length;
    public static String pref_data_transfer_spanner_max_length_tip;

    public static String mappings_rules_dialog_title;
    public static String mappings_rules_dialog_save_settings_checkbox;
    public static String mappings_rules_dialog_save_settings_checkbox_tip;
    public static String mappings_rules_dialog_confirmation_title;
    public static String mappings_rules_dialog_confirmation_message;

    public static String dialog_policy_data_export_title;
    public static String dialog_policy_data_export_msg;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DTUIMessages.class);
    }

    private DTUIMessages() {
    }
}
