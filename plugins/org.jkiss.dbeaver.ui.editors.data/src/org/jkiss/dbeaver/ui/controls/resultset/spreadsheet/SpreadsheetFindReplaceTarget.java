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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCellLocation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetValueController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Find/Replace target for result set viewer
 */
class SpreadsheetFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    private static final Log log = Log.getLog(SpreadsheetFindReplaceTarget.class);

    @NotNull
    private final Object REDRAW_SYNC = new Object();

    /**
     * Uses {@link Object#hashCode()} to identity the current owner and determine whether he was changed or not.
     */
    private int ownerIdentity;
    private Pattern searchPattern;
    private Color scopeHighlightColor;
    private boolean replaceAll;
    private boolean sessionActive = false;
    private boolean firstSearchInSession = true;
    private List<GridPos> originalSelection = new ArrayList<>();
    private final Set<DBDValueRow> updatedRows = new LinkedHashSet<>();
    private final Set<DBDAttributeBinding> updatedAttributes = new LinkedHashSet<>();
    private final Set<GridPos> processedCells = new HashSet<>();
    private AbstractJob redrawJob = null;
    private String currentFindString = "";
    private boolean currentCaseSensitive;
    private boolean currentWholeWord;
    private boolean currentRegEx;

    @NotNull
    private final Spreadsheet spreadsheet;

    public SpreadsheetFindReplaceTarget(@NotNull Spreadsheet spreadsheet) {
        this.scopeHighlightColor = UIStyles.getDefaultTextColor("AbstractTextEditor.Color.FindScope", SWT.COLOR_LIST_SELECTION);
        this.spreadsheet = spreadsheet;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public Pattern getSearchPattern() {
        return searchPattern;
    }

    public Color getScopeHighlightColor() {
        return scopeHighlightColor;
    }

    @Override
    public boolean canPerformFind() {
        return true;
    }

    @NotNull
    @Override
    public Point getSelection() {
        Collection<Integer> rowSelection = this.spreadsheet.getRowSelection();
        int minRow = rowSelection.stream().mapToInt(v -> v).min().orElse(-1);
        int maxRow = rowSelection.stream().mapToInt(v -> v).max().orElse(-1);

        return new Point(minRow, maxRow);
    }

    @NotNull
    @Override
    public String getSelectionText() {
        GridPos selection = this.spreadsheet.getPresentation().getSelection().getFirstElement();
        if (selection == null) {
            return "";
        }
        GridCell cell = this.spreadsheet.posToCell(selection);
        String value = cell == null ? "" : CommonUtils.toString(spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false));
        return CommonUtils.toString(value);
    }

    @Override
    public boolean isEditable() {
        return this.spreadsheet.getPresentation().getController().getReadOnlyStatus() == null;
    }

    @Override
    public void beginSession() {
        synchronized (REDRAW_SYNC) {
            updatedRows.clear();
            updatedAttributes.clear();
        }
        processedCells.clear();

        SpreadsheetPresentation owner = this.spreadsheet.getPresentation();
        this.sessionActive = true;
        owner.getControl().redraw();
        this.originalSelection = new ArrayList<>(this.spreadsheet.getSelection());
        owner.highlightRows(-1, -1, null);
    }

    @Override
    public void endSession() {
        this.sessionActive = false;
        this.searchPattern = null;
        this.firstSearchInSession = true;
        if (!this.spreadsheet.isDisposed()) {
            this.spreadsheet.deselectAll();
            this.spreadsheet.selectCells(this.originalSelection);
            this.spreadsheet.redraw();
        }
    }

    @Override
    public IRegion getScope() {
        return null;
    }

    @Override
    public void setScope(@Nullable IRegion scope) {
        if (scope == null || scope.getLength() == 0) {
            this.spreadsheet.getPresentation().highlightRows(-1, -1, null);
            if (scope == null) {
                this.spreadsheet.deselectAll();
                this.spreadsheet.selectCells(this.originalSelection);
                this.spreadsheet.redraw();
            }
        } else {
            this.spreadsheet.getPresentation().highlightRows(scope.getOffset(), scope.getLength(), scopeHighlightColor);
        }
    }

    @Override
    public Point getLineSelection() {
        return getSelection();
    }

    @Override
    public void setSelection(int offset, int length) {
        int columnCount = this.spreadsheet.getColumnCount();
        List<GridPos> selRows = new ArrayList<>();
        for (int rowNum = 0; rowNum < length; rowNum++) {
            for (int col = 0; col < columnCount; col++) {
                selRows.add(new GridPos(col, offset + rowNum));
            }
        }
        this.spreadsheet.getPresentation().setSelection(new StructuredSelection(selRows));
    }

    @Override
    public void setScopeHighlightColor(Color color) {
        this.scopeHighlightColor = color;
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll) {
        this.replaceAll = replaceAll;
    }

    @Override
    public void replaceSelection(String text) {
        replaceSelection(text, false);
    }

    @Override
    public void replaceSelection(
        @NotNull String text,
        boolean regExReplace
    ) {
        // Lazy initialization of search pattern
        if (searchPattern == null && !currentFindString.isEmpty()) {
            searchPattern = createSearchPattern(
                currentFindString,
                currentCaseSensitive,
                currentWholeWord,
                currentRegEx
            );
        }

        GridPos selection = this.spreadsheet.getPresentation().getSelection().getFirstElement();
        if (selection == null) {
            return;
        }

        if (replaceAll && processedCells.contains(selection)) {
            return;
        }

        GridCell cell = this.spreadsheet.posToCell(selection);
        if (cell == null) {
            return;
        }

        ResultSetCellLocation cellLocation = this.spreadsheet.getPresentation().getCellLocation(cell);
        String oldValue = CommonUtils.toString(this.spreadsheet.getContentProvider().getCellValue(
            cell.col, cell.row, true));
        String newValue = oldValue;

        if (searchPattern != null) {
            newValue = searchPattern.matcher(oldValue).replaceAll(text);
        }

        IResultSetController controller = this.spreadsheet.getPresentation().getController();
        try {
            if (oldValue.equals(newValue)) {
                return;
            }

            Object originalValue = this.spreadsheet.getContentProvider().getCellValue(
                cell.col, cell.row, false);

            if (originalValue instanceof DBDContent content) {
                // Special handling for content/blob values
                content.updateContents(new VoidProgressMonitor(), new StringContentStorage(newValue));
                new ResultSetValueController(controller, cellLocation, IValueController.EditType.NONE, null)
                    .updateValue(originalValue, !replaceAll);
            } else {
                // Standard value update
                // TODO introduce value path here
                controller.updateCellValue(
                    cellLocation.getAttribute(),
                    cellLocation.getRow(),
                    cellLocation.getRowIndexes(),
                    newValue,
                    !replaceAll
                );
            }

            if (replaceAll) {
                processedCells.add(selection);
            }
        } catch (DBException e) {
            log.error("Error updating cell value", e);
        } finally {
            if (replaceAll) {
                searchPattern = null;
                currentFindString = "";
            }
        }

        GridPos currentPos = this.spreadsheet.getFocusPos();
        storeLastFoundPosition(currentPos);
        if (!replaceAll) {
            controller.redrawData(true, true);
            synchronized (REDRAW_SYNC) {
                updatedAttributes.add(cellLocation.getAttribute());
                updatedRows.add(cellLocation.getRow());
                if (redrawJob == null) {
                    redrawJob = new AbstractJob("Redraw grid after replace") {
                        @NotNull
                        @Override
                        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                            Set<DBDAttributeBinding> attrs;
                            Set<DBDValueRow> rows;
                            synchronized (REDRAW_SYNC) {
                                attrs = new LinkedHashSet<>(updatedAttributes);
                                rows = new LinkedHashSet<>(updatedRows);
                                updatedAttributes.clear();
                                updatedRows.clear();
                                redrawJob = null;
                            }
                            UIUtils.syncExec(() -> {
                                controller.refreshHintCache(attrs, rows, null);
                                controller.redrawData(false, true);
                                controller.updatePanelsContent(false);
                            });
                            return Status.OK_STATUS;
                        }
                    };
                    redrawJob.schedule(150);
                }
            }
        }
    }

    @Override
    public int findAndSelect(
        int widgetOffset,
        @NotNull String findString,
        boolean searchForward,
        boolean caseSensitive,
        boolean wholeWord
    ) {
        return findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord, false);
    }

    @Override
    public int findAndSelect(
        int offset,
        @NotNull String findString,
        boolean searchForward,
        boolean caseSensitive,
        boolean wholeWord,
        boolean regExSearch
    ) {
        this.currentFindString = findString;
        this.currentCaseSensitive = caseSensitive;
        this.currentWholeWord = wholeWord;
        this.currentRegEx = regExSearch;

        final SpreadsheetPresentation owner = this.spreadsheet.getPresentation();
        ResultSetModel model = owner.getController().getModel();
        if (model.isEmpty()) {
            return -1;
        }

        int rowCount = this.spreadsheet.getItemCount();
        int columnCount = this.spreadsheet.getColumnCount();

        // Handle record mode special column (-1 indicates record selector column)
        boolean recordMode = owner.getController().isRecordMode();
        int minColumn = recordMode ? -1 : 0;

        int firstRow = Math.max(owner.getHighlightScopeFirstLine(), 0);
        int lastRow = Math.min(owner.getHighlightScopeLastLine(), rowCount - 1);
        if (lastRow < 0) {
            lastRow = rowCount - 1;
        }

        GridPos startPos = getStartPosition(
            spreadsheet,
            searchForward,
            firstRow,
            lastRow,
            minColumn,
            columnCount,
            firstSearchInSession
        );

        if (firstSearchInSession) {
            firstSearchInSession = false;
            storeLastFoundPosition(startPos);
        }

        Pattern pattern = createSearchPattern(currentFindString, currentCaseSensitive, currentWholeWord, currentRegEx);
        if (pattern == null) {
            return -1;
        }
        this.searchPattern = pattern;

        GridPos currentPos = new GridPos(startPos.col, startPos.row);
        boolean wrapped = false;
        int totalCells = (lastRow - firstRow + 1) * (columnCount - minColumn);
        int checked = 0;

        while (checked <= totalCells) {
            if (isCellInScope(currentPos, firstRow, lastRow, minColumn, columnCount)) {
                if (!replaceAll || !processedCells.contains(currentPos)) {
                    String cellText = getCellText(spreadsheet, currentPos, recordMode, minColumn);
                    if (cellText != null && pattern.matcher(cellText).find()) {
                        selectCell(currentPos, minColumn);
                        storeLastFoundPosition(currentPos);
                        return currentPos.row;
                    }
                }
                checked++;
            }

            currentPos = getNextPosition(currentPos, searchForward, columnCount, minColumn, firstRow, lastRow);

            // Handle search wrap-around
            if (!isCellInScope(currentPos, firstRow, lastRow, minColumn, columnCount)) {
                if (wrapped) { // Prevent infinite loop
                    break;
                }
                currentPos = getWrapAroundPosition(searchForward, firstRow, lastRow, minColumn, columnCount);
                wrapped = true;
            }
        }
        processedCells.clear();
        return -1; // No matches found
    }

    private Pattern createSearchPattern(
        @NotNull String findString,
        boolean caseSensitive,
        boolean wholeWord,
        boolean regEx
    ) {
        if (findString.isEmpty()) {
            return null;
        }

        try {
            if (regEx) {
                return Pattern.compile(findString, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } else {
                String pattern = wholeWord
                    ? "\\b" + Pattern.quote(findString) + "\\b"
                    : Pattern.quote(findString);
                return Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            }
        } catch (PatternSyntaxException e) {
            log.error("Invalid search pattern: " + findString, e);
            return null;
        }
    }

    // Helper method to determine initial search position
    private GridPos getStartPosition(
        @NotNull Spreadsheet spreadsheet,
        boolean searchForward,
        int firstRow,
        int lastRow,
        int minColumn,
        int columnCount,
        boolean isFirstSearch
    ) {
        GridPos currentPos = spreadsheet.getCursorPosition();

        // Default to boundary positions when no cursor
        if (currentPos == null) {
            return searchForward ?
                new GridPos(minColumn, firstRow) :  // Start from top-left
                new GridPos(columnCount - 1, lastRow); // Start from bottom-right
        }

        // Use current position for first search, next position otherwise
        return isFirstSearch ? currentPos :
            getNextPosition(currentPos, searchForward, columnCount, minColumn, firstRow, lastRow);
    }

    // Stores the last found position for subsequent operations
    private void storeLastFoundPosition(@NotNull GridPos pos) {
        // Adjust column for record mode
        if (this.spreadsheet.getPresentation().getController().isRecordMode() && pos.col == -1) {
            pos = new GridPos(0, pos.row);
        }
        // Update spreadsheet focus and selection
        this.spreadsheet.setFocusColumn(pos.col);
        this.spreadsheet.setFocusItem(pos.row);
        this.spreadsheet.setCellSelection(pos);
        this.spreadsheet.showSelection();
    }

    // Retrieves cell text based on mode and position
    private String getCellText(
        @NotNull Spreadsheet spreadsheet,
        @NotNull GridPos pos,
        boolean recordMode,
        int minColumn
    ) {
        // Handle record mode special column
        if (recordMode && pos.col == minColumn) {
            return spreadsheet.getLabelProvider().getText(spreadsheet.getRow(pos.row));
        }
        // Standard cell value retrieval
        GridCell cell = spreadsheet.posToCell(pos);
        return cell != null ?
            CommonUtils.toString(spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false)) :
            null;
    }

    // Selects a cell and updates UI focus
    private void selectCell(
        @NotNull GridPos pos,
        int minColumn
    ) {
        // Adjust position for record mode
        if (pos.col == minColumn) {
            pos = new GridPos(0, pos.row);
        }
        // Update spreadsheet state
        this.spreadsheet.setFocusColumn(pos.col);
        this.spreadsheet.setFocusItem(pos.row);
        this.spreadsheet.setCellSelection(pos);
        this.spreadsheet.showSelection();
    }

    // Calculates next cell position based on search direction
    private GridPos getNextPosition(
        @NotNull GridPos pos,
        boolean searchForward,
        int columnCount,
        int minColumn,
        int firstRow,
        int lastRow
    ) {
        GridPos next = new GridPos(pos.col, pos.row);

        if (searchForward) {
            // Move right, wrap to next row
            next.col++;
            if (next.col >= columnCount) {
                next.col = minColumn;
                next.row = (next.row >= lastRow) ? firstRow : next.row + 1;
            }
        } else {
            // Move left, wrap to previous row
            next.col--;
            if (next.col < minColumn) {
                next.col = columnCount - 1;
                next.row = (next.row <= firstRow) ? lastRow : next.row - 1;
            }
        }
        return next;
    }

    // Returns wrap-around position when reaching boundary
    private GridPos getWrapAroundPosition(
        boolean searchForward,
        int firstRow,
        int lastRow,
        int minColumn,
        int columnCount
    ) {
        return searchForward ?
            new GridPos(minColumn, firstRow) :  // Top-left corner
            new GridPos(columnCount - 1, lastRow); // Bottom-right corner
    }

    private boolean isCellInScope(
        GridPos pos,
        int firstRow,
        int lastRow,
        int minColumn,
        int columnCount
    ) {
        return pos.row >= firstRow &&
               pos.row <= lastRow &&
               pos.col >= minColumn &&
               pos.col < columnCount;
    }

    @NotNull
    @Override
    public String toString() {
        DBSDataContainer dataContainer = this.spreadsheet.getPresentation().getController().getDataContainer();
        return "Target: " + (dataContainer == null ? null : dataContainer.getName());
    }
}
