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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridItem;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Spreadsheet command handler.
 * Active when focus is in spreadsheet control
 */
public class SpreadsheetCommandHandler extends AbstractHandler {

    public static final String CMD_COLUMNS_FIT_VALUE = "org.jkiss.dbeaver.core.resultset.grid.columnsFitValue";
    public static final String CMD_COLUMNS_FIT_SCREEN = "org.jkiss.dbeaver.core.resultset.grid.columnsFitScreen";
    public static final String CMD_COLUMNS_HIDE_EMPTY = "org.jkiss.dbeaver.core.resultset.grid.columnsHideEmpty";
    public static final String CMD_SELECT_COLUMNS = "org.jkiss.dbeaver.core.resultset.grid.selectColumn";
    public static final String CMD_SELECT_ROWS = "org.jkiss.dbeaver.core.resultset.grid.selectRow";
    public static final String CMD_MOVE_COLUMNS_RIGHT = "org.jkiss.dbeaver.core.resultset.grid.moveColumnRight";
    public static final String CMD_MOVE_COLUMNS_LEFT = "org.jkiss.dbeaver.core.resultset.grid.moveColumnLeft";
    public static final String CMD_HIDE_COLUMNS = "org.jkiss.dbeaver.core.resultset.grid.hideColumns";
    public static final String CMD_SHOW_COLUMNS = "org.jkiss.dbeaver.core.resultset.grid.showColumns";
    public static final String CMD_SHOW_COLUMN_CONTEXT_MENU = "org.jkiss.dbeaver.core.resultset.grid.showColumnContextMenu";

    public static SpreadsheetPresentation getActiveSpreadsheet(ExecutionEvent event)
    {
        IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet != null) {
            IResultSetPresentation presentation = resultSet.getActivePresentation();
            if (presentation instanceof SpreadsheetPresentation) {
                return (SpreadsheetPresentation) presentation;
            }
        }

        return null;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        SpreadsheetPresentation spreadsheet = getActiveSpreadsheet(event);
        if (spreadsheet == null) {
            return null;
        }

        String actionId = event.getCommand().getId();
        switch (actionId) {
            case CMD_COLUMNS_FIT_VALUE:
                spreadsheet.getSpreadsheet().packColumns(true);
                break;
            case CMD_COLUMNS_FIT_SCREEN:
                spreadsheet.getSpreadsheet().packColumns(false);
                break;
            case CMD_COLUMNS_HIDE_EMPTY: {
                final var model = spreadsheet.getController().getModel();
                final var attributes = new HashSet<>(model.getVisibleAttributes());
                for (ResultSetRow row : model.getAllRows()) {
                    attributes.removeIf(attribute -> !DBUtils.isNullValue(model.getCellValue(attribute, row)));
                }
                if (!attributes.isEmpty()) {
                    for (DBDAttributeBinding attribute : attributes) {
                        model.setAttributeVisibility(attribute, false);
                    }
                    spreadsheet.refreshData(true, false, true);
                }
                break;
            }
            case CMD_SELECT_COLUMNS: {
                Spreadsheet s = spreadsheet.getSpreadsheet();
                GridPos focusPos = s.getFocusPos();
                int rowsCount = s.getItemCount();
                List<IGridColumn> columnsSelection = s.getColumnSelection();
                Collection<GridPos> cellsToSelect = columnsSelection.stream().flatMap(
                    c -> IntStream.range(0, rowsCount).mapToObj(r -> new GridPos(c.getIndex(), r))
                ).collect(Collectors.toList());
                s.selectCells(cellsToSelect);
                s.resetFocus();
                s.setFocusColumn(focusPos.col);
                break;
            }
            case CMD_SELECT_ROWS: {
                Spreadsheet s = spreadsheet.getSpreadsheet();
                GridPos focusPos = s.getFocusPos();
                int columnsCount = s.getColumnCount();
                Collection<Integer> rowsSelection = s.getRowSelection();
                Collection<GridPos> cellsToSelect = rowsSelection.stream().flatMap(
                    r -> IntStream.range(0, columnsCount).mapToObj(c -> new GridPos(c, r))
                ).collect(Collectors.toList());
                s.selectCells(cellsToSelect);
                s.resetFocus();
                s.setFocusItem(focusPos.row);
                break;
            }
            case CMD_MOVE_COLUMNS_RIGHT: {
                Spreadsheet s = spreadsheet.getSpreadsheet();
                Collection<GridCell> selectedCells = s.getCellSelection();
                List<IGridColumn> selectedColumns = s.getColumnSelection();
                GridPos focusPos = s.getFocusPos();
                Object focusColumnElement = s.getFocusColumnElement();
                int rightmostColumnIndex = selectedColumns.stream().mapToInt(IGridColumn::getIndex).max().getAsInt();
                if (rightmostColumnIndex < s.getColumnCount() - 1) {
                    List<Object> columnsToMove = selectedColumns.stream().map(IGridItem::getElement).collect(Collectors.toList());
                    List<GridPos> cellsToSelect = selectedCells.stream().map(s::cellToPos)
                        .map(p -> new GridPos(p.col + 1, p.row)).collect(Collectors.toList());
                    if (spreadsheet.shiftColumns(columnsToMove, 1)) {
                        s.deselectAll();
                        s.selectCells(cellsToSelect);
                        s.resetFocus();
                        s.setFocusColumn(s.getColumnByElement(focusColumnElement).getIndex());
                        s.setFocusItem(focusPos.row);
                    }
                }
                break;
            }
            case CMD_MOVE_COLUMNS_LEFT: {
                Spreadsheet s = spreadsheet.getSpreadsheet();
                Collection<GridCell> selectedCells = s.getCellSelection();
                List<IGridColumn> selectedColumns = s.getColumnSelection();
                GridPos focusPos = s.getFocusPos();
                Object focusColumnElement = s.getFocusColumnElement();
                int leftmostColumnIndex = selectedColumns.stream().mapToInt(IGridColumn::getIndex).min().getAsInt();
                if (leftmostColumnIndex > 0) {
                    List<Object> columnsToMove = selectedColumns.stream().map(IGridItem::getElement).collect(Collectors.toList());
                    List<GridPos> cellsToSelect = selectedCells.stream().map(s::cellToPos)
                        .map(p -> new GridPos(p.col - 1, p.row)).collect(Collectors.toList());
                    if (spreadsheet.shiftColumns(columnsToMove, -1)) {
                        s.deselectAll();
                        s.selectCells(cellsToSelect);
                        s.resetFocus();
                        s.setFocusColumn(s.getColumnByElement(focusColumnElement).getIndex());
                        s.setFocusItem(focusPos.row);
                    }
                }
                break;
            }
            case CMD_HIDE_COLUMNS: {
                ResultSetModel model = spreadsheet.getController().getModel();
                List<IGridColumn> selectedColumns = spreadsheet.getSpreadsheet().getColumnSelection();
                if (selectedColumns.size() >= model.getVisibleAttributeCount()) {
                    UIUtils.showMessageBox(
                        spreadsheet.getControl().getShell(),
                        ResultSetMessages.controls_resultset_viewer_hide_columns_error_title,
                        ResultSetMessages.controls_resultset_viewer_hide_columnss_error_text, SWT.ERROR);
                } else {
                    for (IGridColumn selectedColumn : selectedColumns) {
                        model.setAttributeVisibility((DBDAttributeBinding) selectedColumn.getElement(), false);
                    }
                    spreadsheet.refreshData(true, false, true);
                }
                break;
            }
            case CMD_SHOW_COLUMNS: {
                ResultSetModel model = spreadsheet.getController().getModel();
                List<DBDAttributeBinding> hiddenAttributes = new ArrayList<>();
                List<DBDAttributeConstraint> constraints = model.getDataFilter().getConstraints();
                for (DBDAttributeConstraint ac : constraints) {
                    DBSAttributeBase attribute = ac.getAttribute();
                    if (!ac.isVisible() && attribute instanceof DBDAttributeBinding
                        && DBDAttributeConstraint.isVisibleByDefault((DBDAttributeBinding) attribute)
                    ) {
                        hiddenAttributes.add((DBDAttributeBinding) attribute);
                    }
                }
                for (DBDAttributeBinding attr : hiddenAttributes) {
                    model.setAttributeVisibility(attr, true);
                }
                spreadsheet.refreshData(true, false, true);
                break;
            }
            case CMD_SHOW_COLUMN_CONTEXT_MENU: {
                Spreadsheet s = spreadsheet.getSpreadsheet();
                GridCell focusCell = s.getFocusCell();
                Object focusColumnElement = s.getFocusColumnElement();
                if (focusCell != null) {
                    GridPos focusPos = s.cellToPos(focusCell);
                    Rectangle r = s.getCellBounds(focusPos.col, focusPos.row);
                    s.getMenu().setLocation(s.toDisplay(new Point(r.x + r.width, r.y)));
                } else if (focusColumnElement != null) {
                    IGridColumn focusColumn = s.getColumnByElement(focusColumnElement);
                    Rectangle r = s.getColumnBounds(focusColumn.getIndex());
                    s.getMenu().setLocation(s.toDisplay(new Point(r.x + r.width, r.y)));
                }
                s.setColumnContextMenuShouldBeShown(true);
                s.getMenu().setVisible(true);
                break;
            }
        }

        return null;
    }
}
