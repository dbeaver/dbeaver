/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.core;

import org.eclipse.osgi.util.NLS;

public class CoreMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.core.CoreResources"; //$NON-NLS-1$
	
	public static String actions_menu_about;
	public static String actions_menu_check_update;
	public static String actions_menu_database;
	public static String actions_menu_edit;
	public static String actions_menu_edit_ContentFormat;
	public static String actions_menu_exit_emergency;
	public static String actions_menu_file;
	public static String actions_menu_help;
	public static String actions_menu_navigate;
	public static String actions_menu_window;
	public static String actions_menu_window_showView;
	public static String action_menu_showKeyAssist;
	public static String action_menu_installNewSoftware;
	public static String action_menu_installInfo;
	public static String action_menu_enterpriseEdition;
	
	public static String action_menu_sqleditor_maximizeResultsPanel;
	public static String action_menu_sqleditor_restoreResultsPanel;
	
	public static String action_menu_transaction_manualcommit_name;
	public static String action_menu_transaction_autocommit_name;
	public static String action_menu_transaction_manualcommit_description;
	public static String action_menu_transaction_autocommit_description;
	
	public static String action_menu_transactionMonitor_totalStatement;
	public static String action_menu_transactionMonitor_modifyStatement;
	public static String action_menu_transactionMonitor_autocommitMode;
	public static String action_menu_transactionMonitor_uptime;
	public static String action_menu_transactionMonitor_notConnected;
	
	public static String action_popup_sqleditor_layout_horizontal;
	public static String action_popup_sqleditor_layout_vertical;
	public static String action_popup_sqleditor_layout_detached;
	
	public static String DBeaverCore_error_can_create_temp_dir;
	public static String DBeaverCore_error_can_create_temp_file;

	public static String actions_ContentAssistProposal_label;
	public static String actions_ContentAssistProposal_tooltip;
	public static String actions_ContentAssistProposal_description;

	public static String actions_ContentAssistTip_label;
	public static String actions_ContentAssistTip_tooltip;
	public static String actions_ContentAssistTip_description;

	public static String actions_ContentAssistInfo_label;
	public static String actions_ContentAssistInfo_tooltip;
	public static String actions_ContentAssistInfo_description;

	public static String actions_ContentFormatProposal_label;
	public static String actions_ContentFormatProposal_tooltip;
	public static String actions_ContentFormatProposal_description;

	public static String actions_navigator__objects;
	public static String actions_navigator_bookmark_error_message;
	public static String actions_navigator_bookmark_error_title;
	public static String actions_navigator_bookmark_title;
	public static String actions_navigator_copy_fqn_title;
	public static String actions_navigator_copy_fqn_titles;
	public static String actions_navigator_copy_object_copy_node;
	public static String actions_navigator_copy_object_copy_objects;
	public static String actions_navigator_create_folder_error_message;
	public static String actions_navigator_create_folder_error_title;
	public static String actions_navigator_create_folder_folder_name;
	public static String actions_navigator_create_new;
	public static String actions_navigator_delete_objects;
	public static String actions_navigator_delete_;
	public static String actions_navigator_delete_script;
	public static String actions_navigator_edit;
	public static String actions_navigator_error_dialog_delete_object_title;
	public static String actions_navigator_error_dialog_open_entity_title;
	public static String actions_navigator_error_dialog_open_resource_title;
	public static String actions_navigator_open;
	public static String actions_navigator_view;
	public static String actions_navigator_view_script_button;
    public static String actions_spreadsheet_copy_special;
	
    public static String common_error_sql;
	
    public static String confirm_exit_title;
	public static String confirm_exit_message;
	public static String confirm_exit_toggleMessage;
	public static String confirm_order_resultset_title;
	public static String confirm_order_resultset_message;
	public static String confirm_order_resultset_toggleMessage;
    public static String confirm_fetch_all_rows_title;
    public static String confirm_fetch_all_rows_message;
    public static String confirm_fetch_all_rows_toggleMessage;
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
    public static String confirm_local_folder_delete_title;
    public static String confirm_local_folder_delete_message;
    public static String confirm_close_editor_edit_title;
	public static String confirm_close_editor_edit_message;
	public static String confirm_close_editor_edit_toggleMessage;
	public static String confirm_driver_download_title;
	public static String confirm_driver_download_message;
	public static String confirm_driver_download_toggleMessage;
	public static String confirm_driver_download_manual_title;
	public static String confirm_driver_download_manual_message;
	public static String confirm_driver_download_manual_toggleMessage;
    public static String confirm_version_check_title;
    public static String confirm_version_check_message;
    public static String confirm_version_check_toggleMessage;
	public static String confirm_entity_reject_title;
	public static String confirm_entity_reject_message;
	public static String confirm_entity_reject_toggleMessage;
	public static String confirm_entity_revert_title;
	public static String confirm_entity_revert_message;
	public static String confirm_entity_revert_toggleMessage;

	public static String controls_connection_properties_action_add_property;
	public static String controls_connection_properties_action_remove_property;
	public static String controls_connection_properties_category_user_properties;
	public static String controls_connection_properties_dialog_new_property_title;

    public static String controls_client_home_selector_browse;
	public static String controls_client_homes_panel_button_add_home;
	public static String controls_client_homes_panel_button_remove_home;
	public static String controls_client_homes_panel_confirm_remove_home_text;
	public static String controls_client_homes_panel_confirm_remove_home_title;
	public static String controls_client_homes_panel_dialog_title;
	public static String controls_client_homes_panel_group_information;
	public static String controls_client_homes_panel_label_id;
	public static String controls_client_homes_panel_label_name;
	public static String controls_client_homes_panel_label_path;
	public static String controls_client_homes_panel_label_product_name;
	public static String controls_client_homes_panel_label_product_version;
	public static String controls_column_info_panel_property_key;
	public static String controls_driver_tree_column_connections;
	public static String controls_driver_tree_column_name;
	public static String controls_imageview_fit_window;
	public static String controls_imageview_original_size;
	public static String controls_imageview_rotate;
	public static String controls_imageview_zoom_in;
	public static String controls_imageview_zoom_out;
	public static String controls_itemlist_action_copy;
    public static String controls_locale_selector_group_locale;
    public static String controls_locale_selector_label_country;
    public static String controls_locale_selector_label_language;
    public static String controls_locale_selector_label_locale;
    public static String controls_locale_selector_label_variant;
	public static String controls_object_list_job_props_read;
	public static String controls_object_list_message_items;
	public static String controls_object_list_message_no_items;
	public static String controls_object_list_monitor_load_lazy_props;
	public static String controls_object_list_monitor_load_props;
	public static String controls_object_list_status_objects;
	public static String controls_progress_page_action_close;
	public static String controls_progress_page_job_search;
	public static String controls_progress_page_progress_bar_cancel_tooltip;
	public static String controls_progress_page_progress_bar_loading_tooltip;
	public static String controls_progress_page_toolbar_title;

	public static String controls_querylog__ms;
	public static String controls_querylog_action_clear_log;
	public static String controls_querylog_action_copy;
	public static String controls_querylog_action_copy_all_fields;
	public static String controls_querylog_action_select_all;
	public static String controls_querylog_column_duration_name;
	public static String controls_querylog_column_duration_tooltip;
	public static String controls_querylog_column_result_name;
	public static String controls_querylog_column_result_tooltip;
	public static String controls_querylog_column_rows_name;
	public static String controls_querylog_column_rows_tooltip;
	public static String controls_querylog_column_text_name;
	public static String controls_querylog_column_text_tooltip;
	public static String controls_querylog_column_time_name;
	public static String controls_querylog_column_time_tooltip;
	public static String controls_querylog_column_type_name;
	public static String controls_querylog_column_type_tooltip;
    public static String controls_querylog_column_connection_name;
    public static String controls_querylog_column_connection_tooltip;
	public static String controls_querylog_column_context_name;
	public static String controls_querylog_column_context_tooltip;
    public static String controls_querylog_commit;
	public static String controls_querylog_connected_to;
	public static String controls_querylog_disconnected_from;
	public static String controls_querylog_error;
	public static String controls_querylog_format_minutes;
	public static String controls_querylog_job_refresh;
	public static String controls_querylog_label_result;
	public static String controls_querylog_label_text;
	public static String controls_querylog_label_time;
	public static String controls_querylog_label_type;
	public static String controls_querylog_rollback;
	public static String controls_querylog_savepoint;
	public static String controls_querylog_script;
	public static String controls_querylog_shell_text;
	public static String controls_querylog_success;
	public static String controls_querylog_transaction;

	public static String controls_resultset_filter_button_reset;
	public static String controls_resultset_filter_column_name;
	public static String controls_resultset_filter_column_criteria;
	public static String controls_resultset_filter_column_order;
	public static String controls_resultset_filter_group_columns;
	public static String controls_resultset_filter_group_custom;
	public static String controls_resultset_filter_label_orderby;
	public static String controls_resultset_filter_label_where;
	public static String controls_resultset_filter_title;
	public static String controls_resultset_filter_warning_custom_order_disabled;

	public static String controls_resultset_viewer_action_edit;
	public static String controls_resultset_viewer_action_order_filter;
    public static String controls_resultset_viewer_action_custom_filter;
	public static String controls_resultset_viewer_action_refresh;
	public static String controls_resultset_viewer_action_panels;
    public static String controls_resultset_viewer_action_options;
	public static String controls_resultset_viewer_add_new_row_context_name;
	public static String controls_resultset_viewer_dialog_status_title;
	public static String controls_resultset_check_autocommit_state;
	public static String controls_resultset_viewer_job_update;
	public static String controls_resultset_viewer_monitor_aply_changes;

	public static String controls_time_ms;

	public static String controls_resultset_viewer_status_inserted_;
    public static String controls_resultset_viewer_status_empty;
	public static String controls_resultset_viewer_status_no_data;
	public static String controls_resultset_viewer_status_row;
	public static String controls_resultset_viewer_status_rows;
	public static String controls_resultset_viewer_status_rows_fetched;
	public static String controls_resultset_viewer_status_rows_size;
	public static String controls_resultset_viewer_value;
	public static String controls_rs_pump_job_context_name;
	public static String controls_rs_pump_job_name;

	public static String model_navigator__connections;
	public static String model_navigator_Connection;
	public static String model_navigator_Connections;

	public static String dialog_about_font;
	public static String dialog_about_label_version;
	public static String dialog_about_title;

	public static String dialog_connection_auth_checkbox_save_password;
	public static String dialog_connection_auth_group_user_cridentials;
	public static String dialog_connection_auth_label_password;
	public static String dialog_connection_auth_label_username;
	public static String dialog_connection_auth_title;
	public static String dialog_connection_auth_title_for_handler;
	public static String dialog_connection_button_test;
	public static String dialog_connection_events_checkbox_show_process;
	public static String dialog_connection_events_checkbox_terminate_at_disconnect;
	public static String dialog_connection_events_checkbox_wait_finish;
	public static String dialog_connection_events_checkbox_wait_finish_timeout;
	
	public static String dialog_connection_events_label_command;
	public static String dialog_connection_events_label_event;
	public static String dialog_connection_events_title;
	public static String dialog_connection_message;
	public static String dialog_connection_description;
	public static String dialog_connection_wizard_final_button_test;
	public static String dialog_connection_wizard_final_button_events;
	public static String dialog_connection_wizard_final_checkbox_filter_catalogs;
	public static String dialog_connection_wizard_final_checkbox_filter_schemas;
	public static String dialog_connection_wizard_final_checkbox_save_password_locally;
    public static String dialog_connection_wizard_final_checkbox_auto_commit;
	public static String dialog_connection_wizard_final_checkbox_show_system_objects;
	public static String dialog_connection_wizard_final_checkbox_show_util_objects;
    public static String dialog_connection_wizard_final_checkbox_connection_readonly;
	public static String dialog_connection_wizard_final_default_new_connection_name;
	public static String dialog_connection_wizard_final_description;
	public static String dialog_connection_wizard_final_group_filters;
	public static String dialog_connection_wizard_final_group_security;
    public static String dialog_connection_wizard_final_group_misc;
	public static String dialog_connection_wizard_final_header;
	public static String dialog_connection_wizard_final_label_connection_name;
	public static String dialog_connection_wizard_final_filter_catalogs;
	public static String dialog_connection_wizard_final_filter_schemas_users;
	public static String dialog_connection_wizard_final_filter_tables;
	public static String dialog_connection_wizard_final_filter_link_tooltip;
	public static String dialog_connection_wizard_final_filter_link_not_supported_text;
	public static String dialog_connection_wizard_final_filter_link_not_supported_tooltip;
	public static String dialog_connection_wizard_final_button_tunneling;

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
	public static String dialog_connection_wizard_title;

	public static String dialog_cursor_view_monitor_rows_fetched;

	public static String confirm_keep_statement_open_title;
	public static String confirm_keep_statement_open_message;
	public static String confirm_keep_statement_open_toggleMessage;

	public static String confirm_dangerous_sql_title;
	public static String confirm_dangerous_sql_message;
	public static String confirm_dangerous_sql_toggleMessage;

	public static String dialog_data_format_profiles_button_delete_profile;
	public static String dialog_data_format_profiles_button_new_profile;
	public static String dialog_data_format_profiles_confirm_delete_message;
	public static String dialog_data_format_profiles_confirm_delete_title;
	public static String dialog_data_format_profiles_dialog_name_chooser_title;
	public static String dialog_data_format_profiles_error_message;
	public static String dialog_data_format_profiles_error_title;
	public static String dialog_data_format_profiles_title;
	public static String dialog_data_label_value;
	public static String dialog_text_view_open_editor;
	public static String dialog_text_view_open_editor_tip;
	
	public static String dialog_driver_manager_button_delete;
	public static String dialog_driver_manager_button_edit;
	public static String dialog_driver_manager_button_new;
	public static String dialog_driver_manager_button_copy;
	public static String dialog_driver_manager_label_unavailable;
	public static String dialog_driver_manager_label_user_defined;
	public static String dialog_driver_manager_message_cant_delete_text;
	public static String dialog_driver_manager_message_cant_delete_title;
	public static String dialog_driver_manager_message_delete_driver_text;
	public static String dialog_driver_manager_message_delete_driver_title;
	public static String dialog_driver_manager_title;
	
	public static String dialog_edit_driver_button_add_file;
	public static String dialog_edit_driver_button_add_folder;
	public static String dialog_edit_driver_button_add_artifact;
	public static String dialog_edit_driver_button_bind_class;
	public static String dialog_edit_driver_button_classpath;
	public static String dialog_edit_driver_button_update_version;
	public static String dialog_edit_driver_button_details;
	public static String dialog_edit_driver_button_delete;
	public static String dialog_edit_driver_button_down;
	public static String dialog_edit_driver_button_reset_to_defaults;
	public static String dialog_edit_driver_button_up;
	public static String dialog_edit_driver_dialog_driver_error_message;
	public static String dialog_edit_driver_dialog_driver_error_title;
	public static String dialog_edit_driver_dialog_open_driver_directory;
	public static String dialog_edit_driver_dialog_open_driver_library;
	public static String dialog_edit_driver_label_category;
	public static String dialog_edit_driver_label_class_name;
	public static String dialog_edit_driver_label_default_port;
	public static String dialog_edit_driver_label_description;
	public static String dialog_edit_driver_label_driver_class;
	public static String dialog_edit_driver_label_driver_name;
	public static String dialog_edit_driver_label_sample_url;
	public static String dialog_edit_driver_label_website;
	public static String dialog_edit_driver_tab_name_advanced_parameters;
	public static String dialog_edit_driver_tab_name_connection_properties;
    public static String dialog_edit_driver_tab_name_client_homes;
	public static String dialog_edit_driver_tab_name_driver_libraries;
	public static String dialog_edit_driver_tab_name_license;
	public static String dialog_edit_driver_tab_tooltip_advanced_parameters;
	public static String dialog_edit_driver_tab_tooltip_connection_properties;
	public static String dialog_edit_driver_tab_tooltip_driver_libraries;
	public static String dialog_edit_driver_tab_tooltip_license;
	public static String dialog_edit_driver_text_driver_license;
	public static String dialog_edit_driver_title_create_driver;
	public static String dialog_edit_driver_title_edit_driver;

    public static String data_transfer_wizard_name;
    public static String data_transfer_wizard_final_column_source;
	public static String data_transfer_wizard_final_column_target;
	public static String data_transfer_wizard_final_description;
	public static String data_transfer_wizard_final_group_tables;
	public static String data_transfer_wizard_final_name;
	public static String data_transfer_wizard_final_title;
	public static String data_transfer_wizard_init_column_description;
	public static String data_transfer_wizard_init_column_exported;
	public static String data_transfer_wizard_init_description;
	public static String data_transfer_wizard_init_name;
	public static String data_transfer_wizard_init_title;
	public static String data_transfer_wizard_job_container_name;
	public static String data_transfer_wizard_job_name;
	public static String data_transfer_wizard_job_task_export;
	public static String data_transfer_wizard_job_task_export_table_data;
	public static String data_transfer_wizard_job_task_retrieve;
	public static String data_transfer_wizard_output_checkbox_compress;
	public static String data_transfer_wizard_output_checkbox_new_connection;
	public static String data_transfer_wizard_output_checkbox_open_folder;
	public static String data_transfer_wizard_output_checkbox_select_row_count;
	public static String data_transfer_wizard_output_checkbox_selected_columns_only;
	public static String data_transfer_wizard_output_checkbox_selected_rows_only;
	public static String data_transfer_wizard_output_combo_extract_type_item_by_segments;
	public static String data_transfer_wizard_output_combo_extract_type_item_single_query;
	public static String data_transfer_wizard_output_description;
	public static String data_transfer_wizard_output_dialog_directory_message;
	public static String data_transfer_wizard_output_dialog_directory_text;
	public static String data_transfer_wizard_output_group_general;
	public static String data_transfer_wizard_output_group_progress;
	public static String data_transfer_wizard_output_label_directory;
	public static String data_transfer_wizard_output_label_encoding;
	public static String data_transfer_wizard_output_label_extract_type;
	public static String data_transfer_wizard_output_label_file_name_pattern;
	public static String data_transfer_wizard_output_label_insert_bom;
	public static String data_transfer_wizard_output_label_insert_bom_tooltip;
	public static String data_transfer_wizard_output_label_max_threads;
	public static String data_transfer_wizard_output_label_segment_size;
	public static String data_transfer_wizard_output_name;
	public static String data_transfer_wizard_output_title;
	public static String data_transfer_wizard_settings_binaries_item_inline;
	public static String data_transfer_wizard_settings_binaries_item_save_to_file;
	public static String data_transfer_wizard_settings_binaries_item_set_to_null;
	public static String data_transfer_wizard_settings_button_edit;
	public static String data_transfer_wizard_settings_description;
	public static String data_transfer_wizard_settings_group_exporter;
	public static String data_transfer_wizard_settings_group_general;
	public static String data_transfer_wizard_settings_label_binaries;
	public static String data_transfer_wizard_settings_label_encoding;
	public static String data_transfer_wizard_settings_label_formatting;
	public static String data_transfer_wizard_settings_listbox_formatting_item_default;
	public static String data_transfer_wizard_settings_name;
	public static String data_transfer_wizard_settings_title;
	
	public static String dialog_filter_button_add;
	public static String dialog_filter_button_enable;
	public static String dialog_filter_button_remove;
	public static String dialog_filter_button_clear;
	public static String dialog_filter_global_link;
	public static String dialog_filter_list_exclude;
	public static String dialog_filter_list_include;
	public static String dialog_filter_table_column_value;
	public static String dialog_filter_title;
	public static String dialog_find_replace_1_replacement;
	public static String dialog_find_replace_backward;
	public static String dialog_find_replace_cancel;
	public static String dialog_find_replace_close;
	public static String dialog_find_replace_direction;
	public static String dialog_find_replace_error_;
	public static String dialog_find_replace_find;
	public static String dialog_find_replace_find_literal;
	public static String dialog_find_replace_find_replace;
	public static String dialog_find_replace_copy;
	public static String dialog_find_replace_paste;
	public static String dialog_find_replace_goto_line;
	public static String dialog_find_replace_undo;
	public static String dialog_find_replace_redo;
	public static String dialog_find_replace_forward;
	public static String dialog_find_replace_found_literal;
	public static String dialog_find_replace_ignore_case;
	public static String dialog_find_replace_literal_not_found;
	public static String dialog_find_replace_new_find;
	public static String dialog_find_replace_replace;
	public static String dialog_find_replace_replace_all;
	public static String dialog_find_replace_replace_find;
	public static String dialog_find_replace_replace_with;
	public static String dialog_find_replace_replacements;
	public static String dialog_find_replace_searching;
	public static String dialog_find_replace_stop;
	public static String dialog_find_replace_text;
	public static String dialog_go_to_button_close;
	public static String dialog_go_to_button_go_to_location;
	public static String dialog_go_to_button_show_location;
	public static String dialog_go_to_label_enter_location_number;
	public static String dialog_go_to_label_not_number;
	public static String dialog_go_to_label_out_of_range;
	public static String dialog_go_to_title;
	public static String dialog_migrate_wizard_choose_driver_description;
	public static String dialog_migrate_wizard_choose_driver_title;
	public static String dialog_migrate_wizard_name;
	public static String dialog_migrate_wizard_start_description;
	public static String dialog_migrate_wizard_start_title;
	public static String dialog_migrate_wizard_window_title;
	public static String dialog_connection_network_socket_label_host;
	public static String dialog_connection_network_socket_label_port;
	public static String dialog_connection_network_socket_label_username;
	public static String dialog_connection_network_socket_label_password;
	public static String dialog_new_connection_wizard_monitor_load_data_sources;
	public static String dialog_new_connection_wizard_start_description;
	public static String dialog_new_connection_wizard_start_title;
	public static String dialog_new_connection_wizard_title;
	public static String dialog_project_create_wizard_error_already_exists;
	public static String dialog_project_create_wizard_error_cannot_create;
	public static String dialog_project_create_wizard_error_cannot_create_message;
	public static String dialog_project_create_wizard_title;

	public static String dialog_project_export_wizard_main_page;
	public static String dialog_project_export_wizard_monitor_collect_info;
	public static String dialog_project_export_wizard_monitor_export_driver_info;
	public static String dialog_project_export_wizard_monitor_export_libraries;
	public static String dialog_project_export_wizard_monitor_export_project;
	public static String dialog_project_export_wizard_page_checkbox_overwrite_files;
	public static String dialog_project_export_wizard_page_dialog_choose_export_dir_message;
	public static String dialog_project_export_wizard_page_dialog_choose_export_dir_text;
	public static String dialog_project_export_wizard_page_label_directory;
	public static String dialog_project_export_wizard_page_message_check_script;
	public static String dialog_project_export_wizard_page_message_configure_settings;
	public static String dialog_project_export_wizard_page_message_no_output_dir;
	public static String dialog_project_export_wizard_page_title;
	public static String dialog_project_export_wizard_start_archive_name_prefix;
	public static String dialog_project_export_wizard_start_checkbox_libraries;
	public static String dialog_project_export_wizard_start_dialog_directory_message;
	public static String dialog_project_export_wizard_start_dialog_directory_text;
	public static String dialog_project_export_wizard_start_label_directory;
	public static String dialog_project_export_wizard_start_label_output_file;
	public static String dialog_project_export_wizard_start_message_choose_project;
	public static String dialog_project_export_wizard_start_message_configure_settings;
	public static String dialog_project_export_wizard_start_message_empty_output_directory;
	public static String dialog_project_export_wizard_start_title;
	public static String dialog_project_export_wizard_window_title;

	public static String dialog_project_import_wizard_file_checkbox_import_libraries;
	public static String dialog_project_import_wizard_file_column_source_name;
	public static String dialog_project_import_wizard_file_column_target_name;
	public static String dialog_project_import_wizard_file_description;
	public static String dialog_project_import_wizard_file_dialog_export_archive_text;
	public static String dialog_project_import_wizard_file_group_input;
	public static String dialog_project_import_wizard_file_group_projects;
	public static String dialog_project_import_wizard_file_label_file;
	public static String dialog_project_import_wizard_file_message_cannt_find_projects;
	public static String dialog_project_import_wizard_file_message_choose_project;
    public static String dialog_project_import_wizard_file_message_project_exists;
	public static String dialog_project_import_wizard_file_message_ready;
	public static String dialog_project_import_wizard_file_name;
	public static String dialog_project_import_wizard_file_title;
    public static String dialog_project_import_wizard_message_success_import_message;
	public static String dialog_project_import_wizard_message_success_import_title;
	public static String dialog_project_import_wizard_monitor_import_drivers;
	public static String dialog_project_import_wizard_monitor_import_project;
	public static String dialog_project_import_wizard_monitor_import_projects;
	public static String dialog_project_import_wizard_monitor_load_driver;
	public static String dialog_project_import_wizard_monitor_load_libraries;
	public static String dialog_project_import_wizard_title;

	public static String dialog_scripts_export_wizard_page_name;
	public static String dialog_scripts_export_wizard_window_title;

	public static String dialog_scripts_import_wizard_description;
	public static String dialog_scripts_import_wizard_dialog_choose_dir_message;
	public static String dialog_scripts_import_wizard_dialog_choose_dir_text;
	public static String dialog_scripts_import_wizard_dialog_error_text;
	public static String dialog_scripts_import_wizard_dialog_error_title;
	public static String dialog_scripts_import_wizard_dialog_message_no_scripts;
	public static String dialog_scripts_import_wizard_dialog_message_success_imported;
	public static String dialog_scripts_import_wizard_dialog_message_title;
	public static String dialog_scripts_import_wizard_label_default_connection;
	public static String dialog_scripts_import_wizard_label_file_mask;
	public static String dialog_scripts_import_wizard_label_input_directory;
	public static String dialog_scripts_import_wizard_label_root_folder;
	public static String dialog_scripts_import_wizard_monitor_import_scripts;
	public static String dialog_scripts_import_wizard_name;
	public static String dialog_scripts_import_wizard_title;
	public static String dialog_scripts_import_wizard_window_title;

	public static String dialog_search_objects_button_close;
    public static String dialog_search_objects_button_search;
	public static String dialog_search_objects_column_description;
	public static String dialog_search_objects_column_type;
	public static String dialog_search_objects_combo_contains;
	public static String dialog_search_objects_combo_like;
	public static String dialog_search_objects_combo_starts_with;
	public static String dialog_search_objects_group_object_types;
	public static String dialog_search_objects_group_objects_source;
	public static String dialog_search_objects_item_list_info;
	public static String dialog_search_objects_label_name_match;
	public static String dialog_search_objects_label_object_name;
	public static String dialog_search_objects_message_no_objects_like_;
	public static String dialog_search_objects_message_objects_found;
	public static String dialog_search_objects_spinner_max_results;
    public static String dialog_search_objects_case_sensitive;
	public static String dialog_search_objects_title;

	public static String dialog_select_datasource_error_message;
	public static String dialog_select_datasource_error_title;
	public static String dialog_select_datasource_title;
	public static String dialog_struct_columns_select_column;

    public static String dialog_struct_columns_select_error_load_columns_message;
	public static String dialog_struct_columns_select_error_load_columns_title;
	public static String dialog_struct_columns_select_group_columns;
	public static String dialog_struct_columns_select_label_table;
	public static String dialog_struct_columns_select_title;
	public static String dialog_struct_create_entity_group_name;
	public static String dialog_struct_create_entity_title;
	public static String dialog_struct_create_procedure_combo_type;
	public static String dialog_struct_create_procedure_label_name;
	public static String dialog_struct_create_procedure_title;
	public static String dialog_struct_edit_constrain_label_type;
	public static String dialog_struct_edit_fk_column_col_type;
	public static String dialog_struct_edit_fk_column_column;
	public static String dialog_struct_edit_fk_column_ref_col;
	public static String dialog_struct_edit_fk_column_ref_col_type;
	public static String dialog_struct_edit_fk_combo_on_delete;
	public static String dialog_struct_edit_fk_combo_on_update;
	public static String dialog_struct_edit_fk_combo_unik;
	public static String dialog_struct_edit_fk_error_load_constraint_columns_message;
	public static String dialog_struct_edit_fk_error_load_constraint_columns_title;
	public static String dialog_struct_edit_fk_error_load_constraints_message;
	public static String dialog_struct_edit_fk_error_load_constraints_title;
	public static String dialog_struct_edit_fk_label_columns;
	public static String dialog_struct_edit_fk_label_ref_table;
	public static String dialog_struct_edit_fk_label_table;
	public static String dialog_struct_edit_fk_title;
	public static String dialog_struct_edit_index_label_type;

	public static String dialog_tunnel_checkbox_use_handler;
	public static String dialog_tunnel_title;

	public static String dialog_value_view_button_cancel;
	public static String dialog_value_view_button_sat_null;
	public static String dialog_value_view_button_save;
	public static String dialog_value_view_column_description;
	public static String dialog_value_view_column_value;
	public static String dialog_value_view_context_name;
	public static String dialog_value_view_dialog_error_updating_message;
	public static String dialog_value_view_dialog_error_updating_title;
	public static String dialog_value_view_job_selector_name;
	public static String dialog_value_view_label_dictionary;
	public static String dialog_version_update_available_new_version;
	public static String dialog_version_update_button_more_info;
	public static String dialog_version_update_current_version;
	public static String dialog_version_update_n_a;
	public static String dialog_version_update_new_version;
	public static String dialog_version_update_no_new_version;
	public static String dialog_version_update_notes;
	public static String dialog_version_update_press_more_info_;
	public static String dialog_version_update_title;

	public static String dialog_view_classpath_title;
	public static String dialog_view_sql_button_copy;
	public static String dialog_view_sql_button_persist;

	public static String editor_binary_hex_default_font;
	public static String editor_binary_hex_font_style_bold;
	public static String editor_binary_hex_font_style_bold_italic;
	public static String editor_binary_hex_font_style_italic;
	public static String editor_binary_hex_font_style_regular;
	public static String editor_binary_hex_froup_font_selection;
	public static String editor_binary_hex_label_available_fix_width_fonts;
	public static String editor_binary_hex_label_name;
	public static String editor_binary_hex_label_style;
	public static String editor_binary_hex_label_size;
	public static String editor_binary_hex_sample_text;
	public static String editor_binary_hex_status_line_offset;
	public static String editor_binary_hex_status_line_selection;
	public static String editor_binary_hex_status_line_text_insert;
	public static String editor_binary_hex_status_line_text_ovewrite;
	public static String editor_binary_hex_status_line_value;

	public static String editors_entity_dialog_persist_title;
	public static String editors_entity_dialog_preview_title;
	public static String editors_entity_monitor_add_folder;
	public static String editors_entity_monitor_add_node;
	public static String editors_entity_monitor_preview_changes;
	public static String editors_entity_properties_text;
	public static String editors_entity_properties_tooltip_suffix;

	public static String editors_sql_data_grid;
	public static String editors_sql_description;
	public static String editors_sql_error_cant_execute_query_message;
	public static String editors_sql_error_cant_execute_query_title;
	public static String editors_sql_error_cant_obtain_session;
	public static String editors_sql_error_execution_plan_message;
	public static String editors_sql_error_execution_plan_title;
	public static String editors_sql_execution_log;
	public static String editors_sql_explain_plan;
    public static String editors_sql_output;
	public static String editors_sql_job_execute_query;
	public static String editors_sql_job_execute_script;
	public static String editors_sql_save_on_close_message;
    public static String editors_sql_save_on_close_text;
	public static String editors_sql_status_cant_obtain_document;
	public static String editors_sql_status_empty_query_string;
	public static String editors_sql_status_not_connected_to_database;
	public static String editors_sql_status_rows_updated;
	public static String editors_sql_status_statement_executed;
	public static String editors_sql_status_statement_executed_no_rows_updated;
	public static String editors_sql_staus_connected_to;

	public static String model_html_read_database_meta_data;

	public static String model_jdbc_bad_content_value_;
	public static String model_jdbc_content_length;
	public static String model_jdbc_content_type;
	public static String model_jdbc_could_not_save_content;
	public static String model_jdbc_could_not_save_content_to_file_;
	public static String model_jdbc_load_from_file_;
	public static String model_jdbc_save_to_file_;
	public static String model_jdbc_set_to_current_time;
	public static String model_jdbc_unsupported_content_value_type_;

	public static String model_navigator_Description;
	public static String model_navigator_Name;
	public static String model_navigator_load_;
	public static String model_navigator_load_items_;
	public static String model_navigator_Model_root;
	public static String model_navigator_Project;
	public static String model_navigator_resource_exception_already_exists;
	public static String model_navigator_Root;

	public static String model_project_bookmarks_folder;
	public static String model_project_cant_open_bookmark;
	public static String model_project_open_bookmark;
	public static String model_project_Script;
	public static String model_project_Scripts;

	public static String model_ssh_configurator_group_settings;
	public static String model_ssh_configurator_group_advanced;
	public static String model_ssh_configurator_checkbox_save_pass;
	public static String model_ssh_configurator_combo_auth_method;
	public static String model_ssh_configurator_combo_password;
	public static String model_ssh_configurator_combo_pub_key;
	public static String model_ssh_configurator_dialog_choose_private_key;
	public static String model_ssh_configurator_label_host_ip;
	public static String model_ssh_configurator_label_password;
	public static String model_ssh_configurator_label_passphrase;
	public static String model_ssh_configurator_label_port;
	public static String model_ssh_configurator_label_private_key;
	public static String model_ssh_configurator_label_user_name;
	public static String model_ssh_configurator_label_local_port;
	public static String model_ssh_configurator_label_local_port_description;
    public static String model_ssh_configurator_label_keep_alive;
	public static String model_ssh_configurator_label_tunnel_timeout;

	public static String pref_page_confirmations_combo_always;
	public static String pref_page_confirmations_combo_never;
	public static String pref_page_confirmations_combo_prompt;
	public static String pref_page_confirmations_group_general_actions;
	public static String pref_page_confirmations_group_object_editor;
	
	public static String pref_page_content_editor_checkbox_commit_on_content_apply;
	public static String pref_page_content_editor_checkbox_commit_on_value_apply;
	public static String pref_page_content_editor_checkbox_edit_long_as_lobs;
    public static String pref_page_content_editor_group_keys;
    public static String pref_page_content_editor_checkbox_keys_always_use_all_columns;
	public static String pref_page_content_editor_checkbox_new_rows_after;
	public static String pref_page_content_editor_checkbox_refresh_after_update;
	public static String pref_page_content_editor_group_content;
	public static String pref_page_content_editor_label_max_text_length;
    public static String pref_page_content_editor_group_hex;
    public static String pref_page_content_editor_hex_encoding;
	public static String pref_page_content_cache_clob;
	public static String pref_page_content_cache_blob;
	
	public static String pref_page_database_general_label_cache_max_size;
	public static String pref_page_data_format_button_manage_profiles;
	public static String pref_page_data_format_group_format;
	public static String pref_page_data_format_label_profile;
	public static String pref_page_data_format_label_sample;
	public static String pref_page_data_format_label_settingt;
	public static String pref_page_data_format_label_type;
	
	public static String pref_page_database_general_separate_meta_connection;
	public static String pref_page_database_general_checkbox_case_sensitive_names;
	public static String pref_page_database_general_checkbox_keep_cursor;
	public static String pref_page_database_general_checkbox_rollback_on_error;
	public static String pref_page_database_general_checkbox_show_row_count;
    public static String pref_page_database_general_group_navigator;
	public static String pref_page_database_general_group_metadata;
	public static String pref_page_database_general_group_ordering;
	public static String pref_page_database_general_group_queries;
	public static String pref_page_database_general_group_transactions;
	public static String pref_page_database_general_label_max_lob_length;
	public static String pref_page_database_general_label_result_set_max_size;
	public static String pref_page_database_general_label_result_set_cancel_timeout;
	public static String pref_page_database_general_label_result_set_cancel_timeout_tip;
    public static String pref_page_database_resultsets_group_binary;
    public static String pref_page_database_resultsets_label_binary_use_strings;
    public static String pref_page_database_resultsets_label_binary_presentation;
    public static String pref_page_database_resultsets_label_binary_editor_type;
    public static String pref_page_database_resultsets_label_binary_strings_max_length;
    public static String pref_page_database_resultsets_label_auto_fetch_segment;
    public static String pref_page_database_resultsets_label_use_sql;
    public static String pref_page_database_resultsets_label_server_side_order;
	public static String pref_page_database_resultsets_label_fetch_size;
	public static String pref_page_database_resultsets_label_read_metadata;
	public static String pref_page_database_resultsets_label_read_references;
	public static String pref_page_database_resultsets_group_string;
	public static String pref_page_database_resultsets_checkbox_string_use_editor;
	public static String pref_page_database_resultsets_checkbox_string_use_editor_tip;

    public static String pref_page_query_manager_checkbox_ddl_executions;
	public static String pref_page_query_manager_checkbox_metadata_read;
    public static String pref_page_query_manager_checkbox_metadata_write;
	public static String pref_page_query_manager_checkbox_other;
	public static String pref_page_query_manager_checkbox_queries;
	public static String pref_page_query_manager_checkbox_scripts;
	public static String pref_page_query_manager_checkbox_sessions;
	public static String pref_page_query_manager_checkbox_transactions;
	public static String pref_page_query_manager_checkbox_user_queries;
	public static String pref_page_query_manager_checkbox_user_filtered;
	public static String pref_page_query_manager_checkbox_user_scripts;
	public static String pref_page_query_manager_checkbox_utility_functions;
	public static String pref_page_query_manager_group_object_types;
	public static String pref_page_query_manager_group_query_types;
    public static String pref_page_query_manager_group_settings;
	public static String pref_page_query_manager_group_storage;
    public static String pref_page_query_manager_checkbox_store_log_file;
    public static String pref_page_query_manager_logs_folder;
	public static String pref_page_query_manager_label_days_to_store_log;
	public static String pref_page_query_manager_label_entries_per_page;

	public static String pref_page_sql_editor_checkbox_fetch_resultsets;
	public static String pref_page_sql_editor_text_statement_delimiter;
    public static String pref_page_sql_editor_checkbox_ignore_native_delimiter;
	public static String pref_page_sql_editor_checkbox_blank_line_delimiter;
	public static String pref_page_sql_editor_checkbox_enable_sql_parameters;
	public static String pref_page_sql_editor_title_pattern;
	public static String pref_page_sql_editor_checkbox_delete_empty_scripts;
	public static String pref_page_sql_editor_checkbox_put_new_scripts;
	public static String pref_page_sql_editor_checkbox_create_script_folders;
	public static String pref_page_sql_editor_checkbox_reset_cursor;
	public static String pref_page_sql_editor_checkbox_max_editor_on_script_exec;
	public static String pref_page_sql_editor_checkbox_enable_sql_anonymous_parameters;
	public static String pref_page_sql_editor_text_anonymous_parameter_mark;
	public static String pref_page_sql_editor_text_named_parameter_prefix;
	public static String pref_page_sql_editor_combo_item_each_line_autocommit;
	public static String pref_page_sql_editor_combo_item_each_spec_line;
	public static String pref_page_sql_editor_combo_item_ignore;
	public static String pref_page_sql_editor_combo_item_no_commit;
	public static String pref_page_sql_editor_combo_item_script_end;
	public static String pref_page_sql_editor_combo_item_stop_commit;
	public static String pref_page_sql_editor_combo_item_stop_rollback;
	public static String pref_page_sql_editor_group_common;
	public static String pref_page_sql_editor_group_resources;
	public static String pref_page_sql_editor_group_misc;
	public static String pref_page_results_group_advanced;
	public static String pref_page_sql_editor_group_scripts;
	public static String pref_page_sql_editor_group_parameters;
	public static String pref_page_sql_editor_group_delimiters;
	public static String pref_page_sql_editor_label_commit_after_line;
	public static String pref_page_sql_editor_label_commit_type;
	public static String pref_page_sql_editor_label_error_handling;
    public static String pref_page_sql_editor_label_invalidate_before_execute;
	public static String pref_page_sql_editor_label_sql_timeout;
	public static String pref_page_sql_editor_label_sound_on_query_end;
	public static String pref_page_sql_editor_label_refresh_defaults_after_execute;
	
	public static String pref_page_target_button_use_datasource_settings;
	public static String pref_page_target_link_show_datasource_settings;
	public static String pref_page_target_link_show_global_settings;
	
	public static String pref_page_ui_general_checkbox_automatic_updates;
	public static String pref_page_ui_general_combo_language;
	public static String pref_page_ui_general_combo_language_tip;
	public static String pref_page_ui_general_keep_database_editors;
	public static String pref_page_ui_general_refresh_editor_on_open;
	public static String pref_page_ui_general_group_general;
	public static String pref_page_ui_general_group_language;
	public static String pref_page_ui_general_group_editors;
	public static String pref_page_ui_general_group_http_proxy;
	public static String pref_page_ui_general_label_proxy_host;
	public static String pref_page_ui_general_spinner_proxy_port;
    public static String pref_page_ui_general_label_proxy_user;
    public static String pref_page_ui_general_label_proxy_password;
    public static String pref_page_drivers_group_location;

	public static String registry_entity_editor_descriptor_description;
	public static String registry_entity_editor_descriptor_name;

	public static String runtime_jobs_connect_name;
	public static String runtime_jobs_connect_status_connected;
	public static String runtime_jobs_connect_status_error;
	public static String runtime_jobs_connect_thread_name;
	public static String runtime_jobs_disconnect_error;
	public static String runtime_jobs_disconnect_name;

	public static String toolbar_datasource_selector_action_read_databases;
	public static String toolbar_datasource_selector_combo_database_tooltip;
	public static String toolbar_datasource_selector_combo_datasource_tooltip;
	public static String toolbar_datasource_selector_empty;
	public static String toolbar_datasource_selector_error_change_database_message;
	public static String toolbar_datasource_selector_error_change_database_title;
	public static String toolbar_datasource_selector_error_database_not_found;
	public static String toolbar_datasource_selector_error_database_change_not_supported;
	public static String toolbar_datasource_selector_resultset_segment_size;
	public static String toolbar_datasource_selector_connected;
	public static String toolbar_datasource_selector_all;
	
	public static String toolbar_editors_sql_run_statement_name;
	public static String toolbar_editors_sql_run_statementNew_name;
	public static String toolbar_editors_sql_run_script_name;
    public static String toolbar_editors_sql_run_scriptNew_name;
	public static String toolbar_editors_sql_run_explain_name;
	
	public static String tools_script_execute_wizard_task_completed;
	public static String tools_wizard_dialog_button_start;
	public static String tools_wizard_error_task_error_message;
	public static String tools_wizard_error_task_error_title;
	public static String tools_wizard_error_task_canceled;
	public static String tools_wizard_log_process_exit_code;
	public static String tools_wizard_log_io_error;
	public static String tools_wizard_message_client_home_not_found;
	public static String tools_wizard_message_no_client_home;
	public static String tools_wizard_page_log_task_finished;
	public static String tools_wizard_page_log_task_log_reader;
	public static String tools_wizard_page_log_task_progress;
	public static String tools_wizard_page_log_task_progress_log;
	public static String tools_wizard_page_log_task_started_at;
	
	public static String ui_actions_context_search_name;
    public static String ui_actions_exit_emergency_question;
	public static String ui_common_button_help;
	public static String ui_properties_category_information;
	public static String ui_properties_category_structure;
	public static String ui_properties_name;
	public static String ui_properties_task_add_folder;
	public static String ui_properties_task_add_node;
	public static String ui_properties_tree_viewer__to_default;
	public static String ui_properties_tree_viewer_action_copy_name;
	public static String ui_properties_tree_viewer_action_copy_value;
	public static String ui_properties_tree_viewer_action_reset_value;
	public static String ui_properties_tree_viewer_category_general;
	public static String ui_properties_value;
	
	public static String dialog_connection_edit_driver_button;
	public static String dialog_connection_driver;

	public static String editor_sql_preference;

	public static String controls_resultset_edit_save;
    public static String controls_resultset_edit_cancel;
	public static String controls_resultset_edit_script;
	public static String controls_resultset_config_panels;
	public static String controls_resultset_config_record;

	// New Connection Wizard
	public static String dialog_setting_connection_wizard_title;
	public static String dialog_setting_connection_general;
	public static String dialog_setting_connection_driver_properties_title;
	public static String dialog_setting_connection_driver_properties_description;
	public static String dialog_connection_network_title;
	public static String dialog_connection_wizard_final_label_connection_type;
	public static String dialog_connection_wizard_final_label_edit;
	public static String dialog_connection_wizard_final_label_connection_folder;
	public static String dialog_connection_wizard_final_label_connection;
	public static String dialog_connection_wizard_final_label_isolation_level;
	public static String dialog_connection_wizard_final_label_default_schema;
	public static String dialog_connection_wizard_final_label_keepalive;
	public static String dialog_connection_wizard_final_label_isolation_level_tooltip;
	public static String dialog_connection_wizard_final_label_default_schema_tooltip;
	public static String dialog_connection_wizard_final_label_keepalive_tooltip;
	public static String dialog_connection_wizard_final_label_bootstrap_query;
	public static String dialog_connection_wizard_configure;
	public static String dialog_connection_wizard_final_label_shell_command;
	public static String dialog_connection_wizard_description;
	public static String dialog_connection_wizard_final_label_bootstrap_tooltip;
	public static String dialog_connection_wizard_socksproxy_host;
	public static String dialog_connection_wizard_socksproxy_port;
	public static String dialog_connection_wizard_socksproxy_username;
	public static String dialog_connection_wizard_socksproxy_password;
	public static String dialog_connection_driver_treecontrol_initialText;
	public static String dialog_connection_driver_project;
	

	//Preference/Properties
	// ResultSetsMain
	public static String pref_page_database_resultsets_label_read_metadata_tip;
	public static String pref_page_database_resultsets_label_read_references_tip;
	public static String pref_page_database_resultsets_label_fetch_size_tip;
	// ResultSetPresentation
	public static String pref_page_database_resultsets_group_common;
	public static String pref_page_database_resultsets_label_switch_mode_on_rows;
	public static String pref_page_database_resultsets_label_show_column_description;
	public static String pref_page_database_resultsets_label_show_connection_name;
	public static String pref_page_database_resultsets_label_calc_column_width_by_values;
    public static String pref_page_database_resultsets_label_calc_column_width_by_values_tip;	
	public static String pref_page_database_resultsets_label_structurize_complex_types;
	public static String pref_page_database_resultsets_label_structurize_complex_types_tip;
	public static String pref_page_database_resultsets_group_grid;
	public static String pref_page_database_resultsets_label_mark_odd_rows;
	public static String pref_page_database_resultsets_label_colorize_data_types;
	public static String pref_page_database_resultsets_label_right_justify_numbers_and_date;
	public static String pref_page_database_resultsets_label_row_batch_size;
	public static String pref_page_database_resultsets_label_show_cell_icons;
	public static String pref_page_database_resultsets_label_double_click_behavior;
	public static String pref_page_database_resultsets_group_plain_text;
	public static String pref_page_database_resultsets_lable_value_format;
	public static String pref_page_database_resultsets_label_maximum_column_length;
	public static String pref_page_database_resultsets_label_text_show_nulls;
	public static String pref_page_database_resultsets_label_text_delimiter_leading;
	public static String pref_page_database_resultsets_label_text_delimiter_trailing;
	// Connections
	public static String pref_page_database_client_name_group;
	public static String pref_page_database_client_name_group_description;
	public static String pref_page_database_label_disable_client_application_name;
	public static String pref_page_database_label_override_client_application_name;
	public static String pref_page_database_label_client_application_name;
	// SQLEditor
	public static String pref_page_sql_editor_group_connections;
	public static String pref_page_sql_editor_label_separate_connection_each_editor;
	public static String pref_page_sql_editor_label_connect_on_editor_activation;
	public static String pref_page_sql_editor_label_connect_on_query_execute;
	public static String pref_page_sql_editor_group_auto_save;
	public static String pref_page_sql_editor_label_auto_save_on_close;
	public static String pref_page_sql_editor_label_save_on_query_execute;
	public static String pref_page_sql_editor_group_result_view;
	public static String pref_page_sql_editor_label_close_results_tab_on_error;
	public static String pref_page_sql_editor_label_results_orientation;
	public static String pref_page_sql_editor_label_results_orientation_tip;
	public static String pref_page_sql_editor_link_text_editor;
	// SQLExecute
	public static String pref_page_sql_editor_label_sql_timeout_tip;
	public static String pref_page_sql_editor_enable_parameters_in_ddl;
	// SQLFormat
	public static String pref_page_sql_format_group_auto_close;
	public static String pref_page_sql_format_label_single_quotes;
	public static String pref_page_sql_format_label_double_quotes;
	public static String pref_page_sql_format_label_brackets;
	public static String pref_page_sql_format_group_auto_format;
	public static String pref_page_sql_format_label_convert_keyword_case;
	public static String pref_page_sql_format_label_convert_keyword_case_tip;
	public static String pref_page_sql_format_label_extract_sql_from_source_code;
	public static String pref_page_sql_format_label_extract_sql_from_source_code_tip;
	public static String pref_page_sql_format_group_formatter;
	public static String pref_page_sql_format_label_formatter;
	public static String pref_page_sql_format_label_keyword_case;
	public static String pref_page_sql_format_label_external_command_line;
	public static String pref_page_sql_format_label_external_set_content_tool_tip;
	public static String pref_page_sql_format_label_external_use_temp_file;
	public static String pref_page_sql_format_label_external_use_temp_file_tip;
	public static String pref_page_sql_format_label_external_exec_timeout;
	public static String pref_page_sql_format_label_external_exec_timeout_tip;
	// SQLCompletion
	public static String pref_page_sql_completion_group_sql_assistant;
	public static String pref_page_sql_completion_label_enable_auto_activation;
	public static String pref_page_sql_completion_label_enable_auto_activation_tip;
	public static String pref_page_sql_completion_label_auto_activation_delay;
	public static String pref_page_sql_completion_label_set_auto_activation_delay_tip;
	public static String pref_page_sql_completion_label_activate_on_typing;
	public static String pref_page_sql_completion_label_activate_on_typing_tip;
	public static String pref_page_sql_completion_label_auto_insert_proposal;
	public static String pref_page_sql_completion_label_auto_insert_proposal_tip;
	public static String pref_page_sql_completion_label_insert_case;
	public static String pref_page_sql_completion_label_hide_duplicate_names;
	public static String pref_page_sql_completion_label_use_short_names;
	public static String pref_page_sql_completion_label_use_long_names;
	public static String pref_page_sql_completion_label_insert_space;
	public static String pref_page_sql_completion_label_use_global_search;
	public static String pref_page_sql_completion_label_use_global_search_tip;
	public static String pref_page_sql_completion_group_folding;
	public static String pref_page_sql_completion_label_folding_enabled;
	public static String pref_page_sql_completion_label_folding_enabled_tip;
	// MetaData
	public static String pref_page_database_general_separate_meta_connection_tip;
	public static String pref_page_database_general_checkbox_case_sensitive_names_tip;
	public static String pref_page_database_general_checkbox_show_row_count_tip;
	public static String pref_page_database_general_server_side_object_filters;
	public static String pref_page_database_general_server_side_object_filters_tip;
	public static String pref_page_database_general_group_query_metadata;
	public static String pref_page_database_general_use_column_names;
	public static String pref_page_database_general_use_column_names_tip;
	// EntityEditor
	public static String pref_page_ui_general_keep_database_editors_tip;
	public static String pref_page_ui_general_refresh_editor_on_open_tip;
	public static String pref_page_ui_general_show_full_name_in_editor;
	public static String pref_page_ui_general_show_preview_on_save;
	// Drivers
	public static String pref_page_ui_general_group_settings;
	public static String pref_page_ui_general_check_new_driver_versions;
	public static String pref_page_drivers_local_folder;
	public static String pref_page_drivers_group_file_repositories;
	public static String pref_page_drivers_button_add;
	public static String pref_page_drivers_label_enter_drivers_location_url;
	public static String pref_page_drivers_button_remove;
	// DriversMaven
	public static String pref_page_drivers_maven_group_repositories;
	public static String pref_page_drivers_maven_button_add;
	public static String pref_page_drivers_maven_label_enter_maven_repository_url;
	public static String pref_page_drivers_maven_label_bad_url;
	public static String pref_page_drivers_maven_label_bad_url_tip;
	public static String pref_page_drivers_maven_button_remove;
	// public static String pref_page_drivers_maven_button_disable;
	public static String pref_page_drivers_maven_button_up;
	public static String pref_page_drivers_maven_button_down;
	public static String pref_page_drivers_maven_group_properties;
	public static String pref_page_drivers_maven_label_name;
	public static String pref_page_drivers_maven_label_scope;
	public static String pref_page_drivers_maven_group_authentication;
	public static String pref_page_drivers_maven_label_user;
	public static String pref_page_drivers_maven_label_password;
	public static String pref_page_drivers_maven_label_enable;
	public static String pref_page_drivers_maven_label_disable;
	// DatabaseGeneral
	public static String pref_page_ui_general_group_task_bar;
	public static String pref_page_ui_general_label_enable_long_operations;
	public static String pref_page_ui_general_label_enable_long_operations_tip;
	public static String pref_page_ui_general_label_long_operation_timeout;
	public static String pref_page_ui_general_group_resources;
	public static String pref_page_ui_general_label_default_resource_encoding;
	public static String pref_page_ui_general_label_set_default_resource_encoding_tip;
	public static String pref_page_ui_general_group_debug_logs;
	public static String pref_page_ui_general_label_enable_debug_logs;
	public static String pref_page_ui_general_label_enable_debug_logs_tip;
	public static String pref_page_ui_general_label_log_file_location;
	public static String pref_page_ui_general_label_open_file_text;
	public static String pref_page_ui_general_label_options_take_effect_after_restart;
	public static String pref_page_ui_general_label_settings;
	// DatabaseNavigator
	public static String pref_page_database_general_label_expand_navigator_tree;
	public static String pref_page_database_general_label_show_tips_in_tree;
	public static String pref_page_database_general_label_show_tips_in_tree_tip;
	public static String pref_page_database_general_label_order_elements_alphabetically;
	public static String pref_page_database_general_label_folders_first;
	public static String pref_page_database_general_label_folders_first_tip;
	public static String pref_page_database_general_label_group_database_by_driver;
	public static String pref_page_database_general_label_sync_editor_connection_with_navigator;
	public static String pref_page_database_general_label_sync_editor_connection_with_navigator_tip;
	public static String pref_page_database_general_label_double_click_node;
	public static String pref_page_database_general_label_double_click_node_open_properties;
	public static String pref_page_database_general_label_double_click_node_expand_collapse;
	public static String pref_page_database_general_label_double_click_connection;
	public static String pref_page_database_general_label_double_click_connection_open_properties;
	public static String pref_page_database_general_label_double_click_connection_conn_disconn;
	public static String pref_page_database_general_label_double_click_connection_open_sqleditor;
	public static String pref_page_database_general_label_double_click_connection_expand_collapse;
	// ConnectionTypes
	public static String pref_page_connection_types_label_table_column_name;
	public static String pref_page_connection_types_label_table_column_description;
	public static String pref_page_connection_types_label_delete_connection_type;
	public static String pref_page_connection_types_label_delete_connection_type_description;
	public static String pref_page_connection_types_group_settings;
	public static String pref_page_connection_types_label_name;
	public static String pref_page_connection_types_label_description;
	public static String pref_page_connection_types_label_color;
	public static String pref_page_connection_types_label_auto_commit_by_default;
	public static String pref_page_connection_types_label_confirm_sql_execution;
	// ProjectSettings
	public static String pref_page_projects_settings_label_resource_location;
	public static String pref_page_projects_settings_label_resource;
	public static String pref_page_projects_settings_label_folder;
	public static String pref_page_projects_settings_label_select;
	public static String pref_page_projects_settings_label_root_folder;
	public static String pref_page_projects_settings_label_not_use_hidden_folders;
	public static String pref_page_projects_settings_label_not_store_resources_in_another_project;
	public static String pref_page_projects_settings_label_restart_require_refresh_global_settings;
	// Preference/Properties

	// Connection edit
	public static String dialog_connection_edit_title;
	public static String dialog_connection_edit_connection_settings_variables_hint_label;

	public static String dialog_connection_edit_wizard_conn_conf_network_link;

	public static String dialog_connection_edit_wizard_general;
	public static String dialog_connection_edit_wizard_general_bootstrap_query_title;
	public static String dialog_connection_edit_wizard_general_bootstrap_query_sql_label;
	public static String dialog_connection_edit_wizard_general_bootstrap_query_sql_title;
	public static String dialog_connection_edit_wizard_general_bootstrap_query_ignore_error_lable;
	public static String dialog_connection_edit_wizard_general_filter_save_button;
	public static String dialog_connection_edit_wizard_general_filter_remove_button;
	public static String dialog_connection_edit_wizard_general_filter_save_label;
	public static String dialog_connection_edit_wizard_general_filter_name_label;
	public static String dialog_connection_edit_wizard_general_filter_hint_text;

	public static String dialog_connection_edit_wizard_shell_cmd;
	public static String dialog_connection_edit_wizard_shell_cmd_pause_label;
	public static String dialog_connection_edit_wizard_shell_cmd_pause_tooltip;
	public static String dialog_connection_edit_wizard_shell_cmd_directory_label;
	public static String dialog_connection_edit_wizard_shell_cmd_directory_title;
	public static String dialog_connection_edit_wizard_shell_cmd_variables_hint_label;
	public static String dialog_connection_edit_wizard_shell_cmd_variables_hint_title;

	public static String dialog_connection_edit_wizard_metadata;
	public static String dialog_connection_edit_wizard_metadata_description;
	public static String dialog_connection_edit_wizard_resultset;
	public static String dialog_connection_edit_wizard_resultset_description;
	public static String dialog_connection_edit_wizard_binary;
	public static String dialog_connection_edit_wizard_binary_description;
	public static String dialog_connection_edit_wizard_data_format;
	public static String dialog_connection_edit_wizard_data_format_description;
	public static String dialog_connection_edit_wizard_presentation;
	public static String dialog_connection_edit_wizard_presentation_description;
	public static String dialog_connection_edit_wizard_sql_editor;
	public static String dialog_connection_edit_wizard_sql_editor_description;
	public static String dialog_connection_edit_wizard_sql_processing;
	public static String dialog_connection_edit_wizard_sql_processing_description;
	
	public static String dialog_connection_edit_wizard_conn_change_title;
	public static String dialog_connection_edit_wizard_conn_change_question;
	public static String dialog_connection_edit_wizard_lock_pwd_title;
	public static String dialog_connection_edit_wizard_bad_pwd_title;
	public static String dialog_connection_edit_wizard_bad_pwd_msg;
	public static String dialog_connection_edit_wizard_error_md5_title;
	public static String dialog_connection_edit_wizard_error_md5_msg;
	//Connection edit

    // Driver edit
	public static String dialog_edit_driver_setting;
	public static String dialog_edit_driver_type_label;
	public static String dialog_edit_driver_embedded_label;
	public static String dialog_edit_driver_description;
	public static String dialog_edit_driver_edit_maven_title;
	public static String dialog_edit_driver_edit_maven_group_id_label;
	public static String dialog_edit_driver_edit_maven_artifact_id_label;
	public static String dialog_edit_driver_edit_maven_classfier_label;
	public static String dialog_edit_driver_edit_maven_version_label;
	
	public static String dialog_edit_driver_text_driver_library;	
	public static String dialog_edit_driver_info;
	public static String dialog_edit_driver_driver;
	public static String dialog_edit_driver_library;
	public static String dialog_edit_driver_path;
	public static String dialog_edit_driver_version;
	public static String dialog_edit_driver_file;
	public static String dialog_edit_driver_tab_depencencies;
	public static String dialog_edit_driver_tab_depencencies_tooltip;
	public static String dialog_edit_driver_tab_detail;
	public static String dialog_edit_driver_tab_detail_tooltip;
	public static String dialog_edit_driver_text_license;
	// Driver edit

	// Driver download
	public static String dialog_driver_download_button_edit_dirver;
	public static String dialog_driver_download_button_add_jars;
	
	public static String dialog_driver_download_wizard_title_setting;
	public static String dialog_driver_download_wizard_title_upload_files;
	public static String dialog_driver_download_wizard_title_setup_files;
	public static String dialog_driver_download_wizard_download;
	public static String dialog_driver_download_wizard_open_download;

	public static String dialog_driver_download_page_vendor_link;
	public static String dialog_driver_download_page_download_conf_link;

	public static String dialog_driver_download_manual_page_config_driver_file;
	public static String dialog_driver_download_manual_page_download_driver_file;
	public static String dialog_driver_download_manual_page_download_config_driver_file;
	public static String dialog_driver_download_manual_page_driver_file_missing_text;
	public static String dialog_driver_download_manual_page_driver_file;
	public static String dialog_driver_download_manual_page_column_file;
	public static String dialog_driver_download_manual_page_column_required;
	public static String dialog_driver_download_manual_page_column_description;
	public static String dialog_driver_download_manual_page_yes;
	public static String dialog_driver_download_manual_page_no;
	
	public static String dialog_driver_download_auto_page_auto_download;
	public static String dialog_driver_download_auto_page_download_driver_files;
	public static String dialog_driver_download_auto_page_download_specific_driver_files;
	public static String dialog_driver_download_auto_page_driver_file_missing_text;
	public static String dialog_driver_download_auto_page_force_download;
	public static String dialog_driver_download_auto_page_force_download_tooltip;
	public static String dialog_driver_download_auto_page_required_files;
	public static String dialog_driver_download_auto_page_change_driver_version_text;
	public static String dialog_driver_download_auto_page_obtain_driver_files_text;
	public static String dialog_driver_download_auto_page_cannot_resolve_libraries_text;
	public static String dialog_driver_download_auto_page_driver_download_error;
	public static String dialog_driver_download_auto_page_driver_download_error_msg;
	public static String dialog_driver_download_auto_page_driver_security_warning;
	public static String dialog_driver_download_auto_page_driver_security_warning_msg;
	public static String dialog_driver_download_auto_page_download_rate;
	public static String dialog_driver_download_auto_page_download_failed_msg;
	// Driver download
		
	// SQL editor resultset filter panel
	public static String sql_editor_resultset_tool_item_log;
	public static String sql_editor_resultset_tool_item_output;
	public static String sql_editor_resultset_filter_panel_text_enter_sql_to_filter;
	public static String sql_editor_resultset_filter_panel_text_enter_filter_not_support;
	public static String sql_editor_resultset_filter_panel_btn_apply;
	public static String sql_editor_resultset_filter_panel_btn_remove;
	public static String sql_editor_resultset_filter_panel_btn_save;
	public static String sql_editor_resultset_filter_panel_btn_custom;
	public static String sql_editor_resultset_filter_panel_btn_config_refresh;
	public static String sql_editor_resultset_filter_panel_btn_stop_refresh;
	public static String sql_editor_resultset_filter_panel_label;
	public static String sql_editor_resultset_filter_panel_btn_open_console;
	public static String sql_editor_resultset_filter_panel_menu_refresh_interval;
	public static String sql_editor_resultset_filter_panel_menu_stop;
	public static String sql_editor_resultset_filter_panel_menu_customize;
	public static String sql_editor_resultset_filter_panel_control_no_data;
	public static String sql_editor_resultset_filter_panel_control_execute_to_see_reslut;
	// SQL editor resultset filter panel
	
	//object properties editor
	public static String obj_editor_properties_control_action_filter_setting;
	public static String obj_editor_properties_control_action_configure_columns;
	public static String obj_editor_properties_control_action_configure_columns_description;
	//object properties editor
	
	
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreMessages.class);
	}

	private CoreMessages() {
	}
}
