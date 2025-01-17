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

package org.jkiss.dbeaver.erd.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class ERDUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ERDUIMessages.class);
    }

    public static String action_diagram_layout_name;
    public static String column_;
    public static String entity_diagram_;
    public static String part_note_title;
    public static String pref_page_erd_checkbox_grid_enabled;
    public static String pref_page_erd_checkbox_snap_to_grid;
    public static String pref_page_erd_combo_page_mode;
    public static String pref_page_erd_group_grid;
    public static String pref_page_erd_group_print;
    public static String pref_page_erd_item_fit_height;
    public static String pref_page_erd_item_fit_page;
    public static String pref_page_erd_item_fit_width;
    public static String pref_page_erd_item_tile;
    public static String pref_page_erd_spinner_grid_height;
    public static String pref_page_erd_spinner_grid_width;
    public static String pref_page_erd_spinner_margin_bottom;
    public static String pref_page_erd_spinner_margin_left;
    public static String pref_page_erd_spinner_margin_right;
    public static String pref_page_erd_spinner_margin_top;

    public static String wizard_diagram_create_title;

    public static String wizard_page_diagram_create_description;
    public static String wizard_page_diagram_create_group_settings;
    public static String wizard_page_diagram_create_label_init_content;
    public static String wizard_page_diagram_create_name;
    public static String wizard_page_diagram_create_title;

    //ERD editor action item control
    public static String erd_editor_control_action_toggle_grid;
    public static String erd_editor_control_action_refresh_diagram;
    public static String erd_editor_control_action_print_diagram;
    public static String erd_editor_control_action_configuration;
    public static String erd_editor_control_edit_mode_label;
    //ERD editor action item control

    public static String erd_preference_page_title_advanced;
    public static String erd_preference_page_title_attribute_style;
    public static String erd_preference_page_title_attributes_visibility;
    public static String erd_preference_page_title_diagram_contents;
    public static String erd_preference_page_title_shows_views;
    public static String erd_preference_page_title_shows_partitions;
    public static String erd_preference_page_title_routing_combo;
    public static String erd_preference_page_title_notation_combo;
    public static String erd_preference_page_title_color_pref;
    public static String erd_preference_page_title_change_border_colors;
    public static String erd_preference_page_title_change_header_colors;
    public static String erd_tool_color_action_text_set_color;
    public static String erd_tool_color_action_tip_text_set_figure_color;
    public static String erd_tool_color_action_text_reset_color;
    public static String erd_tool_color_action_tip_text_reset_figure_color;
    public static String erd_tool_create_connection;
    public static String erd_tool_create_connection_tip;
    public static String erd_tool_create_note;
    public static String erd_tool_create_note_tip;
    public static String erd_tool_create_default;
    public static String erd_tool_set_text_text_bring_to_front;
    public static String erd_tool_set_text_text_send_to_back;
    public static String erd_tool_set_text_tip_text_bring_to_front;
    public static String erd_tool_set_text_tip_text_send_to_back;

    public static String erd_view_style_selection_item_comments;
    public static String erd_view_style_selection_item_comments_action;
    public static String erd_view_style_selection_item_data_types;
    public static String erd_view_style_selection_item_data_types_action;
    public static String erd_view_style_selection_item_fully_qualified_names;
    public static String erd_view_style_selection_item_fully_qualified_names_action;
    public static String erd_view_style_selection_item_icons;
    public static String erd_view_style_selection_item_icons_action;
    public static String erd_view_style_selection_item_nullability;
    public static String erd_view_style_selection_item_nullability_action;
    public static String erd_view_style_selection_item_alphabetical_order;
    public static String erd_view_style_selection_item_alphabetical_order_action;
    public static String menu_view_style;
    public static String menu_notation_style;
    public static String menu_router_style;
    public static String menu_attribute_visibility;
    public static String menu_attribute_visibility_default;
    public static String menu_attribute_visibility_entity;

    public static String erd_settings_action_customize;
    public static String erd_settings_action_customize_tip;
    public static String erd_settings_dialog_text_title;
    public static String erd_settings_dialog_group_label;
    public static String erd_settings_checkbox_transparent_label;
    public static String erd_settings_checkbox_transparent_tip;
    public static String erd_settings_color_picker_background_label;
    public static String erd_settings_color_picker_foreground_label;
    public static String erd_settings_border_width_label;
    public static String erd_settings_font_label;
    public static String erd_settings_button_change_font_label;
    public static String erd_settings_font_preview_text;

    public static String erd_action_delete_text;
    public static String erd_action_remove_text;
    public static String erd_action_diagram_toggle_persist_text;
    public static String erd_action_diagram_toggle_persist_description;
    public static String erd_action_diagram_toggle_persist_confirmation_title;
    public static String erd_action_diagram_toggle_persist_confirmation_description;
    public static String erd_action_diagram_toggle_hand_checkbox_text;

    public static String erd_navigator_entity_diagram_name;

    public static String erd_entity_add_command_select_table_dialog;

    public static String erd_accessibility_note_part;
    public static String erd_accessibility_entity_part;
    public static String erd_accessibility_attribute_part;

    public static String erd_accessibility_association_part_logical;
    public static String erd_accessibility_association_part;
    public static String erd_accessibility_association_part_attribute;

    public static String erd_accessibility_attribute_part_type;
    public static String erd_accessibility_attribute_part_nullability;
    public static String erd_accessibility_attribute_part_comments;
    public static String erd_error_of_loading_diagram_label;
    public static String erd_error_of_loading_diagram_title;

    public static String erd_rearrange_diagram_job_title;
    public static String erd_job_set_diagram_palette;
    public static String erd_job_rearrange_diagram;
    public static String erd_job_reset_element_position;
    public static String erd_job_repaint_diagram;
    public static String erd_job_layout_diagram;
    public static String erd_job_visuallize_content;
    

    private ERDUIMessages() {
    }
}
