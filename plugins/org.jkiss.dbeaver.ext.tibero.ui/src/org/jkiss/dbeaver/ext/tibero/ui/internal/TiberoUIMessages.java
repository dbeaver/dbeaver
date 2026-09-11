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
package org.jkiss.dbeaver.ext.tibero.ui.internal;

import org.eclipse.osgi.util.NLS;

public final class TiberoUIMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.tibero.ui.internal.TiberoUIMessages"; //$NON-NLS-1$

    public static String dialog_connection_database;
    public static String dialog_connection_host;
    public static String dialog_connection_jdbc_url;
    public static String dialog_connection_port;
    public static String dialog_connection_settings_group;
    public static String dialog_connection_tibero_properties;
    public static String dialog_connection_tibero_properties_description;
    public static String dialog_controlgroup_content;
    public static String dialog_controlgroup_navigator;
    public static String dialog_controlgroup_source_editor;
    public static String edit_checkbox_compile_package_after_save;
    public static String edit_checkbox_compile_package_after_save_description;
    public static String edit_checkbox_hide_empty_schemas;
    public static String edit_checkbox_hide_empty_schemas_description;
    public static String edit_checkbox_read_column_comments;
    public static String edit_checkbox_read_column_comments_description;
    public static String edit_checkbox_recompile_body_on_spec_save;
    public static String edit_checkbox_recompile_body_on_spec_save_description;
    public static String edit_checkbox_show_only_one_schema;
    public static String edit_checkbox_show_only_one_schema_description;
    public static String edit_checkbox_show_schema_index_table_description;
    public static String edit_checkbox_show_schema_index_table_description_description;
    public static String edit_checkbox_show_schema_table_description;
    public static String edit_checkbox_show_schema_table_description_description;
    public static String edit_checkbox_show_schema_trigger_table_description;
    public static String edit_checkbox_show_schema_trigger_table_description_description;

    static {
        NLS.initializeMessages(BUNDLE_NAME, TiberoUIMessages.class);
    }

    private TiberoUIMessages() {
    }
}
