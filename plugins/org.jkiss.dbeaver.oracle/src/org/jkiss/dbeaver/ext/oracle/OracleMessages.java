/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
	public static String dialog_connection_test_connection;
	public static String dialog_connection_tns_tab;
	public static String dialog_connection_user_name;
	public static String dialog_connection_ver;
	public static String edit_oracle_constraint_manager_dialog_title;
	public static String edit_oracle_data_type_manager_action_create_type_body;
	public static String edit_oracle_data_type_manager_action_create_type_header;
	public static String edit_oracle_data_type_manager_action_drop_data_type;
	public static String edit_oracle_data_type_manager_dialog_title;
	public static String edit_oracle_foreign_key_manager_dialog_title;
	public static String edit_oracle_index_manager_dialog_title;
	public static String edit_oracle_materialized_view_manager_action_create_mater_view;
	public static String edit_oracle_materialized_view_manager_action_drop_mater_view;
	public static String edit_oracle_materialized_view_manager_action_drop_view;
	public static String edit_oracle_package_manager_action_create_package_body;
	public static String edit_oracle_package_manager_action_create_package_header;
	public static String edit_oracle_package_manager_action_drop_package;
	public static String edit_oracle_package_manager_dialog_title;
	public static String edit_oracle_procedure_manager_action_create_procedure;
	public static String edit_oracle_procedure_manager_action_drop_procedure;
	public static String edit_oracle_schema_manager_action_create_schema;
	public static String edit_oracle_schema_manager_action_drop_schema;
	public static String edit_oracle_schema_manager_dialog_title;
	public static String edit_oracle_table_column_manager_action_alter_table_column;
	public static String edit_oracle_table_manager_action_rename_table;
	public static String edit_oracle_trigger_manager_action_create_trigger;
	public static String edit_oracle_trigger_manager_action_drop_trigger;
	public static String edit_oracle_trigger_manager_dialog_title;
	public static String edit_oracle_view_manager_action_create_view;
	public static String edit_oracle_view_manager_action_drop_view;
	public static String editors_oracle_session_editor_action__session;
	public static String editors_oracle_session_editor_action_disconnect;
	public static String editors_oracle_session_editor_action_kill;
	public static String editors_oracle_session_editor_confirm_action;
	public static String editors_oracle_session_editor_confirm_title;
	public static String editors_oracle_session_editor_title_disconnect_session;
	public static String editors_oracle_session_editor_title_kill_session;
	public static String editors_oracle_source_abstract_editor_action_name;
	public static String editors_oracle_source_abstract_editor_state;
}
