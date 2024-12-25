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
package org.jkiss.dbeaver.ui.search.internal;

import org.jkiss.dbeaver.utils.NLS;

public class UISearchMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.search.internal.UISearchMessages"; //$NON-NLS-1$

    public static String dialog_search_objects_button_close;
    public static String dialog_search_objects_button_search;
    public static String dialog_search_objects_column_description;
    public static String dialog_search_objects_column_type;
    public static String dialog_search_objects_combo_contains;
    public static String dialog_search_objects_combo_like;
    public static String dialog_search_objects_combo_starts_with;
    public static String dialog_search_objects_group_object_types;
    public static String dialog_search_objects_group_objects_source;
    public static String dialog_search_objects_group_settings;
    public static String dialog_search_objects_item_list_info;
    public static String dialog_search_objects_label_match_type;
    public static String dialog_search_objects_label_object_name;
    public static String dialog_search_objects_message_no_objects_like_;
    public static String dialog_search_objects_message_objects_found;
    public static String dialog_search_objects_spinner_max_results;
    public static String dialog_search_objects_case_sensitive;
    public static String dialog_search_objects_search_in_comments;
    public static String dialog_search_objects_search_in_definitions;
    public static String dialog_search_objects_title;

    public static String dialog_data_search_hint_text_string_to_search;
    public static String dialog_data_search_control_group_databases;
    public static String dialog_data_search_control_group_settings;
    public static String dialog_data_search_spinner_max_results;
    public static String dialog_data_search_spinner_max_results_tip;
    public static String dialog_data_search_checkbox_case_sensitive_tip;
    public static String dialog_data_search_checkbox_fast_search;
    public static String dialog_data_search_checkbox_fast_search_tip;
    public static String dialog_data_search_checkbox_search_in_numbers;
    public static String dialog_data_search_checkbox_search_in_numbers_tip;
    public static String dialog_data_search_checkbox_search_in_lob;
    public static String dialog_data_search_checkbox_search_in_lob_tip;
    public static String dialog_data_search_checkbox_search_in_foreign_objects;
    public static String dialog_data_search_checkbox_search_in_foreign_objects_tip;
    public static String dialog_data_search_info_label_use_ctrl;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UISearchMessages.class);
    }

    private UISearchMessages() {
    }
}
