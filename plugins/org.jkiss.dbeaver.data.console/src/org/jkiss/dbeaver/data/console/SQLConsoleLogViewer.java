/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.data.console;


import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.console.MessageConsole;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorOutputConsoleViewer;
import org.jkiss.utils.CommonUtils;
import java.util.List;


public class SQLConsoleLogViewer extends SQLEditorOutputConsoleViewer {

    public SQLConsoleLogViewer(@NotNull IWorkbenchPartSite site, @NotNull CTabFolder tabsContainer, int styles) {
        super(site, tabsContainer, new MessageConsole("sql-data-log-output", DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE)));
    }

    public void printGrid(@NotNull DBPPreferenceStore prefs, @NotNull ResultSetModel model, @Nullable String name) {
        final int maxColumnSize = prefs.getInt(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE);
        final boolean delimLeading = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING);
        final boolean delimTrailing = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING);
        final boolean extraSpaces = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES);
        final boolean showNulls = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS);
        final boolean rightJustifyNumbers = prefs.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);
        final boolean rightJustifyDateTime = prefs.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME);
        final int tabSize = prefs.getInt(ResultSetPreferences.RESULT_TEXT_TAB_SIZE);

        DBDDisplayFormat displayFormat = DBDDisplayFormat.safeValueOf(prefs.getString(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT));

        StringBuilder grid = new StringBuilder(512);
        List<DBDAttributeBinding> attrs = model.getVisibleAttributes();
        
        grid.append("> ").append(name).append("\n");
        
        List<ResultSetRow> allRows = model.getAllRows();
        int extraSpacesNum = extraSpaces ? 2 : 0;
        int[] colWidths = new int[attrs.size()];
        
        // Calculate column widths
        for (int i = 0; i < attrs.size(); i++) {
            DBDAttributeBinding attr = attrs.get(i);
            colWidths[i] = getAttributeName(attr).length() + extraSpacesNum;
            if (showNulls && !attr.isRequired()) {
                colWidths[i] = Math.max(colWidths[i], DBConstants.NULL_VALUE_LABEL.length());
            }
            for (ResultSetRow row : allRows) {
                String displayString = getCellString(model, attr, row, displayFormat, showNulls);
                colWidths[i] = Math.max(colWidths[i], getStringWidth(displayString, tabSize) + extraSpacesNum);
            }
        }
        for (int i = 0; i < colWidths.length; i++) {
            if (colWidths[i] > maxColumnSize) {
                colWidths[i] = maxColumnSize;
            }
        }

        // Print header
        if (delimLeading) {
            grid.append("|");
        }
        for (int i = 0; i < attrs.size(); i++) {
            if (i > 0) {
                grid.append("|");
            }
            if (extraSpaces) {
                grid.append(" ");
            }
            DBDAttributeBinding attr = attrs.get(i);
            String attrName = getAttributeName(attr);
            grid.append(attrName);
            for (int k = colWidths[i] - attrName.length() - extraSpacesNum; k > 0; k--) {
                grid.append(" ");
            }
            if (extraSpaces) {
                grid.append(" ");
            }
        }
        if (delimTrailing) {
            grid.append("|");
        }
        grid.append("\n");

        // Print divider
        // Print header
        if (delimLeading) {
            grid.append("|");
        }
        for (int i = 0; i < attrs.size(); i++) {
            if (i > 0) {
                grid.append("|");
            }
            for (int k = colWidths[i]; k > 0; k--) {
                grid.append("-");
            }
        }
        if (delimTrailing) {
            grid.append("|");
        }
        grid.append("\n");

        boolean newLines = false;
        for (int i = 0; i < allRows.size(); i++) {
            newLines = true;
            ResultSetRow row = allRows.get(i);
            if (delimLeading) {
                grid.append("|");
            }
            for (int k = 0; k < attrs.size(); k++) {
                if (k > 0) {
                    grid.append("|");
                }
                DBDAttributeBinding attr = attrs.get(k);
                String displayString = getCellString(model, attr, row, displayFormat, showNulls);
                if (displayString.length() >= colWidths[k]) {
                    displayString = CommonUtils.truncateString(displayString, colWidths[k]);
                }

                int stringWidth = getStringWidth(displayString, tabSize);

                if (extraSpaces) {
                    grid.append(" ");
                }
                DBPDataKind dataKind = attr.getDataKind();
                if ((dataKind == DBPDataKind.NUMERIC && rightJustifyNumbers)
                    || (dataKind == DBPDataKind.DATETIME && rightJustifyDateTime)) {
                    // Right justify value
                    for (int j = colWidths[k] - stringWidth - extraSpacesNum; j > 0; j--) {
                        grid.append(" ");
                    }
                    grid.append(displayString);
                } else {
                    grid.append(displayString);
                    for (int j = colWidths[k] - stringWidth - extraSpacesNum; j > 0; j--) {
                        grid.append(" ");
                    }
                }
                if (extraSpaces) {
                    grid.append(" ");
                }
            }
            if (delimTrailing) {
                grid.append("|");
            }
            grid.append("\n");
        }
        grid.setLength(grid.length() - 1); // cut last line feed
        grid.append("\n");

        // Print divider
        // Print header
        if (delimLeading) {
            grid.append("|");
        }
        for (int i = 0; i < attrs.size(); i++) {
            if (i > 0) {
                grid.append("|");
            }
            for (int k = colWidths[i]; k > 0; k--) {
                grid.append("-");
            }
        }
        if (delimTrailing) {
            grid.append("|");
        }
        grid.append("\n\n");

        if (newLines) {
            this.getOutputWriter().append(grid.toString());
            this.getOutputWriter().append(allRows.size() + " row(s) fetched.\n\n");
            this.getOutputWriter().flush();
            this.scrollToEnd();
        }
    }

    private int getStringWidth(@Nullable String str, int tabSize) {
        int width = 0;
        if (str != null && str.length() > 0) {
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\t') {
                    width += tabSize;
                } else {
                    width++;
                }
            }
        }
        return width;
    }
    

    @NotNull
    private static String getAttributeName(@NotNull DBDAttributeBinding attr) {
        if (CommonUtils.isEmpty(attr.getLabel())) {
            return attr.getName();
        } else {
            return attr.getLabel();
        }
    }

    @NotNull
    private String getCellString(
        @NotNull ResultSetModel model, 
        @NotNull DBDAttributeBinding attr,
        @NotNull ResultSetRow row,
        @NotNull DBDDisplayFormat displayFormat, 
        boolean showNulls
    ) {
        Object cellValue = model.getCellValue(attr, row);
        if (cellValue instanceof DBDValueError) {
            return ((DBDValueError) cellValue).getErrorTitle();
        }
        String displayString = attr.getValueHandler().getValueDisplayString(attr, cellValue, displayFormat);

        if (displayString.isEmpty() && showNulls && DBUtils.isNullValue(cellValue)) {
            displayString = DBConstants.NULL_VALUE_LABEL;
        }

        StringBuilder fixBuffer = new StringBuilder();

        for (int i = 0; i < displayString.length(); i++) {
            char c = displayString.charAt(i);
            switch (c) {
                case '\n':
                    c = CommonUtils.PARAGRAPH_CHAR;
                    break;
                case '\r':
                    continue;
                case 0:
                case 255:
                case '\t':
                    c = ' ';
                    break;
                default:
                    // do nothing
                    break;
            }
            if (c < ' '/* || (c > 127 && c < 255)*/) {
                c = ' ';
            }
            fixBuffer.append(c);
        }

        return fixBuffer.toString();
    }

}
