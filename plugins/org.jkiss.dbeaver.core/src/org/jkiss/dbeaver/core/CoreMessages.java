/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.eclipse.osgi.util.NLS;

public class CoreMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.core.CoreResources"; //$NON-NLS-1$

	public static String actions_menu_about;
    public static String actions_menu_check_update;
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

	public static String actions_navigator__objects;

	public static String actions_navigator_create_new;

	public static String actions_navigator_edit;

	public static String actions_navigator_error_dialog_open_entity_title;

	public static String actions_navigator_error_dialog_open_resource_title;

	public static String actions_navigator_open;

	public static String actions_navigator_view;

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

    public static String confirm_version_check_title;
    public static String confirm_version_check_message;
    public static String confirm_version_check_toggleMessage;

	public static String confirm_entity_reject_title;
	public static String confirm_entity_reject_message;
	public static String confirm_entity_reject_toggleMessage;

	public static String confirm_entity_revert_title;
	public static String confirm_entity_revert_message;
	public static String confirm_entity_revert_toggleMessage;

	public static String connection_properties_control_action_add_property;

	public static String connection_properties_control_action_remove_property;

	public static String connection_properties_control_category_user_properties;

	public static String connection_properties_control_dialog_new_property_title;

	public static String core_model__connections;
	public static String core_model_Connection;
	public static String core_model_Connections;

	public static String dialog_about_font;
	public static String dialog_about_label_version;
	public static String dialog_about_title;

	public static String dialog_connection_message;

	public static String dialog_connection_wizard_final_button_test;

	public static String dialog_connection_wizard_final_button_events;

	public static String dialog_connection_wizard_final_checkbox_filter_catalogs;

	public static String dialog_connection_wizard_final_checkbox_filter_schemas;

	public static String dialog_connection_wizard_final_checkbox_save_password_locally;

	public static String dialog_connection_wizard_final_checkbox_show_system_objects;

	public static String dialog_connection_wizard_final_default_new_connection_name;

	public static String dialog_connection_wizard_final_description;

	public static String dialog_connection_wizard_final_group_filters;

	public static String dialog_connection_wizard_final_group_security;

	public static String dialog_connection_wizard_final_header;

	public static String dialog_connection_wizard_final_label_connection_name;

	public static String dialog_connection_wizard_start_connection_monitor_close;

	public static String dialog_connection_wizard_start_connection_monitor_start;

	public static String dialog_connection_wizard_start_connection_monitor_subtask_test;

	public static String dialog_connection_wizard_start_connection_monitor_success;

	public static String dialog_connection_wizard_start_connection_monitor_connected;

	public static String dialog_connection_wizard_start_connection_monitor_thread;

	public static String dialog_connection_wizard_start_dialog_error_message;

	public static String dialog_connection_wizard_start_dialog_error_title;

	public static String dialog_connection_wizard_start_dialog_interrupted_message;

	public static String dialog_connection_wizard_start_dialog_interrupted_title;

	public static String model_edit_execute_;

	public static String model_html_read_database_meta_data;

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

	public static String model_project_bookmarks_folder;
	public static String model_project_cant_open_bookmark;
	public static String model_project_open_bookmark;
	public static String model_project_Script;
	public static String model_project_Scripts;

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
	public static String model_struct_Unknown;

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
