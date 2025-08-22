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
package org.jkiss.dbeaver.model.sql.messages;

import org.jkiss.dbeaver.utils.NLS;

public class ModelSQLMessages extends NLS {
    static final String BUNDLE_NAME = "org.jkiss.dbeaver.model.sql.messages.ModelSQLResources"; //$NON-NLS-1$

    public static String model_sql_semantic_symbolClass_UNKNOWN;
    public static String model_sql_semantic_symbolClass_QUOTED;
    public static String model_sql_semantic_symbolClass_RESERVED;
    public static String model_sql_semantic_symbolClass_STRING;
    public static String model_sql_semantic_symbolClass_CATALOG;
    public static String model_sql_semantic_symbolClass_SCHEMA;
    public static String model_sql_semantic_symbolClass_TABLE;
    public static String model_sql_semantic_symbolClass_OBJECT;
    public static String model_sql_semantic_symbolClass_FUNCTION;
    public static String model_sql_semantic_symbolClass_TABLE_ALIAS;
    public static String model_sql_semantic_symbolClass_COLUMN;
    public static String model_sql_semantic_symbolClass_COLUMN_DERIVED;
    public static String model_sql_semantic_symbolClass_COMPOSITE_FIELD;
    public static String model_sql_semantic_symbolClass_SQL_BATCH_VARIABLE;
    public static String model_sql_semantic_symbolClass_DBEAVER_VARIABLE;
    public static String model_sql_semantic_symbolClass_DBEAVER_PARAMETER;
    public static String model_sql_semantic_symbolClass_DBEAVER_COMMAND;
    public static String model_sql_semantic_symbolClass_ERROR;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, ModelSQLMessages.class);
    }

    private ModelSQLMessages() {
    }

}
