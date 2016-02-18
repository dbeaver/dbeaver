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
package org.jkiss.dbeaver.model.messages;

import org.eclipse.osgi.util.NLS;

public class ModelMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.model.messages.ModelResources"; //$NON-NLS-1$

	public static String DBeaverCore_error_can_create_temp_dir;
	public static String DBeaverCore_error_can_create_temp_file;

	public static String common_error_sql;

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
	public static String model_jdbc_bad_content_value_;
	public static String model_jdbc_cant_create_null_cursor;
	public static String model_jdbc_column_size;
	public static String model_jdbc_content_length;
	public static String model_jdbc_content_type;
	public static String model_jdbc_could_not_load_content;
	public static String model_jdbc_could_not_load_content_from_file;
	public static String model_jdbc_could_not_save_content;
	public static String model_jdbc_could_not_save_content_to_file_;
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
	public static String model_jdbc_repeatable_read;
	public static String model_jdbc_save_to_file_;
	public static String model_jdbc_scale;
	public static String model_jdbc_Schema;
	public static String model_jdbc_Serializable;
	public static String model_jdbc_set_to_current_time;
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

	public static String model_struct_Cascade;
	public static String model_struct_Check;
	public static String model_struct_Clustered;
	public static String model_struct_Foreign_Key;
	public static String model_struct_Hashed;
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
    public static String model_struct_Pseudo_Key;
	public static String model_struct_Unknown;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ModelMessages.class);
	}

	private ModelMessages() {
	}
}
