/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.compare.simple.ui.internal;

import org.jkiss.dbeaver.utils.NLS;

public class CompareUIMessages extends NLS {

    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.tools.compare.simple.ui.internal.CompareUIMessages"; //$NON-NLS-1$

    public static String compare_objects_page_settings_page;
    public static String compare_objects_page_settings_title;
    public static String compare_objects_page_settings_description;
    public static String compare_objects_page_settings_configuration_output_report;
    public static String compare_objects_page_settings_configuration_output;
    public static String compare_objects_page_settings_configuration_output_type;
    public static String compare_objects_page_settings_group_objects;
    public static String compare_objects_page_settings_nodes_column_name;
    public static String compare_objects_page_settings_nodes_column_type;
    public static String compare_objects_page_settings_nodes_column_full_name;
    public static String compare_objects_page_settings_group_settings;
    public static String compare_objects_page_settings_checkbox_skip_objects;
    public static String compare_objects_page_settings_checkbox_compare_properties;
    public static String compare_objects_page_settings_checkbox_compare_structure;
    public static String compare_objects_page_settings_checkbox_scripts;
    public static String compare_objects_page_checkbox_show_only_differences;
    public static String compare_objects_page_report_settings;

    public static String compare_objects_wizard_title;
    public static String compare_objects_wizard_error_title;
    public static String compare_objects_wizard_finish_report_title;
    public static String compare_objects_wizard_finish_report_info;

    public static String compare_objects_wizard_dialog_button_compare;
    public static String compare_objects_error_just_one_object_selected_title;
    public static String compare_objects_error_just_one_object_selected_message;
    public static String compare_objects_error_different_object_types_title;
    public static String compare_objects_error_different_object_types_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, CompareUIMessages.class);
    }

    public CompareUIMessages() {
    }
}
