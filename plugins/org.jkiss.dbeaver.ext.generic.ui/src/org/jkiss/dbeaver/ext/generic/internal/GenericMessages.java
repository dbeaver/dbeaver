/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.generic.internal;

import org.eclipse.osgi.util.NLS;

public class GenericMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.generic.internal.GenericResources"; //$NON-NLS-1$
    public static String dialog_connection_advanced_tab;

    public static String dialog_connection_advanced_tab_tooltip;

    public static String dialog_connection_browse_button;

    public static String dialog_connection_database_schema_label;

    public static String dialog_connection_db_file_chooser_text;

    public static String dialog_connection_db_folder_chooser_message;

    public static String dialog_connection_db_folder_chooser_text;


    public static String dialog_connection_general_tab;

    public static String dialog_connection_general_tab_tooltip;

    public static String dialog_connection_host_label;
    public static String dialog_connection_jdbc_url_;

    public static String dialog_connection_password_label;

    public static String dialog_connection_path_label;

    public static String dialog_connection_port_label;

    public static String dialog_connection_server_label;

    public static String dialog_connection_test_connection_button;

    public static String dialog_connection_user_name_label;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, GenericMessages.class);
    }
    private GenericMessages() {
    }
}
