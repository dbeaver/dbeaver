/*
 * DBeaver - Universal Database Manager
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
    public static String confirm_close_resultset_edit_toggleMessage;

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


    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ResultSetMessages.class);
    }

    private ResultSetMessages() {
    }
}
