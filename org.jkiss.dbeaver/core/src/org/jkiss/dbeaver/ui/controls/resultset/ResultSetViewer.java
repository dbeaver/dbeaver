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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ThemeConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.grid.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ResultSetViewer
 */
public class ResultSetViewer extends Viewer implements IGridDataProvider, IPropertyChangeListener
{
    static Log log = LogFactory.getLog(ResultSetViewer.class);

    private IWorkbenchPartSite site;
    private ResultSetMode mode;
    private GridControl grid;
    private ResultSetProvider resultSetProvider;
    private ResultSetDataPump dataPump;
    private IThemeManager themeManager;

    private ResultSetColumn[] metaColumns;
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

    public ResultSetViewer(Composite parent, IWorkbenchPartSite site, ResultSetProvider resultSetProvider)
    {
        super();
        this.site = site;
        this.mode = ResultSetMode.GRID;
        this.grid = new GridControl(
            parent,
            SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
            site,
            this);

        createStatusBar(grid);

        this.resultSetProvider = resultSetProvider;
        this.dataPump = new ResultSetDataPump(this);

        this.themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
        this.themeManager.addPropertyChangeListener(this);
        this.grid.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        this.grid.addCursorChangeListener(new Listener() {
            public void handleEvent(Event event)
            {
                onChangeGridCursor(event.x, event.y);
            }
        });
        applyThemeSettings();
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
            if (metaColumns != null) {
                GC gc = new GC(grid);
                gc.setFont(grid.getFont());
                for (ResultSetColumn column : metaColumns) {
                    Point ext = gc.stringExtent(column.metaData.getColumnName());
                    if (ext.x > defaultWidth) {
                        defaultWidth = ext.x;
                    }
                }
                defaultWidth += DBIcon.EDIT_COLUMN.getImage().getBounds().width + 2;
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

    public ResultSetDataPump getDataPump()
    {
        return dataPump;
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

    public void setColumnsInfo(ResultSetColumn[] metaColumns)
    {
        this.metaColumns = metaColumns;
    }

    public void setData(List<Object[]> rows)
    {
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
            grid.addColumn("Value", "Column Value", null);
            grid.setItemCount(metaColumns == null ? 0 : metaColumns.length);
            this.showCurrentRows();
        } else {
            if (metaColumns != null) {
                for (ResultSetColumn column : metaColumns) {
                    Image columnImage = null;
                    if (column.editable) {
                        columnImage = DBIcon.EDIT_COLUMN.getImage();
                    }
                    grid.addColumn(
                        column.metaData.getLabel(),
                        CommonUtils.isEmpty(column.metaData.getTableName()) ?
                            column.metaData.getColumnName() :
                            column.metaData.getTableName() + "." + column.metaData.getColumnName(),
                        columnImage);
                }
            }
            grid.setItemCount(curRows.size());
            this.showRowsCount();
        }

        grid.reinitState();
        grid.setRedraw(true);

        this.updateGridCursor();
    }

    public boolean isEditable()
    {
        return true;
    }

    public boolean isCellEditable(int column, int row) {
        if (mode == ResultSetMode.RECORD) {
            if (row < 0 || row >= metaColumns.length) {
                log.warn("Bad cell requsted - " + column + ":" + row);
                return false;
            }
            return metaColumns[row].editable;
        } else {
            if (column < 0 || column >= metaColumns.length) {
                log.warn("Bad cell requsted - " + column + ":" + row);
                return false;
            }
            return metaColumns[column].editable;
        }
    }

    public boolean isInsertable()
    {
        return false;
    }

    public void fillRowData(IGridRowData row)
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
        } else {
            return colValue.toString();
        }
    }

    public boolean showCellEditor(
        final IGridRowData row,
        final boolean inline,
        final Composite inlinePlaceholder)
    {
        final int columnIndex = (mode == ResultSetMode.GRID ? row.getColumn() : row.getIndex());
        final Object[] curRow = (mode == ResultSetMode.GRID ? curRows.get(row.getIndex()) : curRows.get(curRowNum.row));
        DBDValueController valueController = new DBDValueController() {
            public DBCColumnMetaData getColumnMetaData()
            {
                return metaColumns[columnIndex].metaData;
            }

            public Object getValue()
            {
                return curRow[columnIndex];
            }

            public void updateValue(Object value)
            {
                curRow[columnIndex] = value;
                row.setText(row.getColumn(), getCellValue(value));
            }

            public DBDValueLocator getValueLocator()
            {
                return null;
            }

            public boolean isInlineEdit()
            {
                return inline;
            }

            public boolean isReadOnly()
            {
                return false;
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
                grid.cancelInlineEditor();
            }

            public void showMessage(String message, boolean error)
            {
                setStatus(message, error);
            }
        };
        try {
            return metaColumns[columnIndex].valueHandler.editValue(valueController);
        }
        catch (Exception e) {
            log.error(e);
            return false;
        }
    }

    public void fillRowInfo(int rowNum, IGridRowInfo rowInfo)
    {
        if (mode == ResultSetMode.RECORD) {
            if (rowNum < 0 || rowNum >= metaColumns.length) {
                return;
            }
            rowInfo.setText(metaColumns[rowNum].metaData.getColumnName());
            if (metaColumns[rowNum].editable) {
                rowInfo.setImage(DBIcon.EDIT_COLUMN.getImage());
            }
        } else {
            rowInfo.setText(String.valueOf(rowNum + 1));
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
        updateGridCursor();
    }

}
