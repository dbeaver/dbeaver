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

package org.jkiss.dbeaver.ext.iotdb.ui.internal;

import org.eclipse.osgi.util.NLS;

public class IoTDBUiMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.iotdb.ui.internal.IoTDBUIMessages"; //$NON-NLS-1$

    static {
        NLS.initializeMessages(BUNDLE_NAME, IoTDBUiMessages.class);
    }

    public static String controls_privilege_table_column_privilege_grant;
    public static String controls_privilege_table_column_privilege_grant_tip;
    public static String controls_privilege_table_column_privilege_name;
    public static String controls_privilege_table_column_privilege_name_tip;
    public static String controls_privilege_table_column_privilege_status;
    public static String controls_privilege_table_column_privilege_status_tip;
    public static String controls_privilege_table_push_button_check_all;
    public static String controls_privilege_table_push_button_clear_all;
    public static String editors_user_editor_abstract_load_grants;
    public static String editors_user_editor_general_control_dba_privileges;
    public static String editors_user_editor_general_group_login;
    public static String editors_user_editor_general_label_user_name;
    public static String editors_user_editor_general_service_load_catalog_privileges;
    public static String editors_user_editor_privileges_service_load_privileges;
    public static String editors_user_editor_privileges_service_load_tables;

    private IoTDBUiMessages() {
    }
}
