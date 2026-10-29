/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.sqlite.ui.internal;

import org.eclipse.osgi.util.NLS;

public class SQLiteUIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.sqlite.ui.internal.SQLiteUIMessages";

    public static String dialog_connection_auth_label_token;

    public static String extension_confirmation_title;
    public static String extension_confirmation_message;
    public static String extension_confirmation_connection;
    public static String extension_confirmation_url;
    public static String extension_confirmation_allow;
    public static String extension_confirmation_connect;
    public static String extension_confirmation_libraries;
    public static String extension_confirmation_queries;
    public static String extension_settings_enable;
    public static String extension_settings_warning;
    public static String extension_settings_error;

    public static String extension_settings_open;
    public static String extension_settings_loading_disabled;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SQLiteUIMessages.class);
    }
}
