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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.ui.ThemeColor;
import org.jkiss.dbeaver.ui.ThemeListener;
import org.jkiss.dbeaver.ui.controls.resultset.ThemeConstants;

public class SQLEditorThemeSettings extends ThemeListener {

    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_KEYWORD)
    public Color editorKeywordColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_DATATYPE)
    public Color editorDatatypeColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_FUNCTION)
    public Color editorFunctionColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_STRING)
    public Color editorStringColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_TABLE)
    public Color editorTableColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_TABLE_ALIAS)
    public Color editorTableAliasColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_COLUMN)
    public Color editorColumnColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_COLUMN_DERIVED)
    public Color editorColumnDerivedColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_SCHEMA)
    public Color editorSchemaColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_COMPOSITE_FIELD)
    public Color editorCompositeFieldColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_SQL_VARIABLE)
    public Color editorSqlVariableColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_SEMANTIC_ERROR)
    public Color editorSemanticErrorColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_NUMBER)
    public Color editorNumberColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_COMMENT)
    public Color editorCommentColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_DELIMITER)
    public Color editorDelimiterColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_PARAMETER)
    public Color editorParameterColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_COMMAND)
    public Color editorCommandColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_TEXT)
    public Color editorTextColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_BACKGROUND)
    public Color editorBackgroundColor;
    @ThemeColor(ThemeConstants.SQL_EDITOR_COLOR_DISABLED)
    public Color editorDisabledColor;

    public static final SQLEditorThemeSettings instance = new SQLEditorThemeSettings();

}