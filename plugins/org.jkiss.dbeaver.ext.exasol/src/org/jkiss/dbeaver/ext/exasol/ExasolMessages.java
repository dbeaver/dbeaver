/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol;

import org.eclipse.osgi.util.NLS;

public class ExasolMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.exasol.ExasolResources"; //$NON-NLS-1$

    public static String manager_consumer_drop;
    public static String manager_consumer_rename;
    public static String manager_consumer_create;
    public static String dialog_schema_drop_title;
    public static String dialog_schema_drop_message;
    public static String dialog_connection_alter_message;
    public static String dialog_connection_alter_title;

    public static String dialog_general_continue;

    public static String read_table_details;
    public static String read_schema_details;
    public static String manager_priority_group_comment;
    public static String manager_priority_create;
    public static String manager_priority_drop;
    public static String manager_priority_alter;
    public static String manager_priority_rename;
    public static String manager_schema_owner;
    public static String manager_schema_raw_limit;
    public static String manager_schema_create;
    public static String manager_assign_priority_group;
    public static String exasol_security_policy_name;
    public static String exasol_security_policy_description;
    public static String manager_consumer_alter;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ExasolMessages.class);
    }

    private ExasolMessages() {
    }
}
