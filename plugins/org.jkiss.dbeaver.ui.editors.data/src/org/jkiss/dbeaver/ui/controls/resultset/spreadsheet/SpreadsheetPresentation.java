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

package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.controls.bool.BooleanMode;
import org.jkiss.dbeaver.ui.controls.bool.BooleanStyleSet;
import org.jkiss.dbeaver.ui.controls.lightgrid.*;
import org.jkiss.dbeaver.ui.controls.resultset.*;
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
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.properties.PropertySourceDelegate;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
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
    implements IResultSetEditor, IResultSetDisplayFormatProvider, ISelectionProvider, IStatefulControl, IAdaptable, IGridController {
    public static final String PRESENTATION_ID = "spreadsheet";

    public static final String ATTR_OPTION_PINNED = "pinned";

    private static final int MAX_INLINE_COLLECTION_ELEMENTS = 3;

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
    private boolean showCollectionsInline;
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
            if (newRow != null && newRow.getElement() instanceof ResultSetRow) {
                curRow = (ResultSetRow) newRow.getElement();
                controller.setCurrentRow(curRow);
            }
            if (newCol != null && newCol.getElement() instanceof DBDAttributeBinding) {
                curAttribute = (DBDAttributeBinding) newCol.getElement();
            }
        } else {
            changed = newRow != null && curAttribute != newRow.getElement();
            if (newRow != null && newRow.getElement() instanceof DBDAttributeBinding) {
                curAttribute = (DBDAttributeBinding) newRow.getElement();
            }
            if (newCol != null &&
                newCol.getElement() instanceof ResultSetRow &&
                curRow != newCol.getElement())
            {
                curRow = (ResultSetRow) newCol.getElement();
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
                if (tdt.length() > 0) {
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
                if (value instanceof byte[]) {
                    binaryData = (byte[]) value;
                } else if (value instanceof DBDContent && !ContentUtils.isTextContent((DBDContent) value) && value instanceof DBDContentCached) {
                    try {
                        binaryData = ContentUtils.getContentBinaryValue(new VoidProgressMonitor(), (DBDContent) value);
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
                GridPos focusPos = spreadsheet.getFocusPos();
                int rowNum = focusPos.row;
                if (rowNum < 0) {
                    return;
                }
                //boolean overNewRow = controller.getModel().getRow(rowNum).getState() == ResultSetRow.STATE_ADDED;
                try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), controller.getDataContainer(), "Advanced paste")) {

                    String[][] newLines = parseGridLines(strValue, settings.isInsertMultipleRows());

                    // FIXME: do not create rows twice! Probably need to delete comment after testing. #9095
                    /*if (overNewRow) {
                        for (int i = 0 ; i < newLines.length - 1; i++) {
                            controller.addNewRow(false, true, false);
                        }
                        spreadsheet.refreshRowsData();
                    } else {*/
                    while (rowNum + newLines.length > spreadsheet.getItemCount()) {
                        controller.addNewRow(false, true, false);
                        spreadsheet.refreshRowsData();
                    }
                    //}
                    if (rowNum < 0 || rowNum >= spreadsheet.getItemCount()) {
                        return;
                    }

                    for (String[] line : newLines) {
                        int colNum = focusPos.col;
                        IGridRow gridRow = spreadsheet.getRow(rowNum);
                        for (String value : line) {
                            if (colNum >= spreadsheet.getColumnCount()) {
                                break;
                            }
                            IGridColumn colElement = spreadsheet.getColumn(colNum);
                            final DBDAttributeBinding attr = getAttributeFromGrid(colElement, gridRow);
                            final ResultSetRow row = getResultRowFromGrid(colElement, gridRow);
                            if (attr == null || row == null || controller.getAttributeReadOnlyStatus(attr) != null) {
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
                        }
                        rowNum++;
                        if (rowNum >= spreadsheet.getItemCount()) {
                            // Shouldn't be here
                            break;
                        }
                    }
                }

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
                    if (controller.getAttributeReadOnlyStatus(attr) != null) {
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

    private String[][] parseGridLines(String strValue, boolean splitRows) {
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
                        if (inQuote) {
                            if (i == length - 1 ||
                                strValue.charAt(i + 1) == columnDelimiter ||
                                strValue.charAt(i + 1) == trashDelimiter ||
                                strValue.charAt(i + 1) == rowDelimiter) {
                                inQuote = false;
                                continue;
                            }
                        } else if (cellValue.length() == 0) {
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
        if (cellValue.length() > 0) {
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
        showCollectionsInline = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_COLLECTIONS_INLINE);
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
                        .allMatch(x -> x != null && x.hasOption(ATTR_OPTION_PINNED));
                    final boolean allUnpinned = selectedColumns.stream()
                        .map(x -> dataFilter.getConstraint(((DBDAttributeBinding) x.getElement()).getTopParent()))
                        .allMatch(x -> x != null && !x.hasOption(ATTR_OPTION_PINNED));

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
                                            constraint.setOption(ATTR_OPTION_PINNED, getNextPinIndex(dataFilter));
                                        } else {
                                            constraint.removeOption(ATTR_OPTION_PINNED);
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
                    List<DBDAttributeBinding> hiddenAttributes = new ArrayList<>();
                    List<DBDAttributeConstraint> constraints = getController().getModel().getDataFilter().getConstraints();
                    for (DBDAttributeConstraint ac : constraints) {
                        DBSAttributeBase attribute = ac.getAttribute();
                        if (!ac.isVisible() && attribute instanceof DBDAttributeBinding && DBDAttributeConstraint.isVisibleByDefault((DBDAttributeBinding) attribute)) {
                            hiddenAttributes.add((DBDAttributeBinding) attribute);
                        }
                    }
                    if (!hiddenAttributes.isEmpty()) {
                        manager.insertAfter(IResultSetController.MENU_GROUP_ADDITIONS, new Action(ResultSetMessages.controls_resultset_viewer_show_hidden_columns) {
                            @Override
                            public void run() {
                                ResultSetModel model = controller.getModel();
                                for (DBDAttributeBinding attr : hiddenAttributes) {
                                    model.setAttributeVisibility(attr, true);
                                }
                                refreshData(true, false, true);
                            }
                        });
                    }

                    String hideTitle;
                    if (selectedColumns.size() == 1) {
                        DBDAttributeBinding columnToHide = (DBDAttributeBinding) selectedColumns.get(0).getElement();
                        hideTitle = NLS.bind(ResultSetMessages.controls_resultset_viewer_hide_column_x, columnToHide.getName());
                    } else {
                        hideTitle = NLS.bind(ResultSetMessages.controls_resultset_viewer_hide_columns_x, selectedColumns.size());
                    }

                    manager.insertAfter(IResultSetController.MENU_GROUP_ADDITIONS, new Action(hideTitle) {
                        @Override
                        public void run() {
                            ResultSetModel model = controller.getModel();
                            if (selectedColumns.size() >= model.getVisibleAttributeCount()) {
                                UIUtils.showMessageBox(
                                    getControl().getShell(),
                                    ResultSetMessages.controls_resultset_viewer_hide_columns_error_title,
                                    ResultSetMessages.controls_resultset_viewer_hide_columnss_error_text, SWT.ERROR);
                            } else {
                                for (IGridColumn selectedColumn : selectedColumns) {
                                    model.setAttributeVisibility((DBDAttributeBinding) selectedColumn.getElement(), false);
                                }
                                refreshData(true, false, true);
                            }
                        }
                    });
                }
            }
        }

        if (recordMode && row != null) {
            // Record mode
            List<Integer> selectedRowIndexes = new ArrayList<>();
            for (IGridColumn sRow : spreadsheet.getColumnSelection()) {
                if (sRow.getElement() instanceof ResultSetRow) {
                    selectedRowIndexes.add(((ResultSetRow) sRow.getElement()).getVisualNumber());
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
            Integer pinIndex = ac.getOption(ATTR_OPTION_PINNED);
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
        DBDAttributeBinding attr = getFocusAttribute();
        ResultSetRow row = getFocusRow();
        if (attr == null || row == null) {
            return null;
        }

        ResultSetCellLocation cellLocation = new ResultSetCellLocation(attr, row, getRowNestedIndexes(spreadsheet.getFocusRow()));
        Object cellValue = getController().getModel().getCellValue(cellLocation);
        if (cellValue instanceof DBDValueError) {
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
            String readOnlyStatus = controller.getAttributeReadOnlyStatus(attr);
            if (readOnlyStatus != null) {
                controller.setStatus("Column " + DBUtils.getObjectFullName(attr, DBPEvaluationContext.UI) + " is read-only: " + readOnlyStatus, DBPMessageType.ERROR);
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
        if (activeInlineEditor instanceof IValueEditorStandalone) {
            valueController.registerEditor((IValueEditorStandalone) activeInlineEditor);
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

    public void resetCellValue(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement, boolean delete) {
        final DBDAttributeBinding attr = getAttributeFromGrid (colElement, rowElement);
        final ResultSetRow row = getResultRowFromGrid(colElement, rowElement);
        controller.getModel().resetCellValue(
            new ResultSetCellLocation(attr, row, getRowNestedIndexes(rowElement)));
        updateValueView();
        controller.updatePanelsContent(false);
    }

    ///////////////////////////////////////////////
    // Links

    public void navigateLink(@NotNull GridCell cell, final int state) {
        final DBDAttributeBinding attr = getAttributeFromGrid(cell.col, cell.row);
        final ResultSetRow row = getResultRowFromGrid(cell.col, cell.row);

        Object value = controller.getModel().getCellValue(
            new ResultSetCellLocation(attr, row, getRowNestedIndexes(cell.row)));
        if (isShowAsCheckbox(attr)) {
            if (!getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_CLICK_TOGGLE_BOOLEAN)) {
                return;
            }
            if (!DBExecUtils.isAttributeReadOnly(attr)) {
                // Switch boolean value
                toggleBooleanValue(
                    new ResultSetCellLocation(attr, row, getRowNestedIndexes(cell.row)),
                    value);
            }
        } else if (isAttributeExpandable(attr)) {
            spreadsheet.toggleCellValue(cell.col, cell.row);
        } else if (DBUtils.isNullValue(value)) {
            UIUtils.showMessageBox(getSpreadsheet().getShell(), "Wrong link", "Can't navigate to NULL value", SWT.ICON_ERROR);
        } else if (!CommonUtils.isEmpty(attr.getReferrers())) {
            // Navigate association
            new AbstractJob("Navigate association") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        boolean newWindow;
                        if (RuntimeUtils.isMacOS()) {
                            newWindow = (state & SWT.COMMAND) == SWT.COMMAND;
                        } else {
                            newWindow = (state & SWT.CTRL) == SWT.CTRL;
                        }
                        controller.navigateAssociation(
                            monitor,
                            controller.getModel(),
                            DBExecUtils.getAssociationByAttribute(attr),
                            Collections.singletonList(row),
                            newWindow
                        );
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        } else {
            // Navigate hyperlink
            String strValue = attr.getValueHandler().getValueDisplayString(attr, value, DBDDisplayFormat.UI);
            ShellUtils.launchProgram(strValue);
        }
    }

    public void toggleCellValue(IGridColumn columnElement, IGridRow rowElement) {
        final DBDAttributeBinding attr = getAttributeFromGrid(columnElement, rowElement);
        final ResultSetRow row = getResultRowFromGrid(columnElement, rowElement);
        final ResultSetCellLocation cellLocation = new ResultSetCellLocation(attr, row, getRowNestedIndexes(rowElement));

        if (isShowAsCheckbox(attr)) {
            // Switch boolean value
            Object cellValue = controller.getModel().getCellValue(cellLocation);
            toggleBooleanValue(cellLocation, cellValue);
        } if (isAttributeExpandable(attr) && rowElement.getParent() == null) {
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
        if (!(columnElement instanceof DBDAttributeBinding)) {
            log.debug("Unable to show distinct filter for columnElement" + columnElement);
            return;
        }
        DBDAttributeBinding attributeBinding = (DBDAttributeBinding) columnElement;
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
                if (object instanceof GridCell) {
                    GridCell cell = (GridCell) object;
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
        return controller.isRecordMode() ?
            (DBDAttributeBinding) spreadsheet.getFocusRowElement() :
            (DBDAttributeBinding) spreadsheet.getFocusColumnElement();
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
            for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                Object cell = iter.next();
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

    @Override
    public void moveColumn(Object dragColumn, Object dropColumn, DropLocation location) {
        if (dragColumn instanceof DBDAttributeBinding && dropColumn instanceof DBDAttributeBinding) {
            final DBDDataFilter dataFilter = new DBDDataFilter(controller.getModel().getDataFilter());
            final DBDAttributeConstraint dragC = dataFilter.getConstraint((DBDAttributeBinding) dragColumn);
            final DBDAttributeConstraint dropC = dataFilter.getConstraint((DBDAttributeBinding) dropColumn);
            if (dragC == null || dropC == null) {
                return;
            }
            final boolean pin = dragC.hasOption(ATTR_OPTION_PINNED) && dropC.hasOption(ATTR_OPTION_PINNED);
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
            return constraint.getOption(ATTR_OPTION_PINNED);
        } else {
            return constraint.getVisualPosition();
        }
    }

    private static void setConstraintPosition(@NotNull DBDAttributeConstraint constraint, boolean pin, int position) {
        if (pin) {
            constraint.setOption(ATTR_OPTION_PINNED, position);
        } else {
            constraint.setVisualPosition(position);
        }
    }

    @NotNull
    private static List<DBDAttributeConstraint> getOrderedConstraints(@NotNull DBDDataFilter filter, boolean pin) {
        final List<DBDAttributeConstraint> constraints = filter.getConstraints();
        if (pin) {
            return constraints.stream()
                .filter(x -> x.hasOption(ATTR_OPTION_PINNED))
                .sorted(Comparator.comparing(x -> x.getOption(ATTR_OPTION_PINNED)))
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
            return rowObject == null ? null : (DBDAttributeBinding) rowObject.getElement();
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

    private boolean isAttributeExpandable(@NotNull DBDAttributeBinding attr) {
        return getExpandableAttribute(attr) != null;
    }

    @Nullable
    private DBDAttributeBinding getExpandableAttribute(@NotNull DBDAttributeBinding attr) {
        if (attr.getDataKind() == DBPDataKind.STRUCT && controller.isRecordMode()) {
            return attr;
        }

        for (DBDAttributeBinding cur = attr; cur != null; cur = cur.getParentObject()) {
            final DBPDataKind kind = cur.getDataKind();
            if (kind == DBPDataKind.ARRAY) {
                return cur;
            }
        }

        return null;
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

        @NotNull
        public List<IGridRow> getSelectedGridRows() {
            return spreadsheet.getRowSelection()
                .stream().map(i -> spreadsheet.getRow(i)).collect(Collectors.toList());
        }

        @Override
        public DBDAttributeBinding getElementAttribute(Object element) {
            GridPos pos = (GridPos) element;
            return (DBDAttributeBinding) (controller.isRecordMode() ?
                spreadsheet.getRowElement(pos.row) :
                spreadsheet.getColumnElement(pos.col));
        }

        @Override
        public ResultSetRow getElementRow(Object element) {
            return (ResultSetRow) (controller.isRecordMode() ?
                controller.getCurrentRow() :
                spreadsheet.getRowElement(((GridPos) element).row));
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
            if (item.getElement() instanceof DBDAttributeBinding) {
                final DBDAttributeBinding attr = (DBDAttributeBinding) item.getElement();
                switch (attr.getDataKind()) {
                    case DOCUMENT:
                    case ANY:
                        return !controller.isRecordMode();
                    default:
                        return isAttributeExpandable(attr);
                }
            }
            return false;
        }

        @Nullable
        @Override
        public Object[] getChildren(@NotNull IGridItem item) {
            if (item.getElement() instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) item.getElement();
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
            }

            return null;
        }

        @Override
        public int getCollectionSize(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement) {
            if (rowElement.getParent() != null) {
                // FIXME: implemented deep nested collections support
                return 0;
            }

            final DBDAttributeBinding attr = getExpandableAttribute(getAttributeFromGrid(colElement, rowElement));
            final ResultSetRow row = getResultRowFromGrid(colElement, rowElement);

            final ResultSetCellLocation cellLocation = new ResultSetCellLocation(attr, row, getRowNestedIndexes(rowElement));
            final Object cellValue = controller.getModel().getCellValue(cellLocation);

            if (cellValue instanceof List<?>) {
                return ((List<?>) cellValue).size();
            } else if (cellValue instanceof DBDComposite) {
                return ((DBDComposite) cellValue).getAttributeCount();
            } else {
                return 0;
            }
        }

        @Override
        public int getSortOrder(@Nullable IGridColumn column) {
            if (showAttrOrdering) {
                if (column != null && column.getElement() instanceof DBDAttributeBinding) {
                    DBDAttributeBinding binding = (DBDAttributeBinding) column.getElement();
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
            }
            return SWT.NONE;
        }

        @Override
        public ElementState getDefaultState(@NotNull IGridColumn element) {
            if (element.getElement() instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) element.getElement();
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
        public boolean isVoidCell(IGridColumn gridColumn, IGridRow gridRow) {
            return getCellValue(gridColumn, gridRow, false) == DBDVoid.INSTANCE;
        }

        @Override
        public int getColumnPinIndex(@NotNull IGridColumn element) {
            if (!controller.isRecordMode()) {
                DBDAttributeBinding attr = (DBDAttributeBinding) element.getElement();
                DBDAttributeConstraint ac = controller.getModel().getDataFilter().getConstraint(attr);
                if (ac != null) {
                    Integer pinIndex = ac.getOption(ATTR_OPTION_PINNED);
                    return pinIndex == null ? -1 : pinIndex;
                }
            }
            return -1;
        }

        @Override
        public boolean isElementSupportsFilter(IGridColumn element) {
            if (element.getElement() instanceof DBDAttributeBinding) {
                return supportsAttributeFilter;
            }
            return false;
        }

        @Override
        public boolean isElementSupportsSort(@Nullable IGridColumn element) {
            if (element.getElement() instanceof DBDAttributeBinding) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isElementReadOnly(IGridColumn element) {
            if (element.getElement() instanceof DBDAttributeBinding) {
                return controller.getAttributeReadOnlyStatus((DBDAttributeBinding) element.getElement()) != null;
            }
            return false;
        }

        @Override
        public boolean isElementExpandable(@NotNull IGridItem item) {
            if (item instanceof IGridRow && !controller.isRecordMode()) {
                return hasExpandableElements();
            } else {
                return item.getElement() instanceof DBDAttributeBinding
                    && isAttributeExpandable((DBDAttributeBinding) item.getElement());
            }
        }

        private boolean hasExpandableElements() {
            if (controller.isRecordMode()) {
                for (int i = 0; i < spreadsheet.getItemCount(); i++) {
                    if (isElementExpandable(spreadsheet.getRow(i))) {
                        return true;
                    }
                }
            } else {
                for (int i = 0; i < spreadsheet.getColumnCount(); i++) {
                    if (isElementExpandable(spreadsheet.getColumn(i))) {
                        return true;
                    }
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
            CellInformation info = new CellInformation();

            DBDAttributeBinding attr = getAttributeFromGrid(colElement, rowElement);
            ResultSetRow row = getResultRowFromGrid(colElement, rowElement);
            Object cellValue = row == null || attr == null ? null : getCellValue(colElement, rowElement, false);

            info.value = cellValue;
            info.text = formatValue(attr, row, info.value);

            info.state = STATE_NONE;
            if (attr != null && cellValue != DBDVoid.INSTANCE) {
                // State
                if ((controller.getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_LINKS) != 0) {
                    //ResultSetRow row = (ResultSetRow) (recordMode ? colElement.getElement() : rowElement.getElement());
                    if (isShowAsCheckbox(attr)) {
                        info.state |= booleanStyles.getMode() == BooleanMode.TEXT ? STATE_TOGGLE : STATE_LINK;
                    } else if (!CommonUtils.isEmpty(attr.getReferrers()) || isShowAsExpander(rowElement, attr)) {
                        if (!DBUtils.isNullValue(cellValue)) {
                            info.state |= STATE_LINK;
                        }
                    } else {
                        final String strValue = info.text != null
                            ? info.text.toString()
                            : attr.getValueHandler().getValueDisplayString(attr, cellValue, DBDDisplayFormat.UI);
                        if (strValue != null && strValue.contains("://")) {
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

            info.align = getCellAlign(attr, row, cellValue);

            {
                // Image
                if (booleanStyles.getMode() != BooleanMode.TEXT) {
                    if (isShowAsCheckbox(attr)) {
                        if (cellValue instanceof Number) {
                            cellValue = ((Number) cellValue).byteValue() != 0;
                        }
                        if (DBUtils.isNullValue(cellValue) || cellValue instanceof Boolean) {
                            info.image = booleanStyles.getStyle((Boolean) cellValue).getIcon();
                        }
                    }
                }
                // Collections
                if (info.image == null && isShowAsExpander(rowElement, attr) && !DBUtils.isNullValue(cellValue)) {
                    final GridCell cell = new GridCell(colElement, rowElement);
                    info.image = spreadsheet.isCellExpanded(cell) ? UIIcon.TREE_COLLAPSE : UIIcon.TREE_EXPAND;
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
                }
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
            if (gridRow.getParent() != null && !spreadsheet.isCellExpanded(new GridCell(gridColumn, gridRow.getParent()))) {
                // FIXME: Hack for hiding non-expanded column elements. Will break once nested collection support is added.
                return DBDVoid.INSTANCE;
            }
            DBDAttributeBinding attr = getAttributeFromGrid(gridColumn, gridRow);
            ResultSetRow row = getResultRowFromGrid(gridColumn, gridRow);
            if (attr == null || row == null) {
                return null;
            }
            int[] rowNestedIndexes = getRowNestedIndexes(gridRow);
            return getCellValue(attr, row, rowNestedIndexes, formatString);
        }

        public Object getCellValue(DBDAttributeBinding attr, ResultSetRow row, int[] rowIndexes, boolean formatString) {
            Object value = controller.getModel().getCellValue(attr, row, rowIndexes);

            if (formatString) {
                return formatValue(attr, row, value);
            } else {
                return value;
            }
        }

        @Nullable
        private Object formatValue(DBDAttributeBinding attr, ResultSetRow row, Object value) {
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

            if (isShowAsCheckbox(attr)) {
                if (booleanStyles.getMode() != BooleanMode.TEXT) {
                    return "";
                }
                if (value instanceof Number) {
                    value = ((Number) value).byteValue() != 0;
                }
                if (booleanStyles.getMode() == BooleanMode.TEXT && (DBUtils.isNullValue(value) || value instanceof Boolean)) {
                    return booleanStyles.getStyle((Boolean) value).getText();
                }
                return value;
            }

            if (!showCollectionsInline || controller.isRecordMode()) {
                if (isAttributeExpandable(attr) && value instanceof DBDCollection && !DBUtils.isNullValue(value)) {
                    final DBDCollection collection = (DBDCollection) value;
                    final StringJoiner buffer = new StringJoiner(",", "{", "}");
                    for (int i = 0; i < Math.min(collection.size(), MAX_INLINE_COLLECTION_ELEMENTS); i++) {
                        buffer.add(collection.getComponentValueHandler().getValueDisplayString(
                            collection.getComponentType(),
                            collection.get(i),
                            DBDDisplayFormat.UI
                        ));
                    }
                    if (collection.size() > MAX_INLINE_COLLECTION_ELEMENTS) {
                        buffer.add(" ... [" + collection.getItemCount() + "]");
                    }
                    return buffer.toString();
                } else if (attr.getDataKind() == DBPDataKind.STRUCT && value instanceof DBDComposite && !DBUtils.isNullValue(value)) {
                    return "[" + ((DBDComposite) value).getDataType().getName() + "]";
                }
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
                    if (isShowAsCheckbox(attr)) {
                        if (row.getState() == ResultSetRow.STATE_ADDED) {
                            return ALIGN_CENTER;
                        }
                        if (row.isChanged(attr)) {
                            // Use alignment of an original value to prevent unexpected jumping back and forth.
                            cellValue = row.getOriginalValue(attr);
                        }
                        if (cellValue instanceof Number) {
                            cellValue = ((Number) cellValue).byteValue() != 0;
                        }
                        if (DBUtils.isNullValue(cellValue) || cellValue instanceof Boolean) {
                            switch (booleanStyles.getStyle((Boolean) cellValue).getAlignment()) {
                                case LEFT:
                                    return ALIGN_LEFT;
                                case CENTER:
                                    return ALIGN_CENTER;
                                case RIGHT:
                                    return ALIGN_RIGHT;
                            }
                        }
                    }
                    DBPDataKind dataKind = attr.getDataKind();
                    if ((dataKind == DBPDataKind.NUMERIC && rightJustifyNumbers) ||
                        (dataKind == DBPDataKind.DATETIME && rightJustifyDateTime)) {
                        return ALIGN_RIGHT;
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
                if (cellValue instanceof Number) {
                    cellValue = ((Number) cellValue).byteValue() != 0;
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
                if (normalColor == null || normalColor == backgroundNormal) {
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
                if (normalColor == null || normalColor == backgroundNormal) {
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

            if (!controller.isRecordMode() && showOddRows) {
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
            List<DBSEntityReferrer> referrers = attr.getReferrers();
            if (!CommonUtils.isEmpty(referrers) && !DBUtils.isNullValue(value)) {
                StringBuilder text = new StringBuilder();
                for (DBSEntityReferrer ref : referrers) {
                    if (ref instanceof DBSEntityAssociation) {
                        DBSEntity associatedEntity = ResultSetUtils.getAssociatedEntity(ref);
                        if (associatedEntity != null) {
                            if (text.length() > 0) text.append("\n");
                            text.append(DBUtils.getObjectFullName(associatedEntity, DBPEvaluationContext.UI));
                        }
                    }
                }
                return text.toString();
            }
            return "";
        }

        @Override
        public void resetColors() {
            backgroundNormal = null;
            foregroundDefault = null;
        }
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
    static int[] getRowNestedIndexes(IGridRow gridRow) {
        int[] nestedIndexes = null;
        if (gridRow != null && gridRow.getParent() != null) {
            nestedIndexes = new int[gridRow.getRowDepth()];
            for (IGridRow gr = gridRow; gr.getParent() != null; gr = gr.getParent()) {
                nestedIndexes[gr.getRowDepth() - 1] = gr.getRelativeIndex();
            }
        }
        return nestedIndexes;
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

    private boolean isShowAsExpander(@NotNull IGridRow rowElement, @NotNull DBDAttributeBinding attr) {
        return rowElement.getParent() == null && spreadsheet.getColumnCount() > 1 && isAttributeExpandable(attr);
    }

    private class GridLabelProvider implements IGridLabelProvider {
        @Nullable
        @Override
        public Image getImage(IGridItem item) {
            if (!showAttributeIcons) {
                return null;
            }

            DBDAttributeBinding attr = null;

            if (item instanceof IGridRow && item.getParent() != null && controller.isRecordMode()) {
                final DBDAttributeBinding binding = (DBDAttributeBinding) item.getElement();
                if (binding.getDataKind() == DBPDataKind.STRUCT) {
                    final List<DBDAttributeBinding> bindings = binding.getNestedBindings();
                    final int index = ((IGridRow) item).getRelativeIndex();
                    if (bindings != null && bindings.size() > index) {
                        attr = bindings.get(index);
                    }
                }
            } else if (item.getElement() instanceof DBDAttributeBinding) {
                attr = ((DBDAttributeBinding) item.getElement());
            }

            if (attr != null) {
                DBPImage image = DBValueFormatting.getObjectImage(attr.getAttribute());

                if (isAttributeReadOnly(attr)) {
                    image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LOCK);
                }

                return DBeaverIcons.getImage(image);
            }

            return null;
        }

        private boolean isAttributeReadOnly(@NotNull DBDAttributeBinding attr) {
            return !controller.getModel().isUpdateInProgress()
                && CommonUtils.isBitSet(controller.getDecorator().getDecoratorFeatures(), IResultSetDecorator.FEATURE_EDIT)
                && controller.getAttributeReadOnlyStatus(attr) != null
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

            if (item instanceof IGridRow && item.getParent() != null && controller.isRecordMode()) {
                final DBDAttributeBinding binding = (DBDAttributeBinding) item.getElement();
                if (binding.getDataKind() == DBPDataKind.STRUCT) {
                    final List<DBDAttributeBinding> bindings = binding.getNestedBindings();
                    final int index = ((IGridRow) item).getRelativeIndex();
                    if (bindings != null && bindings.size() > index) {
                        return getAttributeText(bindings.get(index));
                    }
                }
            }

            if (item instanceof IGridRow && !(controller.isRecordMode() && item.getParent() == null)) {
                final IGridRow row = (IGridRow) item;
                final StringJoiner rowNumber = new StringJoiner(".");

                if (!controller.isRecordMode()) {
                    final ResultSetRow rsr = (ResultSetRow) row.getElement();
                    rowNumber.add(String.valueOf(rsr.getVisualNumber() + 1));
                }

                for (IGridRow r = row; r.getParent() != null; r = r.getParent()) {
                    rowNumber.add(String.valueOf(r.getRelativeIndex() + 1));
                }

                return rowNumber.toString();
            }

            return getAttributeText((DBDAttributeBinding) item.getElement());
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
            if (element.getElement() instanceof DBDAttributeBinding) {
                return ((DBDAttributeBinding) element.getElement()).getDescription();
            } else {
                return null;
            }
        }

        @Nullable
        @Override
        public Font getFont(IGridItem element) {
            if (element.getElement() instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element.getElement();
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
            if (element.getElement() instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element.getElement();
                final String name = attributeBinding.getName();
                final String typeName = attributeBinding.getFullTypeName();
                final String description = attributeBinding.getDescription();
                String tip = CommonUtils.isEmpty(description) ?
                    name + ": " + typeName :
                    name + ": " + typeName + "\n" + description;
                String readOnlyStatus = controller.getAttributeReadOnlyStatus(attributeBinding);
                if (readOnlyStatus != null) {
                    tip += " (Read-only: " + readOnlyStatus + ")";
                }
                return tip;
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
                    DBDAttributeBinding attr;
                    ResultSetRow row;
                    if (controller.isRecordMode()) {
                        attr = (DBDAttributeBinding) spreadsheet.getRowElement(pos.row);
                        row = controller.getCurrentRow();
                    } else {
                        attr = (DBDAttributeBinding) spreadsheet.getColumnElement(pos.col);
                        row = (ResultSetRow) spreadsheet.getRowElement(pos.row);
                    }
                    if (attr == null || row == null) {
                        continue;
                    }
                    if (!ArrayUtils.isEmpty(origRowIndexes)) {
                        attr = ResultSetCellLocation.getLeafAttribute(attr, origRowIndexes);
                    }
                    if (attr.getValueHandler() != origAttr.getValueHandler()) {
                        continue;
                    }
                    if (controller.getAttributeReadOnlyStatus(attr) != null) {
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
