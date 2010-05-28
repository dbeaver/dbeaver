/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
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
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.dbc.*;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.runtime.sql.*;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ThemeConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.spreadsheet.ISpreadsheetController;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;

import java.util.*;
import java.util.List;

/**
 * ResultSetViewer
 */
public class ResultSetViewer extends Viewer implements ISpreadsheetController, IPropertyChangeListener
{
    static Log log = LogFactory.getLog(ResultSetViewer.class);

    private static final int DEFAULT_ROW_HEADER_WIDTH = 50;

    private IWorkbenchPartSite site;
    private ResultSetMode mode;
    private Spreadsheet spreadsheet;
    private ResultSetProvider resultSetProvider;
    private ResultSetDataPump dataPump;
    private IThemeManager themeManager;

    // columns
    private ResultSetColumn[] metaColumns;
    // Data
    private final List<Object[]> curRows = new ArrayList<Object[]>();
    // Current row number (for record mode)
    private int curRowNum = -1;

    private Label statusLabel;
    private Color colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    private Color colorBlack = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);

    private ToolItem itemAccept;
    private ToolItem itemReject;

    private ToolItem itemToggleView;
    private ToolItem itemNext;
    private ToolItem itemPrevious;
    private ToolItem itemFirst;
    private ToolItem itemLast;
    private ToolItem itemRefresh;

    // Edited cells
    private Set<CellInfo> editedValues = new HashSet<CellInfo>();
    private Map<ResultSetValueController, DBDValueEditor> openEditors = new HashMap<ResultSetValueController, DBDValueEditor>();
    // Flag saying that edited values update is in progress
    private boolean updateInProgress = false;

    // UI modifiers
    private Color backgroundModified;
    private Color foregroundNull;

    public ResultSetViewer(Composite parent, IWorkbenchPartSite site, ResultSetProvider resultSetProvider)
    {
        super();
        this.site = site;
        this.mode = ResultSetMode.GRID;
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
        this.dataPump = new ResultSetDataPump(this);

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
        backgroundModified = new Color(spreadsheet.getDisplay(), 0xFF, 0xE4, 0xB5);
        foregroundNull = spreadsheet.getDisplay().getSystemColor(SWT.COLOR_GRAY);

        applyThemeSettings();
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
            itemRefresh.setEnabled(resultSetProvider != null && resultSetProvider.isConnected());
        }
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
        boolean hasChanges = !editedValues.isEmpty();
        itemAccept.setEnabled(hasChanges);
        itemReject.setEnabled(hasChanges);
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
                for (ResultSetColumn column : metaColumns) {
                    Point ext = gc.stringExtent(column.metaData.getColumnName());
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

    public ResultSetProvider getResultSetProvider()
    {
        return resultSetProvider;
    }

    public ResultSetDataPump getDataPump()
    {
        return dataPump;
    }

    public void dispose()
    {
        closeEditors();

        if (!spreadsheet.isDisposed()) {
            spreadsheet.dispose();
        }
        itemAccept.dispose();
        itemReject.dispose();
        itemToggleView.dispose();
        itemNext.dispose();
        itemPrevious.dispose();
        itemFirst.dispose();
        itemLast.dispose();
        itemRefresh.dispose();
        statusLabel.dispose();
        themeManager.removePropertyChangeListener(ResultSetViewer.this);
        colorRed.dispose();
        colorBlack.dispose();
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
            statusLabel.setForeground(colorBlack);
        }
        statusLabel.setText(status);
    }

    public void setColumnsInfo(ResultSetColumn[] metaColumns)
    {
        this.metaColumns = metaColumns;
    }

    public void setData(List<Object[]> rows)
    {
        this.curRows.clear();
        this.curRows.addAll(rows);
        this.initResultSet();
    }

    public void appendData(List<Object[]> rows)
    {
        curRows.addAll(rows);
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
        this.editedValues.clear();

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
                editedValues.contains(new CellInfo(col, row, null));
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
        ResultSetColumn metaColumn = metaColumns[columnIndex];
        boolean readOnly = !metaColumn.editable;
        if (readOnly && inline) {
            // No inline editors for readonly columns
            return false;
        }
        ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(rowIndex),
            columnIndex,
            inline ? inlinePlaceholder : null);
        try {
            return metaColumn.valueHandler.editValue(valueController);
        }
        catch (Exception e) {
            log.error(e);
            return false;
        }
    }

    public void fillContextMenu(GridPos cell, IMenuManager manager) {
        final int columnIndex = (mode == ResultSetMode.GRID ? cell.col : cell.row);
        final int rowIndex = (mode == ResultSetMode.GRID ? cell.row : curRowNum);
        ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(rowIndex),
            columnIndex,
            null);
        try {
            metaColumns[columnIndex].valueHandler.fillContextMenu(manager, valueController);
        }
        catch (Exception e) {
            log.error(e);
        }
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

    public ISelection getSelection()
    {
        return new StructuredSelection(spreadsheet.getSelection().toArray());
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public void refresh()
    {
        // Refresh all rows
        this.editedValues.clear();

        this.curRows.clear();
        this.clearResultsView();
        if (resultSetProvider != null) {
            resultSetProvider.extractResultSetData(0);
        }
        updateGridCursor();
    }

    private void applyChanges()
    {
        try {
            new CellDataSaver().applyChanges();
        } catch (DBException e) {
            log.error("Could not obtain result set metdata", e);
        }
    }

    private void rejectChanges()
    {
        new CellDataSaver().rejectChanges();
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

        public DBCSession getSession()
            throws DBException
        {
            return resultSetProvider.getSession();
        }

        public DBDRowController getRow() {
            return this;
        }

        public DBCColumnMetaData getColumnMetaData()
        {
            return metaColumns[columnIndex].metaData;
        }

        public String getColumnId() {
            String dsName;
            try {
                dsName = getSession().getDataSource().getContainer().getName();
            } catch (DBException e) {
                dsName = "datasource";
                log.warn(e);
            }
            String catalogName = getColumnMetaData().getCatalogName();
            String schemaName = getColumnMetaData().getSchemaName();
            String tableName = getColumnMetaData().getTableName();
            String columnName = getColumnMetaData().getColumnName();
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

        public void updateValue(Object value, boolean immediate)
        {
            Object oldValue = curRow[columnIndex];
            if (value instanceof DBDValue || !CommonUtils.equalObjects(oldValue, value)) {
                int rowIndex = getRowIndex(curRow);
                if (rowIndex >= 0) {
                    CellInfo cell = new CellInfo(columnIndex, rowIndex, oldValue);
                    editedValues.add(cell);
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
            return metaColumns[columnIndex].valueLocator;
        }

        public DBDValueHandler getValueHandler()
        {
            return metaColumns[columnIndex].valueHandler;
        }

        public boolean isInlineEdit()
        {
            return inlinePlaceholder != null;
        }

        public boolean isReadOnly()
        {
            return !metaColumns[columnIndex].editable;
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
            for (ResultSetColumn column : metaColumns) {
                columns.add(column.metaData);
            }
            return columns;
        }

        public Object getColumnValue(DBCColumnMetaData column)
        {
            for (int i = 0; i < metaColumns.length; i++) {
                ResultSetColumn metaColumn = metaColumns[i];
                if (metaColumn.metaData == column) {
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

    private static class CellInfo {
        int col;
        int row;
        Object value;

        private CellInfo(int col, int row, Object value) {
            this.col = col;
            this.row = row;
            this.value = value;
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

    class TableRowInfo {
        DBSTable table;
        DBCTableIdentifier id;
        List<CellInfo> tableCells = new ArrayList<CellInfo>();

        TableRowInfo(DBSTable table, DBCTableIdentifier id) {
            this.table = table;
            this.id = id;
        }
    }

    private class CellDataSaver {

        private Map<Integer, Map<DBSTable, TableRowInfo>> updatedRows = new TreeMap<Integer, Map<DBSTable, TableRowInfo>>();
        private List<SQLStatementInfo> statements = new ArrayList<SQLStatementInfo>();

        void applyChanges()
            throws DBException
        {
            prepareRows();
            prepareStatements();
            execute();
        }

        private void prepareRows()
            throws DBException
        {
            // Prepare rows
            for (CellInfo cell : editedValues) {
                Map<DBSTable, TableRowInfo> tableMap = updatedRows.get(cell.row);
                if (tableMap == null) {
                    tableMap = new HashMap<DBSTable, TableRowInfo>();
                    updatedRows.put(cell.row, tableMap);
                }

                DBCTableMetaData metaTable = metaColumns[cell.col].metaData.getTable();
                TableRowInfo tableRowInfo = tableMap.get(metaTable.getTable());
                if (tableRowInfo == null) {
                    tableRowInfo = new TableRowInfo(metaTable.getTable(), metaTable.getBestIdentifier());
                    tableMap.put(metaTable.getTable(), tableRowInfo);
                }
                tableRowInfo.tableCells.add(cell);
            }
        }

        private void prepareStatements()
            throws DBException
        {
            // Make statements
            for (Integer rowNum : updatedRows.keySet()) {
                Map<DBSTable, TableRowInfo> tableMap = updatedRows.get(rowNum);
                for (DBSTable table : tableMap.keySet()) {
                    TableRowInfo rowInfo = tableMap.get(table);

                    String tableName = rowInfo.table.getFullQualifiedName();
                    List<SQLStatementParameter> parameters = new ArrayList<SQLStatementParameter>();
                    StringBuilder query = new StringBuilder();
                    query.append("UPDATE ").append(tableName).append(" SET ");
                    for (int i = 0; i < rowInfo.tableCells.size(); i++) {
                        CellInfo cell = rowInfo.tableCells.get(i);
                        if (i > 0) {
                            query.append(",");
                        }
                        ResultSetColumn metaColumn = metaColumns[cell.col];
                        query.append(metaColumn.metaData.getTableColumn().getName()).append("=?");
                        parameters.add(new SQLStatementParameter(
                            metaColumn.valueHandler,
                            metaColumn.metaData,
                            parameters.size(),
                            curRows.get(rowNum)[cell.col]));
                    }
                    query.append(" WHERE ");
                    Collection<? extends DBSConstraintColumn> idColumns = rowInfo.id.getConstraint().getColumns();
                    boolean firstCol = true;
                    for (DBSConstraintColumn idColumn : idColumns) {
                        if (!firstCol) {
                            query.append(" AND ");
                        }
                        query.append(idColumn.getName()).append("=?");
                        firstCol = false;
                        // Find meta column and add statement parameter
                        ResultSetColumn metaColumn = null;
                        int columnIndex = -1;
                        for (int i = 0; i < metaColumns.length; i++) {
                            ResultSetColumn tmpColumn = metaColumns[i];
                            if (idColumn.getTableColumn() == tmpColumn.metaData.getTableColumn()) {
                                metaColumn = tmpColumn;
                                columnIndex = i;
                                break;
                            }
                        }
                        if (metaColumn == null) {
                            throw new DBCException("Can't find meta column for ID column " + idColumn.getName());
                        }
                        Object keyValue = curRows.get(rowNum)[columnIndex];
                        // Try to find old key value
                        for (CellInfo cell : editedValues) {
                            if (cell.equals(columnIndex, rowNum)) {
                                keyValue = cell.value;
                            }
                        }
                        // Add key parameter
                        parameters.add(new SQLStatementParameter(
                            metaColumn.valueHandler,
                            metaColumn.metaData,
                            parameters.size(),
                            keyValue));
                    }

                    SQLStatementInfo statement = new SQLStatementInfo(query.toString(), parameters);
                    statement.setOffset(rowNum);
                    statement.setData(rowInfo);
                    statements.add(statement);
                }
            }
        }

        private void execute()
            throws DBException
        {
            // Execute statements
            SQLQueryJob executor = new SQLQueryJob(
                "Update ResultSet",
                resultSetProvider.getSession(),
                statements,
                null);
            executor.addQueryListener(new DefaultQueryListener() {
                @Override
                public void onStartJob() {
                    updateInProgress = true;
                }

                @Override
                public void onEndQuery(SQLQueryResult result) {
                    if (result.getError() == null) {
                        // Remove edited values
                        TableRowInfo rowInfo = (TableRowInfo)result.getStatement().getData();
                        if (rowInfo != null) {
                            for (CellInfo cell : rowInfo.tableCells) {
                                editedValues.remove(cell);
                            }
                        }
                    }
                }

                @Override
                public void onEndJob(final boolean hasErrors) {
                    site.getShell().getDisplay().asyncExec(new Runnable() {
                        public void run()
                        {
                            spreadsheet.redrawGrid();
                            updateEditControls();
                            updateInProgress = false;
                        }
                    });
                }
            });
            executor.schedule();
        }

        public void rejectChanges()
        {
            for (CellInfo cell : editedValues) {
                curRows.get(cell.row)[cell.col] = cell.value;
            }
            editedValues.clear();
            spreadsheet.redrawGrid();
            updateEditControls();
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
            return curRows.get(((Number)inputElement).intValue());
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
            if (mode == ResultSetMode.RECORD) {
                // Fill record
                if (curRowNum >= curRows.size() || curRowNum < 0) {
                    log.warn("Bad current row number: " + curRowNum);
                    return null;
                }
                Object[] values = curRows.get(curRowNum);
                if (cell.row >= values.length) {
                    log.warn("Bad record row number: " + cell.row);
                    return null;
                }
                value = values[cell.row];
                valueHandler = metaColumns[cell.row].valueHandler;
            } else {
                if (cell.row >= curRows.size()) {
                    log.warn("Bad grid row number: " + cell.row);
                    return null;
                }
                if (cell.col >= metaColumns.length) {
                    log.warn("Bad grid column number: " + cell.col);
                    return null;
                }
                value = curRows.get(cell.row)[cell.col];
                valueHandler = metaColumns[cell.col].valueHandler;
            }
            if (formatString) {
                return valueHandler.getValueDisplayString(value);
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
            String text = String.valueOf(getValue(element, true));
            if (text == null) {
                return "null";
            }
            return text;
        }

        public Color getForeground(Object element)
        {
            Object value = getValue(element, false);
            if (value == null) {
                return foregroundNull;
            } else if (value instanceof DBDValue && ((DBDValue)value).isNull()) {
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
                ResultSetColumn metaColumn = metaColumns[colNumber];
                if (metaColumn.editable) {
                    return DBIcon.EDIT_COLUMN.getImage();
                }
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
                ResultSetColumn metaColumn = metaColumns[colNumber];
                return metaColumn.metaData.getLabel();
/*
                return CommonUtils.isEmpty(metaColumn.metaData.getTableName()) ?
                    metaColumn.metaData.getColumnName() :
                    metaColumn.metaData.getTableName() + "." + metaColumn.metaData.getColumnName();
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
                if (metaColumns[rowNumber].editable) {
                    return DBIcon.EDIT_COLUMN.getImage();
                }
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int rowNumber = ((Number) element).intValue();
            if (mode == ResultSetMode.RECORD) {
                return metaColumns[rowNumber].metaData.getColumnName();
            } else {
                return String.valueOf(rowNumber + 1);
            }
        }
    }
}
