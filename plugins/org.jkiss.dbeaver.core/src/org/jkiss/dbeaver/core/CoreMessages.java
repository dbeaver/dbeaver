/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.osgi.util.NLS;

public class CoreMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.core.CoreResources"; //$NON-NLS-1$

	public static String actions_menu_about;
	public static String actions_menu_database;
	public static String actions_menu_edit;
	public static String actions_menu_exit_emergency;
	public static String actions_menu_file;
	public static String actions_menu_help;
	public static String actions_menu_navigate;
	public static String actions_menu_window;
	public static String DBeaverCore_error_can_create_temp_dir;
	public static String DBeaverCore_error_can_create_temp_file;

	public static String actions_ContentAssistProposal_label;
	public static String actions_ContentAssistProposal_tooltip;
	public static String actions_ContentAssistProposal_description;

	public static String actions_ContentAssistTip_label;
	public static String actions_ContentAssistTip_tooltip;
	public static String actions_ContentAssistTip_description;

	public static String actions_ContentFormatProposal_label;
	public static String actions_ContentFormatProposal_tooltip;
	public static String actions_ContentFormatProposal_description;

	public static String common_error_sql;

	public static String confirm_exit_title;
	public static String confirm_exit_message;
	public static String confirm_exit_toggleMessage;

	public static String confirm_order_resultset_title;
	public static String confirm_order_resultset_message;
	public static String confirm_order_resultset_toggleMessage;

	public static String confirm_close_resultset_edit_title;
	public static String confirm_close_resultset_edit_message;
	public static String confirm_close_resultset_edit_toggleMessage;

	public static String confirm_disconnect_txn_title;
	public static String confirm_disconnect_txn_message;
	public static String confirm_disconnect_txn_toggleMessage;

	public static String confirm_close_entity_edit_title;
	public static String confirm_close_entity_edit_message;
	public static String confirm_close_entity_edit_toggleMessage;

	public static String confirm_entity_delete_title;
	public static String confirm_entity_delete_message;

	public static String confirm_close_editor_edit_title;
	public static String confirm_close_editor_edit_message;
	public static String confirm_close_editor_edit_toggleMessage;

	public static String confirm_driver_download_title;
	public static String confirm_driver_download_message;
	public static String confirm_driver_download_toggleMessage;

	public static String confirm_entity_reject_title;
	public static String confirm_entity_reject_message;
	public static String confirm_entity_reject_toggleMessage;

	public static String confirm_entity_revert_title;
	public static String confirm_entity_revert_message;
	public static String confirm_entity_revert_toggleMessage;

	public static String core_model__connections;

	public static String core_model_Connection;

	public static String core_model_Connections;

	public static String dialog_about_font;

	public static String dialog_about_label_version;

	public static String dialog_about_title;

	public static String model_edit_execute_;

	public static String model_jdbc_array_result_set;

	public static String model_jdbc_column_size;

	public static String model_jdbc_exception_bad_savepoint_object;

	public static String model_jdbc_exception_could_not_bind_statement_parameter;

	public static String model_jdbc_exception_could_not_close_connection;

	public static String model_jdbc_exception_could_not_get_result_set_value;

	public static String model_jdbc_exception_internal_jdbc_driver_error;

	public static String model_jdbc_exception_invalid_transaction_isolation_parameter;

	public static String model_jdbc_exception_unsupported_array_type_;

	public static String model_jdbc_exception_unsupported_value_type_;

	public static String model_jdbc_find_best_row_identifier;

	public static String model_jdbc_find_version_columns;

	public static String model_jdbc_load_catalogs;

	public static String model_jdbc_load_client_info;

	public static String model_jdbc_load_column_privileges;

	public static String model_jdbc_load_columns;

	public static String model_jdbc_load_cross_reference;

	public static String model_jdbc_load_exported_keys;

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

	public static String model_jdbc_unknown;

	public static String model_project_bookmarks_folder;

	public static String toolbar_datasource_selector_action_read_databases;
	public static String toolbar_datasource_selector_combo_database_tooltip;
	public static String toolbar_datasource_selector_combo_datasource_tooltip;
	public static String toolbar_datasource_selector_empty;
	public static String toolbar_datasource_selector_error_change_database_message;
	public static String toolbar_datasource_selector_error_change_database_title;
	public static String toolbar_datasource_selector_error_database_not_found;
	public static String toolbar_datasource_selector_error_database_change_not_supported;
	public static String toolbar_datasource_selector_resultset_segment_size;

	public static String ui_common_button_help;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreMessages.class);
	}

	private CoreMessages() {
	}
}
