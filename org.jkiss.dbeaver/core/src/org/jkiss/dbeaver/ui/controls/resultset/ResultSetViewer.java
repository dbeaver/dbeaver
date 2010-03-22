package org.jkiss.dbeaver.ui.controls.resultset;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.dbc.DBCLOB;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.ui.ThemeConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.grid.GridControl;
import org.jkiss.dbeaver.ui.controls.grid.IGridDataProvider;
import org.jkiss.dbeaver.ui.controls.grid.IGridRow;
import org.jkiss.dbeaver.ui.controls.grid.GridPos;
import org.jkiss.dbeaver.ui.controls.resultset.view.TextViewDialog;
import org.jkiss.dbeaver.ui.controls.resultset.view.ValueViewDialog;
import org.jkiss.dbeaver.ui.controls.resultset.view.LobViewDialog;
import org.jkiss.dbeaver.DBException;

import java.util.ArrayList;
import java.util.List;

/**
 * ResultSetViewer
 */
public class ResultSetViewer extends Viewer implements IGridDataProvider, IPropertyChangeListener
{
    static Log log = LogFactory.getLog(ResultSetViewer.class);

    private ResultSetMode mode;
    private GridControl grid;
    private ResultSetProvider resultSetProvider;
    private IThemeManager themeManager;

    private DBCResultSetMetaData metaData;
    private List<Object[]> curRows = new ArrayList<Object[]>();
    private GridPos curRowNum = null;

    private Label statusLabel;
    private Color colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
    private Color colorBlack = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);

    private ToolItem itemToggleView;
    private ToolItem itemNext;
    private ToolItem itemPrevious;
    private ToolItem itemFirst;
    private ToolItem itemLast;
    private ToolItem itemRefresh;

    public ResultSetViewer(Composite parent, IWorkbenchPartSite site)
    {
        super();
        mode = ResultSetMode.GRID;
        grid = new GridControl(
            parent,
            SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
            site,
            this);

        createStatusBar(grid);

        themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
        themeManager.addPropertyChangeListener(this);
        grid.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        grid.addCursorChangeListener(new Listener() {
            public void handleEvent(Event event)
            {
                onChangeGridCursor(event.x, event.y);
            }
        });
        applyThemeSettings();

        updateGridCursor();
    }

    private void updateGridCursor()
    {
        Point point = grid.getCursorPosition();
        if (point == null) {
            onChangeGridCursor(0, 0);
        } else {
            onChangeGridCursor(point.x, point.y);
        }
    }

    private void onChangeGridCursor(int col, int row)
    {
        if (mode == ResultSetMode.GRID) {
            int rowsNum = grid.getItemCount();
            //int colsNum = grid.getColumnsCount();
            boolean isFirst = (row <= 0);
            boolean isLast = rowsNum == 0 || (row >= rowsNum - 1);

            itemFirst.setEnabled(!isFirst);
            itemPrevious.setEnabled(!isFirst);
            itemNext.setEnabled(!isLast);
            itemLast.setEnabled(!isLast);
            itemRefresh.setEnabled(resultSetProvider != null && resultSetProvider.isConnected());
        }
    }

    private void updateRecord()
    {
        boolean isFirst = curRowNum.row == 0;
        boolean isLast = curRows.isEmpty() || (curRowNum.row >= curRows.size() - 1);

        itemFirst.setEnabled(!isFirst);
        itemPrevious.setEnabled(!isFirst);
        itemNext.setEnabled(!isLast);
        itemLast.setEnabled(!isLast);

        this.initResultSet();
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
                        curRowNum = new GridPos(curRowNum.col, 0);
                        updateRecord();
                    } else {
                        Point position = grid.getCursorPosition();
                        grid.moveCursor(position.x, 0, false);
                    }
                }
            });
            itemPrevious = UIUtils.createToolItem(toolBar, "Previous", DBIcon.RS_PREV, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && curRowNum.row > 0) {
                        curRowNum = new GridPos(curRowNum.col, curRowNum.row - 1);
                        updateRecord();
                    } else {
                        Point position = grid.getCursorPosition();
                        grid.moveCursor(position.x, position.y - 1, false);
                    }
                }
            });
            itemNext = UIUtils.createToolItem(toolBar, "Next", DBIcon.RS_NEXT, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && curRowNum.row < curRows.size() - 1) {
                        curRowNum = new GridPos(curRowNum.col, curRowNum.row + 1);
                        updateRecord();
                    } else {
                        Point position = grid.getCursorPosition();
                        grid.moveCursor(position.x, position.y + 1, false);
                    }
                }
            });
            itemLast = UIUtils.createToolItem(toolBar, "Last", DBIcon.RS_LAST, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    if (mode == ResultSetMode.RECORD && !curRows.isEmpty()) {
                        curRowNum = new GridPos(curRowNum.col, curRows.size() - 1);
                        updateRecord();
                    } else {
                        Point position = grid.getCursorPosition();
                        grid.moveCursor(position.x, grid.getItemCount() - 1, false);
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
    }

    private void changeMode(ResultSetMode resultSetMode)
    {
        this.mode = resultSetMode;
        if (mode == ResultSetMode.GRID) {
            grid.setPanelWidth(30);
            itemToggleView.setImage(DBIcon.RS_MODE_GRID.getImage());
        } else {
            // Calculate width of grid panel - use longest column title
            int defaultWidth = 0;
            if (metaData != null) {
                GC gc = new GC(grid);
                gc.setFont(grid.getFont());
                for (DBCColumnMetaData column : metaData.getColumns()) {
                    Point ext = gc.stringExtent(column.getName());
                    if (ext.x > defaultWidth) {
                        defaultWidth = ext.x;
                    }
                }
            }
            grid.setPanelWidth(defaultWidth + 30);
            itemToggleView.setImage(DBIcon.RS_MODE_RECORD.getImage());
            curRowNum = grid.getCurrentPos();
            if (curRowNum == null) {
                curRowNum = new GridPos(0, 0);
            }
        }

        this.initResultSet();

        if (mode == ResultSetMode.GRID) {
            grid.moveCursor(curRowNum.col, curRowNum.row, false);
        }
        grid.layout(true, true);
    }

    public ResultSetProvider getResultSetProvider()
    {
        return resultSetProvider;
    }

    public void setResultSetProvider(ResultSetProvider resultSetProvider)
    {
        this.resultSetProvider = resultSetProvider;
    }

    public void dispose()
    {
        if (!grid.isDisposed()) {
            grid.dispose();
        }
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
            this.grid.setFont(rsFont);
        }
        Color selBackColor = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK);
        if (selBackColor != null) {
            this.grid.setBackgroundSelected(selBackColor);
        }
        Color selForeColor = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE);
        if (selForeColor != null) {
            this.grid.setForegroundSelected(selForeColor);
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

    public void setData(DBCResultSetMetaData metaData, List<Object[]> rows)
    {
        this.metaData = metaData;
        this.curRows = rows;
        this.initResultSet();
    }

    public void appendData(List<Object[]> rows)
    {
        curRows.addAll(rows);
        grid.setItemCount(curRows.size());
    }

    private void clearResultsView()
    {
        // Clear previous state
        grid.setRedraw(false);
        grid.clearGrid();
        grid.setRedraw(true);
    }

    private void initResultSet()
    {
        grid.setRedraw(false);
        grid.clearGrid();
        if (mode == ResultSetMode.RECORD) {
            grid.addColumn("Value", "Column Value");
            grid.setItemCount(metaData == null ? 0 : metaData.getColumns().size());
            this.showCurrentRows();
        } else {
            if (metaData != null) {
                for (DBCColumnMetaData columnMeta : metaData.getColumns()) {
                    grid.addColumn(
                        columnMeta.getLabel(),
                        CommonUtils.isEmpty(columnMeta.getTableName()) ?
                            columnMeta.getName() :
                            columnMeta.getTableName() + "." + columnMeta.getName());
                }
            }
            grid.setItemCount(curRows.size());
            this.showRowsCount();
        }

        grid.reinitState();
        grid.setRedraw(true);

        this.updateGridCursor();
    }

/*
    private void showUpdateCount(int updateCount)
    {
        TableColumn column = new TableColumn (resultsView, SWT.NONE);
        column.setText("Updated Rows");
        column.setToolTipText("Updated rows count");
        curColumns.add(column);

        TableItem item = new TableItem(resultsView, SWT.NONE);
        item.setText(String.valueOf(updateCount));

        resultsView.setItemCount(1);

        column.pack();
    }
*/

    public boolean isEditable()
    {
        return false;
    }

    public boolean isInsertable()
    {
        return false;
    }

    public void fillLazyRow(IGridRow row)
    {
        if (mode == ResultSetMode.RECORD) {
            // Fill record
            if (curRowNum.row >= curRows.size()) {
                return;
            }
            Object[] values = curRows.get(curRowNum.row);
            assert(row.getIndex() < values.length);
            Object value = values[row.getIndex()];
            row.setData(value);
            row.setText(0, getCellValue(value));
        } else {
            // Fill rows
            Object[] values = curRows.get(row.getIndex());
            row.setData(values);
            for (int i = 0; i < values.length; i++) {
                row.setText(i, getCellValue(values[i]));
            }
        }
    }

    private String getCellValue(Object colValue)
    {
        if (colValue == null) {
            return "[NULL]";
/*
        } else if (colValue instanceof Blob) {
            return "[BLOB]";
        } else if (colValue instanceof Clob) {
            return "[CLOB]";
*/
        } else {
            return colValue.toString();
        }
    }

    public void showRowViewer(IGridRow row, boolean editable)
    {
        Object data = row.getData();
        DBCColumnMetaData columnInfo;
        if (data instanceof Object[]) {
            data = ((Object[])data)[row.getColumn()];
            columnInfo = metaData.getColumns().get(row.getColumn());
        } else {
            columnInfo = metaData.getColumns().get(row.getIndex());
        }
        // Determine column type and use appropriate viewer
        DBSDataType dataType = null;
        try {
            dataType = resultSetProvider.getDataSource().getInfo().getSupportedDataType(columnInfo.getTypeName());
        } catch (DBException e) {
            log.warn("Can't determine column datatype", e);
        }
        boolean isLOB = false;
        if (dataType != null && dataType.getDataKind() == DBSDataKind.LOB) {
            isLOB = true;
            if (data != null && !(data instanceof DBCLOB)) {
                log.warn("Bad LOB data value");
                isLOB = false;
            }
        }
        ValueViewDialog viewDialog;
        if (isLOB) {
            viewDialog = new LobViewDialog(grid.getShell(), row, columnInfo, (DBCLOB)data);
        } else {
            viewDialog = new TextViewDialog(grid.getShell(), row, columnInfo, data);
        }
        viewDialog.open();
    }

    public String getRowTitle(int rowNum)
    {
        if (mode == ResultSetMode.RECORD) {
            List<DBCColumnMetaData> columns = metaData.getColumns();
            if (rowNum < 0 || rowNum >= columns.size()) {
                return "";
            }
            return columns.get(rowNum).getName();
        } else {
            return String.valueOf(rowNum + 1);
        }
    }

    private void showCurrentRows()
    {
        setStatus("Row " + (curRowNum.row + 1));
    }

    private void showRowsCount()
    {
        setStatus(
            String.valueOf(curRows.size()) +
                " row" + (curRows.size() > 1 ? "s" : "") + " fetched");
    }

    public Control getControl()
    {
        return grid;
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
        return new StructuredSelection(grid.getSelection().toArray());  
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public void refresh()
    {
        // Refresh all rows
        this.curRows.clear();
        this.clearResultsView();
        if (resultSetProvider != null) {
            resultSetProvider.extractResultSetData(0);
        }
    }

}
