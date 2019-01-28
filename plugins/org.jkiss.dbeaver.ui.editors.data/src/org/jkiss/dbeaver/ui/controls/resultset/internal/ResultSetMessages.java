/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.internal;

import org.eclipse.osgi.util.NLS;

public class ResultSetMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages"; //$NON-NLS-1$

    public static String confirm_order_resultset_title;
    public static String confirm_order_resultset_message;
    public static String confirm_order_resultset_toggleMessage;

    public static String confirm_fetch_all_rows_title;
    public static String confirm_fetch_all_rows_message;
    public static String confirm_fetch_all_rows_toggleMessage;

    public static String confirm_close_resultset_edit_title;
    public static String confirm_close_resultset_edit_message;

    public static String confirm_reset_panels_content_title;
    public static String confirm_reset_panels_content_message;
    public static String confirm_reset_panels_content_toggleMessage;

    public static String confirm_keep_statement_open_title;
    public static String confirm_keep_statement_open_message;
    public static String confirm_keep_statement_open_toggleMessage;

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

    public static String controls_resultset_grouping_edit;
    public static String controls_resultset_grouping_remove_column;
    public static String controls_resultset_grouping_clear;
    public static String controls_resultset_grouping_default_sorting;
    public static String controls_resultset_grouping_show_duplicates_only;

    public static String controls_resultset_viewer_action_edit;
    public static String controls_resultset_viewer_action_order_filter;
    public static String controls_resultset_viewer_action_custom_filter;
    public static String controls_resultset_viewer_action_view_format;
    public static String controls_resultset_viewer_action_view_as;
    public static String controls_resultset_viewer_action_data_formats;
    public static String controls_resultset_viewer_action_refresh;
    public static String controls_resultset_viewer_action_panels;
    public static String controls_resultset_viewer_action_options;
    public static String controls_resultset_viewer_add_new_row_context_name;
    public static String controls_resultset_viewer_dialog_status_title;
    public static String controls_resultset_check_autocommit_state;
    public static String controls_resultset_viewer_job_update;
    public static String controls_resultset_viewer_action_navigate;
    public static String controls_resultset_viewer_action_layout;
    public static String controls_resultset_viewer_monitor_aply_changes;
    public static String controls_resultset_viewer_status_inserted_;
    public static String controls_resultset_viewer_status_empty;
    public static String controls_resultset_viewer_status_no_data;
    public static String controls_resultset_viewer_status_row;
    public static String controls_resultset_viewer_status_rows;
    public static String controls_resultset_viewer_status_rows_fetched;
    public static String controls_resultset_viewer_status_rows_size;
    public static String controls_resultset_viewer_value;
    public static String controls_resultset_viewer_calculate_row_count;
    public static String controls_resultset_viewer_hide_column_x;
    public static String controls_resultset_viewer_hide_columns_x;
    public static String controls_resultset_viewer_hide_columns_error_title;
    public static String controls_resultset_viewer_hide_columnss_error_text;

    public static String controls_resultset_ref_menu_no_references;
    public static String controls_resultset_ref_menu_references;
    public static String controls_resultset_ref_menu_no_associations;
    public static String controls_resultset_ref_menu_associations;

    public static String controls_rs_pump_job_context_name;
    public static String controls_rs_pump_job_name;

    public static String controls_resultset_edit_save;
    public static String controls_resultset_edit_cancel;
    public static String controls_resultset_edit_script;
    public static String controls_resultset_config_panels;
    public static String controls_resultset_config_record;

    public static String sql_editor_resultset_filter_panel_text_enter_sql_to_filter;
    public static String sql_editor_resultset_filter_panel_text_enter_filter_not_support;
    public static String sql_editor_resultset_filter_panel_btn_apply;
    public static String sql_editor_resultset_filter_panel_btn_remove;
    public static String sql_editor_resultset_filter_panel_btn_save;
    public static String sql_editor_resultset_filter_panel_btn_custom;
    public static String sql_editor_resultset_filter_panel_label;
    public static String sql_editor_resultset_filter_panel_btn_open_console;
    public static String sql_editor_resultset_filter_panel_control_no_data;
    public static String sql_editor_resultset_filter_panel_control_execute_to_see_reslut;

    public static String actions_spreadsheet_copy_special;

    // Data managers (FIXME: model_jdbc_ is a legacy prefix)
    public static String model_jdbc_bad_content_value_;
    public static String model_jdbc_content_length;
    public static String model_jdbc_content_type;
    public static String model_jdbc_could_not_save_content;
    public static String model_jdbc_could_not_save_content_to_file_;
    public static String model_jdbc_load_from_file_;
    public static String model_jdbc_save_to_file_;
    public static String model_jdbc_set_to_current_time;
    public static String model_jdbc_unsupported_content_value_type_;

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

    public static String dialog_cursor_view_monitor_rows_fetched;

    public static String dialog_data_label_value;
    public static String dialog_text_view_open_editor;
    public static String dialog_text_view_open_editor_tip;

    // Pref pages
    public static String pref_page_content_editor_checkbox_commit_on_content_apply;
    public static String pref_page_content_editor_checkbox_commit_on_value_apply;
    public static String pref_page_content_editor_checkbox_edit_long_as_lobs;
    public static String pref_page_content_editor_group_keys;
    public static String pref_page_content_editor_checkbox_keys_always_use_all_columns;
    public static String pref_page_content_editor_checkbox_new_rows_after;
    public static String pref_page_content_editor_checkbox_refresh_after_update;
    public static String pref_page_content_editor_checkbox_use_navigator_filters;
    public static String pref_page_content_editor_checkbox_use_navigator_filters_tip;
    public static String pref_page_content_editor_group_content;
    public static String pref_page_content_editor_label_max_text_length;
    public static String pref_page_content_editor_group_hex;
    public static String pref_page_content_editor_hex_encoding;

    public static String pref_page_data_format_button_manage_profiles;
    public static String pref_page_data_format_group_settings;
    public static String pref_page_data_format_datetime_use_native_formatting;
    public static String pref_page_data_format_datetime_use_native_formatting_tip;
    public static String pref_page_data_format_group_format;
    public static String pref_page_data_format_label_profile;
    public static String pref_page_data_format_label_sample;
    public static String pref_page_data_format_label_settingt;
    public static String pref_page_data_format_label_type;

    public static String dialog_data_format_profiles_button_delete_profile;
    public static String dialog_data_format_profiles_button_new_profile;
    public static String dialog_data_format_profiles_confirm_delete_message;
    public static String dialog_data_format_profiles_confirm_delete_title;
    public static String dialog_data_format_profiles_dialog_name_chooser_title;
    public static String dialog_data_format_profiles_error_message;
    public static String dialog_data_format_profiles_error_title;
    public static String dialog_data_format_profiles_title;

    //Preference/Properties
    // ResultSetsMain
    public static String pref_page_database_resultsets_label_filter_force_subselect;
    public static String pref_page_database_resultsets_label_filter_force_subselect_tip;
    public static String pref_page_database_resultsets_group_binary;
    public static String pref_page_database_resultsets_label_binary_use_strings;
    public static String pref_page_database_resultsets_label_binary_presentation;
    public static String pref_page_database_resultsets_label_binary_editor_type;
    public static String pref_page_database_resultsets_label_binary_strings_max_length;
    public static String pref_page_database_resultsets_label_auto_fetch_segment;
    public static String pref_page_database_resultsets_label_auto_fetch_segment_tip;
    public static String pref_page_database_resultsets_label_reread_on_scrolling;
    public static String pref_page_database_resultsets_label_reread_on_scrolling_tip;
    public static String pref_page_database_resultsets_label_use_sql;
    public static String pref_page_database_resultsets_label_use_sql_tip;
    public static String pref_page_database_resultsets_label_server_side_order;
    public static String pref_page_database_resultsets_label_fetch_size;
    public static String pref_page_database_resultsets_label_read_metadata;
    public static String pref_page_database_resultsets_label_read_references;
    public static String pref_page_database_resultsets_group_string;
    public static String pref_page_database_resultsets_checkbox_string_use_editor;
    public static String pref_page_database_resultsets_checkbox_string_use_editor_tip;

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
    public static String pref_page_database_resultsets_label_right_justify_datetime;
    public static String pref_page_database_resultsets_label_row_batch_size;
    public static String pref_page_database_resultsets_label_row_batch_size_tip;
    public static String pref_page_database_resultsets_label_show_cell_icons;
    public static String pref_page_database_resultsets_label_show_attr_icons;
    public static String pref_page_database_resultsets_label_show_attr_icons_tip;
    public static String pref_page_database_resultsets_label_show_attr_filters;
    public static String pref_page_database_resultsets_label_show_attr_filters_tip;
    public static String pref_page_database_resultsets_label_show_attr_ordering;
    public static String pref_page_database_resultsets_label_show_attr_ordering_tip;
    public static String pref_page_database_resultsets_label_double_click_behavior;
    public static String pref_page_database_resultsets_group_plain_text;
    public static String pref_page_database_resultsets_label_value_format;
    public static String pref_page_database_resultsets_label_tab_width;
    public static String pref_page_database_resultsets_label_maximum_column_length;
    public static String pref_page_database_resultsets_label_text_show_nulls;
    public static String pref_page_database_resultsets_label_text_delimiter_leading;
    public static String pref_page_database_resultsets_label_text_delimiter_trailing;
    public static String pref_page_database_resultsets_label_text_extra_spaces;

    public static String pref_page_content_cache_clob;
    public static String pref_page_content_cache_blob;
    public static String pref_page_database_general_label_cache_max_size;
    public static String pref_page_database_general_checkbox_keep_cursor;
    public static String pref_page_database_general_group_queries;
    public static String pref_page_database_general_group_transactions;
    public static String pref_page_database_general_label_max_lob_length;
    public static String pref_page_database_general_label_result_set_max_size;
    public static String pref_page_database_general_label_result_set_cancel_timeout;
    public static String pref_page_database_general_label_result_set_cancel_timeout_tip;

    public static String pref_page_result_selector_editor;
	public static String pref_page_result_selector_inline_editor;
	public static String pref_page_result_selector_none;

	public static String pref_page_results_group_advanced;
    public static String pref_page_sql_editor_group_misc;

	public static String value_format_selector_database_native;
	public static String value_format_selector_display;
	public static String value_format_selector_editable;
	public static String value_format_selector_value;
    
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ResultSetMessages.class);
    }

    private ResultSetMessages() {
    }
}
