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

package org.jkiss.dbeaver.ui.controls.resultset;

/**
 * ThemeConstants
 */
public class ThemeConstants
{
    public static final String RESULTS_PROP_PREFIX = "org.jkiss.dbeaver.sql.resultset.";
    public static final String SQL_EDITOR_PROP_PREFIX = "org.jkiss.dbeaver.sql.editor.";

    public static final String FONT_SQL_RESULT_SET = RESULTS_PROP_PREFIX + "font"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_SET_SELECTION_FORE = RESULTS_PROP_PREFIX + "color.selection.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_SET_SELECTION_BACK = RESULTS_PROP_PREFIX + "color.selection.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_SET_PREVIEW_BACK = RESULTS_PROP_PREFIX + "color.preview.background"; //$NON-NLS-1$

    public static final String COLOR_SQL_RESULT_CELL_FORE = RESULTS_PROP_PREFIX + "color.cell.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_CELL_ODD_BACK = RESULTS_PROP_PREFIX + "color.cell.odd.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_CELL_MODIFIED_BACK = RESULTS_PROP_PREFIX + "color.cell.modified.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_CELL_ERROR_BACK = RESULTS_PROP_PREFIX + "color.cell.error.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_CELL_NEW_BACK = RESULTS_PROP_PREFIX + "color.cell.new.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_CELL_DELETED_BACK = RESULTS_PROP_PREFIX + "color.cell.deleted.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_CELL_READ_ONLY = RESULTS_PROP_PREFIX + "color.cell.readonly.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_CELL_MATCHED = RESULTS_PROP_PREFIX + "color.cell.matched.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_HEADER_BACKGROUND = RESULTS_PROP_PREFIX + "color.header.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_HEADER_FOREGROUND = RESULTS_PROP_PREFIX + "color.header.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_HEADER_SELECTED_BACKGROUND = RESULTS_PROP_PREFIX + "color.header.selected.background"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_HEADER_BORDER = RESULTS_PROP_PREFIX + "color.header.border"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_NULL_FOREGROUND = RESULTS_PROP_PREFIX + "color.null.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_BINARY_FOREGROUND = RESULTS_PROP_PREFIX + "color.binary.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_BOOLEAN_FOREGROUND = RESULTS_PROP_PREFIX + "color.boolean.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_DATETIME_FOREGROUND = RESULTS_PROP_PREFIX + "color.datetime.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_NUMERIC_FOREGROUND = RESULTS_PROP_PREFIX + "color.numeric.foreground"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_STRING_FOREGROUND = RESULTS_PROP_PREFIX + "color.string.foreground"; //$NON-NLS-1$

    public static final String COLOR_SQL_RESULT_LINES_NORMAL = RESULTS_PROP_PREFIX + "color.lines.normal"; //$NON-NLS-1$
    public static final String COLOR_SQL_RESULT_LINES_SELECTED = RESULTS_PROP_PREFIX + "color.lines.selected"; //$NON-NLS-1$


    public static final String SQL_EDITOR_COLOR_KEYWORD = SQL_EDITOR_PROP_PREFIX + "color.keyword.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_DATATYPE = SQL_EDITOR_PROP_PREFIX + "color.datatype.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_FUNCTION = SQL_EDITOR_PROP_PREFIX + "color.function.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_STRING = SQL_EDITOR_PROP_PREFIX + "color.string.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_TABLE = SQL_EDITOR_PROP_PREFIX + "color.table.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_TABLE_ALIAS = SQL_EDITOR_PROP_PREFIX + "color.table.alias.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_COLUMN = SQL_EDITOR_PROP_PREFIX + "color.column.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_COLUMN_DERIVED = SQL_EDITOR_PROP_PREFIX + "color.column.derived.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_SCHEMA = SQL_EDITOR_PROP_PREFIX + "color.schema.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_COMPOSITE_FIELD = SQL_EDITOR_PROP_PREFIX + "color.composite.field.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_SQL_VARIABLE = SQL_EDITOR_PROP_PREFIX + "color.sqlVariable.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_SEMANTIC_ERROR = SQL_EDITOR_PROP_PREFIX + "color.semanticError.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_NUMBER = SQL_EDITOR_PROP_PREFIX + "color.number.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_COMMENT = SQL_EDITOR_PROP_PREFIX + "color.comment.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_DELIMITER = SQL_EDITOR_PROP_PREFIX + "color.delimiter.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_PARAMETER = SQL_EDITOR_PROP_PREFIX + "color.parameter.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_COMMAND = SQL_EDITOR_PROP_PREFIX + "color.command.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_TEXT = SQL_EDITOR_PROP_PREFIX + "color.text.foreground"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_BACKGROUND = SQL_EDITOR_PROP_PREFIX + "color.text.background"; //$NON-NLS-1$
    public static final String SQL_EDITOR_COLOR_DISABLED = SQL_EDITOR_PROP_PREFIX + "color.disabled.background"; //$NON-NLS-1$
}
