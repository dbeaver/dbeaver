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

package org.jkiss.dbeaver.model.sql;

/**
 * Preferences constants
 */
public final class SQLModelPreferences {

    public static final String SQL_FORMAT_FORMATTER             = "sql.format.formatter";
    public static final String SQL_PROPOSAL_INSERT_TABLE_ALIAS  = "sql.proposals.insert.table.alias";
    public static final String SQL_EDITOR_PROPOSAL_SHORT_NAME = "SQLEditor.ContentAssistant.proposals.short.name";
    public static final String SQL_EDITOR_PROPOSAL_ALWAYS_FQ = "SQLEditor.ContentAssistant.proposals.long.name";

    public static final String ADVANCED_HIGHLIGHTING_ENABLE = "SQLEditor.Highlighting.advanced.enable";
    public static final String READ_METADATA_FOR_SEMANTIC_ANALYSIS = "SQLEditor.Semantics.metadata.read.enable";
    
    public static final String AUTOCOMPLETION_MODE = "SQLEditor.ContentAssistant.experimental.mode";

}
