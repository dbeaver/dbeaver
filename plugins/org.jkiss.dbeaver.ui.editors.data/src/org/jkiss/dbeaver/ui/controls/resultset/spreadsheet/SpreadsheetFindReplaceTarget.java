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
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCellLocation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetValueController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Find/Replace target for result set viewer
 */
class SpreadsheetFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    private static final Log log = Log.getLog(SpreadsheetFindReplaceTarget.class);
    private static SpreadsheetFindReplaceTarget instance;

    /** Uses {@link Object#hashCode()} to identity the current owner and determine whether he was changed or not. */
    private int ownerIdentity;
    private Pattern searchPattern;
    private Color scopeHighlightColor;
    private boolean replaceAll;
    private boolean sessionActive = false;
    private List<GridPos> originalSelection = new ArrayList<>();

    public static synchronized SpreadsheetFindReplaceTarget getInstance() {
        if (instance == null) {
            instance = new SpreadsheetFindReplaceTarget();
            instance.scopeHighlightColor = UIStyles.getDefaultTextColor("AbstractTextEditor.Color.FindScope", SWT.COLOR_LIST_SELECTION);
        }
        return instance;
    }

    public synchronized SpreadsheetFindReplaceTarget owned(@NotNull SpreadsheetPresentation newOwner) {
        refreshOwner(newOwner);
        return this;
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
    public boolean canPerformFind()
    {
        return true;
    }

    @Override
    public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord)
    {
        return findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord, false);
    }

    @Override
    public Point getSelection()
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return new Point(0, 0);
        }
        Collection<Integer> rowSelection = owner.getSpreadsheet().getRowSelection();
        int minRow = rowSelection.stream().mapToInt(v -> v).min().orElse(-1);
        int maxRow = rowSelection.stream().mapToInt(v -> v).max().orElse(-1);

        return new Point(minRow, maxRow);
    }

    @Override
    public String getSelectionText()
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return "";
        }
        GridPos selection = owner.getSelection().getFirstElement();
        if (selection == null) {
            return "";
        }
        Spreadsheet spreadsheet = owner.getSpreadsheet();
        GridCell cell = spreadsheet.posToCell(selection);
        String value = cell == null ? "" : CommonUtils.toString(spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false));
        return CommonUtils.toString(value);
    }

    @Override
    public boolean isEditable()
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        return owner != null && owner.getController().getReadOnlyStatus() == null;
    }

    @Override
    public void replaceSelection(String text)
    {
        replaceSelection(text, false);
    }

    @Override
    public void beginSession()
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet(false);
        if (owner == null) {
            return;
        }
        this.sessionActive = true;
        owner.getControl().redraw();
        this.originalSelection = new ArrayList<>(owner.getSpreadsheet().getSelection());
        owner.highlightRows(-1, -1, null);
    }

    @Override
    public void endSession()
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet(false);
        if (owner == null) {
            return;
        }
        this.sessionActive = false;
        this.searchPattern = null;
        Control control = owner.getControl();
        if (control != null && !control.isDisposed()) {
            owner.getSpreadsheet().deselectAll();
            owner.getSpreadsheet().selectCells(this.originalSelection);
        }
    }

    @Override
    public IRegion getScope()
    {
        return null;
    }

    @Override
    public void setScope(IRegion scope) {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return;
        }
        if (scope == null || scope.getLength() == 0) {
            owner.highlightRows(-1, -1, null);
            if (scope == null) {
                owner.getSpreadsheet().deselectAll();
                owner.getSpreadsheet().selectCells(this.originalSelection);
            }
        } else {
            owner.highlightRows(scope.getOffset(), scope.getLength(), scopeHighlightColor);
        }
    }

    @Override
    public Point getLineSelection()
    {
        return getSelection();
    }

    @Override
    public void setSelection(int offset, int length)
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return;
        }
        int columnCount = owner.getSpreadsheet().getColumnCount();
        List<GridPos> selRows = new ArrayList<>();
        for (int rowNum = 0; rowNum < length; rowNum++) {
            for (int col = 0; col < columnCount; col++) {
                selRows.add(new GridPos(col, offset + rowNum));
            }
        }
        owner.setSelection(
            new StructuredSelection(selRows));
    }

    @Override
    public void setScopeHighlightColor(Color color)
    {
        this.scopeHighlightColor = color;
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll)
    {
        this.replaceAll = replaceAll;
    }

    @Override
    public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch)
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return - 1;
        }
        searchPattern = null;

        ResultSetModel model = owner.getController().getModel();
        if (model.isEmpty()) {
            return -1;
        }
        Spreadsheet spreadsheet = owner.getSpreadsheet();
        int rowCount = spreadsheet.getItemCount();
        int columnCount = spreadsheet.getColumnCount();
        Collection<GridPos> selection = spreadsheet.getSelection();
        int firstRow = owner.getHighlightScopeFirstLine();
        if (firstRow < 0) firstRow = 0;
        int lastRow = owner.getHighlightScopeLastLine();
        if (lastRow >= rowCount || lastRow < 0) lastRow = rowCount - 1;

        GridPos startPosition = selection.isEmpty() ? null : selection.iterator().next();
        if (startPosition == null) {
            int startRow = searchForward ? firstRow : lastRow;
            if (startRow >= 0) {
                startPosition = new GridPos(0, startRow);
            } else {
                // From the beginning
                startPosition = new GridPos(0, 0);
            }
        }
        Pattern findPattern;
        if (regExSearch) {
            try {
                findPattern = Pattern.compile(findString, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                log.warn("Bad regex pattern: " + findString);
                return -1;
            }
        } else {
            String pattern = Pattern.quote(findString);
            if (wholeWord) {
                pattern = "\\b" + pattern + "\\b";
            }
            findPattern = Pattern.compile(pattern, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        }
        int minColumnNum = owner.getController().isRecordMode() ? -1 : 0;
        for (GridPos curPosition = new GridPos(startPosition);;) {
            //Object element = contentProvider.getElement(curPosition);
            if (searchForward) {
                curPosition.col++;
                if (curPosition.col >= columnCount) {
                    curPosition.col = minColumnNum;
                    curPosition.row++;
                }
            } else {
                curPosition.col--;
                if (curPosition.col < minColumnNum) {
                    curPosition.col = columnCount - 1;
                    curPosition.row--;
                }
            }
            if ((firstRow >= 0 && curPosition.row < firstRow) || (lastRow >= 0 && curPosition.row > lastRow)) {
                if (offset == -1) {
                    // Wrap search - redo search one more time
                    offset = 0;
                    if (searchForward) {
                        curPosition = new GridPos(0, firstRow);
                    } else {
                        curPosition = new GridPos(columnCount - 1, lastRow);
                    }
                } else {
                    // Not found
                    spreadsheet.redraw();
                    return -1;
                }
            }
            String cellText;
            if (owner.getController().isRecordMode() && curPosition.col == minColumnNum) {
                // Header
                cellText = spreadsheet.getLabelProvider().getText(spreadsheet.getRow(curPosition.row));
            } else {
                GridCell cell = spreadsheet.posToCell(curPosition);
                if (cell != null) {
                    cellText = CommonUtils.toString(spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false));
                } else {
                    continue;
                }
            }
            Matcher matcher = findPattern.matcher(cellText);
            if (wholeWord ? matcher.matches() : matcher.find()) {
                if (curPosition.col == minColumnNum) {
                    curPosition.col = 0;
                }
                spreadsheet.setFocusColumn(curPosition.col);
                spreadsheet.setFocusItem(curPosition.row);
                spreadsheet.setCellSelection(curPosition);
                if (!owner.getController().isHasMoreData() || !replaceAll || (curPosition.row >= spreadsheet.getTopIndex() && curPosition.row < spreadsheet.getBottomIndex())) {
                    // Do not scroll to invisible rows to avoid scrolling and slow update
                    spreadsheet.showSelection();
                }
                searchPattern = findPattern;
                return curPosition.row;
            }
        }
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return;
        }
        GridPos selection = owner.getSelection().getFirstElement();
        if (selection == null) {
            return;
        }
        GridCell cell = owner.getSpreadsheet().posToCell(selection);
        if (cell == null) {
            return;
        }
        ResultSetCellLocation cellLocation = owner.getCellLocation(cell);

        String oldValue = CommonUtils.toString(owner.getSpreadsheet().getContentProvider().getCellValue(
            cell.col, cell.row, true));
        String newValue = text;
        if (searchPattern != null) {
            newValue = searchPattern.matcher(oldValue).replaceAll(newValue);
        }

        final Object originalValue = owner.getSpreadsheet().getContentProvider().getCellValue(
            cell.col, cell.row, false);
        try {
            if (originalValue instanceof DBDContent) {
                ((DBDContent) originalValue)
                    .updateContents(new VoidProgressMonitor(), new StringContentStorage(newValue));
                new ResultSetValueController(owner.getController(), cellLocation, IValueController.EditType.NONE, null)
                    .updateValue(originalValue, true);
            } else {
                owner.getController().updateCellValue(
                    cellLocation.getAttribute(),
                    cellLocation.getRow(),
                    cellLocation.getRowIndexes(),
                    newValue,
                    true);
            }
        } catch (DBException e) {
            log.error("Error updating contents", e);
        }

        owner.getController().updatePanelsContent(false);
    }

    @Override
    public String toString()
    {
        final SpreadsheetPresentation owner = getActiveSpreadsheet();
        if (owner == null) {
            return super.toString();
        }
        DBSDataContainer dataContainer = owner.getController().getDataContainer();
        return "Target: " + (dataContainer == null ? null : dataContainer.getName());
    }

    private void refreshOwner(@NotNull SpreadsheetPresentation newOwner) {
        if (this.ownerIdentity == newOwner.hashCode()) {
            return;
        }
        final boolean refreshSession = this.sessionActive;
        final Pattern searchPattern = this.searchPattern;
        if (refreshSession) {
            this.endSession();
        }
        this.ownerIdentity = newOwner.hashCode();
        if (refreshSession) {
            this.beginSession();
            this.searchPattern = searchPattern;
        }
    }

    @Nullable
    private SpreadsheetPresentation getActiveSpreadsheet() {
        return getActiveSpreadsheet(true);
    }

    @Nullable
    private SpreadsheetPresentation getActiveSpreadsheet(boolean refreshActiveSpreadsheet) {
        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            return null;
        }
        final IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return null;
        }
        final SpreadsheetPresentation spreadsheet = activeEditor.getAdapter(SpreadsheetPresentation.class);
        if (spreadsheet == null) {
            return null;
        }
        if (refreshActiveSpreadsheet) {
            refreshOwner(spreadsheet);
        }
        return spreadsheet;
    }
}
