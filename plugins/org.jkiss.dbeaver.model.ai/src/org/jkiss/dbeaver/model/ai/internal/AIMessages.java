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
package org.jkiss.dbeaver.model.ai.internal;

import org.jkiss.dbeaver.utils.NLS;

public class AIMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.model.ai.internal.AIMessages"; //$NON-NLS-1$

    public static String ai_scope_current_schema;
    public static String ai_scope_current_database;
    public static String ai_scope_current_datasource;
    public static String ai_scope_custom;

    public static String ai_execute_query_title;
    public static String ai_execute_query_confirm_sql_message;
    public static String ai_execute_query_confirm_dml_message;
    public static String ai_execute_query_confirm_ddl_message;
    public static String ai_execute_query_auto_commit_disabled_title;
    public static String ai_execute_query_auto_commit_disabled_message;
    public static String ai_execute_command_confirm_sql_message;
    public static String ai_execute_command_confirm_dml_message;
    public static String ai_execute_command_confirm_ddl_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, AIMessages.class);
    }

    private AIMessages() {
    }
}
