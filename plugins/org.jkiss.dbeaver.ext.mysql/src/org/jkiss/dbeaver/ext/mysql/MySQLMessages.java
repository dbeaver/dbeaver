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
package org.jkiss.dbeaver.ext.mysql;

import org.eclipse.osgi.util.NLS;

public class MySQLMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mysql.MySQLMessages";

    public static String exception_direct_database_rename;
    public static String exception_only_select_could_produce_execution_plan;
    public static String table_column_length_tooltip;
    public static String table_column_length;

    static {
        NLS.initializeMessages(BUNDLE_NAME, MySQLMessages.class);
    }

    private MySQLMessages() {
    }

}
