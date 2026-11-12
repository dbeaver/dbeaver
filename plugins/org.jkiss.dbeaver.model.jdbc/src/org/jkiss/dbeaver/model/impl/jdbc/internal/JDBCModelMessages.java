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
package org.jkiss.dbeaver.model.impl.jdbc.internal;

import org.eclipse.osgi.util.NLS;

public class JDBCModelMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.model.impl.jdbc.internal.JDBCModelMessages";

    public static String model_jdbc_read_database_meta_data;
    public static String model_jdbc_Database;
    public static String model_jdbc_exception_bad_savepoint_object;
    public static String model_jdbc_exception_invalid_transaction_isolation_parameter;
    public static String model_jdbc_fetch_table_row_count;
    public static String model_jdbc_None;
    public static String model_jdbc_Procedure;
    public static String model_jdbc_read_committed;
    public static String model_jdbc_read_uncommitted;
    public static String model_jdbc_repeatable_read;
    public static String model_jdbc_Schema;
    public static String model_jdbc_Serializable;
    public static String jdbc_content_view_error_message_title;
    public static String jdbc_content_view_error_message_hint;

    static {
        NLS.initializeMessages(BUNDLE_NAME, JDBCModelMessages.class);
    }

    private JDBCModelMessages() {
    }
}
