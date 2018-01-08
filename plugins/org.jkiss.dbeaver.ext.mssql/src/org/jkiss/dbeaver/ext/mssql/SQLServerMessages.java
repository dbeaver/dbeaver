/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql;

import org.eclipse.osgi.util.NLS;

public class SQLServerMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mssql.SQLServerResources"; //$NON-NLS-1$

    /* PostgreConnectionPage */
    public static String dialog_setting_connection_settings;

    public static String dialog_connection_windows_authentication_button;
    public static String dialog_connection_database_schema_label;
    public static String dialog_connection_host_label;
    public static String dialog_connection_password_label;
    public static String dialog_connection_port_label;
    public static String dialog_connection_user_name_label;
    public static String dialog_setting_show_all_schemas;
    public static String dialog_setting_show_all_schemas_tip;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SQLServerMessages.class);
    }

    private SQLServerMessages() {
    }


}
