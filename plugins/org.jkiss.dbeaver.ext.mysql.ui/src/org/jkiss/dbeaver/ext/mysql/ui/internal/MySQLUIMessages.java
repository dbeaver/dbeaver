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
package org.jkiss.dbeaver.ext.mysql.ui.internal;

import org.eclipse.osgi.util.NLS;

public final class MySQLUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, MySQLUIMessages.class);
    }

    public static String dialog_connection_host;
    public static String dialog_connection_instance;
    public static String dialog_connection_database;
    public static String dialog_connection_port;
    public static String dialog_connection_group_advanced;
    public static String dialog_connection_server_timezone;
    public static String dialog_connection_auto_detect;
    public static String dialog_connection_local_client;
    public static String edit_catalog_manager_dialog_schema_name;
    public static String edit_command_change_user_action_create_new_user;
    public static String edit_command_change_user_action_update_user_record;
    public static String edit_command_change_user_name;
    public static String edit_command_grant_privilege_action_grant_privilege;
    public static String edit_command_grant_privilege_name_revoke_privilege;
    public static String edit_constraint_manager_title;
    public static String edit_foreign_key_manager_title;
    public static String edit_index_manager_title;
    public static String edit_procedure_manager_body;
    public static String edit_user_manager_command_create_user;
    public static String edit_user_manager_command_rename_user;
    public static String edit_user_manager_command_drop_user;
    public static String edit_user_manager_command_flush_privileges;
    public static String edit_view_manager_definition;
    public static String editors_session_editor_action_kill_Session;
    public static String editors_session_editor_action_terminate_Query;
    public static String editors_session_editor_confirm;
    public static String editors_session_hide_sleeping_text;
    public static String editors_session_hide_sleeping_tip;
    public static String editors_session_show_performance_text;
    public static String editors_session_show_performance_tip;
    public static String editors_user_editor_abstract_load_grants;
    public static String editors_user_editor_general_control_dba_privileges;
    public static String editors_user_editor_general_group_limits;
    public static String editors_user_editor_general_group_login;
    public static String editors_user_editor_general_label_confirm;
    public static String editors_user_editor_general_label_host;
    public static String editors_user_editor_general_label_password;
    public static String editors_user_editor_general_label_user_name;
    public static String editors_user_editor_general_service_load_catalog_privileges;
    public static String editors_user_editor_general_spinner_max_connections;
    public static String editors_user_editor_general_spinner_max_queries;
    public static String editors_user_editor_general_spinner_max_updates;
    public static String editors_user_editor_general_spinner_max_user_connections;
    public static String editors_user_editor_privileges_column_catalog;
    public static String editors_user_editor_privileges_column_table;
    public static String editors_user_editor_privileges_control_other_privileges;
    public static String editors_user_editor_privileges_control_table_privileges;
    public static String editors_user_editor_privileges_group_catalogs;
    public static String editors_user_editor_privileges_group_tables;
    public static String editors_user_editor_privileges_service_load_privileges;
    public static String editors_user_editor_privileges_service_load_tables;
    public static String mysql_ssl_configurator_checkbox_allow_public_key;
    public static String mysql_ssl_configurator_checkbox_require_ssl;
    public static String mysql_ssl_configurator_checkbox_verify_server_certificate;
    public static String mysql_ssl_configurator_checkbox_allow_public_key_tip;
    public static String mysql_ssl_configurator_label_ca_certificate;
    public static String mysql_ssl_configurator_label_cipher_suites;
    public static String mysql_ssl_configurator_label_cipher_suites_tip;
    public static String mysql_ssl_configurator_checkbox_require_ssl_tip;
    public static String mysql_ssl_configurator_checkbox_verify_server_certificate_tip;
    public static String mysql_ssl_configurator_legend_advanced;
    public static String tools_db_export_wizard_job_dump_log_reader;
    public static String tools_db_export_wizard_message_export_completed;
    public static String tools_db_export_wizard_monitor_bytes;
    public static String tools_db_export_wizard_monitor_export_db;
    public static String tools_db_export_wizard_page_objects_dialog_wizard_title;
    public static String tools_db_export_wizard_page_objects_dialog_title_choose_objects;
    public static String tools_db_export_wizard_page_objects_dialog_description;
    public static String tools_db_export_wizard_page_settings_checkbox_add_drop;
    public static String tools_db_export_wizard_page_settings_checkbox_addnl_comments;
    public static String tools_db_export_wizard_page_settings_checkbox_remove_definer;
    public static String tools_db_export_wizard_page_settings_checkbox_binary_hex;
    public static String tools_db_export_wizard_page_settings_checkbox_no_data;
    public static String tools_db_export_wizard_page_settings_checkbox_no_routines;
    public static String tools_db_export_wizard_page_settings_checkbox_disable_keys;
    public static String tools_db_export_wizard_page_settings_checkbox_dump_events;
    public static String tools_db_export_wizard_page_settings_checkbox_ext_inserts;
    public static String tools_db_export_wizard_page_settings_checkbox_no_create;
    public static String tools_db_export_wizard_page_settings_combo_item_lock_tables;
    public static String tools_db_export_wizard_page_settings_combo_item_normal;
    public static String tools_db_export_wizard_page_settings_combo_item_online_backup;
    public static String tools_db_export_wizard_page_settings_file_selector_title;
    public static String tools_db_export_wizard_page_settings_group_exe_method;
    public static String tools_db_export_wizard_page_settings_group_objects;
    public static String tools_db_export_wizard_page_settings_group_show_views;
    public static String tools_db_export_wizard_page_settings_group_output;
    public static String tools_db_export_wizard_page_settings_group_settings;
    public static String tools_db_export_wizard_page_settings_page_description;
    public static String tools_db_export_wizard_page_settings_page_name;
    public static String tools_db_export_wizard_task_name;
    public static String tools_db_export_wizard_title;
    public static String tools_script_execute_wizard_db_import;
    public static String tools_script_execute_wizard_execute_script;
    public static String tools_script_execute_wizard_page_settings_group_input;
    public static String tools_script_execute_wizard_page_settings_group_settings;
    public static String tools_script_execute_wizard_page_settings_import_configuration;
    public static String tools_script_execute_wizard_page_settings_label_input_file;
    public static String tools_script_execute_wizard_page_settings_label_log_level;
    public static String tools_script_execute_wizard_page_settings_script_configuration;
    public static String tools_script_execute_wizard_page_settings_set_db_import_settings;
    public static String tools_script_execute_wizard_page_settings_set_script_execution_settings;
    public static String tools_script_execute_wizard_page_settings_checkbox_disable_foreign_key_checks;
    public static String tools_script_execute_wizard_page_settings_checkbox_disable_foreign_key_checks_tooltip;

    public static String tools_db_export_wizard_page_settings_security_group;
    public static String tools_db_export_wizard_page_settings_security_label_info;
    public static String tools_db_export_wizard_page_settings_security_button_auth;
    public static String tools_db_export_wizard_page_settings_security_button_reset;
    public static String tools_db_export_wizard_page_settings_security_checkbox_override_host_credentials;
    public static String tools_db_export_wizard_page_settings_security_checkbox_override_host_credentials_tip;
    public static String tools_db_export_wizard_page_settings_auth_title;
    public static String tools_db_export_wizard_page_settings_auth_save_password_checkbox;
    public static String tools_db_export_wizard_page_settings_auth_save_password_checkbox_tip;

    public static String dialog_create_database_title;
    public static String dialog_create_database_database_name;
    public static String dialog_create_database_charset;
    public static String dialog_create_database_collation;

    public static String controls_privilege_table_column_privilege_name;
    public static String controls_privilege_table_column_privilege_name_tip;
    public static String controls_privilege_table_column_privilege_status;
    public static String controls_privilege_table_column_privilege_status_tip;
    public static String controls_privilege_table_column_privilege_description;
    public static String controls_privilege_table_column_privilege_description_tip;
    public static String controls_privilege_table_push_button_check_all;
    public static String controls_privilege_table_push_button_clear_all;

    private MySQLUIMessages() {
    }
}
