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
    public static String actions_menu_edit_ContentFormat;

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

	public static String controls_resultset_viewer_action_export;

	public static String controls_resultset_viewer_action_order_filter;

    public static String controls_resultset_viewer_action_custom_filter;

	public static String controls_resultset_viewer_action_refresh;

    public static String controls_resultset_viewer_action_options;

	public static String controls_resultset_viewer_action_set_to_null;

    public static String controls_resultset_viewer_action_reset_value;

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

	public static String model_connection_events_event_after_connect;

	public static String model_connection_events_event_after_disconnect;

	public static String model_connection_events_event_before_connect;

	public static String model_connection_events_event_before_disconnect;

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

	public static String dialog_data_format_profiles_button_delete_profile;

	public static String dialog_data_format_profiles_button_new_profile;

	public static String dialog_data_format_profiles_confirm_delete_message;

	public static String dialog_data_format_profiles_confirm_delete_title;

	public static String dialog_data_format_profiles_dialog_name_chooser_title;

	public static String dialog_data_format_profiles_error_message;

	public static String dialog_data_format_profiles_error_title;

	public static String dialog_data_format_profiles_title;

	public static String dialog_data_label_value;

	public static String dialog_driver_manager_button_delete;

	public static String dialog_driver_manager_button_edit;

	public static String dialog_driver_manager_button_new;

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

	public static String dialog_new_connection_wizard_monitor_load_data_sources;

	public static String dialog_new_connection_wizard_start_description;

	public static String dialog_new_connection_wizard_start_title;

	public static String dialog_new_connection_wizard_title;

	public static String dialog_project_create_settings_description;

	public static String dialog_project_create_settings_group_general;

	public static String dialog_project_create_settings_label_description;

	public static String dialog_project_create_settings_label_name;

	public static String dialog_project_create_settings_name;

	public static String dialog_project_create_settings_title;

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

    public static String model_ssh_configurator_label_keep_alive;

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
	public static String pref_page_content_editor_group_content;
	public static String pref_page_content_editor_label_max_text_length;
    public static String pref_page_content_editor_group_hex;
    public static String pref_page_content_editor_hex_encoding;
	public static String pref_page_content_cache_clob;

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

    public static String pref_page_database_resultsets_group_binary;
    public static String pref_page_database_resultsets_label_binary_use_strings;
    public static String pref_page_database_resultsets_label_binary_presentation;
    public static String pref_page_database_resultsets_label_binary_editor_type;
    public static String pref_page_database_resultsets_label_binary_strings_max_length;
    public static String pref_page_database_resultsets_label_auto_fetch_segment;
    public static String pref_page_database_resultsets_label_use_sql;
    public static String pref_page_database_resultsets_label_server_side_order;

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
	public static String pref_page_sql_editor_checkbox_put_new_scripts;
	public static String pref_page_sql_editor_checkbox_reset_cursor;
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
	public static String pref_page_sql_editor_group_scripts;
	public static String pref_page_sql_editor_group_parameters;
	public static String pref_page_sql_editor_group_delimiters;

	public static String pref_page_sql_editor_label_commit_after_line;

	public static String pref_page_sql_editor_label_commit_type;

	public static String pref_page_sql_editor_label_error_handling;
    public static String pref_page_sql_editor_label_invalidate_before_execute;
	public static String pref_page_sql_editor_label_sql_timeout;

	public static String pref_page_target_button_use_datasource_settings;

	public static String pref_page_target_link_show_datasource_settings;

	public static String pref_page_target_link_show_global_settings;

	public static String pref_page_ui_general_checkbox_automatic_updates;
	public static String pref_page_ui_general_keep_database_editors;

	public static String pref_page_ui_general_group_general;

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
	public static String ui_properties_tree_viewer_action_copy_value;
	public static String ui_properties_tree_viewer_action_reset_value;
	public static String ui_properties_tree_viewer_category_general;
	public static String ui_properties_value;
	public static String dialog_connection_edit_driver_button;
	public static String dialog_connection_driver;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CoreMessages.class);
	}

	private CoreMessages() {
	}
}
