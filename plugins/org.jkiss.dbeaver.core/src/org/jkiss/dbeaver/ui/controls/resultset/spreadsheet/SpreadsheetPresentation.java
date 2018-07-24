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

package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.controls.lightgrid.*;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.controls.resultset.panel.valueviewer.ValueViewerPanel;
import org.jkiss.dbeaver.ui.data.IMultiController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueEditorStandalone;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
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

    private static final Log log = Log.getLog(SpreadsheetPresentation.class);

    private Spreadsheet spreadsheet;

    @Nullable
    private DBDAttributeBinding curAttribute;
    private int columnOrder = SWT.NONE;

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
    private Color foregroundNull;
    private final Map<DBPDataKind, Color> dataTypesForegrounds = new HashMap<>();
    private Color foregroundSelected, backgroundSelected;
    private Color backgroundMatched;
    private Color cellHeaderForeground, cellHeaderBackground, cellHeaderSelectionBackground;
    private Font boldFont, italicFont;

    private boolean showOddRows = true;
    private boolean showCelIcons = true;
    private boolean colorizeDataTypes = true;
    private boolean rightJustifyNumbers = true;
    private IValueEditor activeInlineEditor;

    public SpreadsheetPresentation() {
        findReplaceTarget = new SpreadsheetFindReplaceTarget(this);

    }

    public Spreadsheet getSpreadsheet() {
        return spreadsheet;
    }

    @Nullable
    DBPDataSource getDataSource() {
        DBSDataContainer dataContainer = controller.getDataContainer();
        return dataContainer == null ? null : dataContainer.getDataSource();
    }

    @Override
    public boolean isDirty() {
        return activeInlineEditor != null &&
            activeInlineEditor.getControl() != null &&
            !activeInlineEditor.getControl().isDisposed() &&
            activeInlineEditor.getControl().isVisible() &&
            !getController().getModel().isAttributeReadOnly(getCurrentAttribute());
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
                    DBUserInterface.getInstance().showError("Error extracting editor value", null, e);
                }
            }
            spreadsheet.cancelInlineEditor();
        }
        super.applyChanges();
    }

    @Override
    public void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent) {
        super.createPresentation(controller, parent);

        this.boldFont = UIUtils.makeBoldFont(parent.getFont());
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

        applyThemeSettings();

        this.spreadsheet.addDisposeListener(e -> dispose());

        trackPresentationControl();
        UIUtils.enableHostEditorKeyBindingsSupport(controller.getSite(), spreadsheet);
    }

    @Override
    public void dispose()
    {
        closeEditors();
        clearMetaData();

        UIUtils.dispose(this.italicFont);
        UIUtils.dispose(this.boldFont);

        UIUtils.dispose(this.cellHeaderSelectionBackground);
        super.dispose();
    }

    public void scrollToRow(@NotNull RowPosition position)
    {
        boolean recordMode = controller.isRecordMode();
        ResultSetRow curRow = controller.getCurrentRow();
        ResultSetModel model = controller.getModel();

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
                        spreadsheet.setCursor(newCell, false);
                    }
                }
                break;
        }
        if (controller.isRecordMode()) {
            // Update focus cell
            restoreState(curAttribute);
        }
        // Update controls
        controller.updateEditControls();
        controller.updateStatusMessage();

        if (recordMode) {
            // Refresh meta if we are in record mode
            refreshData(true, false, true);
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
        this.spreadsheet.setCursor(cell, false);
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

    @Override
    public Object saveState() {
        return curAttribute;
    }

    @Override
    public void restoreState(Object state) {
        this.curAttribute = controller.getModel().getAttributeBinding((DBDAttributeBinding) state);
        ResultSetRow curRow = controller.getCurrentRow();
        if (curRow != null && this.curAttribute != null) {
            GridCell cell = controller.isRecordMode() ?
                new GridCell(curRow, this.curAttribute) :
                new GridCell(this.curAttribute, curRow);
            //spreadsheet.selectCell(cell);
            spreadsheet.setCursor(cell, false);
        }
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
                            if (controller.isAttributeReadOnly(attr)) {
                                continue;
                            }
                            Object newValue = attr.getValueHandler().getValueFromObject(
                                session, attr.getAttribute(), value, true);
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
                    if (controller.isAttributeReadOnly(attr)) {
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
        }
        catch (Exception e) {
            DBUserInterface.getInstance().showError("Cannot replace cell value", null, e);
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
                        lines.add(curLine.toArray(new String[curLine.size()]));
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
            lines.add(curLine.toArray(new String[curLine.size()]));
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
        showOddRows = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS);
        showCelIcons = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS);
        colorizeDataTypes = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
        rightJustifyNumbers = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_RIGHT_JUSTIFY_NUMBERS);

        spreadsheet.setRedraw(false);
        try {
            spreadsheet.refreshData(refreshMetadata, keepState);
        } finally {
            spreadsheet.setRedraw(true);
        }
    }

    @Override
    public void formatData(boolean refreshData) {
        spreadsheet.refreshData(false, true);
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
        if (curAttribute != null) {
            spreadsheet.showColumn(curAttribute);
        }
    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {
        menu.add(ActionUtils.makeCommandContribution(
            controller.getSite(),
            ResultSetCommandHandler.CMD_TOGGLE_PANELS,
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
                spreadsheet.setCursor(new GridCell(oldAttribute, oldRow), false);
            } else {
                spreadsheet.setCursor(new GridCell(oldRow, oldAttribute), false);
            }
        }
        spreadsheet.layout(true, true);
    }

    public void fillContextMenu(@NotNull IMenuManager manager, @Nullable Object colObject, @Nullable Object rowObject) {
        final DBDAttributeBinding attr = (DBDAttributeBinding)(controller.isRecordMode() ? rowObject : colObject);
        final ResultSetRow row = (ResultSetRow)(controller.isRecordMode() ? colObject : rowObject);
        controller.fillContextMenu(manager, attr, row);

        if (attr != null && row != null) {
            final List<Object> selectedColumns = spreadsheet.getColumnSelection();
            if (!controller.isRecordMode() && !selectedColumns.isEmpty()) {
                IMenuManager viewMenu = (IMenuManager) manager.find(ResultSetViewer.MENU_ID_VIEW);
                if (viewMenu != null) {
                    String hideTitle;
                    if (selectedColumns.size() == 1) {
                        DBDAttributeBinding columnToHide = (DBDAttributeBinding) selectedColumns.get(0);
                        hideTitle = "Hide column '" + columnToHide.getName() + "'";
                    } else {
                        hideTitle = "Hide selected columns (" + selectedColumns.size() + ")";
                    }
                    viewMenu.add(new Separator());
                    viewMenu.add(new Action(hideTitle) {
                        @Override
                        public void run() {
                            ResultSetModel model = controller.getModel();
                            if (selectedColumns.size() >= model.getVisibleAttributeCount()) {
                                UIUtils.showMessageBox(getControl().getShell(), "Hide columns", "Can't hide all result columns, at least one column must be visible", SWT.ERROR);
                            } else {
                                for (int i = 0, selectedColumnsSize = selectedColumns.size(); i < selectedColumnsSize; i++) {
                                    model.setAttributeVisibility((DBDAttributeBinding) selectedColumns.get(i), false);
                                }
                                refreshData(true, false, true);
                            }
                        }
                    });
                }
            }
        }
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
        }

//        if (controller.isAttributeReadOnly(attr) && inline) {
//            // No inline editors for readonly columns
//            return null;
//        }

        Composite placeholder = null;
        if (inline) {
            if (controller.isReadOnly()) {
                return null;
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
/*
        if (inline &&
            (!ArrayUtils.contains(supportedEditTypes, IValueController.EditType.INLINE) || controller.isAttributeReadOnly(attr)) &&
            ArrayUtils.contains(supportedEditTypes, IValueController.EditType.PANEL))
        {
            // Inline editor isn't supported but panel viewer is
            // Enable panel
            if (!isPreviewVisible()) {
                togglePreview();
            }
            placeholder.dispose();

            return null;
        }
*/

        try {
            activeInlineEditor = valueController.getValueManager().createEditor(valueController);
        }
        catch (Exception e) {
            DBUserInterface.getInstance().showError("Cannot edit value", null, e);
            return null;
        }
        if (activeInlineEditor != null) {
            activeInlineEditor.createControl();
            if (activeInlineEditor.getControl() != null) {
                activeInlineEditor.getControl().setData(DATA_VALUE_CONTROLLER, valueController);
            }
        }
        if (activeInlineEditor instanceof IValueEditorStandalone) {
            valueController.registerEditor((IValueEditorStandalone) activeInlineEditor);
            Control editorControl = activeInlineEditor.getControl();
            if (editorControl != null) {
                editorControl.addDisposeListener(e -> valueController.unregisterEditor((IValueEditorStandalone) activeInlineEditor));
            }
            // show dialog in separate job to avoid block
            new UIJob("Open separate editor") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor)
                {
                    ((IValueEditorStandalone) activeInlineEditor).showValueEditor();
                    return Status.OK_STATUS;
                }
            }.schedule();
            //((IValueEditorStandalone)editor).showValueEditor();
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

    public void resetCellValue(@NotNull Object colElement, @NotNull Object rowElement, boolean delete)
    {
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
        if (DBUtils.isNullValue(value)) {
            log.warn("Can't navigate to NULL value");
            return;
        }
        if (!CommonUtils.isEmpty(attr.getReferrers())) {
            // Navigate association
            new AbstractJob("Navigate association") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        boolean ctrlPressed = (state & SWT.CTRL) == SWT.CTRL;
                        controller.navigateAssociation(monitor, attr, row, ctrlPressed);
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

    ///////////////////////////////////////////////
    // Themes

    @Override
    protected void applyThemeSettings()
    {
        ITheme currentTheme = themeManager.getCurrentTheme();
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
        this.foregroundNull = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_NULL_FOREGROUND);
        this.dataTypesForegrounds.put(DBPDataKind.BINARY, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_BINARY_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.BOOLEAN, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_BOOLEAN_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.DATETIME, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_DATETIME_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.NUMERIC, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_NUMERIC_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.STRING, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_STRING_FOREGROUND));


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
            spreadsheet.refreshData(false, true);
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
            page.setPropertySourceProvider(new IPropertySourceProvider() {
                @Nullable
                @Override
                public IPropertySource getPropertySource(Object object) {
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
                }
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
            spreadsheet.refreshData(false, true);
        }

    }

    private class SpreadsheetSelectionImpl implements IResultSetSelection {

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
            if (controller.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ATTR_ORDERING)) {
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
            if (rightJustifyNumbers && !controller.isRecordMode()) {
                DBDAttributeBinding attr = (DBDAttributeBinding)element;
                if (attr != null) {
                    DBPDataKind dataKind = attr.getDataKind();
                    if (dataKind == DBPDataKind.NUMERIC || dataKind == DBPDataKind.DATETIME) {
                        return ALIGN_RIGHT;
                    }
                }
            }
            return ALIGN_LEFT;
        }

        @Override
        public boolean isElementSupportsFilter(Object element) {
            if (element instanceof DBDAttributeBinding) {
                return (controller.getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_FILTER) != 0 &&
                    controller.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ATTR_FILTERS);
            }
            return false;
        }

        @Override
        public int getCellState(Object colElement, Object rowElement, String cellText) {
            int state = STATE_NONE;
            boolean recordMode = controller.isRecordMode();
            DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
            ResultSetRow row = (ResultSetRow)(recordMode ? colElement : rowElement);
            Object value = controller.getModel().getCellValue(attr, row);
            if (!CommonUtils.isEmpty(attr.getReferrers()) && !DBUtils.isNullValue(value)) {
                state |= STATE_LINK;
            } else {
                String strValue = cellText != null ? cellText : attr.getValueHandler().getValueDisplayString(attr, value, DBDDisplayFormat.UI);
                if (strValue != null && strValue.contains(":")) {
                    try {
                        new URL(strValue);
                        state |= STATE_HYPER_LINK;
                    } catch (MalformedURLException e) {
                        // Not a hyperlink
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
        public Object getCellValue(Object colElement, Object rowElement, boolean formatString)
        {
            DBDAttributeBinding attr = (DBDAttributeBinding)(rowElement instanceof DBDAttributeBinding ? rowElement : colElement);
            ResultSetRow row = (ResultSetRow)(colElement instanceof ResultSetRow ? colElement : rowElement);
            int rowNum = row.getVisualNumber();
            Object value = controller.getModel().getCellValue(attr, row);

            boolean recordMode = controller.isRecordMode();
            if (rowNum > 0 &&
                rowNum == controller.getModel().getRowCount() - 1 &&
                controller.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_AUTO_FETCH_NEXT_SEGMENT) &&
                (recordMode || spreadsheet.isRowVisible(rowNum)) && controller.isHasMoreData())
            {
                controller.readNextSegment();
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
                    DBDDisplayFormat.UI);
            } else {
                return value;
            }
        }

        @Nullable
        @Override
        public DBPImage getCellImage(Object colElement, Object rowElement)
        {
            // TODO: tired from cell icons. But maybe they make some sense - let's keep them commented
/*
            if (!showCelIcons) {
                return null;
            }
            Object cellValue = getCellValue(colElement, rowElement, false);
            if (cellValue instanceof DBDContent || cellValue instanceof DBDReference) {
                DBDAttributeBinding attr = (DBDAttributeBinding)(controller.isRecordMode() ? rowElement : colElement);
                return DBUtils.getObjectImage(attr);
            } else {
                return null;
            }
*/
            return null;
        }

        @NotNull
        @Override
        public String getCellText(Object colElement, Object rowElement)
        {
            return String.valueOf(getCellValue(colElement, rowElement, true));
        }

        @Nullable
        @Override
        public Color getCellForeground(Object colElement, Object rowElement, boolean selected)
        {
            if (selected) {
                return foregroundSelected;
            }
            ResultSetRow row = (ResultSetRow) (!controller.isRecordMode() ?  rowElement : colElement);
            if (row.foreground != null) {
                return row.foreground;
            }

            Object value = getCellValue(colElement, rowElement, false);
            if (DBUtils.isNullValue(value)) {
                return foregroundNull;
            } else {
                if (colorizeDataTypes) {
                    DBDAttributeBinding attr =
                            (DBDAttributeBinding)(rowElement instanceof DBDAttributeBinding ? rowElement : colElement);
                    Color color = dataTypesForegrounds.get(attr.getDataKind());
                    if (color != null) {
                        return color;
                    }
                }
                if (foregroundDefault == null) {
                    foregroundDefault = controller.getDefaultForeground();
                }
                return foregroundDefault;
            }
        }

        @Nullable
        @Override
        public Color getCellBackground(Object colElement, Object rowElement, boolean selected)
        {
            if (selected) {
                Color normalColor = getCellBackground(colElement, rowElement, false);
                if (normalColor == backgroundNormal) {
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
                java.util.regex.Pattern searchPattern = findReplaceTarget.getSearchPattern();
                if (searchPattern != null) {
                    String cellText = getCellText(colElement, rowElement);
                    if (searchPattern.matcher(cellText).find()) {
                        return backgroundMatched;
                    }
                }
            }

            if (row.getState() == ResultSetRow.STATE_ADDED) {
                return backgroundAdded;
            }
            if (row.getState() == ResultSetRow.STATE_REMOVED) {
                return backgroundDeleted;
            }
            if (row.changes != null && row.changes.containsKey(attribute)) {
                return backgroundModified;
            }
            if (row.background != null) {
                return row.background;
            }
            if (attribute.getValueHandler() instanceof DBDValueHandlerComposite) {
                return backgroundReadOnly;
            }
            if (!recordMode && showOddRows) {
                // Determine odd/even row
                int rowBatchSize = getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_ROW_BATCH_SIZE);
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
            if (element instanceof DBDAttributeBinding && controller.isAttributeReadOnly((DBDAttributeBinding) element)) {
                return backgroundOdd;
            }
            return cellHeaderBackground;
        }

        @Override
        public Color getCellHeaderSelectionBackground(Object element) {
            return cellHeaderSelectionBackground;
        }

        @Override
        public void resetColors() {
            backgroundNormal = null;
            foregroundDefault = null;
        }
    }

    private class GridLabelProvider implements IGridLabelProvider {
        @Nullable
        @Override
        public Image getImage(Object element)
        {
            if (element instanceof DBDAttributeBinding/* && (!isRecordMode() || !model.isDynamicMetadata())*/) {
                if (controller.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ATTR_ICONS)) {
                    return DBeaverIcons.getImage(DBValueFormatting.getObjectImage(((DBDAttributeBinding) element).getAttribute()));
                }
            }
            return null;
        }

        @Override
        public Object getGridOption(String option) {
            if (OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC.equals(option)) {
                return getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_CALC_COLUMN_WIDTH_BY_VALUES);
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
                    return CoreMessages.controls_resultset_viewer_value;
                }
            }
        }

        @Nullable
        @Override
        public String getDescription(Object element) {
            if (!getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_SHOW_DESCRIPTION)) {
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
                    return boldFont;
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
                if (controller.isAttributeReadOnly(attributeBinding)) {
                    tip += " (read-only)";
                }
                return tip;
            }
            return null;
        }
    }

    /////////////////////////////
    // Value controller

    public class SpreadsheetValueController extends ResultSetValueController implements IMultiController {

        public SpreadsheetValueController(@NotNull IResultSetController controller, @NotNull DBDAttributeBinding binding, @NotNull ResultSetRow row, @NotNull EditType editType, @Nullable Composite inlinePlaceholder) {
            super(controller, binding, row, editType, inlinePlaceholder);
        }

        @Override
        public Object getValue()
        {
            return spreadsheet.getContentProvider().getCellValue(curRow, binding, false);
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
                    if (controller.isAttributeReadOnly(attr)) {
                        // No inline editors for readonly columns
                        continue;
                    }
                    setBinding(attr);
                    setCurRow(row);
                    updateValue(value, true);
                }
            } finally {
                setBinding(origAttr);
                setCurRow(origRow);
            }
        }

        public void registerEditor(IValueEditorStandalone editor) {
            openEditors.put(this, editor);
        }

        public void unregisterEditor(IValueEditorStandalone editor) {
            openEditors.remove(this);
        }

    }

}
