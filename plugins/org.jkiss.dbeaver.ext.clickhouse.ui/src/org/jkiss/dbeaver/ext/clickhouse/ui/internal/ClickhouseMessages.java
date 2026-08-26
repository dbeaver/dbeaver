/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.ui.internal;

import org.eclipse.osgi.util.NLS;

public class ClickhouseMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.clickhouse.ui.internal.ClickhouseMessages"; //$NON-NLS-1$

    public static String dialog_connection_page_advanced_settings;

    public static String dialog_connection_page_checkbox_use_ssl;
    public static String dialog_connection_page_checkbox_tip_use_ssl;
    public static String dialog_connection_page_text_ssl_file_path;
    public static String dialog_connection_page_text_ssl_file_path_tip;
    public static String dialog_connection_page_text_ssl_file_key;
    public static String dialog_connection_page_text_ssl_file_key_tip;
    public static String dialog_connection_page_text_ssl_mode;
    public static String dialog_connection_page_text_ssl_mode_tip;
    public static String dialog_connection_page_text_ssl_group;

    public static String dialog_connection_auth_cloud_sso_info;
    public static String dialog_connection_auth_cloud_sso_sign_out;
    public static String dialog_connection_auth_cloud_sso_sign_out_tip;

    public static String dialog_connection_auth_oidc_email;
    public static String dialog_connection_auth_oidc_email_tip;
    public static String dialog_connection_auth_oidc_issuer;
    public static String dialog_connection_auth_oidc_issuer_tip;
    public static String dialog_connection_auth_oidc_client_id;
    public static String dialog_connection_auth_oidc_client_secret;
    public static String dialog_connection_auth_oidc_client_secret_tip;
    public static String dialog_connection_auth_oidc_audience;
    public static String dialog_connection_auth_oidc_audience_tip;
    public static String dialog_connection_auth_oidc_scopes;
    public static String dialog_connection_auth_oidc_callback_port;
    public static String dialog_connection_auth_oidc_callback_port_tip;
    public static String dialog_connection_auth_oidc_use_device_code;
    public static String dialog_connection_auth_oidc_use_device_code_tip;

    static {
        NLS.initializeMessages(BUNDLE_NAME, ClickhouseMessages.class);
    }

    private ClickhouseMessages() {
    }
}
