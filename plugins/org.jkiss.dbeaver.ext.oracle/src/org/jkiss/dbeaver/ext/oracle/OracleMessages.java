/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.oracle;

import org.eclipse.osgi.util.NLS;

public class OracleMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.oracle.OracleResources"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, OracleMessages.class);
	}

	private OracleMessages() {
	}

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
}
