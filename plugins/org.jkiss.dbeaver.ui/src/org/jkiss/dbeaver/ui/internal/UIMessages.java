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
package org.jkiss.dbeaver.ui.internal;

import org.eclipse.osgi.util.NLS;

public class UIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.internal.UIMessages"; //$NON-NLS-1$

    public static String output_label_directory;

    public static String properties_name;
    public static String properties_value;

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

    public static String controls_locale_selector_group_locale;
    public static String controls_locale_selector_label_country;
    public static String controls_locale_selector_label_language;
    public static String controls_locale_selector_label_locale;
    public static String controls_locale_selector_label_variant;

    public static String ui_properties_name;
    public static String ui_properties_value;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UIMessages.class);
    }

    private UIMessages() {
    }
}
