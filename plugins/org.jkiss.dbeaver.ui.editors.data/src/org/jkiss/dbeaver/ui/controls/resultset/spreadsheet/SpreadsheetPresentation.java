/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.swt.dnd.TextTransfer;
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
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
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
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.properties.PropertySourceDelegate;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Spreadsheet presentation.
 * Visualizes results as grid.
 */
public class SpreadsheetPresentation extends AbstractPresentation implements IResultSetEditor, ISelectionProvider, IStatefulControl, IAdaptable, IGridController {

    public static final String PRESENTATION_ID = "spreadsheet";

    public static final String ATTR_OPTION_PINNED = "pinned";

    private static final Log log = Log.getLog(SpreadsheetPresentation.class);

    private Spreadsheet spreadsheet;

    @Nullable
    private DBDAttributeBinding curAttribute;
    private int columnOrder = SWT.DEFAULT;

    private final Map<SpreadsheetValueController, IValueEditorStandalone> openEditors = new HashMap<>();

    private SpreadsheetFindReplaceTarget findReplaceTarget;

    // UI modifiers
    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;
    private Color backgroundNormal;
    private Color backgroundOdd;
    private Color backgroundReadOnly;
    private Color foregroundDefault;
    private Color foregroundSelected, backgroundSelected;
    private Color backgroundMatched;
    private Color cellHeaderForeground, cellHeaderBackground, cellHeaderSelectionBackground;
    private Font italicFont;

    private boolean showOddRows = true;
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
    private int rowBatchSize;
    private IValueEditor activeInlineEditor;

    private int highlightScopeFirstLine;
    private int highlightScopeLastLine;
    private Color highlightScopeColor;

    public SpreadsheetPresentation() {
        findReplaceTarget = new SpreadsheetFindReplaceTarget(this);

    }

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    public boolean isShowOddRows() {
        return showOddRows;
    }

    public void setShowOddRows(boolean showOddRows) {
        this.showOddRows = showOddRows;
    }

    public boolean isAutoFetchSegments() {
        return autoFetchSegments;
    }

    public void setAutoFetchSegments(boolean autoFetchSegments) {
        this.autoFetchSegments = autoFetchSegments;
    }

    @Nullable
    DBPDataSource getDataSource() {
        DBSDataContainer dataContainer = controller.getDataContainer();
        return dataContainer == null ? null : dataContainer.getDataSource();
    }

    @Override
    public boolean isDirty() {
        boolean hasActiveEditor =
            activeInlineEditor != null &&
            activeInlineEditor.getControl() != null &&
            !activeInlineEditor.getControl().isDisposed() &&
            !getController().getModel().isAttributeReadOnly(getCurrentAttribute()) &&
            !(activeInlineEditor instanceof IValueEditorStandalone);
        return hasActiveEditor;
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

        this.italicFont = UIUtils.modifyFont(parent.getFont(), SWT.ITALIC);

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
            public void widgetSelected(SelectionEvent e)
            {
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
    public void dispose()
    {
        closeEditors();
        clearMetaData();

        UIUtils.dispose(this.italicFont);

        UIUtils.dispose(this.cellHeaderSelectionBackground);
        super.dispose();
    }

    public void scrollToRow(@NotNull RowPosition position)
    {
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
                    if (curRow != null) {
                        GridPos curPos = spreadsheet.getCursorPosition();
                        GridCell newCell = spreadsheet.posToCell(new GridPos(curPos.col, curRow.getVisualNumber()));
                        if (newCell != null) {
                            spreadsheet.setCursor(newCell, false, true);
                        }
                    }
                    break;
            }

            spreadsheet.getHorizontalScrollBarProxy().setSelection(hScrollPos);

            // Update controls
            controller.updateEditControls();
            controller.updateStatusMessage();
            controller.updatePanelsContent(true);

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
        if (curRow == null) {
            return;
        }
        GridCell cell = controller.isRecordMode() ?
            new GridCell(curRow, this.curAttribute) :
            new GridCell(this.curAttribute, curRow);
        this.spreadsheet.setCursor(cell, false, true);
        //this.spreadsheet.showColumn(this.curAttribute);
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

    private void updateGridCursor(GridCell cell)
    {
        boolean changed;
        Object newCol = cell == null ? null : cell.col;
        Object newRow = cell == null ? null : cell.row;
        ResultSetRow curRow = controller.getCurrentRow();
        if (!controller.isRecordMode()) {
            changed = curRow != newRow || curAttribute != newCol;
            if (newRow instanceof ResultSetRow) {
                curRow = (ResultSetRow) newRow;
                controller.setCurrentRow(curRow);
            }
            if (newCol instanceof DBDAttributeBinding) {
                curAttribute = (DBDAttributeBinding) newCol;
            }
        } else {
            changed = curAttribute != newRow;
            if (newRow instanceof DBDAttributeBinding) {
                curAttribute = (DBDAttributeBinding) newRow;
            }
        }
        if (changed) {
            spreadsheet.cancelInlineEditor();
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_MOVE);
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_EDITABLE);
            spreadsheet.redrawGrid();
        }
    }

    @Nullable
    public String copySelectionToString(ResultSetCopySettings settings)
    {
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
        List<Object> selectedColumns = spreadsheet.getColumnSelection();
        IGridLabelProvider labelProvider = spreadsheet.getLabelProvider();
        StringBuilder tdt = new StringBuilder();
        if (settings.isCopyHeader()) {
            if (settings.isCopyRowNumbers()) {
                tdt.append("#");
            }
            for (Object column : selectedColumns) {
                if (tdt.length() > 0) {
                    tdt.append(columnDelimiter);
                }
                tdt.append(labelProvider.getText(column));
            }
            tdt.append(rowDelimiter);
        }

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
                    }
                }
                if (prevCell != null) {
                    tdt.append(rowDelimiter);
                }
                if (settings.isCopyRowNumbers()) {
                    tdt.append(labelProvider.getText(cell.row)).append(columnDelimiter);
                }
            }
            if (prevCell != null && prevCell.col != cell.col) {
                int prevColIndex = selectedColumns.indexOf(prevCell.col);
                int curColIndex = selectedColumns.indexOf(cell.col);
                for (int i = prevColIndex; i < curColIndex; i++) {
                    tdt.append(columnDelimiter);
                }
            }

            boolean recordMode = controller.isRecordMode();
            DBDAttributeBinding column = (DBDAttributeBinding)(!recordMode ?  cell.col : cell.row);
            ResultSetRow row = (ResultSetRow) (!recordMode ?  cell.row : cell.col);
            Object value = controller.getModel().getCellValue(column, row);
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

            if (settings.isCut()) {
                IValueController valueController = new SpreadsheetValueController(
                    controller, column, row, IValueController.EditType.NONE, null);
                if (!valueController.isReadOnly()) {
                    valueController.updateValue(BaseValueManager.makeNullValue(valueController), false);
                }
            }

            prevCell = cell;
        }
        if (settings.isCut()) {
            controller.redrawData(false, false);
            controller.updatePanelsContent(false);
        }

        return tdt.toString();
    }

    @Override
    public void pasteFromClipboard(boolean extended)
    {
        try {
            if (extended) {
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
                boolean overNewRow = controller.getModel().getRow(rowNum).getState() == ResultSetRow.STATE_ADDED;
                try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), controller.getDataContainer(), "Advanced paste")) {

                    String[][] newLines = parseGridLines(strValue);
                    // Create new rows on demand
                    if (overNewRow) {
                        for (int i = 0 ; i < newLines.length - 1; i++) {
                            controller.addNewRow(false, true, false);
                        }
                        spreadsheet.refreshRowsData();
                    } else {
                        while (rowNum + newLines.length > spreadsheet.getItemCount()) {
                            controller.addNewRow(false, true, false);
                            spreadsheet.refreshRowsData();
                        }
                    }
                    if (rowNum < 0 || rowNum >= spreadsheet.getItemCount()) {
                        return;
                    }

                    for (String[] line : newLines) {
                        int colNum = focusPos.col;
                        Object rowElement = spreadsheet.getRowElement(rowNum);
                        for (String value : line) {
                            if (colNum >= spreadsheet.getColumnCount()) {
                                break;
                            }
                            Object colElement = spreadsheet.getColumnElement(colNum);
                            final DBDAttributeBinding attr = (DBDAttributeBinding)(controller.isRecordMode() ? rowElement : colElement);
                            final ResultSetRow row = (ResultSetRow)(controller.isRecordMode() ? colElement : rowElement);
                            if (controller.getAttributeReadOnlyStatus(attr) != null) {
                                continue;
                            }
                            Object newValue = attr.getValueHandler().getValueFromObject(
                                session, attr.getAttribute(), value, true, false);
                            new SpreadsheetValueController(
                                controller,
                                attr,
                                row,
                                IValueController.EditType.NONE,
                                null).updateValue(newValue, false);

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
                    if (controller.getAttributeReadOnlyStatus(attr) != null) {
                        // No inline editors for readonly columns
                        continue;
                    }

                    Object newValue = ResultSetUtils.getAttributeValueFromClipboard(attr);
                    if (newValue == null) {
                        continue;
                    }
                    new SpreadsheetValueController(
                        controller,
                        attr,
                        row,
                        IValueController.EditType.NONE,
                        null).updateValue(newValue, false);
                }
            }
            controller.redrawData(false, true);
            controller.updateEditControls();
            controller.updatePanelsContent(false);
        }
        catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Cannot replace cell value", null, e);
        }
    }

    private String[][] parseGridLines(String strValue) {
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
                        curLine.add(cellValue.toString());
                        cellValue.setLength(0);
                        break;
                    case rowDelimiter:
                        curLine.add(cellValue.toString());
                        lines.add(curLine.toArray(new String[0]));
                        curLine.clear();
                        cellValue.setLength(0);
                        break;
                    case trashDelimiter:
                        // Ignore
                        continue;
                    case quote:
                        if (inQuote) {
                            if (i == length - 1 ||
                                strValue.charAt(i + 1) == columnDelimiter ||
                                strValue.charAt(i + 1) == trashDelimiter ||
                                strValue.charAt(i + 1) == rowDelimiter)
                            {
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
                                    strValue.charAt(k + 1) == rowDelimiter))
                                {
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
        // Cache preferences
        DBPPreferenceStore preferenceStore = getPreferenceStore();
        showOddRows = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ODD_ROWS);
        //showCelIcons = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_CELL_ICONS);
        rightJustifyNumbers = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);
        rightJustifyDateTime = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_RIGHT_JUSTIFY_DATETIME);
        rowBatchSize = preferenceStore.getInt(ResultSetPreferences.RESULT_SET_ROW_BATCH_SIZE);

        showAttrOrdering = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ORDERING);
        showAttributeIcons = controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_ICONS);
        showAttributeDescription = getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_DESCRIPTION);
        supportsAttributeFilter =
            (controller.getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_FILTERS) != 0 &&
            (controller.getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_FILTER) != 0 &&
            controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ATTR_FILTERS);
        autoFetchSegments = controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT);
        calcColumnWidthByValue = getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES);
        showBooleanAsCheckbox = preferenceStore.getBoolean(ResultSetPreferences.RESULT_SET_SHOW_BOOLEAN_AS_CHECKBOX);

        spreadsheet.setColumnScrolling(!getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_USE_SMOOTH_SCROLLING));

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
            if (!recordMode) {
                spreadsheet.setCursor(new GridCell(oldAttribute, oldRow), false, true);
            } else {
                spreadsheet.setCursor(new GridCell(oldRow, oldAttribute), false, true);
            }
        }
        spreadsheet.layout(true, true);
    }

    void fillContextMenu(@NotNull IMenuManager manager, @Nullable Object colObject, @Nullable Object rowObject) {
        final DBDAttributeBinding attr = (DBDAttributeBinding)(controller.isRecordMode() ? rowObject : colObject);
        final ResultSetRow row = (ResultSetRow)(controller.isRecordMode() ? colObject : rowObject);
        controller.fillContextMenu(manager, attr, row);

        if (attr != null && row == null) {
            final List<Object> selectedColumns = spreadsheet.getColumnSelection();
            if (selectedColumns.size() == 1 && attr != null) {
                selectedColumns.clear();
                selectedColumns.add(attr);
            }
            if (!controller.isRecordMode() && !selectedColumns.isEmpty()) {
                manager.insertBefore(IResultSetController.MENU_GROUP_ADDITIONS, new Separator());
                {
                    // Pin/unpin
                    DBDDataFilter dataFilter = controller.getModel().getDataFilter();
                    DBDAttributeConstraint ac = dataFilter.getConstraint(attr.getTopParent());
                    if (ac != null) {
                        Integer pinnedIndex = ac.getOption(ATTR_OPTION_PINNED);
                        manager.insertBefore(IResultSetController.MENU_GROUP_ADDITIONS, new Action(pinnedIndex != null ? "Unpin column" : "Pin column") {
                            @Override
                            public void run() {
                                if (pinnedIndex != null) {
                                    ac.removeOption(ATTR_OPTION_PINNED);
                                } else {
                                    ac.setOption(ATTR_OPTION_PINNED, getMaxPinIndex(dataFilter) + 1);
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
                            hiddenAttributes.add((DBDAttributeBinding)attribute);
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
                        DBDAttributeBinding columnToHide = (DBDAttributeBinding) selectedColumns.get(0);
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
                                for (Object selectedColumn : selectedColumns) {
                                    model.setAttributeVisibility((DBDAttributeBinding) selectedColumn, false);
                                }
                                refreshData(true, false, true);
                            }
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
                        SpreadsheetCommandHandler.CMD_COLUMNS_FIT_VALUE));
                manager.insertAfter(
                    IResultSetController.MENU_GROUP_ADDITIONS,
                    ActionUtils.makeCommandContribution(
                        controller.getSite(),
                        SpreadsheetCommandHandler.CMD_COLUMNS_FIT_SCREEN));
            }
        }
    }

    private static int getMaxPinIndex(DBDDataFilter dataFilter) {
        int maxIndex = 0;
        for (DBDAttributeConstraint ac : dataFilter.getConstraints()) {
            Integer pinIndex = ac.getOption(ATTR_OPTION_PINNED);
            if (pinIndex != null) {
                maxIndex = Math.max(maxIndex, pinIndex);
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
    public Control openValueEditor(final boolean inline)
    {
        // The control that will be the editor must be a child of the Table
        DBDAttributeBinding attr = getFocusAttribute();
        ResultSetRow row = getFocusRow();
        if (attr == null || row == null) {
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
            if (isShowAsCheckbox(attr)) {
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
            attr,
            row,
            inline ? IValueController.EditType.INLINE : IValueController.EditType.EDITOR,
            placeholder);

        IValueController.EditType[] supportedEditTypes = valueController.getValueManager().getSupportedEditTypes();
        if (supportedEditTypes.length == 0) {
            if (placeholder != null) {
                placeholder.dispose();
            }
            return null;
        }

        try {
            activeInlineEditor = valueController.getValueManager().createEditor(valueController);
        }
        catch (Exception e) {
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

    public void resetCellValue(@NotNull Object colElement, @NotNull Object rowElement, boolean delete) {
        boolean recordMode = controller.isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
        final ResultSetRow row = (ResultSetRow)(recordMode ? colElement : rowElement);
        controller.getModel().resetCellValue(attr, row);
        updateValueView();
        controller.updatePanelsContent(false);
    }

    ///////////////////////////////////////////////
    // Links

    public void navigateLink(@NotNull GridCell cell, final int state) {
        boolean recordMode = controller.isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row : cell.col);
        final ResultSetRow row = (ResultSetRow)(recordMode ? cell.col : cell.row);

        Object value = controller.getModel().getCellValue(attr, row);
        if (isShowAsCheckbox(attr)) {
            // Switch boolean value
            toggleBooleanValue(attr, row, value);
        } else if (DBUtils.isNullValue(value)) {
            UIUtils.showMessageBox(getSpreadsheet().getShell(), "Wrong link", "Can't navigate to NULL value", SWT.ICON_ERROR);
        } else if (!CommonUtils.isEmpty(attr.getReferrers())) {
            // Navigate association
            new AbstractJob("Navigate association") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        boolean ctrlPressed = (state & SWT.CTRL) == SWT.CTRL;
                        controller.navigateAssociation(
                            monitor,
                            controller.getModel(),
                            DBExecUtils.getAssociationByAttribute(attr),
                            Collections.singletonList(row), ctrlPressed);
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        } else {
            // Navigate hyperlink
            String strValue = attr.getValueHandler().getValueDisplayString(attr, value, DBDDisplayFormat.UI);
            UIUtils.launchProgram(strValue);
        }
    }

    public void toggleCellValue(Object columnElement, Object rowElement) {
        boolean recordMode = controller.isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : columnElement);
        final ResultSetRow row = (ResultSetRow)(recordMode ? columnElement : rowElement);

        if (isShowAsCheckbox(attr)) {
            // Switch boolean value
            Object cellValue = controller.getModel().getCellValue(attr, row);
            toggleBooleanValue(attr, row, cellValue);
        }
    }

    private void toggleBooleanValue(DBDAttributeBinding attr, ResultSetRow row, Object value) {
        boolean nullable = !attr.isRequired();
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
            attr,
            row,
            IValueController.EditType.NONE,
            null);
        // Update value in all selected rows
        for (ResultSetRow selRow : getSelection().getSelectedRows()) {
            valueController.setCurRow(selRow);
            valueController.updateValue(value, true);
        }
    }

    ///////////////////////////////////////////////
    // Themes

    @Override
    protected void applyThemeSettings(ITheme currentTheme)
    {
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
        this.backgroundSelected = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK);
        this.backgroundMatched = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_MATCHED);

        this.cellHeaderForeground = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_HEADER_FOREGROUND);
        this.cellHeaderBackground = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_HEADER_BACKGROUND);
        {
            if (this.cellHeaderSelectionBackground != null) {
                UIUtils.dispose(this.cellHeaderSelectionBackground);
                this.cellHeaderSelectionBackground = null;
            }
            Color headerSelectionBackground = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_HEADER_SELECTED_BACKGROUND);
            RGB cellSel = UIUtils.blend(
                    headerSelectionBackground.getRGB(),
                    new RGB(255, 255, 255),
                    50);
            this.cellHeaderSelectionBackground = new Color(getSpreadsheet().getDisplay(), cellSel);
        }
        this.spreadsheet.setLineColor(colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_LINES_NORMAL));
        this.spreadsheet.setLineSelectedColor(colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_LINES_SELECTED));

        this.spreadsheet.recalculateSizes();
    }

    ///////////////////////////////////////////////
    // Ordering

    private boolean supportsDataFilter()
    {
        DBSDataContainer dataContainer = controller.getDataContainer();
        return dataContainer != null &&
            (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_FILTER) == DBSDataContainer.DATA_FILTER;
    }

    public void changeSorting(Object columnElement, final int state)
    {
        if (columnElement == null) {
            columnOrder = columnOrder == SWT.DEFAULT ? SWT.UP : (columnOrder == SWT.UP ? SWT.DOWN : SWT.DEFAULT);
            spreadsheet.refreshData(false, true, false);
            spreadsheet.redrawGrid();
            return;
        }
        boolean ctrlPressed = (state & SWT.CTRL) == SWT.CTRL;
        boolean altPressed = (state & SWT.ALT) == SWT.ALT;
        controller.toggleSortOrder((DBDAttributeBinding) columnElement, ctrlPressed, altPressed);
    }
    

	///////////////////////////////////////////////
	// Filtering
    
    public void showFiltering(Object columnElement) {

    	if(getSelection().getSelectedRows().size() == 0 || !getSelection().getSelectedAttributes().contains(columnElement) || curAttribute == null) {
    		spreadsheet.deselectAll();
    		controller.showDistinctFilter((DBDAttributeBinding) columnElement);
    	}   
    	else
    		controller.showDistinctFilter(curAttribute);
    	
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
                    boolean recordMode = controller.isRecordMode();
                    final DBDAttributeBinding attr = (DBDAttributeBinding) (recordMode ? cell.row : cell.col);
                    final ResultSetRow row = (ResultSetRow) (recordMode ? cell.col : cell.row);
                    final SpreadsheetValueController valueController = new SpreadsheetValueController(
                        controller,
                        attr,
                        row,
                        IValueController.EditType.NONE,
                        null);
                    PropertyCollector props = new PropertyCollector(valueController.getBinding().getAttribute(), false);
                    props.collectProperties();
                    valueController.getValueManager().contributeProperties(props, valueController);
                    return new PropertySourceDelegate(props);
                }
                return null;
            });
            return adapter.cast(page);
        } else if (adapter == IFindReplaceTarget.class) {
            return adapter.cast(findReplaceTarget);
        }
        return null;
    }

    @Nullable
    public DBDAttributeBinding getFocusAttribute()
    {
        return controller.isRecordMode() ?
            (DBDAttributeBinding) spreadsheet.getFocusRowElement() :
            (DBDAttributeBinding) spreadsheet.getFocusColumnElement();
    }

    @Nullable
    public ResultSetRow getFocusRow()
    {
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
    public IResultSetSelection getSelection() {
        return new SpreadsheetSelectionImpl();
    }

    @Override
    public void setSelection(ISelection selection) {
        if (selection instanceof IResultSetSelection && ((IResultSetSelection) selection).getController() == getController()) {
            // It may occur on simple focus change so we won't do anything
            return;
        }
        spreadsheet.deselectAll();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            List<GridPos> cellSelection = new ArrayList<>();
            for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
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
        fireSelectionChanged(selection);
    }

    @Override
    public void moveColumn(Object dragColumn, Object dropColumn, DropLocation location) {
        if (dragColumn instanceof DBDAttributeBinding && dropColumn instanceof DBDAttributeBinding) {

            DBDDataFilter dataFilter = new DBDDataFilter(controller.getModel().getDataFilter());
            final DBDAttributeConstraint dragC = dataFilter.getConstraint((DBDAttributeBinding) dragColumn);
            final DBDAttributeConstraint dropC = dataFilter.getConstraint((DBDAttributeBinding) dropColumn);
            if (dragC == null || dropC == null) {
                return;
            }

            int sourcePosition = dragC.getVisualPosition();
            int targetPosition = dropC.getVisualPosition();
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
                    dropC.setVisualPosition(dragC.getVisualPosition());
                    dragC.setVisualPosition(targetPosition);
                    break;

            }
            if (sourcePosition == targetPosition) {
                return;
            }
            if (location != DropLocation.SWAP) {
                // Reposition columns
                for (DBDAttributeConstraint c : dataFilter.getConstraints()) {
                    if (c == dragC) {
                        continue;
                    }
                    int cPos = c.getVisualPosition();
                    if (sourcePosition < targetPosition) {
                        // Move to the right
                        if (cPos > sourcePosition && cPos <= targetPosition) {
                            c.setVisualPosition(cPos - 1);
                        }
                    } else {
                        // Move to the left
                        if (cPos < sourcePosition && cPos >= targetPosition) {
                            c.setVisualPosition(cPos + 1);
                        }
                    }
                }
                dragC.setVisualPosition(targetPosition);
            }
            controller.setDataFilter(dataFilter, false);
            spreadsheet.setFocusColumn(targetPosition);
            spreadsheet.refreshData(false, true, false);
        }

    }

    @Override
    public boolean isMaximizeSingleColumn() {
        return controller.isRecordMode();
    }

    private class SpreadsheetSelectionImpl implements IResultSetSelection, IResultSetSelectionExt {

        @Nullable
        @Override
        public GridPos getFirstElement()
        {
            Collection<GridPos> ssSelection = spreadsheet.getSelection();
            if (ssSelection.isEmpty()) {
                return null;
            }
            return ssSelection.iterator().next();
        }

        @Override
        public Iterator<GridPos> iterator()
        {
            return spreadsheet.getSelection().iterator();
        }

        @Override
        public int size()
        {
            return spreadsheet.getSelection().size();
        }

        @Override
        public Object[] toArray()
        {
            return spreadsheet.getSelection().toArray();
        }

        @Override
        public List toList()
        {
            return new ArrayList<>(spreadsheet.getSelection());
        }

        @Override
        public boolean isEmpty()
        {
            return spreadsheet.getSelection().isEmpty();
        }

        @NotNull
        @Override
        public IResultSetController getController()
        {
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
                for (Object row : spreadsheet.getColumnSelection()) {
                    attrs.add((DBDAttributeBinding) row);
                }
                return attrs;
            }
        }

        @NotNull
        @Override
        public List<ResultSetRow> getSelectedRows()
        {
            if (controller.isRecordMode()) {
                ResultSetRow currentRow = controller.getCurrentRow();
                if (currentRow == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(currentRow);
            } else {
                List<ResultSetRow> rows = new ArrayList<>();
                for (Integer row : spreadsheet.getRowSelection()) {
                    rows.add(controller.getModel().getRow(row));
                }
                rows.sort(Comparator.comparingInt(ResultSetRow::getVisualNumber));
                return rows;
            }
        }

        @Override
        public DBDAttributeBinding getElementAttribute(Object element) {
            GridPos pos = (GridPos)element;
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
                    Object curRow = controller.getCurrentRow();
                    return curRow == null ? new Object[0] : new Object[] {curRow};
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

        @Nullable
        @Override
        public Object[] getChildren(Object element) {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) element;
                switch (binding.getDataKind()) {
                    case ARRAY:
                        if (controller.isRecordMode()) {
                            ResultSetRow curRow = controller.getCurrentRow();
                            if (curRow != null) {
                                Object value = controller.getModel().getCellValue(binding, curRow);
                                if (value instanceof DBDCollection) {
                                    return curRow.getCollectionData(
                                        binding,
                                        ((DBDCollection)value)).getElements();
                                }
                            }
                            return null;
                        }
                    case STRUCT:
                    case DOCUMENT:
                    case ANY:
                        final List<DBDAttributeBinding> children = controller.getModel().getVisibleAttributes(binding);
                        if (children != null) {
                            return children.toArray();
                        }
                        break;
                }
            }

            return null;
        }

        @Override
        public int getSortOrder(@Nullable Object column)
        {
            if (showAttrOrdering) {
                if (column instanceof DBDAttributeBinding) {
                    DBDAttributeBinding binding = (DBDAttributeBinding) column;
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
        public ElementState getDefaultState(@NotNull Object element) {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) element;
                switch (binding.getAttribute().getDataKind()) {
                    case STRUCT:
                    case DOCUMENT:
                        return ElementState.EXPANDED;
                    case ARRAY:
                        ResultSetRow curRow = controller.getCurrentRow();
                        if (curRow != null) {
                            Object cellValue = controller.getModel().getCellValue(binding, curRow);
                            if (cellValue instanceof DBDCollection && ((DBDCollection) cellValue).getItemCount() < 3) {
                                return ElementState.EXPANDED;
                            }
                        }
                        return ElementState.COLLAPSED;
                    default:
                        break;
                }
            }
            return ElementState.NONE;
        }

        @Override
        public int getColumnAlign(@Nullable Object element) {
            if (!controller.isRecordMode()) {
                DBDAttributeBinding attr = (DBDAttributeBinding)element;
                if (attr != null) {
                    if (isShowAsCheckbox(attr)) {
                        return ALIGN_CENTER;
                    }
                    DBPDataKind dataKind = attr.getDataKind();
                    if ((dataKind == DBPDataKind.NUMERIC && rightJustifyNumbers) ||
                        (dataKind == DBPDataKind.DATETIME && rightJustifyDateTime))
                    {
                        return ALIGN_RIGHT;
                    }
                }
            }
            return ALIGN_LEFT;
        }

        @Override
        public int getColumnPinIndex(@NotNull Object element) {
            if (!controller.isRecordMode()) {
                DBDAttributeBinding attr = (DBDAttributeBinding)element;
                DBDAttributeConstraint ac = controller.getModel().getDataFilter().getConstraint(attr);
                if (ac != null) {
                    Integer pinIndex = ac.getOption(ATTR_OPTION_PINNED);
                    return pinIndex == null ? -1 : pinIndex;
                }
            }
            return -1;
        }

        @Override
        public boolean isElementSupportsFilter(Object element) {
            if (element instanceof DBDAttributeBinding) {
                return supportsAttributeFilter;
            }
            return false;
        }

        @Override
        public boolean isElementSupportsSort(@Nullable Object element) {
            if (element instanceof DBDAttributeBinding) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isElementReadOnly(Object element) {
            if (element instanceof DBDAttributeBinding) {
                return controller.getAttributeReadOnlyStatus((DBDAttributeBinding) element) != null;
            }
            return false;
        }

        @Override
        public boolean isGridReadOnly() {
            return controller.getReadOnlyStatus() != null;
        }

        @Override
        public int getCellState(Object colElement, Object rowElement, String cellText) {
            int state = STATE_NONE;
            boolean recordMode = controller.isRecordMode();
            DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
            if ((controller.getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_LINKS) != 0) {
                ResultSetRow row = (ResultSetRow) (recordMode ? colElement : rowElement);
                Object value = controller.getModel().getCellValue(attr, row);
                if (isShowAsCheckbox(attr)) {
                    state |= STATE_LINK;
                } else if (!CommonUtils.isEmpty(attr.getReferrers()) && !DBUtils.isNullValue(value)) {
                    state |= STATE_LINK;
                } else {
                    String strValue = cellText != null ? cellText : attr.getValueHandler().getValueDisplayString(attr, value, DBDDisplayFormat.UI);
                    if (strValue.contains("://")) {
                        try {
                            new URL(strValue);
                            state |= STATE_HYPER_LINK;
                        } catch (MalformedURLException e) {
                            // Not a hyperlink
                        }
                    }
                }
            }

            if (attr.isTransformed()) {
                state |= STATE_TRANSFORMED;
            }
            return state;
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

        @Nullable
        @Override
        public Object getCellValue(Object colElement, Object rowElement, boolean formatString, boolean lockData)
        {
            DBDAttributeBinding attr = (DBDAttributeBinding)(rowElement instanceof DBDAttributeBinding ? rowElement : colElement);
            if (isShowAsCheckbox(attr)) {
                return "";
            }
            ResultSetRow row = (ResultSetRow)(colElement instanceof ResultSetRow ? colElement : rowElement);
            int rowNum = row.getVisualNumber();
            Object value = controller.getModel().getCellValue(attr, row);

            boolean recordMode = controller.isRecordMode();
            if (!lockData &&
                rowNum > 0 &&
                rowNum == controller.getModel().getRowCount() - 1 &&
                autoFetchSegments &&
                (recordMode || spreadsheet.isRowVisible(rowNum)) && controller.isHasMoreData())
            {
                controller.readNextSegment();
            }

            if (value instanceof DBDValueError) {
                return ((DBDValueError) value).getErrorTitle();
            }
            if (formatString) {
                if (recordMode) {
                    if (attr.getDataKind() == DBPDataKind.ARRAY && value instanceof DBDCollection) {
                        return "[" + ((DBDCollection) value).getItemCount() + "]";
                    } else if (attr.getDataKind() == DBPDataKind.STRUCT && value instanceof DBDComposite) {
                        return "[" + ((DBDComposite) value).getDataType().getName() + "]";
                    }
                }
                return attr.getValueRenderer().getValueDisplayString(
                    attr.getAttribute(),
                    value,
                    getValueRenderFormat(attr, value));
            } else {
                return value;
            }
        }

        @Nullable
        @Override
        public DBPImage getCellImage(Object colElement, Object rowElement)
        {
            DBDAttributeBinding attr = (DBDAttributeBinding)(rowElement instanceof DBDAttributeBinding ? rowElement : colElement);
            if (isShowAsCheckbox(attr)) {
                ResultSetRow row = (ResultSetRow)(colElement instanceof ResultSetRow ? colElement : rowElement);
                Object cellValue = controller.getModel().getCellValue(attr, row);
                if (cellValue instanceof Number) {
                    cellValue = ((Number) cellValue).byteValue() != 0;
                }
                if (cellValue instanceof Boolean) {
                    if ((Boolean)cellValue) {
                        return UIIcon.CHECK_ON;
                    } else {
                        return UIIcon.CHECK_OFF;
                    }
                }
                if (DBUtils.isNullValue(cellValue)) {
                    return UIIcon.CHECK_QUEST;
                }
            }
            return null;
        }

        @NotNull
        @Override
        public String getCellText(Object colElement, Object rowElement)
        {
            return String.valueOf(getCellValue(colElement, rowElement, true, false));
        }

        @Nullable
        @Override
        public Color getCellForeground(Object colElement, Object rowElement, boolean selected)
        {
            if (selected) {
                return foregroundSelected;
            }
            boolean recordMode = controller.isRecordMode();
            ResultSetRow row = (ResultSetRow) (!recordMode ?  rowElement : colElement);
            DBDAttributeBinding attribute = (DBDAttributeBinding)(!recordMode ?  colElement : rowElement);

            Color fg = controller.getLabelProvider().getCellForeground(attribute, row);
            if (fg != null) {
                return fg;
            }
            if (foregroundDefault == null) {
                foregroundDefault = controller.getDefaultForeground();
            }
            return foregroundDefault;
        }

        @Nullable
        @Override
        public Color getCellBackground(Object colElement, Object rowElement, boolean selected)
        {
            if (selected) {
                Color normalColor = getCellBackground(colElement, rowElement, false);
                if (normalColor == null || normalColor == backgroundNormal) {
                    return backgroundSelected;
                }
                RGB mixRGB = UIUtils.blend(
                    normalColor.getRGB(),
                    backgroundSelected.getRGB(),
                    50);
                return UIUtils.getSharedTextColors().getColor(mixRGB);
            }
            boolean recordMode = controller.isRecordMode();
            ResultSetRow row = (ResultSetRow) (!recordMode ?  rowElement : colElement);
            DBDAttributeBinding attribute = (DBDAttributeBinding)(!recordMode ?  colElement : rowElement);

            if (findReplaceTarget.isSessionActive()) {
                boolean hasScope = highlightScopeFirstLine >= 0 && highlightScopeLastLine >= 0;
                boolean inScope = hasScope && row.getVisualNumber() >= highlightScopeFirstLine && row.getVisualNumber() <= highlightScopeLastLine;
                if (!hasScope || inScope) {
                    java.util.regex.Pattern searchPattern = findReplaceTarget.getSearchPattern();
                    if (searchPattern != null) {
                        String cellText = getCellText(colElement, rowElement);
                        if (searchPattern.matcher(cellText).find()) {
                            return backgroundMatched;
                        }
                    }
                }
                if (!recordMode && inScope) {
                    return highlightScopeColor != null ? highlightScopeColor : backgroundSelected;
                }
            }

            switch (row.getState()) {
                case ResultSetRow.STATE_ADDED:
                    return backgroundAdded;
                case ResultSetRow.STATE_REMOVED:
                    return backgroundDeleted;
            }
            if (row.changes != null && row.changes.containsKey(attribute)) {
                return backgroundModified;
            }

            Color bg = controller.getLabelProvider().getCellBackground(attribute, row);
            if (bg != null) {
                return bg;
            }

            if (!recordMode && showOddRows) {
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

        @Override
        public Color getCellHeaderForeground(Object element) {
            return cellHeaderForeground;
        }

        @Override
        public Color getCellHeaderBackground(Object element) {
//            if (element instanceof DBDAttributeBinding && controller.getAttributeReadOnlyStatus((DBDAttributeBinding) element) != null) {
//                return backgroundOdd;
//            }
            return cellHeaderBackground;
        }

        @Override
        public Color getCellHeaderSelectionBackground(Object element) {
            return cellHeaderSelectionBackground;
        }

        @NotNull
        @Override
        public String getCellLinkText(Object colElement, Object rowElement) {
            boolean recordMode = controller.isRecordMode();
            DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
            ResultSetRow row = (ResultSetRow)(recordMode ? colElement : rowElement);
            Object value = controller.getModel().getCellValue(attr, row);
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

    private DBDDisplayFormat getValueRenderFormat(DBDAttributeBinding attr, Object value) {
        if (value instanceof Number && controller.getPreferenceStore().getBoolean(ModelPreferences.RESULT_NATIVE_NUMERIC_FORMAT)) {
            return DBDDisplayFormat.NATIVE;
        }
        return DBDDisplayFormat.UI;
    }

    private boolean isShowAsCheckbox(DBDAttributeBinding attr) {
        return showBooleanAsCheckbox && attr.getPresentationAttribute().getDataKind() == DBPDataKind.BOOLEAN;
    }

    private class GridLabelProvider implements IGridLabelProvider {
        @Nullable
        @Override
        public Image getImage(Object element)
        {
            if (element instanceof DBDAttributeBinding/* && (!isRecordMode() || !model.isDynamicMetadata())*/) {
                if (showAttributeIcons) {
                    DBDAttributeBinding attr = (DBDAttributeBinding) element;
                    DBPImage objectImage = DBValueFormatting.getObjectImage(attr.getAttribute());
                    if (!controller.getModel().isUpdateInProgress() &&
                        (controller.getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_EDIT) != 0 &&
                        controller.getAttributeReadOnlyStatus(attr) != null)
                    {
                        objectImage = new DBIconComposite(objectImage, false, null, null, null, DBIcon.OVER_LOCK);
                    }
                    return DBeaverIcons.getImage(objectImage);
                }
            }
            return null;
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
        public Color getForeground(Object element) {
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
        public Color getBackground(Object element) {
            if (backgroundNormal == null) {
                backgroundNormal = controller.getDefaultBackground();
            }
            if (element == null) {
                return backgroundNormal;
            }

/*
            boolean recordMode = controller.isRecordMode();
            ResultSetRow row = (ResultSetRow) (!recordMode ?  element : controller.getCurrentRow());
            boolean odd = row != null && row.getVisualNumber() % 2 == 0;
            if (!recordMode && odd && showOddRows) {
                return backgroundOdd;
            }
            return backgroundNormal;
*/
            return null;
        }

        @NotNull
        @Override
        public String getText(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
                if (CommonUtils.isEmpty(attributeBinding.getLabel())) {
                    return attributeBinding.getName();
                } else {
                    return attributeBinding.getLabel();
                }
            } else {
                if (!controller.isRecordMode()) {
                    return String.valueOf(((ResultSetRow)element).getVisualNumber() + 1);
                } else {
                    return ResultSetMessages.controls_resultset_viewer_value;
                }
            }
        }

        @Nullable
        @Override
        public String getDescription(Object element) {
            if (!showAttributeDescription) {
                return null;
            }
            if (element instanceof DBDAttributeBinding) {
                return ((DBDAttributeBinding) element).getDescription();
            } else {
                return null;
            }
        }

        @Nullable
        @Override
        public Font getFont(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
                DBDAttributeConstraint constraint = controller.getModel().getDataFilter().getConstraint(attributeBinding);
                if (constraint != null && constraint.hasCondition()) {
                    return spreadsheet.getBoldFont();
                }
                if (attributeBinding.isTransformed()) {
                    return italicFont;
                }
            }
            return null;
        }

        @Nullable
        @Override
        public String getToolTipText(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
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

        SpreadsheetValueController(@NotNull IResultSetController controller, @NotNull DBDAttributeBinding binding, @NotNull ResultSetRow row, @NotNull EditType editType, @Nullable Composite inlinePlaceholder) {
            super(controller, binding, row, editType, inlinePlaceholder);
        }

        @Override
        public Object getValue()
        {
            return spreadsheet.getContentProvider().getCellValue(curRow, binding, false, false);
        }

        @Override
        public void closeInlineEditor()
        {
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
                    if (attr.getValueHandler() != origAttr.getValueHandler()) {
                        continue;
                    }
                    if (controller.getAttributeReadOnlyStatus(attr) != null) {
                        // No inline editors for readonly columns
                        continue;
                    }
                    setBinding(attr);
                    setCurRow(row);
                    updateValue(value, false);
                }
                spreadsheet.redrawGrid();
                controller.updatePanelsContent(false);
            } finally {
                setBinding(origAttr);
                setCurRow(origRow);
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
