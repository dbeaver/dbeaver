/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.swt.IFocusService;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.jkiss.dbeaver.ext.IResultSetProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.dbc.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ThemeConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.export.wizard.DataExportWizard;
import org.jkiss.dbeaver.ui.controls.spreadsheet.ISpreadsheetController;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ResultSetViewer
 */
public class ResultSetViewer extends Viewer implements ISpreadsheetController, IPropertyChangeListener
{
    static final Log log = LogFactory.getLog(ResultSetViewer.class);

    private static final int DEFAULT_ROW_HEADER_WIDTH = 50;

    private IWorkbenchPartSite site;
    private ResultSetMode mode;
    private Spreadsheet spreadsheet;
    private IResultSetProvider resultSetProvider;
    private ResultSetDataReceiver dataReciever;
    private IThemeManager themeManager;

    // columns
    private DBDColumnBinding[] metaColumns;
    // Data
    private final List<Object[]> curRows = new ArrayList<Object[]>();
    // Current row number (for record mode)
    private int curRowNum = -1;
    private boolean singleSourceCells;

    // Edited rows and cells
    private final Set<RowInfo> addedRows = new TreeSet<RowInfo>();
    private final Set<RowInfo> removedRows = new TreeSet<RowInfo>();
    private Map<CellInfo, Object> editedValues = new HashMap<CellInfo, Object>();

    private Label statusLabel;

    private ToolItem itemAccept;
    private ToolItem itemReject;

    private ToolItem itemRowEdit;
    private ToolItem itemRowAdd;
    private ToolItem itemRowCopy;
    private ToolItem itemRowDelete;

    private ToolItem itemToggleView;
    private ToolItem itemNext;
    private ToolItem itemPrevious;
    private ToolItem itemFirst;
    private ToolItem itemLast;
    private ToolItem itemRefresh;

    private Map<ResultSetValueController, DBDValueEditor> openEditors = new HashMap<ResultSetValueController, DBDValueEditor>();
    // Flag saying that edited values update is in progress
    private boolean updateInProgress = false;

    // UI modifiers
    private Color colorRed;
    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;
    private Color foregroundNull;

    public ResultSetViewer(Composite parent, IWorkbenchPartSite site, IResultSetProvider resultSetProvider)
    {
        super();
        this.site = site;
        this.mode = ResultSetMode.GRID;

        this.colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
        this.backgroundAdded = new Color(parent.getDisplay(), 0xE4, 0xFF, 0xB5);
        this.backgroundDeleted = new Color(parent.getDisplay(), 0xFF, 0x63, 0x47);
        this.backgroundModified = new Color(parent.getDisplay(), 0xFF, 0xE4, 0xB5);
        this.foregroundNull = parent.getDisplay().getSystemColor(SWT.COLOR_GRAY);

        this.spreadsheet = new Spreadsheet(
            parent,
            SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
            site,
            this,
            new ContentProvider(),
            new ContentLabelProvider(),
            new ColumnLabelProvider(),
            new RowLabelProvider());

        createStatusBar(spreadsheet);
        changeMode(ResultSetMode.GRID);
        this.resultSetProvider = resultSetProvider;
        this.dataReciever = new ResultSetDataReceiver(this);

        this.themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
        this.themeManager.addPropertyChangeListener(this);
        this.spreadsheet.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        this.spreadsheet.addCursorChangeListener(new Listener() {
            public void handleEvent(Event event)
            {
                onChangeGridCursor(event.x, event.y);
            }
        });

        applyThemeSettings();

        {
            final IFocusService focusService = (IFocusService) site.getService(IFocusService.class);
            focusService.addFocusTracker(spreadsheet, "org.jkiss.dbeaver.ui.resultset.grid");
            spreadsheet.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent e) {
                    focusService.removeFocusTracker(spreadsheet);
                }
            });
        }
    }

    private void updateGridCursor()
    {
        GridPos point = spreadsheet.getCursorPosition();
        if (point == null) {
            onChangeGridCursor(0, 0);
        } else {
            onChangeGridCursor(point.col, point.row);
        }
    }

    private void onChangeGridCursor(int col, int row)
    {
        if (mode == ResultSetMode.GRID) {
            int rowsNum = spreadsheet.getItemCount();
            //int colsNum = spreadsheet.getColumnsCount();
            boolean isFirst = (row <= 0);
            boolean isLast = rowsNum == 0 || (row >= rowsNum - 1);

            boolean isVisible = col >= 0;
            itemFirst.setEnabled(isVisible && !isFirst);
            itemPrevious.setEnabled(isVisible && !isFirst);
            itemNext.setEnabled(isVisible && !isLast);
            itemLast.setEnabled(isVisible && !isLast);
            itemRefresh.setEnabled(resultSetProvider != null && resultSetProvider.getDataSource() != null);
        }

        boolean validPosition;
        if (mode == ResultSetMode.GRID) {
            validPosition = (col >= 0 && row >= 0);
        } else {
            validPosition = curRowNum >= 0;
        }
        itemRowEdit.setEnabled(validPosition);
        itemRowAdd.setEnabled(singleSourceCells && !CommonUtils.isEmpty(metaColumns));
        itemRowCopy.setEnabled(singleSourceCells && validPosition);
        itemRowDelete.setEnabled(singleSourceCells && validPosition);
    }

    private void updateRecord()
    {
        boolean isFirst = curRowNum <= 0;
        boolean isLast = curRows.isEmpty() || (curRowNum >= curRows.size() - 1);

        itemFirst.setEnabled(!isFirst);
        itemPrevious.setEnabled(!isFirst);
        itemNext.setEnabled(!isLast);
        itemLast.setEnabled(!isLast);

        this.initResultSet();
    }

    private void updateEditControls()
    {
        boolean hasChanges = !editedValues.isEmpty() || !addedRows.isEmpty() || !removedRows.isEmpty();
        itemAccept.setEnabled(hasChanges);
        itemReject.setEnabled(hasChanges);
    }

    private void refreshSpreadsheet(boolean rowsChanged)
    {
        if (rowsChanged) {
            GridPos curPos = spreadsheet.getCursorPosition();
            if (mode == ResultSetMode.RECORD) {
                if (curRowNum >= curRows.size()) {
                    curRowNum = curRows.size() - 1;
                }
            } else if (mode == ResultSetMode.GRID) {
                if (curPos.row >= curRows.size()) {
                    curPos.row = curRows.size() - 1;
                }
            }

            this.spreadsheet.reinitState();

            // Set cursor on new row
            if (mode == ResultSetMode.GRID) {
                spreadsheet.setCursor(curPos, false);
            } else {
                updateRecord();
            }

        } else {
            this.spreadsheet.redrawGrid();
        }
        updateEditControls();
    }

    private void createStatusBar(Composite parent)
    {
        Composite statusBar = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        statusBar.setLayoutData(gd);
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 5;
        gl.marginHeight = 0;
        statusBar.setLayout(gl);
        
        statusLabel = new Label(statusBar, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        statusLabel.setLayoutData(gd);

        {
            ToolBar toolBar = new ToolBar(statusBar, SWT.FLAT | SWT.HORIZONTAL);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            toolBar.setLayoutData(gd);

            itemAccept = UIUtils.createToolItem(toolBar, "Apply changes", DBIcon.ACCEPT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    applyChanges();
                }
            });
            itemReject = UIUtils.createToolItem(toolBar, "Reject changes", DBIcon.REJECT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    rejectChanges();
                }
            });
            new ToolItem(toolBar, SWT.SEPARATOR);

            itemRowEdit = UIUtils.createToolItem(toolBar, "Edit cell", DBIcon.ROW_EDIT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    showCellEditor(spreadsheet.getCursorPosition(), false, null);
                }
            });
            itemRowAdd = UIUtils.createToolItem(toolBar, "Add row", DBIcon.ROW_ADD, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    addNewRow(false);
                }
            });
            itemRowCopy = UIUtils.createToolItem(toolBar, "Copy row", DBIcon.ROW_COPY, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    addNewRow(true);
                }
            });
            itemRowDelete = UIUtils.createToolItem(toolBar, "Delete row", DBIcon.ROW_DELETE, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    deleteCurrentRow();
                }
            });

            new ToolItem(toolBar, SWT.SEPARATOR);

            itemToggleView = UIUtils.createToolItem(toolBar, "Toggle View", DBIcon.RS_MODE_GRID, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    changeMode(mode == ResultSetMode.GRID ? ResultSetMode.RECORD : ResultSetMode.GRID);
                }
            });
            new ToolItem(toolBar, SWT.SEPARATOR);
            itemFirst = UIUtils.createToolItem(toolBar, "First", DBIcon.RS_FIRST, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD) {
                        curRowNum = 0;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, -spreadsheet.getItemCount(), false);
                    }
                }
            });
            itemPrevious = UIUtils.createToolItem(toolBar, "Previous", DBIcon.RS_PREV, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && curRowNum > 0) {
                        curRowNum--;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, -1, false);
                    }
                }
            });
            itemNext = UIUtils.createToolItem(toolBar, "Next", DBIcon.RS_NEXT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && curRowNum < curRows.size() - 1) {
                        curRowNum++;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, 1, false);
                    }
                }
            });
            itemLast = UIUtils.createToolItem(toolBar, "Last", DBIcon.RS_LAST, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && !curRows.isEmpty()) {
                        curRowNum = curRows.size() - 1;
                        updateRecord();
                    } else {
                        spreadsheet.shiftCursor(0, spreadsheet.getItemCount(), false);
                    }
                }
            });
            new ToolItem(toolBar, SWT.SEPARATOR);
            itemRefresh = UIUtils.createToolItem(toolBar, "Refresh", DBIcon.RS_REFRESH, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    refresh();
                }
            });
        }
        updateEditControls();
    }

    private void changeMode(ResultSetMode resultSetMode)
    {
        this.mode = resultSetMode;
        if (mode == ResultSetMode.GRID) {
            spreadsheet.setRowHeaderWidth(DEFAULT_ROW_HEADER_WIDTH);
            itemToggleView.setImage(DBIcon.RS_MODE_GRID.getImage());
        } else {
            // Calculate width of spreadsheet panel - use longest column title
            int defaultWidth = 0;
            if (metaColumns != null) {
                GC gc = new GC(spreadsheet);
                gc.setFont(spreadsheet.getFont());
                for (DBDColumnBinding column : metaColumns) {
                    Point ext = gc.stringExtent(column.getColumn().getName());
                    if (ext.x > defaultWidth) {
                        defaultWidth = ext.x;
                    }
                }
                defaultWidth += DBIcon.EDIT_COLUMN.getImage().getBounds().width + 2;
            }
            spreadsheet.setRowHeaderWidth(defaultWidth + DEFAULT_ROW_HEADER_WIDTH);
            itemToggleView.setImage(DBIcon.RS_MODE_RECORD.getImage());
            GridPos curPos = spreadsheet.getCursorPosition();
            if (curPos != null) {
                curRowNum = curPos.row;
                if (curRowNum < 0) {
                    curRowNum = 0;
                }
            } else {
                curRowNum = 0;
            }
            updateRecord();
        }

        this.initResultSet();

        if (mode == ResultSetMode.GRID) {
            if (curRowNum >= 0) {
                spreadsheet.setCursor(new GridPos(0, curRowNum), false);
            }
        }
        spreadsheet.layout(true, true);
    }

    public IResultSetProvider getResultSetProvider()
    {
        return resultSetProvider;
    }

    public void dispose()
    {
        closeEditors();
        clearData();

        if (!spreadsheet.isDisposed()) {
            spreadsheet.dispose();
        }
        itemAccept.dispose();
        itemReject.dispose();
        itemRowEdit.dispose();
        itemRowAdd.dispose();
        itemRowCopy.dispose();
        itemRowDelete.dispose();
        itemToggleView.dispose();
        itemNext.dispose();
        itemPrevious.dispose();
        itemFirst.dispose();
        itemLast.dispose();
        itemRefresh.dispose();
        statusLabel.dispose();
        themeManager.removePropertyChangeListener(ResultSetViewer.this);
    }

    private void applyThemeSettings()
    {
        ITheme currentTheme = themeManager.getCurrentTheme();
        Font rsFont = currentTheme.getFontRegistry().get(ThemeConstants.FONT_SQL_RESULT_SET);
        if (rsFont != null) {
            this.spreadsheet.setFont(rsFont);
        }
        Color selBackColor = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK);
        if (selBackColor != null) {
            this.spreadsheet.setBackgroundSelected(selBackColor);
        }
        Color selForeColor = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE);
        if (selForeColor != null) {
            this.spreadsheet.setForegroundSelected(selForeColor);
        }
    }

    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME)
            || event.getProperty().equals(ThemeConstants.FONT_SQL_RESULT_SET)
            || event.getProperty().equals(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK)
            || event.getProperty().equals(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE))
        {
            applyThemeSettings();
        }
    }

    static boolean isColumnReadOnly(DBDColumnBinding column)
    {
        return
            column.getValueLocator() == null ||
            !(column.getValueLocator().getTable() instanceof DBSDataContainer);
    }

    public int getRowsCount()
    {
        return curRows.size();
    }

    int getRowIndex(Object[] row)
    {
        return curRows.indexOf(row);
    }

    public void setStatus(String status)
    {
        setStatus(status, false);
    }

    public void setStatus(String status, boolean error)
    {
        if (error) {
            statusLabel.setForeground(colorRed);
        } else {
            statusLabel.setForeground(null);
        }
        statusLabel.setText(status);
    }

    public void setColumnsInfo(DBDColumnBinding[] metaColumns)
    {
        this.metaColumns = metaColumns;
    }

    public void setData(List<Object[]> rows)
    {
        // Clear previous data
        this.closeEditors();
        this.clearData();

        // Add new data
        this.curRows.addAll(rows);

        // Check single source flag
        this.singleSourceCells = true;
        DBSTable sourceTable = null;
        for (DBDColumnBinding column : metaColumns) {
            if (isColumnReadOnly(column)) {
                singleSourceCells = false;
                break;
            }
            if (sourceTable == null) {
                sourceTable = column.getValueLocator().getTable();
            } else if (sourceTable != column.getValueLocator().getTable()) {
                singleSourceCells = false;
                break;
            }
        }

        this.initResultSet();
    }

    public void appendData(List<Object[]> rows)
    {
        curRows.addAll(rows);
        refreshSpreadsheet(true);
    }

    private void clearResultsView()
    {
        // Clear previous state
        spreadsheet.setRedraw(false);
        spreadsheet.clearGrid();
        spreadsheet.setRedraw(true);
    }

    private void closeEditors() {
        List<DBDValueEditor> editors = new ArrayList<DBDValueEditor>(openEditors.values());
        for (DBDValueEditor editor : editors) {
            editor.closeValueEditor();
        }
        if (!openEditors.isEmpty()) {
            log.warn("Some value editors are still registered at resulset: " + openEditors.size());
        }
        openEditors.clear();
    }

    private void initResultSet()
    {
        closeEditors();

        spreadsheet.setRedraw(false);
        spreadsheet.clearGrid();
        if (mode == ResultSetMode.RECORD) {
            this.showCurrentRows();
        } else {
            this.showRowsCount();
        }

        spreadsheet.reinitState();
        spreadsheet.setRedraw(true);

        this.updateGridCursor();
    }

    public boolean isEditable()
    {
        return !updateInProgress;
    }

    public boolean isCellEditable(int col, int row) {
        return true;
    }

    public boolean isCellModified(int col, int row) {
        return 
            !editedValues.isEmpty() &&
                editedValues.containsKey(new CellInfo(col, row));
    }

    public boolean isInsertable()
    {
        return false;
    }

    public boolean showCellEditor(
        final GridPos cell,
        final boolean inline,
        final Composite inlinePlaceholder)
    {
        final int columnIndex = (mode == ResultSetMode.GRID ? cell.col : cell.row);
        final int rowIndex = (mode == ResultSetMode.GRID ? cell.row : curRowNum);
        if (rowIndex >= curRows.size() || columnIndex >= metaColumns.length) {
            // Out of bounds
            log.debug("Editor position is out of bounds (" + columnIndex + ":" + rowIndex + ")");
            return false;
        }
        if (!inline) {
            GridPos testCell = new GridPos(columnIndex, rowIndex);
            for (ResultSetValueController valueController : openEditors.keySet()) {
                GridPos cellPos = valueController.getCellPos();
                if (cellPos != null && cellPos.equalsTo(testCell)) {
                    openEditors.get(valueController).showValueEditor();
                    return true;
                }
            }
        }
        DBDColumnBinding metaColumn = metaColumns[columnIndex];
        if (isColumnReadOnly(metaColumn) && inline) {
            // No inline editors for readonly columns
            return false;
        }
        ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(rowIndex),
            columnIndex,
            inline ? inlinePlaceholder : null);
        try {
            return metaColumn.getValueHandler().editValue(valueController);
        }
        catch (Exception e) {
            log.error(e);
            return false;
        }
    }

    public void fillContextMenu(final GridPos cell, IMenuManager manager) {

        // Custom oldValue items
        final int columnIndex = (mode == ResultSetMode.GRID ? cell.col : cell.row);
        final int rowIndex = (mode == ResultSetMode.GRID ? cell.row : curRowNum);
        if (rowIndex < 0 || curRows.size() <= rowIndex || columnIndex < 0 || columnIndex >= metaColumns.length) {
            return;
        }
        final ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(rowIndex),
            columnIndex,
            null);
        final Object value = valueController.getValue();

        // Standard items
        manager.add(new Separator());
        manager.add(new Action("Edit ...") {
            @Override
            public void run()
            {
                showCellEditor(cell, false, null);
            }
        });
        if (!DBUtils.isNullValue(value)) {
            manager.add(new Action("Set to NULL") {
                @Override
                public void run()
                {
                    valueController.updateValue(DBUtils.makeNullValue(value));
                }
            });
        }

        // Menus from value handler
        try {
            manager.add(new Separator());
            metaColumns[columnIndex].getValueHandler().fillContextMenu(manager, valueController);
        }
        catch (Exception e) {
            log.error(e);
        }

        // Export and other utility methods
        manager.add(new Separator());
        manager.add(new Action("Export Resultset ... ", DBIcon.EXPORT.getImageDescriptor()) {
            @Override
            public void run()
            {
                DataExportWizard wizard = new DataExportWizard(
                    Collections.singletonList(getResultSetProvider()));
                wizard.init(site.getWorkbenchWindow().getWorkbench(), getSelection());
                ActiveWizardDialog dialog = new ActiveWizardDialog(site.getShell(), wizard);
                dialog.open();
            }
        });

    }

    private void showCurrentRows()
    {
        setStatus("Row " + (curRowNum + 1));
    }

    private void showRowsCount()
    {
        setStatus(
            String.valueOf(curRows.size()) +
                " row" + (curRows.size() > 1 ? "s" : "") + " fetched");
    }

    public Control getControl()
    {
        return spreadsheet;
    }

    public Object getInput()
    {
        return null;
    }

    public void setInput(Object input)
    {
    }

    public IStructuredSelection getSelection()
    {
        return new StructuredSelection(spreadsheet.getSelection().toArray());
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public DBDDataReceiver getDataReciever() {
        return dataReciever;
    }

    public void refresh()
    {
        this.closeEditors();
        this.clearData();
        this.clearResultsView();
        if (resultSetProvider != null) {
            resultSetProvider.startDataExtraction(dataReciever, 0, getSegmentMaxRows());
        }
        updateGridCursor();
    }

    synchronized void readNextSegment()
    {
        if (!dataReciever.isHasMoreData()) {
            return;
        }
        if (resultSetProvider != null && !resultSetProvider.isRunning()) {
            dataReciever.setHasMoreData(false);
            dataReciever.setNextSegmentRead(true);
            resultSetProvider.startDataExtraction(dataReciever, curRows.size(), getSegmentMaxRows());
        }
    }

    int getSegmentMaxRows()
    {
        if (resultSetProvider == null) {
            return 0;
        }
        IPreferenceStore preferenceStore = resultSetProvider.getDataSource().getContainer().getPreferenceStore();
        return preferenceStore.getInt(PrefConstants.RESULT_SET_MAX_ROWS);
    }

    private void clearData()
    {
        // Refresh all rows
        this.releaseAll();
        this.curRows.clear();
        this.curRowNum = 0;

        this.editedValues.clear();
        this.addedRows.clear();
        this.removedRows.clear();
    }

    private void applyChanges()
    {
        try {
            new DataUpdater().applyChanges(null);
        } catch (DBException e) {
            log.error("Could not obtain result set metdata", e);
        }
    }

    private void rejectChanges()
    {
        new DataUpdater().rejectChanges();
    }

    private boolean isRowAdded(int row)
    {
        return addedRows.contains(new RowInfo(row));
    }

    private void addNewRow(boolean copyCurrent)
    {
        GridPos curPos = spreadsheet.getCursorPosition();
        int rowNum;
        if (mode == ResultSetMode.RECORD) {
            rowNum = this.curRowNum;
        } else {
            rowNum = curPos.row;
        }
        if (rowNum < 0) {
            rowNum = 0;
        }
        shiftRows(rowNum, 1);

        // Add new row
        final Object[] cells = new Object[metaColumns.length];
        if (copyCurrent) {
            final int currentRowNumber = rowNum;
            DBeaverCore.getInstance().runAndWait(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    // Copy cell values
                    Object[] origRow = curRows.get(currentRowNumber);
                    for (int i = 0; i < metaColumns.length; i++) {
                        DBDColumnBinding metaColumn = metaColumns[i];
                        if (metaColumn.getTableColumn().isAutoIncrement()) {
                            // set autoincrement columns to null
                            cells[i] = null;
                        } else {
                            try {
                                cells[i] = metaColumn.getValueHandler().copyValueObject(monitor, origRow[i]);
                            }
                            catch (DBCException e) {
                                log.warn(e);
                                cells[i] = DBUtils.makeNullValue(origRow[i]);
                            }
                        }
                    }
                }
            });
        }
        curRows.add(rowNum, cells);

        addedRows.add(new RowInfo(rowNum));
        refreshSpreadsheet(true);
    }

    private void shiftRows(int rowNum, int delta)
    {
        // Slide all existing edited rows/cells down
        for (CellInfo cell : editedValues.keySet()) {
            if (cell.row >= rowNum) cell.row += delta;
        }
        for (RowInfo row : addedRows) {
            if (row.row >= rowNum) row.row += delta;
        }
        for (RowInfo row : removedRows) {
            if (row.row >= rowNum) row.row += delta;
        }
    }

    private boolean isRowDeleted(int row)
    {
        return removedRows.contains(new RowInfo(row));
    }

    private void deleteCurrentRow()
    {
        GridPos curPos = spreadsheet.getCursorPosition();
        int rowNum;
        if (mode == ResultSetMode.RECORD) {
            rowNum = this.curRowNum;
        } else {
            rowNum = curPos.row;
        }

        RowInfo rowInfo = new RowInfo(rowNum);
        if (addedRows.contains(rowInfo)) {
            // Remove just added row 
            addedRows.remove(rowInfo);
            deleteRow(rowNum);

            refreshSpreadsheet(true);

        } else {
            // Mark row as deleted
            removedRows.add(rowInfo);
        }
        spreadsheet.redrawGrid();
        updateEditControls();
    }

    private void deleteRow(int rowNum)
    {
        this.releaseRow(this.curRows.get(rowNum));
        this.curRows.remove(rowNum);
        this.shiftRows(rowNum, -1);
    }

    private boolean deleteRows(Set<RowInfo> rows)
    {
        if (rows != null && !rows.isEmpty()) {
            // Remove rows (in descending order to prevent concurrent modification errors)
            int[] rowsToRemove = new int[rows.size()];
            int i = 0;
            for (RowInfo rowNum : rows) rowsToRemove[i++] = rowNum.row;
            Arrays.sort(rowsToRemove);
            for (i = rowsToRemove.length; i > 0; i--) {
                deleteRow(rowsToRemove[i - 1]);
            }
            rows.clear();
            return true;
        } else {
            return false;
        }
    }

    private void releaseAll()
    {
        for (Object[] row : curRows) {
            releaseRow(row);
        }
        releaseEditedValues();
    }

    private void releaseEditedValues()
    {
        for (Object oldValue : editedValues.values()) {
            releaseValue(oldValue);
        }
    }

    private void releaseRow(Object[] values)
    {
        for (Object value : values) {
            if (value instanceof DBDValue) {
                ((DBDValue)value).release();
            }
        }
    }

    private void releaseValue(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    private Image getColumnImage(DBDColumnBinding column)
    {
        if (column.getColumn() instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)column.getColumn()).getObjectImage();
        } else {
            return DBIcon.TREE_COLUMN.getImage();
        }
    }

    private int getMetaColumnIndex(DBCColumnMetaData column)
    {
        for (int i = 0; i < metaColumns.length; i++) {
            if (column == metaColumns[i].getColumn()) {
                return i;
            }
        }
        return -1;
    }

    private int getMetaColumnIndex(DBSTableColumn column)
    {
        for (int i = 0; i < metaColumns.length; i++) {
            if (column == metaColumns[i].getTableColumn()) {
                return i;
            }
        }
        return -1;
    }

    public int getMetaColumnIndex(DBSTable table, String columnName)
    {
        for (int i = 0; i < metaColumns.length; i++) {
            DBDColumnBinding column = metaColumns[i];
            if (column.getValueLocator().getTable() == table && column.getColumn().getName().equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private class ResultSetValueController implements DBDValueController, DBDRowController {

        private Object[] curRow;
        private int columnIndex;
        private Composite inlinePlaceholder;

        private ResultSetValueController(Object[] curRow, int columnIndex, Composite inlinePlaceholder) {
            this.curRow = curRow;
            this.columnIndex = columnIndex;
            this.inlinePlaceholder = inlinePlaceholder;
        }

        public DBPDataSource getDataSource()
        {
            return resultSetProvider.getDataSource();
        }

        public DBDRowController getRow() {
            return this;
        }

        public DBCColumnMetaData getColumnMetaData()
        {
            return metaColumns[columnIndex].getColumn();
        }

        public String getColumnId() {
            String dsName = getDataSource().getContainer().getName();
            String catalogName = getColumnMetaData().getCatalogName();
            String schemaName = getColumnMetaData().getSchemaName();
            String tableName = getColumnMetaData().getTableName();
            String columnName = getColumnMetaData().getName();
            StringBuilder columnId = new StringBuilder(CommonUtils.escapeIdentifier(dsName));
            if (!CommonUtils.isEmpty(catalogName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(catalogName));
            }
            if (!CommonUtils.isEmpty(schemaName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(schemaName));
            }
            if (!CommonUtils.isEmpty(tableName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(tableName));
            }
            if (!CommonUtils.isEmpty(columnName)) {
                columnId.append('.').append(CommonUtils.escapeIdentifier(columnName));
            }
            return columnId.toString();
        }

        public Object getValue()
        {
            return curRow[columnIndex];
        }

        public void updateValue(Object value)
        {
            Object oldValue = curRow[columnIndex];
            if (value instanceof DBDValue || !CommonUtils.equalObjects(oldValue, value)) {
                int rowIndex = getRowIndex(curRow);
                if (rowIndex >= 0) {
                    if (DBUtils.isNullValue(oldValue) && DBUtils.isNullValue(value)) {
                        // Both nulls - nothing to update
                        return;
                    }
                    // Do not add edited cell for new/deleted rows
                    if (!isRowAdded(rowIndex) && !isRowDeleted(rowIndex)) {
                        // Save old value
                        CellInfo cell = new CellInfo(columnIndex, rowIndex);
                        Object oldOldValue = editedValues.get(cell);
                        if (oldOldValue != null && !CommonUtils.equalObjects(oldValue, oldOldValue)) {
                            // Value rewrite - release previous stored old value
                            releaseValue(oldValue);
                        } else {
                            editedValues.put(cell, oldValue);
                        }
                    }
                    curRow[columnIndex] = value;
                    // Update controls
                    site.getShell().getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            updateEditControls();
                        }
                    });
                }
            }
        }

        public DBDValueLocator getValueLocator()
        {
            return metaColumns[columnIndex].getValueLocator();
        }

        public DBDValueHandler getValueHandler()
        {
            return metaColumns[columnIndex].getValueHandler();
        }

        public boolean isInlineEdit()
        {
            return inlinePlaceholder != null;
        }

        public boolean isReadOnly()
        {
            return isColumnReadOnly(metaColumns[columnIndex]);
        }

        public IWorkbenchPartSite getValueSite()
        {
            return site;
        }

        public Composite getInlinePlaceholder()
        {
            return inlinePlaceholder;
        }

        public void closeInlineEditor()
        {
            spreadsheet.cancelInlineEditor();
        }

        public void nextInlineEditor(boolean next) {
            spreadsheet.cancelInlineEditor();
            spreadsheet.shiftCursor(next ? 1 : -1, 0, false);
            spreadsheet.openCellViewer(true);
        }

        public void registerEditor(DBDValueEditor editor) {
            openEditors.put(this, editor);
        }

        public void unregisterEditor(DBDValueEditor editor) {
            openEditors.remove(this);
        }

        public void showMessage(String message, boolean error)
        {
            setStatus(message, error);
        }

        public Collection<DBCColumnMetaData> getColumnsMetaData() {
            List<DBCColumnMetaData> columns = new ArrayList<DBCColumnMetaData>();
            for (DBDColumnBinding column : metaColumns) {
                columns.add(column.getColumn());
            }
            return columns;
        }

        public DBCColumnMetaData getColumnMetaData(DBCTableMetaData table, String columnName)
        {
            for (DBDColumnBinding column : metaColumns) {
                if (column.getColumn().getTable() == table && column.getColumn().getName().equals(columnName)) {
                    return column.getColumn();
                }
            }
            return null;
        }

        public Object getColumnValue(DBCColumnMetaData column)
        {
            for (int i = 0; i < metaColumns.length; i++) {
                DBDColumnBinding metaColumn = metaColumns[i];
                if (metaColumn.getColumn() == column) {
                    return curRow[i];
                }
            }
            log.warn("Unknown column value requested: " + column);
            return null;
        }

        private GridPos getCellPos()
        {
            int rowIndex = getRowIndex(curRow);
            if (rowIndex >= 0) {
                return new GridPos(columnIndex, rowIndex);
            } else {
                return null;
            }
        }
    }

    private static class RowInfo implements Comparable<RowInfo> {
        int row;

        RowInfo(int row)
        {
            this.row = row;
        }
        @Override
        public int hashCode()
        {
            return row;
        }
        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof RowInfo && ((RowInfo)obj).row == row;
        }
        @Override
        public String toString()
        {
            return String.valueOf(row);
        }
        public int compareTo(RowInfo o)
        {
            return row - o.row;
        }
    }

    private static class CellInfo {
        int col;
        int row;

        private CellInfo(int col, int row) {
            this.col = col;
            this.row = row;
        }

        @Override
        public int hashCode()
        {
            return col ^ row;
        }

        public boolean equals (Object cell)
        {
            return cell instanceof CellInfo &&
                this.col == ((CellInfo)cell).col &&
                this.row == ((CellInfo)cell).row;
        }
        boolean equals (int col, int row)
        {
            return this.col == col && this.row == row;
        }
    }

    static class TableRowInfo {
        DBSTable table;
        DBCTableIdentifier id;
        List<CellInfo> tableCells = new ArrayList<CellInfo>();

        TableRowInfo(DBSTable table, DBCTableIdentifier id) {
            this.table = table;
            this.id = id;
        }
    }

    static class DataStatementInfo {
        DBSManipulationType type;
        RowInfo row;
        DBSTable table;
        List<DBDColumnValue> keyColumns = new ArrayList<DBDColumnValue>();
        List<DBDColumnValue> updateColumns = new ArrayList<DBDColumnValue>();
        boolean executed = false;
        Map<Integer, Object> updatedCells = new HashMap<Integer, Object>();

        DataStatementInfo(DBSManipulationType type, RowInfo row, DBSTable table)
        {
            this.type = type;
            this.row = row;
            this.table = table;
        }
        boolean hasUpdateColumn(DBDColumnBinding column)
        {
            for (DBDColumnValue col : updateColumns) {
                if (col.getColumn() == column.getTableColumn()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class DataUpdater {

        private final Map<Integer, Map<DBSTable, TableRowInfo>> updatedRows = new TreeMap<Integer, Map<DBSTable, TableRowInfo>>();

        private final List<DataStatementInfo> insertStatements = new ArrayList<DataStatementInfo>();
        private final List<DataStatementInfo> deleteStatements = new ArrayList<DataStatementInfo>();
        private final List<DataStatementInfo> updateStatements = new ArrayList<DataStatementInfo>();

        private DataUpdater()
        {
        }

        /**
         * Applies changes.
         * @throws DBException
         * @param listener
         */
        void applyChanges(DBDValueListener listener)
            throws DBException
        {
            prepareDeleteStatements();
            prepareInsertStatements();
            prepareUpdateStatements();
            execute(listener);
        }

        private void prepareUpdateRows()
            throws DBException
        {
            if (editedValues == null) {
                return;
            }
            // Prepare rows
            for (CellInfo cell : editedValues.keySet()) {
                Map<DBSTable, TableRowInfo> tableMap = updatedRows.get(cell.row);
                if (tableMap == null) {
                    tableMap = new HashMap<DBSTable, TableRowInfo>();
                    updatedRows.put(cell.row, tableMap);
                }

                DBDColumnBinding metaColumn = metaColumns[cell.col];
                DBSTable metaTable = metaColumn.getValueLocator().getTable();
                TableRowInfo tableRowInfo = tableMap.get(metaTable);
                if (tableRowInfo == null) {
                    tableRowInfo = new TableRowInfo(metaTable, metaColumn.getValueLocator().getTableIdentifier());
                    tableMap.put(metaTable, tableRowInfo);
                }
                tableRowInfo.tableCells.add(cell);
            }
        }

        private void prepareDeleteStatements()
            throws DBException
        {
            if (removedRows == null) {
                return;
            }
            // Make delete statements
            for (RowInfo rowNum : removedRows) {
                DBSTable table = metaColumns[0].getValueLocator().getTable();
                DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.DELETE, rowNum, table);
                List<? extends DBSTableColumn> keyColumns = metaColumns[0].getValueLocator().getTableColumns();
                for (DBSTableColumn column : keyColumns) {
                    int colIndex = getMetaColumnIndex(column);
                    if (colIndex < 0) {
                        throw new DBCException("Can't find meta column for ID column " + column.getName());
                    }
                    statement.keyColumns.add(new DBDColumnValue(column, curRows.get(rowNum.row)[colIndex]));
                }
                deleteStatements.add(statement);
            }
        }

        private void prepareInsertStatements()
            throws DBException
        {
            if (addedRows == null) {
                return;
            }
            // Make insert statements
            for (RowInfo rowNum : addedRows) {
                Object[] cellValues = curRows.get(rowNum.row);
                DBSTable table = metaColumns[0].getValueLocator().getTable();
                DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.INSERT, rowNum, table);
                for (int i = 0; i < metaColumns.length; i++) {
                    DBDColumnBinding column = metaColumns[i];
                    statement.keyColumns.add(new DBDColumnValue(column.getTableColumn(), cellValues[i]));
                }
                insertStatements.add(statement);
            }
        }

        private void prepareUpdateStatements()
            throws DBException
        {
            prepareUpdateRows();

            if (updatedRows == null) {
                return;
            }

            // Make statements
            for (Integer rowNum : updatedRows.keySet()) {
                Map<DBSTable, TableRowInfo> tableMap = updatedRows.get(rowNum);
                for (DBSTable table : tableMap.keySet()) {
                    TableRowInfo rowInfo = tableMap.get(table);
                    DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.UPDATE, new RowInfo(rowNum), table);
                    // Updated columns
                    for (int i = 0; i < rowInfo.tableCells.size(); i++) {
                        CellInfo cell = rowInfo.tableCells.get(i);
                        DBDColumnBinding metaColumn = metaColumns[cell.col];
                        statement.updateColumns.add(new DBDColumnValue(metaColumn.getTableColumn(), curRows.get(rowNum)[cell.col]));
                    }
                    // Key columns
                    Collection<? extends DBCColumnMetaData> idColumns = rowInfo.id.getResultSetColumns();
                    for (DBCColumnMetaData idColumn : idColumns) {
                        // Find meta column and add statement parameter
                        int columnIndex = getMetaColumnIndex(idColumn);
                        if (columnIndex < 0) {
                            throw new DBCException("Can't find meta column for ID column " + idColumn.getName());
                        }
                        DBDColumnBinding metaColumn = metaColumns[columnIndex];
                        Object keyValue = curRows.get(rowNum)[columnIndex];
                        // Try to find old key oldValue
                        for (CellInfo cell : editedValues.keySet()) {
                            if (cell.equals(columnIndex, rowNum)) {
                                keyValue = editedValues.get(cell);
                            }
                        }
                        statement.keyColumns.add(new DBDColumnValue(metaColumn.getTableColumn(), keyValue));
                    }
                    updateStatements.add(statement);
                }
            }
        }

        private void execute(final DBDValueListener listener)
            throws DBException
        {
            DataUpdaterJob job = new DataUpdaterJob(listener);
            job.schedule();
        }

        public void rejectChanges()
        {
            if (editedValues != null) {
                for (CellInfo cell : editedValues.keySet()) {
                    Object[] row = ResultSetViewer.this.curRows.get(cell.row);
                    releaseValue(row[cell.col]);
                    row[cell.col] = editedValues.get(cell);
                }
                editedValues.clear();
            }
            boolean rowsChanged = deleteRows(addedRows);
            // Remove deleted rows
            if (removedRows != null) {
                removedRows.clear();
            }

            refreshSpreadsheet(rowsChanged);
        }

        // Reflect data changes in viewer
        // Changes affects only rows which statements executed successfully
        private boolean reflectChanges()
        {
            boolean rowsChanged = false;
            if (editedValues != null) {
                for (Iterator<Map.Entry<CellInfo, Object>> iter = editedValues.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry<CellInfo, Object> entry = iter.next();
                    for (DataStatementInfo stat : updateStatements) {
                        if (stat.executed && stat.row.row == entry.getKey().row && stat.hasUpdateColumn(metaColumns[entry.getKey().col])) {
                            reflectKeysUpdate(stat);
                            iter.remove();
                            break;
                        }
                    }
                }
                editedValues.clear();
            }
            if (addedRows != null) {
                for (Iterator<RowInfo> iter = addedRows.iterator(); iter.hasNext(); ) {
                    RowInfo row = iter.next();
                    for (DataStatementInfo stat : insertStatements) {
                        if (stat.executed && stat.row.equals(row)) {
                            reflectKeysUpdate(stat);
                            iter.remove();
                            break;
                        }
                    }
                }
            }
            if (removedRows != null) {
                for (Iterator<RowInfo> iter = removedRows.iterator(); iter.hasNext(); ) {
                    RowInfo row = iter.next();
                    for (DataStatementInfo stat : deleteStatements) {
                        if (stat.executed && stat.row.equals(row)) {
                            deleteRow(row.row);
                            iter.remove();
                            rowsChanged = true;
                            break;
                        }
                    }
                }
            }
            return rowsChanged;
        }

        private void reflectKeysUpdate(DataStatementInfo stat)
        {
            // Update keys
            if (!stat.updatedCells.isEmpty()) {
                for (Map.Entry<Integer, Object> entry : stat.updatedCells.entrySet()) {
                    Object[] row = ResultSetViewer.this.curRows.get(stat.row.row);
                    releaseValue(row[entry.getKey()]);
                    row[entry.getKey()] = entry.getValue();
                }
            }
        }

/*
        private void releaseStatements()
        {
            for (DataStatementInfo stat : updateStatements) releaseStatement(stat);
            for (DataStatementInfo stat : insertStatements) releaseStatement(stat);
            for (DataStatementInfo stat : deleteStatements) releaseStatement(stat);
        }

        private void releaseStatement(DataStatementInfo stat)
        {
            for (DBDColumnValue value : stat.keyColumns) releaseValue(value.getValue());
            for (DBDColumnValue value : stat.updateColumns) releaseValue(value.getValue());
        }

*/
        private class DataUpdaterJob extends DataSourceJob {
            private final DBDValueListener listener;
            private boolean autocommit;
            private int updateCount = 0, insertCount = 0, deleteCount = 0;
            private DBCSavepoint savepoint;

            protected DataUpdaterJob(DBDValueListener listener)
            {
                super("Update data", DBIcon.SQL_EXECUTE.getImageDescriptor(), resultSetProvider.getDataSource());
                this.listener = listener;
            }

            protected IStatus run(DBRProgressMonitor monitor)
            {
                ResultSetViewer.this.updateInProgress = true;
                try {
                    final Throwable error = executeStatements(monitor);

                    if (this.listener != null) {
                        this.listener.onUpdate(error == null);
                    }

                    ResultSetViewer.this.site.getShell().getDisplay().syncExec(new Runnable() {
                        public void run()
                        {
                            boolean rowsChanged = false;
                            if (DataUpdaterJob.this.autocommit || error == null) {
                                rowsChanged = reflectChanges();
                            }
                            //releaseStatements();
                            refreshSpreadsheet(rowsChanged);
                            if (error == null) {
                                setStatus(
                                    "Instered: " + DataUpdaterJob.this.insertCount +
                                    " / Deleted: " + DataUpdaterJob.this.deleteCount +
                                    " / Updated: " + DataUpdaterJob.this.updateCount, false);
                            } else {
                                DBeaverUtils.showErrorDialog(ResultSetViewer.this.site.getShell(), "Data error", "Error synchronizing data with database", error);
                                setStatus(error.getMessage(), true);
                            }
                        }
                    });
                }
                finally {
                    ResultSetViewer.this.updateInProgress = false;
                }
                return Status.OK_STATUS;
            }

            private Throwable executeStatements(DBRProgressMonitor monitor)
            {
                DBCExecutionContext context = getDataSource().openContext(monitor);
                try {
                    try {
                        this.autocommit = context.getTransactionManager().isAutoCommit();
                    }
                    catch (DBCException e) {
                        log.warn("Could not determine autocommit state", e);
                        this.autocommit = true;
                    }
                    if (!this.autocommit && context.getTransactionManager().supportsSavepoints()) {
                        try {
                            this.savepoint = context.getTransactionManager().setSavepoint(null);
                        }
                        catch (Throwable e) {
                            // May be savepoints not supported
                            log.debug("Could not set savepoint", e);
                        }
                    }
                    try {
                        monitor.beginTask("Apply resultset changes", DataUpdater.this.deleteStatements.size() + DataUpdater.this.insertStatements.size() + DataUpdater.this.updateStatements.size());

                        for (DataStatementInfo statement : DataUpdater.this.deleteStatements) {
                            if (monitor.isCanceled()) break;
                            DBSDataContainer dataContainer = (DBSDataContainer)statement.table;
                            try {
                                deleteCount += dataContainer.deleteData(context, statement.keyColumns);
                                processStatementChanges(statement);
                            }
                            catch (DBException e) {
                                processStatementError(statement, context);
                                return e;
                            }
                            monitor.worked(1);
                        }
                        for (DataStatementInfo statement : DataUpdater.this.insertStatements) {
                            if (monitor.isCanceled()) break;
                            DBSDataContainer dataContainer = (DBSDataContainer)statement.table;
                            try {
                                insertCount += dataContainer.insertData(context, statement.keyColumns, new KeyDataReceiver(statement));
                                processStatementChanges(statement);
                            }
                            catch (DBException e) {
                                processStatementError(statement, context);
                                return e;
                            }
                            monitor.worked(1);
                        }
                        for (DataStatementInfo statement : DataUpdater.this.updateStatements) {
                            if (monitor.isCanceled()) break;
                            DBSDataContainer dataContainer = (DBSDataContainer)statement.table;
                            try {
                                this.updateCount += dataContainer.updateData(context, statement.keyColumns, statement.updateColumns, new KeyDataReceiver(statement));
                                processStatementChanges(statement);
                            }
                            catch (DBException e) {
                                processStatementError(statement, context);
                                return e;
                            }
                            monitor.worked(1);
                        }

                        return null;
                    }
                    finally {
                        if (this.savepoint != null) {
                            try {
                                context.getTransactionManager().releaseSavepoint(this.savepoint);
                            }
                            catch (Throwable e) {
                                // May be savepoints not supported
                                log.debug("Could not release savepoint", e);
                            }
                        }
                    }
                }
                finally {
                    monitor.done();
                    context.close();
                }
            }

            private void processStatementChanges(DataStatementInfo statement)
            {
                statement.executed = true;
            }

            private void processStatementError(DataStatementInfo statement, DBCExecutionContext context)
            {
                statement.executed = false;
                try {
                    context.getTransactionManager().rollback(savepoint);
                }
                catch (Throwable e) {
                    log.debug("Error during transaction rollback", e);
                }
            }

        }
    }

    private class KeyDataReceiver implements DBDDataReceiver {
        DataStatementInfo statement;
        public KeyDataReceiver(DataStatementInfo statement)
        {
            this.statement = statement;
        }

        public void fetchStart(DBRProgressMonitor monitor, DBCResultSet resultSet)
            throws DBCException
        {

        }

        public void fetchRow(DBRProgressMonitor monitor, DBCResultSet resultSet)
            throws DBCException
        {
            DBCResultSetMetaData rsMeta = resultSet.getResultSetMetaData();
            List<DBCColumnMetaData> keyColumns = rsMeta.getColumns();
            for (int i = 0; i < keyColumns.size(); i++) {
                DBCColumnMetaData keyColumn = keyColumns.get(i);
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(resultSetProvider.getDataSource(), keyColumn);
                Object keyValue = valueHandler.getValueObject(monitor, resultSet, keyColumn, i);
                boolean updated = false;
                if (!CommonUtils.isEmpty(keyColumn.getName())) {
                    int colIndex = getMetaColumnIndex(statement.table, keyColumn.getName());
                    if (colIndex >= 0) {
                        // Got it. Just update column oldValue
                        statement.updatedCells.put(colIndex, keyValue);
                        //curRows.get(statement.row.row)[colIndex] = keyValue;
                        updated = true;
                    }
                }
                if (!updated) {
                    // Key not found
                    // Try to find and update auto-increment column
                    for (int k = 0; k < metaColumns.length; k++) {
                        DBDColumnBinding column = metaColumns[k];
                        if (column.getTableColumn().isAutoIncrement()) {
                            // Got it
                            statement.updatedCells.put(k, keyValue);
                            //curRows.get(statement.row.row)[k] = keyValue;
                            updated = true;
                            break;
                        }
                    }
                }

                if (!updated) {
                    // Auto-generated key not found
                    // Just skip it..
                    log.debug("Could not find target column for autogenerated key '" + keyColumn.getName() + "'");
                }
            }
        }

        public void fetchEnd(DBRProgressMonitor monitor)
            throws DBCException
        {

        }
    }

    private class ContentProvider implements IGridContentProvider {

        public GridPos getSize()
        {
            if (mode == ResultSetMode.RECORD) {
                return new GridPos(
                    1,
                    metaColumns == null ? 0 : metaColumns.length);
            } else {
                return new GridPos(
                    metaColumns == null ? 0 : metaColumns.length,
                    curRows.size());
            }
        }

        public Object[] getElements(Object inputElement)
        {
            int rowNum = ((Number) inputElement).intValue();
            return curRows.get(rowNum);
        }

        public void dispose()
        {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }
    }

    private class ContentLabelProvider extends LabelProvider implements IColorProvider {
        private Object getValue(Object element, boolean formatString)
        {
            GridPos cell = (GridPos)element;
            Object value;
            DBDValueHandler valueHandler;
            int rowNum;
            if (mode == ResultSetMode.RECORD) {
                // Fill record
                rowNum = curRowNum;
                if (curRowNum >= curRows.size() || curRowNum < 0) {
                    //log.warn("Bad current row number: " + curRowNum);
                    return "";
                }
                Object[] values = curRows.get(curRowNum);
                if (cell.row >= values.length) {
                    log.warn("Bad record row number: " + cell.row);
                    return null;
                }
                value = values[cell.row];
                valueHandler = metaColumns[cell.row].getValueHandler();
            } else {
                rowNum = cell.row;
                if (cell.row >= curRows.size()) {
                    log.warn("Bad grid row number: " + cell.row);
                    return null;
                }
                if (cell.col >= metaColumns.length) {
                    log.warn("Bad grid column number: " + cell.col);
                    return null;
                }
                value = curRows.get(cell.row)[cell.col];
                valueHandler = metaColumns[cell.col].getValueHandler();
            }

            if (dataReciever.isHasMoreData() && rowNum == curRows.size() - 1) {
                readNextSegment();
            }

            if (formatString) {
                return valueHandler.getValueDisplayString(metaColumns[cell.col].getColumn(), value);
            } else {
                return value;
            }
        }

        @Override
        public Image getImage(Object element)
        {
            return null;
        }

        @Override
        public String getText(Object element)
        {
            return String.valueOf(getValue(element, true));
        }

        public Color getForeground(Object element)
        {
            Object value = getValue(element, false);
            if (DBUtils.isNullValue(value)) {
                return foregroundNull;
            } else {
                return null;
            }
        }

        public Color getBackground(Object element)
        {
            GridPos cell = (GridPos)element;
            int col = cell.col;
            int row = cell.row;
            if (mode == ResultSetMode.RECORD) {
                col = row;
                row = curRowNum;
            }
            if (isRowAdded(row)) {
                return backgroundAdded;
            }
            if (isRowDeleted(row)) {
                return backgroundDeleted;
            }
            if (isCellModified(col, row)) {
                return backgroundModified;
            }
            return null;
        }
    }

    private class ColumnLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element)
        {
            if (mode == ResultSetMode.GRID) {
                int colNumber = ((Number)element).intValue();
                return getColumnImage(metaColumns[colNumber]);
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int colNumber = ((Number)element).intValue();
            if (mode == ResultSetMode.RECORD) {
                if (colNumber == 0) {
                    return "Value";
                } else {
                    log.warn("Bad column index: " + colNumber);
                    return null;
                }
            } else {
                DBDColumnBinding metaColumn = metaColumns[colNumber];
                return metaColumn.getColumn().getLabel();
/*
                return CommonUtils.isEmpty(metaColumn.getMetaData().getTableName()) ?
                    metaColumn.getMetaData().getName() :
                    metaColumn.getMetaData().getTableName() + "." + metaColumn.getMetaData().getName();
*/
            }
        }
    }

    private class RowLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element)
        {
            if (mode == ResultSetMode.RECORD) {
                int rowNumber = ((Number) element).intValue();
                return getColumnImage(metaColumns[rowNumber]);
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int rowNumber = ((Number) element).intValue();
            if (mode == ResultSetMode.RECORD) {
                return metaColumns[rowNumber].getColumn().getName();
            } else {
                return String.valueOf(rowNumber + 1);
            }
        }
    }
}
