/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
    public static String data_transfer_wizard_job_container_name;
    public static String data_transfer_wizard_job_task_export;
    public static String data_transfer_wizard_job_task_export_table_data;
    public static String data_transfer_wizard_job_task_retrieve;
    public static String data_transfer_wizard_output_checkbox_compress;
    public static String data_transfer_wizard_output_checkbox_new_connection;
    public static String data_transfer_wizard_output_checkbox_select_row_count;
    public static String data_transfer_wizard_output_checkbox_fetched_rows_only;
    public static String data_transfer_wizard_output_checkbox_selected_columns_only;
    public static String data_transfer_wizard_output_checkbox_selected_rows_only;
    public static String data_transfer_wizard_output_label_copy_to_clipboard;
    public static String data_transfer_wizard_output_label_use_single_file;
    public static String data_transfer_wizard_output_label_directory;
    public static String data_transfer_wizard_output_label_encoding;
    public static String data_transfer_wizard_output_label_timestamp_pattern;
    public static String data_transfer_wizard_output_label_extract_type;
    public static String data_transfer_wizard_output_label_file_name_pattern;
    public static String data_transfer_wizard_output_label_insert_bom;
    public static String data_transfer_wizard_settings_group_exporter;
    public static String data_transfer_wizard_settings_group_general;
    public static String data_transfer_wizard_settings_label_binaries;
    public static String data_transfer_wizard_settings_label_encoding;
    public static String data_transfer_wizard_settings_label_formatting;

    public static String database_consumer_settings_option_use_transactions;
    public static String database_consumer_settings_option_commit_after;
    public static String database_consumer_settings_option_use_multi_insert;
    public static String database_consumer_settings_option_multi_insert_batch;
    public static String database_consumer_settings_option_skip_bind_values;
    public static String database_consumer_settings_option_disable_batches;
    public static String database_consumer_settings_option_ignore_duplicate_rows;
    public static String database_consumer_settings_option_on_duplicate_key_method_label;
    public static String database_consumer_settings_option_transfer_auto_generated_columns;
    public static String database_consumer_settings_option_disable_referential_integrity;
    public static String database_consumer_settings_option_use_bulk_load;
    public static String database_consumer_settings_option_truncate_before_load;
    public static String database_consumer_settings_option_enable_qm_logging;

    public static String data_transfer_settings_title_find_producer;
    public static String data_transfer_settings_message_find_data_producer;
    public static String data_transfer_settings_title_find_consumer;
    public static String data_transfer_settings_message_find_data_consumer;
    public static String database_consumer_settings_title_init_connection;
    public static String database_consumer_settings_message_error_connecting;
    public static String database_mapping_container_title_attributes_read_failed;
    public static String database_mapping_container_message_get_attributes_from;
    public static String database_transfer_consumer_task_error_occurred_during_data_load;
    public static String database_transfer_consumer_task_error_occurred_during_batch_insert;
    public static String stream_transfer_consumer_title_attributes_read_failed;
    public static String stream_transfer_consumer_message_cannot_get_attributes_from;
    public static String stream_transfer_consumer_title_configuration_load_failed;
    public static String stream_transfer_consumer_message_cannot_load_configuration;

    public static String data_transfer_task_handler_unexpected_error_title;
    public static String data_transfer_task_handler_unexpected_error_message;
    public static String data_transfer_task_handler_resoring_referential_integrity_unexpected_error_title;
    public static String data_transfer_task_handler_resoring_referential_integrity_unexpected_error_message;

    public static String data_transfer_blob_file_conflict_title;
    public static String data_transfer_file_conflict_ask_title;
    public static String data_transfer_file_conflict_ask_message;
    public static String data_transfer_file_conflict_ask;
    public static String data_transfer_file_conflict_cancel;
    public static String data_transfer_file_conflict_override;
    public static String data_transfer_file_conflict_fix_name;
    public static String data_transfer_file_conflict_append;
    public static String data_transfer_file_conflict_behavior_setting;
    public static String data_transfer_blob_file_conflict_behavior_setting;
    public static String data_transfer_file_conflict_behavior_apply_to_all;
    public static String data_transfer_file_conflict_behavior_apply_to_all_for_current_object;

    public static String data_transfer_summary_title;

    public static String pref_data_transfer_name_case_default;
    public static String pref_data_transfer_name_case_upper;
    public static String pref_data_transfer_name_case_lower;
    public static String pref_data_transfer_replacing_combo_do_not;
    public static String pref_data_transfer_replacing_combo_underscores;
    public static String pref_data_transfer_replacing_combo_camel_case;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DTMessages.class);
    }

    private DTMessages() {
    }
}
