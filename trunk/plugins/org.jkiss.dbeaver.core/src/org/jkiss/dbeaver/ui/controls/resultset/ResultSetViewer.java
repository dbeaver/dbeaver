/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVConstants;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridLabelProvider;
import org.jkiss.dbeaver.ui.controls.spreadsheet.ISpreadsheetController;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;
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
 *
 * TODO: PROBLEM. Multiple occurrences of the same struct type in a single table.
 * Need to make wrapper over DBSAttributeBase or something. Or maybe it is not a problem
 * because we search for binding by attribute only in constraints and for unique key columns which are unique?
 * But what PK has struct type?
 *
 * TODO: fix command handlers. Replace activeControl handler with activeEditor. It also fixes standard shortcuts problem (http://dbeaver.jkiss.org/forum/posting.php?mode=reply&f=2&t=1247)
 * TODO: links in both directions, multiple links support (context menu)
 * TODO: not-editable cells (struct owners in record mode)
 */
public class ResultSetViewer extends Viewer
    implements IDataSourceProvider, ISpreadsheetController, ISaveablePart2, IAdaptable
{
    static final Log log = Log.getLog(ResultSetViewer.class);
    private final IPropertyChangeListener themeChangeListener;

    private ResultSetValueController panelValueController;

    private static final String VIEW_PANEL_VISIBLE = "viewPanelVisible";
    private static final String VIEW_PANEL_RATIO = "viewPanelRatio";

    public enum RowPosition {
        FIRST,
        PREVIOUS,
        NEXT,
        LAST
    }

    public static class StateItem {
        DBSDataContainer dataContainer;
        DBDDataFilter filter;
        int rowNumber;

        public StateItem(DBSDataContainer dataContainer, @Nullable DBDDataFilter filter, int rowNumber) {
            this.dataContainer = dataContainer;
            this.filter = filter;
            this.rowNumber = rowNumber;
        }

        public String describeState(DBPDataSource dataSource) {
            String desc = dataContainer.getName();
            if (filter != null && filter.hasConditions()) {
                StringBuilder condBuffer = new StringBuilder();
                SQLUtils.appendConditionString(filter, dataSource, null, condBuffer, true);
                desc += " [" + condBuffer + "]";
            }
            return desc;
        }
    }

    @NotNull
    private final IWorkbenchPartSite site;
    private final Composite viewerPanel;
    private Composite filtersPanel;
    private ControlEnableState filtersEnableState;
    private Combo filtersText;
    private Text statusLabel;

    private final SashForm resultsSash;
    private final Spreadsheet spreadsheet;
    private final ViewValuePanel previewPane;

    @NotNull
    private final ResultSetProvider resultSetProvider;
    @NotNull
    private final ResultSetDataReceiver dataReceiver;
    @NotNull
    private final IThemeManager themeManager;
    private ToolBarManager toolBarManager;

    // Current row/col number
    @Nullable
    private RowData curRow;
    @Nullable
    private DBDAttributeBinding curAttribute;
    // Mode
    private boolean recordMode;
    private int columnOrder = SWT.NONE;

    private final Map<ResultSetValueController, DBDValueEditorStandalone> openEditors = new HashMap<ResultSetValueController, DBDValueEditorStandalone>();
    private final List<ResultSetListener> listeners = new ArrayList<ResultSetListener>();

    // UI modifiers
    private final Color colorRed;
    private Color backgroundAdded;
    private Color backgroundDeleted;
    private Color backgroundModified;
    private Color backgroundNormal;
    private Color backgroundOdd;
    private Color backgroundReadOnly;
    private Color foregroundDefault;
    private Color foregroundNull;
    private final Font boldFont;

    private volatile ResultSetDataPumpJob dataPumpJob;
    private ResultSetFindReplaceTarget findReplaceTarget;

    private final ResultSetModel model = new ResultSetModel();
    private StateItem curState = null;
    private final List<StateItem> stateHistory = new ArrayList<StateItem>();
    private int historyPosition = -1;

    private boolean showOddRows = true;
    private boolean showCelIcons = true;

    private ToolItem filtersApplyButton;
    private ToolItem filtersClearButton;
    private ToolItem historyBackButton;
    private ToolItem historyForwardButton;

    public ResultSetViewer(@NotNull Composite parent, @NotNull IWorkbenchPartSite site, @NotNull ResultSetProvider resultSetProvider)
    {
        super();

        this.site = site;
        this.recordMode = false;
        this.resultSetProvider = resultSetProvider;
        this.dataReceiver = new ResultSetDataReceiver(this);

        this.colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());
        this.foregroundNull = getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);

        this.viewerPanel = UIUtils.createPlaceholder(parent, 1);
        UIUtils.setHelp(this.viewerPanel, IHelpContextIds.CTX_RESULT_SET_VIEWER);

        createFiltersPanel();

        {
            resultsSash = new SashForm(viewerPanel, SWT.HORIZONTAL | SWT.SMOOTH);
            resultsSash.setBackgroundMode(SWT.INHERIT_FORCE);
            resultsSash.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultsSash.setSashWidth(5);
            //resultsSash.setBackground(resultsSash.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

            this.spreadsheet = new Spreadsheet(
                resultsSash,
                SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL,
                site,
                this,
                new ContentProvider(),
                new GridLabelProvider());
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

        this.themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
        this.themeChangeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (event.getProperty().startsWith(ThemeConstants.RESULTS_PROP_PREFIX)) {
                    applyThemeSettings();
                }
            }
        };
        this.themeManager.addPropertyChangeListener(themeChangeListener);
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
                if (event.detail != SWT.DRAG && event.detail != SWT.DROP_DOWN) {
                    updateGridCursor((GridCell)event.data);
                }
            }
        });
        //this.spreadsheet.setTopLeftRenderer(new TopLeftRenderer(this));
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

        changeMode(false);
    }

    ////////////////////////////////////////////////////////////
    // Filters

    private void createFiltersPanel()
    {
        filtersPanel = new Composite(viewerPanel, SWT.NONE);
        filtersPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 3;
        gl.marginWidth = 3;
        filtersPanel.setLayout(gl);

        Button sourceQueryButton = new Button(filtersPanel, SWT.PUSH | SWT.NO_FOCUS);
        sourceQueryButton.setImage(DBIcon.SQL_TEXT.getImage());
        sourceQueryButton.setText("SQL");
        sourceQueryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                String queryText = model.getStatistics() == null ? null : model.getStatistics().getQueryText();
                if (queryText == null || queryText.isEmpty()) {
                    queryText = "<empty>";
                }
                ViewSQLDialog dialog = new ViewSQLDialog(site, getDataSource(), "Query Text", DBIcon.SQL_TEXT.getImage(), queryText);
                dialog.setEnlargeViewPanel(false);
                dialog.setWordWrap(true);
                dialog.open();
            }
        });

        Button customizeButton = new Button(filtersPanel, SWT.PUSH | SWT.NO_FOCUS);
        customizeButton.setImage(DBIcon.FILTER.getImage());
        customizeButton.setText("Filters");
        customizeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new FilterSettingsDialog(ResultSetViewer.this).open();
            }
        });

        //UIUtils.createControlLabel(filtersPanel, " Filter");

        this.filtersText = new Combo(filtersPanel, SWT.BORDER | SWT.DROP_DOWN);
        this.filtersText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.filtersText.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                setCustomDataFilter();
            }
        });

        {
            // Register filters text in focus service
            UIUtils.addFocusTracker(site, UIUtils.INLINE_WIDGET_EDITOR_ID, this.filtersText);

            this.filtersText.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    // Unregister from focus service
                    UIUtils.removeFocusTracker(ResultSetViewer.this.site, filtersText);
                    dispose();
                }
            });
        }

        // Handle all shortcuts by filters editor, not by host editor
        this.filtersText.addFocusListener(new FocusListener() {
            private boolean activated = false;
            @SuppressWarnings("deprecation")
            @Override
            public void focusGained(FocusEvent e)
            {
                if (!activated) {
                    UIUtils.enableHostEditorKeyBindings(site, false);
                    activated = true;
                }
            }
            @SuppressWarnings("deprecation")
            @Override
            public void focusLost(FocusEvent e)
            {
                if (activated) {
                    UIUtils.enableHostEditorKeyBindings(site, true);
                    activated = false;
                }
            }
        });

        ToolBar filterToolbar = new ToolBar(filtersPanel, SWT.HORIZONTAL | SWT.RIGHT);

        filtersApplyButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
        filtersApplyButton.setImage(DBIcon.FILTER_APPLY.getImage());
        //filtersApplyButton.setText("Apply");
        filtersApplyButton.setToolTipText("Apply filter criteria");
        filtersApplyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setCustomDataFilter();
            }
        });
        filtersApplyButton.setEnabled(false);

        filtersClearButton = new ToolItem(filterToolbar, SWT.PUSH | SWT.NO_FOCUS);
        filtersClearButton.setImage(DBIcon.FILTER_RESET.getImage());
        filtersClearButton.setToolTipText("Remove all filters");
        filtersClearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resetDataFilter(true);
            }
        });
        filtersClearButton.setEnabled(false);

        historyBackButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
        historyBackButton.setImage(DBIcon.RS_BACK.getImage());
        historyBackButton.setEnabled(false);
        historyBackButton.addSelectionListener(new HistoryMenuListener(historyBackButton, true));

        historyForwardButton = new ToolItem(filterToolbar, SWT.DROP_DOWN | SWT.NO_FOCUS);
        historyForwardButton.setImage(DBIcon.RS_FORWARD.getImage());
        historyForwardButton.setEnabled(false);
        historyForwardButton.addSelectionListener(new HistoryMenuListener(historyForwardButton, false));

        this.filtersText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                if (filtersEnableState == null) {
                    String filterText = filtersText.getText();
                    filtersApplyButton.setEnabled(true);
                    filtersClearButton.setEnabled(!CommonUtils.isEmpty(filterText));
                }
            }
        });

        filtersPanel.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    setCustomDataFilter();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });

        filtersEnableState = ControlEnableState.disable(filtersPanel);
    }

    public void resetDataFilter(boolean refresh)
    {
        setDataFilter(model.createDataFilter(), refresh);
    }

    private void setCustomDataFilter()
    {
        DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return;
        }
        String condition = filtersText.getText();
        StringBuilder currentCondition = new StringBuilder();
        SQLUtils.appendConditionString(model.getDataFilter(), dataSource, null, currentCondition, true);
        if (currentCondition.toString().trim().equals(condition.trim())) {
            // The same
            return;
        }
        DBDDataFilter newFilter = model.createDataFilter();
        newFilter.setWhere(condition);
        setDataFilter(newFilter, true);
        spreadsheet.setFocus();
    }

    public void updateFiltersText()
    {
        boolean enableFilters = false;
        DBPDataSource dataSource = getDataSource();
        if (dataSource != null) {
            StringBuilder where = new StringBuilder();
            SQLUtils.appendConditionString(model.getDataFilter(), dataSource, null, where, true);
            String whereCondition = where.toString().trim();
            filtersText.setText(whereCondition);
            if (!whereCondition.isEmpty()) {
                addFiltersHistory(whereCondition);
            }

            if (resultSetProvider.isReadyToRun() &&
                !model.isUpdateInProgress() &&
                (!CommonUtils.isEmpty(whereCondition) || (model.getVisibleColumnCount() > 0 && supportsDataFilter())))
            {
                enableFilters = true;
            }
        }
        enableFilters(enableFilters);
    }

    private void enableFilters(boolean enableFilters) {
        if (enableFilters) {
            if (filtersEnableState != null) {
                filtersEnableState.restore();
                filtersEnableState = null;
            }
            String filterText = filtersText.getText();
            filtersApplyButton.setEnabled(true);
            filtersClearButton.setEnabled(!CommonUtils.isEmpty(filterText));
            // Update history buttons
            DBPDataSource dataSource = getDataSource();
            if (dataSource != null) {
                if (historyPosition > 0) {
                    historyBackButton.setEnabled(true);
                    historyBackButton.setToolTipText(stateHistory.get(historyPosition - 1).describeState(dataSource));
                } else {
                    historyBackButton.setEnabled(false);
                }
                if (historyPosition < stateHistory.size() - 1) {
                    historyForwardButton.setEnabled(true);
                    historyForwardButton.setToolTipText(stateHistory.get(historyPosition + 1).describeState(dataSource));
                } else {
                    historyForwardButton.setEnabled(false);
                }
            }
        } else if (filtersEnableState == null) {
            filtersEnableState = ControlEnableState.disable(filtersPanel);
        }
    }

    private void addFiltersHistory(String whereCondition)
    {
        int historyCount = filtersText.getItemCount();
        for (int i = 0; i < historyCount; i++) {
            if (filtersText.getItem(i).equals(whereCondition)) {
                if (i > 0) {
                    // Move to beginning
                    filtersText.remove(i);
                    break;
                } else {
                    return;
                }
            }
        }
        filtersText.add(whereCondition, 0);
        filtersText.setText(whereCondition);
    }

    public void setDataFilter(final DBDDataFilter dataFilter, boolean refreshData)
    {
        if (!model.getDataFilter().equalFilters(dataFilter)) {
            if (model.setDataFilter(dataFilter)) {
                refreshSpreadsheet(true);
            }
            if (refreshData) {
                reorderResultSet(true, new Runnable() {
                    @Override
                    public void run()
                    {
                        if (!recordMode) {
                            spreadsheet.refreshData(false);
                        }
                    }
                });
            } else {
                updateFiltersText();
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Misc

    IPreferenceStore getPreferences()
    {
        return DBeaverCore.getGlobalPreferenceStore();
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource()
    {
        DBSDataContainer dataContainer = getDataContainer();
        return dataContainer == null ? null : dataContainer.getDataSource();
    }

    public IFindReplaceTarget getFindReplaceTarget()
    {
        if (findReplaceTarget == null) {
            findReplaceTarget = new ResultSetFindReplaceTarget(this);
        }
        return findReplaceTarget;
    }

    @Nullable
    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IPropertySheetPage.class) {
            // Show cell properties
            PropertyPageStandard page = new PropertyPageStandard();
            page.setPropertySourceProvider(new IPropertySourceProvider() {
                @Nullable
                @Override
                public IPropertySource getPropertySource(Object object)
                {
                    if (object instanceof GridCell) {
                        GridCell cell = (GridCell) object;
                        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row : cell.col);
                        final RowData row = (RowData)(recordMode ? cell.col : cell.row);
                        final ResultSetValueController valueController = new ResultSetValueController(
                            ResultSetViewer.this,
                            attr,
                            row,
                            DBDValueController.EditType.NONE,
                            null);
                        PropertyCollector props = new PropertyCollector(valueController.binding.getAttribute(), false);
                        props.collectProperties();
                        valueController.getValueHandler().contributeProperties(props, valueController);
                        return props;
                    }
                    return null;
                }
            });
            return page;
        } else if (adapter == IFindReplaceTarget.class) {
            return getFindReplaceTarget();
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

    private void updateGridCursor(GridCell cell)
    {
        boolean changed;
        Object newCol = cell == null ? null : cell.col;
        Object newRow = cell == null ? null : cell.row;
        if (!recordMode) {
            changed = curRow != newRow || curAttribute != newCol;
            curRow = (RowData) newRow;
            curAttribute = (DBDAttributeBinding) newCol;
        } else {
            changed = curAttribute != newRow;
            curAttribute = (DBDAttributeBinding) newRow;
        }
        if (curState != null && curRow != null) {
            curState.rowNumber = curRow.getVisualNumber();
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
        DBDAttributeBinding oldAttr = this.curAttribute;
        this.initResultSet();
        this.curAttribute = oldAttr;
        if (curRow != null && oldAttr != null) {
            spreadsheet.setCursor(new GridCell(curRow, oldAttr), false);
        }
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
            int rowCount = model.getRowCount();
            if (curRow == null || curRow.getVisualNumber() >= rowCount) {
                curRow = rowCount == 0 ? null : model.getRow(rowCount - 1);
            }
            GridCell curCell = spreadsheet.getCursorCell();

            // Set cursor on new row
            if (!recordMode) {
                this.initResultSet();
                if (curCell != null) {
                    spreadsheet.setCursor(curCell, false);
                }
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

        toolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

        // Add presentation switcher
        //toolBarManager.add(new Comb);

        // handle own commands
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
        // Link to standard Find/Replace action - it has to be handled by owner site
        toolBarManager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, CommandContributionItem.STYLE_PUSH, DBIcon.FIND_TEXT.getImageDescriptor()));

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
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_MODE, CommandContributionItem.STYLE_CHECK));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_PREVIEW, CommandContributionItem.STYLE_CHECK));
        toolBarManager.add(new ConfigAction());

        toolBarManager.createControl(statusBar);

        //updateEditControls();
    }

    @NotNull
    public Spreadsheet getSpreadsheet()
    {
        return spreadsheet;
    }

    public DBSDataContainer getDataContainer()
    {
        return curState != null ? curState.dataContainer : resultSetProvider.getDataContainer();
    }

    ////////////////////////////////////////////////////////////
    // Grid/Record mode


    public boolean isRecordMode() {
        return recordMode;
    }

    public void toggleMode()
    {
        changeMode(!recordMode);
        // Refresh elements
        ICommandService commandService = (ICommandService) site.getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(ResultSetCommandHandler.CMD_TOGGLE_MODE, null);
        }
    }

    private void changeMode(boolean recordMode)
    {
        RowData oldRow = this.curRow;
        DBDAttributeBinding oldAttribute = this.curAttribute;
        int rowCount = model.getRowCount();
        if (rowCount > 0) {
            // Fix row number if needed
            if (oldRow == null) {
                oldRow = this.curRow = model.getRow(0);
            } else if (oldRow.getVisualNumber() >= rowCount) {
                oldRow = this.curRow = model.getRow(rowCount - 1);
            }
        }
        if (oldAttribute == null && model.getVisibleColumnCount() > 0) {
            oldAttribute = model.getVisibleColumn(0);
        }
        this.recordMode = recordMode;
        this.columnOrder = recordMode ? SWT.DEFAULT : SWT.NONE;
        if (!this.recordMode) {
            this.initResultSet();
        } else {
            this.updateRecordMode();
        }
        if (oldRow != null && oldAttribute != null) {
            if (!recordMode) {
                spreadsheet.setCursor(new GridCell(oldAttribute, oldRow), false);
            } else {
                spreadsheet.setCursor(new GridCell(oldRow, oldAttribute), false);
            }
        }
        spreadsheet.layout(true, true);
        previewValue();
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
        DBDAttributeBinding attr = getFocusAttribute();
        RowData row = getFocusRow();
        if (!isPreviewVisible() || attr == null || row == null) {
            return;
        }
        if (panelValueController == null || panelValueController.binding != attr) {
            panelValueController = new ResultSetValueController(
                this,
                attr,
                row,
                DBDValueController.EditType.PANEL,
                previewPane.getViewPlaceholder());
        } else {
            panelValueController.setCurRow(row);
        }
        previewPane.viewValue(panelValueController);
    }

    ////////////////////////////////////////////////////////////
    // Misc

    private void dispose()
    {
        closeEditors();
        clearData();

        themeManager.removePropertyChangeListener(themeChangeListener);

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
        Color previewBack = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_SET_PREVIEW_BACK);
        if (previewBack != null) {
            this.previewPane.getViewPlaceholder().setBackground(previewBack);
            for (Control control : this.previewPane.getViewPlaceholder().getChildren()) {
                control.setBackground(previewBack);
            }
        }
        this.backgroundAdded = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_NEW_BACK);
        this.backgroundDeleted = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_DELETED_BACK);
        this.backgroundModified = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_MODIFIED_BACK);
        this.backgroundOdd = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_ODD_BACK);
        this.backgroundReadOnly = currentTheme.getColorRegistry().get(ThemeConstants.COLOR_SQL_RESULT_CELL_READ_ONLY);

        this.spreadsheet.recalculateSizes();
    }

    void scrollToRow(RowPosition position)
    {
        switch (position) {
            case FIRST:
                if (recordMode) {
                    if (model.getRowCount() > 0) {
                        curRow = model.getRow(0);
                    } else {
                        curRow = null;
                    }
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, -spreadsheet.getItemCount(), false);
                }
                break;
            case PREVIOUS:
                if (recordMode && curRow != null && curRow.getVisualNumber() > 0) {
                    curRow = model.getRow(curRow.getVisualNumber() - 1);
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, -1, false);
                }
                break;
            case NEXT:
                if (recordMode && curRow != null && curRow.getVisualNumber() < model.getRowCount() - 1) {
                    curRow = model.getRow(curRow.getVisualNumber() + 1);
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, 1, false);
                }
                break;
            case LAST:
                if (recordMode && model.getRowCount() > 0) {
                    curRow = model.getRow(model.getRowCount() - 1);
                    updateRecordMode();
                } else {
                    spreadsheet.shiftCursor(0, spreadsheet.getItemCount(), false);
                }
                break;
        }
        updateToolbar();
    }

    boolean isColumnReadOnly(DBDAttributeBinding column)
    {
        if (isReadOnly()) {
            return true;
        }
        if (!model.isColumnReadOnly(column) || (curRow != null && curRow.getState() == RowData.STATE_ADDED)) {
            return false;
        }
        return true;
    }

    public StateItem getCurrentState() {
        return curState;
    }

    private void setNewState(DBSDataContainer dataContainer, @Nullable DBDDataFilter dataFilter) {
        // Create filter copy to avoid modifications
        dataFilter = new DBDDataFilter(dataFilter);
        // Search in history
        for (int i = 0; i < stateHistory.size(); i++) {
            StateItem item = stateHistory.get(i);
            if (item.dataContainer == dataContainer && CommonUtils.equalObjects(item.filter, dataFilter)) {
                curState = item;
                historyPosition = i;
                return;
            }
        }
        // Save current state in history
        while (historyPosition < stateHistory.size() - 1) {
            stateHistory.remove(stateHistory.size() - 1);
        }
        curState = new StateItem(
            dataContainer,
            dataFilter,
            curRow == null ? -1 : curRow.getVisualNumber());
        stateHistory.add(curState);
        historyPosition = stateHistory.size() - 1;
    }

    public void resetHistory() {
        curState = null;
        stateHistory.clear();
        historyPosition = -1;
    }

    private void navigateHistory(int position) {
        StateItem state = stateHistory.get(position);
        int segmentSize = getSegmentMaxRows();
        if (state.rowNumber >= 0 && state.rowNumber >= segmentSize && segmentSize > 0) {
            segmentSize = (state.rowNumber / segmentSize + 1) * segmentSize;
        }

        runDataPump(state.dataContainer, state.filter, 0, segmentSize, state.rowNumber, null);
    }

    @Nullable
    public RowData getCurrentRow()
    {
        return curRow;
    }

    @Nullable
    public DBDAttributeBinding getFocusAttribute()
    {
        return recordMode ?
            (DBDAttributeBinding) spreadsheet.getFocusRowElement() :
            (DBDAttributeBinding) spreadsheet.getFocusColumnElement();
    }

    @Nullable
    public RowData getFocusRow()
    {
        return recordMode ?
            (RowData) spreadsheet.getFocusColumnElement() :
            (RowData) spreadsheet.getFocusRowElement();
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
        } else if (colorRed.equals(statusLabel.getForeground())) {
            statusLabel.setForeground(filtersText.getForeground());
        }
        if (status == null) {
            status = "???"; //$NON-NLS-1$
        }
        statusLabel.setText(status);
    }

    public void updateStatusMessage()
    {
        if (model.getRowCount() == 0) {
            if (model.getVisibleColumnCount() == 0) {
                setStatus(CoreMessages.controls_resultset_viewer_status_empty + getExecutionTimeMessage());
            } else {
                setStatus(CoreMessages.controls_resultset_viewer_status_no_data + getExecutionTimeMessage());
            }
        } else {
            if (recordMode) {
                setStatus(CoreMessages.controls_resultset_viewer_status_row + (curRow == null ? 0 : curRow.getVisualNumber() + 1) + "/" + model.getRowCount() + getExecutionTimeMessage());
            } else {
                setStatus(String.valueOf(model.getRowCount()) + CoreMessages.controls_resultset_viewer_status_rows_fetched + getExecutionTimeMessage());
            }
        }
    }

    private String getExecutionTimeMessage()
    {
        DBCStatistics statistics = model.getStatistics();
        if (statistics == null || statistics.isEmpty()) {
            return "";
        }
        return " - " + RuntimeUtils.formatExecutionTime(statistics.getTotalTime());
    }

    /**
     * Sets new metadata of result set
     * @param columns columns metadata
     */
    public void setMetaData(DBDAttributeBinding[] columns)
    {
        model.setMetaData(columns);
        if (model.isMetadataChanged()) {
            this.panelValueController = null;
            this.curAttribute = null;
            //getSpreadsheet().set
        }
    }

    public void setData(List<Object[]> rows)
    {
        if (spreadsheet.isDisposed()) {
            return;
        }
        boolean metaChanged = model.isMetadataChanged();
        // Clear previous data
        this.closeEditors();

        this.curRow = null;
        this.model.setData(rows);
        this.curRow = (this.model.getRowCount() > 0 ? this.model.getRow(0) : null);

        if (metaChanged) {

            if (getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE)) {
                boolean newRecordMode = (rows.size() == 1);
                if (newRecordMode != recordMode) {
                    toggleMode();
//                    ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_TOGGLE);
                }
            }

            this.initResultSet();
        } else {
            this.refreshSpreadsheet(true);
        }
        updateEditControls();
    }

    public void appendData(List<Object[]> rows)
    {
        model.appendData(rows);
        //refreshSpreadsheet(true);
        spreadsheet.refreshData(false);

        setStatus(NLS.bind(CoreMessages.controls_resultset_viewer_status_rows_size, model.getRowCount(), rows.size()) + getExecutionTimeMessage());

        updateEditControls();
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
        spreadsheet.setRedraw(false);
        try {
            spreadsheet.refreshData(true);
        } finally {
            spreadsheet.setRedraw(true);
        }

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
            DBeaverPreferences.CONFIRM_RS_EDIT_CLOSE,
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
        return model.isDirty();
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
        return model.hasData();
    }

    @Override
    public boolean isReadOnly()
    {
        if (model.isUpdateInProgress()) {
            return true;
        }
        DBPDataSource dataSource = getDataSource();
        return
            dataSource == null ||
            !dataSource.isConnected() ||
            dataSource.getContainer().isConnectionReadOnly() ||
            dataSource.getInfo().isReadOnlyData();
    }

    /**
     * Checks that current state of result set allows to insert new rows
     * @return true if new rows insert is allowed
     */
    @Override
    public boolean isInsertable()
    {
        return
            !isReadOnly() &&
            model.isSingleSource() &&
            model.getVisibleColumnCount() > 0;
    }

    @Override
    public boolean isRefreshInProgress() {
        return dataPumpJob != null;
    }

    @Nullable
    @Override
    public Control showCellEditor(
        final boolean inline)
    {
        // The control that will be the editor must be a child of the Table
        DBDAttributeBinding attr = getFocusAttribute();
        RowData row = getFocusRow();
        if (attr == null || row == null) {
            return null;
        }

        if (!inline) {
            for (ResultSetValueController valueController : openEditors.keySet()) {
                if (attr == valueController.binding && row == valueController.curRow) {
                    openEditors.get(valueController).showValueEditor();
                    return null;
                }
            }
        }
        final int handlerFeatures = attr.getValueHandler().getFeatures();
        if (handlerFeatures == DBDValueHandler.FEATURE_NONE) {
            return null;
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
            return null;
        }
        if (isColumnReadOnly(attr) && inline) {
            // No inline editors for readonly columns
            return null;
        }

        Composite placeholder = null;
        if (inline) {
            if (isReadOnly()) {
                return null;
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
            this,
            attr,
            row,
            inline ? DBDValueController.EditType.INLINE : DBDValueController.EditType.EDITOR,
            placeholder);
        final DBDValueEditor editor;
        try {
            editor = attr.getValueHandler().createEditor(valueController);
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(site.getShell(), "Cannot edit value", null, e);
            return null;
        }
        if (editor != null) {
            editor.createControl();
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
                try {
                    editor.primeEditorValue(valueController.getValue());
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }
        if (inline) {
            if (editor != null) {
                spreadsheet.showCellEditor(placeholder);
                return editor.getControl();
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
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void resetCellValue(@NotNull Object colElement, @NotNull Object rowElement, boolean delete)
    {
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
        final RowData row = (RowData)(recordMode ? colElement : rowElement);
        model.resetCellValue(attr, row);
        spreadsheet.redrawGrid();
        updateEditControls();
        previewValue();
    }

    @Override
    public void fillContextMenu(@Nullable Object colObject, @Nullable Object rowObject, @NotNull IMenuManager manager)
    {
        final DBDAttributeBinding attr = (DBDAttributeBinding)(colObject instanceof DBDAttributeBinding ? colObject : rowObject);
        final RowData row = (RowData)(colObject instanceof RowData? colObject : rowObject);
        // Custom oldValue items
        if (attr != null && row != null) {
            final ResultSetValueController valueController = new ResultSetValueController(
                this,
                attr,
                row,
                DBDValueController.EditType.NONE,
                null);

            final Object value = valueController.getValue();
            {
                // Standard items
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_CUT));
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_COPY));
                manager.add(ActionUtils.makeCommandContribution(site, ICommandIds.CMD_COPY_SPECIAL));
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_PASTE));
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_DELETE));
                // Edit items
                manager.add(new Separator());
                manager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT));
                manager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT_INLINE));
                if (!valueController.isReadOnly() && !DBUtils.isNullValue(value)/* && !attr.isRequired()*/) {
                    manager.add(new Action(CoreMessages.controls_resultset_viewer_action_set_to_null) {
                        @Override
                        public void run()
                        {
                            valueController.updateValue(
                                DBUtils.makeNullValue(valueController));
                        }
                    });
                }
                if (row.isChanged()) {
                    Action resetValueAction = new Action(CoreMessages.controls_resultset_viewer_action_reset_value)
                    {
                        @Override
                        public void run()
                        {
                            resetCellValue(attr, row, false);
                        }
                    };
                    resetValueAction.setAccelerator(SWT.ESC);
                    manager.add(resetValueAction);
                }
            }

            // Menus from value handler
            try {
                manager.add(new Separator());
                attr.getValueHandler().contributeActions(manager, valueController);
            }
            catch (Exception e) {
                log.error(e);
            }
        }

        if (attr != null && model.getVisibleColumnCount() > 0 && !model.isUpdateInProgress()) {
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
                    fillFiltersMenu(attr, manager);
                }
            });
            manager.add(filtersMenu);

            manager.add(new Action(CoreMessages.controls_resultset_viewer_action_export, DBIcon.EXPORT.getImageDescriptor()) {
                @Override
                public void run()
                {
                    ActiveWizardDialog dialog = new ActiveWizardDialog(
                        site.getWorkbenchWindow(),
                        new DataTransferWizard(
                            new IDataTransferProducer[] {
                                new DatabaseTransferProducer(getDataContainer(), model.getDataFilter())},
                            null),
                        getSelection());
                    dialog.open();
                }
            });
        }
        manager.add(new GroupMarker(ICommandIds.GROUP_TOOLS));
    }

    private void fillFiltersMenu(@NotNull DBDAttributeBinding column, @NotNull IMenuManager filtersMenu)
    {
        if (supportsDataFilter()) {
            DBCLogicalOperator[] operators = column.getValueHandler().getSupportedOperators(column);
            for (DBCLogicalOperator operator : operators) {
                if (operator.getArgumentCount() == 0) {
                    filtersMenu.add(new FilterByColumnAction(operator, FilterByColumnType.NONE, column));
                }
            }
            for (FilterByColumnType type : FilterByColumnType.values()) {
                if (type == FilterByColumnType.NONE) {
                    // Value filters are available only if certain cell is selected
                    continue;
                }
                filtersMenu.add(new Separator());
                if (type.getValue(this, column, DBCLogicalOperator.EQUALS, true) == null) {
                    // Null cell value - no operators can be applied
                    continue;
                }
                for (DBCLogicalOperator operator : operators) {
                    if (operator.getArgumentCount() > 0) {
                        filtersMenu.add(new FilterByColumnAction(operator, type, column));
                    }
                }
            }
            filtersMenu.add(new Separator());
            DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(column);
            if (constraint != null && constraint.hasCondition()) {
                filtersMenu.add(new FilterResetColumnAction(column));
            }
        }
        {
            final List<Object> selectedColumns = getSpreadsheet().getColumnSelection();
            if (!recordMode && !selectedColumns.isEmpty()) {
                String hideTitle;
                if (selectedColumns.size() == 1) {
                    DBDAttributeBinding columnToHide = (DBDAttributeBinding) selectedColumns.get(0);
                    hideTitle = "Hide column '" + columnToHide.getName() + "'";
                } else {
                    hideTitle = "Hide selected columns (" + selectedColumns.size() + ")";
                }
                filtersMenu.add(new Action(hideTitle) {
                    @Override
                    public void run()
                    {
                        if (selectedColumns.size() >= model.getVisibleColumnCount()) {
                            UIUtils.showMessageBox(getControl().getShell(), "Hide columns", "Can't hide all result columns, at least one column must be visible", SWT.ERROR);
                        } else {
                            int[] columnIndexes = new int[selectedColumns.size()];
                            for (int i = 0, selectedColumnsSize = selectedColumns.size(); i < selectedColumnsSize; i++) {
                                columnIndexes[i] = model.getVisibleColumnIndex((DBDAttributeBinding) selectedColumns.get(i));
                            }
                            Arrays.sort(columnIndexes);
                            for (int i = columnIndexes.length; i > 0; i--) {
                                model.setColumnVisibility(model.getVisibleColumn(columnIndexes[i - 1]), false);
                            }
                            refreshSpreadsheet(true);
                        }
                    }
                });
            }
        }
        filtersMenu.add(new Separator());
        filtersMenu.add(new ToggleServerSideOrderingAction());
        filtersMenu.add(new ShowFiltersAction());
    }

    boolean supportsDataFilter()
    {
        return (getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_FILTER) == DBSDataContainer.DATA_FILTER;
    }

    @Override
    public void changeSorting(@NotNull Object columnElement, final int state)
    {
        if (columnElement == null) {
            columnOrder = columnOrder == SWT.DEFAULT ? SWT.DOWN : (columnOrder == SWT.DOWN ? SWT.UP : SWT.DEFAULT);
            spreadsheet.refreshData(false);
            spreadsheet.redrawGrid();
            return;
        }
        DBDDataFilter dataFilter = model.getDataFilter();
        boolean ctrlPressed = (state & SWT.CTRL) == SWT.CTRL;
        boolean altPressed = (state & SWT.ALT) == SWT.ALT;
        if (ctrlPressed) {
            dataFilter.resetOrderBy();
        }
        DBDAttributeBinding metaColumn = (DBDAttributeBinding)columnElement;
        DBDAttributeConstraint constraint = dataFilter.getConstraint(metaColumn);
        assert constraint != null;
        //int newSort;
        if (constraint.getOrderPosition() == 0) {
            if (isServerSideFiltering() && supportsDataFilter()) {
                if (!ConfirmationDialog.confirmActionWithParams(
                    spreadsheet.getShell(),
                    DBeaverPreferences.CONFIRM_ORDER_RESULTSET,
                    metaColumn.getName()))
                {
                    return;
                }
            }
            constraint.setOrderPosition(dataFilter.getMaxOrderingPosition() + 1);
            constraint.setOrderDescending(altPressed);
        } else if (!constraint.isOrderDescending()) {
            constraint.setOrderDescending(true);
        } else {
            for (DBDAttributeConstraint con2 : dataFilter.getConstraints()) {
                if (con2.getOrderPosition() > constraint.getOrderPosition()) {
                    con2.setOrderPosition(con2.getOrderPosition() - 1);
                }
            }
            constraint.setOrderPosition(0);
            constraint.setOrderDescending(false);
        }

        // Reorder
        reorderResultSet(false, new Runnable() {
            @Override
            public void run()
            {
                if (!recordMode) {
                    spreadsheet.refreshData(false);
                }
            }
        });
    }

    @Override
    public void navigateLink(@NotNull GridCell cell, int state) {
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row : cell.col);
        final RowData row = (RowData)(recordMode ? cell.col : cell.row);

        List<DBSEntityReferrer> referrers = attr.getReferrers();
        if (!CommonUtils.isEmpty(referrers)) {
            for (final DBSEntityReferrer referrer : referrers) {
                if (referrer instanceof DBSEntityAssociation) {
                    try {
                        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                            @Override
                            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                try {
                                    navigateAssociation(monitor, (DBSEntityAssociation) referrer, attr, row);
                                } catch (DBException e) {
                                    throw new InvocationTargetException(e);
                                }
                            }
                        });
                    } catch (InvocationTargetException e) {
                        UIUtils.showErrorDialog(site.getShell(), "Cannot navigate to the reference", null, e.getTargetException());
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    return;
                }
            }
        }
    }

    private void navigateAssociation(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAssociation association, @NotNull DBDAttributeBinding attr, @NotNull RowData row)
        throws DBException
    {
        Object value = model.getCellValue(attr, row);
        if (DBUtils.isNullValue(value)) {
            log.warn("Can't navigate to NULL value");
            return;
        }
        DBSEntityConstraint refConstraint = association.getReferencedConstraint();
        if (!(refConstraint instanceof DBSEntityReferrer)) {
            log.warn("Referenced constraint [" + refConstraint + "] is no a referrer");
            return;
        }
        DBSEntity targetEntity = refConstraint.getParentObject();
        if (!(targetEntity instanceof DBSDataContainer)) {
            log.warn("Entity [" + DBUtils.getObjectFullName(targetEntity) + "] is not a data container");
            return;
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<DBDAttributeConstraint>();
        int visualPosition = 0;
        // Set conditions
        List<? extends DBSEntityAttributeRef> ownAttrs = CommonUtils.safeList(((DBSEntityReferrer) association).getAttributeReferences(monitor));
        List<? extends DBSEntityAttributeRef> refAttrs = CommonUtils.safeList(((DBSEntityReferrer) refConstraint).getAttributeReferences(monitor));
        assert ownAttrs.size() == refAttrs.size();
        for (int i = 0; i < ownAttrs.size(); i++) {
            DBSEntityAttributeRef ownAttr = ownAttrs.get(i);
            DBSEntityAttributeRef refAttr = refAttrs.get(i);
            DBDAttributeBinding ownBinding = model.getAttributeBinding(ownAttr.getAttribute());
            assert ownBinding != null;

            DBDAttributeConstraint constraint = new DBDAttributeConstraint(refAttr.getAttribute(), visualPosition++);
            constraint.setVisible(true);
            constraints.add(constraint);

            Object keyValue = model.getCellValue(ownBinding, row);
            constraint.setOperator(DBCLogicalOperator.EQUALS);
            constraint.setValue(keyValue);
            //constraint.setCriteria("='" + ownBinding.getValueHandler().getValueDisplayString(ownBinding.getAttribute(), keyValue, DBDDisplayFormat.NATIVE) + "'");
        }
        DBDDataFilter newFilter = new DBDDataFilter(constraints);

        runDataPump((DBSDataContainer) targetEntity, newFilter, 0, getSegmentMaxRows(), -1, null);
    }

    @Override
    public Control getControl()
    {
        return this.viewerPanel;
    }

    @NotNull
    public IWorkbenchPartSite getSite()
    {
        return site;
    }

    @NotNull
    public ResultSetModel getModel()
    {
        return model;
    }

    @Override
    public ResultSetModel getInput()
    {
        return model;
    }

    @Override
    public void setInput(Object input)
    {
        throw new IllegalArgumentException("ResultSet model can't be changed");
    }

    @Override
    @NotNull
    public ResultSetSelection getSelection()
    {
        return new ResultSetSelectionImpl();
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal)
    {
        if (selection instanceof ResultSetSelectionImpl && ((ResultSetSelectionImpl) selection).getResultSetViewer() == this) {
            // It may occur on simple focus change so we won't do anything
            return;
        }
        spreadsheet.deselectAll();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            List<GridPos> cellSelection = new ArrayList<GridPos>();
            for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                Object cell = iter.next();
                if (cell instanceof GridPos) {
                    cellSelection.add((GridPos) cell);
                } else {
                    log.warn("Bad selection object: " + cell);
                }
            }
            spreadsheet.selectCells(cellSelection);
            if (reveal) {
                spreadsheet.showSelection();
            }
        }
        fireSelectionChanged(new SelectionChangedEvent(this, selection));
    }

    @NotNull
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

        // Cache preferences
        IPreferenceStore preferenceStore = getPreferenceStore();
        showOddRows = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS);
        showCelIcons = preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS);

        // Pump data
        RowData oldRow = curRow;

        if (resultSetProvider.isReadyToRun() && getDataContainer() != null && dataPumpJob == null) {
            int segmentSize = getSegmentMaxRows();
            if (oldRow != null && oldRow.getVisualNumber() >= segmentSize && segmentSize > 0) {
                segmentSize = (oldRow.getVisualNumber() / segmentSize + 1) * segmentSize;
            }
            runDataPump(getDataContainer(), null, 0, segmentSize, oldRow == null ? -1 : oldRow.getVisualNumber(), new Runnable() {
                @Override
                public void run()
                {
                    if (!supportsDataFilter() && !model.getDataFilter().hasOrdering()) {
                        reorderLocally();
                    }
                }
            });
        }
    }

    public void refreshWithFilter(DBDDataFilter filter) {
        runDataPump(
            getDataContainer(),
            filter,
            0,
            getSegmentMaxRows(),
            -1,
            null);
    }

    private boolean isServerSideFiltering()
    {
        return
            getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE) &&
            (dataReceiver.isHasMoreData() || !CommonUtils.isEmpty(model.getDataFilter().getOrder()));
    }

    private void reorderResultSet(boolean force, @Nullable Runnable onSuccess)
    {
        if (force || isServerSideFiltering() && supportsDataFilter()) {
            if (resultSetProvider.isReadyToRun() && getDataContainer() != null && dataPumpJob == null) {
                int segmentSize = getSegmentMaxRows();
                if (curRow != null && curRow.getVisualNumber() >= segmentSize && segmentSize > 0) {
                    segmentSize = (curRow.getVisualNumber() / segmentSize + 1) * segmentSize;
                }
                runDataPump(getDataContainer(), null, 0, segmentSize, -1, onSuccess);
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
        model.resetOrdering();
    }

    synchronized void readNextSegment()
    {
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        if (getDataContainer() != null && !model.isUpdateInProgress() && dataPumpJob == null) {
            dataReceiver.setHasMoreData(false);
            dataReceiver.setNextSegmentRead(true);

            runDataPump(getDataContainer(), null, model.getRowCount(), getSegmentMaxRows(), -1, null);
        }
    }

    int getSegmentMaxRows()
    {
        if (getDataContainer() == null) {
            return 0;
        }
        return getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS);
    }

    @NotNull
    public IPreferenceStore getPreferenceStore()
    {
        DBPDataSource dataSource = getDataSource();
        if (dataSource != null) {
            return dataSource.getContainer().getPreferenceStore();
        }
        return DBeaverCore.getGlobalPreferenceStore();
    }

    synchronized void runDataPump(
        @NotNull final DBSDataContainer dataContainer,
        @Nullable final DBDDataFilter dataFilter,
        final int offset,
        final int maxRows,
        final int focusRow,
        @Nullable final Runnable finalizer)
    {
        if (dataPumpJob == null) {
            // Read data
            final DBDDataFilter useDataFilter = dataFilter != null ? dataFilter :
                (dataContainer == getDataContainer() ? model.getDataFilter() : null);
            dataPumpJob = new ResultSetDataPumpJob(
                dataContainer,
                useDataFilter,
                getDataReceiver(),
                getSpreadsheet());
            dataPumpJob.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void aboutToRun(IJobChangeEvent event) {
                    model.setUpdateInProgress(true);
                    getControl().getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            enableFilters(false);
                        }
                    });
                }

                @Override
                public void done(IJobChangeEvent event) {
                    ResultSetDataPumpJob job = (ResultSetDataPumpJob)event.getJob();
                    final Throwable error = job.getError();
                    if (job.getStatistics() != null) {
                        model.setStatistics(job.getStatistics());
                    }
                    getControl().getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run()
                        {
                            Control control = getControl();
                            if (control == null || control.isDisposed()) {
                                return;
                            }
                            final Shell shell = control.getShell();
                            if (error != null) {
                                setStatus(error.getMessage(), true);
                                UIUtils.showErrorDialog(
                                    shell,
                                    "Error executing query",
                                    "Query execution failed",
                                    error);
                            } else if (focusRow >= 0 && focusRow < model.getRowCount() && model.getVisibleColumnCount() > 0) {
                                // Seems to be refresh
                                // Restore original position
                                curRow = model.getRow(focusRow);
                                //curAttribute = model.getVisibleColumn(0);
                                if (recordMode) {
                                    updateRecordMode();
                                } else {
                                    updateStatusMessage();
                                }
                                GridCell newPos;
                                if (!recordMode) {
                                    newPos = new GridCell(curAttribute, curRow);
                                } else {
                                    newPos = new GridCell(curRow, curAttribute);
                                }
                                spreadsheet.setCursor(newPos, false);
                                previewValue();
                            } else {
                                curAttribute = null;
                                spreadsheet.redraw();
                            }

                            if (error == null) {
                                setNewState(dataContainer, dataFilter != null ? dataFilter :
                                    (dataContainer == getDataContainer() ? model.getDataFilter() : null));
                            }

                            model.setUpdateInProgress(false);
                            if (dataFilter != null) {
                                model.updateDataFilter(dataFilter);
                            }
                            updateFiltersText();

                            if (finalizer != null) {
                                finalizer.run();
                            }
                            dataPumpJob = null;
                        }
                    });
                }
            });
            dataPumpJob.setOffset(offset);
            dataPumpJob.setMaxRows(maxRows);
            dataPumpJob.schedule();
        } else {
            log.warn("Data read is in progress - can't run another");
        }
    }

    private void clearData()
    {
        model.clearData();
        this.curRow = null;
        this.curAttribute = null;
        this.columnOrder = SWT.NONE;
    }

    public void applyChanges(@Nullable DBRProgressMonitor monitor)
    {
        applyChanges(monitor, null);
    }

    /**
     * Saves changes to database
     * @param monitor monitor. If null then save will be executed in async job
     * @param listener finish listener (may be null)
     */
    public void applyChanges(@Nullable DBRProgressMonitor monitor, @Nullable ResultSetPersister.DataUpdateListener listener)
    {
        if (!model.isSingleSource()) {
            UIUtils.showErrorDialog(getControl().getShell(), "Apply changes error", "Can't save data for result set from multiple sources");
            return;
        }
        try {
            boolean needPK = false;
            for (RowData row : model.getAllRows()) {
                if (row.getState() == RowData.STATE_REMOVED || (row.getState() == RowData.STATE_NORMAL && row.isChanged())) {
                    needPK = true;
                    break;
                }
            }
            if (needPK) {
                // If we have deleted or updated rows then check for unique identifier
                if (!checkVirtualEntityIdentifier()) {
                    //UIUtils.showErrorDialog(getControl().getShell(), "Can't apply changes", "Can't apply data changes - not unique identifier defined");
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
        List<Object> selectedColumns = spreadsheet.getColumnSelection();
        IGridLabelProvider labelProvider = spreadsheet.getLabelProvider();
        StringBuilder tdt = new StringBuilder();
        if (copyHeader) {
            if (copyRowNumbers) {
                tdt.append("#");
            }
            for (Object column : selectedColumns) {
                if (tdt.length() > 0) {
                    tdt.append(delimiter);
                }
                tdt.append(labelProvider.getText(column));
            }
            tdt.append(lineSeparator);
        }

        List<GridCell> selectedCells = spreadsheet.getCellSelection();

        GridCell prevCell = null;
        for (GridCell cell : selectedCells) {
            if (prevCell == null || cell.row != prevCell.row) {
                // Next row
                if (prevCell != null && prevCell.col != cell.col) {
                    // Fill empty row tail
                    int prevColIndex = selectedColumns.indexOf(prevCell.col);
                    for (int i = prevColIndex; i < selectedColumns.size() - 1; i++) {
                        tdt.append(delimiter);
                    }
                }
                if (prevCell != null) {
                    tdt.append(lineSeparator);
                }
                if (copyRowNumbers) {
                    tdt.append(labelProvider.getText(cell.row)).append(delimiter);
                }
            }
            if (prevCell != null && prevCell.col != cell.col) {
                int prevColIndex = selectedColumns.indexOf(prevCell.col);
                int curColIndex = selectedColumns.indexOf(cell.col);
                for (int i = prevColIndex; i < curColIndex; i++) {
                    tdt.append(delimiter);
                }
            }

            DBDAttributeBinding column = (DBDAttributeBinding)(!recordMode ?  cell.col : cell.row);
            RowData row = (RowData) (!recordMode ?  cell.row : cell.col);
            Object value = model.getCellValue(column, row);
            String cellText = column.getValueHandler().getValueDisplayString(
                column.getAttribute(),
                value,
                format);
            tdt.append(cellText);

            if (cut) {
                DBDValueController valueController = new ResultSetValueController(
                    this, column, row, DBDValueController.EditType.NONE, null);
                if (!valueController.isReadOnly()) {
                    valueController.updateValue(DBUtils.makeNullValue(valueController));
                }
            }

            prevCell = cell;
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
        DBDAttributeBinding attr = getFocusAttribute();
        RowData row = getFocusRow();
        if (attr == null || row == null) {
            return;
        }
        if (isColumnReadOnly(attr)) {
            // No inline editors for readonly columns
            return;
        }
        try {
            Object newValue = getColumnValueFromClipboard(attr);
            if (newValue == null) {
                return;
            }
            new ResultSetValueController(
                this,
                attr,
                row,
                DBDValueController.EditType.NONE,
                null).updateValue(newValue);
        }
        catch (Exception e) {
            UIUtils.showErrorDialog(site.getShell(), "Cannot replace cell value", null, e);
        }
    }

    @Nullable
    private Object getColumnValueFromClipboard(DBDAttributeBinding metaColumn) throws DBCException
    {
        DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return null;
        }
        DBCSession session = dataSource.openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, "Copy from clipboard");
        try {
            String strValue = (String) getSpreadsheet().getClipboard().getContents(TextTransfer.getInstance());
            return metaColumn.getValueHandler().getValueFromObject(
                    session, metaColumn.getAttribute(), strValue, true);
        } finally {
            session.close();
        }
    }

    void addNewRow(final boolean copyCurrent)
    {
        GridPos curPos = spreadsheet.getCursorPosition();
        int rowNum;
        if (recordMode) {
            rowNum = curRow == null ? 0 : curRow.getVisualNumber();
        } else {
            rowNum = curPos.row;
        }
        if (rowNum >= model.getRowCount()) {
            rowNum = model.getRowCount() - 1;
        }
        if (rowNum < 0) {
            rowNum = 0;
        }

        final DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return;
        }

        // Add new row
        final DBDAttributeBinding[] columns = model.getColumns();
        final Object[] cells = new Object[columns.length];
        final int currentRowNumber = rowNum;
        // Copy cell values in new context
        DBCSession session = dataSource.openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, CoreMessages.controls_resultset_viewer_add_new_row_context_name);
        try {
            if (copyCurrent && currentRowNumber >= 0 && currentRowNumber < model.getRowCount()) {
                Object[] origRow = model.getRowData(currentRowNumber);
                for (int i = 0; i < columns.length; i++) {
                    DBDAttributeBinding metaColumn = columns[i];
                    DBSAttributeBase attribute = metaColumn.getAttribute();
                    if (attribute.isAutoGenerated() || attribute.isPseudoAttribute()) {
                        // set pseudo and autoincrement columns to null
                        cells[i] = null;
                    } else {
                        try {
                            cells[i] = metaColumn.getValueHandler().getValueFromObject(session, attribute, origRow[i], true);
                        } catch (DBCException e) {
                            log.warn(e);
                            try {
                                cells[i] = DBUtils.makeNullValue(session, metaColumn.getValueHandler(), attribute);
                            } catch (DBCException e1) {
                                log.warn(e1);
                            }
                        }
                    }
                }
            } else {
                // Initialize new values
                for (int i = 0; i < columns.length; i++) {
                    DBDAttributeBinding metaColumn = columns[i];
                    try {
                        cells[i] = DBUtils.makeNullValue(session, metaColumn.getValueHandler(), metaColumn.getAttribute());
                    } catch (DBCException e) {
                        log.warn(e);
                    }
                }
            }
        } finally {
            session.close();
        }
        model.addNewRow(rowNum, cells);
        refreshSpreadsheet(true);
        updateEditControls();
        fireResultSetChange();
    }

    void deleteSelectedRows()
    {
        Set<RowData> rowsToDelete = new LinkedHashSet<RowData>();
        if (recordMode) {
            rowsToDelete.add(curRow);
        } else {
            for (GridCell cell : spreadsheet.getCellSelection()) {
                rowsToDelete.add((RowData) cell.row);
            }
        }
        if (rowsToDelete.isEmpty()) {
            return;
        }

        int rowsRemoved = 0;
        int lastRowNum = -1;
        for (RowData row : rowsToDelete) {
            if (model.deleteRow(row)) {
                rowsRemoved++;
            }
            lastRowNum = row.getVisualNumber();
        }
        // Move one row down (if we are in grid mode)
        if (!recordMode && lastRowNum < spreadsheet.getItemCount() - 1) {
            GridCell newPos = new GridCell(curAttribute, model.getRow(lastRowNum - rowsRemoved + 1));
            spreadsheet.setCursor(newPos, false);
        }
        if (rowsRemoved > 0) {
            refreshSpreadsheet(true);
        } else {
            spreadsheet.redrawGrid();
        }
        updateEditControls();
        fireResultSetChange();
    }

    @Nullable
    static Image getTypeImage(DBSTypedObject column)
    {
        if (column instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)column).getObjectImage();
        } else {
            return DBIcon.TREE_COLUMN.getImage();
        }
    }

    //////////////////////////////////
    // Virtual identifier management

    @Nullable
    DBCEntityIdentifier getVirtualEntityIdentifier()
    {
        if (!model.isSingleSource() || model.getVisibleColumnCount() == 0) {
            return null;
        }
        DBDRowIdentifier rowIdentifier = model.getVisibleColumn(0).getRowIdentifier();
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
                // Empty identifier. We have to define it
                RunnableWithResult<Boolean> confirmer = new RunnableWithResult<Boolean>() {
                    @Override
                    public void run()
                    {
                        result = ValidateUniqueKeyUsageDialog.validateUniqueKey(ResultSetViewer.this);
                    }
                };
                UIUtils.runInUI(getControl().getShell(), confirmer);
                return confirmer.getResult();
            }
        }
        return true;
    }

    boolean editEntityIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        DBCEntityIdentifier virtualEntityIdentifier = getVirtualEntityIdentifier();
        if (virtualEntityIdentifier == null) {
            log.warn("No virtual identifier");
            return false;
        }
        DBVEntityConstraint constraint = (DBVEntityConstraint) virtualEntityIdentifier.getReferrer();

        EditConstraintDialog dialog = new EditConstraintDialog(
            getControl().getShell(),
            "Define virtual unique identifier",
            constraint);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        Collection<DBSEntityAttribute> uniqueColumns = dialog.getSelectedColumns();
        constraint.setAttributes(uniqueColumns);
        virtualEntityIdentifier = getVirtualEntityIdentifier();
        if (virtualEntityIdentifier == null) {
            log.warn("No virtual identifier defined");
            return false;
        }
        virtualEntityIdentifier.reloadAttributes(monitor, model.getColumns());
        DBPDataSource dataSource = getDataSource();
        if (dataSource != null) {
            dataSource.getContainer().persistConfiguration();
        }

        return true;
    }

    void clearEntityIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        DBDAttributeBinding firstColumn = model.getVisibleColumn(0);
        DBCEntityIdentifier identifier = firstColumn.getRowIdentifier().getEntityIdentifier();
        DBVEntityConstraint virtualKey = (DBVEntityConstraint) identifier.getReferrer();
        virtualKey.setAttributes(Collections.<DBSEntityAttribute>emptyList());
        identifier.reloadAttributes(monitor, model.getColumns());
        virtualKey.getParentObject().setProperty(DBVConstants.PROPERTY_USE_VIRTUAL_KEY_QUIET, null);

        DBPDataSource dataSource = getDataSource();
        if (dataSource != null) {
            dataSource.getContainer().persistConfiguration();
        }
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

    static class ResultSetValueController implements DBDAttributeController, DBDRowController {

        private final ResultSetViewer viewer;
        private final EditType editType;
        private final Composite inlinePlaceholder;
        private RowData curRow;
        private final DBDAttributeBinding binding;

        ResultSetValueController(
            @NotNull ResultSetViewer viewer,
            @NotNull DBDAttributeBinding binding,
            @NotNull RowData row,
            @NotNull EditType editType,
            @Nullable Composite inlinePlaceholder)
        {
            this.viewer = viewer;
            this.binding = binding;
            this.curRow = row;
            this.editType = editType;
            this.inlinePlaceholder = inlinePlaceholder;
        }

        void setCurRow(RowData curRow)
        {
            this.curRow = curRow;
        }

        @Nullable
        @Override
        public DBPDataSource getDataSource()
        {
            return viewer.getDataSource();
        }

        @Override
        public String getValueName()
        {
            return binding.getName();
        }

        @Override
        public DBSTypedObject getValueType()
        {
            return binding.getMetaAttribute();
        }

        @NotNull
        @Override
        public DBDRowController getRowController() {
            return this;
        }

        @NotNull
        @Override
        public DBDAttributeBinding getBinding()
        {
            return binding;
        }

        @NotNull
        @Override
        public String getColumnId() {
            DBPDataSource dataSource = getDataSource();
            DBCAttributeMetaData metaAttribute = binding.getMetaAttribute();
            return DBUtils.getSimpleQualifiedName(
                dataSource == null ? null : dataSource.getContainer().getName(),
                metaAttribute.getEntityName(),
                metaAttribute.getName());
        }

        @Override
        public Object getValue()
        {
            return viewer.spreadsheet.getContentProvider().getCellValue(curRow, binding, false);
        }

        @Override
        public void updateValue(@Nullable Object value)
        {
            if (viewer.model.updateCellValue(binding, curRow, value)) {
                // Update controls
                viewer.site.getShell().getDisplay().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        viewer.updateEditControls();
                        viewer.spreadsheet.redrawGrid();
                        viewer.previewValue();
                    }
                });
            }
            viewer.fireResultSetChange();
        }

        @Nullable
        @Override
        public DBDRowIdentifier getRowIdentifier()
        {
            return binding.getRowIdentifier();
        }

        @Override
        public DBDValueHandler getValueHandler()
        {
            return binding.getValueHandler();
        }

        @Override
        public EditType getEditType()
        {
            return editType;
        }

        @Override
        public boolean isReadOnly()
        {
            return viewer.isColumnReadOnly(binding);
        }

        @Override
        public IWorkbenchPartSite getValueSite()
        {
            return viewer.site;
        }

        @Nullable
        @Override
        public Composite getEditPlaceholder()
        {
            return inlinePlaceholder;
        }

        @Nullable
        @Override
        public ToolBar getEditToolBar()
        {
            return viewer.isPreviewVisible() ? viewer.previewPane.getToolBar() : null;
        }

        @Override
        public void closeInlineEditor()
        {
            viewer.spreadsheet.cancelInlineEditor();
        }

        @Override
        public void nextInlineEditor(boolean next) {
            viewer.spreadsheet.cancelInlineEditor();
            int colOffset = next ? 1 : -1;
            int rowOffset = 0;
            //final int rowCount = spreadsheet.getItemCount();
            final int colCount = viewer.spreadsheet.getColumnCount();
            final GridPos curPosition = viewer.spreadsheet.getCursorPosition();
            if (colOffset > 0 && curPosition.col + colOffset >= colCount) {
                colOffset = -colCount;
                rowOffset = 1;
            } else if (colOffset < 0 && curPosition.col + colOffset < 0) {
                colOffset = colCount;
                rowOffset = -1;
            }
            viewer.spreadsheet.shiftCursor(colOffset, rowOffset, false);
            viewer.showCellEditor(true);
        }

        public void registerEditor(DBDValueEditorStandalone editor) {
            viewer.openEditors.put(this, editor);
        }

        @Override
        public void unregisterEditor(DBDValueEditorStandalone editor) {
            viewer.openEditors.remove(this);
        }

        @Override
        public void showMessage(String message, boolean error)
        {
            viewer.setStatus(message, error);
        }

        @NotNull
        @Override
        public List<DBDAttributeBinding> getRowAttributes()
        {
            return Arrays.asList(viewer.model.getColumns());
        }

        @Nullable
        @Override
        public Object getAttributeValue(DBDAttributeBinding attribute)
        {
            return viewer.model.getCellValue(attribute, curRow);
        }

    }

    private class ContentProvider implements IGridContentProvider {

        @NotNull
        @Override
        public Object[] getElements(boolean horizontal) {
            if (horizontal) {
                // columns
                if (!recordMode) {
                    return model.getVisibleColumns().toArray();
                } else {
                    return curRow == null ? new Object[0] : new Object[] {curRow};
                }
            } else {
                // rows
                if (!recordMode) {
                    return model.getAllRows().toArray();
                } else {
                    DBDAttributeBinding[] columns = model.getVisibleColumns().toArray(new DBDAttributeBinding[model.getVisibleColumnCount()]);
                    if (columnOrder != SWT.NONE && columnOrder != SWT.DEFAULT) {
                        Arrays.sort(columns, new Comparator<DBDAttributeBinding>() {
                            @Override
                            public int compare(DBDAttributeBinding o1, DBDAttributeBinding o2) {
                                return o1.getName().compareTo(o2.getName()) * (columnOrder == SWT.DOWN ? 1 : -1);
                            }
                        });
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
                        if (recordMode) {
                            if (curRow != null) {
                                Object value = model.getCellValue(binding, curRow);
                                if (value instanceof DBDCollection) {
                                    return curRow.getCollectionData(
                                        binding,
                                        ((DBDCollection)value)).elements;
                                }
                            }
                            return null;
                        }
                    case STRUCT:
                    case ANY:
                        if (binding.getNestedBindings() != null) {
                            return binding.getNestedBindings().toArray();
                        }
                        break;
                }
            }

            return null;
        }

        @Override
        public int getSortOrder(@NotNull Object column)
        {
            if (column instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) column;
                if (!binding.hasNestedBindings()) {
                    DBDAttributeConstraint co = model.getDataFilter().getConstraint(binding);
                    if (co != null && co.getOrderPosition() > 0) {
                        return co.isOrderDescending() ? SWT.UP : SWT.DOWN;
                    }
                    return SWT.DEFAULT;
                }
            } else if (column == null && recordMode) {
                // Columns order in record mode
                return columnOrder;
            }
            return SWT.NONE;
        }

        @Override
        public ElementState getDefaultState(@NotNull Object element) {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding binding = (DBDAttributeBinding) element;
                switch (binding.getAttribute().getDataKind()) {
                    case STRUCT:
                        return ElementState.EXPANDED;
                    case ARRAY:
                        Object cellValue = model.getCellValue(binding, curRow);
                        if (cellValue instanceof DBDCollection && ((DBDCollection) cellValue).getItemCount() < 2) {
                            return ElementState.EXPANDED;
                        }
                        return ElementState.COLLAPSED;
                    default:
                        break;
                }
            }
            return ElementState.NONE;
        }

        @Override
        public int getCellState(Object colElement, Object rowElement) {
            int state = STATE_NONE;
            DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
            RowData row = (RowData)(recordMode ? colElement : rowElement);
            Object value = model.getCellValue(attr, row);
            if (!CommonUtils.isEmpty(attr.getReferrers()) && !DBUtils.isNullValue(value)) {
                state |= STATE_LINK;
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
            RowData row = (RowData)(colElement instanceof RowData ? colElement : rowElement);
            int rowNum = row.getVisualNumber();
            Object value = model.getCellValue(attr, row);

            if (rowNum > 0 && rowNum == model.getRowCount() - 1 && (recordMode || spreadsheet.isRowVisible(rowNum)) && dataReceiver.isHasMoreData()) {
                readNextSegment();
            }

            if (formatString) {
                if (recordMode && attr.getDataKind() == DBPDataKind.ARRAY && value instanceof DBDCollection) {
                    return "[" + ((DBDCollection) value).getItemCount() + "]";
                }
                return attr.getValueHandler().getValueDisplayString(
                    attr.getAttribute(),
                    value,
                    DBDDisplayFormat.UI);
            } else {
                return value;
            }
        }

        @Nullable
        @Override
        public Image getCellImage(Object colElement, Object rowElement)
        {
            if (!showCelIcons) {
                return null;
            }
            DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? rowElement : colElement);
            if ((attr.getValueHandler().getFeatures() & DBDValueHandler.FEATURE_SHOW_ICON) != 0) {
                return getTypeImage(attr.getMetaAttribute());
            } else {
                return null;
            }
        }

        @NotNull
        @Override
        public String getCellText(Object colElement, Object rowElement)
        {
            return String.valueOf(getCellValue(colElement, rowElement, true));
        }

        @Nullable
        @Override
        public Color getCellForeground(Object colElement, Object rowElement)
        {
            Object value = getCellValue(colElement, rowElement, false);
            if (DBUtils.isNullValue(value)) {
                return foregroundNull;
            } else {
                if (foregroundDefault == null) {
                    foregroundDefault = filtersText.getForeground();
                }
                return foregroundDefault;
            }
        }

        @Nullable
        @Override
        public Color getCellBackground(Object colElement, Object rowElement)
        {
            RowData row = (RowData) (!recordMode ?  rowElement : colElement);
            DBDAttributeBinding attribute = (DBDAttributeBinding)(!recordMode ?  colElement : rowElement);
            boolean odd = row.getVisualNumber() % 2 == 0;

            if (row.getState() == RowData.STATE_ADDED) {
                return backgroundAdded;
            }
            if (row.getState() == RowData.STATE_REMOVED) {
                return backgroundDeleted;
            }
            if (row.changes != null && row.changes.containsKey(attribute)) {
                return backgroundModified;
            }
            if ((attribute.getValueHandler().getFeatures() & DBDValueHandler.FEATURE_COMPOSITE) != 0) {
                return backgroundReadOnly;
            }
            if (!recordMode && odd && showOddRows) {
                return backgroundOdd;
            }

            if (backgroundNormal == null) {
                backgroundNormal = filtersText.getBackground();
            }
            return backgroundNormal;
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
            if (element instanceof DBDAttributeBinding && (!isRecordMode() || !model.isDynamicMetadata())) {
                return getTypeImage(((DBDAttributeBinding)element).getMetaAttribute());
            }
            return null;
        }

        @Nullable
        @Override
        public Color getForeground(Object element) {
            if (element == null) {
                if (foregroundDefault == null) {
                    foregroundDefault = filtersText.getForeground();
                }
                return foregroundDefault;
            }
            return null;
        }

        @Nullable
        @Override
        public Color getBackground(Object element) {
            if (backgroundNormal == null) {
                backgroundNormal = filtersText.getBackground();
            }
            if (element == null) {
                return backgroundNormal;
            }

/*
            RowData row = (RowData) (!recordMode ?  element : curRow);
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
                if (!recordMode) {
                    return String.valueOf(((RowData)element).getVisualNumber() + 1);
                } else {
                    return CoreMessages.controls_resultset_viewer_value;
                }
            }
        }

        @Nullable
        @Override
        public Font getFont(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
                DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(attributeBinding);
                if (constraint != null && constraint.hasFilter()) {
                    return boldFont;
                }
            }
            return null;
        }

        @Nullable
        @Override
        public String getTooltip(Object element)
        {
            if (element instanceof DBDAttributeBinding) {
                DBDAttributeBinding attributeBinding = (DBDAttributeBinding) element;
                String name = attributeBinding.getName();
                String typeName = DBUtils.getFullTypeName(attributeBinding.getAttribute());
                return name + ": " + typeName;
            }
            return null;
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
            Menu menu = getMenu(getSpreadsheet());
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

        @Nullable
        @Override
        public Menu getMenu(Menu parent)
        {
            return null;
        }

    }

    private class ShowFiltersAction extends Action {
        public ShowFiltersAction()
        {
            super(CoreMessages.controls_resultset_viewer_action_order_filter, DBIcon.FILTER.getImageDescriptor());
        }

        @Override
        public void run()
        {
            new FilterSettingsDialog(ResultSetViewer.this).open();
        }
    }

    private class ToggleServerSideOrderingAction extends Action {
        public ToggleServerSideOrderingAction()
        {
            super(CoreMessages.pref_page_database_resultsets_label_server_side_order);
        }

        @Override
        public int getStyle()
        {
            return AS_CHECK_BOX;
        }

        @Override
        public boolean isChecked()
        {
            return getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE);
        }

        @Override
        public void run()
        {
            IPreferenceStore preferenceStore = getPreferenceStore();
            preferenceStore.setValue(
                DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE,
                !preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE));
        }
    }

    private enum FilterByColumnType {
        VALUE(DBIcon.FILTER_VALUE.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, DBCLogicalOperator operator, boolean useDefault)
            {
                final DBDAttributeBinding attr = viewer.getFocusAttribute();
                final RowData row = viewer.getFocusRow();
                if (attr == null || row == null) {
                    return null;
                }
                Object cellValue = viewer.model.getCellValue(attr, row);
                if (operator == DBCLogicalOperator.LIKE && cellValue != null) {
                    cellValue = "%" + cellValue + "%";
                }
                return cellValue;
            }
        },
        INPUT(DBIcon.FILTER_INPUT.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, DBCLogicalOperator operator, boolean useDefault)
            {
                if (useDefault) {
                    return "..";
                } else {
                    RowData focusRow = viewer.getFocusRow();
                    if (focusRow == null) {
                        return null;
                    }
                    FilterValueEditDialog dialog = new FilterValueEditDialog(viewer, column, focusRow, operator);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        return dialog.getValue();
                    } else {
                        return null;
                    }
                }
            }
        },
        CLIPBOARD(DBIcon.FILTER_CLIPBOARD.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, DBCLogicalOperator operator, boolean useDefault)
            {
                try {
                    return viewer.getColumnValueFromClipboard(column);
                } catch (DBCException e) {
                    log.debug("Error copying from clipboard", e);
                    return null;
                }
            }
        },
        NONE(DBIcon.FILTER_VALUE.getImageDescriptor()) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, DBCLogicalOperator operator, boolean useDefault)
            {
                return null;
            }
        };

        final ImageDescriptor icon;

        private FilterByColumnType(ImageDescriptor icon)
        {
            this.icon = icon;
        }
        @Nullable
        abstract Object getValue(ResultSetViewer viewer, DBDAttributeBinding column, DBCLogicalOperator operator, boolean useDefault);
    }

    private String translateFilterPattern(DBCLogicalOperator operator, FilterByColumnType type, DBDAttributeBinding column)
    {
        Object value = type.getValue(this, column, operator, true);
        DBPDataSource dataSource = getDataSource();
        String strValue = dataSource == null ? String.valueOf(value) : SQLUtils.convertValueToSQL(dataSource, column, value);
        if (operator.getArgumentCount() == 0) {
            return operator.getStringValue();
        } else {
            return operator.getStringValue() + " " + CommonUtils.truncateString(strValue, 64);
        }
    }

    private class FilterByColumnAction extends Action {
        private final DBCLogicalOperator operator;
        private final FilterByColumnType type;
        private final DBDAttributeBinding column;
        public FilterByColumnAction(DBCLogicalOperator operator, FilterByColumnType type, DBDAttributeBinding column)
        {
            super(column.getName() + " " + translateFilterPattern(operator, type, column), type.icon);
            this.operator = operator;
            this.type = type;
            this.column = column;
        }

        @Override
        public void run()
        {
            Object value = type.getValue(ResultSetViewer.this, column, operator, false);
            if (operator.getArgumentCount() > 0 && value == null) {
                return;
            }
            DBDDataFilter filter = model.getDataFilter();
            DBDAttributeConstraint constraint = filter.getConstraint(column);
            if (constraint != null) {
                constraint.setOperator(operator);
                constraint.setValue(value);
                updateFiltersText();
                refresh();
            }
        }
    }

    private class FilterResetColumnAction extends Action {
        private final DBDAttributeBinding column;
        public FilterResetColumnAction(DBDAttributeBinding column)
        {
            super("Remove filter for '" + column.getName() + "'", DBIcon.REVERT.getImageDescriptor());
            this.column = column;
        }

        @Override
        public void run()
        {
            DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(column);
            if (constraint != null) {
                constraint.setCriteria(null);
                updateFiltersText();
                refresh();
            }
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

    private class ResultSetSelectionImpl implements ResultSetSelection {

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
        public Collection<RowData> getSelectedRows()
        {
            if (recordMode) {
                if (curRow == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(curRow);
            } else {
                List<RowData> rows = new ArrayList<RowData>();
                Collection<Integer> rowSelection = spreadsheet.getRowSelection();
                for (Integer row : rowSelection) {
                    rows.add(model.getRow(row));
                }
                return rows;
            }
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof ResultSetSelectionImpl && super.equals(obj);
        }
    }

    private class HistoryMenuListener extends SelectionAdapter {
        private final ToolItem dropdown;
        private final boolean back;
        public HistoryMenuListener(ToolItem item, boolean back) {
            this.dropdown = item;
            this.back = back;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            DBPDataSource dataSource = getDataSource();
            if (dataSource == null) {
                return;
            }
            if (e.detail == SWT.ARROW) {
                ToolItem item = (ToolItem) e.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

                Menu menu = new Menu(dropdown.getParent().getShell());
                menu.setLocation(pt.x, pt.y + rect.height);
                menu.setVisible(true);
                for (int i = historyPosition + (back ? -1 : 1); i >= 0 && i < stateHistory.size(); i += back ? -1 : 1) {
                    MenuItem mi = new MenuItem(menu, SWT.NONE);
                    StateItem state = stateHistory.get(i);
                    mi.setText(state.describeState(dataSource));
                    final int statePosition = i;
                    mi.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            navigateHistory(statePosition);
                        }
                    });
                }
            } else {
                int newPosition = back ? historyPosition - 1 : historyPosition + 1;
                navigateHistory(newPosition);
            }
        }
    }
}
