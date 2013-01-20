/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.data.query.DBQCondition;
import org.jkiss.dbeaver.model.data.query.DBQOrderColumn;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.model.virtual.DBVConstants;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.tools.data.wizard.DataExportProvider;
import org.jkiss.dbeaver.tools.data.wizard.DataExportWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.spreadsheet.ISpreadsheetController;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseGeneral;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ResultSetViewer
 */
public class ResultSetViewer extends Viewer implements IDataSourceProvider, ISpreadsheetController, IPropertyChangeListener, ISaveablePart2, IAdaptable
{
    static final Log log = LogFactory.getLog(ResultSetViewer.class);

    private static final int DEFAULT_ROW_HEADER_WIDTH = 50;

    public enum ResultSetMode {
        GRID,
        RECORD
    }

    public enum RowPosition {
        FIRST,
        PREVIOUS,
        NEXT,
        LAST
    }

    private IWorkbenchPartSite site;
    private ResultSetMode mode;
    private Composite viewerPanel;
    private Text filtersText;

    private final SashForm resultsSash;
    private final Spreadsheet spreadsheet;
    private final ViewValuePanel previewPane;

    private ResultSetProvider resultSetProvider;
    private ResultSetDataReceiver dataReceiver;
    private IThemeManager themeManager;
    private ToolBarManager toolBarManager;

    // columns
    private DBDAttributeBinding[] metaColumns = new DBDAttributeBinding[0];
    private DBDDataFilter dataFilter = new DBDDataFilter();

    // Data
    private List<Object[]> origRows = new ArrayList<Object[]>();
    private List<Object[]> curRows = new ArrayList<Object[]>();
    // Current row number (for record mode)
    private int curRowNum = -1;
    private int curColNum = -1;
    private boolean singleSourceCells;
    private boolean hasData = false;

    // Edited rows and cells
    private Set<RowInfo> addedRows = new TreeSet<RowInfo>();
    private Set<RowInfo> removedRows = new TreeSet<RowInfo>();
    private Map<GridPos, Object> editedValues = new HashMap<GridPos, Object>();

    private Text statusLabel;

    private Map<ResultSetValueController, DBDValueEditor> openEditors = new HashMap<ResultSetValueController, DBDValueEditor>();
    // Flag saying that edited values update is in progress
    private boolean updateInProgress = false;

    // UI modifiers
    private Color colorRed;
    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;
    private Color foregroundNull;
    private Font boldFont;

    private ResultSetDataPumpJob dataPumpJob;

    private final List<ResultSetListener> listeners = new ArrayList<ResultSetListener>();

    public ResultSetViewer(Composite parent, IWorkbenchPartSite site, ResultSetProvider resultSetProvider)
    {
        super();

        this.site = site;
        this.mode = ResultSetMode.GRID;
        this.resultSetProvider = resultSetProvider;
        this.dataReceiver = new ResultSetDataReceiver(this);

        this.colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
        ISharedTextColors sharedColors = DBeaverUI.getSharedTextColors();
        this.backgroundAdded = sharedColors.getColor(new RGB(0xE4, 0xFF, 0xB5));
        this.backgroundDeleted = sharedColors.getColor(new RGB(0xFF, 0x63, 0x47));
        this.backgroundModified = sharedColors.getColor(new RGB(0xFF, 0xE4, 0xB5));
        this.foregroundNull = parent.getDisplay().getSystemColor(SWT.COLOR_GRAY);
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());

        this.viewerPanel = UIUtils.createPlaceholder(parent, 1);
        UIUtils.setHelp(this.viewerPanel, IHelpContextIds.CTX_RESULT_SET_VIEWER);

        this.filtersText = new Text(viewerPanel, SWT.READ_ONLY | SWT.BORDER);
        this.filtersText.setForeground(colorRed);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.exclude = true;
        this.filtersText.setLayoutData(gd);
        this.filtersText.setVisible(false);

        {
            resultsSash = new SashForm(viewerPanel, SWT.HORIZONTAL | SWT.SMOOTH);
            resultsSash.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultsSash.setSashWidth(5);
            resultsSash.setBackground(resultsSash.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

            this.spreadsheet = new Spreadsheet(
                resultsSash,
                SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
                site,
                this,
                new ContentProvider(),
                new ContentLabelProvider(),
                new ColumnLabelProvider(),
                new RowLabelProvider());
            this.spreadsheet.setLayoutData(new GridData(GridData.FILL_BOTH));

            this.previewPane = new ViewValuePanel(resultsSash) {
                @Override
                protected void hidePanel()
                {
                    togglePreview();
                }
            };

            resultsSash.setWeights(new int[]{75, 25});
            resultsSash.setMaximizedControl(spreadsheet);
        }

        createStatusBar(viewerPanel);
        changeMode(ResultSetMode.GRID);

        this.themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
        this.themeManager.addPropertyChangeListener(this);
        this.spreadsheet.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        this.spreadsheet.addCursorChangeListener(new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                updateGridCursor(event.x, event.y);
            }
        });
        this.spreadsheet.setTopLeftRenderer(new ResultSetTopLeftRenderer(this));
        applyThemeSettings();

        spreadsheet.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e)
            {
                updateToolbar();
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                updateToolbar();
            }
        });
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return getDataContainer().getDataSource();
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        return null;
    }

    public void addListener(ResultSetListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(ResultSetListener listener)
    {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void updateGridCursor(int col, int row)
    {
        boolean changed;
        if (mode == ResultSetMode.GRID) {
            changed = curRowNum != row || curColNum != col;
            curRowNum = row;
            curColNum = col;
        } else {
            changed = curColNum != row;
            curColNum = row;
        }
        if (changed) {
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_MOVE);
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_EDITABLE);
            updateToolbar();
            previewValue();
        }
    }

    private void updateRecordMode()
    {
        int oldColNum = this.curColNum;
        this.initResultSet();
        this.curColNum = oldColNum;
        spreadsheet.setCursor(new GridPos(0, oldColNum), false);
    }

    private void updateEditControls()
    {
        ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_EDITABLE);
        ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CHANGED);
        updateToolbar();
    }

    /**
     * It is a hack function. Generally all command associated widgets should be updated automatically by framework.
     * Freaking E4 do not do it. I've spent a couple of days fighting it. Guys, you owe me.
     * TODO: just remove in future. In fact everything must work without it
     */
    private void updateToolbar()
    {
        if (toolBarManager.isEmpty()) {
            return;
        }
        for (IContributionItem item : toolBarManager.getItems()) {
            item.update();
        }
    }

    private void refreshSpreadsheet(boolean rowsChanged)
    {
        if (spreadsheet.isDisposed()) {
            return;
        }
        if (rowsChanged) {
            if (curRowNum >= curRows.size()) {
                curRowNum = curRows.size() - 1;
            }
            GridPos curPos = spreadsheet.getCursorPosition();
            if (mode == ResultSetMode.GRID) {
                if (curPos.row >= curRows.size()) {
                    curPos.row = curRows.size() - 1;
                }
            }

            this.spreadsheet.reinitState();

            // Set cursor on new row
            if (mode == ResultSetMode.GRID) {
                spreadsheet.setCursor(curPos, false);
            } else {
                updateRecordMode();
            }

        } else {
            this.spreadsheet.redrawGrid();
        }
    }

    private void createStatusBar(Composite parent)
    {
        UIUtils.createHorizontalLine(parent);

        Composite statusBar = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        statusBar.setLayoutData(gd);
        GridLayout gl = new GridLayout(4, false);
        gl.marginWidth = 0;
        gl.marginHeight = 3;
        //gl.marginBottom = 5;
        statusBar.setLayout(gl);
        
        statusLabel = new Text(statusBar, SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        statusLabel.setLayoutData(gd);
        statusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                EditTextDialog.showText(site.getShell(), CoreMessages.controls_resultset_viewer_dialog_status_title, statusLabel.getText());
            }
        });

/*
        IAction viewMessageAction = new Action("View status message", DBIcon.TREE_INFO.getImageDescriptor()) {
            public void run()
            {
            }
        };
*/

        toolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
        //toolBarManager.add(viewMessageAction);
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_APPLY_CHANGES));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_REJECT_CHANGES));
        toolBarManager.add(new Separator());
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_ADD));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_COPY));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_DELETE));
        toolBarManager.add(new Separator());
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_FIRST));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_PREVIOUS));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_NEXT));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_LAST));
        toolBarManager.add(new Separator());
        //toolBarManager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_REFRESH, "Refresh result set", DBIcon.RS_REFRESH.getImageDescriptor()));
        // Use simple action for refresh to avoid ambiguous behaviour of F5 shortcut
        Action refreshAction = new Action(CoreMessages.controls_resultset_viewer_action_refresh, DBIcon.RS_REFRESH.getImageDescriptor()) {
            @Override
            public void run()
            {
                refresh();
            }
        };
        toolBarManager.add(refreshAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_MODE));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_PREVIEW));
        toolBarManager.add(new ConfigAction());

        toolBarManager.createControl(statusBar);

        //updateEditControls();
    }

    Spreadsheet getSpreadsheet()
    {
        return spreadsheet;
    }

    public DBSDataContainer getDataContainer()
    {
        return resultSetProvider.getDataContainer();
    }

    public DBDAttributeBinding[] getMetaColumns()
    {
        return metaColumns;
    }

    ////////////////////////////////////////////////////////////
    // Filters

    public DBDDataFilter getDataFilter()
    {
        return dataFilter;
    }

    public void setDataFilter(final DBDDataFilter dataFilter)
    {
        if (CommonUtils.equalObjects(this.dataFilter, dataFilter)) {
            return;
        }
        this.dataFilter = dataFilter;
        reorderResultSet(true, new Runnable() {
            @Override
            public void run()
            {
                resetColumnOrdering();
            }
        });
        this.updateFiltersText();
    }

    private void updateFiltersText()
    {
        StringBuilder where = new StringBuilder();
        dataFilter.appendConditionString(getDataSource(), where);
        boolean show = false;
        if (where.length() > 0) {
            filtersText.setText(where.toString());
            show = true;
        }
        if (filtersText.getVisible() == show) {
            return;
        }
        filtersText.setVisible(show);
        ((GridData)filtersText.getLayoutData()).exclude = !show;
        viewerPanel.layout();
    }

    // Update all columns ordering
    private void resetColumnOrdering()
    {
        if (!spreadsheet.isDisposed() && metaColumns != null && mode == ResultSetMode.GRID) {
            for (int i = 0, metaColumnsLength = metaColumns.length; i < metaColumnsLength; i++) {
                DBDAttributeBinding column = metaColumns[i];
                DBQOrderColumn columnOrder = dataFilter.getOrderColumn(column.getColumnName());
                GridColumn gridColumn = spreadsheet.getColumn(i);
                if (columnOrder == null) {
                    gridColumn.setSort(SWT.DEFAULT);
                } else {
                    gridColumn.setSort(columnOrder.isDescending() ? SWT.UP : SWT.DOWN);
                }
            }
            spreadsheet.redrawGrid();
        }
    }

    ////////////////////////////////////////////////////////////
    // Grid/Record mode

    public ResultSetMode getMode()
    {
        return mode;
    }

    public void toggleMode()
    {
        changeMode(mode == ResultSetMode.GRID ? ResultSetMode.RECORD : ResultSetMode.GRID);
        // Refresh elements
        ICommandService commandService = (ICommandService) site.getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(ResultSetCommandHandler.CMD_TOGGLE_MODE, null);
        }
    }

    private void changeMode(ResultSetMode resultSetMode)
    {
        int oldRowNum = this.curRowNum, oldColNum = this.curColNum;
        if (oldRowNum < 0 && curRows.size() > 0) {
            oldRowNum  = 0;
        }
        this.mode = resultSetMode;
        if (this.mode == ResultSetMode.GRID) {
            this.spreadsheet.setRowHeaderWidth(DEFAULT_ROW_HEADER_WIDTH);
            this.initResultSet();
        } else {
            this.resetRecordHeaderWidth();
            this.updateRecordMode();
        }
        this.curRowNum = oldRowNum;
        this.curColNum = oldColNum;
        if (mode == ResultSetMode.GRID) {
            if (this.curRowNum >= 0 && this.curRowNum < spreadsheet.getItemCount()) {
                spreadsheet.setCursor(new GridPos(this.curColNum, this.curRowNum), false);
            }
        } else {
            if (this.curColNum >= 0) {
                spreadsheet.setCursor(new GridPos(0, this.curColNum), false);
            }
        }
        spreadsheet.layout(true, true);
    }

    private void resetRecordHeaderWidth()
    {
        // Calculate width of spreadsheet panel - use longest column title
        int defaultWidth = 0;
        GC gc = new GC(spreadsheet);
        gc.setFont(spreadsheet.getFont());
        for (DBDAttributeBinding column : metaColumns) {
            Point ext = gc.stringExtent(column.getColumnName());
            if (ext.x > defaultWidth) {
                defaultWidth = ext.x;
            }
        }
        defaultWidth += DBIcon.EDIT_COLUMN.getImage().getBounds().width + 2;

        spreadsheet.setRowHeaderWidth(defaultWidth + DEFAULT_ROW_HEADER_WIDTH);
    }

    ////////////////////////////////////////////////////////////
    // Value preview

    public boolean isPreviewVisible()
    {
        return resultsSash.getMaximizedControl() == null;
    }

    public void togglePreview()
    {
        if (resultsSash.getMaximizedControl() == null) {
            resultsSash.setMaximizedControl(spreadsheet);
        } else {
            resultsSash.setMaximizedControl(null);
            previewValue();
        }
        // Refresh elements
        ICommandService commandService = (ICommandService) site.getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(ResultSetCommandHandler.CMD_TOGGLE_PREVIEW, null);
        }
    }

    void previewValue()
    {
        if (!isPreviewVisible()) {
            return;
        }
        final GridPos cell = translateGridPos(getCurrentPosition());
        if (!cell.isValid() || cell.col >= metaColumns.length || cell.row >= curRows.size()) {
            return;
        }
        final ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(cell.row),
            cell.col,
            DBDValueController.EditType.PANEL,
            previewPane.getViewPlaceholder());
        previewPane.viewValue(valueController);
    }

    ////////////////////////////////////////////////////////////
    // Misc

    public void dispose()
    {
        closeEditors();
        clearData();
        clearMetaData();

        if (!spreadsheet.isDisposed()) {
            spreadsheet.dispose();
        }

        statusLabel.dispose();
        themeManager.removePropertyChangeListener(ResultSetViewer.this);

        UIUtils.dispose(this.boldFont);
        if (toolBarManager != null) {
            try {
                toolBarManager.dispose();
            } catch (Throwable e) {
                // ignore
                log.debug("Error disposing toolbar", e);
            }
        }
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

    @Override
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

    void scrollToRow(RowPosition position)
    {
        switch (position) {
            case FIRST:
                if (mode == ResultSetMode.RECORD) {
                    curRowNum = 0;
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, -spreadsheet.getItemCount(), false);
                }
                break;
            case PREVIOUS:
                if (mode == ResultSetMode.RECORD && curRowNum > 0) {
                    curRowNum--;
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, -1, false);
                }
                break;
            case NEXT:
                if (mode == ResultSetMode.RECORD && curRowNum < curRows.size() - 1) {
                    curRowNum++;
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, 1, false);
                }
                break;
            case LAST:
                if (mode == ResultSetMode.RECORD && !curRows.isEmpty()) {
                    curRowNum = curRows.size() - 1;
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, spreadsheet.getItemCount(), false);
                }
                break;
        }
    }

    boolean isColumnReadOnly(GridPos pos)
    {
        int column;
        if (mode == ResultSetMode.GRID) {
            column = pos.col;
        } else {
            column = curRowNum;
        }
        return column < 0 || column >= metaColumns.length || isColumnReadOnly(metaColumns[column]);
    }

    boolean isColumnReadOnly(DBDAttributeBinding column)
    {
        if (isReadOnly() || column.getValueLocator() == null || !(column.getValueLocator().getEntity() instanceof DBSDataContainer)) {
            return true;
        }
        DBSDataContainer dataContainer = (DBSDataContainer) column.getValueLocator().getEntity();
        return (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_UPDATE) == 0;
    }

    public int getCurrentRow()
    {
        return mode == ResultSetMode.GRID ? spreadsheet.getCurrentRow() : curRowNum;
    }

    public GridPos getCurrentPosition()
    {
        return spreadsheet.getCursorPosition();
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
        if (statusLabel.isDisposed()) {
            return;
        }
        if (error) {
            statusLabel.setForeground(colorRed);
        } else {
            statusLabel.setForeground(null);
        }
        if (status == null) {
            status = "???"; //$NON-NLS-1$
        }
        statusLabel.setText(status);
    }

    public void setExecutionTime(long executionTime)
    {
        if (statusLabel.isDisposed()) {
            return;
        }
        statusLabel.setText(statusLabel.getText() + " - " + executionTime + CoreMessages.controls_resultset_viewer_ms); //$NON-NLS-1$
    }

    /**
     * Sets new metadata of result set
     * @param columns columns metadata
     * @return true if new metadata differs from old one, false otherwise
     */
    public boolean setMetaData(DBDAttributeBinding[] columns)
    {
        boolean update = false;
        if (this.metaColumns == null || this.metaColumns.length != columns.length) {
            update = true;
        } else {
            for (int i = 0; i < this.metaColumns.length; i++) {
                if (!this.metaColumns[i].getAttribute().equals(columns[i].getAttribute())) {
                    update = true;
                    break;
                }
            }
        }
        if (update) {
            UIUtils.runInUI(null, new Runnable() {
                @Override
                public void run()
                {
                    spreadsheet.clearGrid();
                }
            });
            this.clearData();
            this.metaColumns = columns;
            this.dataFilter = new DBDDataFilter();
        }
        return update;
    }

    public void setData(List<Object[]> rows, boolean updateMetaData)
    {
        // Clear previous data
        this.closeEditors();
        this.clearData();

        // Add new data
        this.origRows.addAll(rows);
        this.curRows.addAll(rows);

        if (updateMetaData) {
            // Check single source flag
            this.singleSourceCells = true;
            DBSEntity sourceTable = null;
            for (DBDAttributeBinding column : metaColumns) {
                if (isColumnReadOnly(column)) {
                    singleSourceCells = false;
                    break;
                }
                if (sourceTable == null) {
                    sourceTable = column.getValueLocator().getEntity();
                } else if (sourceTable != column.getValueLocator().getEntity()) {
                    singleSourceCells = false;
                    break;
                }
            }

            this.initResultSet();
        } else {
            this.refreshSpreadsheet(true);
        }

        String statusMessage;
        if (rows.size() > 0) {
            statusMessage = rows.size() + CoreMessages.controls_resultset_viewer_status_rows;
        } else {
            statusMessage = CoreMessages.controls_resultset_viewer_status_no_data;
        }
        setStatus(statusMessage, false);
        hasData = true;
        updateEditControls();
    }

    public void appendData(List<Object[]> rows)
    {
        origRows.addAll(rows);
        curRows.addAll(rows);
        refreshSpreadsheet(true);

        setStatus(NLS.bind(CoreMessages.controls_resultset_viewer_status_rows_size, curRows.size(), rows.size()), false);
    }

    private void closeEditors() {
        List<DBDValueEditor> editors = new ArrayList<DBDValueEditor>(openEditors.values());
        for (DBDValueEditor editor : editors) {
            editor.closeValueEditor();
        }
        if (!openEditors.isEmpty()) {
            log.warn("Some value editors are still registered at resultset: " + openEditors.size());
        }
        openEditors.clear();
    }

    private void initResultSet()
    {
        closeEditors();

        spreadsheet.setRedraw(false);
        spreadsheet.clearGrid();
        if (mode == ResultSetMode.RECORD) {
            this.resetRecordHeaderWidth();
            this.showCurrentRows();
        } else {
            this.showRowsCount();
        }

        spreadsheet.reinitState();

        spreadsheet.setRedraw(true);

        this.updateGridCursor(-1, -1);
        this.updateFiltersText();
    }

    @Override
    public int promptToSaveOnClose()
    {
        if (!isDirty()) {
            return ISaveablePart2.YES;
        }
        int result = ConfirmationDialog.showConfirmDialog(
            spreadsheet.getShell(),
            PrefConstants.CONFIRM_RS_EDIT_CLOSE,
            ConfirmationDialog.QUESTION_WITH_CANCEL);
        if (result == IDialogConstants.YES_ID) {
            return ISaveablePart2.YES;
        } else if (result == IDialogConstants.NO_ID) {
            rejectChanges();
            return ISaveablePart2.NO;
        } else {
            return ISaveablePart2.CANCEL;
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
        applyChanges(RuntimeUtils.makeMonitor(monitor));
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isDirty()
    {
        return !editedValues.isEmpty() || !addedRows.isEmpty() || !removedRows.isEmpty();
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    public boolean isSaveOnCloseNeeded()
    {
        return true;
    }

    public boolean hasData()
    {
        return hasData;
    }

    @Override
    public boolean isReadOnly()
    {
        if (updateInProgress) {
            return true;
        }
        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return true;
        }
        DBPDataSource dataSource = dataContainer.getDataSource();
        return dataSource == null ||
            !dataSource.isConnected() ||
            dataSource.getContainer().isConnectionReadOnly() ||
            dataSource.getInfo().isReadOnlyData();
    }

    @Override
    public boolean isValidCell(GridPos pos) {
        if (pos == null) {
            return false;
        }
        if (mode == ResultSetMode.GRID) {
            return (pos.col >= 0 && pos.row >= 0);
        } else {
            return curRowNum >= 0 && pos.row >= 0;
        }
    }

    GridPos translateGridPos(GridPos pos)
    {
        if (mode == ResultSetMode.GRID) {
            return pos;
        } else {
            return new GridPos(pos.row, curRowNum);
        }
    }

    public boolean isCellModified(GridPos pos) {
        return !editedValues.isEmpty() && editedValues.containsKey(pos);
    }

    @Override
    public boolean isInsertable()
    {
        return
            !isReadOnly() &&
            singleSourceCells &&

            !CommonUtils.isEmpty(metaColumns);
    }

    @Override
    public boolean showCellEditor(
        final boolean inline)
    {
        // The control that will be the editor must be a child of the Table
        final GridPos focusCell = spreadsheet.getFocusCell();
        //GridPos pos = getPosFromPoint(event.x, event.y);
        if (focusCell == null || focusCell.row < 0 || focusCell.col < 0) {
            return false;
        }
        if (!isValidCell(focusCell)) {
            return false;
        }

        GridPos cell = translateGridPos(focusCell);
        if (cell.row < 0 || cell.row >= curRows.size() || cell.col < 0 || cell.col >= metaColumns.length) {
            // Out of bounds
            log.debug("Editor position is out of bounds (" + cell.col + ":" + cell.row + ")");
            return false;
        }
        if (!inline) {
            for (ResultSetValueController valueController : openEditors.keySet()) {
                GridPos cellPos = valueController.getCellPos();
                if (cellPos != null && cellPos.equalsTo(cell)) {
                    openEditors.get(valueController).showValueEditor();
                    return true;
                }
            }
        }
        DBDAttributeBinding metaColumn = metaColumns[cell.col];
        if (isColumnReadOnly(metaColumn) && inline) {
            // No inline editors for readonly columns
            return false;
        }
        final int handlerFeatures = metaColumn.getValueHandler().getFeatures();
        if (handlerFeatures == DBDValueHandler.FEATURE_NONE) {
            return false;
        }

        Composite placeholder = null;
        if (inline) {
            if (isReadOnly()) {
                return false;
            }
            spreadsheet.cancelInlineEditor();

            placeholder = new Composite(spreadsheet, SWT.NONE);
            placeholder.setFont(spreadsheet.getFont());
            placeholder.setLayout(new FillLayout());

            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            gd.grabExcessHorizontalSpace = true;
            gd.grabExcessVerticalSpace = true;
            placeholder.setLayoutData(gd);
        }

        ResultSetValueController valueController = new ResultSetValueController(
            curRows.get(cell.row),
            cell.col,
            inline ? DBDValueController.EditType.INLINE : DBDValueController.EditType.EDITOR,
            inline ? placeholder : null);
        boolean editSuccess;
        try {
            editSuccess = metaColumn.getValueHandler().editValue(valueController);
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(site.getShell(), "Cannot edit value", null, e);
            return false;
        }

        if (inline) {
            if (editSuccess) {
                spreadsheet.showCellEditor(focusCell, placeholder);
            } else {
                // No editor was created so just drop placeholder
                placeholder.dispose();
            }
        }
        return editSuccess;
    }

    @Override
    public void resetCellValue(GridPos cell, boolean delete)
    {
        cell = translateGridPos(cell);
        if (editedValues != null && editedValues.containsKey(cell)) {
            Object[] row = this.curRows.get(cell.row);
            resetValue(row[cell.col]);
            row[cell.col] = this.editedValues.get(cell);
            this.editedValues.remove(cell);
        }
        spreadsheet.redrawGrid();
        updateEditControls();
    }

    @Override
    public void fillContextMenu(GridPos curCell, IMenuManager manager) {

        // Custom oldValue items
        final GridPos cell = translateGridPos(curCell);
        boolean noCellSelected = (cell.row < 0 || curRows.size() <= cell.row || cell.col < 0 || cell.col >= metaColumns.length);
        if (!noCellSelected) {
            final ResultSetValueController valueController = new ResultSetValueController(
                curRows.get(cell.row),
                cell.col,
                DBDValueController.EditType.NONE,
                null);
            final Object value = valueController.getValue();

            if (isValidCell(cell)) {
                // Standard items
                manager.add(new Separator());
                manager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT));
                manager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT_INLINE));
                if (!valueController.isReadOnly() && !DBUtils.isNullValue(value)) {
                    manager.add(new Action(CoreMessages.controls_resultset_viewer_action_set_to_null) {
                        @Override
                        public void run()
                        {
                            valueController.updateValue(DBUtils.makeNullValue(value));
                        }
                    });
                }
                if (isCellModified(cell)) {
                    Action resetValueAction = new Action(CoreMessages.controls_resultset_viewer_action_reset_value)
                    {
                        @Override
                        public void run()
                        {
                            resetCellValue(cell, false);
                        }
                    };
                    resetValueAction.setAccelerator(SWT.ESC);
                    manager.add(resetValueAction);
                }
            }

            // Menus from value handler
            try {
                manager.add(new Separator());
                metaColumns[cell.col].getValueHandler().fillContextMenu(manager, valueController);
            }
            catch (Exception e) {
                log.error(e);
            }
        }

        if (!CommonUtils.isEmpty(metaColumns)) {
            // Export and other utility methods
            manager.add(new Separator());
            MenuManager filtersMenu = new MenuManager(
                CoreMessages.controls_resultset_viewer_action_order_filter,
                DBIcon.FILTER.getImageDescriptor(),
                "filters"); //$NON-NLS-1$
            filtersMenu.setRemoveAllWhenShown(true);
            filtersMenu.addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    fillFiltersMenu(manager);
                }
            });
            manager.add(filtersMenu);

            manager.add(new Action(CoreMessages.controls_resultset_viewer_action_export, DBIcon.EXPORT.getImageDescriptor()) {
                @Override
                public void run()
                {
                    ActiveWizardDialog dialog = new ActiveWizardDialog(
                        site.getWorkbenchWindow(),
                        new DataExportWizard(
                            Collections.singletonList(
                                new DataExportProvider(getDataContainer(), getDataFilter()))),
                        getSelection());
                    dialog.open();
                }
            });
        }
    }

    private void fillFiltersMenu(IMenuManager filtersMenu)
    {
        GridPos currentPosition = getCurrentPosition();
        int columnIndex = translateGridPos(currentPosition).col;
        if (supportsDataFilter() && columnIndex >= 0) {
            DBDAttributeBinding column = metaColumns[columnIndex];
            if (column.getTableColumn() == null) {
                return;
            }
            DBSDataKind dataKind = column.getAttribute().getDataKind();
            if (!column.getAttribute().isRequired()) {
                filtersMenu.add(new FilterByColumnAction("IS NULL", FilterByColumnType.NONE, column));
                filtersMenu.add(new FilterByColumnAction("IS NOT NULL", FilterByColumnType.NONE, column));
            }
            for (FilterByColumnType type : FilterByColumnType.values()) {
                if (type == FilterByColumnType.NONE || (type == FilterByColumnType.VALUE && !isValidCell(currentPosition))) {
                    // Value filters are available only if certain cell is selected
                    continue;
                }
                filtersMenu.add(new Separator());
                if (type.getValue(this, column, true) == null) {
                    continue;
                }
                if (dataKind == DBSDataKind.BOOLEAN) {
                    filtersMenu.add(new FilterByColumnAction("= ?", type, column));
                    filtersMenu.add(new FilterByColumnAction("<> ?", type, column));
                } else if (dataKind == DBSDataKind.NUMERIC || dataKind == DBSDataKind.DATETIME) {
                    filtersMenu.add(new FilterByColumnAction("= ?", type, column));
                    filtersMenu.add(new FilterByColumnAction("<> ?", type, column));
                    filtersMenu.add(new FilterByColumnAction("> ?", type, column));
                    filtersMenu.add(new FilterByColumnAction("< ?", type, column));
                } else if (dataKind == DBSDataKind.STRING) {
                    filtersMenu.add(new FilterByColumnAction("= '?'", type, column));
                    filtersMenu.add(new FilterByColumnAction("<> '?'", type, column));
                    filtersMenu.add(new FilterByColumnAction("> '?'", type, column));
                    filtersMenu.add(new FilterByColumnAction("< '?'", type, column));
                    filtersMenu.add(new FilterByColumnAction("LIKE '%?%'", type, column));
                    filtersMenu.add(new FilterByColumnAction("NOT LIKE '%?%'", type, column));
                }
            }
            filtersMenu.add(new Separator());
            if (getDataFilter().getFilterColumn(column.getColumnName()) != null) {
                filtersMenu.add(new FilterResetColumnAction(column));
            }
        }
        filtersMenu.add(new ShowFiltersAction());
    }

    private String translateFilterPattern(String pattern, FilterByColumnType type, DBDAttributeBinding column)
    {
        String value = CommonUtils.truncateString(
            CommonUtils.toString(
                type.getValue(this, column, true)),
            30);
        return pattern.replace("?", value);
    }

    boolean supportsDataFilter()
    {
        return (getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_FILTER) == DBSDataContainer.DATA_FILTER;
    }

    @Override
    public void changeSorting(final GridColumn column, final int state)
    {
        boolean ctrlPressed = (state & SWT.CTRL) == SWT.CTRL;
        boolean altPressed = (state & SWT.ALT) == SWT.ALT;
        if (ctrlPressed) {
            dataFilter.clearOrderColumns();
        }
        DBDAttributeBinding metaColumn = metaColumns[column.getIndex()];
        DBQOrderColumn columnOrder = dataFilter.getOrderColumn(metaColumn.getColumnName());
        //int newSort;
        if (columnOrder == null) {
            if (dataReceiver.isHasMoreData() && supportsDataFilter()) {
                if (!ConfirmationDialog.confirmActionWithParams(
                    spreadsheet.getShell(),
                    PrefConstants.CONFIRM_ORDER_RESULTSET,
                    metaColumn.getColumnName()))
                {
                    return;
                }
            }
            columnOrder = new DBQOrderColumn(metaColumn.getColumnName(), altPressed);
            dataFilter.addOrderColumn(columnOrder);
            //newSort = SWT.DOWN;

        } else {
            if (!columnOrder.isDescending()) {
                columnOrder.setDescending(!altPressed);
                //newSort = SWT.UP;
            } else {
                //newSort = SWT.DEFAULT;
                dataFilter.removeOrderColumn(columnOrder);
            }
        }
        //final int sort = newSort;
        reorderResultSet(false, new Runnable() {
            @Override
            public void run()
            {
                resetColumnOrdering();
            }
        });
    }

    private void showCurrentRows()
    {
        setStatus(CoreMessages.controls_resultset_viewer_status_row + (curRowNum + 1));
    }

    private void showRowsCount()
    {
        setStatus(String.valueOf(curRows.size()) + CoreMessages.controls_resultset_viewer_status_rows_fetched);
    }

    @Override
    public Control getControl()
    {
        return this.viewerPanel;
    }

    public Spreadsheet getGridControl()
    {
        return this.spreadsheet;
    }

    @Override
    public Object getInput()
    {
        return null;
    }

    @Override
    public void setInput(Object input)
    {
    }

    @Override
    public IStructuredSelection getSelection()
    {
        return new StructuredSelection(spreadsheet.getSelection().toArray());
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal)
    {
        fireSelectionChanged(new SelectionChangedEvent(this, selection));
    }

    public DBDDataReceiver getDataReceiver() {
        return dataReceiver;
    }

    @Override
    public void refresh()
    {
        // Check if we are dirty
        if (isDirty()) {
            switch (promptToSaveOnClose()) {
                case ISaveablePart2.CANCEL:
                    return;
                case ISaveablePart2.YES:
                    // Apply changes
                    applyChanges(null, new DBDValueListener() {
                        @Override
                        public void onUpdate(boolean success) {
                            if (success) {
                                getControl().getDisplay().asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        refresh();
                                    }
                                });
                            }
                        }
                    });
                    return;
                default:
                    // Just ignore previous RS values
                    break;
            }
        }

        int oldRowNum = curRowNum;
        int oldColNum = curColNum;

        if (resultSetProvider != null && resultSetProvider.isReadyToRun() && getDataContainer() != null && dataPumpJob == null) {
            int segmentSize = getSegmentMaxRows();
            if (oldRowNum >= segmentSize) {
                segmentSize = (oldRowNum / segmentSize + 1) * segmentSize;
            }
            runDataPump(0, segmentSize, new GridPos(oldColNum, oldRowNum), new Runnable() {
                @Override
                public void run()
                {
                    if (!supportsDataFilter() && !dataFilter.getOrderColumns().isEmpty()) {
                        reorderLocally();
                    }
                }
            });
        }
    }

    private boolean isServerSideFiltering()
    {
        return dataReceiver.isHasMoreData() || dataFilter.hasCustomFilters();
    }

    private void reorderResultSet(boolean force, Runnable onSuccess)
    {
        if ((force || isServerSideFiltering()) && supportsDataFilter()) {
            if (resultSetProvider != null && resultSetProvider.isReadyToRun() && getDataContainer() != null && dataPumpJob == null) {
                int segmentSize = getSegmentMaxRows();
                if (curRowNum >= segmentSize) {
                    segmentSize = (curRowNum / segmentSize + 1) * segmentSize;
                }
                runDataPump(0, segmentSize, new GridPos(curColNum, curRowNum), onSuccess);
            }
            return;
        }

        try {
            reorderLocally();
        } finally {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }
    }

    private void reorderLocally()
    {
        rejectChanges();

        // Sort locally
        curRows = new ArrayList<Object[]>(this.origRows);
        if (dataFilter.getOrderColumns().isEmpty()) {
            return;
        }
        Collections.sort(curRows, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] row1, Object[] row2)
            {
                int result = 0;
                for (DBQOrderColumn co : dataFilter.getOrderColumns()) {
                    final int colIndex = getMetaColumnIndex(null, co.getColumnName());
                    if (colIndex < 0) {
                        continue;
                    }
                    Object cell1 = row1[colIndex];
                    Object cell2 = row2[colIndex];
                    if (cell1 == cell2) {
                        result = 0;
                    } else if (DBUtils.isNullValue(cell1)) {
                        result = 1;
                    } else if (DBUtils.isNullValue(cell2)) {
                        result = -1;
                    } else if (cell1 instanceof Comparable<?>) {
                        result = ((Comparable)cell1).compareTo(cell2);
                    } else {
                        String str1 = cell1.toString();
                        String str2 = cell2.toString();
                        result = str1.compareTo(str2);
                    }
                    if (co.isDescending()) {
                        result = -result;
                    }
                    if (result != 0) {
                        break;
                    }
                }
                return result;
            }
        });
    }

    synchronized void readNextSegment()
    {
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        if (getDataContainer() != null && dataPumpJob == null) {
            dataReceiver.setHasMoreData(false);
            dataReceiver.setNextSegmentRead(true);
            runDataPump(curRows.size(), getSegmentMaxRows(), null, null);
        }
    }

    int getSegmentMaxRows()
    {
        if (getDataContainer() == null) {
            return 0;
        }
        IPreferenceStore preferenceStore = getDataSource().getContainer().getPreferenceStore();
        return preferenceStore.getInt(PrefConstants.RESULT_SET_MAX_ROWS);
    }

    private synchronized void runDataPump(
        final int offset,
        final int maxRows,
        final GridPos oldPos,
        final Runnable finalizer)
    {
        if (dataPumpJob == null) {
            dataPumpJob = new ResultSetDataPumpJob(this);
            dataPumpJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    final Throwable error = dataPumpJob == null ? null : dataPumpJob.getError();
                    dataPumpJob = null;
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run()
                        {
                            final Shell shell = getControl().getShell();
                            if (error != null) {
                                setStatus(error.getMessage(), true);
                                UIUtils.showErrorDialog(
                                    shell,
                                    "Error executing query",
                                    "Query execution failed",
                                    error);
                            }
                            if (oldPos != null) {
                                // Seems to be refresh
                                // Restore original position
                                ResultSetViewer.this.curRowNum = oldPos.row;
                                ResultSetViewer.this.curColNum = oldPos.col;
                                if (mode == ResultSetMode.GRID) {
                                    spreadsheet.setCursor(new GridPos(curColNum, curRowNum), false);
                                } else {
                                    spreadsheet.setCursor(new GridPos(0, curColNum), false);
                                }
                            }
                            if (finalizer != null) {
                                finalizer.run();
                            }
                        }
                    });
                }
            });
            dataPumpJob.setOffset(offset);
            dataPumpJob.setMaxRows(maxRows);
            dataPumpJob.schedule();
        }
    }

    private void clearMetaData()
    {
        this.metaColumns = new DBDAttributeBinding[0];
        this.dataFilter = new DBDDataFilter();
    }

    private void clearData()
    {
        // Refresh all rows
        this.releaseAll();
        this.origRows = new ArrayList<Object[]>();
        this.curRows = new ArrayList<Object[]>();
        this.curRowNum = -1;
        this.curColNum = -1;

        clearEditedData();

        hasData = false;
    }

    private void clearEditedData()
    {
        this.editedValues = new HashMap<GridPos, Object>();
        this.addedRows = new TreeSet<RowInfo>();
        this.removedRows = new TreeSet<RowInfo>();
    }

    boolean hasChanges()
    {
        return !editedValues.isEmpty() || !addedRows.isEmpty() || !removedRows.isEmpty();
    }

    public void applyChanges(DBRProgressMonitor monitor)
    {
        applyChanges(monitor, null);
    }

    /**
     * Saves changes to database
     * @param monitor monitor. If null then save will be executed in async job
     * @param listener finish listener
     */
    public void applyChanges(DBRProgressMonitor monitor, DBDValueListener listener)
    {
        if (!singleSourceCells) {
            log.error("Can't save data for resultset from multiple sources");
            return;
        }
        try {
            if (!checkVirtualEntityIdentifier()) {
                return;
            }
            if (getVirtualEntityIdentifier() != null) {
                // Ask user
                RunnableWithResult<Boolean> confirmer = new RunnableWithResult<Boolean>() {
                    @Override
                    public void run()
                    {
                        ConfirmVirtualKeyUsageDialog dialog = new ConfirmVirtualKeyUsageDialog(ResultSetViewer.this);
                        result = (dialog.open() == IDialogConstants.OK_ID);
                    }
                };
                UIUtils.runInUI(getControl().getShell(), confirmer);
                if (!confirmer.getResult()) {
                    return;
                }
            }
            new DataUpdater().applyChanges(monitor, listener);
        } catch (DBException e) {
            UIUtils.showErrorDialog(getControl().getShell(), "Apply changes error", "Error saving changes in database", e);
        }
    }

    public void rejectChanges()
    {
        new DataUpdater().rejectChanges();
    }

    public void pasteCellValue()
    {
        GridPos cell = getCurrentPosition();
        if (cell == null) {
            return;
        }
        DBDAttributeBinding metaColumn = metaColumns[cell.col];
        if (isColumnReadOnly(metaColumn)) {
            // No inline editors for readonly columns
            return;
        }
        try {
            Object newValue = metaColumn.getValueHandler().getValueFromClipboard(
                metaColumn.getAttribute(),
                getSpreadsheet().getClipboard());
            if (newValue == null) {
                return;
            }
            new ResultSetValueController(
                this.curRows.get(cell.row),
                cell.col,
                DBDValueController.EditType.NONE,
                null).updateValue(newValue);
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(site.getShell(), "Cannot replace cell value", null, e);
        }
    }

    private boolean isRowAdded(int row)
    {
        return addedRows.contains(new RowInfo(row));
    }

    void addNewRow(final boolean copyCurrent)
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
        final int currentRowNumber = rowNum;
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    // Copy cell values in new context
                    DBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.UTIL, CoreMessages.controls_resultset_viewer_add_new_row_context_name);
                    try {
                        if (copyCurrent && currentRowNumber >= 0 && currentRowNumber < curRows.size()) {
                            Object[] origRow = curRows.get(currentRowNumber);
                            for (int i = 0; i < metaColumns.length; i++) {
                                DBDAttributeBinding metaColumn = metaColumns[i];
                                if (metaColumn.getTableColumn().isSequence()) {
                                    // set autoincrement columns to null
                                    cells[i] = null;
                                } else {
                                    try {
                                        cells[i] = metaColumn.getValueHandler().copyValueObject(context, metaColumn.getTableColumn(), origRow[i]);
                                    } catch (DBCException e) {
                                        log.warn(e);
                                        cells[i] = DBUtils.makeNullValue(origRow[i]);
                                    }
                                }
                            }
                        } else {
                            // Initialize new values
                            for (int i = 0; i < metaColumns.length; i++) {
                                try {
                                    cells[i] = metaColumns[i].getValueHandler().createValueObject(context, metaColumns[i].getTableColumn());
                                } catch (DBCException e) {
                                    log.warn(e);
                                }
                            }
                        }
                    } finally {
                        context.close();
                    }
                }
            });
        } catch (InvocationTargetException e) {
            log.error("Could not create new row", e.getTargetException());
        } catch (InterruptedException e) {
            // interrupted - do nothing
        }
        curRows.add(rowNum, cells);

        addedRows.add(new RowInfo(rowNum));
        refreshSpreadsheet(true);
        updateEditControls();
        fireResultSetChange();
    }

    private void shiftRows(int rowNum, int delta)
    {
        // Slide all existing edited rows/cells down
        for (GridPos cell : editedValues.keySet()) {
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

    void deleteSelectedRows()
    {
        GridPos curPos = spreadsheet.getCursorPosition();
        TreeSet<Integer> rowNumbers = new TreeSet<Integer>();
        if (mode == ResultSetMode.RECORD) {
            rowNumbers.add(this.curRowNum);
        } else {
            for (GridPos pos : spreadsheet.getSelection()) {
                rowNumbers.add(pos.row);
            }
        }
        for (Iterator<Integer> iter = rowNumbers.iterator(); iter.hasNext(); ) {
            int rowNum = iter.next();
            if (rowNum < 0 || rowNum >= curRows.size()) {
                iter.remove();
            }
        }
        if (rowNumbers.isEmpty()) {
            return;
        }

        int rowsRemoved = 0;
        int lastRowNum = -1;
        for (Iterator<Integer> iter = rowNumbers.descendingIterator(); iter.hasNext(); ) {
            int rowNum = iter.next();
            if (rowNum > lastRowNum) {
                lastRowNum = rowNum;
            }
            RowInfo rowInfo = new RowInfo(rowNum);
            if (addedRows.contains(rowInfo)) {
                // Remove just added row
                addedRows.remove(rowInfo);
                cleanupRow(rowNum);
                rowsRemoved++;
            } else {
                // Mark row as deleted
                removedRows.add(rowInfo);
            }
        }
        // Move one row down (if we are in grid mode)
        if (mode == ResultSetMode.GRID && lastRowNum < spreadsheet.getItemCount() - 1) {
            curPos.row = lastRowNum - rowsRemoved + 1;
            spreadsheet.setCursor(curPos, false);
        }
        if (rowsRemoved > 0) {
            refreshSpreadsheet(true);
        } else {
            spreadsheet.redrawGrid();
        }
        updateEditControls();
        fireResultSetChange();
    }

    private void cleanupRow(int rowNum)
    {
        this.releaseRow(this.curRows.get(rowNum));
        this.curRows.remove(rowNum);
        this.shiftRows(rowNum, -1);
    }

    private boolean cleanupRows(Set<RowInfo> rows)
    {
        if (rows != null && !rows.isEmpty()) {
            // Remove rows (in descending order to prevent concurrent modification errors)
            int[] rowsToRemove = new int[rows.size()];
            int i = 0;
            for (RowInfo rowNum : rows) rowsToRemove[i++] = rowNum.row;
            Arrays.sort(rowsToRemove);
            for (i = rowsToRemove.length; i > 0; i--) {
                cleanupRow(rowsToRemove[i - 1]);
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

    private void resetValue(Object value)
    {
        if (value instanceof DBDContent) {
            ((DBDContent)value).resetContents();
        }
    }

    public static Image getAttributeImage(DBSAttributeBase column)
    {
        if (column instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)column).getObjectImage();
        } else {
            return DBIcon.TREE_COLUMN.getImage();
        }
    }

    private int getMetaColumnIndex(DBCAttributeMetaData attribute)
    {
        for (int i = 0; i < metaColumns.length; i++) {
            if (attribute == metaColumns[i].getAttribute()) {
                return i;
            }
        }
        return -1;
    }

    private int getMetaColumnIndex(DBSEntityAttribute column)
    {
        for (int i = 0; i < metaColumns.length; i++) {
            if (column == metaColumns[i].getTableColumn()) {
                return i;
            }
        }
        return -1;
    }

    private int getMetaColumnIndex(DBSEntity table, String columnName)
    {
        for (int i = 0; i < metaColumns.length; i++) {
            DBDAttributeBinding column = metaColumns[i];
            if ((table == null || column.getValueLocator().getEntity() == table) &&
                column.getColumnName().equals(columnName))
            {
                return i;
            }
        }
        return -1;
    }

    //////////////////////////////////
    // Virtual identifier management

    DBCEntityIdentifier getVirtualEntityIdentifier()
    {
        if (!singleSourceCells || CommonUtils.isEmpty(metaColumns)) {
            return null;
        }
        DBCEntityIdentifier identifier = metaColumns[0].getValueLocator().getEntityIdentifier();
        if (identifier.getReferrer() instanceof DBVEntityConstraint) {
            return identifier;
        } else {
            return null;
        }
    }

    boolean checkVirtualEntityIdentifier() throws DBException
    {
        // Check for value locators
        // Probably we have only virtual one with empty column set
        final DBCEntityIdentifier identifier = getVirtualEntityIdentifier();
        if (identifier != null) {
            if (CommonUtils.isEmpty(identifier.getAttributes())) {
                UIUtils.runInUI(getControl().getShell(), new Runnable() {
                    @Override
                    public void run()
                    {
                        // It is safe to use void monitor cos' it is virtual constraint
                        if (editEntityIdentifier(VoidProgressMonitor.INSTANCE)) {
                            try {
                                identifier.reloadAttributes(VoidProgressMonitor.INSTANCE, metaColumns[0].getAttribute().getEntity());
                            } catch (DBException e) {
                                log.error(e);
                            }
                        }
                    }
                });
            }
            return !CommonUtils.isEmpty(identifier.getAttributes());
        }
        return true;
    }

    boolean editEntityIdentifier(DBRProgressMonitor monitor)
    {
        DBVEntityConstraint constraint = (DBVEntityConstraint)getVirtualEntityIdentifier().getReferrer();

        EditConstraintDialog dialog = new EditConstraintDialog(
            getControl().getShell(),
            "Define virtual unique identifier",
            constraint,
            monitor);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        Collection<DBSEntityAttribute> uniqueColumns = dialog.getSelectedColumns();
        constraint.setAttributes(uniqueColumns);

        getDataSource().getContainer().persistConfiguration();

        return true;
    }

    void clearEntityIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        DBCEntityIdentifier identifier = metaColumns[0].getValueLocator().getEntityIdentifier();
        DBVEntityConstraint virtualKey = (DBVEntityConstraint) identifier.getReferrer();
        virtualKey.setAttributes(Collections.<DBSEntityAttribute>emptyList());
        identifier.reloadAttributes(monitor, metaColumns[0].getAttribute().getEntity());
        virtualKey.getParentObject().setProperty(DBVConstants.PROPERTY_USE_VIRTUAL_KEY_QUIET, null);

        getDataSource().getContainer().persistConfiguration();
    }

    /////////////////////////////
    // Value controller

    private class ResultSetValueController implements DBDAttributeController, DBDRowController {

        private DBDAttributeBinding[] columns;
        private Object[] curRow;
        private int columnIndex;
        private EditType editType;
        private Composite inlinePlaceholder;

        private ResultSetValueController(Object[] curRow, int columnIndex, EditType editType, Composite inlinePlaceholder) {
            this.columns = ResultSetViewer.this.metaColumns;
            this.curRow = curRow;
            this.columnIndex = columnIndex;
            this.editType = editType;
            this.inlinePlaceholder = inlinePlaceholder;
        }

        @Override
        public DBPDataSource getDataSource()
        {
            return ResultSetViewer.this.getDataSource();
        }

        @Override
        public DBDRowController getRow() {
            return this;
        }

        @Override
        public DBCAttributeMetaData getAttributeMetaData()
        {
            return columns[columnIndex].getAttribute();
        }

        @Override
        public String getColumnId() {
            String dsName = getDataSource().getContainer().getName();
            String catalogName = getAttributeMetaData().getCatalogName();
            String schemaName = getAttributeMetaData().getSchemaName();
            String tableName = getAttributeMetaData().getTableName();
            String columnName = getAttributeMetaData().getName();
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

        @Override
        public Object getValue()
        {
            return curRow[columnIndex];
        }

        @Override
        public void updateValue(Object value)
        {
            Object oldValue = curRow[columnIndex];
            if ((value instanceof DBDValue && value == oldValue) || !CommonUtils.equalObjects(oldValue, value)) {
                // If DBDValue was updated (kind of LOB?) or actual value was changed
                int rowIndex = getRowIndex(curRow);
                if (rowIndex >= 0) {
                    if (DBUtils.isNullValue(oldValue) && DBUtils.isNullValue(value)) {
                        // Both nulls - nothing to update
                        return;
                    }
                    // Do not add edited cell for new/deleted rows
                    if (!isRowAdded(rowIndex) && !isRowDeleted(rowIndex)) {
                        // Save old value
                        GridPos cell = new GridPos(columnIndex, rowIndex);
                        boolean cellWasEdited = editedValues.containsKey(cell); // use "contains" cos' old value may be "null".
                        Object oldOldValue = editedValues.get(cell);
                        if (cellWasEdited && !CommonUtils.equalObjects(oldValue, oldOldValue)) {
                            // Value rewrite - release previous stored old value
                            releaseValue(oldValue);
                        } else {
                            editedValues.put(cell, oldValue);
                        }
                    }
                    curRow[columnIndex] = value;
                    // Update controls
                    site.getShell().getDisplay().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            updateEditControls();
                            spreadsheet.redrawGrid();
                        }
                    });
                }
                fireResultSetChange();
            }
        }

        @Override
        public DBDValueLocator getValueLocator()
        {
            return columns[columnIndex].getValueLocator();
        }

        @Override
        public DBDValueHandler getValueHandler()
        {
            return columns[columnIndex].getValueHandler();
        }

        @Override
        public EditType getEditType()
        {
            return editType;
        }

        @Override
        public boolean isReadOnly()
        {
            return isColumnReadOnly(columns[columnIndex]);
        }

        @Override
        public IWorkbenchPartSite getValueSite()
        {
            return site;
        }

        @Override
        public Composite getEditPlaceholder()
        {
            return inlinePlaceholder;
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
            final int rowCount = spreadsheet.getItemCount();
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
            showCellEditor(true);
        }

        @Override
        public void registerEditor(DBDValueEditor editor) {
            openEditors.put(this, editor);
        }

        @Override
        public void unregisterEditor(DBDValueEditor editor) {
            openEditors.remove(this);
        }

        @Override
        public void showMessage(String message, boolean error)
        {
            setStatus(message, error);
        }

        @Override
        public Collection<DBCAttributeMetaData> getAttributesMetaData() {
            List<DBCAttributeMetaData> attributes = new ArrayList<DBCAttributeMetaData>();
            for (DBDAttributeBinding column : this.columns) {
                attributes.add(column.getAttribute());
            }
            return attributes;
        }

        @Override
        public DBCAttributeMetaData getAttributeMetaData(DBCEntityMetaData entity, String columnName)
        {
            for (DBDAttributeBinding column : columns) {
                if (column.getAttribute().getEntity() == entity && column.getColumnName().equals(columnName)) {
                    return column.getAttribute();
                }
            }
            return null;
        }

        @Override
        public Object getAttributeValue(DBCAttributeMetaData attribute)
        {
            for (int i = 0; i < columns.length; i++) {
                DBDAttributeBinding metaColumn = columns[i];
                if (metaColumn.getAttribute() == attribute) {
                    return curRow[i];
                }
            }
            log.warn("Unknown column value requested: " + attribute);
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

    private void fireResultSetChange() {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (ResultSetListener listener : listeners) {
                    listener.handleResultSetChange();
                }
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
        @Override
        public int compareTo(RowInfo o)
        {
            return row - o.row;
        }
    }

    static class TableRowInfo {
        DBSEntity table;
        DBCEntityIdentifier id;
        List<GridPos> tableCells = new ArrayList<GridPos>();

        TableRowInfo(DBSEntity table, DBCEntityIdentifier id) {
            this.table = table;
            this.id = id;
        }
    }

    static class DataStatementInfo {
        DBSManipulationType type;
        RowInfo row;
        DBSEntity table;
        List<DBDAttributeValue> keyAttributes = new ArrayList<DBDAttributeValue>();
        List<DBDAttributeValue> updateAttributes = new ArrayList<DBDAttributeValue>();
        boolean executed = false;
        Map<Integer, Object> updatedCells = new HashMap<Integer, Object>();

        DataStatementInfo(DBSManipulationType type, RowInfo row, DBSEntity table)
        {
            this.type = type;
            this.row = row;
            this.table = table;
        }
        boolean hasUpdateColumn(DBDAttributeBinding column)
        {
            for (DBDAttributeValue col : updateAttributes) {
                if (col.getAttribute() == column.getTableColumn()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class DataUpdater {

        private final Map<Integer, Map<DBSEntity, TableRowInfo>> updatedRows = new TreeMap<Integer, Map<DBSEntity, TableRowInfo>>();

        private final List<DataStatementInfo> insertStatements = new ArrayList<DataStatementInfo>();
        private final List<DataStatementInfo> deleteStatements = new ArrayList<DataStatementInfo>();
        private final List<DataStatementInfo> updateStatements = new ArrayList<DataStatementInfo>();

        private DataUpdater()
        {
        }

        /**
         * Applies changes.
         * @throws DBException
         * @param monitor
         * @param listener
         */
        void applyChanges(DBRProgressMonitor monitor, DBDValueListener listener)
            throws DBException
        {
            prepareDeleteStatements();
            prepareInsertStatements();
            prepareUpdateStatements();
            execute(monitor, listener);
        }

        private void prepareUpdateRows()
            throws DBException
        {
            if (editedValues == null) {
                return;
            }
            // Prepare rows
            for (GridPos cell : editedValues.keySet()) {
                Map<DBSEntity, TableRowInfo> tableMap = updatedRows.get(cell.row);
                if (tableMap == null) {
                    tableMap = new HashMap<DBSEntity, TableRowInfo>();
                    updatedRows.put(cell.row, tableMap);
                }

                DBDAttributeBinding metaColumn = metaColumns[cell.col];
                DBSEntity metaTable = metaColumn.getValueLocator().getEntity();
                TableRowInfo tableRowInfo = tableMap.get(metaTable);
                if (tableRowInfo == null) {
                    tableRowInfo = new TableRowInfo(metaTable, metaColumn.getValueLocator().getEntityIdentifier());
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
                DBSEntity table = metaColumns[0].getValueLocator().getEntity();
                DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.DELETE, rowNum, table);
                Collection<? extends DBSEntityAttribute> keyColumns = metaColumns[0].getValueLocator().getEntityIdentifier().getAttributes();
                for (DBSEntityAttribute column : keyColumns) {
                    int colIndex = getMetaColumnIndex(column);
                    if (colIndex < 0) {
                        throw new DBCException("Can't find meta column for ID column " + column.getName());
                    }
                    statement.keyAttributes.add(new DBDAttributeValue(column, curRows.get(rowNum.row)[colIndex]));
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
                DBSEntity table = metaColumns[0].getValueLocator().getEntity();
                DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.INSERT, rowNum, table);
                for (int i = 0; i < metaColumns.length; i++) {
                    DBDAttributeBinding column = metaColumns[i];
                    statement.keyAttributes.add(new DBDAttributeValue(column.getTableColumn(), cellValues[i]));
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
                Map<DBSEntity, TableRowInfo> tableMap = updatedRows.get(rowNum);
                for (DBSEntity table : tableMap.keySet()) {
                    TableRowInfo rowInfo = tableMap.get(table);
                    DataStatementInfo statement = new DataStatementInfo(DBSManipulationType.UPDATE, new RowInfo(rowNum), table);
                    // Updated columns
                    for (int i = 0; i < rowInfo.tableCells.size(); i++) {
                        GridPos cell = rowInfo.tableCells.get(i);
                        DBDAttributeBinding metaColumn = metaColumns[cell.col];
                        statement.updateAttributes.add(new DBDAttributeValue(metaColumn.getTableColumn(), curRows.get(rowNum)[cell.col]));
                    }
                    // Key columns
                    Collection<? extends DBCAttributeMetaData> idColumns = rowInfo.id.getResultSetColumns();
                    for (DBCAttributeMetaData idAttribute : idColumns) {
                        // Find meta column and add statement parameter
                        int columnIndex = getMetaColumnIndex(idAttribute);
                        if (columnIndex < 0) {
                            throw new DBCException("Can't find meta column for ID column " + idAttribute.getName());
                        }
                        DBDAttributeBinding metaColumn = metaColumns[columnIndex];
                        Object keyValue = curRows.get(rowNum)[columnIndex];
                        // Try to find old key oldValue
                        for (GridPos cell : editedValues.keySet()) {
                            if (cell.equals(columnIndex, rowNum)) {
                                keyValue = editedValues.get(cell);
                            }
                        }
                        statement.keyAttributes.add(new DBDAttributeValue(metaColumn.getTableColumn(), keyValue));
                    }
                    updateStatements.add(statement);
                }
            }
        }

        private void execute(DBRProgressMonitor monitor, final DBDValueListener listener)
            throws DBException
        {
            DataUpdaterJob job = new DataUpdaterJob(listener);
            if (monitor == null) {
                job.schedule();
            } else {
                job.run(monitor);
            }
        }

        public void rejectChanges()
        {
            if (editedValues != null) {
                for (GridPos cell : editedValues.keySet()) {
                    Object[] row = ResultSetViewer.this.curRows.get(cell.row);
                    resetValue(row[cell.col]);
                    row[cell.col] = editedValues.get(cell);
                }
                editedValues.clear();
            }
            boolean rowsChanged = cleanupRows(addedRows);
            // Remove deleted rows
            if (removedRows != null) {
                removedRows.clear();
            }

            refreshSpreadsheet(rowsChanged);
            fireResultSetChange();
            updateEditControls();
        }

        // Reflect data changes in viewer
        // Changes affects only rows which statements executed successfully
        private boolean reflectChanges()
        {
            boolean rowsChanged = false;
            if (editedValues != null) {
                for (Iterator<Map.Entry<GridPos, Object>> iter = editedValues.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry<GridPos, Object> entry = iter.next();
                    for (DataStatementInfo stat : updateStatements) {
                        if (stat.executed && stat.row.row == entry.getKey().row && stat.hasUpdateColumn(metaColumns[entry.getKey().col])) {
                            reflectKeysUpdate(stat);
                            iter.remove();
                            break;
                        }
                    }
                }
                //editedValues.clear();
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
                            cleanupRow(row.row);
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
                super(CoreMessages.controls_resultset_viewer_job_update, DBIcon.SQL_EXECUTE.getImageDescriptor(), ResultSetViewer.this.getDataSource());
                this.listener = listener;
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor)
            {
                ResultSetViewer.this.updateInProgress = true;
                try {
                    final Throwable error = executeStatements(monitor);

                    UIUtils.runInUI(ResultSetViewer.this.site.getShell(), new Runnable() {
                        @Override
                        public void run() {
                            boolean rowsChanged = false;
                            if (DataUpdaterJob.this.autocommit || error == null) {
                                rowsChanged = reflectChanges();
                            }
                            if (!getControl().isDisposed()) {
                                //releaseStatements();
                                refreshSpreadsheet(rowsChanged);
                                updateEditControls();
                                if (error == null) {
                                    setStatus(
                                            NLS.bind(CoreMessages.controls_resultset_viewer_status_inserted_,
                                                    new Object[] {DataUpdaterJob.this.insertCount, DataUpdaterJob.this.deleteCount, DataUpdaterJob.this.updateCount}),
                                            false);
                                } else {
                                    UIUtils.showErrorDialog(ResultSetViewer.this.site.getShell(), "Data error", "Error synchronizing data with database", error);
                                    setStatus(error.getMessage(), true);
                                }
                            }
                            fireResultSetChange();
                        }
                    });
                    if (this.listener != null) {
                        this.listener.onUpdate(error == null);
                    }

                }
                finally {
                    ResultSetViewer.this.updateInProgress = false;
                }
                return Status.OK_STATUS;
            }

            private Throwable executeStatements(DBRProgressMonitor monitor)
            {
                DBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.UTIL, CoreMessages.controls_resultset_viewer_execute_statement_context_name);
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
                        monitor.beginTask(CoreMessages.controls_resultset_viewer_monitor_aply_changes, DataUpdater.this.deleteStatements.size() + DataUpdater.this.insertStatements.size() + DataUpdater.this.updateStatements.size());

                        for (DataStatementInfo statement : DataUpdater.this.deleteStatements) {
                            if (monitor.isCanceled()) break;
                            DBSDataContainer dataContainer = (DBSDataContainer)statement.table;
                            try {
                                deleteCount += dataContainer.deleteData(context, statement.keyAttributes);
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
                                insertCount += dataContainer.insertData(context, statement.keyAttributes, new KeyDataReceiver(statement));
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
                                this.updateCount += dataContainer.updateData(context, statement.keyAttributes, statement.updateAttributes, new KeyDataReceiver(statement));
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

        @Override
        public void fetchStart(DBCExecutionContext context, DBCResultSet resultSet)
            throws DBCException
        {

        }

        @Override
        public void fetchRow(DBCExecutionContext context, DBCResultSet resultSet)
            throws DBCException
        {
            DBCResultSetMetaData rsMeta = resultSet.getResultSetMetaData();
            List<DBCAttributeMetaData> keyAttributes = rsMeta.getAttributes();
            for (int i = 0; i < keyAttributes.size(); i++) {
                DBCAttributeMetaData keyAttribute = keyAttributes.get(i);
                DBDValueHandler valueHandler = DBUtils.getColumnValueHandler(context, keyAttribute);
                Object keyValue = valueHandler.getValueObject(context, resultSet, keyAttribute, i);
                if (keyValue == null) {
                    // [MSSQL] Sometimes driver returns empty list of generated keys if
                    // table has auto-increment columns and user performs simple row update
                    // Just ignore such empty keys. We can't do anything with them anyway
                    continue;
                }
                boolean updated = false;
                if (!CommonUtils.isEmpty(keyAttribute.getName())) {
                    int colIndex = getMetaColumnIndex(statement.table, keyAttribute.getName());
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
                        DBDAttributeBinding column = metaColumns[k];
                        if (column.getTableColumn().isSequence()) {
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
                    log.debug("Could not find target column for autogenerated key '" + keyAttribute.getName() + "'");
                }
            }
        }

        @Override
        public void fetchEnd(DBCExecutionContext context)
            throws DBCException
        {

        }

        @Override
        public void close()
        {
        }
    }

    private class ContentProvider implements IGridContentProvider {

        @Override
        public GridPos getSize()
        {
            if (mode == ResultSetMode.RECORD) {
                return new GridPos(
                    1,
                    metaColumns.length);
            } else {
                return new GridPos(
                    metaColumns.length,
                    curRows.size());
            }
        }

        @Override
        public Object getElement(GridPos pos)
        {
            if (mode == ResultSetMode.RECORD) {
                return curRows.get(curRowNum)[pos.row];
            } else {
                return curRows.get(pos.row)[pos.col];
            }
        }

        @Override
        public void updateColumn(GridColumn column)
        {
            if (mode == ResultSetMode.RECORD) {
                column.setSort(SWT.NONE);
            } else {
                column.setSort(SWT.DEFAULT);
                int index = column.getIndex();
                for (DBQOrderColumn co : dataFilter.getOrderColumns()) {
                    if (getMetaColumnIndex(null, co.getColumnName()) == index) {
                        column.setSort(co.isDescending() ? SWT.UP : SWT.DOWN);
                        break;
                    }
                }
                column.setSortRenderer(new ResultSetSortRenderer(column));
            }
        }

        @Override
        public Object[] getElements(Object inputElement)
        {
            if (mode == ResultSetMode.RECORD) {
                return curRows.get(curRowNum);
            } else {
                int rowNum = ((Number) inputElement).intValue();
                return curRows.get(rowNum);
            }
        }

        @Override
        public void dispose()
        {
        }

        @Override
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

            if (rowNum == curRows.size() - 1 && (mode == ResultSetMode.RECORD || spreadsheet.isRowVisible(rowNum)) && dataReceiver.isHasMoreData()) {
                readNextSegment();
            }

            if (formatString) {
                return valueHandler.getValueDisplayString(metaColumns[cell.col].getAttribute(), value);
            } else {
                return value;
            }
        }

        @Override
        public Image getImage(Object element)
        {
            GridPos cell = (GridPos)element;
            DBDAttributeBinding attr;
            if (mode == ResultSetMode.RECORD) {
                if (cell.row >= metaColumns.length) {
                    return null;
                }
                attr = metaColumns[cell.row];
            } else {
                if (cell.col >= metaColumns.length) {
                    return null;
                }
                attr = metaColumns[cell.col];
            }
            if ((attr.getValueHandler().getFeatures() & DBDValueHandler.FEATURE_SHOW_ICON) != 0) {
                return getAttributeImage(attr.getAttribute());
            } else {
                return null;
            }
        }

        @Override
        public String getText(Object element)
        {
            return String.valueOf(getValue(element, true));
        }

        @Override
        public Color getForeground(Object element)
        {
            Object value = getValue(element, false);
            if (DBUtils.isNullValue(value)) {
                return foregroundNull;
            } else {
                return null;
            }
        }

        @Override
        public Color getBackground(Object element)
        {
            GridPos cell = translateGridPos((GridPos)element);
            if (isRowAdded(cell.row)) {
                return backgroundAdded;
            }
            if (isRowDeleted(cell.row)) {
                return backgroundDeleted;
            }
            if (isCellModified(cell)) {
                return backgroundModified;
            }
            return null;
        }
    }

    private class ColumnLabelProvider extends LabelProvider implements IFontProvider {
        @Override
        public Image getImage(Object element)
        {
            if (mode == ResultSetMode.GRID) {
                int colNumber = ((Number)element).intValue();
                return getAttributeImage(metaColumns[colNumber].getAttribute());
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int colNumber = ((Number)element).intValue();
            if (mode == ResultSetMode.RECORD) {
                if (colNumber == 0) {
                    return CoreMessages.controls_resultset_viewer_value;
                } else {
                    log.warn("Bad column index: " + colNumber);
                    return null;
                }
            } else {
                DBDAttributeBinding metaColumn = metaColumns[colNumber];
                DBCAttributeMetaData attribute = metaColumn.getAttribute();
                if (CommonUtils.isEmpty(attribute.getLabel())) {
                    return metaColumn.getColumnName();
                } else {
                    return attribute.getLabel();
                }
/*
                return CommonUtils.isEmpty(metaColumn.getMetaData().getTableName()) ?
                    metaColumn.getMetaData().getName() :
                    metaColumn.getMetaData().getTableName() + "." + metaColumn.getMetaData().getName();
*/
            }
        }

        @Override
        public Font getFont(Object element)
        {
            int colNumber = ((Number)element).intValue();
            if (mode == ResultSetMode.GRID) {
                if (dataFilter.getFilterColumn(metaColumns[colNumber].getColumnName()) != null) {
                    return boldFont;
                }
            }
            return null;
        }
    }

    private class RowLabelProvider extends LabelProvider {
        @Override
        public Image getImage(Object element)
        {
            if (mode == ResultSetMode.RECORD) {
                int rowNumber = ((Number) element).intValue();
                return getAttributeImage(metaColumns[rowNumber].getAttribute());
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int rowNumber = ((Number) element).intValue();
            if (mode == ResultSetMode.RECORD) {
                return metaColumns[rowNumber].getColumnName();
            } else {
                return String.valueOf(rowNumber + 1);
            }
        }
    }

    private class ConfigAction extends Action implements IMenuCreator {
        public ConfigAction()
        {
            super(CoreMessages.controls_resultset_viewer_action_options, IAction.AS_DROP_DOWN_MENU);
            setImageDescriptor(DBIcon.CONFIGURATION.getImageDescriptor());
        }

        @Override
        public IMenuCreator getMenuCreator()
        {
            return this;
        }

        @Override
        public void runWithEvent(Event event)
        {
            Menu menu = getMenu(getControl());
            if (menu != null && event.widget instanceof ToolItem) {
                Rectangle bounds = ((ToolItem) event.widget).getBounds();
                Point point = ((ToolItem) event.widget).getParent().toDisplay(bounds.x, bounds.y + bounds.height);
                menu.setLocation(point.x, point.y);
                menu.setVisible(true);
            }
        }

        @Override
        public void dispose()
        {

        }

        @Override
        public Menu getMenu(Control parent)
        {
            ResultSetViewer.this.getControl().setFocus();
            MenuManager menuManager = new MenuManager();
            menuManager.add(new ShowFiltersAction());
            menuManager.add(new Separator());
            menuManager.add(new VirtualKeyEditAction(true));
            menuManager.add(new VirtualKeyEditAction(false));
            menuManager.add(new DictionaryEditAction());
            menuManager.add(new Separator());
            menuManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_MODE, CommandContributionItem.STYLE_CHECK));
            menuManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_PREVIEW, CommandContributionItem.STYLE_CHECK));
            menuManager.add(new Separator());
            menuManager.add(new Action("Preferences") {
                @Override
                public void run()
                {
                    UIUtils.showPreferencesFor(
                        getControl().getShell(),
                        ResultSetViewer.this,
                        PrefPageDatabaseGeneral.PAGE_ID);
                }
            });
            return menuManager.createContextMenu(parent);
        }

        @Override
        public Menu getMenu(Menu parent)
        {
            return null;
        }

    }

    private class ShowFiltersAction extends Action {
        public ShowFiltersAction()
        {
            super(CoreMessages.controls_resultset_viewer_action_custom_filter);
        }

        @Override
        public void run()
        {
            new ResultSetFilterDialog(ResultSetViewer.this).open();
        }
    }

    private enum FilterByColumnType {
        VALUE(DBIcon.FILTER_VALUE.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault)
            {
                Object value = viewer.curRows.get(viewer.getCurrentRow())[viewer.getMetaColumnIndex(column.getAttribute())];
                if (value instanceof Number) {
                    return value.toString();
                }
                return column.getValueHandler().getValueDisplayString(column.getAttribute(), value);
            }
        },
        INPUT(DBIcon.FILTER_INPUT.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault)
            {
                if (useDefault) {
                    return "..";
                } else {
                    return EditTextDialog.editText(
                        viewer.getControl().getShell(),
                        "Enter value",
                        "");
                }
            }
        },
        CLIPBOARD(DBIcon.FILTER_CLIPBOARD.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault)
            {
                return column.getValueHandler().getValueFromClipboard(
                    column.getAttribute(),
                    viewer.getSpreadsheet().getClipboard());
            }
        },
        NONE(DBIcon.FILTER_VALUE.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault)
            {
                return "";
            }
        };

        final ImageDescriptor icon;

        private FilterByColumnType(ImageDescriptor icon)
        {
            this.icon = icon;
        }
        abstract Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault);
    }

    private class FilterByColumnAction extends Action {
        private final String pattern;
        private final FilterByColumnType type;
        private final DBDAttributeBinding column;
        public FilterByColumnAction(String pattern, FilterByColumnType type, DBDAttributeBinding column)
        {
            super(DBUtils.getQuotedIdentifier(column.getTableColumn()) + " " + translateFilterPattern(pattern, type, column), type.icon);
            this.pattern = pattern;
            this.type = type;
            this.column = column;
        }

        @Override
        public void run()
        {
            Object value = type.getValue(ResultSetViewer.this, column, false);
            if (value == null) {
                return;
            }
            String stringValue = pattern.replace("?", value.toString());
            DBDDataFilter filter = getDataFilter();
            DBQCondition filterColumn = filter.getFilterColumn(column.getColumnName());
            if (filterColumn == null) {
                filterColumn = new DBQCondition(column.getColumnName(), stringValue);
                filter.addFilterColumn(filterColumn);
            } else {
                filterColumn.setCondition(stringValue);
            }
            refresh();
        }
    }

    private class FilterResetColumnAction extends Action {
        private final DBDAttributeBinding column;
        public FilterResetColumnAction(DBDAttributeBinding column)
        {
            super("Remove filter", DBIcon.REVERT.getImageDescriptor());
            this.column = column;
        }

        @Override
        public void run()
        {
            DBDDataFilter filter = getDataFilter();
            DBQCondition filterColumn = filter.getFilterColumn(column.getColumnName());
            if (filterColumn != null) {
                filter.removeFilterColumn(filterColumn);
            }
            refresh();
        }
    }

    private class VirtualKeyEditAction extends Action {
        private boolean define;

        public VirtualKeyEditAction(boolean define)
        {
            super(define ? "Define virtual unique key" : "Clear virtual unique key");
            this.define = define;
        }

        @Override
        public boolean isEnabled()
        {
            DBCEntityIdentifier identifier = getVirtualEntityIdentifier();
            return identifier != null && (define || !CommonUtils.isEmpty(identifier.getAttributes()));
        }

        @Override
        public void run()
        {
            DBeaverUI.runUIJob("Edit virtual key", new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        if (define) {
                            editEntityIdentifier(monitor);
                        } else {
                            clearEntityIdentifier(monitor);
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
    }

    private class DictionaryEditAction extends Action {
        public DictionaryEditAction()
        {
            super("Define dictionary");
        }

        @Override
        public void run()
        {

        }

        @Override
        public boolean isEnabled()
        {
            return false;
        }
    }

}
