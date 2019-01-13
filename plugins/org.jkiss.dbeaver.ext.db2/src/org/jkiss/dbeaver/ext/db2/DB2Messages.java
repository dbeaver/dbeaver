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

package org.jkiss.dbeaver.ext.db2;

import org.eclipse.osgi.util.NLS;

public class DB2Messages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.db2.DB2Resources"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DB2Messages.class);
    }

    private DB2Messages()
    {
        // Pure Utility Class
    }

    public static String dialog_connection_browse_button;
    public static String dialog_connection_database_schema_label;
    public static String dialog_connection_db_file_chooser_text;
    public static String dialog_connection_db_folder_chooser_message;
    public static String dialog_connection_db_folder_chooser_text;
    public static String dialog_connection_edit_driver_button;
    public static String dialog_connection_host_label;
    public static String dialog_connection_jdbc_url_;
    public static String dialog_connection_password_label;
    public static String dialog_connection_path_label;
    public static String dialog_connection_port_label;
    public static String dialog_connection_server_label;
    public static String dialog_connection_test_connection_button;
    public static String dialog_connection_user_name_label;

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
    public static String edit_db2_constraint_manager_dialog_title;
    public static String edit_db2_data_type_manager_dialog_title;
    public static String edit_db2_foreign_key_manager_dialog_title;
    public static String edit_db2_index_manager_dialog_title;
    public static String edit_db2_package_manager_dialog_title;
    public static String edit_db2_schema_manager_dialog_title;
    public static String edit_db2_trigger_manager_dialog_title;
    public static String edit_db2_sequence_manager_dialog_title;

    public static String dialog_explain_choose_tablespace;
    public static String dialog_explain_choose_tablespace_tablespace;
    public static String dialog_explain_no_tables;
    public static String dialog_explain_no_tablespace_found;
    public static String dialog_explain_no_tablespace_found_title;
    public static String dialog_explain_no_tables_found_ex;
    public static String dialog_explain_ask_to_create;

    public static String editors_db2_application_editor_title_force_application;
    public static String editors_db2_application_editor_action_force;
    public static String editors_db2_application_editor_confirm_action;

    public static String dialog_table_tools_success_title;
    public static String dialog_table_tools_options;
    public static String dialog_table_tools_result;
    public static String dialog_table_tools_progress;

    public static String dialog_table_tools_runstats_title;
    public static String dialog_table_tools_runstats_cols_title;
    public static String dialog_table_tools_runstats_cols_all;
    public static String dialog_table_tools_runstats_cols_all_and_distribution;
    public static String dialog_table_tools_runstats_cols_no;
    public static String dialog_table_tools_runstats_indexes_title;
    public static String dialog_table_tools_runstats_indexes_detailed;
    public static String dialog_table_tools_runstats_indexes_all;
    public static String dialog_table_tools_runstats_indexes_no;
    public static String dialog_table_tools_runstats_stats_title;

    public static String dialog_table_tools_reorg_title;
    public static String dialog_table_tools_reorg_inplace;
    public static String dialog_table_tools_reorg_useindex;
    public static String dialog_table_tools_reorg_useindexscan;
    public static String dialog_table_tools_reorg_truncate;
    public static String dialog_table_tools_reorg_usetempts;
    public static String dialog_table_tools_reorg_reorglobs;
    public static String dialog_table_tools_reorg_reorglobsts;
    public static String dialog_table_tools_reorg_resetdict;
    public static String dialog_table_tools_reorg_access_title;
    public static String dialog_table_tools_reorg_access_no;
    public static String dialog_table_tools_reorg_access_read;
    public static String dialog_table_tools_reorg_access_readwrite;

    public static String dialog_table_tools_reorgix_title;
    public static String dialog_table_tools_reorgix_access_title;
    public static String dialog_table_tools_reorgix_access_default;
    public static String dialog_table_tools_reorgix_access_no;
    public static String dialog_table_tools_reorgix_access_read;
    public static String dialog_table_tools_reorgix_access_readwrite;
    public static String dialog_table_tools_reorgix_options_title;
    public static String dialog_table_tools_reorgix_options_full;
    public static String dialog_table_tools_reorgix_options_cleanup_keys;
    public static String dialog_table_tools_reorgix_options_cleanup_pages;

    public static String dialog_table_tools_reorgcheck_title;

    public static String dialog_table_tools_reorgcheckix_title;

    public static String dialog_table_tools_truncate_title;
    public static String dialog_table_tools_truncate_storage_title;
    public static String dialog_table_tools_truncate_storage_reuse;
    public static String dialog_table_tools_truncate_storage_drop;
    public static String dialog_table_tools_truncate_triggers_title;
    public static String dialog_table_tools_truncate_triggers_ignore;
    public static String dialog_table_tools_truncate_triggers_restrict;

    public static String no_ddl_for_system_tables;
    public static String no_ddl_for_nicknames;
    public static String no_ddl_for_nonsql_routines;
    public static String no_ddl_for_spaces_in_name;

    public static String dialog_tools_msg_title;
    public static String dialog_tools_msg_code;
    public static String dialog_tools_mes_error_code_title;
    public static String dialog_tools_mes_error_code;
    public static String dialog_tools_mes_message;

}
