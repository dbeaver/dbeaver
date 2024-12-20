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
import org.jkiss.dbeaver.ui.ThemeListener;
import org.jkiss.dbeaver.ui.ThemeParameter;

/**
 * Theme settings
 */
public class ResultSetThemeSettings extends ThemeListener {

    @ThemeParameter(ThemeConstants.FONT_SQL_RESULT_SET)
    public volatile Font resultSetFont;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_CELL_NEW_BACK)
    public volatile Color backgroundAdded;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_CELL_DELETED_BACK)
    public volatile Color backgroundDeleted;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_CELL_MODIFIED_BACK)
    public volatile Color backgroundModified;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_CELL_ODD_BACK)
    public volatile Color backgroundOdd;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_CELL_READ_ONLY)
    public volatile Color backgroundReadOnly;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE)
    public volatile Color foregroundSelected;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_NULL_FOREGROUND)
    public volatile Color foregroundNull;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK)
    public volatile Color backgroundSelected;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_CELL_MATCHED)
    public volatile Color backgroundMatched;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_CELL_ERROR_BACK)
    public volatile Color backgroundError;
    @ThemeParameter(BaseEditorColors.COLOR_ERROR)
    public volatile Color foregroundError;

    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_HEADER_FOREGROUND)
    public volatile Color cellHeaderForeground;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_HEADER_BACKGROUND)
    public volatile Color cellHeaderBackground;
    @ThemeParameter(ThemeConstants.COLOR_SQL_RESULT_HEADER_BORDER)
    public volatile Color cellHeaderBorder;

    public static final ResultSetThemeSettings instance = new ResultSetThemeSettings();
}
