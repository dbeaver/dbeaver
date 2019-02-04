/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.internal;

import org.eclipse.osgi.util.NLS;

public class SQLEditorMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages"; //$NON-NLS-1$

    public static String confirm_close_running_query_title;
    public static String confirm_close_running_query_message;
    public static String confirm_close_running_query_toggleMessage;

    public static String confirm_dangerous_sql_title;
    public static String confirm_dangerous_sql_message;
    public static String confirm_dangerous_sql_toggleMessage;

    public static String confirm_mass_parallel_sql_title;
    public static String confirm_mass_parallel_sql_message;
    public static String confirm_mass_parallel_sql_toggleMessage;

    public static String dialog_view_sql_button_copy;
    public static String dialog_view_sql_button_persist;

    public static String dialog_sql_param_title;
    public static String dialog_sql_param_column_name;
    public static String dialog_sql_param_column_value;
    public static String dialog_sql_param_hide_checkbox;
    public static String dialog_sql_param_hide_checkbox_tip;
    public static String dialog_sql_param_hint;

    public static String editor_query_log_viewer_draw_text_type_qury_part;

	public static String editor_sql_preference;
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
    public static String editors_sql_status_rows_updated;
    public static String editors_sql_status_statement_executed;
    public static String editors_sql_status_statement_executed_no_rows_updated;
    public static String editors_sql_staus_connected_to;
    public static String editors_sql_actions_copy_as_source_code;
    public static String editors_sql_actions_copy_as_source_code_tip;

    public static String action_menu_sqleditor_maximizeResultsPanel;
    public static String action_menu_sqleditor_restoreResultsPanel;

    public static String script_close_behavior_delete_always;
	public static String script_close_behavior_delete_only_new_scripts;
	public static String script_close_behavior_do_not_delete;
	public static String sql_editor_menu_format;

    public static String action_popup_sqleditor_layout_horizontal;
    public static String action_popup_sqleditor_layout_vertical;
    public static String action_popup_sqleditor_layout_detached;

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
    public static String pref_page_sql_editor_enable_parameters_in_ddl_tip;
    public static String pref_page_sql_editor_enable_variables;
    public static String pref_page_sql_editor_enable_variables_tip;
    // SQProposalsSearch
    public static String pref_page_sql_format_group_search;
    public static String pref_page_sql_completion_label_match_contains;
    public static String pref_page_sql_completion_label_match_contains_tip;
    public static String pref_page_sql_completion_label_use_global_search;
    public static String pref_page_sql_completion_label_use_global_search_tip;
    public static String pref_page_sql_completion_label_show_column_procedures;
    public static String pref_page_sql_completion_label_show_column_procedures_tip;
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
    public static String pref_page_sql_format_group_style;
    public static String pref_page_sql_format_label_bold_keywords;
    public static String pref_page_sql_format_label_bold_keywords_tip;
    public static String pref_page_sql_format_group_formatter;
    public static String pref_page_sql_format_label_formatter;
    public static String pref_page_sql_format_label_add_line_feed_before_close_bracket;
    public static String pref_page_sql_format_label_keyword_case;
    public static String pref_page_sql_format_label_external_command_line;
    public static String pref_page_sql_format_label_external_set_content_tool_tip;
    public static String pref_page_sql_format_label_external_use_temp_file;
    public static String pref_page_sql_format_label_external_use_temp_file_tip;
    public static String pref_page_sql_format_label_external_exec_timeout;
    public static String pref_page_sql_format_label_external_exec_timeout_tip;
    public static String pref_page_sql_format_label_indent_size;
    public static String pref_page_sql_format_label_insert_spaces_for_tabs;
    public static String pref_page_sql_format_label_insert_line_feed_before_commas;
    public static String pref_page_sql_format_label_settings;
    public static String  pref_page_sql_format_label_SQLPreview;
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
    public static String pref_page_sql_completion_label_replace_word_after;
    public static String pref_page_sql_completion_label_replace_word_after_tip;
    public static String pref_page_sql_completion_label_hide_duplicate_names;
    public static String pref_page_sql_completion_label_use_short_names;
    public static String pref_page_sql_completion_label_use_long_names;
    public static String pref_page_sql_completion_label_insert_space;
    public static String pref_page_sql_completion_label_sort_alphabetically;
    public static String pref_page_sql_completion_label_show_server_help_topics;
    public static String pref_page_sql_completion_label_show_server_help_topics_tip;
    public static String pref_page_sql_completion_group_folding;
    public static String pref_page_sql_completion_group_misc;
    public static String pref_page_sql_completion_label_folding_enabled;
    public static String pref_page_sql_completion_label_folding_enabled_tip;
    public static String pref_page_sql_completion_label_mark_occurrences;
    public static String pref_page_sql_completion_label_mark_occurrences_tip;
    public static String pref_page_sql_completion_label_mark_occurrences_for_selections;
    public static String pref_page_sql_completion_label_mark_occurrences_for_selections_tip;

    public static String pref_page_sql_editor_checkbox_fetch_resultsets;
    public static String pref_page_sql_editor_text_statement_delimiter;
    public static String pref_page_sql_editor_checkbox_ignore_native_delimiter;
    public static String pref_page_sql_editor_checkbox_remove_trailing_delimiter;
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
    public static String pref_page_sql_editor_group_connection_association;
    public static String pref_page_sql_editor_group_resources;
    public static String pref_page_sql_editor_checkbox_bind_connection_hint;
    public static String pref_page_sql_editor_checkbox_bind_embedded_read;
    public static String pref_page_sql_editor_checkbox_bind_embedded_read_tip;
    public static String pref_page_sql_editor_checkbox_bind_embedded_write;
    public static String pref_page_sql_editor_checkbox_bind_embedded_write_tip;
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
    public static String pref_page_sql_editor_label_refresh_defaults_after_execute_tip;
    public static String pref_page_sql_editor_label_clear_output_before_execute;
    public static String pref_page_sql_editor_label_clear_output_before_execute_tip;

	public static String pref_page_sql_insert_case_default;
	public static String pref_page_sql_insert_case_lower_case;
	public static String pref_page_sql_insert_case_upper_case;

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
    public static String sql_editor_menu_choose_format;
    public static String  sql_editor_panel_format;
    
    public static String sql_script_binding_type_radio_button_connection_name;
	public static String sql_script_binding_type_radio_button_connection_parameters;
	public static String sql_script_binding_type_radio_button_connection_unique;
	public static String sql_script_binding_type_radio_button_connection_url;

	static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SQLEditorMessages.class);
    }

    private SQLEditorMessages() {
    }
}
