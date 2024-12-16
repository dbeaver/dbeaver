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
package org.jkiss.dbeaver.ui.internal;

import org.eclipse.osgi.util.NLS;

public class UIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.internal.UIMessages"; //$NON-NLS-1$

    public static String output_label_directory;

    public static String pref_page_connections_tool_tip_text_allowed_variables;

    public static String properties_name;
    public static String properties_value;

    public static String label_ms;
    public static String label_sec;

    public static String ui_actions_context_search_name;

    public static String ui_properties_tree_viewer__to_default;
    public static String ui_properties_tree_viewer_action_copy_name;
    public static String ui_properties_tree_viewer_action_copy_value;
    public static String ui_properties_tree_viewer_action_reset_value;
    public static String ui_properties_tree_viewer_category_general;

    public static String controls_progress_page_action_close;
    public static String controls_progress_page_job_search;
    public static String controls_progress_page_progress_bar_cancel_tooltip;
    public static String controls_progress_page_progress_bar_loading_tooltip;
    public static String controls_progress_page_toolbar_title;

    public static String sql_editor_resultset_filter_panel_btn_config_refresh;
    public static String sql_editor_resultset_filter_panel_btn_stop_refresh;
    public static String sql_editor_resultset_filter_panel_menu_refresh_interval;
    public static String sql_editor_resultset_filter_panel_menu_refresh_interval_1;
    public static String sql_editor_resultset_filter_panel_menu_stop;
    public static String sql_editor_resultset_filter_panel_menu_customize;

    public static String button_add;
    public static String button_enable;
    public static String button_remove;
    public static String button_clear;
    public static String button_reset_to_defaults;
    public static String button_start;
    public static String button_skip_all;
    public static String browse_button_choose;
    public static String browse_button_choose_tooltip;
    public static String bad_container_node;
    public static String bad_container_node_message;

    public static String controls_locale_selector_group_locale;
    public static String controls_locale_selector_label_country;
    public static String controls_locale_selector_label_language;
    public static String controls_locale_selector_label_locale;
    public static String controls_locale_selector_label_variant;

    public static String edit_text_dialog_title_edit_value;

    public static String dialogs_name_and_password_dialog_group_settings;
    public static String dialogs_name_and_password_dialog_label_name;
    public static String dialogs_name_and_password_dialog_label_password;
    public static String dialogs_name_and_password_dialog_label_verify_password;

    public static String ui_properties_name;
    public static String ui_properties_value;
    
    public static String save_execution_plan;
    public static String load_execution_plan;

    public static String control_label_connection_folder;

    public static String control_boolean_mode_text;
    public static String control_boolean_mode_text_tip;
    public static String control_boolean_mode_icon;
    public static String control_boolean_mode_icon_tip;
    public static String control_boolean_state_checked;
    public static String control_boolean_state_unchecked;
    public static String control_boolean_state_null;
    public static String control_default_color_selector_reset_default_tip;

    public static String control_alignment_left;
    public static String control_alignment_center;
    public static String control_alignment_right;
    public static String control_font_normal;
    public static String control_font_italic;
    public static String control_font_bold;

    public static String tooltip_restore;
    public static String tooltip_hide;

    public static String utils_actions_copy_label;
    public static String utils_actions_copy_all_label;
    public static String text_with_open_dialog_set_text;
    public static String text_with_open_dialog_edit_text;
    public static String text_with_open_dialog_browse;
    public static String text_with_open_dialog_browse_remote;
    public static String text_with_open_dialog_edit_file;

    public static String preference_page_no_access;

    public static String notification_popup_context_message;

    public static String link_external_label;
    public static String link_external_tip;
    public static String label_catalog_schema;
    public static String label_choose;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UIMessages.class);
    }

    private UIMessages() {
    }
}
