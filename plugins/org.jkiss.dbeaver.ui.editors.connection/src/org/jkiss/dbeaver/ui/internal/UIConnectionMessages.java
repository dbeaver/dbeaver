/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

public class UIConnectionMessages extends NLS {

    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ui.internal.UIConnectionMessages"; //$NON-NLS-1$

    public static String dialog_connection_edit_wizard_conn_conf_general_link;
    public static String dialog_connection_edit_wizard_conn_conf_network_link;
    public static String dialog_connection_edit_connection_settings_variables_hint_label;
    public static String dialog_connection_advanced_settings;
    public static String dialog_connection_env_variables_hint;

    public static String dialog_connection_driver;
    public static String dialog_connection_edit_driver_button;

    public static String dialog_connection_wizard_final_checkbox_save_password_locally;

    public static String controls_client_home_selector_browse;
    public static String controls_client_homes_panel_button_add_home;
    public static String controls_client_homes_panel_button_remove_home;
    public static String controls_client_homes_panel_confirm_remove_home_text;
    public static String controls_client_homes_panel_confirm_remove_home_title;
    public static String controls_client_homes_panel_dialog_title;
    public static String controls_client_homes_panel_group_information;
    public static String controls_client_homes_panel_label_id;
    public static String controls_client_homes_panel_label_name;
    public static String controls_client_homes_panel_label_path;
    public static String controls_client_homes_panel_label_product_name;
    public static String controls_client_homes_panel_label_product_version;

    public static String dialog_setting_connection_driver_properties_title;
    public static String dialog_setting_connection_driver_properties_description;
    public static String dialog_setting_connection_driver_properties_advanced;
    public static String dialog_setting_connection_driver_properties_advanced_tip;
    public static String dialog_setting_connection_driver_properties_docs_web_reference;

    public static String controls_connection_properties_action_add_property;
    public static String controls_connection_properties_action_remove_property;
    public static String controls_connection_properties_category_user_properties;
    public static String controls_connection_properties_dialog_new_property_title;

    public static String dialog_connection_auth_group;
    public static String dialog_connection_auth_checkbox_save_password;
    public static String dialog_connection_auth_group_user_cridentials;
    public static String dialog_connection_auth_label_password;
    public static String dialog_connection_auth_label_username;
    public static String dialog_connection_auth_label_show_password;

    public static String dialog_connection_network_socket_label_host;
    public static String dialog_connection_network_socket_label_port;
    public static String dialog_connection_network_socket_label_username;
    public static String dialog_connection_network_socket_label_password;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, UIConnectionMessages.class);
    }

    private UIConnectionMessages() {
    }
}
