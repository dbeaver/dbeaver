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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.controls.bool.BooleanMode;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyleSet;
import org.jkiss.dbeaver.ui.controls.lightgrid.*;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController.RowPlacement;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetPropertyTester;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.panel.valueviewer.ValueViewerPanel;
import org.jkiss.dbeaver.ui.data.IMultiController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueEditorStandalone;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.properties.PropertySourceDelegate;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;
import org.jkiss.utils.xml.XMLUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spreadsheet presentation.
 * Visualizes results as grid.
 */
public class SpreadsheetPresentation extends AbstractPresentation
    implements IResultSetEditor, IResultSetDisplayFormatProvider, ISelectionProvider, IStatefulControl, DBPAdaptable, IGridController {
    public static final String PRESENTATION_ID = "spreadsheet";
    public static final EnumSet<DBDValueHint.HintType> INLINE_HINT_TYPES = EnumSet.of(
        DBDValueHint.HintType.STRING, DBDValueHint.HintType.ACTION, DBDValueHint.HintType.IMAGE);

    private static final Log log = Log.getLog(SpreadsheetPresentation.class);

    private Spreadsheet spreadsheet;

    @Nullable
    private DBDAttributeBinding curAttribute;
    private int columnOrder = SWT.DEFAULT;

    private final Map<SpreadsheetValueController, IValueEditorStandalone> openEditors = new HashMap<>();

    // UI modifiers
    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;
    private Color backgroundNormal;
    private Color backgroundOdd;
    private Color backgroundReadOnly;
    private Color foregroundDefault;
    private Color foregroundSelected, backgroundSelected;
    private Color foregroundNull;
    private Color backgroundMatched;
    private Color backgroundError;

    private Color cellHeaderForeground;
    private Color cellHeaderBackground;
    private Color cellHeaderSelectionBackground;
    private Color cellHeaderBorder;
    private boolean isHighContrastTheme = false;

    private boolean showOddRows = true;
    private boolean highlightRowsWithSelectedCells;
    //private boolean showCelIcons = true;
    private boolean showAttrOrdering;
    private boolean supportsAttributeFilter;
    private boolean autoFetchSegments;
    private boolean showAttributeIcons;
    private boolean showAttributeDescription;
    private boolean calcColumnWidthByValue;

    private boolean rightJustifyNumbers = true;
    private boolean rightJustifyDateTime = true;
    private boolean showBooleanAsCheckbox;
    private boolean showWhitespaceCharacters;
    private BooleanStyleSet booleanStyles;
    private int rowBatchSize;
    private IValueEditor activeInlineEditor;

    private int highlightScopeFirstLine;
    private int highlightScopeLastLine;
    private Color highlightScopeColor;
    private boolean useNativeNumbersFormat;

    private boolean colorizeDataTypes = true;
    private final Map<DBPDataKind, Color> dataTypesForegrounds = new IdentityHashMap<>();
    private DBDDisplayFormat gridValueFormat;

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    @Override
    public boolean isDirty() {
        return activeInlineEditor != null &&
            activeInlineEditor.getControl() != null &&
            !activeInlineEditor.getControl().isDisposed() &&
            !DBExecUtils.isAttributeReadOnly(getCurrentAttribute()) &&
            !(activeInlineEditor instanceof IValueEditorStandalone);
    }

    @Override
    public void applyChanges() {
        if (activeInlineEditor != null && activeInlineEditor.getControl() != null && !activeInlineEditor.getControl().isDisposed()) {
            IValueController valueController = (IValueController) activeInlineEditor.getControl().getData(DATA_VALUE_CONTROLLER);
            if (valueController != null) {
                try {
                    Object value = activeInlineEditor.extractEditorValue();
                    valueController.updateValue(value, true);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Error extracting editor value", null, e);
                }
            }
            spreadsheet.cancelInlineEditor();
        }
        super.applyChanges();
    }

    @Override
    public void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        this.spreadsheet = new Spreadsheet(
            parent,
            SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
            controller.getSite(),
            this,
            new ContentProvider(),
            new GridLabelProvider(),
            this);
        this.spreadsheet.setLayoutData(new GridData(GridData.FILL_BOTH));

        this.spreadsheet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.detail != SWT.DRAG && e.detail != SWT.DROP_DOWN) {
                    updateGridCursor((GridCell) e.data);
                }
                fireSelectionChanged(new SpreadsheetSelectionImpl());
            }
        });
        this.spreadsheet.addMouseWheelListener(e -> {

        });
        spreadsheet.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                spreadsheet.cancelInlineEditor();
            }
        });
        spreadsheet.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
                if (controller.isPanelsVisible()) {
                    controller.getVisiblePanel().setFocus();
                    e.doit = false;
                }
            }
        });

        activateTextKeyBindings(controller, spreadsheet);

        applyCurrentThemeSettings();

        trackPresentationControl();
        TextEditorUtils.enableHostEditorKeyBindingsSupport(controller.getSite(), spreadsheet);
    }

    @Override
    public void dispose() {
        closeEditors();
        clearMetaData();

        UIUtils.dispose(this.cellHeaderSelectionBackground);
        super.dispose();
    }

    public void scrollToRow(@NotNull RowPosition position) {
        boolean recordMode = controller.isRecordMode();
        ResultSetRow curRow = controller.getCurrentRow();
        ResultSetModel model = controller.getModel();

        spreadsheet.setRedraw(false);
        try {
            int hScrollPos = spreadsheet.getHorizontalScrollBarProxy().getSelection();

            switch (position) {
                case FIRST:
                    if (recordMode) {
                        if (model.getRowCount() > 0) {
                            controller.setCurrentRow(model.getRow(0));
                        } else {
                            controller.setCurrentRow(null);
                        }
                    } else {
                        spreadsheet.shiftCursor(0, -spreadsheet.getItemCount(), false);
                    }
                    break;
                case PREVIOUS:
                    if (recordMode && curRow != null && curRow.getVisualNumber() > 0) {
                        controller.setCurrentRow(model.getRow(curRow.getVisualNumber() - 1));
                    } else {
                        spreadsheet.shiftCursor(0, -1, false);
                    }
                    break;
                case NEXT:
                    if (recordMode && curRow != null && curRow.getVisualNumber() < model.getRowCount() - 1) {
                        controller.setCurrentRow(model.getRow(curRow.getVisualNumber() + 1));
                    } else {
                        spreadsheet.shiftCursor(0, 1, false);
                    }
                    break;
                case LAST:
                    if (recordMode && model.getRowCount() > 0) {
                        controller.setCurrentRow(model.getRow(model.getRowCount() - 1));
                    } else {
                        spreadsheet.shiftCursor(0, spreadsheet.getItemCount(), false);
                    }
                    break;
                case CURRENT:
                    if (curRow != null && !recordMode) {
                        GridPos curPos = spreadsheet.getCursorPosition();
                        // Find corresponding grid row
                        IGridRow gridRow = spreadsheet.getRowByElement(curRow.getVisualNumber(), curRow);
                        if (gridRow != null) {
                            GridCell newCell = spreadsheet.posToCell(new GridPos(curPos.col, gridRow.getVisualPosition()));
                            if (newCell != null) {
                                spreadsheet.setCursor(newCell, false, true, true);
                            }
                        }
                    }
                    break;
            }

            if (recordMode && controller.getSelectedRecords().length > 1 && curRow != null) {
                // Shift to new row in record mode
                curRow = controller.getCurrentRow();
                int newColumnIndex = curRow == null ? -1 : ArrayUtils.indexOf(controller.getSelectedRecords(), 0, curRow.getVisualNumber());
                if (newColumnIndex >= 0) {
                    GridPos focusPos = spreadsheet.getCursorPosition();
                    GridCell newPos = spreadsheet.posToCell(new GridPos(newColumnIndex, focusPos.row));
                    if (newPos != null) {
                        spreadsheet.setCursor(newPos, true, true, false);
                    }
                }
            }

            spreadsheet.getHorizontalScrollBarProxy().setSelection(hScrollPos);

            // Update controls
            controller.updateStatusMessage();
            controller.updatePanelsContent(false);

            if (recordMode) {
                // Refresh meta if we are in record mode
                refreshData(true, false, true);
            }
        } finally {
            spreadsheet.setRedraw(true);
        }
    }

    @Nullable
    @Override
    public DBDAttributeBinding getCurrentAttribute() {
        return curAttribute;
    }

    @Override
    public void setCurrentAttribute(@NotNull DBDAttributeBinding attribute) {
        this.curAttribute = attribute;

        ResultSetRow curRow = controller.getCurrentRow();
        boolean recordMode = controller.isRecordMode();
        IGridColumn gridColumn = spreadsheet.getColumnByElement(
            recordMode ? curRow : this.curAttribute);
        IGridRow gridRow = spreadsheet.getRowByElement(
            recordMode ? 0 : curRow == null ? 0 : curRow.getVisualNumber(),
            recordMode ? this.curAttribute : curRow);
        GridCell cell = new GridCell(gridColumn, gridRow);
        this.spreadsheet.setCursor(cell, false, true, true);
        //this.spreadsheet.showColumn(this.curAttribute);
    }

    @Override
    public void showAttribute(@NotNull DBDAttributeBinding attribute) {
        this.spreadsheet.showColumn(attribute);
    }

    @Nullable
    @Override
    public int[] getCurrentRowIndexes() {
        GridPos focusPos = spreadsheet.getFocusPos();
        if (focusPos.row >= 0) {
            return getRowNestedIndexes(spreadsheet.getRow(focusPos.row));
        }
        return null;
    }

    @Override
    public Point getCursorLocation() {
        GridPos focusPos = spreadsheet.getFocusPos();
        if (focusPos.col >= 0) {
            Rectangle columnBounds = spreadsheet.getColumnBounds(focusPos.col);
            if (columnBounds != null) {
                columnBounds.y += spreadsheet.getHeaderHeight() +
                    (focusPos.row - spreadsheet.getTopIndex()) * (spreadsheet.getItemHeight() + 1) + spreadsheet.getItemHeight() / 2;
                return new Point(columnBounds.x + 20, columnBounds.y);
            }
        }
        return super.getCursorLocation();
    }


    @Override
    protected void performHorizontalScroll(int scrollCount) {
        spreadsheet.scrollHorizontally(scrollCount);
    }

    void highlightRows(int firstLine, int lastLine, Color color) {
        this.highlightScopeFirstLine = firstLine;
        this.highlightScopeLastLine = lastLine;
        this.highlightScopeColor = color;
    }

    /////////////////////////////////////////////////
    // State

    private static class ViewState {
        DBDAttributeBinding focusedAttribute;
        int hScrollSelection;

        ViewState(DBDAttributeBinding focusedAttribute, int hScrollSelection) {
            this.focusedAttribute = focusedAttribute;
            this.hScrollSelection = hScrollSelection;
        }
    }

    @Override
    public Object saveState() {
        return new ViewState(curAttribute, spreadsheet.getHorizontalScrollBarProxy().getSelection());
    }

    @Override
    public void restoreState(Object state) {
        if (state == null) {
            return;
        }
        ViewState viewState = (ViewState) state;
        this.curAttribute = controller.getModel().getAttributeBinding(viewState.focusedAttribute);
        /*ResultSetRow curRow = controller.getCurrentRow();
        if (curRow != null && this.curAttribute != null) {
            GridCell cell = controller.isRecordMode() ?
                new GridCell(curRow, this.curAttribute) :
                new GridCell(this.curAttribute, curRow);
            //spreadsheet.selectCell(cell);
            spreadsheet.setCursor(cell, false, false);
        }*/
        spreadsheet.getHorizontalScrollBarProxy().setSelection(viewState.hScrollSelection);
        spreadsheet.setDefaultFocusRow();
    }

    private void updateGridCursor(GridCell cell) {
        boolean changed;
        IGridColumn newCol = cell == null ? null : cell.col;
        IGridRow newRow = cell == null ? null : cell.row;
        ResultSetRow curRow = controller.getCurrentRow();
        if (!controller.isRecordMode()) {
            changed = (newRow != null && curRow != newRow.getElement()) ||
                (newCol != null && curAttribute != newCol.getElement());
            if (newRow != null && newRow.getElement() instanceof ResultSetRow resultSetRow) {
                curRow = resultSetRow;
                controller.setCurrentRow(curRow);
            }
            if (newCol != null && newCol.getElement() instanceof DBDAttributeBinding attributeBinding) {
                curAttribute = attributeBinding;
            }
        } else {
            changed = newRow != null && curAttribute != newRow.getElement();
            if (newRow != null && newRow.getElement() instanceof DBDAttributeBinding attributeBinding) {
                curAttribute = attributeBinding;
            }
            if (newCol != null &&
                newCol.getElement() instanceof ResultSetRow resultSetRow &&
                curRow != newCol.getElement())
            {
                curRow = resultSetRow;
                controller.setCurrentRow(curRow);
            }
        }
        if (changed) {
            spreadsheet.cancelInlineEditor();
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_MOVE);
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_EDITABLE);
            spreadsheet.redrawGrid();
        }
    }

    @NotNull
    @Override
    public Map<Transfer, Object> copySelection(ResultSetCopySettings settings) {
        boolean copyHTML = settings.isCopyHTML();

        StringBuilder tdt = new StringBuilder();
        StringBuilder html = new StringBuilder();
        byte[] binaryData = null;

        Map<Transfer, Object> formats = new LinkedHashMap<>();

        String columnDelimiter = settings.getColumnDelimiter();
        if (columnDelimiter == null) {
            columnDelimiter = "\t";
        }

        String rowDelimiter = settings.getRowDelimiter();
        if (rowDelimiter == null) {
            rowDelimiter = GeneralUtils.getDefaultLineSeparator();
        }
        String quoteString = settings.getQuoteString();
        if ((CommonUtils.isEmpty(quoteString))) {
            quoteString = "\"";
        }
        List<IGridColumn> selectedColumns = spreadsheet.getColumnSelection();
        IGridLabelProvider labelProvider = spreadsheet.getLabelProvider();
        if (copyHTML) html.append("<table border=\"1\">");
        if (settings.isCopyHeader()) {
            if (copyHTML) html.append("<thead>");
            if (settings.isCopyRowNumbers()) {
                tdt.append("#");
                if (copyHTML) html.append("<th>#</th>");
            }
            for (IGridColumn column : selectedColumns) {
                if (!tdt.isEmpty()) {
                    tdt.append(columnDelimiter);
                }
                String columnText = labelProvider.getText(column);
                tdt.append(columnText);
                if (copyHTML) html.append("<th>").append(XMLUtils.escapeXml(columnText)).append("</th>");
            }
            tdt.append(rowDelimiter);
            if (copyHTML) html.append("</thead>").append(rowDelimiter);
        }

        if (copyHTML) html.append("<tbody>");

        List<GridCell> selectedCells = spreadsheet.getCellSelection();
        boolean quoteCells = settings.isQuoteCells() && selectedCells.size() > 1;
        boolean forceQuotes = settings.isForceQuotes();

        GridCell prevCell = null;
        for (GridCell cell : selectedCells) {
            if (prevCell == null || cell.row != prevCell.row) {
                // Next row
                if (prevCell != null && prevCell.col != cell.col) {
                    // Fill empty row tail
                    int prevColIndex = selectedColumns.indexOf(prevCell.col);
                    for (int i = prevColIndex; i < selectedColumns.size() - 1; i++) {
                        tdt.append(columnDelimiter);
                        if (copyHTML) html.append("<td></td>");
                    }
                }
                if (prevCell != null) {
                    tdt.append(rowDelimiter);
                    if (copyHTML) html.append("</tr>").append(rowDelimiter);
                }
                if (settings.isCopyRowNumbers()) {
                    String rowNumber = labelProvider.getText(cell.row);
                    tdt.append(rowNumber).append(columnDelimiter);
                    if (copyHTML) html.append("<td>").append(rowNumber).append("</td>");
                }
                if (copyHTML) html.append("<tr>");
            }
            if (prevCell != null && prevCell.col != cell.col) {
                int prevColIndex = selectedColumns.indexOf(prevCell.col);
                int curColIndex = selectedColumns.indexOf(cell.col);
                for (int i = prevColIndex; i < curColIndex; i++) {
                    tdt.append(columnDelimiter);
                    if (i != prevColIndex) {
                        if (copyHTML) html.append("<td></td>");
                    }
                }
            }

            DBDAttributeBinding column = getAttributeFromGrid(cell.col, cell.row);
            Object value = spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false);
            //Object value = controller.getModel().getCellValue(column, row);
            if (binaryData == null && (column.getDataKind() == DBPDataKind.BINARY || column.getDataKind() == DBPDataKind.CONTENT)) {
                if (value instanceof byte[] bValue) {
                    binaryData = bValue;
                } else if (value instanceof DBDContent content && !ContentUtils.isTextContent(content) && value instanceof DBDContentCached) {
                    try {
                        binaryData = ContentUtils.getContentBinaryValue(new VoidProgressMonitor(), content);
                    } catch (DBCException e) {
                        log.debug("Error reading content binary value");
                    }
                }
            }
            String cellText = column.getValueRenderer().getValueDisplayString(
                column.getAttribute(),
                value,
                settings.getFormat());
            if (forceQuotes || (quoteCells && !CommonUtils.isEmpty(cellText))) {
                if (forceQuotes || cellText.contains(columnDelimiter) || cellText.contains(rowDelimiter)) {
                    cellText = quoteString + cellText + quoteString;
                }
            }
            tdt.append(cellText);
            if (copyHTML) html.append("<td>").append(XMLUtils.escapeXml(cellText)).append("</td> ");

            if (settings.isCut()) {
                ResultSetRow row = getResultRowFromGrid (cell.col, cell.row);

                IValueController valueController = new SpreadsheetValueController(
                    controller,
                    new ResultSetCellLocation(column, row, getRowNestedIndexes(cell.row)),
                    IValueController.EditType.NONE,
                    null);
                if (!valueController.isReadOnly()) {
                    valueController.updateValue(BaseValueManager.makeNullValue(valueController), false);
                }
            }

            prevCell = cell;
        }
        if (copyHTML) {
            html.append("</tbody>").append(rowDelimiter);
            html.append("</table>").append(rowDelimiter);
        }
        if (settings.isCut()) {
            controller.redrawData(false, false);
            controller.updatePanelsContent(false);
        }

        formats.put(TextTransfer.getInstance(), tdt.toString());
        if (copyHTML) {
            formats.put(HTMLTransfer.getInstance(), html.toString());
        }
        if (binaryData != null) {
            formats.put(SimpleByteArrayTransfer.getInstance(), binaryData);
        }

        return formats;
    }

    @Override
    public void pasteFromClipboard(@Nullable ResultSetPasteSettings settings) {
        try {
            if (settings != null) {
                String strValue;
                Clipboard clipboard = new Clipboard(Display.getCurrent());
                try {
                    strValue = (String) clipboard.getContents(TextTransfer.getInstance());
                } finally {
                    clipboard.dispose();
                }
                if (CommonUtils.isEmpty(strValue)) {
                    return;
                }
                final Pair<GridPos, GridPos> targetRange;
                if (spreadsheet.getItemCount() == 0) {
                    // A special case when the grid is empty
                    targetRange = new Pair<>(new GridPos(0, 0), null);
                } else {
                    targetRange = getContinuousRange(List.copyOf(spreadsheet.getSelection()));
                }
                if (targetRange == null) {
                    DBWorkbench.getPlatformUI().showWarningMessageBox(
                        "Advanced paste",
                        "You can't perform this operation on a multiple range selection.\n\nPlease select a single range and try again."
                    );
                    return;
                }
                final GridPos rangeStart = targetRange.getFirst();
                final GridPos rangeEnd = targetRange.getSecond();
                int rowNum = rangeStart.row;
                //boolean overNewRow = controller.getModel().getRow(rowNum).getState() == ResultSetRow.STATE_ADDED;
                try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), controller.getDataContainer(), "Advanced paste")) {

                    String[][] newLines = parseGridLines(strValue, settings.isInsertMultipleRows(), settings.isIgnoreQuotes());

                    // FIXME: do not create rows twice! Probably need to delete comment after testing. #9095
                    /*if (overNewRow) {
                        for (int i = 0 ; i < newLines.length - 1; i++) {
                            controller.addNewRow(false, true, false);
                        }
                        spreadsheet.refreshRowsData();
                    } else {*/
                    while (rangeEnd == null && rowNum + newLines.length > spreadsheet.getItemCount()) {
                        controller.addNewRow(RowPlacement.AT_END, false, false);
                        spreadsheet.refreshRowsData();
                    }
                    //}
                    if (rowNum < 0 || rowNum >= spreadsheet.getItemCount()) {
                        return;
                    }

                    for (String[] line : newLines) {
                        int colNum = rangeStart.col;
                        IGridRow gridRow = spreadsheet.getRow(rowNum);
                        for (String value : line) {
                            IGridColumn colElement = spreadsheet.getColumn(colNum);
                            final DBDAttributeBinding attr = getAttributeFromGrid(colElement, gridRow);
                            final ResultSetRow row = getResultRowFromGrid(colElement, gridRow);
                            if (attr == null || row == null ||
                                controller.getAttributeReadOnlyStatus(attr, true, true) != null
                            ) {
                                continue;
                            }
                            final Object newValue;
                            if (settings.isInsertNulls() && settings.getNullValueMark().equalsIgnoreCase(value)) {
                                newValue = null;
                            } else {
                                newValue = attr.getValueHandler().getValueFromObject(session, attr.getAttribute(), value, true, false);
                            }
                            new SpreadsheetValueController(
                                controller,
                                new ResultSetCellLocation(attr, row, getRowNestedIndexes(gridRow)),
                                IValueController.EditType.NONE, null).updateValue(newValue, false);

                            colNum++;
                            if (colNum >= spreadsheet.getColumnCount()) {
                                break;
                            }
                            if (rangeEnd != null && rangeStart.col != rangeEnd.col && colNum > rangeEnd.col) {
                                // If we have the range end and it spans more than one column, then limit insertion to that range
                                break;
                            }
                        }
                        rowNum++;
                        if (rowNum >= spreadsheet.getItemCount()) {
                            break;
                        }
                        if (rangeEnd != null && rowNum > rangeEnd.row) {
                            break;
                        }
                    }
                }
                this.scrollToRow(IResultSetPresentation.RowPosition.CURRENT);

            } else {
                Collection<GridPos> ssSelection = spreadsheet.getSelection();
                for (GridPos pos : ssSelection) {
                    DBDAttributeBinding attr;
                    ResultSetRow row;
                    IGridRow gridRow = spreadsheet.getRow(pos.row);
                    if (controller.isRecordMode()) {
                        attr = (DBDAttributeBinding) spreadsheet.getRowElement(pos.row);
                        row = controller.getCurrentRow();
                    } else {
                        attr = (DBDAttributeBinding) spreadsheet.getColumnElement(pos.col);
                        row = (ResultSetRow) gridRow.getElement();
                    }
                    if (attr == null || row == null) {
                        continue;
                    }
                    if (controller.getAttributeReadOnlyStatus(attr, true, false) != null) {
                        // No inline editors for readonly columns
                        continue;
                    }

                    SpreadsheetValueController valueController = new SpreadsheetValueController(
                        controller,
                        new ResultSetCellLocation(attr, row, getRowNestedIndexes(gridRow)),
                        IValueController.EditType.NONE, null);

                    Object newValue = null;

                    if (attr.getDataKind() == DBPDataKind.BINARY || attr.getDataKind() == DBPDataKind.CONTENT) {

                        Clipboard clipboard = new Clipboard(Display.getCurrent());
                        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), attr, "Copy from clipboard")) {
                            byte[] binaryContents = (byte[]) clipboard.getContents(SimpleByteArrayTransfer.getInstance());
                            if (binaryContents != null) {
                                newValue = valueController.getValueHandler().getValueFromObject(session, attr, binaryContents, false, false);
                            }
                        } finally {
                            clipboard.dispose();
                        }
                    }
                    if (newValue == null) {
                        newValue = ResultSetUtils.getAttributeValueFromClipboard(attr);
                        if (newValue == null) {
                            continue;
                        }
                    }
                    valueController.updateValue(newValue, false);
                }
            }
            controller.redrawData(false, true);
            controller.updateEditControls();
            controller.updatePanelsContent(false);
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Cannot replace cell value", null, e);
        }
    }

    /**
     * Retrieves a continuous range from a set of grid coordinates.
     * <p>
     * A continuous range is a range in which all grid coordinates are selected.
     *
     * @param selection a list of positions to retrieve a continuous range from
     * @return a pair containing either:
     * <ul>
     *     <li>{@code null} if no selection is present or selected range is not continuous</li>
     *     <li>{@code (GridPos, null)} if only one cell is selected</li>
     *     <li>{@code (GridPos, GridPos)} if selected range is continuous</li>
     * </ul>
     */
    @Nullable
    private Pair<GridPos, GridPos> getContinuousRange(@NotNull List<GridPos> selection) {
        return switch (selection.size()) {
            case 0 -> null;
            case 1 -> new Pair<>(selection.get(0), null);
            default -> {
                GridPos min = selection.get(0);
                GridPos max = new GridPos(min);

                for (int i = 0; i < selection.size(); i++) {
                    final GridPos cur = selection.get(i);

                    if (i > 0 && (cur.row - max.row > 1 || cur.col - max.col > 1)) {
                        yield null;
                    }

                    max = cur;
                }

                yield new Pair<>(min, max);
            }
        };
    }

    private String[][] parseGridLines(String strValue, boolean splitRows, boolean ignoreQuotes) {
        final char columnDelimiter = '\t';
        final char rowDelimiter = '\n';
        final char trashDelimiter = '\r';
        final char quote = '"';

        final List<String[]> lines = new ArrayList<>();

        final StringBuilder cellValue = new StringBuilder();
        final List<String> curLine = new ArrayList<>();
        boolean inQuote = false;
        int length = strValue.length();
        for (int i = 0; i < length; i++) {
            char c = strValue.charAt(i);
            if (inQuote && c != quote) {
                cellValue.append(c);
            } else {
                switch (c) {
                    case columnDelimiter:
                    case rowDelimiter:
                        curLine.add(cellValue.toString());
                        cellValue.setLength(0);
                        if (c == rowDelimiter && splitRows) {
                            lines.add(curLine.toArray(new String[0]));
                            curLine.clear();
                        }
                        break;
                    case trashDelimiter:
                        // Ignore
                        continue;
                    case quote:
                        if (ignoreQuotes) {
                            cellValue.append(c);
                            break;
                        }
                        if (inQuote) {
                            if (i == length - 1 ||
                                strValue.charAt(i + 1) == columnDelimiter ||
                                strValue.charAt(i + 1) == trashDelimiter ||
                                strValue.charAt(i + 1) == rowDelimiter) {
                                inQuote = false;
                                continue;
                            }
                        } else if (cellValue.isEmpty()) {
                            // Search for end quote
                            for (int k = i + 1; k < length; k++) {
                                if (strValue.charAt(k) == quote &&
                                    (k == length - 1 ||
                                        strValue.charAt(k + 1) == columnDelimiter ||
                                        strValue.charAt(k + 1) == trashDelimiter ||
                                        strValue.charAt(k + 1) == rowDelimiter)) {
                                    inQuote = true;
                                    break;
                                }
                            }
                            if (inQuote) {
                                continue;
                            }
                        }
                    default:
                        cellValue.append(c);
                        break;
                }
            }
        }
        if (!cellValue.isEmpty()) {
            curLine.add(cellValue.toString());
        }
        if (!curLine.isEmpty()) {
            lines.add(curLine.toArray(new String[0]));
        }

        return lines.toArray(new String[lines.size()][]);
    }

    @Override
    public Control getControl() {
        return spreadsheet;
    }

    @Override
    public void refreshData(boolean refreshMetadata, boolean append, boolean keepState) {
        if (spreadsheet.isDisposed()) {
            return;
        }
        isHighContrastTheme = UIStyles.isHighContrastTheme();

        // Cache preferences
        DBPPreferenceStore preferenceStore = getPreferenceStore();
        showOddRows = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS);
        highlightRowsWithSelectedCells = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_HIGHLIGHT_SELECTED_ROWS);
        //showCelIcons = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS);
        rightJustifyNumbers = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);
        rightJustifyDateTime = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME);
        rowBatchSize = preferenceStore.getInt(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE);

        showAttrOrdering = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING);
        showAttributeIcons = controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS);
        showAttributeDescription = getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION);
        supportsAttributeFilter =
            controller.getDataContainer() != null &&
                (controller.getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_FILTERS) != 0 &&
                controller.getDataContainer().isFeatureSupported(DBSDataContainer.FEATURE_DATA_FILTER) &&
                controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS);
        autoFetchSegments = controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT);
        calcColumnWidthByValue = getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES);
        showBooleanAsCheckbox = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_BOOLEAN_AS_CHECKBOX);
        showWhitespaceCharacters = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_WHITESPACE_CHARACTERS);
        booleanStyles = BooleanStyleSet.getDefaultStyles(preferenceStore);
        useNativeNumbersFormat = controller.getPreferenceStore().getBoolean(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT);

        spreadsheet.setColumnScrolling(!getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_USE_SMOOTH_SCROLLING));
        gridValueFormat = CommonUtils.valueOf(DBDDisplayFormat.class, getPreferenceStore().getString(ResultSetPreferences.RESULT_GRID_VALUE_FORMAT), DBDDisplayFormat.UI);

        spreadsheet.setRedraw(false);
        try {
            spreadsheet.refreshData(refreshMetadata, keepState, false);
        } finally {
            spreadsheet.setRedraw(true);
        }
    }

    @Override
    public void formatData(boolean refreshData) {
        spreadsheet.refreshData(false, true, false);
    }

    @Override
    public void clearMetaData() {
        this.curAttribute = null;
        if (this.columnOrder != SWT.NONE) {
            this.columnOrder = SWT.DEFAULT;
        }
    }

    @Override
    public void updateValueView() {
        spreadsheet.redrawGrid();
        spreadsheet.updateScrollbars();

        if (curAttribute == null) {
            curAttribute = getFocusAttribute();
        }
    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {
        menu.add(ActionUtils.makeCommandContribution(
            controller.getSite(),
            ResultSetHandlerMain.CMD_TOGGLE_PANELS,
            CommandContributionItem.STYLE_CHECK));
    }

    @Override
    public void changeMode(boolean recordMode) {
        ResultSetRow oldRow = controller.getCurrentRow();
        DBDAttributeBinding oldAttribute = this.curAttribute;
        int rowCount = controller.getModel().getRowCount();
        if (rowCount > 0) {
            // Fix row number if needed
            if (oldRow == null) {
                oldRow = controller.getModel().getRow(0);
            } else if (oldRow.getVisualNumber() >= rowCount) {
                oldRow = controller.getModel().getRow(rowCount - 1);
            }
        }
        if (oldAttribute == null && controller.getModel().getVisibleAttributeCount() > 0) {
            oldAttribute = controller.getModel().getVisibleAttribute(0);
        }

        this.columnOrder = recordMode ? SWT.DEFAULT : SWT.NONE;
        if (oldRow != null && oldAttribute != null) {
            IGridColumn gridColumn = spreadsheet.getColumnByElement(
                recordMode ? oldRow : oldAttribute);
            IGridRow gridRow = spreadsheet.getRowByElement(
                recordMode ? 0 : oldRow.getVisualNumber(),
                recordMode ? oldAttribute : oldRow);

            spreadsheet.setCursor(
                new GridCell(gridColumn, gridRow), false, true, true);
        }
        spreadsheet.layout(true, true);
    }

    void fillContextMenu(
        @NotNull IMenuManager manager,
        @Nullable IGridColumn colObject,
        @Nullable IGridRow rowObject)
    {
        boolean recordMode = controller.isRecordMode();
        final DBDAttributeBinding attr = getAttributeFromGrid(colObject, rowObject);
        final ResultSetRow row = getResultRowFromGrid(colObject, rowObject);
        controller.fillContextMenu(manager, attr, row, getRowNestedIndexes(rowObject));

        if (attr != null && row == null) {
            final List<IGridColumn> selectedColumns = spreadsheet.getColumnSelection();
            if (selectedColumns.size() == 1) {
                IGridColumn attrCol = spreadsheet.getColumnByElement(attr);
                if (attrCol != null) {
                    selectedColumns.clear();
                    selectedColumns.add(attrCol);
                }
            }
            if (!recordMode && !selectedColumns.isEmpty()) {
                // Row mode
                manager.insertBefore(IResultSetController.MENU_GROUP_ADDITIONS, new Separator());
                {
                    // Pin/unpin
                    DBDDataFilter dataFilter = controller.getModel().getDataFilter();

                    final boolean allPinned = selectedColumns.stream()
                        .map(x -> dataFilter.getConstraint(((DBDAttributeBinding) x.getElement()).getTopParent()))
                        .allMatch(x -> x != null && x.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED));
                    final boolean allUnpinned = selectedColumns.stream()
                        .map(x -> dataFilter.getConstraint(((DBDAttributeBinding) x.getElement()).getTopParent()))
                        .allMatch(x -> x != null && !x.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED));

                    if (allUnpinned != allPinned) {
                        final String pinnedTitle = allUnpinned
                            ? selectedColumns.size() == 1
                            ? NLS.bind(ResultSetMessages.controls_resultset_viewer_pin_column, ((DBDAttributeBinding) selectedColumns.get(0).getElement()).getName())
                            : NLS.bind(ResultSetMessages.controls_resultset_viewer_pin_columns, selectedColumns.size())
                            : selectedColumns.size() == 1
                            ? NLS.bind(ResultSetMessages.controls_resultset_viewer_unpin_column, ((DBDAttributeBinding) selectedColumns.get(0).getElement()).getName())
                            : NLS.bind(ResultSetMessages.controls_resultset_viewer_unpin_columns, selectedColumns.size());

                        manager.insertBefore(IResultSetController.MENU_GROUP_ADDITIONS, new Action(pinnedTitle) {
                            @Override
                            public void run() {
                                for (IGridColumn column : selectedColumns) {
                                    final DBDAttributeBinding attribute = (DBDAttributeBinding) column.getElement();
                                    final DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute.getTopParent());
                                    if (constraint != null) {
                                        if (allUnpinned) {
                                            constraint.setOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED, getNextPinIndex(dataFilter));
                                        } else {
                                            constraint.removeOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED);
                                        }
                                    }
                                }
                                spreadsheet.refreshData(true, true, false);
                            }
                        });
                    }
                }
                {
                    // Hide/show
                    if (getController().getModel().getDataFilter().hasHiddenAttributes()) {
                        manager.insertAfter(
                            IResultSetController.MENU_GROUP_ADDITIONS,
                            ActionUtils.makeCommandContribution(
                                controller.getSite(),
                                SpreadsheetCommandHandler.CMD_SHOW_COLUMNS,
                                ResultSetMessages.controls_resultset_viewer_show_hidden_columns,
                                null
                            )
                        );
                    }

                    String hideTitle;
                    if (selectedColumns.size() == 1) {
                        DBDAttributeBinding columnToHide = (DBDAttributeBinding) selectedColumns.get(0).getElement();
                        hideTitle = NLS.bind(ResultSetMessages.controls_resultset_viewer_hide_column_x, columnToHide.getName());
                    } else {
                        hideTitle = NLS.bind(ResultSetMessages.controls_resultset_viewer_hide_columns_x, selectedColumns.size());
                    }
                    manager.insertAfter(
                        IResultSetController.MENU_GROUP_ADDITIONS, 
                        ActionUtils.makeCommandContribution(
                            controller.getSite(),
                            SpreadsheetCommandHandler.CMD_HIDE_COLUMNS,
                            hideTitle,
                            null
                        )
                    );
                }
            }
        }

        if (recordMode && row != null) {
            // Record mode
            List<Integer> selectedRowIndexes = new ArrayList<>();
            for (IGridColumn sRow : spreadsheet.getColumnSelection()) {
                if (sRow.getElement() instanceof ResultSetRow resultSetRow) {
                    selectedRowIndexes.add(resultSetRow.getVisualNumber());
                }
            }

            if (!selectedRowIndexes.isEmpty() && selectedRowIndexes.size() < controller.getSelectedRecords().length) {
                List<Integer> curRowIndexes = Arrays.stream(controller.getSelectedRecords())
                    .boxed().collect(Collectors.toList());
                curRowIndexes.removeAll(selectedRowIndexes);
                if (!curRowIndexes.isEmpty()) {
                    manager.insertAfter(IResultSetController.MENU_GROUP_ADDITIONS, new Action("Hide row(s)") {
                        @Override
                        public void run() {
                            controller.setSelectedRecords(curRowIndexes.stream().mapToInt(i -> i).toArray());
                            refreshData(true, false, true);
                        }
                    });
                }
            }
        }
        if (row == null) {
            if (!controller.getModel().getVisibleAttributes().isEmpty()) {
                manager.insertAfter(
                    IResultSetController.MENU_GROUP_ADDITIONS,
                    ActionUtils.makeCommandContribution(
                        controller.getSite(),
                        SpreadsheetCommandHandler.CMD_COLUMNS_HIDE_EMPTY));
                manager.insertAfter(
                    IResultSetController.MENU_GROUP_ADDITIONS,
                    ActionUtils.makeCommandContribution(
                        controller.getSite(),
                        SpreadsheetCommandHandler.CMD_COLUMNS_FIT_VALUE));
                manager.insertAfter(
                    IResultSetController.MENU_GROUP_ADDITIONS,
                    ActionUtils.makeCommandContribution(
                        controller.getSite(),
                        SpreadsheetCommandHandler.CMD_COLUMNS_FIT_SCREEN));
            }
        }
    }

    public static int getNextPinIndex(@NotNull DBDDataFilter dataFilter) {
        int maxIndex = 0;
        for (DBDAttributeConstraint ac : dataFilter.getConstraints()) {
            Integer pinIndex = ac.getOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED);
            if (pinIndex != null) {
                maxIndex = Math.max(maxIndex, pinIndex + 1);
            }
        }
        return maxIndex;
    }

    /////////////////////////////////////////////////
    // Edit

    private void closeEditors() {
        List<IValueEditorStandalone> editors = new ArrayList<>(openEditors.values());
        for (IValueEditorStandalone editor : editors) {
            if (editor.getControl() != null && !editor.getControl().isDisposed()) {
                editor.closeValueEditor();
            }
        }
        openEditors.clear();
    }

    @Override
    @Nullable
    public Control openValueEditor(final boolean inline) {
        // The control that will be the editor must be a child of the Table
        IGridColumn focusColumn = spreadsheet.getFocusColumn();
        IGridRow focusRow = spreadsheet.getFocusRow();
        DBDAttributeBinding attr = getAttributeFromGrid(focusColumn, focusRow);
        ResultSetRow row = getResultRowFromGrid(focusColumn, focusRow);
        if (attr == null || row == null) {
            return null;
        }

        ResultSetCellLocation cellLocation = new ResultSetCellLocation(attr, row, getRowNestedIndexes(focusRow));
        Object cellValue = getController().getModel().getCellValue(cellLocation);
        if (cellValue instanceof DBDValueSurrogate) {
            return null;
        }

        if (!inline) {
            for (Iterator<SpreadsheetValueController> iterator = openEditors.keySet().iterator(); iterator.hasNext(); ) {
                SpreadsheetValueController valueController = iterator.next();
                if (attr == valueController.getBinding() && row == valueController.getCurRow()) {
                    IValueEditorStandalone editor = openEditors.get(valueController);
                    if (editor.getControl() != null && !editor.getControl().isDisposed()) {
                        editor.showValueEditor();
                        return null;
                    } else {
                        // Remove disposed editor from the list
                        iterator.remove();
                    }
                }
            }
        } else {
            if (isShowAsCheckbox(attr) && getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN)) {
                // No inline boolean editor. Single click changes value
                return null;
            }
        }

        Composite placeholder = null;
        if (inline) {
            String readOnlyStatus = controller.getAttributeReadOnlyStatus(attr, true, true);
            if (readOnlyStatus != null) {
                controller.setStatus(
                    NLS.bind(ResultSetMessages.controls_resultset_viewer_action_open_value_editor_column_readonly,
                        DBUtils.getObjectFullName(attr, DBPEvaluationContext.UI), readOnlyStatus),
                    DBPMessageType.ERROR);
            }
            spreadsheet.cancelInlineEditor();
            activeInlineEditor = null;

            placeholder = new Composite(spreadsheet, SWT.NONE);
            placeholder.setFont(spreadsheet.getFont());
            placeholder.setLayout(new FillLayout());

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            placeholder.setLayoutData(gd);

            placeholder.addDisposeListener(e -> controller.updateStatusMessage());

            controller.lockActionsByControl(placeholder);
        }

        SpreadsheetValueController valueController = new SpreadsheetValueController(
            controller,
            cellLocation,
            inline ? IValueController.EditType.INLINE : IValueController.EditType.EDITOR, placeholder);

        IValueController.EditType[] supportedEditTypes = valueController.getValueManager().getSupportedEditTypes();
        if (supportedEditTypes.length == 0) {
            if (placeholder != null) {
                placeholder.dispose();
            }
            return null;
        }

        try {
            activeInlineEditor = valueController.getValueManager().createEditor(valueController);
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Cannot edit value", null, e);
            return null;
        }
        if (activeInlineEditor != null) {
            activeInlineEditor.createControl();
            if (activeInlineEditor.getControl() != null) {
                activeInlineEditor.getControl().setFocus();
                activeInlineEditor.getControl().setData(DATA_VALUE_CONTROLLER, valueController);
            }
        }
        if (activeInlineEditor instanceof IValueEditorStandalone editorStandalone) {
            valueController.registerEditor(editorStandalone);
            Control editorControl = activeInlineEditor.getControl();
            if (editorControl != null) {
                editorControl.addDisposeListener(e -> valueController.unregisterEditor((IValueEditorStandalone) activeInlineEditor));
            }
            UIUtils.asyncExec(() -> ((IValueEditorStandalone) activeInlineEditor).showValueEditor());
        } else {
            // Set editable value
            if (activeInlineEditor != null) {
                try {
                    activeInlineEditor.primeEditorValue(valueController.getValue());
                } catch (DBException e) {
                    log.error(e);
                }
                activeInlineEditor.setDirty(false);
            }
        }
        if (inline) {
            if (activeInlineEditor != null) {
                spreadsheet.showCellEditor(placeholder);
                if (activeInlineEditor instanceof BaseValueEditor && CommonUtils.getBoolean(
                        getPreferenceStore().getString(ResultSetPreferences.RESULT_SET_INLINE_ENTER))) {
                    ((BaseValueEditor<?>) activeInlineEditor).addAdditionalTraverseActions((it) -> {
                        //We don't want to create another listener due to baseValueTraverseListener
                        //removing any information about traverse event and setting it to TRAVERSE_NONE
                        if (it.detail == SWT.TRAVERSE_RETURN) {
                            final Event applyEvent = new Event();
                            if ((it.stateMask & SWT.SHIFT) == 0) {
                                applyEvent.keyCode = SWT.ARROW_DOWN;
                            } else {
                                applyEvent.keyCode = SWT.ARROW_RIGHT;
                            }
                            getSpreadsheet().notifyListeners(SWT.KeyDown, applyEvent);
                            openValueEditor(true);
                        }
                    });
                }

                return activeInlineEditor.getControl();
            } else {
                // No editor was created so just drop placeholder
                placeholder.dispose();
                // Probably we can just show preview panel
                if (ArrayUtils.contains(supportedEditTypes, IValueController.EditType.PANEL)) {
                    // Inline editor isn't supported but panel viewer is
                    // Enable panel
                    controller.activatePanel(ValueViewerPanel.PANEL_ID, true, true);
                    return null;
                }
            }
        }
        return null;
    }

    ///////////////////////////////////////////////
    // Links

    public void navigateLink(@NotNull GridCell cell, int x, int y, int state) {
        final DBDAttributeBinding attr = getAttributeFromGrid(cell.col, cell.row);
        final ResultSetRow row = getResultRowFromGrid(cell.col, cell.row);

        Object value = controller.getModel().getCellValue(
            new ResultSetCellLocation(attr, row, getRowNestedIndexes(cell.row)));
        if ((value instanceof Boolean || value instanceof Number || value == null) && isShowAsCheckbox(attr)) {
            if (!getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN)) {
                return;
            }
            if (!DBExecUtils.isAttributeReadOnly(attr)) {
                // Switch boolean value
                toggleBooleanValue(
                    new ResultSetCellLocation(attr, row, getRowNestedIndexes(cell.row)),
                    value);
            }
        } else if (isAttributeExpandable(cell.row, attr)) {
            spreadsheet.toggleCellValue(cell.col, cell.row);
        } else if (DBUtils.isNullValue(value)) {
            UIUtils.showMessageBox(getSpreadsheet().getShell(), "Wrong link", "Can't navigate to NULL value", SWT.ICON_ERROR);
        } else {
            IGridContentProvider.CellInformation cellInfo = spreadsheet.getContentProvider().getCellInfo(cell.col, cell.row, false);
            if ((cellInfo.state & IGridContentProvider.STATE_HYPER_LINK) != 0) {
                // Navigate hyperlink
                String strValue = attr.getValueHandler().getValueDisplayString(attr, value, DBDDisplayFormat.UI);
                if (isHyperlinkText(strValue)) {
                    ShellUtils.launchProgram(strValue);
                } else {
                    EditTextDialog dialog = new EditTextDialog(
                        getSpreadsheet().getShell(),
                        attr.getName(),
                        strValue,
                        true);
                    dialog.open();
                }
            } else {
                spreadsheet.getCellRenderer().executeHintAction(cell.row, cell.col, cellInfo, x, y, state);
            }
        }
    }

    public void toggleCellValue(IGridColumn columnElement, IGridRow rowElement) {
        final DBDAttributeBinding attr = getAttributeFromGrid(columnElement, rowElement);
        final ResultSetRow row = getResultRowFromGrid(columnElement, rowElement);
        final ResultSetCellLocation cellLocation = new ResultSetCellLocation(attr, row, getRowNestedIndexes(rowElement));

        if (isShowAsCheckbox(attr)) {
            // Switch boolean value
            Object cellValue = controller.getModel().getCellValue(cellLocation);
            if (cellValue instanceof Boolean || cellValue instanceof Number) {
                toggleBooleanValue(cellLocation, cellValue);
            }
        }
        if (isAttributeExpandable(rowElement, attr)) {
            spreadsheet.toggleRowExpand(rowElement, columnElement);
        }
    }

    private void toggleBooleanValue(ResultSetCellLocation cellLocation, Object value) {
        boolean nullable = !cellLocation.getAttribute().isRequired();
        if (value instanceof Number) {
            value = ((Number) value).byteValue() != 0;
        }
        if (Boolean.TRUE.equals(value)) {
            value = false;
        } else if (Boolean.FALSE.equals(value)) {
            value = nullable ? null : true;
        } else {
            value = true;
        }
        final SpreadsheetValueController valueController = new SpreadsheetValueController(
            controller,
            cellLocation,
            IValueController.EditType.NONE, null);
        // Update value in all selected rows
        for (ResultSetRow selRow : getSelection().getSelectedRows()) {
            valueController.setCurRow(selRow, cellLocation.getRowIndexes());
            valueController.updateValue(value, true);
        }
    }

    ///////////////////////////////////////////////
    // Themes

    @Override
    protected void applyThemeSettings(ITheme currentTheme) {
        Font rsFont = currentTheme.getFontRegistry().get(ThemeConstants.FONT_SQL_RESULT_SET);
        if (rsFont != null) {
            this.spreadsheet.setFont(rsFont);
        }
        final ColorRegistry colorRegistry = currentTheme.getColorRegistry();
        Color previewBack = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_SET_PREVIEW_BACK);
        if (previewBack != null) {
//            this.previewPane.getViewPlaceholder().setBackground(previewBack);
//            for (Control control : this.previewPane.getViewPlaceholder().getChildren()) {
//                control.setBackground(previewBack);
//            }
        }
        //this.foregroundDefault = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_FORE);
        this.backgroundAdded = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_NEW_BACK);
        this.backgroundDeleted = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_DELETED_BACK);
        this.backgroundModified = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_MODIFIED_BACK);
        this.backgroundOdd = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_ODD_BACK);
        this.backgroundReadOnly = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_READ_ONLY);
        this.foregroundSelected = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE);
        this.foregroundNull = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_NULL_FOREGROUND);
        this.backgroundSelected = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK);
        this.backgroundMatched = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_MATCHED);
        this.backgroundError = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_ERROR_BACK);

        this.cellHeaderForeground = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_HEADER_FOREGROUND);
        this.cellHeaderBackground = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_HEADER_BACKGROUND);
        this.cellHeaderBorder = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_HEADER_BORDER);

        {
            if (this.cellHeaderSelectionBackground != null) {
                UIUtils.dispose(this.cellHeaderSelectionBackground);
                this.cellHeaderSelectionBackground = null;
            }
            Color headerSelectionBackground = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_HEADER_SELECTED_BACKGROUND);
            RGB cellSel = UIUtils.blend(
                headerSelectionBackground.getRGB(),
                UIStyles.isDarkTheme() ? new RGB(100, 100, 100) : new RGB(255, 255, 255),
                50);
            this.cellHeaderSelectionBackground = new Color(getSpreadsheet().getDisplay(), cellSel);
        }
        this.spreadsheet.setLineColor(colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_LINES_NORMAL));
        this.spreadsheet.setLineSelectedColor(colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_LINES_SELECTED));

        this.spreadsheet.recalculateSizes(true);

        this.booleanStyles = BooleanStyleSet.getDefaultStyles(getPreferenceStore());

        this.colorizeDataTypes = getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);

        this.dataTypesForegrounds.put(DBPDataKind.BINARY, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_BINARY_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.BOOLEAN, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_BOOLEAN_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.DATETIME, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_DATETIME_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.NUMERIC, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_NUMERIC_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.STRING, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_STRING_FOREGROUND));
    }

    ///////////////////////////////////////////////
    // Ordering

    private boolean supportsDataFilter() {
        DBSDataContainer dataContainer = controller.getDataContainer();
        return dataContainer != null &&
            dataContainer.isFeatureSupported(DBSDataContainer.FEATURE_DATA_FILTER);
    }

    public void changeSorting(Object columnElement, final int state) {
        if (columnElement == null) {
            columnOrder = columnOrder == SWT.DEFAULT ? SWT.UP : (columnOrder == SWT.UP ? SWT.DOWN : SWT.DEFAULT);
            spreadsheet.refreshData(false, true, false);
            spreadsheet.redrawGrid();
            return;
        }
        boolean ctrlPressed = (state & SWT.CTRL) == SWT.CTRL;
        boolean altPressed = (state & SWT.ALT) == SWT.ALT;
        controller.toggleSortOrder((DBDAttributeBinding) columnElement,
            ctrlPressed ? IResultSetController.ColumnOrder.ASC :
                (altPressed ? IResultSetController.ColumnOrder.DESC : null));
    }


    ///////////////////////////////////////////////
    // Filtering

    void handleColumnIconClick(Object columnElement) {
        if (!(columnElement instanceof DBDAttributeBinding attributeBinding)) {
            log.debug("Unable to show distinct filter for columnElement" + columnElement);
            return;
        }
        controller.showColumnMenu(attributeBinding);
    }

    ///////////////////////////////////////////////
    // Misc

    public DBPPreferenceStore getPreferenceStore() {
        return controller.getPreferenceStore();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IPropertySheetPage.class) {
            // Show cell properties
            PropertyPageStandard page = new PropertyPageStandard();
            page.setPropertySourceProvider(object -> {
                if (object instanceof GridCell cell) {
                    final DBDAttributeBinding attr = getAttributeFromGrid(cell.col, cell.row);
                    final ResultSetRow row = getResultRowFromGrid(cell.col, cell.row);
                    final SpreadsheetValueController valueController = new SpreadsheetValueController(
                        controller,
                        new ResultSetCellLocation(attr, row, getRowNestedIndexes(cell.row)),
                        IValueController.EditType.NONE, null);
                    PropertyCollector props = new PropertyCollector(valueController.getBinding().getAttribute(), false);
                    props.collectProperties();
                    valueController.getValueManager().contributeProperties(props, valueController);
                    return new PropertySourceDelegate(props);
                }
                return null;
            });
            return adapter.cast(page);
        } else if (adapter == IFindReplaceTarget.class) {
            return adapter.cast(SpreadsheetFindReplaceTarget.getInstance().owned(this));
        }
        return null;
    }

    @Nullable
    @Override
    public DBDAttributeBinding getFocusAttribute() {
        IGridItem gridItem = controller.isRecordMode() ?
            spreadsheet.getFocusRow() :
            spreadsheet.getFocusColumn();
        while (gridItem != null) {
            if (gridItem.getElement() instanceof DBDAttributeBinding ab) {
                return ab;
            }
            gridItem = gridItem.getParent();
        }
        return null;
    }

    @Nullable
    public ResultSetRow getFocusRow() {
        return controller.isRecordMode() ?
            (ResultSetRow) spreadsheet.getFocusColumnElement() :
            (ResultSetRow) spreadsheet.getFocusRowElement();
    }

    ///////////////////////////////////////////////
    // Selection provider

    public int getHighlightScopeFirstLine() {
        return highlightScopeFirstLine;
    }

    public int getHighlightScopeLastLine() {
        return highlightScopeLastLine;
    }

    @Override
    public SpreadsheetSelectionImpl getSelection() {
        return new SpreadsheetSelectionImpl();
    }

    @Override
    public void setSelection(ISelection selection) {
        setSelection(selection, true);
    }

    @Override
    public void setSelection(@NotNull ISelection selection, boolean reflect) {
        if (selection instanceof IResultSetSelection && ((IResultSetSelection) selection).getController() == getController()) {
            // It may occur on simple focus change so we won't do anything
            return;
        }
        spreadsheet.deselectAll();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            List<GridPos> cellSelection = new ArrayList<>();
            for (Object cell : (IStructuredSelection) selection) {
                if (cell instanceof GridPos) {
                    cellSelection.add((GridPos) cell);
                } else {
                    log.warn("Bad selection object: " + cell);
                }
            }
            spreadsheet.selectCells(cellSelection);
            spreadsheet.showSelection();
        }
        if (reflect) {
            fireSelectionChanged(selection);
        }
    }
    
    /**
     * Moves specified columns to delta.
     *
     * @param columns - columns to move
     * @param delta determines where columns should be moved. Negative number means to the left, positive - to the right
     * @return true if all columns were moved, otherwise false
     */
    public boolean shiftColumns(@NotNull List<Object> columns, int delta) {
        if (delta == 0) {
            return false;
        }
        final DBDDataFilter dataFilter = new DBDDataFilter(controller.getModel().getDataFilter());
        List<DBDAttributeConstraint> constraintsToMove = new ArrayList<>(columns.size());
        int pinnedAttrsCount = 0;
        int normalAttrsCount = 0;
        for (Object column : columns) {
            if (column instanceof DBDAttributeBinding) {
                final DBDAttributeConstraint attrConstraint = dataFilter.getConstraint((DBDAttributeBinding) column);
                if (attrConstraint != null) {
                    constraintsToMove.add(attrConstraint);
                    if (attrConstraint.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED)) {
                        pinnedAttrsCount++;
                    } else {
                        normalAttrsCount++;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (pinnedAttrsCount != 0 && normalAttrsCount != 0) {
            return false;
        }
        boolean pin = pinnedAttrsCount > 0;
        int order = delta > 0 ? -1 : 1;
        // right to left while shifting right, left to right while shifting left
        constraintsToMove.sort((a, b) -> Integer.compare(getConstraintPosition(a, pin), getConstraintPosition(b, pin)) * order);
        List<DBDAttributeConstraint> allConstraints = getOrderedConstraints(dataFilter, pin);
        int leftmostIndex = constraintsToMove.stream().mapToInt(c -> getConstraintPosition(c, pin)).min().getAsInt();
        int rightmostIndex = constraintsToMove.stream().mapToInt(c -> getConstraintPosition(c, pin)).max().getAsInt();
        if ((delta < 0 && leftmostIndex + delta < 0) || (delta > 0 && rightmostIndex + delta >= allConstraints.size())) {
            return false;
        }
        // reorder constraints affecting the whole collection of them 
        for (DBDAttributeConstraint constraint : constraintsToMove) {
            int oldIndex = getConstraintPosition(constraint, pin);
            int newIndex = oldIndex + delta;
            allConstraints.remove(constraint);
            allConstraints.add(newIndex, constraint);
        }
        // fix up the positions for all the affected constraints after the order modifications 
        for (int i = 0; i < allConstraints.size(); i++) {
            setConstraintPosition(allConstraints.get(i), pin, i);
        }
        controller.setDataFilter(dataFilter, false);
        spreadsheet.refreshData(false, true, false);
        return true;
    }

    @Override
    public void moveColumn(Object dragColumn, Object dropColumn, DropLocation location) {
        if (dragColumn instanceof DBDAttributeBinding && dropColumn instanceof DBDAttributeBinding) {
            final DBDDataFilter dataFilter = new DBDDataFilter(controller.getModel().getDataFilter());
            final DBDAttributeConstraint dragC = dataFilter.getConstraint((DBDAttributeBinding) dragColumn);
            final DBDAttributeConstraint dropC = dataFilter.getConstraint((DBDAttributeBinding) dropColumn);
            if (dragC == null || dropC == null) {
                return;
            }
            final boolean pin = dragC.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED) && dropC.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED);
            int sourcePosition = getConstraintPosition(dragC, pin);
            int targetPosition = getConstraintPosition(dropC, pin);
            switch (location) {
                case DROP_AFTER:
                    if (sourcePosition > targetPosition && targetPosition < dataFilter.getConstraints().size() - 1) {
                        targetPosition++;
                    }
                    break;
                case DROP_BEFORE:
                    if (sourcePosition < targetPosition && targetPosition > 0) {
                        targetPosition--;
                    }
                    break;
                case SWAP:
                    setConstraintPosition(dropC, pin, sourcePosition);
                    setConstraintPosition(dragC, pin, targetPosition);
                    break;
            }
            if (sourcePosition == targetPosition) {
                return;
            }
            if (location != DropLocation.SWAP) {
                // Reposition columns
                final List<DBDAttributeConstraint> constraints = getOrderedConstraints(dataFilter, pin);
                if (sourcePosition < targetPosition) {
                    for (int i = sourcePosition + 1; i <= targetPosition; i++) {
                        setConstraintPosition(constraints.get(i), pin, i - 1);
                    }
                } else {
                    for (int i = sourcePosition - 1; i >= targetPosition; i--) {
                        setConstraintPosition(constraints.get(i), pin, i + 1);
                    }
                }
                setConstraintPosition(dragC, pin, targetPosition);
            }
            controller.setDataFilter(dataFilter, false);
            spreadsheet.setFocusColumn(targetPosition);
            spreadsheet.refreshData(false, true, false);
        }
    }

    private static int getConstraintPosition(@NotNull DBDAttributeConstraint constraint, boolean pin) {
        if (pin) {
            return constraint.getOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED);
        } else {
            return constraint.getVisualPosition();
        }
    }

    private static void setConstraintPosition(@NotNull DBDAttributeConstraint constraint, boolean pin, int position) {
        if (pin) {
            constraint.setOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED, position);
        } else {
            constraint.setVisualPosition(position);
        }
    }

    @NotNull
    private static List<DBDAttributeConstraint> getOrderedConstraints(@NotNull DBDDataFilter filter, boolean pin) {
        final List<DBDAttributeConstraint> constraints = filter.getConstraints();
        if (pin) {
            return constraints.stream()
                .filter(x -> x.hasOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED))
                .sorted(Comparator.comparing(x -> x.getOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED)))
                .collect(Collectors.toList());
        } else {
            return constraints.stream()
                .sorted(Comparator.comparing(DBDAttributeConstraintBase::getVisualPosition))
                .collect(Collectors.toList());
        }
    }

    @Override
    public boolean isMaximizeSingleColumn() {
        return controller.isRecordMode();
    }

    public Color getBackgroundAdded() {
        return backgroundAdded;
    }

    public Color getBackgroundDeleted() {
        return backgroundDeleted;
    }

    public Color getBackgroundModified() {
        return backgroundModified;
    }

    public Color getBackgroundNormal() {
        return backgroundNormal;
    }

    public Color getBackgroundOdd() {
        return backgroundOdd;
    }

    private DBDAttributeBinding getAttributeFromGrid(IGridColumn colObject, IGridRow rowObject) {
        if (controller.isRecordMode()) {
            if (rowObject == null) {
                return null;
            } else if (rowObject.getElement() instanceof DBDAttributeBinding attr) {
                return attr;
            } else if (rowObject.getElement() instanceof DBSAttributeBase attr) {
                // This may happen in case of dynamic data types
                // Find binding
                for (IGridRow pr = rowObject.getParent(); pr != null; pr = pr.getParent()) {
                    if (pr.getElement() instanceof DBDAttributeBindingType bt) {
                        DBDAttributeBinding ab = DBUtils.findObject(bt.getNestedBindings(), attr.getName());
                        if (ab != null) {
                            return ab;
                        }
                    }
                }
                return controller.getModel().getAttributeBinding(attr);
            } else if (rowObject.getParent() != null) {
                return getAttributeFromGrid(colObject, rowObject.getParent());
            }
            return null;
        } else {
            return colObject == null ? null : (DBDAttributeBinding) colObject.getElement();
        }
    }

    private ResultSetRow getResultRowFromGrid(IGridColumn colObject, IGridRow rowObject) {
        if (controller.isRecordMode()) {
            return colObject == null ? null : (ResultSetRow) colObject.getElement();
        } else {
            return rowObject == null ? null : (ResultSetRow) rowObject.getElement();
        }
    }

        private boolean isAttributeExpandable(@Nullable IGridRow row, @NotNull DBSAttributeBase attr) {
        if (attr.getDataKind() == DBPDataKind.STRUCT && controller.isRecordMode()) {
            return true;
        }

        if (attr instanceof DBDAttributeBinding binding) {
            for (DBDAttributeBinding cur = binding; cur != null; cur = cur.getParentObject()) {
                final DBPDataKind kind = cur.getDataKind();
                if (kind == DBPDataKind.ARRAY) {
                    return true;
                }
            }
        }
        return false;
    }

    class SpreadsheetSelectionImpl implements IResultSetSelection, IResultSetSelectionExt {

        @Nullable
        @Override
        public GridPos getFirstElement() {
            Collection<GridPos> ssSelection = spreadsheet.getSelection();
            if (ssSelection.isEmpty()) {
                return null;
            }
            return ssSelection.iterator().next();
        }

        @NotNull
        @Override
        public Iterator<GridPos> iterator() {
            return spreadsheet.getSelection().iterator();
        }

        @Override
        public int size() {
            return spreadsheet.getSelection().size();
        }

        @Override
        public Object[] toArray() {
            return spreadsheet.getSelection().toArray();
        }

        @Override
        public List<GridPos> toList() {
            return new ArrayList<>(spreadsheet.getSelection());
        }

        @Override
        public boolean isEmpty() {
            return spreadsheet.getSelection().isEmpty();
        }

        @NotNull
        @Override
        public IResultSetController getController() {
            return SpreadsheetPresentation.this.getController();
        }

        @NotNull
        @Override
        public List<DBDAttributeBinding> getSelectedAttributes() {
            if (controller.isRecordMode()) {
                Object[] elements = spreadsheet.getContentProvider().getElements(false);
                List<DBDAttributeBinding> attrs = new ArrayList<>();
                List<Integer> rowSelection = new ArrayList<>(spreadsheet.getRowSelection());
                Collections.sort(rowSelection);
                for (Integer row : rowSelection) {
                    if (row < elements.length) {
                        // Index may be out of bounds in case of complex attributes
                        attrs.add((DBDAttributeBinding) elements[row]);
                    }
                }
                return attrs;
            } else {
                List<DBDAttributeBinding> attrs = new ArrayList<>();
                for (IGridColumn row : spreadsheet.getColumnSelection()) {
                    attrs.add((DBDAttributeBinding) row.getElement());
                }
                return attrs;
            }
        }

        @NotNull
        @Override
        public List<ResultSetRow> getSelectedRows() {
            {
                List<ResultSetRow> rows = new ArrayList<>();
                if (controller.isRecordMode()) {
                    for (IGridColumn col : spreadsheet.getColumnSelection()) {
                        if (col.getElement() instanceof ResultSetRow) {
                            rows.add((ResultSetRow) col.getElement());
                        }
                    }
                } else {
                    for (Integer row : spreadsheet.getRowSelection()) {
                        IGridRow gridRow = spreadsheet.getRow(row);
                        ResultSetRow rsr = (ResultSetRow) gridRow.getElement();
                        if (!rows.contains(rsr)) {
                            rows.add(rsr);
                        }
                    }
                }
                rows.sort(Comparator.comparingInt(ResultSetRow::getVisualNumber));
                return rows;
            }
        }

        @Override
        public DBDAttributeBinding getElementAttribute(Object element) {
            GridPos pos = (GridPos) element;
            return getAttributeFromGrid(
                spreadsheet.getColumn(pos.col),
                spreadsheet.getRow(pos.row)
            );
        }

        @Override
        public ResultSetRow getElementRow(Object element) {
            GridPos pos = (GridPos) element;
            return getResultRowFromGrid(
                spreadsheet.getColumn(pos.col),
                spreadsheet.getRow(pos.row)
            );
        }

        @Override
        public int[] getElementRowIndexes(Object element) {
            IGridRow row = spreadsheet.getRow(((GridPos) element).row);
            return getRowNestedIndexes(row);
        }

        @Override
        public int getSelectedColumnCount() {
            return spreadsheet.getColumnSelectionSize();
        }

        @Override
        public int getSelectedRowCount() {
            return spreadsheet.getRowSelectionSize();
        }

        @Override
        public int getSelectedCellCount() {
            return spreadsheet.getCellSelectionSize();
        }
    }

    private class ContentProvider implements IGridContentProvider {

        @NotNull
        @Override
        public Object[] getElements(boolean horizontal) {
            boolean recordMode = controller.isRecordMode();
            ResultSetModel model = controller.getModel();
            if (horizontal) {
                // columns
                if (!recordMode) {
                    return model.getVisibleAttributes().toArray();
                } else {
                    int[] selectedRecords = controller.getSelectedRecords();
                    List<Object> rows = new ArrayList<>(selectedRecords.length);
                    for (int i = 0; i < selectedRecords.length; i++) {
                        if (selectedRecords[i] < controller.getModel().getRowCount()) {
                            rows.add(controller.getModel().getRow(selectedRecords[i]));
                        }
                    }
                    return rows.toArray();
                }
            } else {
                // rows
                if (!recordMode) {
                    return model.getAllRows().toArray();
                } else {
                    DBDAttributeBinding[] columns = model.getVisibleAttributes().toArray(new DBDAttributeBinding[model.getVisibleAttributeCount()]);
                    if (columnOrder != SWT.NONE && columnOrder != SWT.DEFAULT) {
                        Arrays.sort(columns, (o1, o2) -> o1.getName().compareTo(o2.getName()) * (columnOrder == SWT.UP ? 1 : -1));
                    }
                    return columns;
                }
            }
        }

        @Override
        public boolean hasChildren(@NotNull IGridItem item) {
            if (item.getElement() instanceof DBSAttributeBase attr) {
                return switch (attr.getDataKind()) {
                    case DOCUMENT, ANY -> !controller.isRecordMode();
                    default -> isAttributeExpandable(null, attr);
                };
            } else if (controller.isRecordMode()) {
                if (item.getElement() instanceof DBDComplexValue) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        @Override
        public Object[] getChildren(@NotNull IGridItem item) {
            if (item.getElement() instanceof DBDAttributeBinding binding) {
                if (controller.isRecordMode() && binding.getDataKind() == DBPDataKind.ARRAY && controller.getCurrentRow() != null) {
                    Object cellValue = controller.getModel().getCellValue(
                        binding,
                        controller.getCurrentRow(),
                        getRowNestedIndexes(item),
                        false);
                    if (cellValue instanceof Collection<?> col) {
                        return col.toArray();
                    } else if (cellValue instanceof DBDComposite composite) {
                        return null;
                    } else {
                        return null;
                    }
                }
                switch (binding.getDataKind()) {
                    case ARRAY:
                    case STRUCT:
                    case DOCUMENT:
                    case ANY:
                        final List<DBDAttributeBinding> children = controller.getModel().getVisibleAttributes(binding);
                        if (!CommonUtils.isEmpty(children)) {
                            return children.toArray();
                        }
                        break;
                }
            } else if (item.getElement() instanceof Collection<?> col) {
                return col.toArray();
            } else if (item.getElement() instanceof DBDComposite composite) {
                // This happens in record mode and dynamic databases
                return composite.getAttributes();
            }

            return null;
        }

        @Override
        public int getCollectionSize(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement) {
            final ResultSetRow row = getResultRowFromGrid(colElement, rowElement);
            final DBDAttributeBinding attr  = getAttributeFromGrid(colElement, rowElement);


            // Get indexes for parent node
            int[] nestedIndexes = getRowNestedIndexes(rowElement);
            final ResultSetCellLocation cellLocation = new ResultSetCellLocation(attr, row, nestedIndexes);
            final Object cellValue = controller.getModel().getCellValue(cellLocation);

            if (cellValue instanceof List<?>) {
                return ((List<?>) cellValue).size();
            } else if (cellValue instanceof DBDComposite composite && controller.isRecordMode()) {
                return composite.getAttributeCount();
            } else {
                return 0;
            }
        }

        @Override
        public int getSortOrder(@Nullable IGridColumn column) {
            if (column != null && column.getElement() instanceof DBDAttributeBinding binding) {
                if (!binding.hasNestedBindings()) {
                    DBDAttributeConstraint co = controller.getModel().getDataFilter().getConstraint(binding);
                    if (co != null && co.getOrderPosition() > 0) {
                        return co.isOrderDescending() ? SWT.DOWN : SWT.UP;
                    }
                    return SWT.DEFAULT;
                }
            } else if (column == null && controller.isRecordMode()) {
                // Columns order in record mode
                return columnOrder;
            }
            return SWT.NONE;
        }

        @Override
        public ElementState getDefaultState(@NotNull IGridColumn element) {
            if (element.getElement() instanceof DBDAttributeBinding binding) {
                switch (binding.getAttribute().getDataKind()) {
                    case STRUCT:
                    case DOCUMENT:
                    case ANY:
                        return ElementState.EXPANDED;
                    case ARRAY:
                        ResultSetRow curRow = controller.getCurrentRow();
                        if (curRow != null) {
                            Object cellValue = controller.getModel().getCellValue(
                                new ResultSetCellLocation(binding, curRow));
                            if (cellValue instanceof List<?>) {
                                if (((List<?>) cellValue).size() < 3) {
                                    return ElementState.EXPANDED;
                                }
                                if (!DBUtils.isNullValue(cellValue)) {
                                    return ElementState.COLLAPSED;
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            return ElementState.NONE;
        }

        @Override
        public IGridStatusColumn[] getStatusColumns() {
            return new IGridStatusColumn[] {
                new IGridStatusColumn() {
                    @Override
                    public String getDisplayName() {
                        return ResultSetMessages.controls_resultset_results_read_only_status;
                    }

                    @Override
                    public String getStatusText() {
                        return getController().getReadOnlyStatus();
                    }

                    @Override
                    public DBPImage getStatusIcon() {
                        if (getController().getReadOnlyStatus() != null) {
                            return UIIcon.BUTTON_READ_ONLY;
                        }
                        return null;
                    }
                },
                new IGridStatusColumn() {
                    @Override
                    public String getDisplayName() {
                        return ResultSetMessages.controls_resultset_results_edit_key;
                    }

                    @Override
                    public String getStatusText() {
                        DBDRowIdentifier rowIdentifier = getController().getModel().getDefaultRowIdentifier();
                        if (rowIdentifier != null && !rowIdentifier.isIncomplete()) {
                            return
                                rowIdentifier.getUniqueKey().getConstraintType().getName() + " " +
                                rowIdentifier.getAttributes().stream()
                                    .map(DBDAttributeBinding::getName).collect(Collectors.joining(","));
                        } else {
                            DBSDataContainer dataContainer = getController().getDataContainer();
                            if (dataContainer instanceof DBSEntity && !dataContainer.isFeatureSupported(DBSDataManipulator.FEATURE_DATA_UPDATE)) {
                                return "Data modification is not supported by database.";
                            }
                            if (rowIdentifier == null) {
                                return "Table metadata not found. Data edit is not possible.";
                            }
                            if (rowIdentifier.isIncomplete()) {
                                return "No unique key was found. Data modification is not possible.";
                            }
                            return "Virtual key is used";
                        }
                    }

                    @Override
                    public DBPImage getStatusIcon() {
                        DBDRowIdentifier rowIdentifier = getController().getModel().getDefaultRowIdentifier();
                        if (rowIdentifier == null) {
                            return ResultSetIcons.META_TABLE_NA;
                        } else if (rowIdentifier.isIncomplete()) {
                            return ResultSetIcons.META_KEY_NA;
                        } else if (rowIdentifier.getUniqueKey() instanceof DBVEntityConstraint) {
                            return ResultSetIcons.META_KEY_VIRTUAL;
                        } else {
                            return ResultSetIcons.META_KEY_OK;
                        }
                    }
                },
            };
        }

        @Override
        public boolean isVoidCell(IGridColumn gridColumn, IGridRow gridRow) {
            return getCellValue(gridColumn, gridRow, false) == DBDVoid.INSTANCE;
        }

        @Override
        public int getColumnPinIndex(@NotNull IGridColumn element) {
            if (!controller.isRecordMode()) {
                DBDAttributeBinding attr = (DBDAttributeBinding) element.getElement();
                DBDAttributeConstraint ac = controller.getModel().getDataFilter().getConstraint(attr);
                if (ac != null) {
                    Integer pinIndex = ac.getOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED);
                    return pinIndex == null ? -1 : pinIndex;
                }
            }
            return -1;
        }

        @Override
        public boolean isElementSupportsFilter(IGridColumn element) {
            if (element != null && element.getElement() instanceof DBDAttributeBinding) {
                return supportsAttributeFilter;
            }
            return false;
        }

        @Override
        public boolean isElementSupportsSort(@Nullable IGridColumn element) {
            if (element != null && element.getElement() instanceof DBDAttributeBinding) {
                return showAttrOrdering;
            }
            return false;
        }

        @Override
        public boolean isElementReadOnly(IGridColumn element) {
            if (element.getElement() instanceof DBDAttributeBinding) {
                return controller.getAttributeReadOnlyStatus(
                    (DBDAttributeBinding) element.getElement(),
                    true, true) != null;
            }
            return false;
        }

        @Override
        public boolean isElementExpandable(@NotNull IGridItem item) {
            if (item instanceof IGridRow row) {
                return hasExpandableElements(row);
            }
            return false;
        }

        private boolean hasExpandableElements(@NotNull IGridRow row) {
            for (int i = 0; i < spreadsheet.getColumnCount(); i++) {
                Object cellValue = getCellValue(spreadsheet.getColumn(i), row, false);
                if (cellValue instanceof DBDComplexValue) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isGridReadOnly() {
            return controller.isAllAttributesReadOnly();
        }

        @Override
        public void validateDataPresence(IGridColumn gridColumn, IGridRow gridRow) {
            // Check for next segment read
            ResultSetRow row = getResultRowFromGrid(gridColumn, gridRow);
            int rowNum = row.getVisualNumber();
            if (rowNum > 0 &&
                rowNum == controller.getModel().getRowCount() - 1 &&
                autoFetchSegments &&
                !controller.isRefreshInProgress() &&
                !(controller.getContainer().getDataContainer() != null && controller.getContainer().getDataContainer().isFeatureSupported(DBSDataContainer.FEATURE_DATA_MODIFIED_ON_REFRESH)) &&
                !(getPreferenceStore().getInt(ModelPreferences.RESULT_SET_MAX_ROWS) < getSpreadsheet().getMaxVisibleRows()) &&
                (controller.isRecordMode() || spreadsheet.isRowVisible(rowNum))) {
                controller.readNextSegment();
            }
        }

        @NotNull
        @Override
        public CellInformation getCellInfo(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement, boolean selected) {
            DBDAttributeBinding attr = getAttributeFromGrid(colElement, rowElement);
            ResultSetRow row = getResultRowFromGrid(colElement, rowElement);

            CellInformation info = new CellInformation();
            Object cellValue = row == null || attr == null ? null : getCellValue(colElement, rowElement, false);

            info.value = cellValue;
            info.text = formatValue(colElement, rowElement, info.value);
            info.state = STATE_NONE;

            if (attr != null && cellValue != DBDVoid.INSTANCE) {

                // State
                if ((controller.getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_LINKS) != 0) {
                    //ResultSetRow row = (ResultSetRow) (recordMode ? colElement.getElement() : rowElement.getElement());
                    if (isShowAsCheckbox(attr)) {
                        info.state |= booleanStyles.getMode() == BooleanMode.TEXT ? STATE_TOGGLE : STATE_LINK;
                    } else if (
                        (cellValue instanceof DBDCollection col && !col.isEmpty()) ||
                        (cellValue instanceof DBDComposite && controller.isRecordMode())
                    ) {
                        if (!DBUtils.isNullValue(cellValue)) {
                            info.state |= STATE_LINK;
                        }
                    } else {
                        final String strValue = info.text != null
                            ? info.text.toString()
                            : attr.getValueHandler().getValueDisplayString(attr, cellValue, DBDDisplayFormat.UI);
                        if (strValue != null && isHyperlinkText(strValue)) {
                            try {
                                new URL(strValue);
                                info.state |= STATE_HYPER_LINK;
                            } catch (MalformedURLException e) {
                                // Not a hyperlink
                            }
                        }
                    }
                }
                if (showWhitespaceCharacters) {
                    info.state |= STATE_DECORATED;
                }
                if (attr.isTransformed()) {
                    info.state |= STATE_TRANSFORMED;
                }
            }

            if (CommonUtils.isBitSet(info.state, STATE_LINK)) {
                info.align = ALIGN_LEFT;
            } else {
                info.align = getCellAlign(attr, row, cellValue);
            }

            if (attr != null && cellValue != DBDVoid.INSTANCE) {
                // Image
                if (booleanStyles.getMode() != BooleanMode.TEXT) {
                    if (isShowAsCheckbox(attr)) {
                        if (cellValue instanceof Number) {
                            cellValue = ((Number) cellValue).byteValue() != 0;
                        }
                        if (cellValue instanceof Boolean || cellValue == null) {
                            info.image = booleanStyles.getStyle((Boolean) cellValue).getIcon();
                        }
                    }
                }
                // Collections
                if (info.image == null && cellValue instanceof DBDComplexValue cv && !cv.isNull() &&
                    (!(cellValue instanceof Collection<?> col && col.isEmpty()))
                ) {
                    final GridCell cell = new GridCell(colElement, rowElement);
                    boolean cellExpanded = spreadsheet.isCellExpanded(cell);
                    info.state |= cellExpanded ? IGridContentProvider.STATE_EXPANDED : IGridContentProvider.STATE_COLLAPSED;
                    info.image = cellExpanded ? UIIcon.TREE_COLLAPSE : UIIcon.TREE_EXPAND;
                }
            }

            {
                // Background
                info.background = getCellBackground(
                    attr, row, cellValue, rowElement.getVisualPosition(), selected, false);

                // Foreground
                info.foreground = getCellForeground(attr, row, cellValue, info.background, selected);

                // Font
                if (row != null && attr != null && isShowAsCheckbox(attr)) {
                    if (cellValue instanceof Number) {
                        cellValue = ((Number) cellValue).byteValue() != 0;
                    }
                    if ((DBUtils.isNullValue(cellValue) || cellValue instanceof Boolean) &&
                        booleanStyles.getMode() != BooleanMode.ICON)
                    {
                        info.font = spreadsheet.getFont(booleanStyles.getStyle((Boolean) cellValue).getFontStyle());
                    }
                }/* else if (isShowAsCollection(rowElement, colElement, cellValue)) {
                    info.font = spreadsheet.getFont(UIElementFontStyle.ITALIC);
                }*/
            }
            return info;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }

        @Nullable
        @Override
        public Object getCellValue(IGridColumn gridColumn, IGridRow gridRow, boolean formatString) {
            final Object value = getCellValue(gridColumn, gridRow, getRowNestedIndexes(gridRow), false);
            if (formatString) {
                return formatValue(gridColumn, gridRow, value);
            } else {
                return value;
            }
        }

        @Nullable
        public Object getCellValue(
            @NotNull IGridColumn gridColumn,
            @NotNull IGridRow gridRow,
            @Nullable int[] rowIndexes,
            boolean retrieveDeepestCollectionElement
        ) {
            if (gridRow.getParent() != null && !spreadsheet.isCellExpanded(gridRow.getParent(), gridColumn)) {
                return DBDVoid.INSTANCE;
            }
            DBDAttributeBinding attr = getAttributeFromGrid(gridColumn, gridRow);
            ResultSetRow row = getResultRowFromGrid(gridColumn, gridRow);
            if (attr == null || row == null) {
                return null;
            }

            return controller.getModel().getCellValue(attr, row, rowIndexes, retrieveDeepestCollectionElement);
        }

        @Nullable
        private Object formatValue(@NotNull IGridColumn gridColumn, @NotNull IGridRow gridRow, @Nullable Object value) {
            final DBDAttributeBinding attr = getAttributeFromGrid(gridColumn, gridRow);
            final ResultSetRow row = getResultRowFromGrid(gridColumn, gridRow);
            if (attr == null || row == null) {
                return null;
            }

            if (DBUtils.isNullValue(value) && row.getState() == ResultSetRow.STATE_ADDED) {
                // New row and no value. Let's try to show default value
                DBSEntityAttribute entityAttribute = attr.getEntityAttribute();
                if (entityAttribute != null) {
                    String defaultValue = entityAttribute.getDefaultValue();
                    if (defaultValue != null && !SQLConstants.NULL_VALUE.equalsIgnoreCase(defaultValue)) {
                        value = defaultValue;
                    }
                }
            }

            if (value instanceof DBDValueError) {
                return ((DBDValueError) value).getErrorTitle();
            }

            if ((value instanceof Boolean || value instanceof Number || value == null) && isShowAsCheckbox(attr)) {
                if (booleanStyles.getMode() != BooleanMode.TEXT) {
                    return "";
                }
                if (value instanceof Number) {
                    value = ((Number) value).byteValue() != 0;
                }
                if (booleanStyles.getMode() == BooleanMode.TEXT) {
                    return booleanStyles.getStyle((Boolean) value).getText();
                }
                return value;
            }
            if (value instanceof DBDCollection collection && !collection.isNull()) {
                if (isShowAsCollection(gridRow, gridColumn, value)) {
                    if (collection.isEmpty()) {
                        return "";
                    } else {
                        return "[" + collection.size() + "]";
                    }
                }
                Object child = getCellValue(gridColumn, gridRow, getRowNestedIndexes(gridRow), true);
                if (child == value) {
                    return value;
                }
                return formatValue(gridColumn, gridRow, child);
            } else if (value instanceof DBDComposite composite && !DBUtils.isNullValue(value)) {
//                return Arrays.stream(composite.getAttributes())
//                    .map(DBPNamedObject::getName)
//                    .collect(Collectors.joining(",", "[", "]"));
                return composite.toString();
            }
            try {
                return attr.getValueRenderer().getValueDisplayString(
                    attr.getAttribute(),
                    value,
                    getValueRenderFormat(attr, value));
            } catch (Exception e) {
                return new DBDValueError(e);
            }
        }

        public int getCellAlign(@Nullable DBDAttributeBinding attr, ResultSetRow row, Object cellValue) {
            if (!controller.isRecordMode()) {
                if (attr != null) {
                    if (cellValue instanceof DBDCollection) {
                        return ALIGN_LEFT;
                    }
                    if (isShowAsCheckbox(attr)) {
                        if (row.getState() == ResultSetRow.STATE_ADDED) {
                            return ALIGN_CENTER;
                        }
                        if (cellValue instanceof Number number) {
                            cellValue = number.byteValue() != 0;
                        }
                        if (DBUtils.isNullValue(cellValue) || cellValue instanceof Boolean) {
                            return switch (booleanStyles.getStyle((Boolean) cellValue).getAlignment()) {
                                case LEFT -> ALIGN_LEFT;
                                case CENTER -> ALIGN_CENTER;
                                case RIGHT -> ALIGN_RIGHT;
                            };
                        }
                    }
                    DBPDataKind dataKind = attr.getDataKind();
                    if ((dataKind == DBPDataKind.NUMERIC && rightJustifyNumbers) ||
                        (dataKind == DBPDataKind.DATETIME && rightJustifyDateTime)) {
                        if (CommonUtils.isEmpty(attr.getReferrers()) && !attr.isInRowIdentifier()) {
                            return ALIGN_RIGHT;
                        }
                    }
                }
            }
            return ALIGN_LEFT;
        }

        @Nullable
        private Color getCellForeground(DBDAttributeBinding attribute, ResultSetRow row, Object cellValue, Color background, boolean selected) {
            if (selected) {
                return foregroundSelected;
            }
            if (isShowAsCheckbox(attribute) && booleanStyles.getMode() == BooleanMode.TEXT) {
                if (cellValue instanceof Number number) {
                    cellValue = number.byteValue() != 0;
                }
                if (DBUtils.isNullValue(cellValue) || cellValue instanceof Boolean) {
                    return UIUtils.getSharedColor(booleanStyles.getStyle((Boolean) cellValue).getColor());
                }
                return null;
            }

            final DBDAttributeDecorator dataLabelProvider = getController().getDecorator().getDataLabelProvider();
            if (dataLabelProvider != null) {
                final String fg = dataLabelProvider.getCellForeground(attribute, row.getVisualNumber());
                if (fg != null) {
                    return UIUtils.getSharedColor(fg);
                }
            }

            {
                if (row.colorInfo != null) {
                    if (row.colorInfo.cellFgColors != null) {
                        Color cellFG = row.colorInfo.cellFgColors[attribute.getOrdinalPosition()];
                        if (cellFG != null) {
                            return cellFG;
                        }
                    }
                    if (row.colorInfo.rowForeground != null) {
                        return row.colorInfo.rowForeground;
                    }
                }

                if (DBUtils.isNullValue(cellValue)) {
                    return foregroundNull;
                } else {
                    if (colorizeDataTypes) {
                        Color color = dataTypesForegrounds.get(attribute.getDataKind());
                        if (color != null) {
                            return color;
                        }
                    }
                }
            }
            return UIUtils.getContrastColor(background);
        }

        private Color getCellBackground(
            DBDAttributeBinding attribute,
            ResultSetRow row,
            Object cellValue,
            int rowPosition,
            boolean cellSelected,
            boolean ignoreRowSelection)
        {
            if (cellValue == DBDVoid.INSTANCE) {
                return cellHeaderBackground;
            }

            if (spreadsheet.getCellSelectionSize() == 1 && getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_MARK_CELL_VALUE_OCCURRENCES)) {
                final GridCell sourceCell = spreadsheet.getCursorCell();
                if (sourceCell != null) {
                    final ResultSetRow sourceRow = getResultRowFromGrid(sourceCell.col, sourceCell.row);
                    final DBDAttributeBinding sourceAttribute = getAttributeFromGrid(sourceCell.col, sourceCell.row);

                    if (sourceRow != row || sourceAttribute != attribute) {
                        final Object sourceValue = spreadsheet.getContentProvider().getCellValue(sourceCell.col, sourceCell.row, false);

                        if (CommonUtils.equalObjects(sourceValue, cellValue)) {
                            return backgroundMatched;
                        }
                    }
                }
            }

            if (cellSelected) {
                Color normalColor = getCellBackground(attribute, row, cellValue, rowPosition, false, true);
                if (normalColor == null || normalColor == backgroundNormal || isHighContrastTheme) {
                    return backgroundSelected;
                }
                RGB mixRGB = UIUtils.blend(
                    normalColor.getRGB(),
                    backgroundSelected.getRGB(),
                    50
                );
                return UIUtils.getSharedTextColors().getColor(mixRGB);
            }

            final SpreadsheetFindReplaceTarget findReplaceTarget = SpreadsheetFindReplaceTarget
                .getInstance()
                .owned(SpreadsheetPresentation.this);

            if (findReplaceTarget.isSessionActive()) {
                boolean hasScope = highlightScopeFirstLine >= 0 && highlightScopeLastLine >= 0;
                boolean inScope = hasScope &&
                    rowPosition >= highlightScopeFirstLine &&
                    rowPosition <= highlightScopeLastLine;
                if (!hasScope || inScope) {
                    java.util.regex.Pattern searchPattern = findReplaceTarget.getSearchPattern();
                    if (searchPattern != null) {
                        String cellText = CommonUtils.toString(cellValue);
                        if (searchPattern.matcher(cellText).find()) {
                            return backgroundMatched;
                        }
                    }
                }
                if (!controller.isRecordMode() && inScope) {
                    return highlightScopeColor != null ? highlightScopeColor : backgroundSelected;
                }
            }

            if (!ignoreRowSelection && highlightRowsWithSelectedCells && spreadsheet.isRowSelected(rowPosition)) {
                Color normalColor = getCellBackground(attribute, row, cellValue, rowPosition, false, true);
                Color selectedCellColor;
                if (normalColor == null || normalColor == backgroundNormal || isHighContrastTheme) {
                    selectedCellColor = backgroundSelected;
                } else {
                    RGB mixRGB = UIUtils.blend(
                        normalColor.getRGB(),
                        backgroundSelected.getRGB(),
                        50
                    );
                    selectedCellColor = UIUtils.getSharedTextColors().getColor(mixRGB);
                }
                RGB mixRGB = UIUtils.blend(
                    selectedCellColor.getRGB(),
                    normalColor.getRGB(),
                    25
                );
                return UIUtils.getSharedTextColors().getColor(mixRGB);
            }

            final DBDAttributeDecorator dataLabelProvider = getController().getDecorator().getDataLabelProvider();
            if (dataLabelProvider != null) {
                final String bg = dataLabelProvider.getCellBackground(attribute, row.getVisualNumber());
                if (bg != null) {
                    return UIUtils.getSharedColor(bg);
                }
            }

            switch (row.getState()) {
                case ResultSetRow.STATE_ADDED:
                    return backgroundAdded;
                case ResultSetRow.STATE_REMOVED:
                    return backgroundDeleted;
            }
            if (row.isChanged(attribute)) {
                return backgroundModified;
            }

            {
                if (row.colorInfo != null) {
                    if (row.colorInfo.cellBgColors != null) {
                        Color cellBG = row.colorInfo.cellBgColors[attribute.getOrdinalPosition()];
                        if (cellBG != null) {
                            return cellBG;
                        }
                    }
                    if (row.colorInfo.rowBackground != null) {
                        return row.colorInfo.rowBackground;
                    }
                }

                if (cellValue != null && cellValue.getClass() == DBDValueError.class) {
                    return backgroundError;
                }
            }

            if (!controller.isRecordMode() && showOddRows && !isHighContrastTheme) {
                // Determine odd/even row
                if (rowBatchSize < 1) {
                    rowBatchSize = 1;
                }

                int rowNumber = row.getVisualNumber();
                int rowRelativeNumber = rowNumber % (rowBatchSize * 2);

                boolean odd = rowRelativeNumber < rowBatchSize;
                if (odd) {
                    return backgroundOdd;
                }
            }

            if (backgroundNormal == null) {
                backgroundNormal = controller.getDefaultBackground();
            }
            return backgroundNormal;
        }

        @NotNull
        @Override
        public String getCellLinkText(IGridColumn colElement, IGridRow rowElement) {
            DBDAttributeBinding attr = getAttributeFromGrid(colElement, rowElement);
            ResultSetRow row = getResultRowFromGrid(colElement, rowElement);
            Object value = controller.getModel().getCellValue(
                new ResultSetCellLocation(attr, row, getRowNestedIndexes(rowElement)));

            List<IGridHint> cellHints = getCellHints(colElement, rowElement, value, DBDValueHintProvider.OPTION_ACTION_TOOLTIP);
            if (cellHints != null) {
                for (IGridHint hint : cellHints) {
                    if (hint.hasAction()) {
                        String hintText = hint.getActionToolTip();
                        if (hintText != null) {
                            return hintText;
                        }
                    }
                }
            }
            return "";
        }

        @Override
        public String getCellToolTip(IGridColumn colElement, IGridRow rowElement) {
            Object cellValue = getCellValue(colElement, rowElement, false);
            StringBuilder toolTip = new StringBuilder();
            toolTip.append(formatValue(colElement, rowElement, cellValue));

            // Add tips
            boolean hasHints = false;
            List<IGridHint> cellHints = getCellHints(colElement, rowElement, cellValue, DBDValueHintProvider.OPTION_TOOLTIP);
            if (cellHints != null) {
                for (IGridHint hint : cellHints) {
                    String hintText = hint.getText();
                    if (hintText != null) {
                        String hintLabel = hint.getHintLabel();
                        toolTip.append("\n");
                        if (hintLabel != null) {
                            toolTip.append(hintLabel).append(": ");
                        }
                        toolTip.append(hintText);
                        hasHints = true;
                    }
                }
            }
            if (hasHints) {
                toolTip.insert(0, "Value: ");
            }
            return toolTip.toString();
        }

        @Override
        public List<IGridHint> getCellHints(IGridColumn colElement, IGridRow rowElement, Object cellValue, int options) {
            DBDAttributeBinding attr = getAttributeFromGrid(colElement, rowElement);
            if (attr == null) {
                return null;
            }
            ResultSetRow row = getResultRowFromGrid(colElement, rowElement);
            if (row == null) {
                return null;
            }
            CellInformation cellInfo = getCellInfo(colElement, rowElement, false);
            int hintOptions = options;
            if ((IGridContentProvider.STATE_EXPANDED & cellInfo.state) != 0) {
                hintOptions |= DBDValueHintProvider.OPTION_ROW_EXPANDED;
            }

            List<IGridHint> gridHints = null;
            for (DBDValueHintProvider hintProvider : controller.getModel().getHintProviders(attr)) {
                DBDValueHint[] valueHints = hintProvider.getValueHint(
                    controller.getModel().getHintContext(),
                    attr,
                    row,
                    cellValue,
                    INLINE_HINT_TYPES,
                    hintOptions
                );
                if (valueHints != null) {
                    for (DBDValueHint hint : valueHints) {
                        if (gridHints == null) {
                            gridHints = new ArrayList<>();
                        }
                        gridHints.add(new SpreadsheetHint(getController(), hint));
                    }
                }
            }
            return gridHints;
        }

        @Override
        public int getColumnHintsWidth(IGridColumn colElement) {
            DBDAttributeBinding attr = colElement.getElement() instanceof DBDAttributeBinding ab ? ab : null;
            if (attr == null) {
                return 0;
            }
            int hintSize = 0;
            for (DBDValueHintProvider hintProvider : controller.getModel().getHintProviders(attr)) {
                hintSize += hintProvider.getAttributeHintSize(
                    controller.getModel().getHintContext(),
                    attr);
            }
            return hintSize;
        }

        @Override
        public void resetColors() {
            backgroundNormal = null;
            foregroundDefault = null;
        }
    }

    private static boolean isHyperlinkText(String strValue) {
        return strValue.startsWith("http://") || strValue.startsWith("https://");
    }

    public ResultSetCellLocation getCurrentCellLocation() {
        DBDAttributeBinding currentAttribute = getCurrentAttribute();
        ResultSetRow currentRow = getController().getCurrentRow();
        IGridRow focusRow = spreadsheet.getFocusRow();
        return new ResultSetCellLocation(currentAttribute, currentRow, getRowNestedIndexes(focusRow));
    }

    public ResultSetCellLocation getCellLocation(GridCell cell) {
        final boolean recordMode = getController().isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row.getElement() : cell.col.getElement());
        final ResultSetRow row = (ResultSetRow)(recordMode ? cell.col.getElement() : cell.row.getElement());
        return new ResultSetCellLocation(attr, row, getRowNestedIndexes(cell.row));
    }

    @Nullable
    private int[] getRowNestedIndexes(IGridItem gridRow) {
        int[] nestedIndexes = null;
        if (gridRow != null && gridRow.getParent() != null) {
            nestedIndexes = new int[gridRow.getLevel()];
            if (controller.isRecordMode()) {
                // In record mode attributes hierarchy includes struct attributes too
                // Leave only array indexes. For each row we find it's array attribute
                // and use it only once
                int lastIndex = nestedIndexes.length;
                for (IGridItem gr = gridRow; gr.getParent() != null; gr = gr.getParent()) {
                    if (hasParentArrayRow(gr)) {
                        nestedIndexes[lastIndex - 1] = gr.getRelativeIndex();
                        lastIndex--;
                    }
                }
                if (lastIndex == nestedIndexes.length) {
                    return null;
                }
                int indexesCount = gridRow.getLevel() - lastIndex;
                if (indexesCount != nestedIndexes.length) {
                    int[] indexesCopy = new int[indexesCount];
                    System.arraycopy(nestedIndexes, lastIndex, indexesCopy, 0, indexesCount);
                    nestedIndexes = indexesCopy;
                }
            } else {
                for (IGridItem gr = gridRow; gr.getParent() != null; gr = gr.getParent()) {
                    nestedIndexes[gr.getLevel() - 1] = gr.getRelativeIndex();
                }
            }
        }
        return nestedIndexes;
    }

    // Checks whether there is parent row with attribute of type array
    // We need to check this recursively because of multi-dimensional arrays
    // where we may have multiple levels of nested rows for a single array attribute
    private boolean hasParentArrayRow(IGridItem item) {
        for (IGridItem gr = item.getParent(); gr != null; gr = gr.getParent()) {
            if (gr.getElement() instanceof DBSTypedObject b) {
                return b.getDataKind() == DBPDataKind.ARRAY;
            } else if (gr.getElement() instanceof DBDComposite) {
                return false;
            }
        }
        return false;
    }

    private DBDDisplayFormat getValueRenderFormat(DBDAttributeBinding attr, Object value) {
        if (value instanceof Number && useNativeNumbersFormat) {
            return DBDDisplayFormat.NATIVE;
        }
        return gridValueFormat;
    }

    @Override
    public DBDDisplayFormat getDefaultDisplayFormat() {
        return gridValueFormat;
    }

    @Override
    public void setDefaultDisplayFormat(DBDDisplayFormat displayFormat) {
        this.gridValueFormat = displayFormat;
        getPreferenceStore().setValue(ResultSetPreferences.RESULT_GRID_VALUE_FORMAT, this.gridValueFormat.name());
    }

    private boolean isShowAsCheckbox(DBDAttributeBinding attr) {
        return showBooleanAsCheckbox && attr.getPresentationAttribute().getDataKind() == DBPDataKind.BOOLEAN;
    }

    private boolean isShowAsCollection(@NotNull IGridRow row, @NotNull IGridColumn column, @Nullable Object value) {
        return value instanceof DBDCollection collection
            && !collection.isNull()
            && (collection.isEmpty() || spreadsheet.isCellExpanded(row, column));
    }

    private class GridLabelProvider implements IGridLabelProvider {
        @Nullable
        @Override
        public Image getImage(IGridItem item) {
            if (!showAttributeIcons) {
                return null;
            }

            if (item.getElement() instanceof DBDAttributeBinding attr) {
                DBPImage image = DBValueFormatting.getObjectImage(attr.getAttribute());

                boolean attributeReadOnly = isAttributeReadOnly(attr);
                boolean hasReferences = !CommonUtils.isEmpty(attr.getReferrers());
                DBDRowIdentifier rowIdentifier = attr.getRowIdentifier();
                boolean isKey = rowIdentifier != null && rowIdentifier.getAttributes().contains(attr);
                if (attributeReadOnly || hasReferences || isKey) {
                    image = new DBIconComposite(image, false,
                        null,
                        attributeReadOnly ? DBIcon.OVER_LOCK : null,
                        isKey ? DBIcon.OVER_KEY : null,
                        hasReferences ? DBIcon.OVER_REFERENCE : null);
                }

                return DBeaverIcons.getImage(image);
            } else if (item.getElement() instanceof DBSAttributeBase attrBase) {
                return DBeaverIcons.getImage(
                    DBValueFormatting.getObjectImage(attrBase));
            }

            return null;
        }

        private boolean isAttributeReadOnly(@NotNull DBDAttributeBinding attr) {
            return
                CommonUtils.isBitSet(controller.getDecorator().getDecoratorFeatures(), IResultSetDecorator.FEATURE_EDIT)
                && controller.getAttributeReadOnlyStatus(attr, true, true) != null
                && !controller.isAllAttributesReadOnly();
        }

        @Override
        public Object getGridOption(String option) {
            if (OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC.equals(option)) {
                return calcColumnWidthByValue;
            }
            return null;
        }

        @Nullable
        @Override
        public Color getForeground(IGridItem element) {
            if (element == null) {
                if (foregroundDefault == null) {
                    foregroundDefault = controller.getDefaultForeground();
                }
                return foregroundDefault;
            }
            return null;
        }

        @Nullable
        @Override
        public Color getBackground(IGridItem element) {
            if (backgroundNormal == null) {
                backgroundNormal = controller.getDefaultBackground();
            }
            if (element == null) {
                return backgroundNormal;
            }

            return null;
        }

        @NotNull
        @Override
        public Color getHeaderForeground(@Nullable IGridItem item, boolean selected) {
            return cellHeaderForeground;
        }

        @NotNull
        @Override
        public Color getHeaderBackground(@Nullable IGridItem item, boolean selected) {
            return selected ? cellHeaderSelectionBackground : cellHeaderBackground;
        }

        @NotNull
        @Override
        public Color getHeaderBorder(@Nullable IGridItem item) {
            return cellHeaderBorder;
        }

        @NotNull
        @Override
        public String getText(@NotNull IGridItem item) {
            if (item instanceof IGridColumn && controller.isRecordMode()) {
                final ResultSetRow rsr = (ResultSetRow) item.getElement();
                return ResultSetMessages.controls_resultset_viewer_status_row + " #" + (rsr.getVisualNumber() + 1);
            }

            if (item instanceof IGridRow row && !controller.isRecordMode()) {
                return row.toString();
            }

            if (item.getElement() instanceof DBDAttributeBinding binding) {
                return getAttributeText(binding);
            } else if (item.getElement() instanceof DBSAttributeBase attr) {
                return attr.getName();
            } else if (item instanceof IGridRow row) {
                return String.valueOf(row.getRelativeIndex() + 1);
            } else {
                return String.valueOf(item.getElement());
            }
        }

        @NotNull
        private String getAttributeText(DBDAttributeBinding binding) {
            if (CommonUtils.isEmpty(binding.getLabel())) {
                return binding.getName();
            } else {
                return binding.getLabel();
            }
        }

        @Nullable
        @Override
        public String getDescription(IGridItem element) {
            if (!showAttributeDescription || element.getParent() != null) {
                return null;
            }
            if (element.getElement() instanceof DBDAttributeBinding attributeBinding) {
                return attributeBinding.getDescription();
            } else {
                return null;
            }
        }

        @Nullable
        @Override
        public Font getFont(IGridItem element) {
            if (element.getElement() instanceof DBDAttributeBinding attributeBinding) {
                DBDAttributeConstraint constraint = controller.getModel().getDataFilter().getConstraint(attributeBinding);
                if (constraint != null && constraint.hasCondition()) {
                    return spreadsheet.getFont(UIElementFontStyle.BOLD);
                }
                if (attributeBinding.isTransformed()) {
                    return spreadsheet.getFont(UIElementFontStyle.ITALIC);
                }
            }
            return null;
        }

        @Nullable
        @Override
        public String getToolTipText(IGridItem element) {
            if (element.getElement() instanceof DBDAttributeBinding attributeBinding) {
                final String name = attributeBinding.getName();
                final String typeName = attributeBinding.getFullTypeName();
                final String description = attributeBinding.getDescription();
                StringBuilder tip = new StringBuilder();
                tip.append("Column: ");
                tip.append(name).append(": ").append(typeName);
                if (!CommonUtils.isEmpty(description)) {
                    tip.append("\nDescription: ").append(description);
                }
                DBDRowIdentifier rowIdentifier = attributeBinding.getRowIdentifier();
                if (rowIdentifier != null) {
                    tip.append("\nTable: ").append(DBUtils.getObjectFullName(rowIdentifier.getEntity(), DBPEvaluationContext.UI));
                }
                if (rowIdentifier != null &&
                    !rowIdentifier.isIncomplete() &&
                    rowIdentifier != getController().getModel().getDefaultRowIdentifier()
                ) {
                    tip.append("\n").append(ResultSetMessages.controls_resultset_results_edit_key).append(": ")
                        .append(rowIdentifier.getEntity().getName())
                        .append("(")
                        .append(rowIdentifier.getAttributes().stream().map(DBDAttributeBinding::getName).collect(Collectors.joining(",")))
                        .append(")");
                }
                String readOnlyStatus = controller.getAttributeReadOnlyStatus(attributeBinding, true, true);
                if (readOnlyStatus != null) {
                    tip.append("\n").append(ResultSetMessages.controls_resultset_results_read_only_status)
                        .append(": ").append(readOnlyStatus);
                }
                return tip.toString();
            }
            return null;
        }
    }

    /////////////////////////////
    // Value controller

    public class SpreadsheetValueController extends ResultSetValueController implements IMultiController {

        SpreadsheetValueController(
            @NotNull IResultSetController controller,
            @NotNull ResultSetCellLocation cellLocation,
            @NotNull EditType editType,
            @Nullable Composite inlinePlaceholder)
        {
            super(controller, cellLocation, editType, inlinePlaceholder);
        }

        @Override
        public Object getValue() {
            return controller.getModel().getCellValue(cellLocation);
        }

        @Override
        public void closeInlineEditor() {
            spreadsheet.cancelInlineEditor();
        }

        @Override
        public void nextInlineEditor(boolean next) {
            spreadsheet.cancelInlineEditor();
            int colOffset = next ? 1 : -1;
            int rowOffset = 0;
            //final int rowCount = spreadsheet.getItemCount();
            final int colCount = spreadsheet.getColumnCount();
            final GridPos curPosition = spreadsheet.getCursorPosition();
            if (colOffset > 0 && curPosition.col + colOffset >= colCount) {
                colOffset = -colCount;
                rowOffset = 1;
            } else if (colOffset < 0 && curPosition.col + colOffset < 0) {
                colOffset = colCount;
                rowOffset = -1;
            }
            spreadsheet.shiftCursor(colOffset, rowOffset, false);
            openValueEditor(true);
        }

        @Override
        public void updateValue(@Nullable Object value, boolean updatePresentation) {
            super.updateValue(value, updatePresentation);
            if (updatePresentation) {
                spreadsheet.redrawGrid();
            }
        }

        @Override
        public void updateSelectionValue(Object value) {
            DBDAttributeBinding origAttr = getBinding();
            ResultSetRow origRow = getCurRow();
            int[] origRowIndexes = cellLocation.getRowIndexes();
            try {
                Collection<GridPos> ssSelection = spreadsheet.getSelection();
                for (GridPos pos : ssSelection) {
                    GridColumn gridColumn = spreadsheet.getColumn(pos.col);
                    IGridRow gridRow = spreadsheet.getRow(pos.row);
                    DBDAttributeBinding attr = getAttributeFromGrid(gridColumn, gridRow);
                    ResultSetRow row =  getResultRowFromGrid(gridColumn, gridRow);
                    if (attr == null || row == null) {
                        continue;
                    }
                    if (!ArrayUtils.isEmpty(origRowIndexes)) {
                        attr = ResultSetCellLocation.getLeafAttribute(attr, origRowIndexes);
                    }
                    if (attr.getValueHandler() != origAttr.getValueHandler()) {
                        continue;
                    }
                    if (controller.getAttributeReadOnlyStatus(attr, true, false) != null) {
                        // No inline editors for readonly columns
                        continue;
                    }
                    setBinding(attr);
                    setCurRow(row, spreadsheet.getPresentation().getCurrentRowIndexes());
                    updateValue(value, false);
                }
                spreadsheet.redrawGrid();
                controller.updatePanelsContent(false);
            } finally {
                setBinding(origAttr);
                setCurRow(origRow, origRowIndexes);
            }
        }

        void registerEditor(IValueEditorStandalone editor) {
            openEditors.put(this, editor);
        }

        void unregisterEditor(IValueEditorStandalone editor) {
            openEditors.remove(this);
        }

    }
}
