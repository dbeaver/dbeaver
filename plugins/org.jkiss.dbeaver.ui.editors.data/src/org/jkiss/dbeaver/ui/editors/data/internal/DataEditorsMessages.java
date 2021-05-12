/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.data.internal;

import org.eclipse.osgi.util.NLS;

public class DataEditorsMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsResources"; //$NON-NLS-1$

	public static String grid_tooltip_sort_by_column;
	public static String grid_tooltip_filter_by_column;
    public static String controls_column_info_panel_property_key;
	public static String resultset_segment_size;
	
    // ResultSetPresentation
    public static String pref_page_database_resultsets_group_common;
    public static String pref_page_database_resultsets_label_switch_mode_on_rows;
    public static String pref_page_database_resultsets_label_show_column_description;
    public static String pref_page_database_resultsets_label_show_connection_name;
    public static String pref_page_database_resultsets_label_calc_column_width_by_values;
    public static String pref_page_database_resultsets_label_calc_column_width_by_values_tip;
    public static String pref_page_database_resultsets_label_structurize_complex_types;
    public static String pref_page_database_resultsets_label_structurize_complex_types_tip;
    public static String pref_page_database_resultsets_label_right_justify_numbers_and_date;
    public static String pref_page_database_resultsets_label_right_justify_datetime;
    public static String pref_page_database_resultsets_label_auto_completion;
    public static String pref_page_database_resultsets_label_auto_completion_tip;
    // ResultSetGrid
    public static String pref_page_database_resultsets_group_grid;
    public static String pref_page_database_resultsets_label_mark_odd_rows;
    public static String pref_page_database_resultsets_label_highlight_rows_with_selected_cells;
    public static String pref_page_database_resultsets_label_colorize_data_types;
    public static String pref_page_database_resultsets_label_row_batch_size;
    public static String pref_page_database_resultsets_label_row_batch_size_tip;
    public static String pref_page_database_resultsets_label_show_cell_icons;
    public static String pref_page_database_resultsets_label_show_attr_icons;
    public static String pref_page_database_resultsets_label_show_attr_icons_tip;
    public static String pref_page_database_resultsets_label_show_attr_filters;
    public static String pref_page_database_resultsets_label_show_attr_filters_tip;
    public static String pref_page_database_resultsets_label_show_attr_ordering;
    public static String pref_page_database_resultsets_label_show_attr_ordering_tip;
    public static String pref_page_database_resultsets_label_use_smooth_scrolling;
    public static String pref_page_database_resultsets_label_use_smooth_scrolling_tip;
    public static String pref_page_database_resultsets_label_show_boolean_as_checkbox;
    public static String pref_page_database_resultsets_label_show_boolean_as_checkbox_tip;
    public static String pref_page_database_resultsets_label_toggle_boolean_on_click;
    public static String pref_page_database_resultsets_label_toggle_boolean_on_click_tip;
    public static String pref_page_database_resultsets_label_show_boolean_config_link;
    public static String pref_page_database_resultsets_label_double_click_behavior;

    public static String pref_page_result_selector_editor;
	public static String pref_page_result_selector_inline_editor;
	public static String pref_page_result_selector_none;
    public static String pref_page_result_selector_copy_cell;
    public static String pref_page_result_selector_paste_cell_value;
    public static String pref_page_database_resultsets_label_max_def_column_width;
    public static String pref_page_database_resultsets_label_max_def_column_width_tip;
    // ResultSetPlainText
    public static String pref_page_database_resultsets_group_plain_text;
    public static String pref_page_database_resultsets_label_value_format;
    public static String pref_page_database_resultsets_label_tab_width;
    public static String pref_page_database_resultsets_label_maximum_column_length;
    public static String pref_page_database_resultsets_label_text_show_nulls;
    public static String pref_page_database_resultsets_label_text_delimiter_leading;
    public static String pref_page_database_resultsets_label_text_delimiter_trailing;
    public static String pref_page_database_resultsets_label_text_delimiter_top;
    public static String pref_page_database_resultsets_label_text_delimiter_bottom;
    public static String pref_page_database_resultsets_label_text_extra_spaces;
    
	public static String virtual_structure_editor_abstract_job_load_entity;
	public static String virtual_structure_editor_info_label_entity_structure;
	public static String virtual_structure_editor_dictionary_page_text;
	public static String virtual_structure_editor_columns_group_virtual;
	public static String virtual_structure_editor_columns_group_unique_keys;
	public static String virtual_structure_editor_table_column_key_name;
	public static String virtual_structure_editor_table_column_columns;
	public static String virtual_structure_editor_dialog_button_add;
	public static String virtual_structure_editor_constraint_page_edit_key;
	public static String virtual_structure_editor_dialog_button_edit;
	public static String virtual_structure_editor_dialog_button_remove;
	public static String virtual_structure_editor_confirm_action_delete_key;
	public static String virtual_structure_editor_confirm_action_question_delete;
	public static String virtual_structure_editor_control_group_label_foreign_key;
	public static String virtual_structure_editor_table_column_target_table;
	public static String virtual_structure_editor_table_column_datasource;
	public static String virtual_structure_editor_confirm_action_delete_fk;
	public static String virtual_structure_editor_confirm_action_question_delete_foreign;
	public static String virtual_structure_editor_control_group_references;
	public static String virtual_structure_editor_table_column_source_table;
	public static String virtual_structure_editor_table_column_source_datasource;
	public static String virtual_structure_editor_dialog_button_refresh;

    public static String complex_object_editor_dialog_menu_copy_element;
    public static String complex_object_editor_dialog_menu_add_element;
    public static String complex_object_editor_dialog_menu_remove_element;
    public static String complex_object_editor_dialog_menu_move_up_element;
    public static String complex_object_editor_dialog_menu_move_down_element;

    static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, DataEditorsMessages.class);
	}

	private DataEditorsMessages() {
	}
}
