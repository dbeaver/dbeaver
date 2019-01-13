/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata;

import org.eclipse.osgi.util.NLS;

public class MockDataMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mockdata.MockDataResources"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, MockDataMessages.class);
    }

    public MockDataMessages() {
    }

    public static String tools_mockdata_message_title;
    public static String tools_mockdata_error_notconnected;
    public static String tools_mockdata_error_tableonly;

    public static String tools_mockdata_prop_nulls_label;
    public static String tools_mockdata_prop_nulls_description;
    public static String tools_mockdata_prop_lowercase_label;
    public static String tools_mockdata_prop_uppercase_label;

    public static String tools_mockdata_generator_boolean_sequence_prop_order_value_constant;
    public static String tools_mockdata_generator_boolean_sequence_prop_order_value_alternately;


    public static String tools_mockdata_wizard_title;
    public static String tools_mockdata_wizard_page_name;
    public static String tools_mockdata_wizard_message_process_completed;

    public static String tools_mockdata_wizard_task_generate_data;
    public static String tools_mockdata_wizard_task_insert_data;

    public static String tools_mockdata_wizard_page_settings_page_name;
    public static String tools_mockdata_wizard_page_settings_page_description;

    public static String tools_mockdata_wizard_page_settings_group_settings;
    public static String tools_mockdata_wizard_page_settings_checkbox_remove_old_data;
    public static String tools_mockdata_wizard_page_settings_confirm_delete_old_data_message;
    public static String tools_mockdata_wizard_page_settings_combo_rows;
    public static String tools_mockdata_wizard_page_settings_batch_size;
    public static String tools_mockdata_wizard_page_settings_text_entity;

    public static String tools_mockdata_wizard_page_settings_group_generators;
    public static String tools_mockdata_wizard_page_settings_button_autoassign;
    public static String tools_mockdata_wizard_page_settings_button_autoassign_confirm;
    public static String tools_mockdata_wizard_page_settings_generatorselector_attribute;
    public static String tools_mockdata_wizard_page_settings_generatorselector_generator;
    public static String tools_mockdata_wizard_page_settings_button_reset;
    public static String tools_mockdata_wizard_page_settings_button_info_notfound;
    public static String tools_mockdata_wizard_page_settings_button_info_noattributes;
    public static String tools_mockdata_wizard_page_settings_notfound;

    public static String tools_mockdata_wizard_negative_numeric_error;
    public static String tools_mockdata_wizard_log_removing_from;
    public static String tools_mockdata_wizard_log_removing_error;
    public static String tools_mockdata_wizard_log_rows_updated;
    public static String tools_mockdata_wizard_log_duration;
    public static String tools_mockdata_wizard_log_cleaning;
    public static String tools_mockdata_wizard_log_not_removed;
    public static String tools_mockdata_wizard_log_inserting_into;
    public static String tools_mockdata_wizard_log_inserted_rows;
    public static String tools_mockdata_wizard_log_error_inserting;
    public static String tools_mockdata_wizard_log_error_generating;

    public static String tools_mockdata_attribute_generator_skip;

}
