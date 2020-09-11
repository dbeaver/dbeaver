/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017 Liu, Yuanyuan (liuyuanyuan@highgo.com)
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
package org.jkiss.dbeaver.ext.postgresql;

import org.eclipse.osgi.util.NLS;

public class PostgreMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.postgresql.PostgresResources"; //$NON-NLS-1$

    /*backup wizard*/
    public static String wizard_backup_title;
    public static String wizard_backup_msgbox_success_title;
    public static String wizard_backup_msgbox_success_description;
    public static String wizard_backup_page_object_title_schema_table;
    public static String wizard_backup_page_object_title;
    public static String wizard_backup_page_object_description;
    public static String wizard_backup_page_object_group_object;
    public static String wizard_backup_page_object_checkbox_show_view;
    public static String wizard_backup_page_setting_title_setting;
    public static String wizard_backup_page_setting_title;
    public static String wizard_backup_page_setting_description;
    public static String wizard_backup_page_setting_group_setting;
    public static String wizard_backup_page_setting_label_format;
    public static String wizard_backup_page_setting_label_compression;
    public static String wizard_backup_page_setting_label_encoding;
    public static String wizard_backup_page_setting_checkbox_use_insert;
    public static String wizard_backup_page_setting_checkbox_no_privileges;
    public static String wizard_backup_page_setting_checkbox_no_owner;
    public static String wizard_backup_page_setting_group_output;
    public static String wizard_backup_page_setting_label_output_folder;
    public static String wizard_backup_page_setting_label_file_name_pattern;
    public static String wizard_backup_page_setting_label_file_name_pattern_output;
    public static String wizard_backup_page_setting_group_security;
    public static String wizard_backup_page_setting_group_security_label_info;
    public static String wizard_backup_page_setting_group_security_btn_authentication;
    public static String wizard_backup_page_setting_group_security_btn_reset_default;

    /* wizard restore*/
    public static String wizard_restore_title;
    public static String wizard_restore_page_setting_btn_clean_first;
    public static String wizard_restore_page_setting_description;
    public static String wizard_restore_page_setting_label_backup_file;
    public static String wizard_restore_page_setting_label_choose_backup_file;
    public static String wizard_restore_page_setting_label_format;
    public static String wizard_restore_page_setting_label_input;
    public static String wizard_restore_page_setting_label_setting;
    public static String wizard_restore_page_setting_title;
    public static String wizard_restore_page_setting_title_setting;

    /* tool script */
    public static String wizard_script_title_import_db;
    public static String wizard_script_title_execute_script;
    public static String tool_script_title_execute;
    public static String tool_script_title_import;
    public static String tool_script_description_execute;
    public static String tool_script_description_import;
    public static String tool_script_label_input;
    public static String tool_script_label_input_file;

    /* dialog create db */
    public static String dialog_create_db_group_definition;
    public static String dialog_create_db_group_general;
    public static String dialog_create_db_label_db_name;
    public static String dialog_create_db_label_encoding;
    public static String dialog_create_db_label_owner;
    public static String dialog_create_db_label_tablesapce;
    public static String dialog_create_db_label_template_db;
    public static String dialog_create_db_title;
    public static String dialog_create_db_tablespace_default;

    /* dialog create schema */
    public static String dialog_create_schema_name;
    public static String dialog_create_schema_owner;
    public static String dialog_create_schema_title;

    /* dialog create extension */
    public static String dialog_create_extension_name;
    public static String dialog_create_extension_schema;
    public static String dialog_create_extension_title;
    public static String dialog_create_extension_column_name;
    public static String dialog_create_extension_column_version;
    public static String dialog_create_extension_column_description;
    public static String dialog_create_extension_database;

    /* PostgresSSLConfigurator */
    public static String dialog_connection_network_postgres_ssl_certificates;
    public static String dialog_connection_network_postgres_ssl_certificates_root;
    public static String dialog_connection_network_postgres_ssl_certificates_ca;
    public static String dialog_connection_network_postgres_ssl_certificates_ssl;
    public static String dialog_connection_network_postgres_ssl_certificates_ssl_key;
    public static String dialog_connection_network_postgres_ssl_advanced;
    public static String dialog_connection_network_postgres_ssl_advanced_ssl_mode;
    public static String dialog_connection_network_postgres_ssl_advanced_ssl_factory;
    public static String dialog_connection_network_postgres_ssl_advanced_use_proxy;
    public static String dialog_connection_network_postgres_ssl_advanced_use_proxy_tip;

    /* PostgreConnectionPage */
    public static String dialog_setting_connection_host;
    public static String dialog_setting_connection_port;
    public static String dialog_setting_connection_database;
    public static String dialog_setting_connection_user;
    public static String dialog_setting_connection_password;
    public static String dialog_setting_connection_settings;
    public static String dialog_setting_connection_localClient;
    public static String dialog_setting_connection_nondefaultDatabase;
    public static String dialog_setting_connection_nondefaultDatabase_tip;
    public static String dialog_setting_connection_show_templates;
    public static String dialog_setting_connection_show_templates_tip;
    public static String dialog_setting_connection_show_not_available_for_conn;
    public static String dialog_setting_connection_show_not_available_for_conn_tip;
    public static String dialog_setting_group_sql;
    public static String dialog_setting_sql_dd_plain_label;
    public static String dialog_setting_sql_dd_plain_tip;
    public static String dialog_setting_sql_dd_tag_label;
    public static String dialog_setting_sql_dd_tag_tip;
    public static String dialog_setting_sql_dd_string;
    public static String dialog_setting_sql_dd_code_block;

    /* PostgreCreateRoleDialog */
    public static String dialog_create_role_title;
    public static String dialog_create_role_group_general;
    public static String dialog_create_role_label_role_name;
    public static String dialog_create_role_label_user_password;
    public static String dialog_create_role_label_user_role;

    /* Permissions */
    public static String edit_command_grant_privilege_action_grant_privilege;
    public static String edit_command_grant_privilege_action_revoke_privilege;
    public static String edit_constraint_page_add_constraint;

    public static String dialog_create_push_button_grant_all;
    public static String dialog_create_push_button_revoke_all;
    public static String dialog_object_description_text_no_objects;
    public static String dialog_create_table_column_name_permission;
    public static String dialog_create_table_column_name_with_garant;
    public static String dialog_create_table_column_name_with_hierarchy;
    public static String postgre_foreign_key_manager_checkbox_deferrable;
    public static String postgre_foreign_key_manager_checkbox_deferred;
    public static String postgre_foreign_key_manager_header_edit_foreign_key;

    /* Tablespaces */
    public static String dialog_create_tablespace_title;
    public static String dialog_create_tablespace_database;
    public static String dialog_create_tablespace_name;
    public static String dialog_create_tablespace_owner;
    public static String dialog_create_tablespace_loc;
    public static String dialog_create_tablespace_options;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, PostgreMessages.class);
    }

    private PostgreMessages() {
    }

}
