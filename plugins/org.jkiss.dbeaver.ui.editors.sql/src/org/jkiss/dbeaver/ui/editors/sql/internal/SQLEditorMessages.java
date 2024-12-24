/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.utils.NLS;

public class SQLEditorMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages"; //$NON-NLS-1$

    public static String dialog_view_sql_button_copy;
    public static String dialog_view_sql_button_refresh;
    public static String dialog_view_sql_button_execute;

    public static String dialog_sql_param_title;
    public static String dialog_sql_param_column_name;
    public static String dialog_sql_param_column_value;
    public static String dialog_sql_param_hide_checkbox;
    public static String dialog_sql_param_hide_checkbox_tip;
    public static String dialog_sql_param_hint;

    public static String editor_file_delete_error_title;
    public static String editor_file_delete_error_text;
    public static String editor_file_delete_confirm_delete_text;
    public static String editor_file_delete_confirm_delete_title;

    public static String editor_query_log_viewer_draw_text_type_qury_part;
    public static String editor_query_log_viewer_reexecute_query_button_text;
    
    public static String editor_sql_preference;
    public static String editors_sql_data_grid;
    public static String editors_sql_description;
    public static String editors_sql_editor_presentation;
    public static String editors_sql_error_cant_execute_query_message;
    public static String editors_sql_error_cant_execute_query_title;
    public static String editors_sql_error_cant_execute_permissions_query_message;
    public static String editors_sql_error_cant_execute_readonly_query_message;
    public static String editors_sql_error_cant_obtain_session;
    public static String editors_sql_error_execution_plan_message;
    public static String editors_sql_error_execution_plan_title;
    public static String editors_sql_execution_log;
    public static String editors_sql_execution_log_tip;
    public static String editors_sql_variables;
    public static String editors_sql_variables_tip;
    public static String editors_sql_explain_plan;
    public static String editors_sql_explain_refresh_plan_action_text;
    public static String editors_sql_explain_refresh_tree_viewer_plan_toggle_view_text;
    public static String editors_sql_output;
    public static String editors_sql_output_tip;
    public static String editors_sql_warning_many_subtables_title;
    public static String editors_sql_warning_many_subtables_text;
    public static String editors_sql_statistics;
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
    
    public static String editors_sql_actions_search_selected_text_online;
    public static String editors_sql_actions_search_selected_text_online_tip;

    public static String action_menu_sqleditor_maximizeResultsPanel;
    public static String action_menu_sqleditor_restoreResultsPanel;

    public static String script_close_behavior_delete_always;
	public static String script_close_behavior_delete_only_new_scripts;
	public static String script_close_behavior_do_not_delete;
	public static String sql_editor_menu_format;
    public static String sql_editor_error_position;

    public static String action_result_tabs_close_all_tabs;
    public static String action_result_tabs_close_query_tabs;
    public static String action_result_tabs_close_other_tabs;
    public static String action_result_tabs_close_tabs_to_the_left;
    public static String action_result_tabs_close_tabs_to_the_right;
    public static String action_result_tabs_pin_tab;
    public static String action_result_tabs_unpin_tab;
    public static String action_result_tabs_unpin_all_tabs;
    public static String action_result_tabs_detach_tab;
    public static String action_result_tabs_set_name;
    public static String action_result_tabs_set_name_title;
    public static String action_result_tabs_assign_variable;
    public static String action_result_tabs_assign_variable_sql;
    public static String action_result_tabs_delete_variables;
    public static String action_assign_variables_error_duplicated_title;
    public static String action_assign_variables_error_duplicated_info;
    public static String action_assign_variables_error_invalid_title;
    public static String action_assign_variables_error_invalid_info;

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
    public static String pref_page_sql_editor_label_separate_connection_each_editor_tip;
    public static String pref_page_sql_editor_label_connect_on_editor_activation;
    public static String pref_page_sql_editor_label_connect_on_query_execute;

    public static String pref_page_sql_editor_label_auto_save_on_change;
    public static String pref_page_sql_editor_label_auto_save_on_change_tip;
    public static String pref_page_sql_editor_group_auto_save;
    public static String pref_page_sql_editor_label_auto_save_on_close;
    public static String pref_page_sql_editor_label_save_on_query_execute;
    public static String pref_page_sql_editor_label_save_active_schema;
    public static String pref_page_sql_editor_label_save_active_schema_tip;
    public static String pref_page_sql_editor_group_result_view;
    public static String pref_page_sql_editor_label_close_results_tab_on_error;
    public static String pref_page_sql_editor_label_close_results_tab_on_error_tip;
    public static String pref_page_sql_editor_label_auto_open_output_view;
    public static String pref_page_sql_editor_label_auto_open_output_view_tip;
    public static String pref_page_sql_editor_label_size_warning_threshold;
    public static String pref_page_sql_editor_label_size_warning_threshold_tip;
    public static String pref_page_sql_editor_label_replace_on_single_query_exec_view;
    public static String pref_page_sql_editor_label_replace_on_single_query_exec_view_tip;
    public static String pref_page_sql_editor_label_results_orientation;
    public static String pref_page_sql_editor_label_results_orientation_tip;
    public static String pref_page_sql_editor_link_text_editor;
    public static String pref_page_sql_editor_link_colors_and_fonts;
    public static String pref_page_sql_editor_new_script_template_group;
    public static String pref_page_sql_editor_new_script_template_enable_checkbox;
    public static String pref_page_sql_editor_new_script_template_variables;
    public static String pref_page_sql_editor_new_script_template_variables_tip;
    public static String pref_page_sql_editor_new_script_template_template;
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
    public static String pref_page_sql_completion_label_activate_hippie;
    public static String pref_page_sql_completion_label_activate_hippie_tip;
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
    public static String pref_page_sql_format_label_insert_delimiters_in_empty_lines;
    public static String pref_page_sql_format_tip_insert_delimiters_in_empty_lines;
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
    public static String  pref_page_sql_format_label_format_active_query;
    public static String  pref_page_sql_format_label_format_active_query_tip;
    // SQLCompletion
    public static String pref_page_sql_completion_group_sql_assistant;
    public static String pref_page_sql_completion_label_enable_auto_activation;
    public static String pref_page_sql_completion_label_enable_auto_activation_tip;
    public static String pref_page_sql_completion_label_enable_experimental_features;
    public static String pref_page_sql_completion_label_enable_experimental_features_tip;
    public static String pref_page_sql_completion_label_completion_mode;
    public static String pref_page_sql_completion_label_completion_mode_default;
    public static String pref_page_sql_completion_label_completion_mode_new_engine;
    public static String pref_page_sql_completion_label_completion_mode_combined;
    public static String pref_page_sql_completion_label_auto_activation_delay;
    public static String pref_page_sql_completion_label_set_auto_activation_delay_tip;
    public static String pref_page_sql_completion_label_activate_on_typing;
    public static String pref_page_sql_completion_label_activate_on_typing_tip;
    public static String pref_page_sql_completion_label_auto_insert_proposal;
    public static String pref_page_sql_completion_label_auto_insert_proposal_tip;
    public static String pref_page_sql_completion_label_autocomplete_by_tab;
    public static String pref_page_sql_completion_label_autocomplete_by_tab_tip;
    public static String pref_page_sql_completion_label_insert_case;
    public static String pref_page_sql_completion_label_replace_word_after;
    public static String pref_page_sql_completion_label_replace_word_after_tip;
    public static String pref_page_sql_completion_label_hide_duplicate_names;
    public static String pref_page_sql_completion_label_object_names_form;
    public static String pref_page_sql_completion_label_use_short_names;
    public static String pref_page_sql_completion_label_use_long_names;
    public static String pref_page_sql_completion_label_insert_space;
    public static String pref_page_sql_completion_label_sort_alphabetically;
    public static String pref_page_sql_completion_label_insert_table_alias;
    public static String pref_page_sql_completion_label_show_server_help_topics;
    public static String pref_page_sql_completion_label_show_server_help_topics_tip;
    public static String pref_page_sql_completion_label_show_values;
    public static String pref_page_sql_completion_label_show_values_tip;
    public static String pref_page_sql_completion_group_folding;
    public static String pref_page_sql_completion_group_misc;
    public static String pref_page_code_editor_group_analysis;
    public static String pref_page_sql_completion_label_folding_enabled;
    public static String pref_page_sql_completion_label_folding_enabled_tip;
    public static String pref_page_sql_completion_label_smart_word_iterator;
    public static String pref_page_sql_completion_label_smart_word_iterator_tip;
    public static String pref_page_sql_completion_label_problem_markers_enabled;
    public static String pref_page_sql_completion_label_problem_markers_enabled_tip;
    public static String pref_page_sql_completion_label_mark_occurrences;
    public static String pref_page_sql_completion_label_mark_occurrences_tip;
    public static String pref_page_sql_completion_label_mark_occurrences_for_selections;
    public static String pref_page_sql_completion_label_mark_occurrences_for_selections_tip;
    public static String pref_page_code_editor_label_advanced_highlighting_enabled;
    public static String pref_page_code_editor_label_advanced_highlighting_enabled_tip;
    public static String pref_page_code_editor_label_read_metadata_enabled;
    public static String pref_page_code_editor_label_read_metadata_enabled_tip;

    public static String pref_page_sql_editor_checkbox_fetch_resultsets;
    public static String pref_page_sql_editor_text_statement_delimiter;
    public static String pref_page_sql_editor_checkbox_ignore_native_delimiter;
    public static String pref_page_sql_editor_checkbox_ignore_native_delimiter_tip;
    public static String pref_page_sql_editor_checkbox_remove_trailing_delimiter;
    public static String pref_page_sql_editor_checkbox_remove_trailing_delimiter_tip;
    public static String pref_page_sql_editor_checkbox_blank_line_delimiter;
    public static String pref_page_sql_editor_checkbox_blank_line_delimiter_tip;
    public static String pref_page_sql_editor_checkbox_enable_sql_parameters;
    public static String pref_page_sql_editor_title_pattern;
    public static String pref_page_sql_editor_file_name_pattern;
    public static String pref_page_sql_editor_file_name_pattern_tip;
    public static String pref_page_sql_editor_checkbox_delete_empty_scripts;
    public static String pref_page_sql_editor_checkbox_put_new_scripts;
    public static String pref_page_sql_editor_checkbox_put_new_scripts_tip;
    public static String pref_page_sql_editor_checkbox_create_script_folders;
    public static String pref_page_sql_editor_checkbox_create_script_folders_tip;
    public static String pref_page_sql_editor_checkbox_reset_cursor;
    public static String pref_page_sql_editor_checkbox_max_editor_on_script_exec;
    public static String pref_page_sql_editor_checkbox_show_statistics_for_queries_with_results;
    public static String pref_page_sql_editor_checkbox_show_statistics_for_queries_with_results_tip;
    public static String pref_page_sql_editor_checkbox_select_statistics_tab;
    public static String pref_page_sql_editor_checkbox_select_statistics_tab_tip;
    public static String pref_page_sql_editor_checkbox_close_included_script_after_execution;
    public static String pref_page_sql_editor_checkbox_close_included_script_after_execution_tip;
    public static String pref_page_sql_editor_checkbox_enable_sql_anonymous_parameters;
    public static String pref_page_sql_editor_text_anonymous_parameter_mark;
    public static String pref_page_sql_editor_text_named_parameter_prefix;
    public static String pref_page_sql_editor_text_control_command_prefix;
    public static String pref_page_sql_editor_text_explanation_link;
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

    public static String pref_page_sql_default;
    public static String pref_page_sql_insert_case_lower_case;
    public static String pref_page_sql_insert_case_upper_case;

    public static String sql_editor_menu_choose_format;
    public static String sql_editor_panel_format;
    public static String sql_editor_result_set_orientation_detached;
    public static String sql_editor_result_set_orientation_detached_tip;
    public static String sql_editor_result_set_orientation_horizontal;
    public static String sql_editor_result_set_orientation_horizontal_tip;
    public static String sql_editor_result_set_orientation_vertical;
    public static String sql_editor_result_set_orientation_vertical_tip;
    public static String sql_script_binding_type_radio_button_connection_name;
    public static String sql_script_binding_type_radio_button_connection_parameters;
    public static String sql_script_binding_type_radio_button_connection_unique;
    public static String sql_script_binding_type_radio_button_connection_url;

    public static String sql_generator_dialog_button_use_fully_names;
    public static String sql_generator_dialog_button_compact_sql;
    public static String sql_generator_dialog_button_exclude_columns;
    public static String sql_generator_dialog_button_use_custom_data_format;
    public static String sql_generator_dialog_button_show_comments;
    public static String sql_generator_dialog_button_show_permissions;
    public static String sql_generator_dialog_button_show_full_DDL;
    public static String sql_generator_dialog_button_separate_fk_constraints_definition;
    public static String sql_generator_dialog_button_show_partitions_DDL;

    public static String sql_generator_dialog_button_show_cast_params;
    public static String sql_generator_dialog_button_show_cast_params_tip;
    public static String action_result_tabs_delete_variables_question;
    public static String script_selector_create_script;
    public static String script_selector_project_scripts;
    public static String script_selector_project_table_name_label;
    public static String script_selector_project_table_name_description;
    public static String script_selector_project_table_time_label;
    public static String script_selector_project_table_time_description;
    public static String script_selector_project_table_info_label;
    public static String script_selector_project_table_info_description;
    public static String script_selector_project_table_folder_description;
    public static String script_selector_project_table_folder_label;

    public static String source_viewer_open_in_sql_console;
    public static String source_viewer_show_ddl_text;
    public static String source_viewer_show_ddl_tip;
    public static String source_viewer_show_permissions_text;
    public static String source_viewer_show_permissions_tip;
    public static String source_viewer_separate_fk_text;
    public static String source_viewer_separate_fk_tip;
    public static String source_viewer_show_comments_text;
    public static String source_viewer_show_comments_tip;
    public static String source_viewer_show_partitions_ddl_text;
    public static String source_viewer_show_partitions_ddl_tip;

    public static String sql_editor_action_clear;
    
    public static String sql_editor_title_tooltip_path;
    public static String sql_editor_data_receiver_result_name_tooltip_connection;
    public static String sql_editor_data_receiver_result_name_tooltip_time;
    public static String sql_editor_data_receiver_result_name_tooltip_query;
    public static String sql_editor_title_tooltip_connecton;
    public static String sql_editor_title_tooltip_type;
    public static String sql_editor_title_tooltip_url;
    public static String sql_editor_title_tooltip_database;
    public static String sql_editor_title_tooltip_schema;

    public static String sql_generator_title_text;
    public static String sql_generator_no_obj_container_text;
    public static String sql_generator_no_ddl_text;
    public static String sql_generator_nonsql_text;

    public static String sql_editor_separate_connection_no_editor_or_ds_selected;
    public static String sql_editor_prefs_save_as_script_text;
    public static String sql_editor_prefs_save_as_script_tip;
    public static String sql_editor_prefs_disable_services_text;
    public static String sql_editor_prefs_disable_services_tip;
    public static String sql_editor_prefs_script_advanced_settings;
    public static String sql_editor_prefs_script_disable_sql_syntax_parsing_for_scripts_bigger_than;
    public static String sql_editor_confirm_no_fetch_result_for_big_script_title;
    public static String sql_editor_confirm_no_fetch_result_for_big_script_question;
    public static String sql_editor_confirm_no_fetch_result_for_big_script_yes;
    public static String sql_editor_confirm_no_fetch_result_for_big_script_no;
    public static String sql_editor_confirm_no_fetch_result_for_big_script_remember;

    public static String dialog_save_script_title;
    public static String dialog_save_script_message;

    public static String sql_editor_panel_output_filter_message;
    public static String sql_editor_panel_output_filter_hint;
    public static String sql_generator_dialog_title;

    public static String sql_editor_status_bar_rollback_label;
    public static String sql_editor_status_bar_disconnect_label;
    
    public static String sql_editor_outline_no_elements_label;
    public static String sql_editor_outline_query_analysis_disabled_label;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SQLEditorMessages.class);
    }

    private SQLEditorMessages() {
    }
}
