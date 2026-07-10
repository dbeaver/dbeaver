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
package org.jkiss.dbeaver.model.ai.functions;

public final class AIFunctionParams {
    public static final String CATALOG_PARAM = "catalogName";
    public static final String SCHEMA_PARAM = "schemaName";
    public static final String TABLE_NAMES_PARAM = "tableNames";

    public static final String DDL_CONSTRAINTS_PARAM = "ddlConstraints";
    public static final String DDL_INDEXES_PARAM = "ddlIndexes";
    public static final String DDL_COMMENTS_PARAM = "ddlComments";
    public static final String DDL_REFERENCES_PARAM = "ddlReferences";
}
