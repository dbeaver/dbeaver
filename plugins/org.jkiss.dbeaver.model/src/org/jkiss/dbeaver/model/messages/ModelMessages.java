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
package org.jkiss.dbeaver.model.messages;

import org.eclipse.osgi.util.NLS;

public class ModelMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.model.messages.ModelResources"; //$NON-NLS-1$

	public static String error_not_connected_to_database;

	public static String error_can_create_temp_dir;
	public static String error_can_create_temp_file;

	public static String common_error_sql;

	public static String model_constraint_type_foreign_key;
	public static String model_constraint_type_primary_key;
	public static String model_constraint_type_unique_key;

	public static String model_navigator__connections;
	public static String model_navigator_Connection;

	public static String model_connection_events_event_after_connect;
	public static String model_connection_events_event_after_disconnect;
	public static String model_connection_events_event_before_connect;
	public static String model_connection_events_event_before_disconnect;

	public static String model_edit_execute_;
	public static String model_jdbc_read_database_meta_data;

	public static String model_jdbc__rows_fetched;
	public static String model_jdbc_array_result_set;
	public static String model_jdbc_create_new_constraint;
	public static String model_jdbc_create_new_foreign_key;
	public static String model_jdbc_create_new_index;
	public static String model_jdbc_create_new_object;
	public static String model_jdbc_create_new_table;
	public static String model_jdbc_create_new_table_column;
	public static String model_jdbc_Database;
	public static String model_jdbc_delete_object;
	public static String model_jdbc_driver_properties;
	public static String model_jdbc_drop_constraint;
	public static String model_jdbc_drop_foreign_key;
	public static String model_jdbc_drop_index;
	public static String model_jdbc_drop_table;
	public static String model_jdbc_drop_table_column;
	public static String model_jdbc_exception_bad_savepoint_object;
	public static String model_jdbc_exception_could_not_bind_statement_parameter;
	public static String model_jdbc_exception_could_not_close_connection;
	public static String model_jdbc_exception_could_not_get_result_set_value;
	public static String model_jdbc_exception_internal_jdbc_driver_error;
	public static String model_jdbc_exception_invalid_transaction_isolation_parameter;
	public static String model_jdbc_exception_unsupported_array_type_;
	public static String model_jdbc_exception_unsupported_value_type_;
	public static String model_jdbc_fetch_table_data;
	public static String model_jdbc_fetch_table_row_count;
	public static String model_jdbc_find_best_row_identifier;
	public static String model_jdbc_find_objects_by_name;
	public static String model_jdbc_find_version_columns;
	public static String model_jdbc_jdbc_error;
	public static String model_jdbc_load_catalogs;
	public static String model_jdbc_load_client_info;
	public static String model_jdbc_load_column_privileges;
	public static String model_jdbc_load_columns;
	public static String model_jdbc_load_cross_reference;
	public static String model_jdbc_load_exported_keys;
	public static String model_jdbc_load_from_file_;
	public static String model_jdbc_load_function_columns;
	public static String model_jdbc_load_functions;
	public static String model_jdbc_load_imported_keys;
	public static String model_jdbc_load_indexes;
	public static String model_jdbc_load_primary_keys;
	public static String model_jdbc_load_procedure_columns;
	public static String model_jdbc_load_procedures;
	public static String model_jdbc_load_schemas;
	public static String model_jdbc_load_super_tables;
	public static String model_jdbc_load_super_types;
	public static String model_jdbc_load_table_privileges;
	public static String model_jdbc_load_table_types;
	public static String model_jdbc_load_tables;
	public static String model_jdbc_load_type_info;
	public static String model_jdbc_load_udt_attributes;
	public static String model_jdbc_load_udts;
	public static String model_jdbc_lob_and_binary_data_cant_be_edited_inline;
	public static String model_jdbc_max_length;
	public static String model_jdbc_None;
	public static String model_jdbc_precision;
	public static String model_jdbc_Procedure;
	public static String model_jdbc_read_committed;
	public static String model_jdbc_read_uncommitted;
	public static String model_jdbc_rename_object;
	public static String model_jdbc_reorder_object;
	public static String model_jdbc_repeatable_read;
	public static String model_jdbc_save_to_file_;
	public static String model_jdbc_scale;
	public static String model_jdbc_Schema;
	public static String model_jdbc_Serializable;
	public static String model_jdbc_type_name;
	public static String model_jdbc_unknown;
	public static String model_jdbc_unsupported_column_type_;
	public static String model_jdbc_unsupported_content_value_type_;
	public static String model_jdbc_unsupported_value_type_;

	public static String model_navigator_Description;
	public static String model_navigator_load_;
	public static String model_navigator_load_items_;
	public static String model_navigator_Model_root;
	public static String model_navigator_Name;
	public static String model_navigator_Project;

	public static String model_navigator_Root;

	public static String model_struct_Association;
    public static String model_struct_Cascade;
	public static String model_struct_Check;
	public static String model_struct_Clustered;
    public static String model_struct_Foreign_Key;
	public static String model_struct_Hashed;
    public static String model_struct_Index;
    public static String model_struct_Inheritance;
	public static String model_struct_No_Action;
	public static String model_struct_Not_NULL;
	public static String model_struct_Other;
	public static String model_struct_Primary_Key;
	public static String model_struct_Restrict;
	public static String model_struct_Set_Default;
	public static String model_struct_Set_NULL;
	public static String model_struct_Statistic;
	public static String model_struct_Unique_Key;
    public static String model_struct_Virtual_Key;
	public static String model_struct_Virtual_Foreign_Key;
    public static String model_struct_Pseudo_Key;
	public static String model_struct_Unknown;

	public static String CreateLinkedFileRunnable_e_cancelled_link;
	public static String CreateLinkedFileRunnable_e_unable_to_link;
	public static String CreateLinkedFolderRunnable_e_cancelled_link;
	public static String CreateLinkedFolderRunnable_e_unable_to_link;

	public static String dbp_connection_type_table_development;

	public static String dbp_connection_type_table_production;

	public static String dbp_connection_type_table_production_database;

	public static String dbp_connection_type_table_regular_development_database;

	public static String dbp_connection_type_table_test;

	public static String dbp_connection_type_table_test_database;

    public static String dialog_connection_wizard_start_connection_monitor_close;
    public static String dialog_connection_wizard_start_connection_monitor_start;
    public static String dialog_connection_wizard_start_connection_monitor_subtask_test;
    public static String dialog_connection_wizard_start_connection_monitor_success;
    public static String dialog_connection_wizard_start_connection_monitor_connected;
    public static String dialog_connection_wizard_start_connection_monitor_thread;
    public static String dialog_connection_wizard_start_dialog_error_message;

    static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ModelMessages.class);
	}

	private ModelMessages() {
	}
}
