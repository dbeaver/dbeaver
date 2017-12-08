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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * TableColumnSortListener
 */
public class TableColumnSortListener implements Listener {
    private final Table table;
    private final int columnIndex;
    private int sortDirection = SWT.DOWN;
    private TableColumn prevColumn = null;

    public TableColumnSortListener(Table table, int columnIndex) {
        this.table = table;
        this.columnIndex = columnIndex;
    }

    private static void sortTable(Table table, Comparator<TableItem> comparator)
    {
        int columnCount = table.getColumnCount();
        String[] values = new String[columnCount];
        TableItem[] items = table.getItems();
        for (int i = 1; i < items.length; i++) {
            for (int j = 0; j < i; j++) {
                TableItem item = items[i];
                if (comparator.compare(item, items[j]) < 0) {
                    for (int k = 0; k < columnCount; k++) {
                        values[k] = item.getText(k);
                    }
                    Object data = item.getData();
                    boolean checked = item.getChecked();
                    item.dispose();

                    item = new TableItem(table, SWT.NONE, j);
                    item.setText(values);
                    item.setData(data);
                    item.setChecked(checked);
                    items = table.getItems();
                    break;
                }
            }
        }
    }

    @Override
    public void handleEvent(Event e) {
        final Collator collator = Collator.getInstance(Locale.getDefault());
        TableColumn column = (TableColumn) e.widget;
        if (prevColumn == column) {
            // Set reverse order
            sortDirection = (sortDirection == SWT.UP ? SWT.DOWN : SWT.UP);
        }
        prevColumn = column;
        this.table.setSortColumn(column);
        this.table.setSortDirection(sortDirection);
        sortTable(this.table, (e1, e2) -> {
            int mul = (sortDirection == SWT.UP ? 1 : -1);
            String text1 = e1.getText(columnIndex);
            String text2 = e2.getText(columnIndex);
            try {
                Double num1 = getNumberFromString(text1);
                if (num1 != null) {
                    Double num2 = getNumberFromString(text2);
                    if (num2 != null) {
                        return (int)(num1 - num2) * mul;
                    }
                }
            } catch (NumberFormatException e3) {
                // Ignore
            }
            return collator.compare(text1, text2) * mul;
        });
    }

    private static Double getNumberFromString(String str) {
        if (str.isEmpty()) return null;
        if (!Character.isDigit(str.charAt(0))) return null;
        int numLength = 1;
        for (; numLength < str.length(); numLength++) {
            if (Character.isWhitespace(str.charAt(numLength))) {
                break;
            }
        }
        if (numLength == str.length()) {
            return Double.parseDouble(str);
        } else {
            return Double.parseDouble(str.substring(0, numLength));
        }
    }
}
