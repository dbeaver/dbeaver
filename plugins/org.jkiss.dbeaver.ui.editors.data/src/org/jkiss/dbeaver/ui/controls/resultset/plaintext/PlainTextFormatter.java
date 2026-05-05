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
package org.jkiss.dbeaver.ui.controls.resultset.plaintext;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class PlainTextFormatter {

    private final DBPPreferenceStore prefs;

    private boolean rightJustifyNumbers;
    private boolean rightJustifyDateTime;
    private int maxColumnSize;
    private boolean delimLeading;
    private boolean delimTrailing;
    private boolean delimTop;
    private boolean delimBottom;
    private boolean extraSpaces;
    private boolean lineNumbers;
    private boolean showNulls;
    private DBDDisplayFormat displayFormat;
    private int[] colWidths;
    private int startOffset;

    private final StringBuilder fixCellStringBuffer = new StringBuilder();

    public PlainTextFormatter(@NotNull DBPPreferenceStore prefs) {
        this.prefs = prefs;
        loadPrefs(prefs);
    }

    private void loadPrefs(@NotNull DBPPreferenceStore prefs) {
        maxColumnSize = prefs.getInt(ResultSetPreferences.RESULT_TEXT_MAX_COLUMN_SIZE);
        delimLeading = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_LEADING);
        delimTrailing = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_TRAILING);
        delimTop = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_TOP);
        delimBottom = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_DELIMITER_BOTTOM);
        extraSpaces = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES);
        lineNumbers = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_LINE_NUMBER);
        showNulls = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_SHOW_NULLS);
        displayFormat = DBDDisplayFormat.safeValueOf(prefs.getString(ResultSetPreferences.RESULT_TEXT_VALUE_FORMAT));
        extraSpaces = prefs.getBoolean(ResultSetPreferences.RESULT_TEXT_EXTRA_SPACES);
        rightJustifyNumbers = prefs.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);
        rightJustifyDateTime = prefs.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME);
    }

    public int[] getColWidths() {
        return colWidths;
    }

    public int getStartOffset() {
        return startOffset;
    }

    // returns number of rows
    public int printGrid(StringBuilder grid, ResultSetModel model) {
        List<DBDAttributeBinding> attrs = model.getVisibleAttributes();
        List<ResultSetRow> allRows = model.getAllRows();
        int extraSpacesNum = extraSpaces ? 2 : 0;
        if (colWidths == null) {
            // Calculate column widths
            colWidths = new int[attrs.size()];
            if (!attrs.isEmpty() && lineNumbers) {
                startOffset = getStringWidth(String.valueOf(allRows.size() + 1)) + extraSpacesNum + 1;
            } else {
                startOffset = 0;
            }
            for (int i = 0; i < attrs.size(); i++) {
                DBDAttributeBinding attr = attrs.get(i);
                colWidths[i] = getAttributeName(attr).length() + extraSpacesNum;
                if (showNulls && !attr.isRequired()) {
                    colWidths[i] = Math.max(colWidths[i], DBConstants.NULL_VALUE_LABEL.length());
                }
                for (ResultSetRow row : allRows) {
                    String displayString = this.getCellString(model, attr, row, displayFormat);
                    colWidths[i] = Math.max(colWidths[i], getStringWidth(displayString) + extraSpacesNum);
                }
            }
            for (int i = 0; i < colWidths.length; i++) {
                if (colWidths[i] > maxColumnSize) {
                    colWidths[i] = maxColumnSize;
                }
            }
        }

        if (delimTop) {
            // Print divider before header
            this.printSeparator(grid, colWidths);
        }
        // Print header
        if (delimLeading) {
            grid.append("|");
        }
        if (lineNumbers && attrs.size() != 0) {
            if (extraSpaces) {
                grid.append(" ");
            }
            grid.append("#");
            grid.append(" ".repeat(Math.max(0, startOffset - extraSpacesNum - 2)));
            if (extraSpaces) {
                grid.append(" ");
            }
        }
        for (int i = 0; i < attrs.size(); i++) {
            if (i  > 0 || startOffset != 0) {
                grid.append("|");
            }
            if (extraSpaces) {
                grid.append(" ");
            }
            DBDAttributeBinding attr = attrs.get(i);
            String attrName = getAttributeName(attr);
            grid.append(attrName).append(" ".repeat(Math.max(0, colWidths[i] - attrName.length() - extraSpacesNum)));
            if (extraSpaces) {
                grid.append(" ");
            }
        }
        if (delimTrailing) {
            grid.append("|");
        }
        grid.append("\n");

        // Print divider
        this.printSeparator(grid, colWidths);

        // Print rows
        int i = 1;
        for (ResultSetRow row : allRows) {
            if (delimLeading) {
                grid.append("|");
            }
            if (lineNumbers) {
                if (extraSpaces) {
                    grid.append(" ");
                }
                String displayNumber = String.valueOf(i);
                grid.append(displayNumber);
                int stringWidth = getStringWidth(displayNumber);
                grid.append(" ".repeat(Math.max(0, startOffset - stringWidth - extraSpacesNum - 1)));
                if (extraSpaces) {
                    grid.append(" ");
                }
            }
            for (int k = 0; k < attrs.size(); k++) {
                if (k > 0 || startOffset != 0) {
                    grid.append("|");
                }
                DBDAttributeBinding attr = attrs.get(k);
                String displayString = this.getCellString(model, attr, row, displayFormat);
                if (displayString.length() >= colWidths[k]) {
                    displayString = CommonUtils.truncateString(displayString, colWidths[k]);
                }

                int stringWidth = getStringWidth(displayString);

                if (extraSpaces) {
                    grid.append(" ");
                }
                DBPDataKind dataKind = attr.getDataKind();
                if ((dataKind == DBPDataKind.NUMERIC && rightJustifyNumbers)
                    || (dataKind == DBPDataKind.DATETIME && rightJustifyDateTime)) {
                    // Right justify value
                    grid.append(" ".repeat(Math.max(0, colWidths[k] - stringWidth - extraSpacesNum))).append(displayString);
                } else {
                    grid.append(displayString).append(" ".repeat(Math.max(0, colWidths[k] - stringWidth - extraSpacesNum)));
                }
                if (extraSpaces) {
                    grid.append(" ");
                }
            }
            if (delimTrailing) {
                grid.append("|");
            }
            grid.append("\n");
            i++;
        }
        if (delimBottom) {
            // Print divider after rows
            this.printSeparator(grid, colWidths);
        }
        grid.setLength(grid.length() - 1); // cut last line feed
        return allRows.size();
    }

    public String getCellString(ResultSetModel model, DBDAttributeBinding attr, ResultSetRow row, DBDDisplayFormat displayFormat) {
        Object cellValue = model.getCellValue(attr, row);
        if (cellValue instanceof DBDValueError) {
            return ((DBDValueError) cellValue).getErrorTitle();
        }
        if (cellValue instanceof Number && prefs.getBoolean(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT)) {
            displayFormat = DBDDisplayFormat.NATIVE;
        }

        String displayString = attr.getValueHandler().getValueDisplayString(attr, cellValue, displayFormat);

        if (displayString.isEmpty() &&
            showNulls &&
            DBUtils.isNullValue(cellValue))
        {
            displayString = DBConstants.NULL_VALUE_LABEL;
        }

        fixCellStringBuffer.setLength(0);
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
            fixCellStringBuffer.append(c);
        }

        return fixCellStringBuffer.toString();
    }

    public void printRecord(StringBuilder grid, ResultSetModel model, ResultSetRow currentRow) {
        String indent = extraSpaces ? " " : "";
        List<DBDAttributeBinding> attrs = model.getVisibleAttributes();
        String[] values = new String[attrs.size()];

        // Calculate column widths
        int nameWidth = 4;
        int valueWidth = 5;
        for (int i = 0; i < attrs.size(); i++) {
            DBDAttributeBinding attr = attrs.get(i);
            nameWidth = Math.max(nameWidth, getAttributeName(attr).length());
            if (currentRow != null) {
                String displayString = this.getCellString(model, attr, currentRow, displayFormat);
                values[i] = displayString;
                valueWidth = Math.max(valueWidth, values[i].length());
            }
        }
        final int extraSpacesNum = extraSpaces ? 2 : 0;
        final int[] colWidths = {nameWidth + extraSpacesNum, valueWidth + extraSpacesNum};

        if (delimTop) {
            // Print divider before header
            this.printSeparator(grid, colWidths);
        }

        // Header
        if (delimLeading) {
            grid.append("|");
        }
        grid.append(indent).append("Name").append(" ".repeat(nameWidth - 4));
        grid.append(indent).append("|").append(indent).append("Value").append(" ".repeat(valueWidth - 5));
        grid.append(indent);
        if (delimTrailing) {
            grid.append("|");
        }
        grid.append("\n");

        // Print divider between header and data
        printSeparator(grid, colWidths);

        if (currentRow != null) {
            // Values
            for (int i = 0; i < attrs.size(); i++) {
                DBDAttributeBinding attr = attrs.get(i);
                String name = getAttributeName(attr);
                if (delimLeading) {
                    grid.append("|");
                }
                grid.append(indent).append(name).append(indent).append(" ".repeat(Math.max(0, nameWidth - name.length())));
                grid.append("|");
                grid.append(indent).append(values[i]).append(" ".repeat(Math.max(0, valueWidth - values[i].length())));
                grid.append(indent);

                if (delimTrailing) {
                    grid.append("|");
                }
                grid.append("\n");
            }
        }
        if (delimBottom) {
            // Print divider after record
            printSeparator(grid, colWidths);
        }
        grid.setLength(grid.length() - 1); // cut last line feed
    }

    public void printSeparator(StringBuilder output, int[] columnWidth) {
        if (delimLeading) {
            output.append('+');
        }
        if (startOffset != 0) {
            output.append("-".repeat(Math.max(0, startOffset - 1)));
        }
        for (int i = 0; i < columnWidth.length; i++) {
            if (i > 0 || startOffset != 0) output.append('+');
            output.append("-".repeat(Math.max(0, columnWidth[i])));
        }
        if (delimTrailing) {
            output.append('+');
        }
        output.append('\n');
    }

    public void printQueryName(StringBuilder sb, String name) {
        sb.append("> ").append(name).append("\n");
    }

    private int getStringWidth(String str) {
        int width = 0;
        if (str != null && str.length() > 0) {
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '\t') {
                    width += prefs.getInt(ResultSetPreferences.RESULT_TEXT_TAB_SIZE);
                } else {
                    width++;
                }
            }
        }
        return width;
    }


    private static String getAttributeName(DBDAttributeBinding attr) {
        if (CommonUtils.isEmpty(attr.getLabel())) {
            return attr.getName();
        } else {
            return attr.getLabel();
        }
    }

}
