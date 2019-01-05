/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol;

import org.eclipse.osgi.util.NLS;

public class ExasolMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.exasol.ExasolResources"; //$NON-NLS-1$
	public static String dialog_schema_drop_title;
	public static String dialog_schema_drop_message;
	public static String dialog_connection_alter_message;
	public static String dialog_connection_alter_title;

	public static String dialog_table_tools_options;
	public static String dialog_table_tools_result;

	public static String dialog_general_continue;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ExasolMessages.class);
	}

	private ExasolMessages() {
	}

	public static String editors_exasol_session_editor_title_kill_session;
	public static String editors_exasol_session_editor_action_kill;
	public static String editors_exasol_session_editor_confirm_action;
	public static String editors_exasol_session_editor_title_kill_session_statement;

	public static String dialog_connection_password;
	public static String dialog_connection_port;
	public static String dialog_connection_user_name;

	public static String dialog_table_tools_progress;
	public static String dialog_table_tools_success_title;
	public static String dialog_table_open_output_directory;
	public static String dialog_table_tools_export_title;
	public static String dialog_table_tools_export_compress;
	public static String dialog_table_tools_column_heading;
	public static String dialog_table_tools_file_template;
	public static String dialog_table_tools_string_sep_mode;
	public static String dialog_table_tools_string_sep;
	public static String dialog_table_tools_column_sep;
	public static String dialog_table_tools_row_sep;
	public static String dialog_table_tools_encoding;
	public static String dialog_table_tools_import_title;
	public static String dialog_struct_edit_fk_label_fk_name;
	public static String edit_exasol_constraint_manager_dialog_title;

	public static String read_table_details;
	public static String read_schema_details;
	public static String dialog_create_priority_group;
	public static String dialog_priority_group_description;
	public static String dialog_priority_group_name;
	public static String dialog_priority_group_weight;
	public static String manager_priority_group_comment;
	public static String manager_priority_create;
	public static String manager_priority_drop;
	public static String manager_priority_alter;
	public static String manager_priority_rename;
	public static String manager_schema_owner;
	public static String manager_schema_raw_limit;
	public static String manager_schema_create;
	public static String dialog_create_user_userid;
	public static String dialog_create_user_comment;
	public static String dialog_create_user_kerberos;
	public static String dialog_create_user_ldap;
	public static String dialog_create_user_local;
	public static String dialog_create_user_local_password;
	public static String dialog_create_user_kerberos_principal;
	public static String dialog_create_user_ldap_dn;
	public static String manager_assign_priority_group;
	public static String exasol_security_policy_name;
	public static String exasol_security_policy_description;
	

}
