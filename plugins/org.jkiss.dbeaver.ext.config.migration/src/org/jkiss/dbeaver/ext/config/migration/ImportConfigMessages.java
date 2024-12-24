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
package org.jkiss.dbeaver.ext.config.migration;

import org.jkiss.dbeaver.utils.NLS;

public class ImportConfigMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.config.migration.ImportConfigMessages"; //$NON-NLS-1$
    public static String config_import_wizard_header_import_configuration;
    public static String config_import_wizard_page_caption_connections;
    public static String config_import_wizard_page_dbvis_label_installation_not_found;
    public static String config_import_wizard_page_label_connection_list;
    public static String config_import_wizard_page_squirrel_label_installation_not_found;
    public static String config_import_wizard_page_th_driver;
    public static String config_import_wizard_page_th_name;
    public static String config_import_wizard_page_th_url;
    public static String config_import_wizard_page_driver_unknown;
    public static String config_import_wizard_btn_select_all;
    public static String config_import_wizard_btn_deselect_all;
    public static String config_import_wizard_btn_set_driver;
    public static String config_import_wizard_error;
    public static String config_import_wizard_no_connection_found_error;
    public static String config_import_wizard_choose_driver_for_connections;
    public static String config_import_wizard_driver_selection_name;
    public static String config_import_wizard_driver_selection_description;
    public static String config_import_wizard_custom_driver_settings;
    public static String config_import_wizard_custom_driver_import_settings_name;
    public static String config_import_wizard_custom_driver_import_settings_file_format_description;
    public static String config_import_wizard_custom_driver_import_settings_file_description;
    public static String config_import_wizard_custom_input_type;
    public static String config_import_wizard_custom_input_file;
    public static String config_import_wizard_custom_input_file_configuration;
    public static String config_import_wizard_file_doesnt_exist_error;
    public static String config_import_wizard_file_encoding;
    public static String config_import_wizard_dbvis_name;
    public static String config_import_wizard_dbvis_description;
    public static String config_import_wizard_navicat_name;
    public static String config_import_wizard_navicat_description;
    public static String config_import_wizard_navicat_connection_export_file;
    public static String config_import_wizard_squirrel_name;
    public static String config_import_wizard_squirrel_description;
    public static String config_import_wizard_import_driver;
    public static String config_import_wizard_extract_url_parameters;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ImportConfigMessages.class);
    }

    private ImportConfigMessages() {
    }
}
