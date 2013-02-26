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
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
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
import org.jkiss.dbeaver.model.virtual.DBVConstants;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.tools.data.wizard.DataExportProvider;
import org.jkiss.dbeaver.tools.data.wizard.DataExportWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.AbstractRenderer;
import org.jkiss.dbeaver.ui.controls.spreadsheet.ISpreadsheetController;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseGeneral;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.properties.tabbed.PropertyPageStandard;
import org.jkiss.dbeaver.utils.ContentUtils;
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
    private ResultSetValueController panelValueController;

    private static final String VIEW_PANEL_VISIBLE = "viewPanelVisible";
    private static final String VIEW_PANEL_RATIO = "viewPanelRatio";

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

    private final IWorkbenchPartSite site;
    private final Composite viewerPanel;
    private final Text filtersText;
    private Text statusLabel;

    private final SashForm resultsSash;
    private final Spreadsheet spreadsheet;
    private final ViewValuePanel previewPane;

    private final ResultSetProvider resultSetProvider;
    private final ResultSetDataReceiver dataReceiver;
    private final IThemeManager themeManager;
    private ToolBarManager toolBarManager;

    // Columns
    private DBDAttributeBinding[] columns = new DBDAttributeBinding[0];
    private DBDAttributeBinding[] visibleColumns = new DBDAttributeBinding[0];
    private DBDDataFilter dataFilter = new DBDDataFilter();
    private boolean singleSourceCells;

    // Data
    private List<Object[]> origRows = new ArrayList<Object[]>();
    private List<Object[]> curRows = new ArrayList<Object[]>();
    // Current row number (for record mode)
    private int curRowNum = -1;
    private int curColNum = -1;
    private boolean hasData = false;
    // Flag saying that edited values update is in progress
    private boolean updateInProgress = false;
    private ResultSetMode mode;

    // Edited rows and cells
    private final Set<RowInfo> addedRows = new TreeSet<RowInfo>();
    private final Set<RowInfo> removedRows = new TreeSet<RowInfo>();
    private final Map<GridPos, Object> editedValues = new HashMap<GridPos, Object>();

    private final Map<ResultSetValueController, DBDValueEditorStandalone> openEditors = new HashMap<ResultSetValueController, DBDValueEditorStandalone>();

    // UI modifiers
    private final Color colorRed;
    private final Color backgroundAdded;
    private final Color backgroundDeleted;
    private final Color backgroundModified;
    private final Color foregroundNull;
    private final Font boldFont;

    private ResultSetDataPumpJob dataPumpJob;

    private final List<ResultSetListener> listeners = new ArrayList<ResultSetListener>();

    //private static volatile boolean adapterRegistered;

    public ResultSetViewer(Composite parent, IWorkbenchPartSite site, ResultSetProvider resultSetProvider)
    {
        super();

/*
        if (!adapterRegistered) {
            ResultSetAdapterFactory nodesAdapter = new ResultSetAdapterFactory();
            IAdapterManager mgr = Platform.getAdapterManager();
            mgr.registerAdapters(nodesAdapter, ResultSetProvider.class);
            mgr.registerAdapters(nodesAdapter, IPageChangeProvider.class);
            adapterRegistered = true;
        }
*/

        this.site = site;
        this.mode = ResultSetMode.GRID;
        this.resultSetProvider = resultSetProvider;
        this.dataReceiver = new ResultSetDataReceiver(this);

        this.colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
        final ISharedTextColors sharedColors = DBeaverUI.getSharedTextColors();
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

            final IPreferenceStore preferences = getPreferences();
            int ratio = preferences.getInt(VIEW_PANEL_RATIO);
            boolean viewPanelVisible = preferences.getBoolean(VIEW_PANEL_VISIBLE);
            if (ratio <= 0) {
                ratio = 750;
            }
            resultsSash.setWeights(new int[]{ratio, 1000 - ratio});
            if (!viewPanelVisible) {
                resultsSash.setMaximizedControl(spreadsheet);
            }
            previewPane.addListener(SWT.Resize, new Listener() {
                @Override
                public void handleEvent(Event event)
                {
                    DBPDataSource dataSource = getDataSource();
                    if (dataSource != null) {
                        if (!resultsSash.isDisposed()) {
                            int[] weights = resultsSash.getWeights();
                            int ratio = weights[0];
                            preferences.setValue(VIEW_PANEL_RATIO, ratio);
                        }
                    }
                }
            });
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
        this.spreadsheet.setTopLeftRenderer(new TopLeftRenderer(this));
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

        this.spreadsheet.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                fireSelectionChanged(new SelectionChangedEvent(ResultSetViewer.this, new ResultSetSelectionImpl()));
            }
        });
    }

    IPreferenceStore getPreferences()
    {
        return DBeaverCore.getGlobalPreferenceStore();
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return getDataContainer().getDataSource();
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IPropertySheetPage.class) {
            PropertyPageStandard page = new PropertyPageStandard();
            page.setPropertySourceProvider(new IPropertySourceProvider() {
                @Override
                public IPropertySource getPropertySource(Object object)
                {
                    if (object instanceof GridPos) {
                        final GridPos cell = translateGridPos((GridPos)object);
                        boolean noCellSelected = (cell.row < 0 || curRows.size() <= cell.row || cell.col < 0 || cell.col >= visibleColumns.length);
                        if (!noCellSelected) {
                            final ResultSetValueController valueController = new ResultSetValueController(
                                curRows.get(cell.row),
                                cell.col,
                                DBDValueController.EditType.NONE,
                                null);
                            PropertyCollector props = new PropertyCollector(valueController.getAttribute(), false);
                            props.collectProperties();
                            valueController.getValueHandler().fillProperties(props, valueController);
                            return props;
                        }
                    }
                    return null;
                }
            });
            return page;
        }
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
            if (col >= 0 && row >= 0) {
                previewValue();
            }
        }
    }

    private void updateRecordMode()
    {
        int oldColNum = this.curColNum;
        this.initResultSet();
        this.curColNum = oldColNum;
        spreadsheet.setCursor(new GridPos(0, oldColNum), false);
    }

    void updateEditControls()
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

    void refreshSpreadsheet(boolean rowsChanged)
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

    private void updateStatusMessage()
    {
        if (curRows.isEmpty()) {
            if (CommonUtils.isEmpty(visibleColumns)) {
                setStatus("Empty");
            } else {
                setStatus(CoreMessages.controls_resultset_viewer_status_no_data);
            }
        } else {
            if (mode == ResultSetMode.RECORD) {
                this.resetRecordHeaderWidth();
                setStatus(CoreMessages.controls_resultset_viewer_status_row + (curRowNum + 1) + "/" + curRows.size());
            } else {
                setStatus(String.valueOf(curRows.size()) + CoreMessages.controls_resultset_viewer_status_rows_fetched);
            }
        }
    }

    // Update all columns ordering
    private void resetColumnOrdering()
    {
        if (!spreadsheet.isDisposed() && visibleColumns != null && mode == ResultSetMode.GRID) {
            for (int i = 0, metaColumnsLength = visibleColumns.length; i < metaColumnsLength; i++) {
                DBDAttributeBinding column = visibleColumns[i];
                DBQOrderColumn columnOrder = dataFilter.getOrderColumn(column.getAttributeName());
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
        if (!curRows.isEmpty()) {
            // Fix row number if needed
            if (oldRowNum < 0) {
                oldRowNum = this.curRowNum = 0;
            } else if (oldRowNum >= curRows.size()) {
                oldRowNum = this.curRowNum = curRows.size() - 1;
            }
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
        previewValue();
    }

    private void resetRecordHeaderWidth()
    {
        // Calculate width of spreadsheet panel - use longest column title
        int defaultWidth = 0;
        GC gc = new GC(spreadsheet);
        gc.setFont(spreadsheet.getFont());
        for (DBDAttributeBinding column : visibleColumns) {
            Point ext = gc.stringExtent(column.getAttributeName());
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
        getPreferences().setValue(VIEW_PANEL_VISIBLE, isPreviewVisible());

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
        if (!cell.isValid() || cell.col >= visibleColumns.length || cell.row >= curRows.size()) {
            previewPane.clearValue();
            return;
        }
        if (panelValueController == null || panelValueController.columnIndex != cell.col) {
            panelValueController = new ResultSetValueController(
                curRows.get(cell.row),
                cell.col,
                DBDValueController.EditType.PANEL,
                previewPane.getViewPlaceholder());
        } else {
            panelValueController.setCurRow(curRows.get(cell.row));
        }
        previewPane.viewValue(panelValueController);
    }

    ////////////////////////////////////////////////////////////
    // Misc

    private void dispose()
    {
        closeEditors();
        clearData();
        clearMetaData();

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
        Color previewBack = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_PREVIEW_BACK);
        if (previewBack != null) {
            this.previewPane.getViewPlaceholder().setBackground(previewBack);
            for (Control control : this.previewPane.getViewPlaceholder().getChildren()) {
                control.setBackground(previewBack);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().equals(IThemeManager.CHANGE_CURRENT_THEME)
            || event.getProperty().equals(ThemeConstants.FONT_SQL_RESULT_SET)
            || event.getProperty().equals(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_BACK)
            || event.getProperty().equals(ThemeConstants.COLOR_SQL_RESULT_SET_SELECTION_FORE)
            || event.getProperty().equals(ThemeConstants.COLOR_SQL_RESULT_SET_PREVIEW_BACK))
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
            column = pos.row;
        }
        return column < 0 || column >= visibleColumns.length || isColumnReadOnly(visibleColumns[column]);
    }

    boolean isColumnReadOnly(DBDAttributeBinding column)
    {
        if (isReadOnly() || column.getRowIdentifier() == null || !(column.getRowIdentifier().getEntity() instanceof DBSDataContainer)) {
            return true;
        }
        DBSDataContainer dataContainer = (DBSDataContainer) column.getRowIdentifier().getEntity();
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

    Object[] getRowData(int index)
    {
        return curRows.get(index);
    }

    int getRowIndex(Object[] row)
    {
        return curRows.indexOf(row);
    }

    Set<RowInfo> getAddedRows()
    {
        return addedRows;
    }

    Set<RowInfo> getRemovedRows()
    {
        return removedRows;
    }

    Map<GridPos, Object> getEditedValues()
    {
        return editedValues;
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
        if (this.visibleColumns == null || this.visibleColumns.length != columns.length) {
            update = true;
        } else {
            for (int i = 0; i < this.visibleColumns.length; i++) {
                if (!this.visibleColumns[i].getMetaAttribute().equals(columns[i].getMetaAttribute())) {
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
            this.columns = this.visibleColumns = columns;
            this.panelValueController = null;
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
            for (DBDAttributeBinding column : visibleColumns) {
                if (isColumnReadOnly(column)) {
                    break;
                }
                if (sourceTable == null) {
                    sourceTable = column.getRowIdentifier().getEntity();
                } else if (sourceTable != column.getRowIdentifier().getEntity()) {
                    singleSourceCells = false;
                    break;
                }
            }

            this.initResultSet();
        } else {
            this.refreshSpreadsheet(true);
        }

        hasData = true;
        updateEditControls();
    }

    public void appendData(List<Object[]> rows)
    {
        origRows.addAll(rows);
        curRows.addAll(rows);
        refreshSpreadsheet(true);

        setStatus(NLS.bind(CoreMessages.controls_resultset_viewer_status_rows_size, curRows.size(), rows.size()));
    }

    private void closeEditors() {
        List<DBDValueEditorStandalone> editors = new ArrayList<DBDValueEditorStandalone>(openEditors.values());
        for (DBDValueEditorStandalone editor : editors) {
            editor.closeValueEditor();
        }
        if (!openEditors.isEmpty()) {
            log.warn("Some value editors are still registered at result set: " + openEditors.size());
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
        }

        spreadsheet.reinitState();

        spreadsheet.setRedraw(true);

        this.updateFiltersText();
        this.updateStatusMessage();
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

    @Override
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

    void setUpdateInProgress(boolean updateInProgress)
    {
        this.updateInProgress = updateInProgress;
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

    /**
     * Returns single source of this result set. Usually it is a table.
     * If result set is a result of joins or contains synthetic columns then
     * single source is null. If driver doesn't support meta information
     * for queries then is will null.
     * @return single source entity
     */
    public DBSEntity getSingleSource()
    {
        if (!singleSourceCells) {
            return null;
        }
        return visibleColumns[0].getRowIdentifier().getEntity();
    }

    @Override
    public boolean isInsertable()
    {
        return
            !isReadOnly() &&
            singleSourceCells &&

            !CommonUtils.isEmpty(visibleColumns);
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
        if (cell.row < 0 || cell.row >= curRows.size() || cell.col < 0 || cell.col >= visibleColumns.length) {
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
        DBDAttributeBinding metaColumn = visibleColumns[cell.col];
        final int handlerFeatures = metaColumn.getValueHandler().getFeatures();
        if (handlerFeatures == DBDValueHandler.FEATURE_NONE) {
            return false;
        }
        if (inline &&
            (handlerFeatures & DBDValueHandler.FEATURE_INLINE_EDITOR) == 0 &&
            (handlerFeatures & DBDValueHandler.FEATURE_VIEWER) != 0)
        {
            // Inline editor isn't supported but panel viewer is
            // Enable panel
            if (!isPreviewVisible()) {
                togglePreview();
            }
            return true;
        }
        if (isColumnReadOnly(metaColumn) && inline) {
            // No inline editors for readonly columns
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
        final DBDValueEditor editor;
        try {
            editor = metaColumn.getValueHandler().createEditor(valueController);
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(site.getShell(), "Cannot edit value", null, e);
            return false;
        }
        if (editor instanceof DBDValueEditorStandalone) {
            valueController.registerEditor((DBDValueEditorStandalone)editor);
            // show dialog in separate job to avoid block
            new UIJob("Open separate editor") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor)
                {
                    ((DBDValueEditorStandalone)editor).showValueEditor();
                    return Status.OK_STATUS;
                }
            }.schedule();
            //((DBDValueEditorStandalone)editor).showValueEditor();
        } else {
            // Set editable value
            if (editor != null) {
                editor.refreshValue();
            }
        }
        if (inline) {
            if (editor != null) {
                spreadsheet.showCellEditor(focusCell, placeholder);
            } else {
                // No editor was created so just drop placeholder
                placeholder.dispose();
                // Probably we can just show preview panel
                if ((handlerFeatures & DBDValueHandler.FEATURE_VIEWER) != 0) {
                    // Inline editor isn't supported but panel viewer is
                    // Enable panel
                    if (!isPreviewVisible()) {
                        togglePreview();
                    }
                    return true;
                }
            }
        }
        return editor != null;
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
        previewValue();
    }

    @Override
    public void fillContextMenu(GridPos curCell, IMenuManager manager) {

        // Custom oldValue items
        final GridPos cell = translateGridPos(curCell);
        boolean noCellSelected = (cell.row < 0 || curRows.size() <= cell.row || cell.col < 0 || cell.col >= visibleColumns.length);
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
                            valueController.updateValue(
                                DBUtils.makeNullValue(valueController));
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
                visibleColumns[cell.col].getValueHandler().fillContextMenu(manager, valueController);
            }
            catch (Exception e) {
                log.error(e);
            }
        }

        if (!CommonUtils.isEmpty(visibleColumns)) {
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
        manager.add(new GroupMarker(ICommandIds.GROUP_TOOLS));
    }

    private void fillFiltersMenu(IMenuManager filtersMenu)
    {
        GridPos currentPosition = getCurrentPosition();
        int columnIndex = translateGridPos(currentPosition).col;
        if (supportsDataFilter() && columnIndex >= 0) {
            DBDAttributeBinding column = visibleColumns[columnIndex];
            if (column.getEntityAttribute() == null) {
                return;
            }
            DBSDataKind dataKind = column.getMetaAttribute().getDataKind();
            if (!column.getMetaAttribute().isRequired()) {
                filtersMenu.add(new FilterByColumnAction("IS NULL", FilterByColumnType.NONE, column));
                filtersMenu.add(new FilterByColumnAction("IS NOT NULL", FilterByColumnType.NONE, column));
            }
            for (FilterByColumnType type : FilterByColumnType.values()) {
                if (type == FilterByColumnType.NONE || (type == FilterByColumnType.VALUE && !isValidCell(currentPosition))) {
                    // Value filters are available only if certain cell is selected
                    continue;
                }
                filtersMenu.add(new Separator());
                if (type.getValue(this, column, true, DBDDisplayFormat.NATIVE) == null) {
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
            if (getDataFilter().getFilterColumn(column.getAttributeName()) != null) {
                filtersMenu.add(new FilterResetColumnAction(column));
            }
        }
        filtersMenu.add(new ShowFiltersAction());
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
        DBDAttributeBinding metaColumn = visibleColumns[column.getIndex()];
        DBQOrderColumn columnOrder = dataFilter.getOrderColumn(metaColumn.getAttributeName());
        //int newSort;
        if (columnOrder == null) {
            if (dataReceiver.isHasMoreData() && supportsDataFilter()) {
                if (!ConfirmationDialog.confirmActionWithParams(
                    spreadsheet.getShell(),
                    PrefConstants.CONFIRM_ORDER_RESULTSET,
                    metaColumn.getAttributeName()))
                {
                    return;
                }
            }
            columnOrder = new DBQOrderColumn(metaColumn.getAttributeName(), altPressed);
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

    @Override
    public Control getControl()
    {
        return this.viewerPanel;
    }

    public IWorkbenchPartSite getSite()
    {
        return site;
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
    public ResultSetSelection getSelection()
    {
        return new ResultSetSelectionImpl();
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
                    applyChanges(null, new ResultSetPersister.DataUpdateListener() {
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
                                ResultSetViewer.this.curRowNum = Math.min(oldPos.row, ResultSetViewer.this.curRows.size() - 1);
                                ResultSetViewer.this.curColNum = Math.min(oldPos.col, visibleColumns.length - 1);
                                if (mode == ResultSetMode.GRID) {
                                    spreadsheet.setCursor(new GridPos(curColNum, curRowNum), false);
                                } else {
                                    spreadsheet.setCursor(new GridPos(0, curColNum), false);
                                }
                                spreadsheet.setSelection(-1, -1);
                                updateStatusMessage();
                                previewValue();
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
        this.columns = this.visibleColumns = new DBDAttributeBinding[0];
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
        this.editedValues.clear();
        this.addedRows.clear();
        this.removedRows.clear();
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
     * @param listener finish listener (may be null)
     */
    public void applyChanges(DBRProgressMonitor monitor, ResultSetPersister.DataUpdateListener listener)
    {
        if (!singleSourceCells) {
            UIUtils.showErrorDialog(getControl().getShell(), "Apply changes error", "Can't save data for result set from multiple sources");
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
            new ResultSetPersister(this).applyChanges(monitor, listener);
        } catch (DBException e) {
            UIUtils.showErrorDialog(getControl().getShell(), "Apply changes error", "Error saving changes in database", e);
        }
    }

    public void rejectChanges()
    {
        new ResultSetPersister(this).rejectChanges();
    }

    public void copySelectionToClipboard(
        boolean copyHeader,
        boolean copyRowNumbers,
        boolean cut,
        String delimiter,
        DBDDisplayFormat format)
    {
        if (delimiter == null) {
            delimiter = "\t";
        }
        String lineSeparator = ContentUtils.getDefaultLineSeparator();
        List<Integer> colsSelected = new ArrayList<Integer>();
        int firstCol = Integer.MAX_VALUE, lastCol = Integer.MIN_VALUE;
        int firstRow = Integer.MAX_VALUE;
        Collection<GridPos> selection = spreadsheet.getSelection();
        for (GridPos pos : selection) {
            if (firstCol > pos.col) {
                firstCol = pos.col;
            }
            if (lastCol < pos.col) {
                lastCol = pos.col;
            }
            if (firstRow > pos.row) {
                firstRow = pos.row;
            }
            if (!colsSelected.contains(pos.col)) {
                colsSelected.add(pos.col);
            }
        }
        int rowNumber = 1;
        StringBuilder tdt = new StringBuilder();
        if (copyHeader) {
            if (copyRowNumbers) {
                tdt.append("-");
            }
            for (int colIndex : colsSelected) {
                GridColumn column = spreadsheet.getColumn(colIndex);
                if (tdt.length() > 0) {
                    tdt.append(delimiter);
                }
                tdt.append(column.getText());
            }
            tdt.append(lineSeparator);
        }
        if (copyRowNumbers) {
            tdt.append(rowNumber++).append(delimiter);
        }

        int prevRow = firstRow;
        int prevCol = firstCol;
        for (GridPos pos : selection) {
            if (pos.row > prevRow) {
                if (prevCol < lastCol) {
                    for (int i = prevCol; i < lastCol; i++) {
                        if (colsSelected.contains(i)) {
                            tdt.append(delimiter);
                        }
                    }
                }
                tdt.append(lineSeparator);
                if (copyRowNumbers) {
                    tdt.append(rowNumber++).append(delimiter);
                }
                prevRow = pos.row;
                prevCol = firstCol;
            }
            if (pos.col > prevCol) {
                for (int i = prevCol; i < pos.col; i++) {
                    if (colsSelected.contains(i)) {
                        tdt.append(delimiter);
                    }
                }
                prevCol = pos.col;
            }
            GridPos cellPos = translateGridPos(pos);
            Object[] curRow = curRows.get(cellPos.row);
            Object value = curRow[cellPos.col];
            String cellText = visibleColumns[cellPos.col].getValueHandler().getValueDisplayString(
                visibleColumns[cellPos.col].getMetaAttribute(),
                value,
                format);
            if (cellText != null) {
                tdt.append(cellText);
            }
            if (cut) {
                DBDValueController valueController = new ResultSetValueController(
                    curRow, cellPos.col, DBDValueController.EditType.NONE, null);
                if (!valueController.isReadOnly()) {
                    valueController.updateValue(DBUtils.makeNullValue(valueController));
                }
            }
        }
        if (tdt.length() > 0) {
            TextTransfer textTransfer = TextTransfer.getInstance();
            getSpreadsheet().getClipboard().setContents(
                new Object[]{tdt.toString()},
                new Transfer[]{textTransfer});
        }
    }

    public void pasteCellValue()
    {
        GridPos cell = getCurrentPosition();
        if (cell == null) {
            return;
        }
        cell = translateGridPos(cell);
        DBDAttributeBinding metaColumn = visibleColumns[cell.col];
        if (isColumnReadOnly(metaColumn)) {
            // No inline editors for readonly columns
            return;
        }
        try {
            Object newValue = getColumnValueFromClipboard(metaColumn);
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

    private Object getColumnValueFromClipboard(DBDAttributeBinding metaColumn) throws DBCException
    {
        DBCExecutionContext context = getDataSource().openContext(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, "Copy from clipboard");
        Object newValue;
        try {
            newValue = metaColumn.getValueHandler().getValueFromClipboard(
                context, metaColumn.getMetaAttribute(),
                getSpreadsheet().getClipboard());
        } finally {
            context.close();
        }
        return newValue;
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
        final Object[] cells = new Object[visibleColumns.length];
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
                            for (int i = 0; i < visibleColumns.length; i++) {
                                DBDAttributeBinding metaColumn = visibleColumns[i];
                                if (metaColumn.getEntityAttribute().isSequence()) {
                                    // set autoincrement columns to null
                                    cells[i] = null;
                                } else {
                                    try {
                                        cells[i] = metaColumn.getValueHandler().getValueFromObject(context, metaColumn.getEntityAttribute(), origRow[i], true);
                                    } catch (DBCException e) {
                                        log.warn(e);
                                        try {
                                            cells[i] = DBUtils.makeNullValue(context, metaColumn.getValueHandler(), metaColumn.getMetaAttribute());
                                        } catch (DBCException e1) {
                                            log.warn(e1);
                                        }
                                    }
                                }
                            }
                        } else {
                            // Initialize new values
                            for (int i = 0; i < visibleColumns.length; i++) {
                                DBDAttributeBinding metaColumn = visibleColumns[i];
                                try {
                                    cells[i] = DBUtils.makeNullValue(context, metaColumn.getValueHandler(), metaColumn.getMetaAttribute());
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

    void cleanupRow(int rowNum)
    {
        this.releaseRow(this.curRows.get(rowNum));
        this.curRows.remove(rowNum);
        this.shiftRows(rowNum, -1);
    }

    boolean cleanupRows(Set<RowInfo> rows)
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

    void releaseValue(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    void resetValue(Object value)
    {
        if (value instanceof DBDContent) {
            ((DBDContent)value).resetContents();
        }
    }

    static Image getTypeImage(DBSTypedObject column)
    {
        if (column instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)column).getObjectImage();
        } else {
            return DBIcon.TREE_COLUMN.getImage();
        }
    }

    public DBDAttributeBinding[] getVisibleColumns()
    {
        return visibleColumns;
    }

    public DBDAttributeBinding getAttributeBinding(DBSAttributeBase attribute)
    {
        for (DBDAttributeBinding binding : visibleColumns) {
            if (binding.getMetaAttribute() == attribute || binding.getEntityAttribute() == attribute) {
                return binding;
            }
        }
        return null;
    }

    int getMetaColumnIndex(DBCAttributeMetaData attribute)
    {
        for (int i = 0; i < visibleColumns.length; i++) {
            if (attribute == visibleColumns[i].getMetaAttribute()) {
                return i;
            }
        }
        return -1;
    }

    int getMetaColumnIndex(DBSEntityAttribute column)
    {
        for (int i = 0; i < visibleColumns.length; i++) {
            if (column == visibleColumns[i].getEntityAttribute()) {
                return i;
            }
        }
        return -1;
    }

    int getMetaColumnIndex(DBSAttributeBase attribute)
    {
        return attribute instanceof DBCAttributeMetaData ?
            getMetaColumnIndex((DBCAttributeMetaData) attribute) :
            getMetaColumnIndex((DBSEntityAttribute) attribute);
    }

    int getMetaColumnIndex(DBSEntity table, String columnName)
    {
        for (int i = 0; i < visibleColumns.length; i++) {
            DBDAttributeBinding column = visibleColumns[i];
            if ((table == null || column.getRowIdentifier().getEntity() == table) &&
                column.getAttributeName().equals(columnName))
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
        if (!singleSourceCells || CommonUtils.isEmpty(visibleColumns)) {
            return null;
        }
        DBDRowIdentifier rowIdentifier = visibleColumns[0].getRowIdentifier();
        DBCEntityIdentifier identifier = rowIdentifier == null ? null : rowIdentifier.getEntityIdentifier();
        if (identifier != null && identifier.getReferrer() instanceof DBVEntityConstraint) {
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
                        if (editEntityIdentifier()) {
                            try {
                                identifier.reloadAttributes(VoidProgressMonitor.INSTANCE, visibleColumns[0].getMetaAttribute().getEntity());
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

    boolean editEntityIdentifier()
    {
        DBVEntityConstraint constraint = (DBVEntityConstraint)getVirtualEntityIdentifier().getReferrer();

        EditConstraintDialog dialog = new EditConstraintDialog(
            getControl().getShell(),
            "Define virtual unique identifier",
            constraint);
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
        DBCEntityIdentifier identifier = visibleColumns[0].getRowIdentifier().getEntityIdentifier();
        DBVEntityConstraint virtualKey = (DBVEntityConstraint) identifier.getReferrer();
        virtualKey.setAttributes(Collections.<DBSEntityAttribute>emptyList());
        identifier.reloadAttributes(monitor, visibleColumns[0].getMetaAttribute().getEntity());
        virtualKey.getParentObject().setProperty(DBVConstants.PROPERTY_USE_VIRTUAL_KEY_QUIET, null);

        getDataSource().getContainer().persistConfiguration();
    }

    void fireResultSetChange() {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (ResultSetListener listener : listeners) {
                    listener.handleResultSetChange();
                }
            }
        }
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
            this.columns = ResultSetViewer.this.visibleColumns;
            this.curRow = curRow;
            this.columnIndex = columnIndex;
            this.editType = editType;
            this.inlinePlaceholder = inlinePlaceholder;
        }

        void setCurRow(Object[] curRow)
        {
            this.curRow = curRow;
        }

        @Override
        public DBPDataSource getDataSource()
        {
            return ResultSetViewer.this.getDataSource();
        }

        @Override
        public String getValueName()
        {
            return getAttribute().getName();
        }

        @Override
        public DBSTypedObject getValueType()
        {
            return getAttribute();
        }

        @Override
        public DBDRowController getRow() {
            return this;
        }

        @Override
        public DBCAttributeMetaData getAttribute()
        {
            return columns[columnIndex].getMetaAttribute();
        }

        @Override
        public String getColumnId() {
            String dsName = getDataSource().getContainer().getName();
            String catalogName = getAttribute().getCatalogName();
            String schemaName = getAttribute().getSchemaName();
            String tableName = getAttribute().getEntityName();
            String columnName = getAttribute().getName();
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
                            previewValue();
                        }
                    });
                }
                fireResultSetChange();
            }
        }

        @Override
        public DBDRowIdentifier getValueLocator()
        {
            return columns[columnIndex].getRowIdentifier();
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
        public ToolBar getEditToolBar()
        {
            return isPreviewVisible() ? previewPane.getToolBar() : null;
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

        public void registerEditor(DBDValueEditorStandalone editor) {
            openEditors.put(this, editor);
        }

        @Override
        public void unregisterEditor(DBDValueEditorStandalone editor) {
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
                attributes.add(column.getMetaAttribute());
            }
            return attributes;
        }

        @Override
        public DBCAttributeMetaData getAttributeMetaData(DBCEntityMetaData entity, String columnName)
        {
            for (DBDAttributeBinding column : columns) {
                if (column.getMetaAttribute().getEntity() == entity && column.getAttributeName().equals(columnName)) {
                    return column.getMetaAttribute();
                }
            }
            return null;
        }

        @Override
        public Object getAttributeValue(DBCAttributeMetaData attribute)
        {
            for (int i = 0; i < columns.length; i++) {
                DBDAttributeBinding metaColumn = columns[i];
                if (metaColumn.getMetaAttribute() == attribute) {
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

    static class TableRowInfo {
        DBSEntity table;
        DBCEntityIdentifier id;
        List<GridPos> tableCells = new ArrayList<GridPos>();

        TableRowInfo(DBSEntity table, DBCEntityIdentifier id) {
            this.table = table;
            this.id = id;
        }
    }

    private class ContentProvider implements IGridContentProvider {

        @Override
        public GridPos getSize()
        {
            if (mode == ResultSetMode.RECORD) {
                return new GridPos(
                    1,
                    visibleColumns.length);
            } else {
                return new GridPos(
                    visibleColumns.length,
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
                column.setSortRenderer(new SortRenderer(column));
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
                valueHandler = visibleColumns[cell.row].getValueHandler();
            } else {
                rowNum = cell.row;
                if (cell.row >= curRows.size()) {
                    log.warn("Bad grid row number: " + cell.row);
                    return null;
                }
                if (cell.col >= visibleColumns.length) {
                    log.warn("Bad grid column number: " + cell.col);
                    return null;
                }
                value = curRows.get(cell.row)[cell.col];
                valueHandler = visibleColumns[cell.col].getValueHandler();
            }

            if (rowNum == curRows.size() - 1 && (mode == ResultSetMode.RECORD || spreadsheet.isRowVisible(rowNum)) && dataReceiver.isHasMoreData()) {
                readNextSegment();
            }

            if (formatString) {
                return valueHandler.getValueDisplayString(visibleColumns[cell.col].getMetaAttribute(), value, DBDDisplayFormat.UI);
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
                if (cell.row >= visibleColumns.length) {
                    return null;
                }
                attr = visibleColumns[cell.row];
            } else {
                if (cell.col >= visibleColumns.length) {
                    return null;
                }
                attr = visibleColumns[cell.col];
            }
            if ((attr.getValueHandler().getFeatures() & DBDValueHandler.FEATURE_SHOW_ICON) != 0) {
                return getTypeImage(attr.getMetaAttribute());
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
                return getTypeImage(visibleColumns[colNumber].getMetaAttribute());
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
                DBDAttributeBinding metaColumn = visibleColumns[colNumber];
                DBCAttributeMetaData attribute = metaColumn.getMetaAttribute();
                if (CommonUtils.isEmpty(attribute.getLabel())) {
                    return metaColumn.getAttributeName();
                } else {
                    return attribute.getLabel();
                }
/*
                return CommonUtils.isEmpty(metaColumn.getMetaData().getEntityName()) ?
                    metaColumn.getMetaData().getName() :
                    metaColumn.getMetaData().getEntityName() + "." + metaColumn.getMetaData().getName();
*/
            }
        }

        @Override
        public Font getFont(Object element)
        {
            int colNumber = ((Number)element).intValue();
            if (mode == ResultSetMode.GRID) {
                if (dataFilter.getFilterColumn(visibleColumns[colNumber].getAttributeName()) != null) {
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
                return getTypeImage(visibleColumns[rowNumber].getMetaAttribute());
            }
            return null;
        }

        @Override
        public String getText(Object element)
        {
            int rowNumber = ((Number) element).intValue();
            if (mode == ResultSetMode.RECORD) {
                return visibleColumns[rowNumber].getAttributeName();
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
            String getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault, DBDDisplayFormat format)
            {
                Object value = viewer.curRows.get(viewer.getCurrentRow())[viewer.getMetaColumnIndex(column.getMetaAttribute())];
                return column.getValueHandler().getValueDisplayString(column.getMetaAttribute(), value, format);
            }
        },
        INPUT(DBIcon.FILTER_INPUT.getImageDescriptor()) {
            @Override
            String getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault, DBDDisplayFormat format)
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
            String getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault, DBDDisplayFormat format)
            {
                try {
                    return column.getValueHandler().getValueDisplayString(
                        column.getMetaAttribute(),
                        viewer.getColumnValueFromClipboard(column),
                        format);
                } catch (DBCException e) {
                    log.error("Error copying from clipboard", e);
                    return null;
                }
            }
        },
        NONE(DBIcon.FILTER_VALUE.getImageDescriptor()) {
            @Override
            String getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault, DBDDisplayFormat format)
            {
                return "";
            }
        };

        final ImageDescriptor icon;

        private FilterByColumnType(ImageDescriptor icon)
        {
            this.icon = icon;
        }
        abstract String getValue(ResultSetViewer viewer, DBDAttributeBinding column, boolean useDefault, DBDDisplayFormat format);
    }

    private String translateFilterPattern(String pattern, FilterByColumnType type, DBDAttributeBinding column)
    {
        String value = CommonUtils.truncateString(
            CommonUtils.toString(
                type.getValue(this, column, true, DBDDisplayFormat.UI)),
            30);
        return pattern.replace("?", value);
    }

    private class FilterByColumnAction extends Action {
        private final String pattern;
        private final FilterByColumnType type;
        private final DBDAttributeBinding column;
        public FilterByColumnAction(String pattern, FilterByColumnType type, DBDAttributeBinding column)
        {
            super(DBUtils.getQuotedIdentifier(column.getEntityAttribute()) + " " + translateFilterPattern(pattern, type, column), type.icon);
            this.pattern = pattern;
            this.type = type;
            this.column = column;
        }

        @Override
        public void run()
        {
            String value = type.getValue(ResultSetViewer.this, column, false, DBDDisplayFormat.NATIVE);
            if (value == null) {
                return;
            }
            String stringValue = pattern.replace("?", value);
            DBDDataFilter filter = getDataFilter();
            DBQCondition filterColumn = filter.getFilterColumn(column.getAttributeName());
            if (filterColumn == null) {
                filterColumn = new DBQCondition(column.getAttributeName(), stringValue);
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
            DBQCondition filterColumn = filter.getFilterColumn(column.getAttributeName());
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
                            editEntityIdentifier();
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

    private class ResultSetSelectionImpl implements ResultSetSelection {
        @Override
        public Object getFirstElement()
        {
            Collection<GridPos> ssSelection = spreadsheet.getSelection();
            return ssSelection.isEmpty() ? null : ssSelection.iterator().next();
        }

        @Override
        public Iterator iterator()
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
            return new ArrayList<GridPos>(spreadsheet.getSelection());
        }

        @Override
        public boolean isEmpty()
        {
            return spreadsheet.getSelection().isEmpty();
        }

        @Override
        public ResultSetViewer getResultSetViewer()
        {
            return ResultSetViewer.this;
        }

        @Override
        public Collection<ResultSetRow> getSelectedRows()
        {
            List<ResultSetRow> rows = new ArrayList<ResultSetRow>();
            if (mode == ResultSetMode.RECORD) {
                if (curRowNum < 0 || curRowNum >= curRows.size()) {
                    return Collections.emptyList();
                }
                rows.add(new ResultSetRow(ResultSetViewer.this, curRows.get(curRowNum)));
            } else {
                Collection<Integer> rowSelection = spreadsheet.getRowSelection();
                for (Integer row : rowSelection) {
                    rows.add(new ResultSetRow(ResultSetViewer.this, curRows.get(row)));
                }
            }
            return rows;
        }
    }

    /**
     * The column header sort arrow renderer.
     */
    static class SortRenderer extends AbstractRenderer {
        private Image asterisk;
        private Image arrowUp;
        private Image arrowDown;
        private GridColumn column;
        private Cursor hoverCursor;

        SortRenderer(GridColumn column)
        {
            super(column.getParent());
            this.column = column;
            this.asterisk = DBIcon.SORT_UNKNOWN.getImage();
            this.arrowUp = DBIcon.SORT_DECREASE.getImage();
            this.arrowDown = DBIcon.SORT_INCREASE.getImage();
            this.hoverCursor = getDisplay().getSystemCursor(SWT.CURSOR_HAND);
            Rectangle imgBounds = arrowUp.getBounds();
            setSize(imgBounds.width, imgBounds.height);
        }

        @Override
        public void paint(GC gc)
        {
            Rectangle bounds = getBounds();
            switch (column.getSort()) {
                case SWT.DEFAULT:
                    gc.drawImage(asterisk, bounds.x, bounds.y);
                    break;
                case SWT.UP:
                    gc.drawImage(arrowUp, bounds.x, bounds.y);
                    break;
                case SWT.DOWN:
                    gc.drawImage(arrowDown, bounds.x, bounds.y);
                    break;
            }
    /*
            if (isSelected()) {
                gc.drawLine(bounds.x, bounds.y, bounds.x + 6, bounds.y);
                gc.drawLine(bounds.x + 1, bounds.y + 1, bounds.x + 5, bounds.y + 1);
                gc.drawLine(bounds.x + 2, bounds.y + 2, bounds.x + 4, bounds.y + 2);
                gc.drawPoint(bounds.x + 3, bounds.y + 3);
            } else {
                gc.drawPoint(bounds.x + 3, bounds.y);
                gc.drawLine(bounds.x + 2, bounds.y + 1, bounds.x + 4, bounds.y + 1);
                gc.drawLine(bounds.x + 1, bounds.y + 2, bounds.x + 5, bounds.y + 2);
                gc.drawLine(bounds.x, bounds.y + 3, bounds.x + 6, bounds.y + 3);
            }
    */
        }

        @Override
        public Cursor getHoverCursor() {
            return hoverCursor;
        }
    }

    static class TopLeftRenderer extends AbstractRenderer {
        private Button cfgButton;

        public TopLeftRenderer(final ResultSetViewer resultSetViewer) {
            super(resultSetViewer.getSpreadsheet());

            cfgButton = new Button(grid, SWT.FLAT | SWT.NO_FOCUS);
            cfgButton.setImage(DBIcon.FILTER.getImage());
            cfgButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    new ResultSetFilterDialog(resultSetViewer).open();
                }
            });
            ControlEditor controlEditor = new ControlEditor(grid);
            controlEditor.setEditor(cfgButton);
            //cfgButton.setText("...");
        }

        @Override
        public void setBounds(Rectangle bounds) {

            Rectangle cfgBounds = new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2);
            cfgButton.setBounds(bounds);

            super.setBounds(bounds);
        }

        @Override
        public void paint(GC gc) {
            //cfgButton.redraw();
            //gc.drawImage(DBIcon.FILTER.getImage(), 0, 0);
        }

    }
}
