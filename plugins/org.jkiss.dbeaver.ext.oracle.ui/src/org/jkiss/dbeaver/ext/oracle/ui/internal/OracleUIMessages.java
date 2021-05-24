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

package org.jkiss.dbeaver.ext.oracle.ui.internal;

import org.eclipse.osgi.util.NLS;

public final class OracleUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, OracleUIMessages.class);
    }

    private OracleUIMessages() {
    }

    public static String config_import_wizard_page_sql_developer_label_installation_not_found;
    public static String dialog_connection_advanced_tab;
    public static String dialog_connection_advanced_tab_tooltip;
    public static String dialog_connection_basic_tab;
    public static String dialog_connection_connection_type_group;
    public static String dialog_connection_custom_tab;
    public static String dialog_connection_general_tab;
    public static String dialog_connection_general_tab_tooltip;
    public static String dialog_connection_host;
    public static String dialog_connection_ora_home;
    public static String dialog_connection_os_authentication;
    public static String dialog_connection_password;
    public static String dialog_connection_port;
    public static String dialog_connection_role;
    public static String dialog_connection_security_group;
    public static String dialog_connection_select_ora_home_msg;
    public static String dialog_connection_sid_service;
    public static String dialog_connection_sid;
    public static String dialog_connection_service;
    public static String dialog_connection_database;
    public static String dialog_connection_test_connection;
    public static String dialog_connection_tns_tab;
    public static String dialog_connection_user_name;
    public static String dialog_connection_ver;
    public static String edit_oracle_constraint_manager_dialog_title;
    public static String edit_oracle_data_type_manager_dialog_title;
    public static String edit_oracle_foreign_key_manager_dialog_title;
    public static String edit_oracle_index_manager_dialog_title;
    public static String edit_oracle_package_manager_dialog_title;
    public static String edit_oracle_schema_manager_dialog_title;
    public static String edit_oracle_trigger_manager_dialog_title;
    public static String editors_oracle_session_editor_action__session;
    public static String editors_oracle_session_editor_action_disconnect;
    public static String editors_oracle_session_editor_action_kill;
    public static String editors_oracle_session_editor_confirm_action;
    public static String editors_oracle_session_editor_confirm_title;
    public static String editors_oracle_session_editor_title_disconnect_session;
    public static String editors_oracle_session_editor_title_kill_session;
    public static String editors_oracle_source_abstract_editor_action_name;
    public static String editors_oracle_source_abstract_editor_state;
    public static String tools_script_execute_wizard_error_sqlplus_not_found;
    public static String tools_script_execute_wizard_page_name;
    public static String tools_script_execute_wizard_page_settings_button_browse;
    public static String tools_script_execute_wizard_page_settings_group_input;
    public static String tools_script_execute_wizard_page_settings_label_input_file;
    public static String tools_script_execute_wizard_page_settings_page_description;
    public static String tools_script_execute_wizard_page_settings_page_name;
    public static String views_oracle_compiler_dialog_button_compile;
    public static String views_oracle_compiler_dialog_button_compile_all;
    public static String views_oracle_compiler_dialog_column_name;
    public static String views_oracle_compiler_dialog_column_type;
    public static String views_oracle_compiler_dialog_message_compilation_error;
    public static String views_oracle_compiler_dialog_message_compilation_success;
    public static String views_oracle_compiler_dialog_message_compile_unit;
    public static String views_oracle_compiler_dialog_title;
    public static String views_oracle_compiler_log_viewer_action_clear_log;
    public static String views_oracle_compiler_log_viewer_action_copy;
    public static String views_oracle_compiler_log_viewer_action_select_all;
    public static String views_oracle_compiler_log_viewer_column_line;
    public static String views_oracle_compiler_log_viewer_column_message;
    public static String views_oracle_compiler_log_viewer_column_pos;

    public static String dialog_connection_oracle_properties;
    public static String dialog_connection_oracle_properties_description;
    public static String dialog_controlgroup_session_settings;
    public static String edit_label_combo_language;
    public static String edit_label_combo_language_tool_tip_text;
    public static String edit_label_combo_territory;
    public static String edit_label_combo_territory_tool_tip_text;
    public static String edit_label_text_date_format;
    public static String edit_label_text_timestamp_format;
    public static String edit_label_text_length_format;
    public static String edit_label_text_currency_format;
    public static String dialog_controlgroup_content;
    public static String edit_create_checkbox_hide_empty_schemas;
    public static String edit_create_checkbox_hide_empty_schemas_tool_tip_text;
    public static String edit_create_checkbox_content_group_show;
    public static String edit_create_checkbox_content_group_show_description;
    public static String edit_create_checkbox_content_group_use;
    public static String edit_create_checkbox_content_group_use_description;
    public static String edit_create_checkbox_content_group_use_sys_schema;
    public static String edit_create_checkbox_content_group_use_simple_constraints;
    public static String edit_create_checkbox_content_group_use_sys_schema_description;
    public static String edit_create_checkbox_content_group_use_simple_constraints_description;
    public static String edit_create_checkbox_content_group_use_another_table_query;
    public static String edit_create_checkbox_content_group_use_another_table_query_description;
    public static String edit_create_checkbox_content_group_search_metadata_in_synonyms;
    public static String edit_create_checkbox_content_group_search_metadata_in_synonyms_tooltip;
    public static String dialog_controlgroup_performance;
    public static String edit_create_checkbox_group_use_rule;
    public static String edit_create_checkbox_adds_rule_tool_tip_text;
    public static String edit_create_checkbox_group_use_metadata_optimizer;
    public static String edit_create_checkbox_group_use_metadata_optimizer_tip;
    public static String pref_page_oracle_checkbox_disable_escape_processing;
    public static String pref_page_oracle_checkbox_enable_dbms_output;
    public static String pref_page_oracle_checkbox_use_rowid_to_identify_rows;
    public static String pref_page_oracle_checkbox_read_all_synonyms;
    public static String pref_page_oracle_label_by_default_plan_table;
    public static String pref_page_oracle_label_disable_client_side_parser;
    public static String pref_page_oracle_label_if_unchecked_java_classes;
    public static String pref_page_oracle_label_plan_table;
    public static String pref_page_oracle_legend_execution_plan;
    public static String pref_page_oracle_legend_misc;
    public static String pref_page_oracle_legend_performance;
}
