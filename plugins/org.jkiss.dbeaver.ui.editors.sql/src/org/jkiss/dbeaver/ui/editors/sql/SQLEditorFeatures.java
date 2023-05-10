/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.model.runtime.features.DBRFeature;
import org.jkiss.dbeaver.ui.editors.BaseTextEditorCommands;

/**
 * SQL editor features
 */
public interface SQLEditorFeatures {

    DBRFeature CATEGORY_SQL_EDITOR = DBRFeature.createCategory("SQL editor", "SQL editor features");

    DBRFeature SQL_EDITOR_OPEN = DBRFeature.createFeature(CATEGORY_SQL_EDITOR, "Open SQL editor");
    DBRFeature SQL_SCRIPT_RECENT = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_SQL_EDITOR_RECENT);
    DBRFeature SQL_SCRIPT_NEW = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_SQL_EDITOR_NEW);

    DBRFeature SQL_EDITOR_EXECUTE_QUERY = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_STATEMENT);
    DBRFeature SQL_EDITOR_EXECUTE_QUERY_NEW = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_STATEMENT_NEW);
    DBRFeature SQL_EDITOR_EXECUTE_SCRIPT = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_SCRIPT);;
    DBRFeature SQL_EDITOR_EXECUTE_SCRIPT_NEW = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_SCRIPT_NEW);;

    DBRFeature SQL_EDITOR_EXPLAIN_PLAN = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXPLAIN_PLAN);
    DBRFeature SQL_EDITOR_LOAD_PLAN = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_LOAD_PLAN);

    DBRFeature SQL_EDITOR_SHOW_OUTPUT = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_SQL_SHOW_OUTPUT);
    DBRFeature SQL_EDITOR_SHOW_LOG = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_SQL_SHOW_LOG);
    DBRFeature SQL_EDITOR_SHOW_VARIABLES = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_SQL_SHOW_VARIABLES);

    DBRFeature SQL_EDITOR_SELECT_ROW_COUNT = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_ROW_COUNT);
    DBRFeature SQL_EDITOR_SELECT_ALL_ROWS = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_ALL_ROWS);
    DBRFeature SQL_EDITOR_EXECUTE_EXPRESSION = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_EXPRESSION);

    DBRFeature SQL_EDITOR_FORMAT_SQL = DBRFeature.createCommandFeature(CATEGORY_SQL_EDITOR, BaseTextEditorCommands.CMD_CONTENT_FORMAT);

    DBRFeature SQL_EDITOR_COPY_AS_SOURCE_CODE = DBRFeature.createFeature(CATEGORY_SQL_EDITOR, "Copy SQL as source code");

    DBRFeature SQL_EDITOR_QUERY_PARAMS = DBRFeature.createFeature(CATEGORY_SQL_EDITOR, "Use SQL query parameters");

    DBRFeature CATEGORY_SQL_TOOLS = DBRFeature.createCategory("SQL tools", "SQL tools features");

    DBRFeature SQL_EDITOR_GENERATE_SQL_ON_OBJECT = DBRFeature.createFeature(CATEGORY_SQL_TOOLS, "Generate SQL on object");
}
