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
package org.jkiss.dbeaver.registry.internal;

import org.jkiss.dbeaver.utils.NLS;

public class RegistryMessages extends NLS {
    public static final String BUNDLE_NAME = "org.jkiss.dbeaver.registry.internal.RegistryMessages"; //$NON-NLS-1$

    public static String dialog_connection_auth_title;
    public static String dialog_connection_auth_title_for_handler;

    public static String dialog_connection_auth_username;
    public static String dialog_connection_auth_password;
    public static String dialog_connection_auth_passphrase;

    public static String dialog_data_source_synchronization_fail_title;
    public static String dialog_data_source_synchronization_fail_local_message;
    public static String dialog_data_source_synchronization_fail_remote_message;

    public static String navigator_settings_preset_simple_view_name;
    public static String navigator_settings_preset_simple_view_description;
    public static String navigator_settings_preset_advanced_view_name;
    public static String navigator_settings_preset_advanced_view_description;
    public static String navigator_settings_preset_custom_view_name;
    public static String navigator_settings_preset_custom_view_description;
    public static String project_description_comment;

    public static String project_open_cannot_read_credentials_title;
    public static String project_open_cannot_read_credentials_message;
    public static String project_open_cannot_read_credentials_button_text;
    public static String project_open_cannot_read_configuration_title;
    public static String project_open_cannot_read_configuration_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, RegistryMessages.class);
    }

    private RegistryMessages() {
        // prevents instantiation
    }
}
