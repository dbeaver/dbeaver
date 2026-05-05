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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.accessibility.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridLabelProvider;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.util.Map;

public class SpreadsheetAccessibleAdapter extends AccessibleControlAdapter implements AccessibleListener {
    private static final Map<String, String> lobMimeTypeNames = Map.of(
        MimeTypes.TEXT_HTML, "html",
        MimeTypes.TEXT_XML, "xml",
        MimeTypes.TEXT_CSS, "css",
        MimeTypes.TEXT_JSON, "json",
        MimeTypes.APPLICATION_JSON, "json",
        MimeTypes.OCTET_STREAM, "blob",
        MimeTypes.MULTIPART_ANY, "multipart",
        MimeTypes.MULTIPART_RELATED, "multipart"
    );

    private static final boolean READ_VALUE = false;

    private static GridCell lastCell;

    private final Spreadsheet spreadsheet;
    private final GridPos lastPosition;

    private IGridLabelProvider labelProvider;
    private IGridContentProvider contentProvider;
    private GridPos position;
    private GridCell cell;
    private Object value;
    private int rowsCount;
    private int colsCount;
    private int cellsCount;
    private String valueStr;
    private String valueType = "";

    private SpreadsheetAccessibleAdapter(@NotNull Spreadsheet spreadsheet) {
        this.spreadsheet = spreadsheet;
        this.lastPosition = new GridPos(-1, -1);
        this.labelProvider = spreadsheet.getLabelProvider();
        this.contentProvider = spreadsheet.getContentProvider();
    }

    public static void install(@NotNull Spreadsheet spreadsheet) {
        final Accessible accessible = spreadsheet.getAccessible();
        final SpreadsheetAccessibleAdapter adapter = new SpreadsheetAccessibleAdapter(spreadsheet);

        accessible.addAccessibleListener(adapter);
        accessible.addAccessibleControlListener(adapter);

        spreadsheet.addCursorChangeListener(event -> {
            final GridCell cell = spreadsheet.getFocusCell();
            if (cell != null && cell != lastCell) {
                adapter.setNewCell(cell);
                accessible.sendEvent(ACC.EVENT_NAME_CHANGED, new Object[]{null, cell.getRow().getElement()});
            }
        });
    }

    @Override
    public void getValue(AccessibleControlEvent e) {
        if (!READ_VALUE) {
            return;
        }

        if (cellsCount == 1) {
            if (cell == null) {
                return;
            }

            valueStr = "";
            valueType = "";

            readValueStringAndType(contentProvider, cell, value);

            if (lastPosition.col != position.col && lastPosition.row == position.row) {
                e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_grid_value_col, new Object[]{
                    valueStr,
                    labelProvider.getText(cell.getColumn()),
                });
            } else if (lastPosition.row != position.row && lastPosition.col == position.col) {
                e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_grid_value_row, new Object[]{
                    valueStr,
                    labelProvider.getText(cell.getRow()),
                });
            } else {
                e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_grid_value_row_col, new Object[]{
                    valueStr,
                    labelProvider.getText(cell.getRow()),
                    labelProvider.getText(cell.getColumn()),
                    valueType
                });
            }
            e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_columns_selected, colsCount);
        } else if (colsCount == 1) {
            e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_rows_selected, rowsCount);
        } else {
            e.result = DataEditorsMessages.spreadsheet_accessibility_freeform_range_selected;
        }
    }

    // This is a duplicate of the getValue() method. Because for some reason,
    // some accessibility tools (like JAWs) use only results from the getName() method.
    @Override
    public void getName(AccessibleEvent e) {
        if (cellsCount == 1) {
            if (cell == null) {
                return;
            }

            spreadsheet.setAccessibilityEnabled(true);
            valueStr = "";
            valueType = "";

            readValueStringAndType(contentProvider, cell, value);

            if (lastPosition.col != position.col && lastPosition.row == position.row) {
                e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_grid_value_col, new Object[]{
                    valueStr,
                    labelProvider.getText(cell.getColumn()),
                });
            } else if (lastPosition.row != position.row && lastPosition.col == position.col) {
                e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_grid_value_row, new Object[]{
                    valueStr,
                    labelProvider.getText(cell.getRow()),
                });
            } else if (!lastPosition.equals(position)) {
                e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_grid_value_row_col, new Object[]{
                    valueStr,
                    labelProvider.getText(cell.getRow()),
                    labelProvider.getText(cell.getColumn()),
                    valueType
                });
            } else {
                e.result = valueStr;
            }
        } else if (rowsCount == 1) {
            e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_columns_selected, colsCount);
        } else if (colsCount == 1) {
            e.result = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_rows_selected, rowsCount);
        } else {
            e.result = DataEditorsMessages.spreadsheet_accessibility_freeform_range_selected;
        }
    }

    private void setNewCell(GridCell cell) {
        if (position != null) {
            lastPosition.col = position.col;
            lastPosition.row = position.row;
        }

        lastCell = cell;
        this.cell = cell;
        position = new GridPos(spreadsheet.getCursorPosition());

        value = contentProvider.getCellValue(cell.getColumn(), cell.getRow(), false);

        rowsCount = spreadsheet.getRowSelectionSize();
        colsCount = spreadsheet.getColumnSelectionSize();
        cellsCount = spreadsheet.getCellSelectionSize();
    }

    private void readValueStringAndType(@NotNull IGridContentProvider contentProvider, @NotNull GridCell cell, Object value) {
        final String contentType = value instanceof DBDContent ? ((DBDContent) value).getContentType() : null;
        final String collectionType = value instanceof DBDCollection ? ((DBDCollection) value).getComponentType().getName() : null;

        if (contentType != null && lobMimeTypeNames.get(contentType) != null) {
            valueStr = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_object_of_type, lobMimeTypeNames.get(contentType));
        } else if (collectionType != null) {
            valueStr = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_collection_of_type, collectionType);
        } else if (value instanceof Boolean) {
            valueType = DataEditorsMessages.spreadsheet_accessibility_boolean;
            valueStr = value.toString();
        } else {
            if (value instanceof String) {
                valueType = DataEditorsMessages.spreadsheet_accessibility_string;
            } else if (value instanceof Number) {
                valueType = DataEditorsMessages.spreadsheet_accessibility_numeric;
            }
            valueStr = contentProvider.getCellValue(cell.getColumn(), cell.getRow(), true).toString();
        }

        if (valueStr.isEmpty()) {
            valueStr = DataEditorsMessages.spreadsheet_accessibility_empty_string;
        }

        if (contentProvider.isElementReadOnly(cell.getColumn())) {
            valueType = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_readonly, valueType);
        }

        final String valueLink = contentProvider.getCellLinkText(cell.col, cell.row);
        if (!valueLink.isEmpty()) {
            valueType = NLS.bind(DataEditorsMessages.spreadsheet_accessibility_foreign_key, valueType, valueLink);
        }
    }

    @Override
    public void getHelp(AccessibleEvent e) {
        // not implemented

    }

    @Override
    public void getKeyboardShortcut(AccessibleEvent e) {
        // not implemented
    }

    @Override
    public void getDescription(AccessibleEvent e) {
        e.result = DataEditorsMessages.spreadsheet_accessibility_description;
    }
}
