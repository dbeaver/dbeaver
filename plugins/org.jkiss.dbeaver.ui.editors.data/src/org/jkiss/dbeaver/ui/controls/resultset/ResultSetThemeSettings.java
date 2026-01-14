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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.dbeaver.ui.BaseEditorColors;
import org.jkiss.dbeaver.ui.ThemeColor;
import org.jkiss.dbeaver.ui.ThemeFont;
import org.jkiss.dbeaver.ui.ThemeListener;

/**
 * Theme settings
 */
public class ResultSetThemeSettings extends ThemeListener {

    @ThemeFont(ThemeConstants.FONT_SQL_RESULT_SET)
    public volatile Font resultSetFont;
    @ThemeFont(value = ThemeConstants.FONT_SQL_RESULT_SET, bold = true)
    public volatile Font resultSetFontBold;
    @ThemeFont(value = ThemeConstants.FONT_SQL_RESULT_SET, italic = true)
    public volatile Font resultSetFontItalic;

    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_CELL_NEW_BACK)
    public volatile Color backgroundAdded;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_CELL_DELETED_BACK)
    public volatile Color backgroundDeleted;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_CELL_MODIFIED_BACK)
    public volatile Color backgroundModified;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_CELL_ODD_BACK)
    public volatile Color backgroundOdd;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_CELL_READ_ONLY)
    public volatile Color backgroundReadOnly;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE)
    public volatile Color foregroundSelected;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_NULL_FOREGROUND)
    public volatile Color foregroundNull;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK)
    public volatile Color backgroundSelected;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_CELL_MATCHED)
    public volatile Color backgroundMatched;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_CELL_ERROR_BACK)
    public volatile Color backgroundError;
    @ThemeColor(BaseEditorColors.COLOR_ERROR)
    public volatile Color foregroundError;

    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_HEADER_FOREGROUND)
    public volatile Color cellHeaderForeground;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_HEADER_BACKGROUND)
    public volatile Color cellHeaderBackground;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_HEADER_SELECTED_BACKGROUND)
    public volatile Color cellHeaderSelectedBackground;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_HEADER_BORDER)
    public volatile Color cellHeaderBorder;

    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_LINES_NORMAL)
    public volatile Color lineNormalColor;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_LINES_SELECTED)
    public volatile Color lineSelectedColor;

    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_BINARY_FOREGROUND)
    public volatile Color dtBinaryColor;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_BOOLEAN_FOREGROUND)
    public volatile Color dtBooleanColor;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_DATETIME_FOREGROUND)
    public volatile Color dtDateTimeColor;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_NUMERIC_FOREGROUND)
    public volatile Color dtNumericColor;
    @ThemeColor(ThemeConstants.COLOR_SQL_RESULT_STRING_FOREGROUND)
    public volatile Color dtStringColor;

    public static final ResultSetThemeSettings instance = new ResultSetThemeSettings();
}
