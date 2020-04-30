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

package org.jkiss.dbeaver.ext.db2.ui.internal;

import org.eclipse.osgi.util.NLS;

public class DB2Messages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.db2.ui.internal.DB2Resources"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, DB2Messages.class);
    }

    private DB2Messages()
    {
        // Pure Utility Class
    }

    public static String db2_connection_page_tab_database;
	public static String db2_connection_page_tab_security;
	public static String db2_connection_trace_page_checkbox_append;
	public static String db2_connection_trace_page_checkbox_connect;
	public static String db2_connection_trace_page_checkbox_connection_calls;
	public static String db2_connection_trace_page_checkbox_diagnostics;
	public static String db2_connection_trace_page_checkbox_drda_flows;
	public static String db2_connection_trace_page_checkbox_driver_configuration;
	public static String db2_connection_trace_page_checkbox_enable_trace;
	public static String db2_connection_trace_page_checkbox_parameter_metadata;
	public static String db2_connection_trace_page_checkbox_result_set_calls;
	public static String db2_connection_trace_page_checkbox_result_set_metadata;
	public static String db2_connection_trace_page_checkbox_sql_j;
	public static String db2_connection_trace_page_checkbox_statement_calls;
	public static String db2_connection_trace_page_checkbox_xa_calls;
	public static String db2_connection_trace_page_header_levels;
	public static String db2_connection_trace_page_label_file_name;
	public static String db2_connection_trace_page_label_folder;
	public static String db2_connection_trace_page_string_trace;
	public static String db2_connection_trace_page_tab_description_trace_settings;
	public static String db2_connection_trace_page_tab_trace_settings;

    public static String dialog_connection_host;
    public static String dialog_connection_password;
    public static String dialog_connection_port;
    public static String dialog_connection_database;
    public static String dialog_connection_user_name;

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

    public static String dialog_table_tools_truncate_title;
    public static String dialog_table_tools_truncate_storage_title;
    public static String dialog_table_tools_truncate_storage_reuse;
    public static String dialog_table_tools_truncate_storage_drop;
    public static String dialog_table_tools_truncate_triggers_title;
    public static String dialog_table_tools_truncate_triggers_ignore;
    public static String dialog_table_tools_truncate_triggers_restrict;

    public static String dialog_tools_msg_title;
    public static String dialog_tools_msg_code;
    public static String dialog_tools_mes_error_code_title;
    public static String dialog_tools_mes_error_code;
    public static String dialog_tools_mes_message;
    
    public static String edit_db2_constraint_manager_dialog_title;
    public static String edit_db2_foreign_key_manager_dialog_title;
    public static String edit_db2_index_manager_dialog_title;
    
    public static String editors_db2_application_editor_title_force_application;
    public static String editors_db2_application_editor_action_force;
    public static String editors_db2_application_editor_confirm_action;

    public static String dialog_explain_choose_tablespace;
    public static String dialog_explain_choose_tablespace_tablespace;
    public static String dialog_explain_no_tables;
    public static String dialog_explain_no_tablespace_found_title;
    public static String dialog_explain_ask_to_create;

}
