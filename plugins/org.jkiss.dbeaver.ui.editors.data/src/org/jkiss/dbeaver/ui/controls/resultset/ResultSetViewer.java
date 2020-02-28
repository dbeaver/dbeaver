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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.sql.DBSQLException;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.TabFolderReorder;
import org.jkiss.dbeaver.ui.controls.ToolbarSeparatorContribution;
import org.jkiss.dbeaver.ui.controls.VerticalButton;
import org.jkiss.dbeaver.ui.controls.VerticalFolder;
import org.jkiss.dbeaver.ui.controls.autorefresh.AutoRefreshControl;
import org.jkiss.dbeaver.ui.controls.resultset.colors.CustomizeColorsAction;
import org.jkiss.dbeaver.ui.controls.resultset.colors.ResetRowColorAction;
import org.jkiss.dbeaver.ui.controls.resultset.colors.SetRowColorAction;
import org.jkiss.dbeaver.ui.controls.resultset.handler.*;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.controls.resultset.panel.ResultSetPanelDescriptor;
import org.jkiss.dbeaver.ui.controls.resultset.valuefilter.FilterValueEditPopup;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.view.ErrorPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.view.StatisticsPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.virtual.*;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.ui.editors.data.preferences.PrefPageDataFormat;
import org.jkiss.dbeaver.ui.editors.data.preferences.PrefPageResultSetMain;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ResultSetViewer
 *
 * TODO: not-editable cells (struct owners in record mode)
 * TODO: PROBLEM. Multiple occurrences of the same struct type in a single table.
 * Need to make wrapper over DBSAttributeBase or something. Or maybe it is not a problem
 * because we search for binding by attribute only in constraints and for unique key columns which are unique?
 * But what PK has struct type?
 *
 */
public class ResultSetViewer extends Viewer
    implements DBPContextProvider, IResultSetController, ISaveablePart2, IAdaptable, DBPEventListener
{
    private static final Log log = Log.getLog(ResultSetViewer.class);

    private static final String TOOLBAR_GROUP_NAVIGATION = "navigation";
    private static final String TOOLBAR_GROUP_PRESENTATIONS = "presentations";
    private static final String TOOLBAR_GROUP_ADDITIONS = IWorkbenchActionConstants.MB_ADDITIONS;

    private static final String SETTINGS_SECTION_PRESENTATIONS = "presentations";

    private static final String TOOLBAR_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.controls.resultset.status";

    public static final String EMPTY_TRANSFORMER_NAME = "Default";
    public static final String CONTROL_ID = ResultSetViewer.class.getSimpleName();
    public static final String DEFAULT_QUERY_TEXT = "SQL";
    public static final String CUSTOM_FILTER_VALUE_STRING = "..";

    private static final DecimalFormat ROW_COUNT_FORMAT = new DecimalFormat("###,###,###,###,###,##0");
    private static final IResultSetListener[] EMPTY_LISTENERS = new IResultSetListener[0];

    private IResultSetFilterManager filterManager;
    @NotNull
    private final IWorkbenchPartSite site;
    private final Composite mainPanel;
    private final Composite viewerPanel;
    private final IResultSetDecorator decorator;
    @Nullable
    private ResultSetFilterPanel filtersPanel;
    private SashForm viewerSash;

    private final VerticalFolder panelSwitchFolder;
    private CTabFolder panelFolder;
    private ToolBarManager panelToolBar;

    private final VerticalFolder presentationSwitchFolder;
    private final Composite presentationPanel;

    private final List<ToolBarManager> toolbarList = new ArrayList<>();
    private Composite statusBar;
    private StatusLabel statusLabel;
    private ActiveStatusMessage rowCountLabel;
    private Text resultSetSize;

    private final DynamicFindReplaceTarget findReplaceTarget;

    // Presentation
    private IResultSetPresentation activePresentation;
    private ResultSetPresentationDescriptor activePresentationDescriptor;
    private List<ResultSetPresentationDescriptor> availablePresentations;
    private final List<ResultSetPanelDescriptor> availablePanels = new ArrayList<>();

    private final Map<ResultSetPresentationDescriptor, PresentationSettings> presentationSettings = new HashMap<>();
    private final Map<String, IResultSetPanel> activePanels = new HashMap<>();
    private final Map<String, ToolBarManager> activeToolBars = new HashMap<>();

    @NotNull
    private final IResultSetContainer container;
    @NotNull
    private final ResultSetDataReceiver dataReceiver;

    // Current row/col number
    @Nullable
    private ResultSetRow curRow;
    // Mode
    private boolean recordMode;

    private final List<IResultSetListener> listeners = new ArrayList<>();

    private final List<ResultSetJobDataRead> dataPumpJobQueue = new ArrayList<>();
    private final AtomicBoolean dataPumpRunning = new AtomicBoolean();

    private final ResultSetModel model = new ResultSetModel();
    private HistoryStateItem curState = null;
    private final List<HistoryStateItem> stateHistory = new ArrayList<>();
    private int historyPosition = -1;

    private AutoRefreshControl autoRefreshControl;
    private boolean actionsDisabled;
    private volatile boolean isUIUpdateRunning;

    private Color defaultBackground, defaultForeground;
    private GC sizingGC;
    private VerticalButton recordModeButton;

    public ResultSetViewer(@NotNull Composite parent, @NotNull IWorkbenchPartSite site, @NotNull IResultSetContainer container)
    {
        super();

        this.site = site;
        this.recordMode = false;
        this.container = container;
        this.decorator = container.createResultSetDecorator();
        this.dataReceiver = new ResultSetDataReceiver(this);

        this.filterManager = GeneralUtils.adapt(this, IResultSetFilterManager.class);
        if (this.filterManager == null) {
            this.filterManager = new SimpleFilterManager();
        }

        loadPresentationSettings();

        this.sizingGC = new GC(parent);
        this.defaultBackground = UIStyles.getDefaultTextBackground();
        this.defaultForeground = UIStyles.getDefaultTextForeground();

        boolean supportsPanels = (decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_PANELS) != 0;

        this.mainPanel = UIUtils.createPlaceholder(parent, supportsPanels ? 3 : 2);

        this.autoRefreshControl = new AutoRefreshControl(
            this.mainPanel, ResultSetViewer.class.getSimpleName(), monitor -> refreshData(null));

        if ((decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_FILTERS) != 0) {
            this.filtersPanel = new ResultSetFilterPanel(this, this.mainPanel);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = ((GridLayout)mainPanel.getLayout()).numColumns;
            this.filtersPanel.setLayoutData(gd);
        }

        this.presentationSwitchFolder = new VerticalFolder(mainPanel, SWT.LEFT);
        this.presentationSwitchFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        CSSUtils.setCSSClass(this.presentationSwitchFolder, DBStyles.COLORED_BY_CONNECTION_TYPE);

        this.viewerPanel = UIUtils.createPlaceholder(mainPanel, 1);
        this.viewerPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.viewerPanel.setData(CONTROL_ID, this);
        CSSUtils.setCSSClass(this.viewerPanel, DBStyles.COLORED_BY_CONNECTION_TYPE);
        UIUtils.setHelp(this.viewerPanel, IHelpContextIds.CTX_RESULT_SET_VIEWER);
        this.viewerPanel.setRedraw(false);

        if (supportsPanels) {
            this.panelSwitchFolder = new VerticalFolder(mainPanel, SWT.RIGHT);
            this.panelSwitchFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));
            CSSUtils.setCSSClass(this.panelSwitchFolder, DBStyles.COLORED_BY_CONNECTION_TYPE);
        } else {
            panelSwitchFolder = null;
        }

        try {
            this.findReplaceTarget = new DynamicFindReplaceTarget();

            this.viewerSash = UIUtils.createPartDivider(site.getPart(), this.viewerPanel, SWT.HORIZONTAL | SWT.SMOOTH);
            this.viewerSash.setLayoutData(new GridData(GridData.FILL_BOTH));

            this.presentationPanel = UIUtils.createPlaceholder(this.viewerSash, 1);
            this.presentationPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (supportsPanels) {
                this.panelFolder = new CTabFolder(this.viewerSash, SWT.FLAT | SWT.TOP);
                CSSUtils.setCSSClass(panelFolder, DBStyles.COLORED_BY_CONNECTION_TYPE);
                new TabFolderReorder(panelFolder);
                this.panelFolder.marginWidth = 0;
                this.panelFolder.marginHeight = 0;
                this.panelFolder.setMinimizeVisible(true);
                this.panelFolder.setMRUVisible(true);
                this.panelFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
                this.panelFolder.addListener(SWT.MouseDoubleClick, event -> {
                    if (event.button != 1) {
                        return;
                    }
                    CTabItem selectedItem = panelFolder.getItem(new Point(event.getBounds().x, event.getBounds().y));
                    if (selectedItem != null && selectedItem == panelFolder.getSelection()) {
                        togglePanelsMaximize();
                    }
                });

                this.panelToolBar = new ToolBarManager(SWT.HORIZONTAL | SWT.RIGHT | SWT.FLAT);
                ToolBar panelToolbarControl = this.panelToolBar.createControl(panelFolder);
                this.panelFolder.setTopRight(panelToolbarControl, SWT.RIGHT | SWT.WRAP);
                this.panelFolder.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        CTabItem activeTab = panelFolder.getSelection();
                        if (activeTab != null) {
                            setActivePanel((String) activeTab.getData());
                        }
                    }
                });
                this.panelFolder.addListener(SWT.Resize, event -> {
                    if (!viewerSash.isDisposed() && !isUIUpdateRunning) {
                        int[] weights = viewerSash.getWeights();
                        getPresentationSettings().panelRatio = weights[1];
                    }
                });
                this.panelFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
                    @Override
                    public void close(CTabFolderEvent event) {
                        CTabItem item = (CTabItem) event.item;
                        String panelId = (String) item.getData();
                        removePanel(panelId);
                    }

                    @Override
                    public void minimize(CTabFolderEvent event) {
                        showPanels(false, true, true);
                    }

                    @Override
                    public void maximize(CTabFolderEvent event) {

                    }
                });
                MenuManager panelsMenuManager = new MenuManager();
                panelsMenuManager.setRemoveAllWhenShown(true);
                panelsMenuManager.addMenuListener(manager -> {
                    for (IContributionItem menuItem : fillPanelsMenu()) {
                        panelsMenuManager.add(menuItem);
                    }
                });
                Menu panelsMenu = panelsMenuManager.createContextMenu(this.panelFolder);
                this.panelFolder.setMenu(panelsMenu);
                this.panelFolder.addDisposeListener(e -> panelsMenuManager.dispose());
            }

            showEmptyPresentation();

            if (supportsStatusBar()) {
                createStatusBar();
            }

            this.viewerPanel.addDisposeListener(e -> dispose());

            changeMode(false);
        } finally {
            this.viewerPanel.setRedraw(true);
        }

        updateFiltersText();

        DBPProject project = container.getProject();
        if (project != null) {
            project.getDataSourceRegistry().addDataSourceListener(this);
        }
    }

    @Override
    @NotNull
    public IResultSetContainer getContainer() {
        return container;
    }

    @NotNull
    @Override
    public IResultSetDecorator getDecorator() {
        return decorator;
    }

    AutoRefreshControl getAutoRefresh() {
        return autoRefreshControl;
    }

    public GC getSizingGC() {
        return sizingGC;
    }

    ////////////////////////////////////////////////////////////
    // Filters

    private boolean supportsPanels() {
        return (decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_PANELS) != 0 &&
            activePresentationDescriptor != null &&
            activePresentationDescriptor.supportsPanels();
    }

    public boolean supportsStatusBar() {
        return (decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_STATUS_BAR) != 0;
    }

    public boolean supportsDataFilter() {
        DBSDataContainer dataContainer = getDataContainer();
        return dataContainer != null &&
            (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_FILTER) == DBSDataContainer.DATA_FILTER;
    }

    public boolean supportsNavigation() {
        return activePresentationDescriptor != null && activePresentationDescriptor.supportsNavigation();
    }

    public boolean supportsEdit() {
        return activePresentationDescriptor != null && activePresentationDescriptor.supportsEdit();
    }

    public void resetDataFilter(boolean refresh)
    {
        setDataFilter(model.createDataFilter(), refresh);
    }

    public void showFilterSettingsDialog() {
        new FilterSettingsDialog(ResultSetViewer.this).open();
    }

    public void saveDataFilter() {
        DBCExecutionContext context = getExecutionContext();
        if (context == null) {
            log.error("Can't save data filter with null context");
            return;
        }
        DataFilterRegistry.getInstance().saveDataFilter(getDataContainer(), model.getDataFilter());

        if (filtersPanel != null) {
            DBeaverNotifications.showNotification(DBeaverNotifications.NT_GENERAL,
                "Data filter was saved",
                filtersPanel.getFilterText(),
                DBPMessageType.INFORMATION, null);
        }
    }

    public void switchFilterFocus() {
        if (filtersPanel == null) {
            return;
        }
        boolean filterFocused = filtersPanel.getEditControl().isFocusControl();
        if (filterFocused) {
            if (activePresentation != null) {
                activePresentation.getControl().setFocus();
            }
        } else {
            filtersPanel.getEditControl().setFocus();
        }
    }

    private void updateFiltersText()
    {
        updateFiltersText(true);
    }

    public void updateFiltersText(boolean resetFilterValue)
    {
        if (filtersPanel == null || this.viewerPanel.isDisposed()) {
            return;
        }
        if (resultSetSize != null && !resultSetSize.isDisposed()) {
            resultSetSize.setEnabled(getDataContainer() != null);
        }

        this.viewerPanel.setRedraw(false);
        try {
            boolean enableFilters = false;

            DBCExecutionContext context = getExecutionContext();
            if (context != null) {
                if (activePresentation instanceof StatisticsPresentation) {
                    enableFilters = false;
                } else {
                    StringBuilder where = new StringBuilder();
                    SQLUtils.appendConditionString(model.getDataFilter(), context.getDataSource(), null, where, true);
                    String whereCondition = where.toString().trim();
                    if (resetFilterValue) {
                        filtersPanel.setFilterValue(whereCondition);
                        if (!whereCondition.isEmpty()) {
                            filtersPanel.addFiltersHistory(whereCondition);
                        }
                    }

                    if (container.isReadyToRun() && !model.isUpdateInProgress()) {
                        enableFilters = true;
                    }
                }
            }
            filtersPanel.enableFilters(enableFilters);
            //presentationSwitchToolbar.setEnabled(enableFilters);
        } finally {
            this.viewerPanel.setRedraw(true);
        }
    }

    public void setDataFilter(final DBDDataFilter dataFilter, boolean refreshData)
    {
        //if (!model.getDataFilter().equals(dataFilter))
        {
            //model.setDataFilter(dataFilter);
            if (refreshData) {
                refreshWithFilter(dataFilter);
            } else {
                model.setDataFilter(dataFilter);
                activePresentation.refreshData(true, false, true);
                updateFiltersText();
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Misc

    @NotNull
    public DBPPreferenceStore getPreferenceStore()
    {
        DBCExecutionContext context = getExecutionContext();
        if (context != null) {
            return context.getDataSource().getContainer().getPreferenceStore();
        }
        return DBWorkbench.getPlatform().getPreferenceStore();
    }

    @NotNull
    @Override
    public Color getDefaultBackground() {
        if (filtersPanel == null) {
            return defaultBackground;
        }
        return UIStyles.getDefaultTextBackground();
    }

    @NotNull
    @Override
    public Color getDefaultForeground() {
        if (filtersPanel == null) {
            return defaultForeground;
        }
        return UIStyles.getDefaultTextForeground();
    }

    public void persistConfig() {
        DBCExecutionContext context = getExecutionContext();
        if (context != null) {
            context.getDataSource().getContainer().persistConfiguration();
        }
    }

    ////////////////////////////////////////////////////////////
    // Presentation & panels

    @NotNull
    public List<ResultSetPresentationDescriptor> getAvailablePresentations() {
        return availablePresentations != null ? availablePresentations : Collections.emptyList();
    }

    @Override
    @NotNull
    public IResultSetPresentation getActivePresentation() {
        return activePresentation;
    }

    @Override
    public void showEmptyPresentation() {
        activePresentationDescriptor = null;
        setActivePresentation(new EmptyPresentation());
        updatePresentationInToolbar();
    }

    void showErrorPresentation(String sqlText, String message, Throwable error) {
        activePresentationDescriptor = null;
        setActivePresentation(
            new ErrorPresentation(
                sqlText,
                GeneralUtils.makeErrorStatus(message, error)));
        updatePresentationInToolbar();
    }

    void updatePresentation(final DBCResultSet resultSet, boolean metadataChanged) {
        if (getControl().isDisposed()) {
            return;
        }
        boolean changed = false;
        try {
            isUIUpdateRunning = true;
            if (resultSet instanceof StatResultSet) {
                // Statistics - let's use special presentation for it
                availablePresentations = Collections.emptyList();
                setActivePresentation(new StatisticsPresentation());
                activePresentationDescriptor = null;
                changed = true;
            } else {
                // Regular results
                IResultSetContext context = new ResultSetContextImpl(this, resultSet);
                final List<ResultSetPresentationDescriptor> newPresentations = ResultSetPresentationRegistry.getInstance().getAvailablePresentations(resultSet, context);
                changed = CommonUtils.isEmpty(this.availablePresentations) || !newPresentations.equals(this.availablePresentations);
                this.availablePresentations = newPresentations;
                if (!this.availablePresentations.isEmpty()) {
                    if (activePresentationDescriptor != null && (!metadataChanged || activePresentationDescriptor.getPresentationType().isPersistent())) {
                        if (this.availablePresentations.contains(activePresentationDescriptor)) {
                            // Keep the same presentation
                            return;
                        }
                    }
                    String defaultPresentationId = getPreferenceStore().getString(ResultSetPreferences.RESULT_SET_PRESENTATION);
                    ResultSetPresentationDescriptor newPresentation = null;
                    if (!CommonUtils.isEmpty(defaultPresentationId)) {
                        for (ResultSetPresentationDescriptor pd : this.availablePresentations) {
                            if (pd.getId().equals(defaultPresentationId)) {
                                newPresentation = pd;
                                break;
                            }
                        }
                    }
                    changed = true;
                    if (newPresentation == null) {
                        newPresentation = this.availablePresentations.get(0);
                    }
                    try {
                        IResultSetPresentation instance = newPresentation.createInstance();
                        activePresentationDescriptor = newPresentation;
                        setActivePresentation(instance);
                    } catch (Throwable e) {
                        DBWorkbench.getPlatformUI().showError("Presentation activate", "Can't instantiate data view '" + newPresentation.getLabel() + "'", e);
                    }
                } else {
                    // No presentation for this resulset
                    log.debug("No presentations for result set [" + resultSet.getClass().getSimpleName() + "]");
                    showEmptyPresentation();
                }
            }
        } finally {
            if (changed && presentationSwitchFolder != null) {
                updatePresentationInToolbar();
            }
            isUIUpdateRunning = false;
        }

    }

    private void updatePresentationInToolbar() {
        // Update combo
        mainPanel.setRedraw(false);
        try {
            boolean pVisible = activePresentationDescriptor != null;
            ((GridData) presentationSwitchFolder.getLayoutData()).exclude = !pVisible;
            presentationSwitchFolder.setVisible(pVisible);
            if (!pVisible) {
                presentationSwitchFolder.setEnabled(false);
            } else {
                presentationSwitchFolder.setEnabled(true);
                for (Control item : presentationSwitchFolder.getChildren()) {
                    item.dispose();
                }
                for (ResultSetPresentationDescriptor pd : availablePresentations) {
                    VerticalButton item = new VerticalButton(presentationSwitchFolder, SWT.LEFT | SWT.RADIO);
                    item.setImage(DBeaverIcons.getImage(pd.getIcon()));
                    item.setText(pd.getLabel());
                    item.setToolTipText(pd.getDescription());
                    item.setData(pd);
                    if (pd == activePresentationDescriptor) {
                        presentationSwitchFolder.setSelection(item);
                    }
                    item.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (e.widget != null && e.widget.getData() != null) {
                                switchPresentation((ResultSetPresentationDescriptor) e.widget.getData());
                            }
                        }
                    });
                }
                UIUtils.createEmptyLabel(presentationSwitchFolder, 1, 1).setLayoutData(new GridData(GridData.FILL_VERTICAL));
                recordModeButton = new VerticalButton(presentationSwitchFolder, SWT.LEFT | SWT.CHECK);
                recordModeButton.setAction(new ToggleModeAction(), true);

                if (statusBar != null) {
                    ((GridLayout) presentationSwitchFolder.getLayout()).marginBottom = statusBar.getSize().y;
                }
            }
            mainPanel.layout(true, true);
        } catch (Exception e) {
            log.debug("Error updating presentation toolbar", e);
        } finally {
            // Enable redraw
            mainPanel.setRedraw(true);
        }
    }

    private void setActivePresentation(@NotNull IResultSetPresentation presentation) {
        boolean focusInPresentation = UIUtils.isParent(presentationPanel, viewerPanel.getDisplay().getFocusControl());

        // Dispose previous presentation and panels
        if (activePresentation != null) {
            activePresentation.dispose();
        }
        UIUtils.disposeChildControls(presentationPanel);
        if (panelFolder != null) {
            CTabItem curItem = panelFolder.getSelection();
            for (CTabItem panelItem : panelFolder.getItems()) {
                if (panelItem != curItem) {
                    panelItem.dispose();
                }
            }
            if (curItem != null) {
                curItem.dispose();
            }
        }

        // Set new presentation
        activePresentation = presentation;
        availablePanels.clear();
        activePanels.clear();

        IResultSetContext context = new ResultSetContextImpl(this, null);
        if (activePresentationDescriptor != null) {
            availablePanels.addAll(ResultSetPresentationRegistry.getInstance().getSupportedPanels(
                context, getDataSource(), activePresentationDescriptor.getId(), activePresentationDescriptor.getPresentationType()));
        } else if (activePresentation instanceof StatisticsPresentation) {
            // Stats presentation
            availablePanels.addAll(ResultSetPresentationRegistry.getInstance().getSupportedPanels(
                context, getDataSource(), null, IResultSetPresentation.PresentationType.COLUMNS));
        }
        activePresentation.createPresentation(this, presentationPanel);

        // Clear panels toolbar
        if (panelSwitchFolder != null) {
            UIUtils.disposeChildControls(panelSwitchFolder);
        }

        // Activate panels
        if (supportsPanels()) {
            boolean panelsVisible = false;
            boolean verticalLayout = false;
            int[] panelWeights = new int[]{700, 300};

            if (activePresentationDescriptor != null) {
                PresentationSettings settings = getPresentationSettings();
                panelsVisible = settings.panelsVisible;
                verticalLayout = settings.verticalLayout;
                if (settings.panelRatio > 0) {
                    panelWeights = new int[] {1000 - settings.panelRatio, settings.panelRatio};
                }
                activateDefaultPanels(settings);
            }
            viewerSash.setOrientation(verticalLayout ? SWT.VERTICAL : SWT.HORIZONTAL);
            viewerSash.setWeights(panelWeights);

            showPanels(panelsVisible, false, false);

            if (!availablePanels.isEmpty()) {
                VerticalButton panelsButton = new VerticalButton(panelSwitchFolder, SWT.RIGHT | SWT.CHECK);
                {
                    panelsButton.setText(ResultSetMessages.controls_resultset_config_panels);
                    panelsButton.setImage(DBeaverIcons.getImage(UIIcon.PANEL_CUSTOMIZE));
                    panelsButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            showPanels(!isPanelsVisible(), true, true);
                            panelsButton.setChecked(isPanelsVisible());
                            updatePanelsButtons();
                        }
                    });
                    String toolTip = ActionUtils.findCommandDescription(ResultSetHandlerMain.CMD_TOGGLE_PANELS, getSite(), false);
                    if (!CommonUtils.isEmpty(toolTip)) {
                        panelsButton.setToolTipText(toolTip);
                    }
                    panelsButton.setChecked(panelsVisible);
                }

                // Add all panels
                for (final ResultSetPanelDescriptor panel : availablePanels) {
                    VerticalButton panelButton = new VerticalButton(panelSwitchFolder, SWT.RIGHT | SWT.CHECK);
                    GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
                    gd.verticalIndent = 2;
                    gd.horizontalIndent = 1;
                    panelButton.setLayoutData(gd);
                    panelButton.setData(panel);
                    panelButton.setImage(DBeaverIcons.getImage(panel.getIcon()));
                    panelButton.setToolTipText(panel.getLabel());
                    String toolTip = ActionUtils.findCommandDescription(
                        ResultSetHandlerTogglePanel.CMD_TOGGLE_PANEL, getSite(), true,
                        ResultSetHandlerTogglePanel.PARAM_PANEL_ID, panel.getId());
                    if (!CommonUtils.isEmpty(toolTip)) {
                        panelButton.setToolTipText(panel.getLabel() + " (" + toolTip + ")");
                    }

                    panelButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            boolean isPanelVisible = isPanelsVisible() && isPanelVisible(panel.getId());
                            if (isPanelVisible) {
                                closePanel(panel.getId());
                            } else {
                                activatePanel(panel.getId(), true, true);
                            }
                            panelButton.setChecked(!isPanelVisible);
                            panelsButton.setChecked(isPanelsVisible());
                            if (panelSwitchFolder != null) {
                                panelSwitchFolder.redraw();
                            }
                        }
                    });
                    panelButton.setChecked(panelsVisible && isPanelVisible(panel.getId()));
                }

                UIUtils.createEmptyLabel(panelSwitchFolder, 1, 1).setLayoutData(new GridData(GridData.FILL_VERTICAL));
                VerticalButton.create(panelSwitchFolder, SWT.RIGHT | SWT.CHECK, getSite(), ResultSetHandlerMain.CMD_TOGGLE_LAYOUT, false);

            }
        } else {
            if (viewerSash != null) {
                viewerSash.setMaximizedControl(viewerSash.getChildren()[0]);
            }
        }

        mainPanel.layout(true, true);
        if (recordModeButton != null) {
            recordModeButton.setVisible(activePresentationDescriptor != null && activePresentationDescriptor.supportsRecordMode());
        }

        // Update dynamic find/replace target
        {
            IFindReplaceTarget nested = null;
            if (presentation instanceof IAdaptable) {
                nested = ((IAdaptable) presentation).getAdapter(IFindReplaceTarget.class);
            }
            findReplaceTarget.setTarget(nested);
        }

        if (!toolbarList.isEmpty()) {
            for (ToolBarManager tb : toolbarList) {
                tb.update(true);
            }
        }

        // Listen presentation selection change
        if (presentation instanceof ISelectionProvider) {
            ((ISelectionProvider) presentation).addSelectionChangedListener(this::fireResultSetSelectionChange);
        }


        // Set focus in presentation control
        // Use async exec to avoid focus switch after user UI interaction (e.g. combo)
        if (focusInPresentation) {
            UIUtils.asyncExec(() -> {
                Control control = activePresentation.getControl();
                if (control != null && !control.isDisposed()) {
                    control.setFocus();
                }
            });
        }
    }

    private void updatePanelsButtons() {
        boolean panelsVisible = isPanelsVisible();
        for (Control child : panelSwitchFolder.getChildren()) {
            if (child instanceof VerticalButton && child.getData() instanceof ResultSetPanelDescriptor) {
                boolean newChecked = panelsVisible &&
                    isPanelVisible(((ResultSetPanelDescriptor) child.getData()).getId());
                if (((VerticalButton) child).isChecked() != newChecked) {
                    ((VerticalButton) child).setChecked(newChecked);
                    child.redraw();
                }
            }
        }
    }

    /**
     * Switch to the next presentation
     */
    public void switchPresentation() {
        if (availablePresentations.size() < 2) {
            return;
        }
        int index = availablePresentations.indexOf(activePresentationDescriptor);
        if (index < availablePresentations.size() - 1) {
            index++;
        } else {
            index = 0;
        }
        switchPresentation(availablePresentations.get(index));
    }

    public void switchPresentation(ResultSetPresentationDescriptor selectedPresentation) {
        try {
            IResultSetPresentation instance = selectedPresentation.createInstance();
            activePresentationDescriptor = selectedPresentation;
            setActivePresentation(instance);
            instance.refreshData(true, false, false);

            if (presentationSwitchFolder != null) {
                for (VerticalButton item : presentationSwitchFolder.getItems()) {
                    if (item.getData() == activePresentationDescriptor) {
                        presentationSwitchFolder.setSelection(item);
                        break;
                    }
                }
            }

            // Save in global preferences
            if (activePresentationDescriptor.getPresentationType().isPersistent()) {
                // Save current presentation (only if it is persistent)
                DBWorkbench.getPlatform().getPreferenceStore().setValue(
                    ResultSetPreferences.RESULT_SET_PRESENTATION, activePresentationDescriptor.getId());
            }
            savePresentationSettings();
        } catch (Throwable e1) {
            DBWorkbench.getPlatformUI().showError(
                    "Presentation switch",
                "Can't switch presentation",
                e1);
        }
    }

    private void loadPresentationSettings() {
        IDialogSettings pSections = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_PRESENTATIONS);
        for (IDialogSettings pSection : ArrayUtils.safeArray(pSections.getSections())) {
            String pId = pSection.getName();
            ResultSetPresentationDescriptor presentation = ResultSetPresentationRegistry.getInstance().getPresentation(pId);
            if (presentation == null) {
                log.warn("Presentation '" + pId + "' not found. ");
                continue;
            }
            PresentationSettings settings = new PresentationSettings();
            String panelIdList = pSection.get("enabledPanelIds");
            if (panelIdList != null) {
                Collections.addAll(settings.enabledPanelIds, panelIdList.split(","));
            }
            settings.activePanelId = pSection.get("activePanelId");
            settings.panelRatio = pSection.getInt("panelRatio");
            settings.panelsVisible = pSection.getBoolean("panelsVisible");
            settings.verticalLayout = pSection.getBoolean("verticalLayout");
            presentationSettings.put(presentation, settings);
        }
    }

    private PresentationSettings getPresentationSettings() {
        PresentationSettings settings = this.presentationSettings.get(activePresentationDescriptor);
        if (settings == null) {
            settings = new PresentationSettings();
            // By default panels are visible for column presentations
            settings.panelsVisible = activePresentationDescriptor != null &&
                (activePresentationDescriptor.getPresentationType() == IResultSetPresentation.PresentationType.COLUMNS);
            this.presentationSettings.put(activePresentationDescriptor, settings);
        }
        return settings;
    }

    private void savePresentationSettings() {
        if ((decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_PANELS) != 0) {
            IDialogSettings pSections = ResultSetUtils.getViewerSettings(SETTINGS_SECTION_PRESENTATIONS);
            for (Map.Entry<ResultSetPresentationDescriptor, PresentationSettings> pEntry : presentationSettings.entrySet()) {
                if (pEntry.getKey() == null) {
                    continue;
                }
                String pId = pEntry.getKey().getId();
                PresentationSettings settings = pEntry.getValue();
                IDialogSettings pSection = UIUtils.getSettingsSection(pSections, pId);

                pSection.put("enabledPanelIds", CommonUtils.joinStrings(",", settings.enabledPanelIds));
                pSection.put("activePanelId", settings.activePanelId);
                pSection.put("panelRatio", settings.panelRatio);
                pSection.put("panelsVisible", settings.panelsVisible);
                pSection.put("verticalLayout", settings.verticalLayout);
            }
        }
    }

    @Override
    public IResultSetPanel getVisiblePanel() {
        return isPanelsVisible() ? activePanels.get(getPresentationSettings().activePanelId) : null;
    }

    String getActivePanelId() {
        return getPresentationSettings().activePanelId;
    }

    void closeActivePanel() {
        CTabItem activePanelItem = panelFolder.getSelection();
        if (activePanelItem != null) {
            activePanelItem.dispose();
        }
        if (panelFolder.getItemCount() <= 0) {
            showPanels(false, true, true);
        }
    }

    @Override
    public IResultSetPanel[] getActivePanels() {
        return activePanels.values().toArray(new IResultSetPanel[0]);
    }

    @Override
    public boolean activatePanel(String id, boolean setActive, boolean showPanels) {
        if (!supportsPanels()) {
            return false;
        }
        if (showPanels && !isPanelsVisible()) {
            showPanels(true, false, false);
        }

        PresentationSettings presentationSettings = getPresentationSettings();

        IResultSetPanel panel = activePanels.get(id);
        if (panel != null) {
            CTabItem panelTab = getPanelTab(id);
            if (panelTab != null) {
                if (setActive) {
                    panelFolder.setSelection(panelTab);
                    presentationSettings.activePanelId = id;
                }
                return true;
            } else {
                log.debug("Panel '" + id + "' tab not found");
            }
        }
        // Create panel
        ResultSetPanelDescriptor panelDescriptor = getPanelDescriptor(id);
        if (panelDescriptor == null) {
            log.debug("Panel '" + id + "' not found");
            return false;
        }
        try {
            panel = panelDescriptor.createInstance();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Can't show panel", "Can't create panel '" + id + "'", e);
            return false;
        }
        activePanels.put(id, panel);

        // Create control and tab item
        panelFolder.setRedraw(false);
        try {
            Control panelControl = panel.createContents(activePresentation, panelFolder);

            boolean firstPanel = panelFolder.getItemCount() == 0;
            CTabItem panelTab = new CTabItem(panelFolder, SWT.CLOSE);
            panelTab.setData(id);
            panelTab.setText(panelDescriptor.getLabel());
            panelTab.setImage(DBeaverIcons.getImage(panelDescriptor.getIcon()));
            panelTab.setToolTipText(panelDescriptor.getDescription());
            panelTab.setControl(panelControl);
            UIUtils.disposeControlOnItemDispose(panelTab);

            if (setActive || firstPanel) {
                panelFolder.setSelection(panelTab);
            }
        } finally {
            panelFolder.setRedraw(true);
        }

        presentationSettings.enabledPanelIds.add(id);
        if (setActive) {
            setActivePanel(id);
        }
        updatePanelsButtons();
        return true;
    }

    private void activateDefaultPanels(PresentationSettings settings) {
        // Cleanup unavailable panels
        settings.enabledPanelIds.removeIf(CommonUtils::isEmpty);

        // Add default panels if needed
        if (settings.enabledPanelIds.isEmpty()) {
            for (ResultSetPanelDescriptor pd : availablePanels) {
                if (pd.isShowByDefault()) {
                    settings.enabledPanelIds.add(pd.getId());
                }
            }
        }
        if (!settings.enabledPanelIds.contains(settings.activePanelId)) {
            settings.activePanelId = null;
        }
        if (!settings.enabledPanelIds.isEmpty()) {
            if (settings.activePanelId == null) {
                // Set first panel active
                settings.activePanelId = settings.enabledPanelIds.iterator().next();
            }
            for (String panelId : new ArrayList<>(settings.enabledPanelIds)) {
                if (!CommonUtils.isEmpty(panelId)) {
                    if (!activatePanel(panelId, panelId.equals(settings.activePanelId), false)) {
                        settings.enabledPanelIds.remove(panelId);

                    }
                }
            }
        }
    }

    private void setActivePanel(String panelId) {
        PresentationSettings settings = getPresentationSettings();
        settings.activePanelId = panelId;
        IResultSetPanel panel = activePanels.get(panelId);
        if (panel != null) {
            panel.activatePanel();
            updatePanelActions();
            savePresentationSettings();
        }
    }

    private void removePanel(String panelId) {
        IResultSetPanel panel = activePanels.remove(panelId);
        if (panel != null) {
            panel.deactivatePanel();
        }
        getPresentationSettings().enabledPanelIds.remove(panelId);
        if (activePanels.isEmpty()) {
            showPanels(false, true, true);
        }
        updatePanelsButtons();
    }

    private ResultSetPanelDescriptor getPanelDescriptor(String id) {
        for (ResultSetPanelDescriptor panel : availablePanels) {
            if (panel.getId().equals(id)) {
                return panel;
            }
        }
        return null;
    }

    private CTabItem getPanelTab(String panelId) {
        if (panelFolder != null) {
            for (CTabItem tab : panelFolder.getItems()) {
                if (CommonUtils.equalObjects(tab.getData(), panelId)) {
                    return tab;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isPanelsVisible() {
        return viewerSash != null && viewerSash.getMaximizedControl() == null;
    }

    public void showPanels(boolean show, boolean showDefaults, boolean saveSettings) {
        if (!supportsPanels() || show == isPanelsVisible()) {
            return;
        }
        CTabItem activePanelTab = panelFolder.getSelection();

        if (!show) {
            viewerSash.setMaximizedControl(viewerSash.getChildren()[0]);
            if (activePanelTab != null && !activePanelTab.getControl().isDisposed() && UIUtils.hasFocus(activePanelTab.getControl())) {
                // Set focus to presentation
                activePresentation.getControl().setFocus();
            }
        } else {
            if (showDefaults) {
                activateDefaultPanels(getPresentationSettings());
            }
            viewerSash.setMaximizedControl(null);
            updatePanelActions();
            updatePanelsContent(false);
            activePresentation.updateValueView();

            // Set focus to panel
            if (activePanelTab != null && !activePanelTab.getControl().isDisposed() && UIUtils.hasFocus(activePresentation.getControl())) {
                activePanelTab.getControl().setFocus();
            }
        }
        getPresentationSettings().panelsVisible = show;
        if (saveSettings) {
            savePresentationSettings();
        }
        updatePanelsButtons();
    }

    public void togglePanelsFocus() {
        boolean panelsActive = UIUtils.hasFocus(panelFolder);
        if (panelsActive) {
            presentationPanel.setFocus();
        } else if (panelFolder != null) {
            CTabItem activePanelTab = panelFolder.getSelection();
            if (activePanelTab != null && activePanelTab.getControl() != null) {
                activePanelTab.getControl().setFocus();
            }
        }
    }

    public boolean isPanelVisible(String panelId) {
        return getPresentationSettings().enabledPanelIds.contains(panelId);
    }

    public void closePanel(String panelId) {
        CTabItem panelTab = getPanelTab(panelId);
        if (panelTab != null) {
            panelTab.dispose();
            removePanel(panelId);
        }
    }

    public void toggleVerticalLayout() {
        PresentationSettings settings = getPresentationSettings();
        settings.verticalLayout = !settings.verticalLayout;
        viewerSash.setOrientation(settings.verticalLayout ? SWT.VERTICAL : SWT.HORIZONTAL);
        savePresentationSettings();
    }

    public void togglePanelsMaximize() {
        if (this.viewerSash.getMaximizedControl() == null) {
            this.viewerSash.setMaximizedControl(this.panelFolder);
        } else {
            this.viewerSash.setMaximizedControl(null);
        }
    }

    private List<IContributionItem> fillPanelsMenu() {
        List<IContributionItem> items = new ArrayList<>();

        for (final ResultSetPanelDescriptor panel : availablePanels) {
            CommandContributionItemParameter params = new CommandContributionItemParameter(
                site,
                panel.getId(),
                ResultSetHandlerTogglePanel.CMD_TOGGLE_PANEL,
                CommandContributionItem.STYLE_CHECK
            );
            Map<String, String> parameters = new HashMap<>();
            parameters.put(ResultSetHandlerTogglePanel.PARAM_PANEL_ID, panel.getId());
            params.parameters = parameters;
            items.add(new CommandContributionItem(params));
        }
        items.add(new Separator());
//        if (viewerSash.getMaximizedControl() == null) {
//            items.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_LAYOUT));
//        }
        items.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_MAXIMIZE));
//        items.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_PANELS));
        items.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ACTIVATE_PANELS));
        return items;
    }

    private void addDefaultPanelActions() {
        panelToolBar.add(new Action("View Menu", DBeaverIcons.getViewMenuImageDescriptor()) {
            @Override
            public void run() {
                ToolBar tb = panelToolBar.getControl();
                for (ToolItem item : tb.getItems()) {
                    if (item.getData() instanceof ActionContributionItem && ((ActionContributionItem) item.getData()).getAction() == this) {
                        MenuManager panelMenu = new MenuManager();
                        for (IContributionItem menuItem : fillPanelsMenu()) {
                            panelMenu.add(menuItem);
                        }
                        final Menu swtMenu = panelMenu.createContextMenu(panelToolBar.getControl());
                        Rectangle ib = item.getBounds();
                        Point displayAt = item.getParent().toDisplay(ib.x, ib.y + ib.height);
                        swtMenu.setLocation(displayAt);
                        swtMenu.setVisible(true);
                        tb.addDisposeListener(e -> panelMenu.dispose());
                        return;
                    }
                }
            }
        });
    }

    ////////////////////////////////////////////////////////////
    // Actions

    public boolean isActionsDisabled() {
        return actionsDisabled;
    }

    @Override
    public void lockActionsByControl(Control lockedBy) {
        if (checkDoubleLock(lockedBy)) {
            return;
        }
        actionsDisabled = true;
        lockedBy.addDisposeListener(e -> actionsDisabled = false);
    }

    @Override
    public void lockActionsByFocus(final Control lockedBy) {
        lockedBy.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                checkDoubleLock(lockedBy);
                actionsDisabled = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
                actionsDisabled = false;
            }
        });
        lockedBy.addDisposeListener(e -> actionsDisabled = false);
    }

    public boolean isPresentationInFocus() {
        Control activeControl = getActivePresentation().getControl();
        return !activeControl.isDisposed() && activeControl.isFocusControl();
    }

    private boolean checkDoubleLock(Control lockedBy) {
        if (actionsDisabled) {
            log.debug("Internal error: actions double-lock by [" + lockedBy + "]");
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        if (UIUtils.isUIThread()) {
            if (UIUtils.hasFocus(filtersPanel)) {
                T result = filtersPanel.getAdapter(adapter);
                if (result != null) {
                    return result;
                }
            } else if (UIUtils.hasFocus(panelFolder)) {
                IResultSetPanel visiblePanel = getVisiblePanel();
                if (visiblePanel instanceof IAdaptable) {
                    T adapted = ((IAdaptable) visiblePanel).getAdapter(adapter);
                    if (adapted != null) {
                        return adapted;
                    }
                }
            }
        }
        if (activePresentation != null) {
            if (adapter.isAssignableFrom(activePresentation.getClass())) {
                return adapter.cast(activePresentation);
            }
            // Try to get it from adapter
            if (activePresentation instanceof IAdaptable) {
                T adapted = ((IAdaptable) activePresentation).getAdapter(adapter);
                if (adapted != null) {
                    return adapted;
                }
            }
        }
        if (adapter == IFindReplaceTarget.class) {
            return adapter.cast(findReplaceTarget);
        }
        return null;
    }

    @Override
    public void addListener(IResultSetListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(IResultSetListener listener)
    {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void updateEditControls()
    {
        ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_EDITABLE);
        ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CHANGED);
        fireResultSetChange();
        updateToolbar();
        // Enable presentations
        for (VerticalButton pb : presentationSwitchFolder.getItems()) {
            if (pb.getData() instanceof ResultSetPresentationDescriptor) {
                pb.setVisible(!recordMode || ((ResultSetPresentationDescriptor) pb.getData()).supportsRecordMode());
            }
        }
    }

    /**
     * It is a hack function. Generally all command associated widgets should be updated automatically by framework.
     * Freaking E4 do not do it. I've spent a couple of days fighting it. Guys, you owe me.
     */
    private void updateToolbar()
    {
        for (ToolBarManager tb : toolbarList) {
            UIUtils.updateContributionItems(tb);
        }
        if (panelToolBar != null) {
            UIUtils.updateContributionItems(panelToolBar);
        }
    }

    public void redrawData(boolean attributesChanged, boolean rowsChanged)
    {
        if (viewerPanel.isDisposed()) {
            return;
        }
        if (rowsChanged) {
            int rowCount = model.getRowCount();
            if (curRow == null || curRow.getVisualNumber() >= rowCount) {
                curRow = rowCount == 0 ? null : model.getRow(rowCount - 1);
            }

            // Set cursor on new row
            if (!recordMode) {
                this.updateFiltersText();
            }
        }
        activePresentation.refreshData(attributesChanged || (rowsChanged && recordMode), false, true);
        //this.updateStatusMessage();
    }

    private void createStatusBar()
    {
        statusBar = new Composite(viewerPanel, SWT.NONE);
        statusBar.setBackgroundMode(SWT.INHERIT_FORCE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        statusBar.setLayoutData(gd);
        CSSUtils.setCSSClass(statusBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
        RowLayout toolbarsLayout = new RowLayout(SWT.HORIZONTAL);
        toolbarsLayout.marginTop = 0;
        toolbarsLayout.marginBottom = 0;
        toolbarsLayout.center = true;
        toolbarsLayout.wrap = true;
        toolbarsLayout.pack = true;
        //toolbarsLayout.fill = true;
        statusBar.setLayout(toolbarsLayout);

        {
            ToolBarManager editToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);

            // handle own commands
            editToolBarManager.add(new Separator());
            editToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_APPLY_CHANGES, "Save", null, null, true));
            editToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_REJECT_CHANGES, "Cancel", null, null, true));
            editToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_GENERATE_SCRIPT, "Script", null, null, true));
            editToolBarManager.add(new Separator());
            editToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_EDIT));
            editToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_ADD));
            editToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_COPY));
            editToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_DELETE));

            ToolBar editorToolBar = editToolBarManager.createControl(statusBar);
            CSSUtils.setCSSClass(editorToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);

            toolbarList.add(editToolBarManager);
        }
        {
            ToolBarManager navToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            navToolBarManager.add(new ToolbarSeparatorContribution(true));
            navToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_FIRST));
            navToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_PREVIOUS));
            navToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_NEXT));
            navToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_LAST));
            navToolBarManager.add(new Separator());
            navToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FETCH_PAGE));
            navToolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FETCH_ALL));
            navToolBarManager.add(new Separator(TOOLBAR_GROUP_NAVIGATION));
            ToolBar navToolBar = navToolBarManager.createControl(statusBar);
            CSSUtils.setCSSClass(navToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);

            toolbarList.add(navToolBarManager);
        }
        {
            ToolBarManager configToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            configToolBarManager.add(new ToolbarSeparatorContribution(true));

/*
            if (supportsPanels()) {
                CommandContributionItemParameter ciParam = new CommandContributionItemParameter(
                    site,
                    "org.jkiss.dbeaver.core.resultset.panels",
                    ResultSetHandlerMain.CMD_TOGGLE_PANELS,
                    CommandContributionItem.STYLE_PULLDOWN);
                ciParam.label = ResultSetMessages.controls_resultset_config_panels;
                ciParam.mode = CommandContributionItem.MODE_FORCE_TEXT;
                configToolBarManager.add(new CommandContributionItem(ciParam));
            }
            configToolBarManager.add(new ToolbarSeparatorContribution(true));
*/

            ToolBar configToolBar = configToolBarManager.createControl(statusBar);
            CSSUtils.setCSSClass(configToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
            toolbarList.add(configToolBarManager);
        }

        {
            ToolBarManager addToolbBarManagerar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            addToolbBarManagerar.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_EXPORT));

            addToolbBarManagerar.add(new GroupMarker(TOOLBAR_GROUP_PRESENTATIONS));
            addToolbBarManagerar.add(new Separator(TOOLBAR_GROUP_ADDITIONS));

            final IMenuService menuService = getSite().getService(IMenuService.class);
            if (menuService != null) {
                menuService.populateContributionManager(addToolbBarManagerar, TOOLBAR_CONTRIBUTION_ID);
            }
            ToolBar addToolBar = addToolbBarManagerar.createControl(statusBar);
            CSSUtils.setCSSClass(addToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
            toolbarList.add(addToolbBarManagerar);
        }

        {
            // Config toolbar
            ToolBarManager configToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            configToolBarManager.add(new ToolbarSeparatorContribution(true));
            configToolBarManager.add(new ConfigAction());
            configToolBarManager.update(true);
            ToolBar configToolBar = configToolBarManager.createControl(statusBar);
            CSSUtils.setCSSClass(configToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
            toolbarList.add(configToolBarManager);
        }
        {
            final int fontHeight = UIUtils.getFontHeight(statusBar);

            resultSetSize = new Text(statusBar, SWT.BORDER);
            resultSetSize.setLayoutData(new RowData(5 * fontHeight, SWT.DEFAULT));
            resultSetSize.setBackground(UIStyles.getDefaultTextBackground());
            resultSetSize.setToolTipText(DataEditorsMessages.resultset_segment_size);
            resultSetSize.addModifyListener(e -> {
                DBSDataContainer dataContainer = getDataContainer();
                int fetchSize = CommonUtils.toInt(resultSetSize.getText());
                if (fetchSize > 0 && dataContainer != null && dataContainer.getDataSource() != null) {
                    DBPPreferenceStore store = dataContainer.getDataSource().getContainer().getPreferenceStore();
                    int oldFetchSize = store.getInt(ModelPreferences.RESULT_SET_MAX_ROWS);
                    if (oldFetchSize > 0 && oldFetchSize != fetchSize) {
                        store.setValue(ModelPreferences.RESULT_SET_MAX_ROWS, fetchSize);
                        PrefUtils.savePreferenceStore(store);
                    }
                }
            });
            UIUtils.addDefaultEditActionsSupport(site, resultSetSize);

            rowCountLabel = new ActiveStatusMessage(statusBar, DBeaverIcons.getImage(UIIcon.RS_REFRESH), ResultSetMessages.controls_resultset_viewer_calculate_row_count, this) {
                @Override
                protected boolean isActionEnabled() {
                    return hasData();
                }

                @Override
                protected ILoadService<String> createLoadService() {
                    return new DatabaseLoadService<String>("Load row count", getExecutionContext()) {
                        @Override
                        public String evaluate(DBRProgressMonitor monitor) {
                            try {
                                long rowCount = readRowCount(monitor);
                                return ROW_COUNT_FORMAT.format(rowCount);
                            } catch (DBException e) {
                                log.error(e);
                                return e.getMessage();
                            }
                        }
                    };
                }
            };
            //rowCountLabel.setLayoutData();
            CSSUtils.setCSSClass(rowCountLabel, DBStyles.COLORED_BY_CONNECTION_TYPE);
            rowCountLabel.setMessage("Row Count");

            UIUtils.createToolBarSeparator(statusBar, SWT.VERTICAL);
            statusLabel = new StatusLabel(statusBar, SWT.NONE, this);
            statusLabel.setLayoutData(new RowData(30 * fontHeight, SWT.DEFAULT));
            CSSUtils.setCSSClass(statusLabel, DBStyles.COLORED_BY_CONNECTION_TYPE);

            statusBar.addListener(SWT.Resize, event -> {

            });
        }
    }

    @Nullable
    public DBPDataSource getDataSource() {
        return getDataContainer() == null ? null : getDataContainer().getDataSource();
    }

    @Nullable
    public DBSDataContainer getDataContainer()
    {
        return curState != null ? curState.dataContainer : container.getDataContainer();
    }

    public void setDataContainer(DBSDataContainer targetEntity, DBDDataFilter newFilter) {
        // Workaround for script results
        // In script mode history state isn't updated so we check for it here
        if (curState == null) {
            setNewState(targetEntity, model.getDataFilter());
        }
        runDataPump(targetEntity, newFilter, 0, getSegmentMaxRows(), -1, true, false, false, null);
    }

    ////////////////////////////////////////////////////////////
    // Grid/Record mode

    @Override
    public boolean isRecordMode() {
        return recordMode;
    }

    public void toggleMode()
    {
        changeMode(!recordMode);
        if (recordModeButton != null) {
            recordModeButton.redraw();
        }

        updateEditControls();
    }

    private void changeMode(boolean recordMode)
    {
        //Object state = savePresentationState();
        this.recordMode = recordMode;
        //redrawData(false);
        activePresentation.refreshData(true, false, false);
        activePresentation.changeMode(recordMode);
        updateStatusMessage();
        //restorePresentationState(state);
    }

    ////////////////////////////////////////////////////////////
    // Misc

    private void dispose()
    {
        if (activePresentation != null) {
            activePresentation.dispose();
        }
        DBPProject project = container.getProject();
        if (project != null) {
            project.getDataSourceRegistry().removeDataSourceListener(this);
        }

        savePresentationSettings();
        clearData();

        for (ToolBarManager tb : toolbarList) {
            try {
                tb.dispose();
            } catch (Throwable e) {
                // ignore
                log.debug("Error disposing toolbar " + tb, e);
            }
        }
        toolbarList.clear();

        UIUtils.dispose(this.sizingGC);
    }

    @Override
    public String getAttributeReadOnlyStatus(DBDAttributeBinding attr) {
        String dataStatus = getReadOnlyStatus();
        if (dataStatus != null) {
            return dataStatus;
        }
        boolean newRow = (curRow != null && curRow.getState() == ResultSetRow.STATE_ADDED);
        if (!newRow) {
            return model.getAttributeReadOnlyStatus(attr);
        }
        return null;
    }

    private Object savePresentationState() {
        Object[] state = new Object[1];
        if (activePresentation instanceof IStatefulControl) {
            UIUtils.syncExec(() ->
                state[0] =  ((IStatefulControl) activePresentation).saveState());
        }
        return state[0];
    }

    private boolean restorePresentationState(Object state) {
        if (activePresentation instanceof IStatefulControl) {
            UIUtils.syncExec(() ->
                ((IStatefulControl) activePresentation).restoreState(state));
            return true;
        }
        return false;
    }

    ///////////////////////////////////////
    // History

    List<HistoryStateItem> getStateHistory() {
        return stateHistory;
    }

    private void setNewState(DBSDataContainer dataContainer, @Nullable DBDDataFilter dataFilter) {
        // Create filter copy to avoid modifications
        dataFilter = new DBDDataFilter(dataFilter == null ? model.getDataFilter() : dataFilter);
        // Search in history
        for (int i = 0; i < stateHistory.size(); i++) {
            HistoryStateItem item = stateHistory.get(i);
            if (item.dataContainer == dataContainer && item.filter != null && item.filter.equalFilters(dataFilter, false)) {
                item.filter = dataFilter; // Update data filter - it may contain some orderings
                curState = item;
                historyPosition = i;
                return;
            }
        }
        // Save current state in history
        while (historyPosition < stateHistory.size() - 1) {
            stateHistory.remove(stateHistory.size() - 1);
        }
        curState = new HistoryStateItem(
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

    ///////////////////////////////////////
    // Misc

    @Nullable
    public ResultSetRow getCurrentRow()
    {
        return curRow;
    }

    @Override
    public void setCurrentRow(@Nullable ResultSetRow curRow) {
        this.curRow = curRow;
        if (curState != null && curRow != null) {
            curState.rowNumber = curRow.getVisualNumber();
        }
//        if (recordMode) {
//            updateRecordMode();
//        }
    }

    ///////////////////////////////////////
    // Status

    public void setStatus(String status)
    {
        setStatus(status, DBPMessageType.INFORMATION);
    }

    public void setStatus(String status, DBPMessageType messageType)
    {
        if (statusLabel == null || statusLabel.isDisposed()) {
            return;
        }
        statusLabel.setStatus(status, messageType);
        rowCountLabel.updateActionState();

        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null && dataContainer.getDataSource() != null) {
            resultSetSize.setText(String.valueOf(dataContainer.getDataSource().getContainer().getPreferenceStore().getInt(ModelPreferences.RESULT_SET_MAX_ROWS)));
        }
    }

    public void updateStatusMessage()
    {
        String statusMessage;
        if (model.getRowCount() == 0) {
            if (model.getVisibleAttributeCount() == 0) {
                statusMessage = ResultSetMessages.controls_resultset_viewer_status_empty + getExecutionTimeMessage();
            } else {
                statusMessage = ResultSetMessages.controls_resultset_viewer_status_no_data + getExecutionTimeMessage();
            }
        } else {
            if (recordMode) {
                statusMessage =
                    ResultSetMessages.controls_resultset_viewer_status_row + (curRow == null ? 0 : curRow.getVisualNumber() + 1) +
                        "/" + model.getRowCount() +
                    (curRow == null ? getExecutionTimeMessage() : "");
            } else {
                long rowsFetched, rowsUpdated = -1;
                DBCStatistics stats = getModel().getStatistics();
                if (stats == null || stats.isEmpty()) {
                    rowsFetched = getModel().getRowCount();
                } else {
                    rowsFetched = stats.getRowsFetched();
                    rowsUpdated = stats.getRowsUpdated();
                }
                if (rowsFetched < 0 && rowsUpdated >= 0) {
                    statusMessage =
                        ResultSetUtils.formatRowCount(rowsUpdated) +
                            ResultSetMessages.controls_resultset_viewer_status_rows_updated + getExecutionTimeMessage();
                } else {
                    statusMessage =
                        ResultSetUtils.formatRowCount(rowsFetched) +
                            ResultSetMessages.controls_resultset_viewer_status_rows_fetched + getExecutionTimeMessage();
                }
            }
        }
        boolean hasWarnings = !dataReceiver.getErrorList().isEmpty();
        if (hasWarnings) {
            statusMessage += " - " + dataReceiver.getErrorList().size() + " warning(s)";
        }
        if (getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_CONNECTION_NAME)) {
            DBSDataContainer dataContainer = getDataContainer();
            if (dataContainer != null) {
                DBPDataSource dataSource = dataContainer.getDataSource();
                if (dataSource != null) {
                    statusMessage += " [" + dataSource.getContainer().getName() + "]";
                }
            }
        }
        setStatus(statusMessage, hasWarnings ? DBPMessageType.WARNING : DBPMessageType.INFORMATION);

        if (rowCountLabel != null && !rowCountLabel.isDisposed()) {
            // Update row count label
            String rcMessage;
            if (!hasData()) {
                rcMessage = "No Data";
            } else if (!isHasMoreData()) {
                rcMessage = ROW_COUNT_FORMAT.format(model.getRowCount());
            } else {
                if (model.getTotalRowCount() == null) {
                    rcMessage = ROW_COUNT_FORMAT.format(model.getRowCount()) + "+";
                } else {
                    // We know actual row count
                    rcMessage = ROW_COUNT_FORMAT.format(model.getTotalRowCount());
                }
            }
            if (!CommonUtils.equalObjects(rowCountLabel.getMessage(), rcMessage)) {
                rowCountLabel.setMessage(rcMessage);
                rowCountLabel.updateActionState();
                statusBar.layout(true, true);
            }
        }
    }

    private String getExecutionTimeMessage()
    {
        DBCStatistics statistics = model.getStatistics();
        if (statistics == null || statistics.isEmpty()) {
            return "";
        }
        long fetchTime = statistics.getFetchTime();
        long totalTime = statistics.getTotalTime();
        if (fetchTime <= 0) {
            return " - " + RuntimeUtils.formatExecutionTime(totalTime);
        } else {
            return " - " + RuntimeUtils.formatExecutionTime(statistics.getExecuteTime()) + " (+" + RuntimeUtils.formatExecutionTime(fetchTime) + ")";
        }
    }

    ///////////////////////////////////////
    // Ordering

    @Override
    public void toggleSortOrder(DBDAttributeBinding columnElement, boolean forceAscending, boolean forceDescending) {
        DBDDataFilter dataFilter = getModel().getDataFilter();
        if (forceAscending) {
            dataFilter.resetOrderBy();
        }
        DBDAttributeBinding metaColumn = columnElement;
        DBDAttributeConstraint constraint = dataFilter.getConstraint(metaColumn);
        assert constraint != null;
        //int newSort;
        if (constraint.getOrderPosition() == 0) {
            if (ResultSetUtils.isServerSideFiltering(this) && supportsDataFilter()) {
                if (ConfirmationDialog.showConfirmDialogNoToggle(
                    ResourceBundle.getBundle(ResultSetMessages.BUNDLE_NAME),
                    viewerPanel.getShell(),
                    ResultSetPreferences.CONFIRM_ORDER_RESULTSET,
                    ConfirmationDialog.QUESTION,
                    ConfirmationDialog.WARNING,
                    metaColumn.getName()) != IDialogConstants.YES_ID)
                {
                    return;
                }
            }
            constraint.setOrderPosition(dataFilter.getMaxOrderingPosition() + 1);
            constraint.setOrderDescending(forceDescending);
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
        // Remove custom ordering. We can't use both custom and attribute-based ordering at once
        // Also it is required to implement default grouping ordering (count desc)
        dataFilter.setOrder(null);

        if (!ResultSetUtils.isServerSideFiltering(this) || !this.isHasMoreData()) {
            if (!this.checkForChanges()) {
                return;
            }
            reorderLocally();
        } else {
            this.refreshData(null);
        }
    }

    private void reorderLocally()
    {
        this.rejectChanges();
        this.getModel().resetOrdering();
        this.getActivePresentation().refreshData(false, false, true);
        this.updateFiltersText();
    }


    ///////////////////////////////////////
    // Data & metadata

    /**
     * Sets new metadata of result set
     * @param resultSet  resultset
     * @param attributes attributes metadata
     */
    void setMetaData(@NotNull DBCResultSet resultSet, @NotNull DBDAttributeBinding[] attributes)
    {
        model.setMetaData(resultSet, attributes);
        activePresentation.clearMetaData();
    }

    void setData(List<Object[]> rows, int focusRow)
    {
        if (viewerPanel.isDisposed()) {
            return;
        }
        this.curRow = null;
        this.model.setData(rows);
        this.curRow = (this.model.getRowCount() > 0 ? this.model.getRow(0) : null);
        if (focusRow > 0 && focusRow < model.getRowCount()) {
            this.curRow = model.getRow(focusRow);
        }

        {

            Boolean autoRecordMode = getDecorator().getAutoRecordMode();
            if (autoRecordMode != null ||
                (model.isMetadataChanged() && getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_AUTO_SWITCH_MODE)))
            {
                boolean newRecordMode;
                if (autoRecordMode != null) {
                    if (rows.size() <= 1) {
                        newRecordMode = autoRecordMode;
                    } else {
                        newRecordMode = false;
                    }
                } else {
                    newRecordMode = (rows.size() <= 1);
                }
                if (newRecordMode != recordMode) {
                    UIUtils.asyncExec(this::toggleMode);
                }
            }
        }
    }

    void appendData(List<Object[]> rows, boolean resetOldRows)
    {
        model.appendData(rows, resetOldRows);

        UIUtils.asyncExec(() -> {
            setStatus(NLS.bind(ResultSetMessages.controls_resultset_viewer_status_rows_size, model.getRowCount(), rows.size()) + getExecutionTimeMessage());

            updateEditControls();
        });
    }

    @Override
    public int promptToSaveOnClose()
    {
        if (!isDirty()) {
            return ISaveablePart2.YES;
        }
        int result = ConfirmationDialog.showConfirmDialog(
            ResourceBundle.getBundle(ResultSetMessages.BUNDLE_NAME),
            viewerPanel.getShell(),
            ResultSetPreferences.CONFIRM_RS_EDIT_CLOSE,
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
        doSave(RuntimeUtils.makeMonitor(monitor));
    }

    public void doSave(DBRProgressMonitor monitor)
    {
        applyChanges(monitor, new ResultSetSaveSettings());
    }

    @Override
    public void doSaveAs()
    {
    }

    @Override
    public boolean isDirty()
    {
        return model.isDirty() || (activePresentation != null && activePresentation.isDirty());
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
    public boolean hasData() {
        return model.hasData();
    }

    @Override
    public boolean isHasMoreData() {
        return getExecutionContext() != null && dataReceiver.isHasMoreData();
    }

    @Override
    public boolean isReadOnly()
    {
        if (model.isUpdateInProgress() || !(activePresentation instanceof IResultSetEditor) ||
            (decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_EDIT) == 0)
        {
            return true;
        }
        DBCExecutionContext executionContext = getExecutionContext();
        return
            executionContext == null ||
            !executionContext.isConnected() ||
            !executionContext.getDataSource().getContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_DATA) ||
            executionContext.getDataSource().getInfo().isReadOnlyData();
    }

    @Override
    public String getReadOnlyStatus() {
        if (model.isUpdateInProgress()) {
            return "Update in progress";
        }
        if (!(activePresentation instanceof IResultSetEditor) || (decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_EDIT) == 0) {
            return "Active presentation doesn't support data edit";
        }

        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null || !executionContext.isConnected()) {
            return "No connected to database";
        }
        if (!executionContext.getDataSource().getContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_DATA)) {
            return "Data edit restricted";
        }
        if (executionContext.getDataSource().getInfo().isReadOnlyData()) {
            return "Connection is in read-only state";
        }
        return null;
    }

    /**
     * Checks that current state of result set allows to insert new rows
     * @return true if new rows insert is allowed
     */
    public boolean isInsertable()
    {
        return
            getReadOnlyStatus() == null &&
            model.isSingleSource() &&
            model.getVisibleAttributeCount() > 0;
    }

    public boolean isRefreshInProgress() {
        synchronized (dataPumpJobQueue) {
            return dataPumpRunning.get();
        }
    }

    public void cancelJobs() {
        List<ResultSetJobDataRead> dpjCopy;
        synchronized (dataPumpJobQueue) {
            dpjCopy = new ArrayList<>(this.dataPumpJobQueue);
            this.dataPumpJobQueue.clear();
        }
        for (ResultSetJobDataRead dpj : dpjCopy) {
            if (dpj.isActiveTask()) {
                dpj.cancel();
            }
        }
    }

    ///////////////////////////////////////////////////////
    // Context menu & filters

    @NotNull
    IResultSetFilterManager getFilterManager() {
        return filterManager;
    }

    public void showFiltersMenu() {
        DBDAttributeBinding curAttribute = getActivePresentation().getCurrentAttribute();
        if (curAttribute == null) {
            return;
        }
        MenuManager menuManager = new MenuManager();
        fillFiltersMenu(menuManager, curAttribute, getCurrentRow());
        showContextMenuAtCursor(menuManager);
        viewerPanel.addDisposeListener(e -> menuManager.dispose());
    }

    @Override
    public void showDistinctFilter(DBDAttributeBinding curAttribute) {
        showFiltersDistinctMenu(curAttribute, false);
    }

    public void showFiltersDistinctMenu(DBDAttributeBinding curAttribute, boolean atKeyboardCursor) {
        Collection<ResultSetRow> selectedRows = getSelection().getSelectedRows();
        ResultSetRow[] rows = selectedRows.toArray(new ResultSetRow[0]);

        FilterValueEditPopup popup = new FilterValueEditPopup(getSite().getShell(), ResultSetViewer.this, curAttribute, rows);

        Point location;
        if (atKeyboardCursor) {
            location = getKeyboardCursorLocation();
        } else {
            location = getSite().getWorkbenchWindow().getWorkbench().getDisplay().getCursorLocation();
        }
        if (location != null) {
            popup.setLocation(location);
        }

        if (popup.open() == IDialogConstants.OK_ID) {
            Object value = popup.getValue();

            DBDDataFilter filter = new DBDDataFilter(model.getDataFilter());
            DBDAttributeConstraint constraint = filter.getConstraint(curAttribute);
            if (constraint != null) {
                constraint.setOperator(DBCLogicalOperator.IN);
                constraint.setValue(value);
                setDataFilter(filter, true);
            }
        }
    }

    public void showReferencesMenu(boolean openInNewWindow) {
        MenuManager[] menuManager = new MenuManager[1];
        try {
            UIUtils.runInProgressService(monitor -> {
                menuManager[0] = createRefTablesMenu(monitor, openInNewWindow);
            });
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // Ignore
        }
        if (menuManager[0] != null) {
            showContextMenuAtCursor(menuManager[0]);
            viewerPanel.addDisposeListener(e -> menuManager[0].dispose());
        }
    }

    private void showContextMenuAtCursor(MenuManager menuManager) {
        Point location = getKeyboardCursorLocation();
        if (location != null) {
            final Menu contextMenu = menuManager.createContextMenu(getActivePresentation().getControl());
            contextMenu.setLocation(location);
            contextMenu.setVisible(true);
        }
    }

    @Nullable
    private Point getKeyboardCursorLocation() {
        Control control = getActivePresentation().getControl();
        Point cursorLocation = getActivePresentation().getCursorLocation();
        if (cursorLocation == null) {
            return null;
        }
        return control.getDisplay().map(control, null, cursorLocation);
    }

    ////////////////////////////////////////////////////////////
    // Context menus

    @Override
    public void fillContextMenu(@NotNull IMenuManager manager, @Nullable final DBDAttributeBinding attr, @Nullable final ResultSetRow row) {
        // Custom oldValue items
        final ResultSetValueController valueController;
        if (attr != null && row != null) {
            valueController = new ResultSetValueController(
                this,
                attr,
                row,
                IValueController.EditType.NONE,
                null);
        } else {
            valueController = null;
        }

        {
            {
                // Standard items
                if (attr == null && row != null) {
                    manager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_COPY_ROW_NAMES));
                } else if (attr != null && row == null) {
                    manager.add(ActionUtils.makeCommandContribution(
                        site,
                        ResultSetHandlerMain.CMD_COPY_COLUMN_NAMES,
                        SWT.PUSH,
                        null,
                        null,
                        null,
                        false,
                        Collections.singletonMap("columns", attr.getName())));
                } else {
                    manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_COPY));
                }

                if (row != null) {
                    MenuManager extCopyMenu = new MenuManager(ActionUtils.findCommandName(ResultSetHandlerCopySpecial.CMD_COPY_SPECIAL));
                    extCopyMenu.setRemoveAllWhenShown(true);
                    extCopyMenu.addMenuListener(manager1 -> ResultSetHandlerCopyAs.fillCopyAsMenu(ResultSetViewer.this, manager1));

                    manager.add(extCopyMenu);
                }

                if (row != null) {
                    manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_PASTE));
                    manager.add(ActionUtils.makeCommandContribution(site, IActionConstants.CMD_PASTE_SPECIAL));
                    manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_CUT));
                }

                manager.add(new Separator());

                // Filters and View
                {
                    MenuManager filtersMenu = new MenuManager(
                        ResultSetMessages.controls_resultset_viewer_action_filter,
                        DBeaverIcons.getImageDescriptor(UIIcon.FILTER),
                        MENU_ID_FILTERS); //$NON-NLS-1$
                    filtersMenu.setActionDefinitionId(ResultSetHandlerMain.CMD_FILTER_MENU);
                    filtersMenu.setRemoveAllWhenShown(true);
                    filtersMenu.addMenuListener(manager1 -> fillFiltersMenu(manager1, attr, row));
                    manager.add(filtersMenu);
                }
                {
                    MenuManager orderMenu = new MenuManager(
                        ResultSetMessages.controls_resultset_viewer_action_order,
                        DBeaverIcons.getImageDescriptor(UIIcon.SORT),
                        MENU_ID_ORDER); //$NON-NLS-1$
                    orderMenu.setRemoveAllWhenShown(true);
                    orderMenu.addMenuListener(manager1 -> fillOrderingsMenu(manager1, attr, row));
                    manager.add(orderMenu);
                }
                {
                    MenuManager navigateMenu = new MenuManager(
                        ResultSetMessages.controls_resultset_viewer_action_navigate,
                        null,
                        "navigate"); //$NON-NLS-1$
                    fillNavigateMenu(navigateMenu);
                    manager.add(navigateMenu);
                }

                if (row != null) {
//                    MenuManager editMenu = new MenuManager(
//                        IDEWorkbenchMessages.Workbench_edit,
//                        DBeaverIcons.getImageDescriptor(UIIcon.ROW_EDIT),
//                        MENU_ID_EDIT); //$NON-NLS-1$
                    manager.add(new Separator());
                    fillEditMenu(manager, attr, row, valueController);
                    //manager.add(editMenu);
                }
            }
        }
        manager.add(new GroupMarker(MENU_GROUP_EDIT));

        if (getDataSource() != null && attr != null && model.getVisibleAttributeCount() > 0 && !model.isUpdateInProgress()) {
            MenuManager viewMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_view_format,
                null,
                MENU_ID_VIEW); //$NON-NLS-1$
            viewMenu.setRemoveAllWhenShown(true);
            viewMenu.addMenuListener(manager1 -> fillColumnViewMenu(manager1, attr, row, valueController));
            manager.add(viewMenu);
        }

        {
            MenuManager viewMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_logical_structure,
                null,
                MENU_ID_VIRTUAL_MODEL); //$NON-NLS-1$
            viewMenu.setRemoveAllWhenShown(true);
            viewMenu.addMenuListener(manager1 -> fillVirtualModelMenu(manager1, attr, row, valueController));
            manager.add(viewMenu);
        }

        {
            MenuManager layoutMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_layout,
                null,
                MENU_ID_LAYOUT); //$NON-NLS-1$
            fillLayoutMenu(layoutMenu);
            manager.add(layoutMenu);
        }

        manager.add(new Separator());

        final DBSDataContainer dataContainer = getDataContainer();

        // Fill general menu
        if (row != null && dataContainer != null && model.hasData()) {
            manager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_EXPORT));
            MenuManager openWithMenu = new MenuManager(ActionUtils.findCommandName(ResultSetHandlerOpenWith.CMD_OPEN_WITH));
            openWithMenu.setRemoveAllWhenShown(true);
            openWithMenu.addMenuListener(manager1 -> ResultSetHandlerOpenWith.fillOpenWithMenu(ResultSetViewer.this, manager1));
            manager.add(openWithMenu);
        }

        if (attr != null && row != null) {
            manager.add(new GroupMarker(NavigatorCommands.GROUP_TOOLS));
            manager.add(new GroupMarker(MENU_GROUP_EXPORT));
        }

        manager.add(new Separator(MENU_GROUP_ADDITIONS));

        if (dataContainer != null && model.hasData()) {
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_REFRESH));
        }

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        decorator.fillContributions(manager);
    }

    private void fillColumnViewMenu(IMenuManager viewMenu, @NotNull DBDAttributeBinding attr, @Nullable ResultSetRow row, ResultSetValueController valueController) {
        final DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return;
        }
        List<? extends DBDAttributeTransformerDescriptor> transformers =
            dataSource.getContainer().getPlatform().getValueHandlerRegistry().findTransformers(
                dataSource, attr, null);
        if (!CommonUtils.isEmpty(transformers)) {
            MenuManager transformersMenu = new MenuManager(NLS.bind(ResultSetMessages.controls_resultset_viewer_action_view_column_type, attr.getName()));
            transformersMenu.setRemoveAllWhenShown(true);
            transformersMenu.addMenuListener(manager12 -> fillAttributeTransformersMenu(manager12, attr));
            viewMenu.add(transformersMenu);
        }
        viewMenu.add(new TransformerSettingsAction());
        viewMenu.add(new TransformComplexTypesToggleAction());
        viewMenu.add(new Separator());
        {
            if (valueController != null) {
                viewMenu.add(new SetRowColorAction(this, attr, valueController.getValue()));
                if (getModel().hasColorMapping(attr)) {
                    viewMenu.add(new ResetRowColorAction(this, attr, valueController.getValue()));
                }
            }
            viewMenu.add(new CustomizeColorsAction(this, attr, row));
//            if (getModel().getSingleSource() != null && getModel().hasColorMapping(getModel().getSingleSource())) {
//                viewMenu.add(new ResetAllColorAction());
//            }
        }
        viewMenu.add(new Separator());
        viewMenu.add(new ColorizeDataTypesToggleAction());
        viewMenu.add(new DataFormatsPreferencesAction());
    }

    private void fillVirtualModelMenu(@NotNull IMenuManager vmMenu, @Nullable DBDAttributeBinding attr, @Nullable ResultSetRow row, ResultSetValueController valueController) {
        final DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return;
        }
        List<IAction> possibleActions = new ArrayList<>();
        possibleActions.add(new VirtualAttributeAddAction(this));
        if (attr != null) {
            possibleActions.add(new VirtualAttributeEditAction(this, attr));
            possibleActions.add(new VirtualAttributeDeleteAction(this, attr));
        }

        possibleActions.add(new VirtualForeignKeyEditAction(this));

        possibleActions.add(new VirtualUniqueKeyEditAction(this, true));
        possibleActions.add(new VirtualUniqueKeyEditAction(this, false));

        for (IAction action : possibleActions) {
            if (action.isEnabled()) {
                vmMenu.add(action);
            }
        }

        vmMenu.add(new Separator());

        vmMenu.add(new VirtualEntityEditAction(this));
    }

    private void fillEditMenu(IMenuManager editMenu, @Nullable DBDAttributeBinding attr, @NotNull ResultSetRow row, ResultSetValueController valueController) {
        if (valueController != null) {
            final Object value = valueController.getValue();

            // Edit items
            if (!valueController.isReadOnly()) {
                if (!DBUtils.isNullValue(value) && attr != null && !attr.isRequired()) {
                    editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_CELL_SET_NULL));
                }
                if (valueController.getValueHandler() instanceof DBDValueDefaultGenerator) {
                    String commandName = ActionUtils.findCommandName(ResultSetHandlerMain.CMD_CELL_SET_DEFAULT) +
                        " (" + ((DBDValueDefaultGenerator) valueController.getValueHandler()).getDefaultValueLabel() + ")";
                    DBPImage image = DBValueFormatting.getObjectImage(attr);
                    editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_CELL_SET_DEFAULT, commandName, image));
                }
            }
            if (row.getState() == ResultSetRow.STATE_REMOVED || (row.changes != null && row.changes.containsKey(attr))) {
                editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_CELL_RESET));
            }

            // Menus from value handler
            try {
                valueController.getValueManager().contributeActions(editMenu, valueController, null);
            } catch (Exception e) {
                log.error(e);
            }

            editMenu.add(new Separator());
        }
    }

    private void fillLayoutMenu(IMenuManager layoutMenu) {
        if (activePresentationDescriptor != null && activePresentationDescriptor.supportsRecordMode()) {
            layoutMenu.add(new ToggleModeAction());
        }
        layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_PANELS));
        layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_LAYOUT));
        layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_SWITCH_PRESENTATION));
        {
            MenuManager panelsMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_panels,
                DBeaverIcons.getImageDescriptor(UIIcon.PANEL_CUSTOMIZE),
                "result_panels"); //$NON-NLS-1$
            layoutMenu.add(panelsMenu);
            for (IContributionItem item : fillPanelsMenu()) {
                panelsMenu.add(item);
            }
        }
        layoutMenu.add(new Separator());
        for (ResultSetPresentationDescriptor pd : getAvailablePresentations()) {
            Action psAction = new Action(pd.getLabel(), Action.AS_CHECK_BOX) {
                ResultSetPresentationDescriptor presentation;

                {
                    presentation = pd;
                    setImageDescriptor(DBeaverIcons.getImageDescriptor(presentation.getIcon()));
                }

                @Override
                public boolean isChecked() {
                    return activePresentationDescriptor == presentation;
                }

                @Override
                public void run() {
                    switchPresentation(presentation);
                }
            };
            layoutMenu.add(psAction);
        }
    }

    private void fillNavigateMenu(IMenuManager navigateMenu) {
        boolean hasNavTables = false;
        if (ActionUtils.isCommandEnabled(ResultSetHandlerMain.CMD_NAVIGATE_LINK, site)) {
            // Foreign key to some external table
            navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_NAVIGATE_LINK));
            hasNavTables = true;
        }
        if (model.isSingleSource()) {
            // Add menu for referencing tables
            MenuManager refTablesMenu = createRefTablesMenu(null, false);
            if (refTablesMenu != null) {
                navigateMenu.add(refTablesMenu);
                hasNavTables = true;
            }
        }
        if (hasNavTables) {
            navigateMenu.add(new Separator());
        }

        navigateMenu.add(new Separator());
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FOCUS_FILTER));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ITextEditorActionDefinitionIds.LINE_GOTO));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_FIRST));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_PREVIOUS));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_NEXT));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_LAST));
        navigateMenu.add(new Separator());
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FETCH_PAGE));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FETCH_ALL));
        if (isHasMoreData() && getDataContainer() != null && (getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_COUNT) != 0) {
            navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_COUNT));
        }
        navigateMenu.add(new Separator());
        navigateMenu.add(new ToggleRefreshOnScrollingAction());
        navigateMenu.add(new Separator());
        navigateMenu.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_BACK));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_FORWARD));
    }

    @Nullable
    private MenuManager createRefTablesMenu(@Nullable DBRProgressMonitor monitor, boolean openInNewWindow) {
        DBSEntity singleSource = model.getSingleSource();
        if (singleSource == null) {
            return null;
        }
        String menuName = ActionUtils.findCommandName(ResultSetHandlerMain.CMD_REFERENCES_MENU);

        MenuManager refTablesMenu = new MenuManager(menuName, null, "ref-tables");
        refTablesMenu.setActionDefinitionId(ResultSetHandlerMain.CMD_REFERENCES_MENU);
        refTablesMenu.add(ResultSetReferenceMenu.NOREFS_ACTION);
        if (monitor != null) {
            ResultSetReferenceMenu.fillRefTablesActions(monitor, this, getSelection().getSelectedRows(), singleSource, refTablesMenu, openInNewWindow);
        } else {
            refTablesMenu.addMenuListener(manager ->
                ResultSetReferenceMenu.fillRefTablesActions(null, this, getSelection().getSelectedRows(), singleSource, manager, openInNewWindow));
        }

        return refTablesMenu;
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event) {
        if (event.getObject() instanceof DBVEntity &&
            event.getData() instanceof DBVEntityForeignKey &&
            event.getObject() == model.getVirtualEntity(false))
        {
            // Virtual foreign key change - let's refresh
            refreshData(null);
        }
    }


    private class TransformerAction extends Action {
        private final DBDAttributeBinding attribute;
        TransformerAction(DBDAttributeBinding attr, String text, int style, boolean checked) {
            super(text, style);
            this.attribute = attr;
            setChecked(checked);
        }
        @NotNull
        DBVTransformSettings getTransformSettings() {
            final DBVTransformSettings settings = DBVUtils.getTransformSettings(attribute, true);
            if (settings == null) {
                throw new IllegalStateException("Can't get/create transformer settings for '" + attribute.getFullyQualifiedName(DBPEvaluationContext.UI) + "'");
            }
            return settings;
        }
        void saveTransformerSettings() {
            attribute.getDataSource().getContainer().persistConfiguration();
            refreshData(null);
        }
    }

    private class TransformerSettingsAction extends Action {
        TransformerSettingsAction() {
            super(ResultSetMessages.controls_resultset_viewer_action_view_column_types);
        }

        @Override
        public void run() {
            DBPDataSource dataSource = getDataSource();
            if (dataSource == null) {
                return;
            }
            TransformerSettingsDialog settingsDialog = new TransformerSettingsDialog(ResultSetViewer.this, null, true);
            if (settingsDialog.open() == IDialogConstants.OK_ID) {
                dataSource.getContainer().persistConfiguration();
                refreshData(null);
            }
        }
    }

    private void fillAttributeTransformersMenu(IMenuManager manager, final DBDAttributeBinding attr) {
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return;
        }
        final DBPDataSource dataSource = dataContainer.getDataSource();
        final DBDRegistry registry = DBWorkbench.getPlatform().getValueHandlerRegistry();
        final DBVTransformSettings transformSettings = DBVUtils.getTransformSettings(attr, false);
        DBDAttributeTransformerDescriptor customTransformer = null;
        if (transformSettings != null && transformSettings.getCustomTransformer() != null) {
            customTransformer = registry.getTransformer(transformSettings.getCustomTransformer());
        }
        List<? extends DBDAttributeTransformerDescriptor> customTransformers =
            registry.findTransformers(dataSource, attr, true);
        if (customTransformers != null && !customTransformers.isEmpty()) {
            manager.add(new TransformerAction(
                attr,
                EMPTY_TRANSFORMER_NAME,
                IAction.AS_RADIO_BUTTON,
                transformSettings == null || CommonUtils.isEmpty(transformSettings.getCustomTransformer()))
            {
                @Override
                public void run() {
                    if (isChecked()) {
                        getTransformSettings().setCustomTransformer(null);
                        saveTransformerSettings();
                    }
                }
            });
            for (final DBDAttributeTransformerDescriptor descriptor : customTransformers) {
                final TransformerAction action = new TransformerAction(
                    attr,
                    descriptor.getName(),
                    IAction.AS_RADIO_BUTTON,
                    transformSettings != null && descriptor.getId().equals(transformSettings.getCustomTransformer()))
                {
                    @Override
                    public void run() {
                        try {
                            if (isChecked()) {
                                final DBVTransformSettings settings = getTransformSettings();
                                final String oldCustomTransformer = settings.getCustomTransformer();
                                settings.setCustomTransformer(descriptor.getId());
                                TransformerSettingsDialog settingsDialog = new TransformerSettingsDialog(
                                    ResultSetViewer.this, attr, false);
                                if (settingsDialog.open() == IDialogConstants.OK_ID) {
                                    // If there are no options - save settings without opening dialog
                                    saveTransformerSettings();
                                } else {
                                    settings.setCustomTransformer(oldCustomTransformer);
                                }
                            }
                        } catch (Exception e) {
                            DBWorkbench.getPlatformUI().showError("Transform error", "Error transforming column", e);
                        }
                    }
                };
                manager.add(action);
            }
        }
        if (customTransformer != null && !CommonUtils.isEmpty(customTransformer.getProperties())) {
            manager.add(new TransformerAction(attr, "Settings ...", IAction.AS_UNSPECIFIED, false) {
                @Override
                public void run() {
                    TransformerSettingsDialog settingsDialog = new TransformerSettingsDialog(
                        ResultSetViewer.this, attr, false);
                    if (settingsDialog.open() == IDialogConstants.OK_ID) {
                        saveTransformerSettings();
                    }
                }
            });
        }

        List<? extends DBDAttributeTransformerDescriptor> applicableTransformers =
            registry.findTransformers(dataSource, attr, false);
        if (applicableTransformers != null) {
            manager.add(new Separator());

            for (final DBDAttributeTransformerDescriptor descriptor : applicableTransformers) {
                boolean checked;
                if (transformSettings != null) {
                    if (descriptor.isApplicableByDefault()) {
                        checked = !transformSettings.isExcluded(descriptor.getId());
                    } else {
                        checked = transformSettings.isIncluded(descriptor.getId());
                    }
                } else {
                    checked = descriptor.isApplicableByDefault();
                }
                manager.add(new TransformerAction(attr, descriptor.getName(), IAction.AS_CHECK_BOX, checked) {
                    @Override
                    public void run() {
                        getTransformSettings().enableTransformer(descriptor, !isChecked());
                        saveTransformerSettings();
                    }
                });
            }
        }
    }

    private void fillFiltersMenu(@NotNull IMenuManager filtersMenu, @Nullable DBDAttributeBinding attribute, @Nullable ResultSetRow row)
    {
        if (attribute != null && supportsDataFilter()) {
            if (row != null) {
                filtersMenu.add(new Separator());

                //filtersMenu.add(new FilterByListAction(operator, type, attribute));
                DBCLogicalOperator[] operators = attribute.getValueHandler().getSupportedOperators(attribute);

                // Operators with single input
                for (FilterByAttributeType type : FilterByAttributeType.values()) {
                    if (type == FilterByAttributeType.NONE) {
                        // Value filters are available only if certain cell is selected
                        continue;
                    }
                    MenuManager subMenu = null;
                    //filtersMenu.add(new Separator());
                    if (type.getValue(this, attribute, DBCLogicalOperator.EQUALS, true) == null) {
                        // Null cell value - no operators can be applied
                        continue;
                    }
                    for (DBCLogicalOperator operator : operators) {
                        if (operator.getArgumentCount() > 0) {
                            if (subMenu == null) {
                                subMenu = new MenuManager(type.title, type.icon, type.name());
                            }
                            subMenu.add(new FilterByAttributeAction(this, operator, type, attribute));
                        }
                    }
                    if (subMenu != null) {
                        filtersMenu.add(subMenu);
                    }
                }
                filtersMenu.add(new Separator());

/*
                // Operators with multiple inputs
                for (DBCLogicalOperator operator : operators) {
                    if (operator.getArgumentCount() < 0) {
                        filtersMenu.add(new FilterByAttributeAction(operator, FilterByAttributeType.INPUT, attribute));
                    }
                }
*/

                filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_MENU_DISTINCT));

                // Operators with no inputs
                for (DBCLogicalOperator operator : operators) {
                    if (operator.getArgumentCount() == 0) {
                        filtersMenu.add(new FilterByAttributeAction(this, operator, FilterByAttributeType.NONE, attribute));
                    }
                }
            }

            filtersMenu.add(new Separator());
            DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(attribute);
            if (constraint != null && constraint.hasCondition()) {
                filtersMenu.add(new FilterResetAttributeAction(this, attribute));
            }
        }
        filtersMenu.add(new Separator());
        filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_SAVE_SETTING));
        filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_CLEAR_SETTING));
        filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_EDIT_SETTINGS));
    }

    private void fillOrderingsMenu(@NotNull IMenuManager filtersMenu, @Nullable DBDAttributeBinding attribute, @Nullable ResultSetRow row)
    {
        if (attribute != null) {
            filtersMenu.add(new Separator());
            filtersMenu.add(new OrderByAttributeAction(attribute, true));
            filtersMenu.add(new OrderByAttributeAction(attribute, false));
            filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_ORDER));
            filtersMenu.add(new ToggleServerSideOrderingAction());
        }
    }

    @Override
    public void navigateAssociation(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ResultSetModel bindingsModel,
        @NotNull DBSEntityAssociation association,
        @NotNull List<ResultSetRow> rows,
        boolean newWindow)
        throws DBException
    {
        if (!confirmProceed()) {
            return;
        }
        if (!newWindow && !confirmPanelsReset()) {
            return;
        }

        if (getExecutionContext() == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        DBSEntityConstraint refConstraint = association.getReferencedConstraint();
        if (refConstraint == null) {
            throw new DBException("Broken association (referenced constraint missing)");
        }
        if (!(refConstraint instanceof DBSEntityReferrer)) {
            throw new DBException("Referenced constraint [" + refConstraint + "] is not a referrer");
        }
        DBSEntity targetEntity = refConstraint.getParentObject();
        targetEntity = DBVUtils.getRealEntity(monitor, targetEntity);
        if (!(targetEntity instanceof DBSDataContainer)) {
            throw new DBException("Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] is not a data container");
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<>();

        // Set conditions
        List<? extends DBSEntityAttributeRef> ownAttrs = CommonUtils.safeList(((DBSEntityReferrer) association).getAttributeReferences(monitor));
        List<? extends DBSEntityAttributeRef> refAttrs = CommonUtils.safeList(((DBSEntityReferrer) refConstraint).getAttributeReferences(monitor));
        if (ownAttrs.size() != refAttrs.size()) {
            throw new DBException(
                "Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] association [" + association.getName() +
                    "] columns differs from referenced constraint [" + refConstraint.getName() + "] (" + ownAttrs.size() + "<>" + refAttrs.size() + ")");
        }
        // Add association constraints
        for (int i = 0; i < ownAttrs.size(); i++) {
            DBSEntityAttributeRef ownAttr = ownAttrs.get(i);
            DBSEntityAttributeRef refAttr = refAttrs.get(i);
            DBDAttributeBinding ownBinding = bindingsModel.getAttributeBinding(ownAttr.getAttribute());
            if (ownBinding == null) {
                DBWorkbench.getPlatformUI().showError("Can't navigate", "Attribute " + ownAttr.getAttribute() + " is missing in result set");
                return;
            }

            DBSEntityAttribute attribute = refAttr.getAttribute();
            if (attribute != null) {
                DBDAttributeConstraint constraint = new DBDAttributeConstraint(attribute, DBDAttributeConstraint.NULL_VISUAL_POSITION);
                constraint.setVisible(true);
                constraints.add(constraint);
                createFilterConstraint(rows, ownBinding, constraint);
            }

        }
        // Save cur data filter in state
        if (curState == null) {
            setNewState((DBSDataContainer) targetEntity, model.getDataFilter());
        }
        curState.filter = new DBDDataFilter(bindingsModel.getDataFilter());
        navigateEntity(monitor, newWindow, targetEntity, constraints);
    }

    /**
     * Navigate reference
     * @param bindingsModel       data bindings providing model. Can be a model from another results viewer.
     */
    @Override
    public void navigateReference(@NotNull DBRProgressMonitor monitor, @NotNull ResultSetModel bindingsModel, @NotNull DBSEntityAssociation association, @NotNull List<ResultSetRow> rows, boolean newWindow)
        throws DBException
    {
        if (!confirmProceed()) {
            return;
        }

        if (getExecutionContext() == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }

        DBSEntity targetEntity = association.getParentObject();
        //DBSDataContainer dataContainer = DBUtils.getAdapter(DBSDataContainer.class, targetEntity);
        targetEntity = DBVUtils.getRealEntity(monitor, targetEntity);
        if (!(targetEntity instanceof DBSDataContainer)) {
            throw new DBException("Referencing entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] is not a data container");
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<>();

        // Set conditions
        DBSEntityConstraint refConstraint = association.getReferencedConstraint();
        if (refConstraint == null) {
            throw new DBException("Can't obtain association '" + DBUtils.getQuotedIdentifier(association) + "' target constraint (table " +
                (association.getAssociatedEntity() == null ? "???" : DBUtils.getQuotedIdentifier(association.getAssociatedEntity())) + ")");
        }
        List<? extends DBSEntityAttributeRef> ownAttrs = CommonUtils.safeList(((DBSEntityReferrer) association).getAttributeReferences(monitor));
        List<? extends DBSEntityAttributeRef> refAttrs = CommonUtils.safeList(((DBSEntityReferrer) refConstraint).getAttributeReferences(monitor));
        if (ownAttrs.size() != refAttrs.size()) {
            throw new DBException(
                "Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] association [" + association.getName() +
                    "] columns differ from referenced constraint [" + refConstraint.getName() + "] (" + ownAttrs.size() + "<>" + refAttrs.size() + ")");
        }
        if (ownAttrs.isEmpty()) {
            throw new DBException("Association '" + DBUtils.getQuotedIdentifier(association) + "' has empty column list");
        }
        // Add association constraints
        for (int i = 0; i < refAttrs.size(); i++) {
            DBSEntityAttributeRef refAttr = refAttrs.get(i);

            DBDAttributeBinding attrBinding = bindingsModel.getAttributeBinding(refAttr.getAttribute());
            if (attrBinding == null) {
                log.error("Can't find attribute binding for ref attribute '" + refAttr.getAttribute().getName() + "'");
            } else {
                // Constrain use corresponding own attr
                DBSEntityAttributeRef ownAttr = ownAttrs.get(i);
                DBDAttributeConstraint constraint = new DBDAttributeConstraint(ownAttr.getAttribute(), DBDAttributeConstraint.NULL_VISUAL_POSITION);
                constraint.setVisible(true);
                constraints.add(constraint);

                createFilterConstraint(rows, attrBinding, constraint);

            }
        }
        navigateEntity(monitor, newWindow, targetEntity, constraints);
    }

    private void createFilterConstraint(@NotNull List<ResultSetRow> rows, DBDAttributeBinding attrBinding, DBDAttributeConstraint constraint) {
        if (rows.size() == 1) {
            Object keyValue = model.getCellValue(attrBinding, rows.get(0));
            constraint.setOperator(DBCLogicalOperator.EQUALS);
            constraint.setValue(keyValue);
        } else {
            Object[] keyValues = new Object[rows.size()];
            for (int k = 0; k < rows.size(); k++) {
                keyValues[k] = model.getCellValue(attrBinding, rows.get(k));
            }
            constraint.setOperator(DBCLogicalOperator.IN);
            constraint.setValue(keyValues);
        }
    }

    private void navigateEntity(@NotNull DBRProgressMonitor monitor, boolean newWindow, DBSEntity targetEntity, List<DBDAttributeConstraint> constraints) {
        DBDDataFilter newFilter = new DBDDataFilter(constraints);

        if (newWindow) {
            openResultsInNewWindow(monitor, targetEntity, newFilter);
        } else {
            setDataContainer((DBSDataContainer) targetEntity, newFilter);
        }
    }

    private boolean confirmProceed() {
        return new UIConfirmation() { @Override public Boolean runTask() { return checkForChanges(); } }.confirm();
    }

    private boolean confirmPanelsReset() {
        return new UIConfirmation() {
            @Override public Boolean runTask() {
                boolean panelsDirty = false;
                for (IResultSetPanel panel : getActivePanels()) {
                    if (panel.isDirty()) {
                        panelsDirty = true;
                        break;
                    }
                }
                if (panelsDirty) {
                    int result = ConfirmationDialog.showConfirmDialog(
                        ResourceBundle.getBundle(ResultSetMessages.BUNDLE_NAME),
                        viewerPanel.getShell(),
                        ResultSetPreferences.CONFIRM_RS_PANEL_RESET,
                        ConfirmationDialog.CONFIRM);
                    return result != IDialogConstants.CANCEL_ID;
                }
                return true;
            }
        }.confirm();
    }

    private void openResultsInNewWindow(DBRProgressMonitor monitor, DBSEntity targetEntity, final DBDDataFilter newFilter) {
        if (targetEntity instanceof DBSDataContainer) {
            getContainer().openNewContainer(monitor, (DBSDataContainer) targetEntity, newFilter);
        } else {
            UIUtils.showMessageBox(null, "Open link", "Target entity '" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "' - is not a data container", SWT.ICON_ERROR);
        }
    }

    @Override
    public int getHistoryPosition() {
        return historyPosition;
    }

    @Override
    public int getHistorySize() {
        return stateHistory.size();
    }

    @Override
    public void navigateHistory(int position) {
        if (position < 0 || position >= stateHistory.size()) {
            // out of range
            log.debug("Wrong history position: " + position);
            return;
        }
        HistoryStateItem state = stateHistory.get(position);
        int segmentSize = getSegmentMaxRows();
        if (state.rowNumber >= 0 && state.rowNumber >= segmentSize && segmentSize > 0) {
            segmentSize = (state.rowNumber / segmentSize + 1) * segmentSize;
        }

        runDataPump(state.dataContainer, state.filter, 0, segmentSize, state.rowNumber, true, false, false, null);
    }

    @Override
    public void updatePanelsContent(boolean forceRefresh) {
        updateEditControls();
        IResultSetPanel visiblePanel = getVisiblePanel();
        if (visiblePanel != null) {
            visiblePanel.refresh(forceRefresh);
        }
//        for (IResultSetPanel panel : getActivePanels()) {
//            if (visiblePanel == panel) {
//                panel.refresh(forceRefresh);
//            }
//        }
    }

    @Override
    public void updatePanelActions() {
        IResultSetPanel visiblePanel = getVisiblePanel();
        panelToolBar.removeAll();
        if (visiblePanel != null) {
            visiblePanel.contributeActions(panelToolBar);
        }
        addDefaultPanelActions();
        panelToolBar.update(true);

        if (this.panelFolder != null) {
            ToolBar toolBar = panelToolBar.getControl();
            Point toolBarSize = toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            this.panelFolder.setTabHeight(toolBarSize.y);
        }
    }

    @Override
    public Composite getControl()
    {
        return this.mainPanel;
    }

    public Composite getViewerPanel()
    {
        return this.viewerPanel;
    }

    @NotNull
    @Override
    public IWorkbenchPartSite getSite()
    {
        return site;
    }

    @Override
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
    public IResultSetSelection getSelection()
    {
        if (activePresentation instanceof ISelectionProvider) {
            ISelection selection = ((ISelectionProvider) activePresentation).getSelection();
            if (selection.isEmpty()) {
                return new EmptySelection();
            } else if (selection instanceof IResultSetSelection) {
                return (IResultSetSelection) selection;
            } else {
                log.debug("Bad selection type (" + selection + ") in presentation " + activePresentation);
            }
        }
        return new EmptySelection();
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal)
    {
        if (activePresentation instanceof ISelectionProvider) {
            ((ISelectionProvider) activePresentation).setSelection(selection);
        }
    }

    @NotNull
    @Override
    public ResultSetDataReceiver getDataReceiver() {
        return dataReceiver;
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return container.getExecutionContext();
    }

    @Override
    public boolean checkForChanges() {
        // Check if we are dirty
        if (isDirty()) {
            int checkResult = new UITask<Integer>() {
                @Override
                protected Integer runTask() {
                    return promptToSaveOnClose();
                }
            }.execute();
            switch (checkResult) {
                case ISaveablePart2.CANCEL:
                    return false;
                case ISaveablePart2.YES:
                    // Apply changes
                    saveChanges(null, new ResultSetSaveSettings(), success -> {
                        if (success) {
                            UIUtils.asyncExec(() -> refreshData(null));
                        }
                    });
                    return false;
                default:
                    // Just ignore previous RS values
                    return true;
            }
        }
        return true;
    }

    /**
     * Refresh is called to execute new query/browse new data. It is public API function.
     */
    @Override
    public void refresh()
    {
        if (!checkForChanges()) {
            return;
        }
        // Disable auto-refresh
        autoRefreshControl.enableAutoRefresh(false);

        // Pump data
        DBSDataContainer dataContainer = getDataContainer();

        if (dataContainer != null) {
            DBDDataFilter dataFilter = restoreDataFilter(dataContainer);
            int segmentSize = getSegmentMaxRows();
            Runnable finalizer = () -> {
                if (activePresentation.getControl() != null && !activePresentation.getControl().isDisposed()) {
                    activePresentation.formatData(true);
                }
            };

            dataReceiver.setNextSegmentRead(false);
            // Trick - in fact it is not a refresh but "execute" action
            runDataPump(dataContainer, dataFilter, 0, segmentSize, 0, true, false, false, finalizer);
        } else {
            DBWorkbench.getPlatformUI().showError("Error executing query", "Viewer detached from data source");
        }
    }

    private DBDDataFilter restoreDataFilter(final DBSDataContainer dataContainer) {

        // Restore data filter
        final DataFilterRegistry.SavedDataFilter savedConfig = DataFilterRegistry.getInstance().getSavedConfig(dataContainer);
        if (savedConfig != null) {
            final DBDDataFilter dataFilter = new DBDDataFilter();
            DBRRunnableWithProgress restoreTask = monitor -> {
                try {
                    savedConfig.restoreDataFilter(monitor, dataContainer, dataFilter);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            };
            RuntimeUtils.runTask(restoreTask, "Restore data filter", 60000);
            if (dataFilter.hasFilters()) {
                return dataFilter;
            }
        }
        return null;
    }

    public void refreshWithFilter(DBDDataFilter filter) {
        if (!checkForChanges()) {
            return;
        }

        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null) {
            dataReceiver.setNextSegmentRead(false);
            runDataPump(
                dataContainer,
                filter,
                0,
                getSegmentMaxRows(),
                curRow == null ? -1 : curRow.getRowNumber(),
                true,
                false,
                true,
                null);
        }
    }

    @Override
    public boolean refreshData(@Nullable Runnable onSuccess) {
        if (!checkForChanges()) {
            return false;
        }

        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null) {
            int segmentSize = getSegmentMaxRows();
            if (curRow != null && curRow.getVisualNumber() >= segmentSize && segmentSize > 0) {
                segmentSize = (curRow.getVisualNumber() / segmentSize + 1) * segmentSize;
            }
            dataReceiver.setNextSegmentRead(false);
            return runDataPump(dataContainer, null, 0, segmentSize, curRow == null ? 0 : curRow.getRowNumber(), false, false, true, onSuccess);
        } else {
            return false;
        }
    }

    // Refreshes model metadata (virtual objects + colors and other)
    // It is a bit hacky function because we need to bind custom attributes (usually this happens during data read)
    public boolean refreshMetaData() {
        DBPDataSource dataSource = getDataSource();
        DBSDataContainer dataContainer = getDataContainer();
        if (dataSource == null || dataContainer == null) {
            log.error("Can't refresh metadata on disconnected data viewer");
            return false;
        }

        DBDAttributeBinding[] curAttributes = model.getRealAttributes();
        // Add virtual attributes
        DBDAttributeBinding[] newAttributes = DBUtils.injectAndFilterAttributeBindings(dataSource, dataContainer, curAttributes, false);
        if (newAttributes.length > curAttributes.length) {
            // Bind custom attributes
            try (DBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), dataContainer, "Bind custom attributes")) {
                int rowCount = model.getRowCount();
                List<Object[]> rows = new ArrayList<>(rowCount);
                for (int i = 0; i < rowCount; i++) {
                    rows.add(model.getRowData(i));
                }
                for (DBDAttributeBinding attr : newAttributes) {
                    if (attr instanceof DBDAttributeBindingCustom) {
                        try {
                            attr.lateBinding(session, rows);
                        } catch (DBException e) {
                            log.debug("Error binding virtual attribute '" + attr.getName() + "'", e);
                        }
                    }
                }
            }
        }
        model.updateMetaData(newAttributes);
        model.updateDataFilter();

        redrawData(true, false);
        updatePanelsContent(true);

        return true;
    }

    public void readNextSegment()
    {
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null && !model.isUpdateInProgress()) {
            dataReceiver.setHasMoreData(false);
            dataReceiver.setNextSegmentRead(true);

            runDataPump(
                dataContainer,
                model.getDataFilter(),
                model.getRowCount(),
                getSegmentMaxRows(),
                -1,//curRow == null ? -1 : curRow.getRowNumber(), // Do not reposition cursor after next segment read!
                false,
                true,
                true,
                null);
        }
    }

    @Override
    public void readAllData() {
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        if (ConfirmationDialog.showConfirmDialogEx(
            ResourceBundle.getBundle(ResultSetMessages.BUNDLE_NAME),
            viewerPanel.getShell(),
            ResultSetPreferences.CONFIRM_RS_FETCH_ALL,
            ConfirmationDialog.QUESTION,
            ConfirmationDialog.WARNING) != IDialogConstants.YES_ID)
        {
            return;
        }

        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null && !model.isUpdateInProgress()) {
            dataReceiver.setHasMoreData(false);
            dataReceiver.setNextSegmentRead(true);

            runDataPump(
                dataContainer,
                model.getDataFilter(),
                model.getRowCount(),
                -1,
                curRow == null ? -1 : curRow.getRowNumber(),
                false,
                true,
                true,
                null);
        }
    }

    public void updateRowCount() {
        rowCountLabel.executeAction();
    }

    /**
     * Reads row count and sets value in status label
     */
    private long readRowCount(DBRProgressMonitor monitor) throws DBException {
        final DBCExecutionContext executionContext = getExecutionContext();
        DBSDataContainer dataContainer = getDataContainer();
        if (executionContext == null || dataContainer == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        long[] result = new long[1];
        DBExecUtils.tryExecuteRecover(monitor, executionContext.getDataSource(), param -> {
            try (DBCSession session = executionContext.openSession(
                monitor,
                DBCExecutionPurpose.USER,
                "Read total row count")) {
                long rowCount = dataContainer.countData(
                    new AbstractExecutionSource(dataContainer, executionContext, this),
                    session,
                    model.getDataFilter(),
                    DBSDataContainer.FLAG_NONE);
                model.setTotalRowCount(rowCount);
                result[0] = rowCount;
            } catch (DBCException e) {
                throw new InvocationTargetException(e);
            }
        });
        return result[0];
    }

    private int getSegmentMaxRows()
    {
        if (getDataContainer() == null) {
            return 0;
        }
        return getPreferenceStore().getInt(ModelPreferences.RESULT_SET_MAX_ROWS);
    }

    @NotNull
    public String getActiveQueryText() {
        DBCStatistics statistics = getModel().getStatistics();
        String queryText = statistics == null ? null : statistics.getQueryText();
        if (queryText == null || queryText.isEmpty()) {
            DBSDataContainer dataContainer = getDataContainer();
            if (dataContainer != null) {
                if (dataContainer instanceof SQLQueryContainer) {
                    SQLScriptElement query = ((SQLQueryContainer) dataContainer).getQuery();
                    if (query != null) {
                        return query.getText();
                    }
                }
                return dataContainer.getName();
            }
            queryText = DEFAULT_QUERY_TEXT;
        }
        return queryText;
    }


    private boolean runDataPump(
        @NotNull final DBSDataContainer dataContainer,
        @Nullable final DBDDataFilter dataFilter,
        final int offset,
        final int maxRows,
        final int focusRow,
        final boolean saveHistory, // Save history state (sometimes we don'ty need it)
        final boolean scroll, // Scroll operation
        final boolean refresh, // Refresh. Nothing was changed but refresh from server or scroll happened
        @Nullable final Runnable finalizer)
    {
        if (viewerPanel.isDisposed()) {
            return false;
        }
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null || dataContainer.getDataSource() != executionContext.getDataSource()) {
            // This may happen during cross-database entity navigation
            executionContext = DBUtils.getDefaultContext(dataContainer, false);
        }
        if (executionContext == null) {
            UIUtils.showMessageBox(viewerPanel.getShell(), "Data read", "Can't read data - no active connection", SWT.ICON_WARNING);
            return false;
        }
        // Cancel any refresh jobs
        autoRefreshControl.cancelRefresh();

        // Read data
        final DBDDataFilter useDataFilter = dataFilter != null ? dataFilter :
            (dataContainer == getDataContainer() ? model.getDataFilter() : null);
        Composite progressControl = viewerPanel;
        if (activePresentation.getControl() instanceof Composite) {
            progressControl = (Composite) activePresentation.getControl();
        }

        final Object presentationState = savePresentationState();
        ResultSetJobDataRead dataPumpJob = new ResultSetJobDataRead(
            dataContainer,
            useDataFilter,
            this,
            executionContext,
            progressControl)
        {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                synchronized (dataPumpJobQueue) {
                    if (dataPumpRunning.get()) {
                        log.debug("Internal error: multiple data reads started (" + dataPumpJobQueue + ")");
                        return Status.CANCEL_STATUS;
                    }
                    dataPumpRunning.set(true);
                }
                beforeDataRead();
                try {
                    IStatus status = super.run(monitor);
                    afterDataRead();
                    return status;
                } finally {
                    synchronized (dataPumpJobQueue) {
                        if (!dataPumpRunning.get()) {
                            log.debug("Internal error: data read status is empty");
                        }
                        dataPumpRunning.set(false);
                    }
                }
            }

            private void beforeDataRead() {
                dataReceiver.setFocusRow(focusRow);
                // Set explicit target container
                dataReceiver.setTargetDataContainer(dataContainer);

                model.setUpdateInProgress(true);
                model.setStatistics(null);
                if (filtersPanel != null) {
                    UIUtils.asyncExec(() -> filtersPanel.enableFilters(false));
                }
            }

            private void afterDataRead() {
                final Throwable error = getError();
                if (getStatistics() != null) {
                    model.setStatistics(getStatistics());
                }
                UIUtils.syncExec(() -> {
                    try {
                        final Control control1 = getControl();
                        if (control1.isDisposed()) {
                            return;
                        }
                        model.setUpdateInProgress(false);

                        // update history. Do it first otherwise we are in the incorrect state (getDatacontainer() may return wrong value)
                        if (saveHistory && error == null) {
                            setNewState(dataContainer, useDataFilter);
                        }

                        final boolean metadataChanged = !scroll && model.isMetadataChanged();
                        if (error != null) {
                            String errorMessage = error.getMessage();
                            setStatus(errorMessage, DBPMessageType.ERROR);

                            String sqlText;
                            if (error instanceof DBSQLException) {
                                sqlText = ((DBSQLException) error).getSqlQuery();
                            } else if (dataContainer instanceof SQLQueryContainer) {
                                SQLScriptElement query = ((SQLQueryContainer) dataContainer).getQuery();
                                sqlText = query == null ? getActiveQueryText() : query.getText();
                            } else {
                                sqlText = getActiveQueryText();
                            }

                            if (getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_SHOW_ERRORS_IN_DIALOG)) {
                                DBWorkbench.getPlatformUI().showError("Error executing query", "Query execution failed", error);
                            } else {
                                if (CommonUtils.isEmpty(errorMessage)) {
                                    if (error.getCause() instanceof InterruptedException) {
                                        errorMessage = "Query execution was interrupted";
                                    } else {
                                        errorMessage = "Error executing query";
                                    }
                                }
                                showErrorPresentation(sqlText, errorMessage, error);
                                log.error(errorMessage, error);
                            }
                        } else {
                            if (!metadataChanged) {
                                // Seems to be refresh
                                // Restore original position
                                restorePresentationState(presentationState);
                            }
                            if (focusRow >= 0 && focusRow < model.getRowCount() && model.getVisibleAttributeCount() > 0) {
                                if (getCurrentRow() == null) {
                                    setCurrentRow(getModel().getRow(focusRow));
                                }
                                if (getActivePresentation().getCurrentAttribute() == null) {
                                    getActivePresentation().setCurrentAttribute(model.getVisibleAttribute(0));
                                }
                            }
                        }
                        activePresentation.updateValueView();

                        if (!scroll) {
                            if (dataFilter != null) {
                                boolean visibilityChanged = !model.getDataFilter().equalVisibility(dataFilter);
                                model.updateDataFilter(dataFilter, true);
                                // New data filter may have different columns visibility
                                redrawData(visibilityChanged, false);
                            }
                        }
                        updatePanelsContent(true);
                        if (getStatistics() == null || !getStatistics().isEmpty()) {
                            if (error == null) {
                                // Update status (update execution statistics)
                                updateStatusMessage();
                            }
                            try {
                                fireResultSetLoad();
                            } catch (Throwable e) {
                                log.debug("Error handling resulset load", e);
                            }
                        }
                        updateFiltersText(true);
                        updateToolbar();
                        // auto-refresh
                        autoRefreshControl.scheduleAutoRefresh(error != null);
                    } finally {
                        if (finalizer != null) {
                            try {
                                finalizer.run();
                            } catch (Throwable e) {
                                log.error(e);
                            }
                        }
                    }
                });
            }
        };
        dataPumpJob.setOffset(offset);
        dataPumpJob.setMaxRows(maxRows);
        dataPumpJob.setRefresh(refresh);

        queueDataPump(dataPumpJob);

        return true;
    }

    /**
     * Adds new data read job in queue.
     * In some cases there may be many frequent data read requests (e.g. when user works
     * with references panel). We need to execute only current one and the last one. All
     * intrmediate data read requests must be ignored.
     */
    private void queueDataPump(ResultSetJobDataRead dataPumpJob) {
        synchronized (dataPumpJobQueue) {
            // Clear queue
            dataPumpJobQueue.clear();
            dataPumpJobQueue.add(dataPumpJob);
        }
        new AbstractJob("Initiate data read") {
            {
                setUser(false);
            }
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                if (dataPumpRunning.get()) {
                    // Retry later
                    schedule(50);
                } else {
                    synchronized (dataPumpJobQueue) {
                        if (dataPumpRunning.get()) {
                            schedule(50);
                        } else {
                            if (!dataPumpJobQueue.isEmpty()) {
                                ResultSetJobDataRead curJob = dataPumpJobQueue.get(0);
                                dataPumpJobQueue.remove(curJob);
                                curJob.schedule();
                            }
                        }
                    }
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void clearData()
    {
        this.model.clearData();
        this.curRow = null;
        this.activePresentation.clearMetaData();
    }

    @Override
    public boolean applyChanges(@Nullable DBRProgressMonitor monitor, @NotNull ResultSetSaveSettings settings)
    {
        DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return false;
        }
        if (dataSource.getContainer().getConnectionConfiguration().getConnectionType().isConfirmDataChange()) {
            ResultSetSaveReport saveReport = generateChangesReport();
            if (saveReport == null) {
                return false;
            }
            settings = UITask.run(() -> {
                SavePreviewDialog spd = new SavePreviewDialog(
                    this,
                    saveReport.isHasReferences() && saveReport.getDeletes() > 0,
                    saveReport);
                if (spd.open() == IDialogConstants.OK_ID) {
                    return spd.getSaveSettings();
                } else {
                    return null;
                }
            });
            if (settings == null) {
                return false;
            }
        }

        return saveChanges(monitor, settings, null);
    }

    /**
     * Saves changes to database
     * @param monitor monitor. If null then save will be executed in async job
     * @param listener finish listener (may be null)
     */
    private boolean saveChanges(@Nullable final DBRProgressMonitor monitor, @NotNull ResultSetSaveSettings settings, @Nullable final ResultSetPersister.DataUpdateListener listener)
    {
        UIUtils.syncExec(() -> getActivePresentation().applyChanges());
        try {
            final ResultSetPersister persister = createDataPersister(false);
            final ResultSetPersister.DataUpdateListener applyListener = success -> {
                if (listener != null) {
                    listener.onUpdate(success);
                }
                if (success && getPreferenceStore().getBoolean(ResultSetPreferences.RS_EDIT_REFRESH_AFTER_UPDATE)) {
                    // Refresh updated rows
                    try {
                        persister.refreshInsertedRows();
                    } catch (Throwable e) {
                        log.error("Error refreshing rows after update", e);
                    }
                }
            };

            return persister.applyChanges(monitor, false, settings, applyListener);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Apply changes error", "Error saving changes in database", e);
            return false;
        }
    }

    @Override
    public void rejectChanges()
    {
        if (!isDirty()) {
            return;
        }
        try {
            createDataPersister(true).rejectChanges();
            if (model.getAllRows().isEmpty()) {
                curRow = null;
            }
        } catch (DBException e) {
            log.debug(e);
        }
    }

    @Nullable
    @Override
    public ResultSetSaveReport generateChangesReport() {
        try {
            return createDataPersister(false).generateReport();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Report error", "Error generating changes report", e);
            return null;
        }
    }

    @Override
    public List<DBEPersistAction> generateChangesScript(@NotNull DBRProgressMonitor monitor, @NotNull ResultSetSaveSettings settings) {
        try {
            ResultSetPersister persister = createDataPersister(false);
            persister.applyChanges(monitor, true, settings, null);
            return persister.getScript();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("SQL script generate error", "Error saving changes in database", e);
            return Collections.emptyList();
        }
    }

    @NotNull
    private ResultSetPersister createDataPersister(boolean skipKeySearch)
        throws DBException
    {
//        if (!skipKeySearch && !model.isSingleSource()) {
//            throw new DBException("Can't save data for result set from multiple sources");
//        }
        boolean needPK = false;
        if (!skipKeySearch) {
            for (ResultSetRow row : model.getAllRows()) {
                if (row.getState() == ResultSetRow.STATE_REMOVED || (row.getState() == ResultSetRow.STATE_NORMAL && row.isChanged())) {
                    needPK = true;
                    break;
                }
            }
        }
        ResultSetPersister persister = new ResultSetPersister(this);
        if (needPK) {
            // If we have deleted or updated rows then check for unique identifier
            persister.checkEntityIdentifiers();
        }
        return persister;
    }

    @NotNull
    public ResultSetRow addNewRow(final boolean copyCurrent, boolean afterCurrent, boolean updatePresentation)
    {
        List<ResultSetRow> selectedRows = new ArrayList<>(getSelection().getSelectedRows());
        int rowNum = curRow == null ? 0 : curRow.getVisualNumber();
        int initRowCount = model.getRowCount();
        if (rowNum >= initRowCount) {
            rowNum = initRowCount - 1;
        }
        if (rowNum < 0) {
            rowNum = 0;
        }

        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            throw new IllegalStateException("Can't add/copy rows in disconnected results");
        }

        // Add new row
        // Copy cell values in new context
        try (DBCSession session = executionContext.openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, ResultSetMessages.controls_resultset_viewer_add_new_row_context_name)) {

            final DBDAttributeBinding docAttribute = model.getDocumentAttribute();
            final DBDAttributeBinding[] attributes = model.getAttributes();

            int rowsToCopy[];
            if (selectedRows.size() > 1) {
                rowsToCopy = new int[selectedRows.size()];
                for (int i = 0; i < selectedRows.size(); i++) {
                    rowsToCopy[i] = selectedRows.get(i).getVisualNumber();
                }
                rowNum = rowsToCopy[0];
            } else {
                rowsToCopy = new int[]{rowNum};
            }
            int newRowIndex = afterCurrent ? rowNum + rowsToCopy.length : rowNum;
            if (newRowIndex > initRowCount) {
                newRowIndex = initRowCount; // May happen if we insert "after" current row and there are no rows at all
            }
            for (int rowIndex = rowsToCopy.length - 1, rowCount = 0; rowIndex >= 0; rowIndex--, rowCount++) {
                int currentRowNumber = rowsToCopy[rowIndex];
                if (!afterCurrent) {
                    currentRowNumber += rowCount;
                }
                final Object[] cells;

                if (docAttribute != null) {
                    cells = new Object[1];
                    if (copyCurrent && currentRowNumber >= 0 && currentRowNumber < model.getRowCount()) {
                        Object[] origRow = model.getRowData(currentRowNumber);
                        try {
                            cells[0] = docAttribute.getValueHandler().getValueFromObject(session, docAttribute, origRow[0], true, false);
                        } catch (DBCException e) {
                            log.warn(e);
                        }
                    }
                    if (cells[0] == null) {
                        try {
                            cells[0] = DBUtils.makeNullValue(session, docAttribute.getValueHandler(), docAttribute.getAttribute());
                        } catch (DBCException e) {
                            log.warn(e);
                        }
                    }
                } else {
                    cells = new Object[attributes.length];
                    if (copyCurrent && currentRowNumber >= 0 && currentRowNumber < model.getRowCount()) {
                        Object[] origRow = model.getRowData(currentRowNumber);
                        for (int i = 0; i < attributes.length; i++) {
                            DBDAttributeBinding metaAttr = attributes[i];
                            if (metaAttr.isPseudoAttribute() || metaAttr.isAutoGenerated()) {
                                // set pseudo and autoincrement attributes to null
                                cells[i] = null;
                            } else {
                                DBSAttributeBase attribute = metaAttr.getAttribute();
                                try {
                                    cells[i] = metaAttr.getValueHandler().getValueFromObject(session, attribute, origRow[i], true, false);
                                } catch (DBCException e) {
                                    log.warn(e);
                                    try {
                                        cells[i] = DBUtils.makeNullValue(session, metaAttr.getValueHandler(), attribute);
                                    } catch (DBCException e1) {
                                        log.warn(e1);
                                    }
                                }
                            }
                        }
                    } else {
                        // Initialize new values
                        for (int i = 0; i < attributes.length; i++) {
                            DBDAttributeBinding metaAttr = attributes[i];
                            try {
                                cells[i] = DBUtils.makeNullValue(session, metaAttr.getValueHandler(), metaAttr.getAttribute());
                            } catch (DBCException e) {
                                log.warn(e);
                            }
                        }
                    }
                }
                curRow = model.addNewRow(newRowIndex, cells);
            }
        }
        if (updatePresentation) {
            redrawData(false, true);
            updateEditControls();
            fireResultSetChange();
        }

        return curRow;
    }

    public void deleteSelectedRows()
    {
        Set<ResultSetRow> rowsToDelete = new LinkedHashSet<>();
        if (recordMode) {
            rowsToDelete.add(curRow);
        } else {
            IResultSetSelection selection = getSelection();
            if (!selection.isEmpty()) {
                rowsToDelete.addAll(selection.getSelectedRows());
            }
        }
        if (rowsToDelete.isEmpty()) {
            return;
        }

        int rowsRemoved = 0;
        int lastRowNum = -1;
        for (ResultSetRow row : rowsToDelete) {
            if (model.deleteRow(row)) {
                rowsRemoved++;
            }
            lastRowNum = row.getVisualNumber();
        }
        redrawData(false, rowsRemoved > 0);
        // Move one row down (if we are in grid mode)
        if (!recordMode && lastRowNum < model.getRowCount() - 1 && rowsRemoved == 0) {
            activePresentation.scrollToRow(IResultSetPresentation.RowPosition.NEXT);
        } else {
            activePresentation.scrollToRow(IResultSetPresentation.RowPosition.CURRENT);
        }

        updateEditControls();
        fireResultSetChange();
    }

    //////////////////////////////////
    // Virtual identifier management

    @Nullable
    public DBDRowIdentifier getVirtualEntityIdentifier()
    {
        if (model.getVisibleAttributeCount() == 0) {
            return null;
        }
        DBDRowIdentifier rowIdentifier = model.getVisibleAttribute(0).getRowIdentifier();
        DBSEntityConstraint identifier = rowIdentifier == null ? null : rowIdentifier.getUniqueKey();
        if (identifier instanceof DBVEntityConstraint) {
            return rowIdentifier;
        } else {
            return null;
        }
    }

    public boolean editEntityIdentifier() {
        EditVirtualEntityDialog dialog = new EditVirtualEntityDialog(
            ResultSetViewer.this, model.getSingleSource(), model.getVirtualEntity(true));
        dialog.setInitPage(EditVirtualEntityDialog.InitPage.UNIQUE_KEY);
        if (dialog.open() == IDialogConstants.OK_ID) {
            DBDRowIdentifier virtualID = getVirtualEntityIdentifier();
            if (virtualID != null) {
                try {
                    virtualID.reloadAttributes(new VoidProgressMonitor(), getModel().getAttributes());
                } catch (DBException e) {
                    log.error(e);
                }
            }
            persistConfig();
            return true;
        }
        return false;
    }

    public void clearEntityIdentifier()
    {
        DBVEntity vEntity = model.getVirtualEntity(false);
        if (vEntity != null) {
            DBVEntityConstraint vConstraint = vEntity.getBestIdentifier();
            if (vConstraint != null) {
                vConstraint.setAttributes(Collections.emptyList());
            }

            DBDAttributeBinding firstAttribute = model.getVisibleAttribute(0);
            DBDRowIdentifier rowIdentifier = firstAttribute.getRowIdentifier();
            if (rowIdentifier != null && rowIdentifier.getUniqueKey() == vConstraint) {
                rowIdentifier.clearAttributes();
            }

            persistConfig();
        }
    }

    @NotNull
    private IResultSetListener[] getListenersCopy() {
        IResultSetListener[] listenersCopy;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return EMPTY_LISTENERS;
            }
            listenersCopy = listeners.toArray(new IResultSetListener[0]);
        }
        return listenersCopy;
    }

    void fireResultSetChange() {
        for (IResultSetListener listener : getListenersCopy()) {
            listener.handleResultSetChange();
        }
    }

    private void fireResultSetLoad() {
        for (IResultSetListener listener : getListenersCopy()) {
            listener.handleResultSetLoad();
        }
    }

    private void fireResultSetSelectionChange(SelectionChangedEvent event) {
        for (IResultSetListener listener : getListenersCopy()) {
            listener.handleResultSetSelectionChange(event);
        }
    }

    private static class SimpleFilterManager implements IResultSetFilterManager {
        private final Map<String, List<String>> filterHistory = new HashMap<>();
        @NotNull
        @Override
        public List<String> getQueryFilterHistory(@NotNull String query) {
            final List<String> filters = filterHistory.get(query);
            if (filters != null) {
                return filters;
            }
            return Collections.emptyList();
        }

        @Override
        public void saveQueryFilterValue(@NotNull String query, @NotNull String filterValue) {
            List<String> filters = filterHistory.get(query);
            if (filters == null) {
                filters = new ArrayList<>();
                filterHistory.put(query, filters);
            }
            filters.add(filterValue);
        }

        @Override
        public void deleteQueryFilterValue(@NotNull String query, String filterValue) throws DBException {
            List<String> filters = filterHistory.get(query);
            if (filters != null) {
                filters.add(filterValue);
            }
        }
    }

    private class EmptySelection extends StructuredSelection implements IResultSetSelection {
        @NotNull
        @Override
        public IResultSetController getController() {
            return ResultSetViewer.this;
        }

        @NotNull
        @Override
        public List<DBDAttributeBinding> getSelectedAttributes() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public List<ResultSetRow> getSelectedRows() {
            return Collections.emptyList();
        }

        @Override
        public DBDAttributeBinding getElementAttribute(Object element) {
            return null;
        }

        @Override
        public ResultSetRow getElementRow(Object element) {
            return null;
        }
    }

    public static class PanelsMenuContributor extends CompoundContributionItem
    {
        @Override
        protected IContributionItem[] getContributionItems() {
            final ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet(
                UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart());
            if (rsv == null) {
                return new IContributionItem[0];
            }
            List<IContributionItem> items = rsv.fillPanelsMenu();
            return items.toArray(new IContributionItem[items.size()]);
        }
    }

    private class ConfigAction extends Action {
        ConfigAction()
        {
            super(ResultSetMessages.controls_resultset_viewer_action_options);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
        }

        @Override
        public void runWithEvent(Event event)
        {
            UIUtils.showPreferencesFor(
                getControl().getShell(),
                ResultSetViewer.this,
                PrefPageResultSetMain.PAGE_ID);
        }

    }

    private class DataFormatsPreferencesAction extends Action {
        public DataFormatsPreferencesAction() {
            super(ResultSetMessages.controls_resultset_viewer_action_data_formats);
        }

        @Override
        public void run() {
            UIUtils.showPreferencesFor(
                getControl().getShell(),
                ResultSetViewer.this,
                PrefPageDataFormat.PAGE_ID);
        }
    }

    private abstract class ToggleConnectionPreferenceAction extends Action {
        private final String prefId;
        ToggleConnectionPreferenceAction(String prefId, String title) {
            super(title);
            this.prefId = prefId;
        }

        @Override
        public int getStyle()
        {
            return AS_CHECK_BOX;
        }

        @Override
        public boolean isChecked()
        {
            return getPreferenceStore().getBoolean(prefId);
        }

        @Override
        public void run()
        {
            DBPPreferenceStore preferenceStore = getPreferenceStore();
            preferenceStore.setValue(
                prefId,
                !preferenceStore.getBoolean(prefId));
        }
    }

    private class ToggleServerSideOrderingAction extends ToggleConnectionPreferenceAction {
        ToggleServerSideOrderingAction() {
            super(ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE, ResultSetMessages.pref_page_database_resultsets_label_server_side_order);
        }
    }

    private class ToggleRefreshOnScrollingAction extends ToggleConnectionPreferenceAction {
        ToggleRefreshOnScrollingAction() {
            super(ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING, ResultSetMessages.pref_page_database_resultsets_label_reread_on_scrolling);
        }
    }

    String translateFilterPattern(DBCLogicalOperator operator, FilterByAttributeType type, DBDAttributeBinding attribute)
    {
        Object value = type.getValue(this, attribute, operator, true);

        DBCExecutionContext executionContext = getExecutionContext();
        String strValue = executionContext == null ? String.valueOf(value) : attribute.getValueHandler().getValueDisplayString(attribute, value, DBDDisplayFormat.UI);
        strValue = strValue.replaceAll("\\s+", " ").replace("@", "^").trim();
        strValue = UITextUtils.getShortText(sizingGC, strValue, 150);
        if (operator.getArgumentCount() == 0) {
            return operator.getStringValue();
        } else {
            if (!CUSTOM_FILTER_VALUE_STRING.equals(strValue)) {
                strValue = "'" + strValue + "'";
            }
            return operator.getStringValue() + " " + strValue;
        }
    }


    private class OrderByAttributeAction extends Action {
        private final DBDAttributeBinding attribute;
        private final boolean ascending;

        OrderByAttributeAction(DBDAttributeBinding attribute, boolean ascending) {
            super("Order by " + attribute.getName() + " " + (ascending ? "ASC" : "DESC"), AS_CHECK_BOX);
            this.attribute = attribute;
            this.ascending = ascending;
        }

        @Override
        public boolean isChecked() {
            DBDDataFilter dataFilter = getModel().getDataFilter();
            DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
            return constraint != null && constraint.getOrderPosition() > 0 && constraint.isOrderDescending() != ascending;
        }

        @Override
        public void run() {
            toggleSortOrder(attribute, ascending, !ascending);
        }
    }

    private class TransformComplexTypesToggleAction extends Action {
        TransformComplexTypesToggleAction()
        {
            super(ResultSetMessages.actions_name_structurize_complex_types, AS_CHECK_BOX);
            setToolTipText("Visualize complex types (arrays, structures, maps) in results grid as separate columns");
        }

        @Override
        public boolean isChecked() {
            DBPDataSource dataSource = getDataContainer().getDataSource();
            return dataSource != null &&
                dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES);
        }

        @Override
        public void run()
        {
            DBPDataSource dataSource = getDataContainer().getDataSource();
            if (dataSource == null) {
                return;
            }
            DBPPreferenceStore preferenceStore = dataSource.getContainer().getPreferenceStore();
            boolean curValue = preferenceStore.getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES);
            preferenceStore.setValue(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES, !curValue);
            dataSource.getContainer().persistConfiguration();
            refreshData(null);
        }

    }

    private class ColorizeDataTypesToggleAction extends Action {
        ColorizeDataTypesToggleAction()
        {
            super(ResultSetMessages.actions_name_colorize_data_types, AS_CHECK_BOX);
            setToolTipText("Set different foreground color for data types");
        }

        @Override
        public boolean isChecked() {
            DBPDataSource dataSource = getDataContainer().getDataSource();
            return dataSource != null &&
                dataSource.getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
        }

        @Override
        public void run()
        {
            DBPDataSource dataSource = getDataContainer().getDataSource();
            if (dataSource == null) {
                return;
            }
            DBPPreferenceStore dsStore = dataSource.getContainer().getPreferenceStore();
            boolean curValue = dsStore.getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
            // Set local setting to default
            dsStore.setValue(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES, !curValue);
            dataSource.getContainer().persistConfiguration();
            refreshData(null);
        }

    }

    private class ToggleModeAction extends Action {
        {
            setActionDefinitionId(ResultSetHandlerMain.CMD_TOGGLE_MODE);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.RS_DETAILS));
        }

        ToggleModeAction() {
            super(ResultSetMessages.dialog_text_check_box_record, Action.AS_CHECK_BOX);
            String toolTip = ActionUtils.findCommandDescription(ResultSetHandlerMain.CMD_TOGGLE_MODE, getSite(), false);
            if (!CommonUtils.isEmpty(toolTip)) {
                setToolTipText(toolTip);
            }
        }

        @Override
        public boolean isChecked() {
            return isRecordMode();
        }

        @Override
        public void run() {
            toggleMode();
        }
    }

    class HistoryStateItem {
        DBSDataContainer dataContainer;
        DBDDataFilter filter;
        int rowNumber;

        HistoryStateItem(DBSDataContainer dataContainer, @Nullable DBDDataFilter filter, int rowNumber) {
            this.dataContainer = dataContainer;
            this.filter = filter;
            this.rowNumber = rowNumber;
        }

        String describeState() {
            DBCExecutionContext context = getExecutionContext();
            String desc = dataContainer.getName();
            if (context != null && filter != null && filter.hasConditions()) {
                StringBuilder condBuffer = new StringBuilder();
                SQLUtils.appendConditionString(filter, context.getDataSource(), null, condBuffer, true);
                desc += " [" + condBuffer + "]";
            }
            return desc;
        }
    }

    static class PresentationSettings {
        PresentationSettings() {
        }

        final Set<String> enabledPanelIds = new LinkedHashSet<>();
        String activePanelId;
        int panelRatio;
        boolean panelsVisible;
        boolean verticalLayout;
    }

}
