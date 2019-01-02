/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;

/**
 * SQL editor features
 */
public interface SQLEditorFeatures {


    DBRFeature SQL_EDITOR = DBRFeature.createCategory("SQL Editor", "SQL Editor features");
    DBRFeature SQL_EDITOR_QUERY_PARAMS = DBRFeature.createFeature(SQL_EDITOR, "Use SQL query parameters");
    DBRFeature SQL_EDITOR_EXPLAIN_PLAN = DBRFeature.createCommandFeature(SQL_EDITOR, SQLEditorCommands.CMD_EXPLAIN_PLAN);
    DBRFeature SQL_EDITOR_EXECUTE_SCRIPT = DBRFeature.createFeature(SQL_EDITOR, "Execute SQL script");
    DBRFeature SQL_EDITOR_EXECUTE_QUERY_NEW = DBRFeature.createCommandFeature(SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_STATEMENT_NEW);
    DBRFeature SQL_EDITOR_EXECUTE_QUERY = DBRFeature.createCommandFeature(SQL_EDITOR, SQLEditorCommands.CMD_EXECUTE_STATEMENT);
    DBRFeature SQL_EDITOR_OPEN = DBRFeature.createFeature(SQL_EDITOR, "Open SQL Editor");
}