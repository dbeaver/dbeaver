/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.registry.BasePolicyDataProvider;
import org.jkiss.dbeaver.registry.data.hints.ValueHintProviderDescriptor;
import org.jkiss.dbeaver.registry.data.hints.ValueHintRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.TabFolderReorder;
import org.jkiss.dbeaver.ui.controls.ToolbarSeparatorContribution;
import org.jkiss.dbeaver.ui.controls.VerticalButton;
import org.jkiss.dbeaver.ui.controls.VerticalFolder;
import org.jkiss.dbeaver.ui.controls.autorefresh.AutoRefreshControl;
import org.jkiss.dbeaver.ui.controls.resultset.actions.*;
import org.jkiss.dbeaver.ui.controls.resultset.colors.CustomizeColorsAction;
import org.jkiss.dbeaver.ui.controls.resultset.colors.ResetAllColorAction;
import org.jkiss.dbeaver.ui.controls.resultset.colors.ResetRowColorAction;
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
import org.jkiss.dbeaver.ui.editors.data.preferences.PrefPageResultSetMain;
import org.jkiss.dbeaver.ui.navigator.NavigatorCommands;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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
    implements DBPContextProvider, IResultSetController, ISaveablePart2, DBPAdaptable, DBPEventListener
{
    private static final Log log = Log.getLog(ResultSetViewer.class);

    private static final String TOOLBAR_GROUP_NAVIGATION = "navigation";
    private static final String TOOLBAR_GROUP_PRESENTATIONS = "presentations";
    private static final String TOOLBAR_GROUP_ADDITIONS = IWorkbenchActionConstants.MB_ADDITIONS;

    private static final String SETTINGS_SECTION_PRESENTATIONS = "presentations";

    private static final String TOOLBAR_EDIT_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.controls.resultset.status.editCmds";
    private static final String TOOLBAR_NAVIGATION_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.controls.resultset.status.navCmds";
    private static final String TOOLBAR_EXPORT_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.controls.resultset.status.exportCmd";
    private static final String TOOLBAR_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.controls.resultset.status";
    private static final String TOOLBAR_CONFIGURATION_VISIBLE_PROPERTY = "org.jkiss.dbeaver.ui.toolbar.configuration.visible";

    private static final String CONFIRM_SERVER_SIDE_ORDERING_UNAVAILABLE = "org.jkiss.dbeaver.sql.resultset.serverSideOrderingUnavailable";

    private static final int THEME_UPDATE_DELAY_MS = 250;

    public static final String EMPTY_TRANSFORMER_NAME = "Default";
    public static final String CONTROL_ID = ResultSetViewer.class.getSimpleName();
    public static final String DEFAULT_QUERY_TEXT = "SQL";
    public static final String CUSTOM_FILTER_VALUE_STRING = "..";

    private static final DecimalFormat ROW_COUNT_FORMAT = new DecimalFormat("###,###,###,###,###,##0");
    private static final DateTimeFormatter EXECUTION_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss");

    private static final IResultSetListener[] EMPTY_LISTENERS = new IResultSetListener[0];
    private static final String CSS_CLASS_RESULT_SET_VIEWER = "ResultSetViewer";

    private IResultSetFilterManager filterManager;
    @NotNull
    private final IWorkbenchPartSite site;
    private final Composite mainPanel;
    private final Composite viewerPanel;
    private final IResultSetDecorator decorator;
    @Nullable
    private ResultSetFilterPanel filtersPanel;
    private final SashForm viewerSash;

    private final VerticalFolder panelSwitchFolder;
    private CTabFolder panelFolder;
    private ToolBarManager panelToolBar;

    private final VerticalFolder presentationSwitchFolder;
    private final Composite presentationPanel;

    private final List<ToolBarManager> toolbarList = new ArrayList<>();
    private Composite statusBar;
    private StatusLabel statusLabel;
    private ActiveStatusMessage rowCountLabel;
    private Text selectionStatLabel;
    private Text resultSetSize;

    private final DynamicFindReplaceTarget findReplaceTarget;

    // Presentation
    private IResultSetPresentation activePresentation;
    private ResultSetPresentationDescriptor activePresentationDescriptor;
    private List<ResultSetPresentationDescriptor> availablePresentations;
    private final List<ResultSetPanelDescriptor> availablePanels = new ArrayList<>();

    private final Map<ResultSetPresentationDescriptor, PresentationSettings> presentationSettings = new HashMap<>();
    private final Map<String, IResultSetPanel> activePanels = new HashMap<>();
    //private final Map<String, ToolBarManager> activeToolBars = new HashMap<>();

    @NotNull
    private final IResultSetContainer container;

    @NotNull
    private final ResultSetDataReceiver dataReceiver;

    @NotNull
    private final DBPPreferenceListener dataPropertyListener;

    // Current row/col number
    @Nullable
    private ResultSetRow curRow;
    // Mode
    private boolean recordMode;
    private int[] selectedRecords = new int[0];

    private Integer segmentFetchSize;

    private final List<IResultSetListener> listeners = new ArrayList<>();

    private final List<ResultSetJobAbstract> dataPumpJobQueue = new ArrayList<>();
    private final AtomicBoolean dataPumpRunning = new AtomicBoolean();

    private final ResultSetModel model = new ResultSetModel();
    private HistoryStateItem curState = null;
    private final List<HistoryStateItem> stateHistory = new ArrayList<>();
    private int historyPosition = -1;

    private final AutoRefreshControl autoRefreshControl;
    private boolean actionsDisabled;
    private volatile boolean isUIUpdateRunning;

    private final boolean isDarkHighContrast;
    private final Color defaultBackground;
    private final Color defaultForeground;
    private VerticalButton recordModeButton;

    // Theme listener
    private IPropertyChangeListener themeChangeListener;
    private final AbstractJob themeUpdateJob;
    private long lastThemeUpdateTime;

    private volatile boolean nextSegmentReadingBlocked;

    public ResultSetViewer(@NotNull Composite parent, @NotNull IWorkbenchPartSite site, @NotNull IResultSetContainer container) {
        super();
        this.site = site;
        this.recordMode = false;
        this.container = container;
        this.decorator = container.createResultSetDecorator();
        this.dataReceiver = new ResultSetDataReceiver(this);
        this.dataPropertyListener = event -> {
            DBPDataSourceContainer dataSourceContainer = null;
            if (event.getSource() instanceof DBPDataSourceContainerProvider) {
                dataSourceContainer = ((DBPDataSourceContainerProvider) event.getSource()).getDataSourceContainer();
            }
            handleDataPropertyChange(dataSourceContainer, event.getProperty(), event.getOldValue(), event.getNewValue());
        };

        this.filterManager = GeneralUtils.adapt(this, IResultSetFilterManager.class);
        if (this.filterManager == null) {
            this.filterManager = new SimpleFilterManager();
        }

        loadPresentationSettings();
        isDarkHighContrast = UIStyles.isDarkHighContrastTheme();
        this.defaultBackground = isDarkHighContrast ? UIStyles.getDefaultWidgetBackground() : UIStyles.getDefaultTextBackground();
        this.defaultForeground = isDarkHighContrast ? UIUtils.COLOR_WHITE : UIStyles.getDefaultTextForeground();

        long decoratorFeatures = decorator.getDecoratorFeatures();

        boolean supportsPanels = (decoratorFeatures & IResultSetDecorator.FEATURE_PANELS) != 0;

        this.mainPanel = UIUtils.createPlaceholder(parent, supportsPanels ? 3 : 2);
        CSSUtils.setCSSClass(this.mainPanel, CSS_CLASS_RESULT_SET_VIEWER);

        this.autoRefreshControl = new AutoRefreshControl(
            this.mainPanel, ResultSetViewer.class.getSimpleName(), monitor -> refreshData(null));
        this.autoRefreshControl.setHintSupplier(() -> {
            DBCExecutionContext executionContext = getExecutionContext();
            if (executionContext != null) {
                DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
                if (txnManager != null) {
                    try {
                        if (txnManager.isAutoCommit()) {
                            return ResultSetMessages.controls_resultset_viewer_frequent_refresh_hint;
                        } else {
                            return ResultSetMessages.controls_resultset_viewer_switch_autocommit_hint;
                        }
                    } catch (DBCException e) {
                        log.debug(e);
                    }
                }
            }
            return null;
        });

        if ((decoratorFeatures & IResultSetDecorator.FEATURE_FILTERS) != 0) {
            this.filtersPanel = new ResultSetFilterPanel(this, this.mainPanel,
                (decoratorFeatures & IResultSetDecorator.FEATURE_COMPACT_FILTERS) != 0);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = ((GridLayout)mainPanel.getLayout()).numColumns;
            this.filtersPanel.setLayoutData(gd);
        }

        if ((decoratorFeatures & IResultSetDecorator.FEATURE_PRESENTATIONS) != 0) {
            this.presentationSwitchFolder = new VerticalFolder(mainPanel, SWT.LEFT);
            this.presentationSwitchFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));
            CSSUtils.setCSSClass(this.presentationSwitchFolder, DBStyles.COLORED_BY_CONNECTION_TYPE);
        } else {
            this.presentationSwitchFolder = null;
        }

        this.viewerPanel = UIUtils.createPlaceholder(mainPanel, 1);
        this.viewerPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.viewerPanel.setData(CONTROL_ID, this);
        UIUtils.setHelp(this.viewerPanel, IHelpContextIds.CTX_RESULT_SET_VIEWER);
        this.viewerPanel.setRedraw(false);
        CSSUtils.setCSSClass(this.viewerPanel, CSS_CLASS_RESULT_SET_VIEWER);

        if (supportsPanels) {
            this.panelSwitchFolder = new VerticalFolder(mainPanel, SWT.RIGHT);
            this.panelSwitchFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));
            CSSUtils.setCSSClass(this.panelSwitchFolder, DBStyles.COLORED_BY_CONNECTION_TYPE);
        } else {
            panelSwitchFolder = null;
        }

        try {
            this.findReplaceTarget = new DynamicFindReplaceTarget();

            this.viewerSash = new SashForm(this.viewerPanel, SWT.HORIZONTAL | SWT.SMOOTH);
            this.viewerSash.setSashWidth(5);
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
                Composite trControl = new Composite(panelFolder, SWT.NONE);
                trControl.setLayout(new FillLayout());
                ToolBar panelToolbarControl = this.panelToolBar.createControl(trControl);
                this.panelFolder.setTopRight(trControl, SWT.RIGHT | SWT.WRAP);
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
                        if (weights.length == 2) {
                            getPresentationSettings().panelRatio = weights[1];
                        }
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

        addListener(new ResultSetStatListener(this));

        // Listen datasource events (like connect/disconnect/update)
        DBPProject project = container.getProject();
        if (project != null) {
            project.getDataSourceRegistry().addDataSourceListener(this);
        }

        themeUpdateJob = new AbstractJob("Update theme") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                if (!getControl().isDisposed()) {
                    UIUtils.syncExec(() -> {
                        if (!getControl().isDisposed()) {
                            applyCurrentPresentationThemeSettings();
                        }
                    });
                    lastThemeUpdateTime = System.currentTimeMillis();
                }
                return Status.OK_STATUS;
            }
        };
        themeUpdateJob.setSystem(true);
        themeUpdateJob.setUser(false);
        scheduleThemeUpdate();

        themeChangeListener = e -> scheduleThemeUpdate();
        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeChangeListener);

        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(dataPropertyListener);
        DBWorkbench.getPlatform().getDataSourceProviderRegistry().getGlobalDataSourcePreferenceStore().addPropertyChangeListener(dataPropertyListener);
    }

    private void applyCurrentPresentationThemeSettings() {
        if (panelFolder != null) {
            panelFolder.setFont(JFaceResources.getFont(UIFonts.DBEAVER_FONTS_MAIN_FONT));
        }

        if (statusBar != null) {
            UIUtils.applyMainFont(statusBar);
            updateToolbar();
        }

        UIUtils.applyMainFont(presentationSwitchFolder);
        UIUtils.applyMainFont(panelSwitchFolder);

        if (activePresentation instanceof AbstractPresentation) {
            ITheme currentTheme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
            ((AbstractPresentation) activePresentation).applyThemeSettings(currentTheme);
        }
    }

    private void scheduleThemeUpdate() {
        final long currentTime = System.currentTimeMillis();
        final long elapsedTime = currentTime - lastThemeUpdateTime;

        if (!themeUpdateJob.isCanceled()) {
            themeUpdateJob.cancel();
        }

        if (lastThemeUpdateTime > 0 && elapsedTime < THEME_UPDATE_DELAY_MS) {
            themeUpdateJob.schedule(THEME_UPDATE_DELAY_MS - elapsedTime);
        } else {
            themeUpdateJob.schedule();
        }
    }

    private void handleDataPropertyChange(
        @Nullable DBPDataSourceContainer dataSource,
        @NotNull String property,
        @Nullable Object oldValue,
        @Nullable Object newValue
    ) {
        if (ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES.equals(property)) {
            scheduleThemeUpdate();
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
        return dataContainer != null && dataContainer.isFeatureSupported(DBSDataContainer.FEATURE_DATA_FILTER);
    }

    public boolean supportsNavigation() {
        return activePresentationDescriptor != null && activePresentationDescriptor.supportsNavigation();
    }

    public boolean supportsEdit() {
        return activePresentationDescriptor != null && activePresentationDescriptor.supportsEdit();
    }

    public void resetDataFilter(boolean refresh) {
        setDataFilter(model.createDataFilter(), refresh);
    }

    /**
     * Creates a new data filter, keeping all visual state (visibility, etc.) from a previous one.
     */
    public void clearDataFilter(boolean refresh) {
        DBDDataFilter newFilter = model.createDataFilter();
        DBDDataFilter curFilter = model.getDataFilter();

        Map<String, Map<String, Object>> states = new HashMap<>();

        for (DBDAttributeConstraint constraint : curFilter.getConstraints()) {
            Map<String, Object> state = saveConstraintVisualState(constraint);
            if (!state.isEmpty()) {
                states.put(constraint.getFullAttributeName(), state);
            }
        }

        for (DBDAttributeConstraint constraint : newFilter.getConstraints()) {
            Map<String, Object> state = states.get(constraint.getFullAttributeName());
            if (state != null) {
                restoreConstraintVisualState(constraint, state);
            }
        }

        setDataFilter(newFilter, refresh);
    }

    private Map<String, Object> saveConstraintVisualState(DBDAttributeConstraint constraint) {
        Map<String, Object> state = new Hashtable<>();
        state.put("visible", constraint.isVisible());
        if (!ArrayUtils.isEmpty(constraint.getOptions())) {
            state.put("options", constraint.getOptions());
        }
        return state;
    }

    private void restoreConstraintVisualState(DBDAttributeConstraint constraint, Map<String, Object> state) {
        constraint.setVisible((boolean) state.get("visible"));
        if (state.containsKey("options")) {
            constraint.setOptions((Object[]) state.get("options"));
        }
    }

    public void showFilterSettingsDialog() {
        new FilterSettingsDialog(ResultSetViewer.this).open();
    }

    public void saveDataFilter() {
        DBCExecutionContext context = getExecutionContext();
        DBSDataContainer dataContainer = getDataContainer();
        if (context == null || dataContainer == null) {
            log.error("Can't save data filter with null context");
            return;
        }
        DataFilterRegistry.getInstance().saveDataFilter(dataContainer, model.getDataFilter());

        if (filtersPanel != null) {
            DBeaverNotifications.showNotification(
                DBeaverNotifications.NT_GENERIC,
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
        boolean enableFilters = getExecutionContext() != null && container.isReadyToRun() && !model.isUpdateInProgress();
        getAutoRefresh().enableControls(enableFilters);

        if (filtersPanel == null || this.viewerPanel.isDisposed()) {
            return;
        }
        if (resultSetSize != null && !resultSetSize.isDisposed()) {
            resultSetSize.setEnabled(getDataContainer() != null);
        }

        this.viewerPanel.setRedraw(false);
        try {
            DBCExecutionContext context = getExecutionContext();
            if (context != null) {
                if (!(activePresentation instanceof StatisticsPresentation)) {
                    StringBuilder where = new StringBuilder();
                    SQLUtils.appendConditionString(
                        model.getDataFilter(),
                        context.getDataSource(),
                        null,
                        where,
                        true,
                        SQLSemanticProcessor.isForceFilterSubQuery(context.getDataSource()));
                    if (resetFilterValue) {
                        String whereCondition = where.toString().trim();
                        filtersPanel.setFilterValue(whereCondition);
                        if (!whereCondition.isEmpty()) {
                            filtersPanel.addFiltersHistory(whereCondition);
                        }
                    }
                }
            }
            filtersPanel.enableFilters(enableFilters);
            //presentationSwitchToolbar.setEnabled(enableFilters);
        } finally {
            this.viewerPanel.setRedraw(true);
        }
    }

    @Override
    public DBDDataFilter getDataFilter() {
        return model.getDataFilter();
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

    @Override
    public void setSegmentFetchSize(Integer segmentFetchSize) {
        this.segmentFetchSize = segmentFetchSize;
    }

    ////////////////////////////////////////////////////////////
    // Misc

    @NotNull
    public DBPPreferenceStore getPreferenceStore() {
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
        return isDarkHighContrast ? UIStyles.getDefaultWidgetBackground() : UIStyles.getDefaultTextBackground();
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

    private void showErrorPresentation(String sqlText, String message, Throwable error) {
        activePresentationDescriptor = null;
        setActivePresentation(
            new ErrorPresentation(
                sqlText,
                GeneralUtils.makeErrorStatus(message, error), container instanceof IResultSetContainerExt ? (IResultSetContainerExt) container : null));
        updatePresentationInToolbar();
        fireQueryExecuted(sqlText, null, error.getMessage());
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
                if (filtersPanel != null) {
                    UIUtils.setControlVisible(filtersPanel, false);
                }
                if (statusBar != null) {
                    UIUtils.setControlVisible(statusBar, false);
                }
                availablePresentations = Collections.emptyList();
                setActivePresentation(new StatisticsPresentation());
                activePresentationDescriptor = null;
                changed = true;
            } else {
                // Regular results
                if (filtersPanel != null) {
                    UIUtils.setControlVisible(filtersPanel, true);
                }
                if (statusBar != null) {
                    UIUtils.setControlVisible(statusBar, true);
                }
                IResultSetContext context = new ResultSetContextImpl(this, resultSet);
                final List<ResultSetPresentationDescriptor> newPresentations;

                // Check for preferred presentation
                String preferredPresentationId = getDecorator().getPreferredPresentation();
                if (CommonUtils.isEmpty(preferredPresentationId)) {
                    newPresentations = ResultSetPresentationRegistry.getInstance().getAvailablePresentations(resultSet, context);
                } else {
                    ResultSetPresentationDescriptor preferredPresentation = ResultSetPresentationRegistry.getInstance().getPresentation(preferredPresentationId);
                    if (preferredPresentation != null) {
                        newPresentations = Collections.singletonList(preferredPresentation);
                    } else {
                        log.error("Presentation '" + preferredPresentationId + "' not found");
                        newPresentations = Collections.emptyList();
                    }
                }
                changed = CommonUtils.isEmpty(this.availablePresentations) || !newPresentations.equals(this.availablePresentations);
                this.availablePresentations = newPresentations;
                if (!this.availablePresentations.isEmpty()) {
                    if (activePresentationDescriptor != null && (!metadataChanged || activePresentationDescriptor.getPresentationType().isPersistent())) {
                        if (this.availablePresentations.contains(activePresentationDescriptor)) {
                            // Keep the same presentation
                            fireResultSetModelPrepared();
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
                        if(getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_AUTOMATIC_ROW_COUNT)){
                            updateRowCount();
                        }
                    } catch (Throwable e) {
                        DBWorkbench.getPlatformUI().showError("Presentation activate", "Can't instantiate data view '" + newPresentation.getLabel() + "'", e);
                    }
                } else {
                    log.debug("No presentations for result set [" + resultSet.getClass().getSimpleName() + "]");
                    showEmptyPresentation();
                }
                fireResultSetModelPrepared();
            }
        } finally {
            if (changed && presentationSwitchFolder != null) {
                updatePresentationInToolbar();
            }
            isUIUpdateRunning = false;
        }

    }

    private void updatePresentationInToolbar() {
        if (presentationSwitchFolder == null) {
            return;
        }
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
                recordModeButton.setAction(new ToggleModeAction(this), true);

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
                            ResultSetHandlerTogglePanel.showResultsPanel(ResultSetViewer.this, panel.getId(), isPanelVisible);
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
        if (selectedPresentation == activePresentationDescriptor) {
            return;
        }
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

/*
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
*/

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
                    if (showPanels) {
                        panel.setFocus();
                    }
                    //panelTab.getControl().setFocus();
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
            if (showPanels) {
                panel.setFocus();
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
        if (settings.enabledPanelIds.isEmpty() && !availablePanels.isEmpty()) {
            settings.enabledPanelIds.add(availablePanels.get(0).getId());
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
            if (showDefaults) {
                updatePanelsContent(false);
            }
            activePresentation.updateValueView();

            // Set focus to panel
            if (activePanelTab != null && !activePanelTab.getControl().isDisposed() && UIUtils.hasFocus(activePresentation.getControl())) {
                activePanelTab.getControl().setFocus();
            }
            // Make sure focus cell is visible
            DBDAttributeBinding currentAttribute = getActivePresentation().getCurrentAttribute();
            if (currentAttribute != null) {
                getActivePresentation().showAttribute(currentAttribute);
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
        panelToolBar.add(new Action(ResultSetMessages.result_set_view_menu_text, WorkbenchImages.getImageDescriptor(IWorkbenchGraphicConstants.IMG_LCL_VIEW_MENU)) {
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
        if (adapter == IResultSetContext.class) {
            return adapter.cast(new ResultSetContextImpl(this, null));
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

    @Override
    public void updateDirtyFlag() {
        synchronized (listeners) {
            for (IResultSetListener listener : listeners) {
                listener.handleResultSetChange();
            }
        }
    }

    public void updateEditControls()
    {
        fireResultSetChange();
        updateToolbar();
        if (presentationSwitchFolder != null) {
            // Enable presentations
            for (VerticalButton pb : presentationSwitchFolder.getItems()) {
                if (pb.getData() instanceof ResultSetPresentationDescriptor) {
                    pb.setVisible(!recordMode || ((ResultSetPresentationDescriptor) pb.getData()).supportsRecordMode());
                }
            }
        }
    }

    /**
     * It is a hack function. Generally all command associated widgets should be updated automatically by framework.
     * Freaking E4 do not do it. I've spent a couple of days fighting it. Guys, you owe me.
     */
    @Override
    public void updateToolbar() {
        if (statusBar == null || statusBar.isDisposed()) {
            return;
        }
        if (statusBar != null) statusBar.setRedraw(false);
        try {
            for (ToolBarManager tb : toolbarList) {
                UIUtils.updateContributionItems(tb);
            }
            if (panelToolBar != null) {
                UIUtils.updateContributionItems(panelToolBar);
            }
            if (statusBar != null) statusBar.layout(true, true);
        } finally {
            if (statusBar != null) statusBar.setRedraw(true);
        }
    }

    @NotNull
    @Override
    public DBDValueHintContext getHintContext() {
        return model.getHintContext();
    }

    public void redrawData(boolean attributesChanged, boolean rowsChanged) {
        if (viewerPanel.isDisposed()) {
            return;
        }
        if (rowsChanged) {
            int rowCount = model.getRowCount();
            if (curRow == null || curRow.getVisualNumber() >= rowCount) {
                curRow = rowCount == 0 ? null : model.getRow(rowCount - 1);
                selectedRecords = curRow == null ? new int[0] : new int[] { curRow.getVisualNumber() };
            }

            // Set cursor on new row
            if (!recordMode) {
                this.updateFiltersText();
            }
        }
        model.refreshValueHandlersConfiguration();
        activePresentation.refreshData(attributesChanged || (rowsChanged && recordMode), false, true);
        //this.updateStatusMessage();
    }

    private final Job statusBarLayoutJob = new Job("Pending resultset view status bar relayout") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            UIUtils.asyncExec(() -> {
                boolean changed = false;
                for (ToolBarManager toolbarManager : toolbarList) {
                    ToolBar toolbar = toolbarManager.getControl();
                    boolean wasCollapsed = toolbar.getLayoutData() != null;
                    toolbar.setLayoutData(toolbar.getItemCount() > 0 ? null : new RowData(0, 0));
                    boolean nowCollapsed = toolbar.getLayoutData() != null;
                    changed |= (wasCollapsed != nowCollapsed);
                }
                if (changed) {
                    ResultSetViewer.this.getControl().layout(true, true);
                }
            });
            return Status.OK_STATUS;
        }
    };

    private final IPropertyChangeListener propertyEvaluationRequestListener = ev -> {
        if (TOOLBAR_CONFIGURATION_VISIBLE_PROPERTY.equals(ev.getProperty())) { //$NON-NLS-1$
            if (!getControl().isDisposed()) {
                statusBarLayoutJob.schedule(30);
            }
        }
    };

    private void createStatusBar() {
        ActionUtils.addPropertyEvaluationRequestListener(propertyEvaluationRequestListener);

        final IMenuService menuService = getSite().getService(IMenuService.class);

        Composite statusComposite = UIUtils.createPlaceholder(viewerPanel, 3);
        statusComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        statusBar = new Composite(statusComposite, SWT.NONE);
        statusBar.setBackgroundMode(SWT.INHERIT_FORCE);
        statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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
            ToolBar rsToolbar = new ToolBar(statusBar, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            // Refresh
            getAutoRefresh().populateRefreshButton(
                rsToolbar,
                ActionUtils.findCommandName(IWorkbenchCommandConstants.FILE_REFRESH),
                ResultSetMessages.controls_resultset_viewer_action_refresh + " (" +
                    ActionUtils.findCommandDescription(IWorkbenchCommandConstants.FILE_REFRESH, getSite(), true) + ")",
                UIIcon.REFRESH,
                () -> {
                    if (!isRefreshInProgress()) {
                        refreshData(null);
                    }
                }
            );
            getAutoRefresh().enableControls(false);
        }
        if (CommonUtils.isBitSet(decorator.getDecoratorFeatures(), IResultSetDecorator.FEATURE_EDIT)) {
            ToolBarManager editToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            menuService.populateContributionManager(editToolBarManager, TOOLBAR_EDIT_CONTRIBUTION_ID);
            ToolBar editorToolBar = editToolBarManager.createControl(statusBar);
            CSSUtils.setCSSClass(editorToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
            toolbarList.add(editToolBarManager);
        }
        {
            ToolBarManager navToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            menuService.populateContributionManager(navToolBarManager, TOOLBAR_NAVIGATION_CONTRIBUTION_ID);
            ToolBar navToolBar = navToolBarManager.createControl(statusBar);
            CSSUtils.setCSSClass(navToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
            toolbarList.add(navToolBarManager);
        }
        {
            ToolBarManager configToolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            ToolBar configToolBar = configToolBarManager.createControl(statusBar);
            CSSUtils.setCSSClass(configToolBar, DBStyles.COLORED_BY_CONNECTION_TYPE);
            toolbarList.add(configToolBarManager);
        }

        {
            ToolBarManager addToolbBarManagerar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            if (!BasePolicyDataProvider.getInstance().isPolicyEnabled(DTConstants.POLICY_DATA_EXPORT)) {
                menuService.populateContributionManager(addToolbBarManagerar, TOOLBAR_EXPORT_CONTRIBUTION_ID);
            }

            addToolbBarManagerar.add(new GroupMarker(TOOLBAR_GROUP_PRESENTATIONS));
            addToolbBarManagerar.add(new Separator(TOOLBAR_GROUP_ADDITIONS));

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
            resultSetSize.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    String realValue = String.valueOf(getSegmentMaxRows());
                    if (!realValue.equals(resultSetSize.getText())) {
                        resultSetSize.setText(realValue);
                    }
                }
            });
            resultSetSize.addModifyListener(e -> {
                DBSDataContainer dataContainer = getDataContainer();
                int fetchSize = CommonUtils.toInt(resultSetSize.getText());
                if (fetchSize > 0 && fetchSize < ResultSetPreferences.MIN_SEGMENT_SIZE) {
                    fetchSize = ResultSetPreferences.MIN_SEGMENT_SIZE;
                }
                if (dataContainer != null && dataContainer.getDataSource() != null) {
                    DBPPreferenceStore store = dataContainer.getDataSource().getContainer().getPreferenceStore();
                    int oldFetchSize = store.getInt(ModelPreferences.RESULT_SET_MAX_ROWS);
                    if (oldFetchSize != fetchSize) {
                        store.setValue(ModelPreferences.RESULT_SET_MAX_ROWS, fetchSize);
                        PrefUtils.savePreferenceStore(store);
                    }
                }
            });
            UIUtils.addDefaultEditActionsSupport(site, resultSetSize);

            rowCountLabel = new ActiveStatusMessage(statusBar, DBeaverIcons.getImage(UIIcon.COMPILE), ResultSetMessages.controls_resultset_viewer_calculate_row_count, this) {
                @Override
                protected boolean isActionEnabled() {
                    return hasData();
                }

                @Override
                protected ILoadService<String> createLoadService() {
                    return new DatabaseLoadService<>("Load row count", getExecutionContext()) {
                        @Override
                        public String evaluate(DBRProgressMonitor monitor) throws InvocationTargetException {
                            try {
                                long rowCount = readRowCount(monitor);
                                return ROW_COUNT_FORMAT.format(rowCount);
                            } catch (DBException e) {
                                log.error(e);
                                throw new InvocationTargetException(e);
                            }
                        }
                    };
                }
            };
            //rowCountLabel.setLayoutData();
            CSSUtils.setCSSClass(rowCountLabel, DBStyles.COLORED_BY_CONNECTION_TYPE);
            rowCountLabel.setMessage("Row Count");
            rowCountLabel.setToolTipText("Calculates total row count in the current dataset");
            UIUtils.createToolBarSeparator(statusBar, SWT.VERTICAL);

            selectionStatLabel = new Text(statusBar, SWT.READ_ONLY);
            selectionStatLabel.setToolTipText(ResultSetMessages.result_set_viewer_selection_stat_tooltip);
            CSSUtils.setCSSClass(selectionStatLabel, DBStyles.COLORED_BY_CONNECTION_TYPE);
            selectionStatLabel.setText(" ");

//            Label filler = new Label(statusComposite, SWT.NONE);
//            filler.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            //UIUtils.createToolBarSeparator(statusBar, SWT.VERTICAL);

            if (!CommonUtils.isBitSet(decorator.getDecoratorFeatures(), IResultSetDecorator.FEATURE_COMPACT_STATUS)) {
                statusLabel = new StatusLabel(statusBar, SWT.NONE, this);
                RowData rd = new RowData();
                rd.width = 50 * fontHeight;
                statusLabel.setLayoutData(rd);
                CSSUtils.setCSSClass(statusLabel, DBStyles.COLORED_BY_CONNECTION_TYPE);
            }
        }

        statusBarLayoutJob.schedule(10);
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

    @Override
    public int[] getSelectedRecords() {
        return selectedRecords;
    }

    @Override
    public void setSelectedRecords(int[] indexes) {
        selectedRecords = indexes;
    }

    @Override
    public boolean isAllAttributesReadOnly() {
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext != null &&
            (executionContext.getDataSource().getContainer().isConnectionReadOnly() ||
            executionContext.getDataSource().getInfo().isReadOnlyData() ||
            isUniqueKeyUndefinedButRequired(executionContext)))
        {
            return true;
        }
        if (model.getAttributes().length == 0) {
            return false;
        }
        for (DBDAttributeBinding attr : model.getAttributes()) {
            DBDRowIdentifier rowIdentifier = attr.getRowIdentifier();
            if (rowIdentifier != null && rowIdentifier.isValidIdentifier()) {
                return false;
            }
        }
        return true;
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
        List<ResultSetRow> selectedRows = getSelection().getSelectedRows();
        if (selectedRows.isEmpty()) {
            if (model.getRowCount() > 0) {
                selectedRows = Collections.singletonList(model.getRow(0));
            }
        }
        this.selectedRecords = new int[selectedRows.size()];
        for (int i = 0; i < selectedRows.size(); i++) {
            this.selectedRecords[i] = selectedRows.get(i).getVisualNumber();
        }
        if (selectedRecords.length > 0 && selectedRecords[0] < model.getRowCount()) {
            curRow = model.getRow(selectedRecords[0]);
        } else {
            curRow = null;
        }

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
        ActionUtils.removePropertyEvaluationRequestListener(propertyEvaluationRequestListener);

        if (!themeUpdateJob.isCanceled()) {
            themeUpdateJob.cancel();
        }
        if (themeChangeListener != null) {
            PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeChangeListener);
            themeChangeListener = null;
        }

        DBWorkbench.getPlatform().getDataSourceProviderRegistry().getGlobalDataSourcePreferenceStore().removePropertyChangeListener(dataPropertyListener);
        DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(dataPropertyListener);

        if (activePresentation != null) {
            activePresentation.dispose();
        }
        DBPProject project = container.getProject();
        if (project != null) {
            project.getDataSourceRegistry().removeDataSourceListener(this);
        }

        savePresentationSettings();
        clearData(true);

        for (ToolBarManager tb : toolbarList) {
            try {
                tb.dispose();
            } catch (Throwable e) {
                // ignore
                log.debug("Error disposing toolbar " + tb, e);
            }
        }
        toolbarList.clear();
        autoRefreshControl.enableAutoRefresh(false);
    }

    @Override
    public String getAttributeReadOnlyStatus(DBDAttributeBinding attr, boolean checkEntity, boolean checkKey) {
        if (checkEntity) {
            String dataStatus = getReadOnlyStatus();
            if (dataStatus != null) {
                return dataStatus;
            }
        }
        boolean newRow = (curRow != null && curRow.getState() == ResultSetRow.STATE_ADDED);
        if (!newRow) {
            return DBExecUtils.getAttributeReadOnlyStatus(
                attr,
                checkKey);
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
    public void setCurrentRow(@Nullable ResultSetRow newRow) {
        int rowShift = 0;
        if (this.curRow != null && newRow != null) {
            rowShift = newRow.getVisualNumber() - curRow.getVisualNumber();
        }
        this.curRow = newRow;
        if (curState != null && newRow != null) {
            curState.rowNumber = newRow.getVisualNumber();
        }
        if (this.recordMode && rowShift != 0 && selectedRecords.length > 0) {
            if (!ArrayUtils.contains(selectedRecords, curRow.getVisualNumber())) {
                // Shift     selected records
                int firstSelRecord = selectedRecords[0];
                firstSelRecord += rowShift;
                if (firstSelRecord < 0) firstSelRecord = 0;
                if (firstSelRecord > model.getRowCount() - selectedRecords.length) {
                    firstSelRecord = model.getRowCount() - selectedRecords.length;
                }
                for (int i = 0; i < selectedRecords.length; i++) {
                    selectedRecords[i] = firstSelRecord + i;
                }
            }
        } else {
            selectedRecords = new int[0];
        }
    }

    ///////////////////////////////////////
    // Status

    public void setStatus(String status) {
        setStatus(status, DBPMessageType.INFORMATION);
    }

    public void setStatus(String status, DBPMessageType messageType)
    {
        if (statusLabel != null && !statusLabel.isDisposed() &&
            (statusLabel.getMessageType() != messageType || !CommonUtils.equalObjects(statusLabel.getMessage(), status))
        ) {
            statusLabel.setStatus(status, messageType);
            ((RowData) statusLabel.getLayoutData()).width = statusLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        }

        if (rowCountLabel != null) {
            rowCountLabel.updateActionState();
        }

        if (resultSetSize != null) {
            DBSDataContainer dataContainer = getDataContainer();
            if (dataContainer != null && dataContainer.getDataSource() != null) {
                resultSetSize.setText(String.valueOf(getSegmentMaxRows()));
            }
        }
    }

    private void setStatusTooltip(String message) {
        if (statusLabel == null || statusLabel.isDisposed()) {
            return;
        }
        statusLabel.setStatusTooltip(message);
    }

    public void updateStatusMessage()
    {
        updateStatusInfo(false);
        updateStatusInfo(true);
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

    private void updateStatusInfo(boolean isTooltip) {
        String statusMessage;
        if (model.getRowCount() == 0) {
            if (model.getVisibleAttributeCount() == 0) {
                statusMessage =
                    ResultSetMessages.controls_resultset_viewer_status_empty + getExecutionTimeMessage(isTooltip);
            } else {
                statusMessage =
                    ResultSetMessages.controls_resultset_viewer_status_no_data + getExecutionTimeMessage(isTooltip);
            }
        } else {
            if (recordMode) {
                statusMessage =
                    ResultSetMessages.controls_resultset_viewer_status_row + (curRow == null ? 0 : curRow.getVisualNumber() + 1) +
                        "/" + model.getRowCount() +
                    (curRow == null ? getExecutionTimeMessage(isTooltip) : "");
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
                    statusMessage = NLS.bind(
                        ResultSetMessages.controls_resultset_viewer_status_rows_updated,
                        ResultSetUtils.formatRowCount(rowsUpdated),
                        getExecutionTimeMessage(isTooltip)
                    );
                } else {
                    statusMessage = NLS.bind(
                        ResultSetMessages.controls_resultset_viewer_status_rows_fetched,
                        ResultSetUtils.formatRowCount(rowsFetched),
                        getExecutionTimeMessage(isTooltip)
                    );
                }
            }
        }
        boolean hasWarnings = !dataReceiver.getErrorList().isEmpty();
        if (hasWarnings) {
            statusMessage += " - " + dataReceiver.getErrorList().size() + " warning(s)";
        } else if (model.getStatistics() != null) {
            hasWarnings = model.getStatistics().getError() != null;
            if (hasWarnings) {
                statusMessage += " - finished with error";
            }
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
        if (isTooltip) {
            setStatusTooltip(statusMessage);
        } else {
            setStatus(statusMessage, hasWarnings ? DBPMessageType.WARNING :DBPMessageType.INFORMATION);
        }
    }

    private String getExecutionTimeMessage(boolean extended)
    {
        DBCStatistics statistics = model.getStatistics();
        if (statistics == null || statistics.isEmpty()) {
            return "";
        }
        long fetchTime = statistics.getFetchTime();
        long totalTime = statistics.getTotalTime();
        Object endTime;
        if (extended) {
            endTime = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(statistics.getEndTime()), TimeZone.getDefault().toZoneId())
                .format(EXECUTION_TIME_FORMATTER);
        } else {
            endTime = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(statistics.getEndTime()), TimeZone.getDefault().toZoneId())
                .truncatedTo(ChronoUnit.SECONDS);
        }
        if (fetchTime <= 0) {
            if (endTime instanceof LocalDateTime) {
                return NLS.bind(
                    ResultSetMessages.controls_resultset_viewer_status_rows_time,
                    new Object[]{
                        RuntimeUtils.formatExecutionTime(totalTime),
                        DateTimeFormatter.ISO_DATE.format(((LocalDateTime) endTime)),
                        DateTimeFormatter.ISO_TIME.format(((LocalDateTime) endTime)),
                    });
            } else {
                return NLS.bind(
                    ResultSetMessages.controls_resultset_viewer_status_rows_time_long,
                    new Object[]{
                        RuntimeUtils.formatExecutionTime(totalTime), endTime
                    });

            }
        } else {
            if (endTime instanceof LocalDateTime) {
                return NLS.bind(
                    ResultSetMessages.controls_resultset_viewer_status_rows_time_fetch,
                    new Object[]{
                        RuntimeUtils.formatExecutionTime(totalTime),
                        RuntimeUtils.formatExecutionTime(fetchTime),
                        DateTimeFormatter.ISO_DATE.format(((LocalDateTime) endTime)),
                        DateTimeFormatter.ISO_TIME.format(((LocalDateTime) endTime)),
                    });
            } else {
                return NLS.bind(
                    ResultSetMessages.controls_resultset_viewer_status_rows_time_fetch_long,
                    new Object[]{
                        RuntimeUtils.formatExecutionTime(totalTime),
                        RuntimeUtils.formatExecutionTime(fetchTime),
                        endTime,
                    });
            }
        }
    }

    ///////////////////////////////////////
    // Ordering

    @Override
    public void toggleSortOrder(@NotNull DBDAttributeBinding columnElement, @Nullable ColumnOrder forceOrder) {
        DBDDataFilter dataFilter = getModel().getDataFilter();
        // if (forceOrder == ColumnOrder.ASC) {
        //     dataFilter.resetOrderBy();
        // }
        DBDAttributeConstraint constraint = dataFilter.getConstraint(columnElement);
        assert constraint != null;
        ResultSetUtils.OrderingMode orderingMode = ResultSetUtils.getOrderingMode(this);
        if (CommonUtils.isNotEmpty(model.getDataFilter().getOrder())) {
            orderingMode = ResultSetUtils.OrderingMode.SERVER_SIDE;
        }
        if (constraint.getOrderPosition() == 0 && forceOrder != ColumnOrder.NONE) {
            if (orderingMode == ResultSetUtils.OrderingMode.SERVER_SIDE && supportsDataFilter()) {
                if (ConfirmationDialog.confirmAction(
                    viewerPanel.getShell(),
                    ConfirmationDialog.WARNING,
                    ResultSetPreferences.CONFIRM_ORDER_RESULTSET,
                    ConfirmationDialog.QUESTION,
                    columnElement.getName()) != IDialogConstants.YES_ID)
                {
                    return;
                }
            }
            constraint.setOrderPosition(dataFilter.getMaxOrderingPosition() + 1);
            constraint.setOrderDescending(forceOrder == ColumnOrder.DESC);
        } else if (forceOrder == ColumnOrder.ASC) {
            constraint.setOrderDescending(false);
        } else if (forceOrder == ColumnOrder.DESC) {
            constraint.setOrderDescending(true);
        } else if (forceOrder == null && constraint.getOrderPosition() > 0 && !constraint.isOrderDescending()) {
            // Toggle to DESC ordering
            constraint.setOrderDescending(true);
        } else {
            // Reset order
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

        if (!this.checkForChanges()) {
            return;
        }

        boolean serverSideOrdering = switch (orderingMode) {
            case CLIENT_SIDE -> false;
            case SERVER_SIDE -> true;
            default -> isHasMoreData();
        };

        if (serverSideOrdering && getDataSource() != null && !getDataSource().getInfo().supportsResultSetOrdering()) {
            ConfirmationDialog.confirmAction(getControl().getShell(), CONFIRM_SERVER_SIDE_ORDERING_UNAVAILABLE, ConfirmationDialog.WARNING);
            serverSideOrdering = false;
        }

        if (serverSideOrdering) {
            this.refreshData(null);
        } else {
            this.reorderLocally(columnElement);
        }
    }

    private void reorderLocally(DBDAttributeBinding columnElement)
    {
        this.rejectChanges();
        this.getModel().resetOrdering(columnElement);
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

    void setData(@NotNull DBRProgressMonitor monitor, List<Object[]> rows, int focusRow)
    {
        if (viewerPanel.isDisposed()) {
            return;
        }
        this.curRow = null;
        this.model.setData(monitor, rows);
        this.curRow = (this.model.getRowCount() > 0 ? this.model.getRow(0) : null);
        if (focusRow > 0 && focusRow < model.getRowCount()) {
            this.curRow = model.getRow(focusRow);
        }
        if (this.selectedRecords.length > 1) {
            this.selectedRecords = Arrays.stream(this.selectedRecords).filter(value -> value < rows.size()).toArray();
            if (this.selectedRecords.length == 0) {
                this.selectedRecords = this.curRow == null ? new int[0] : new int[]{curRow.getVisualNumber()};
            }
        } else {
            this.selectedRecords = this.curRow == null ? new int[0] : new int[]{curRow.getVisualNumber()};
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

    void appendData(@NotNull DBRProgressMonitor monitor, List<Object[]> rows, boolean resetOldRows) {
        model.appendData(monitor, rows, resetOldRows);

        UIUtils.asyncExec(() -> {
            String message = NLS.bind(ResultSetMessages.controls_resultset_viewer_status_rows_size, model.getRowCount(),
                rows.size()) + getExecutionTimeMessage(false);
            String tooltip = NLS.bind(ResultSetMessages.controls_resultset_viewer_status_rows_size, model.getRowCount(),
                rows.size()) + getExecutionTimeMessage(true);
            setStatus(message, DBPMessageType.INFORMATION);
            setStatusTooltip(tooltip);
            updateEditControls();
        });
    }

    @Override
    public int promptToSaveOnClose()
    {
        if (!isDirty()) {
            return ISaveablePart2.YES;
        }
        int result = ConfirmationDialog.confirmAction(
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
            executionContext.getDataSource().getInfo().isReadOnlyData() ||
            isUniqueKeyUndefinedButRequired(executionContext);
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
            return "No connection to database";
        }
        if (executionContext.getDataSource().getContainer().isConnectionReadOnly()) {
            return "Connection is in read-only state";
        }
        if (executionContext.getDataSource().getInfo().isReadOnlyData()) {
            return "Read-only data container";
        }
        if (!executionContext.getDataSource().getContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_DATA)) {
            return "Data edit restricted";
        }
        if (isUniqueKeyUndefinedButRequired(executionContext)) {
            return "No unique key defined";
        }
        return null;
    }

    private boolean isUniqueKeyUndefinedButRequired(@NotNull DBCExecutionContext context) {
        final DBPPreferenceStore store = context.getDataSource().getContainer().getPreferenceStore();

        if (store.getBoolean(ResultSetPreferences.RS_EDIT_DISABLE_IF_KEY_MISSING)) {
            final DBDRowIdentifier identifier = model.getDefaultRowIdentifier();
            return identifier == null || !identifier.isValidIdentifier();
        }

        return false;
    }

    /**
     * Checks that current state of result set allows to insert new rows
     * @return true if new rows insert is allowed
     */
    public boolean isInsertable()
    {
        return
            getReadOnlyStatus() == null &&
            model.getSingleSource() instanceof DBSDataManipulator &&
            model.getVisibleAttributeCount() > 0;
    }

    public boolean isRefreshInProgress() {
        synchronized (dataPumpJobQueue) {
            return dataPumpRunning.get();
        }
    }

    public void cancelJobs() {
        List<ResultSetJobAbstract> dpjCopy;
        synchronized (dataPumpJobQueue) {
            dpjCopy = new ArrayList<>(this.dataPumpJobQueue);
            this.dataPumpJobQueue.clear();
        }
        for (ResultSetJobAbstract dpj : dpjCopy) {
            if (dpj.isActiveTask()) {
                dpj.cancel();
            }
        }
        DataSourceJob updateJob = model.getUpdateJob();
        if (updateJob != null) {
            updateJob.cancel();
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
    public void showColumnMenu(DBDAttributeBinding curAttribute) {
        MenuManager columnMenu = new MenuManager();
        ResultSetRow currentRow = getCurrentRow();

        fillOrderingsMenu(columnMenu, curAttribute, currentRow);
        fillFiltersMenu(columnMenu, curAttribute, currentRow);

        final Menu contextMenu = columnMenu.createContextMenu(getActivePresentation().getControl());
        contextMenu.setLocation(Display.getCurrent().getCursorLocation());
        contextMenu.addMenuListener(MenuListener.menuHiddenAdapter(menuEvent -> UIUtils.asyncExec(columnMenu::dispose)));
        contextMenu.setVisible(true);
    }

    public void showFiltersDistinctMenu(DBDAttributeBinding curAttribute, boolean atKeyboardCursor) {

        boolean isExpensiveFilter = true;
        {
            DBSEntityReferrer descReferrer = ResultSetUtils.getEnumerableConstraint(curAttribute);
            if (descReferrer instanceof DBSEntityAssociation) {
                // FK to dictionary - simple query
                isExpensiveFilter = false;
            } else {
                // Column enumeration is expensive
            }
        }
        if (isExpensiveFilter) {
            if (curAttribute.getEntityAttribute() == null) {
                // No entity reference - we cannot fetch any filter values for this attribute anyway
            } else {
                if (ConfirmationDialog.confirmAction(
                    viewerPanel.getShell(),
                    ConfirmationDialog.WARNING, ResultSetPreferences.CONFIRM_FILTER_RESULTSET,
                    ConfirmationDialog.CONFIRM,
                    curAttribute.getName()) != IDialogConstants.OK_ID)
                {
                    return;
                }
            }
        }

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

        popup.setModeless(true);
        if (popup.open() == IDialogConstants.OK_ID) {
            Object value = popup.getValue();

            DBDDataFilter filter = new DBDDataFilter(model.getDataFilter());
            DBDAttributeConstraint constraint = filter.getConstraint(curAttribute);
            DBCLogicalOperator[] supportedOperators =
                curAttribute.getValueHandler().getSupportedOperators(curAttribute);
            if (constraint != null) {
                if (!ArrayUtils.isEmpty((Object[]) value)) {
                    if (getDataSource() != null && !ArrayUtils.contains(supportedOperators, DBCLogicalOperator.IN)) {
                        constraint.setOperator(DBCLogicalOperator.EQUALS);
                    } else {
                        constraint.setOperator(DBCLogicalOperator.IN);
                    }
                    constraint.setValue(value);
                } else {
                    constraint.setOperator(null);
                    constraint.setValue(null);
                }
                setDataFilter(filter, true);
            }
        }
    }

    public void showReferencesMenu(boolean openInNewWindow) {
        MenuManager[] menuManager = new MenuManager[1];
        try {
            UIUtils.runInProgressService(monitor ->
                menuManager[0] = createRefTablesMenu(monitor, openInNewWindow));
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
    public void fillContextMenu(@NotNull IMenuManager manager, @Nullable final DBDAttributeBinding attr, @Nullable final ResultSetRow row, int[] rowIndexes) {
        // Custom oldValue items
        final ResultSetValueController valueController;
        if (attr != null && row != null) {
            valueController = new ResultSetValueController(
                this,
                new ResultSetCellLocation(attr, row, rowIndexes),
                IValueController.EditType.NONE,
                null);
        } else {
            valueController = null;
        }

        long decoratorFeatures = getDecorator().getDecoratorFeatures();
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
                }

                manager.add(new Separator());

                // Filters and View
                if ((decoratorFeatures & IResultSetDecorator.FEATURE_FILTERS) != 0) {
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

                final MenuManager editMenu = new MenuManager(
                    ResultSetMessages.controls_resultset_viewer_action_edit,
                    DBeaverIcons.getImageDescriptor(UIIcon.ROW_EDIT),
                    MENU_ID_EDIT
                );

                if (row != null) {
                    fillEditMenu(editMenu, attr, row, valueController);
                }

                {
                    editMenu.add(new Separator());
                    editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_ADD));
                    editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_COPY));
                    editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_DELETE));
                    editMenu.add(new Separator());
                    editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_COPY_FROM_ABOVE));
                    editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_COPY_FROM_BELOW));
                }

                manager.add(new Separator());
                manager.add(editMenu);
                manager.add(new Separator());
            }
        }
        manager.add(new GroupMarker(MENU_GROUP_EDIT));

        DBPDataSource dataSource = getDataSource();
        if (dataSource != null && attr != null && model.getVisibleAttributeCount() > 0 && !model.isUpdateInProgress()) {
            MenuManager viewMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_view_format,
                null,
                MENU_ID_VIEW); //$NON-NLS-1$
            viewMenu.setRemoveAllWhenShown(true);
            viewMenu.addMenuListener(manager1 -> fillColumnViewMenu(manager1, attr, row, valueController));
            manager.add(viewMenu);
        }

        if (dataSource != null && !dataSource.getContainer().getNavigatorSettings().isHideVirtualModel()) {
            MenuManager viewMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_logical_structure,
                null,
                MENU_ID_VIRTUAL_MODEL); //$NON-NLS-1$
            viewMenu.setRemoveAllWhenShown(true);
            viewMenu.addMenuListener(manager1 -> fillVirtualModelMenu(manager1, attr, row, valueController));
            manager.add(viewMenu);
        }

        if ((decoratorFeatures & IResultSetDecorator.FEATURE_PANELS) != 0 || (decoratorFeatures & IResultSetDecorator.FEATURE_PRESENTATIONS) != 0) {
            MenuManager layoutMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_layout,
                null,
                MENU_ID_LAYOUT); //$NON-NLS-1$
            fillLayoutMenu(layoutMenu, attr);
            manager.add(layoutMenu);
        }

        manager.add(new Separator());

        final DBSDataContainer dataContainer = getDataContainer();

        // Fill general menu
        if (dataContainer != null) {
            if (!BasePolicyDataProvider.getInstance().isPolicyEnabled(DTConstants.POLICY_DATA_EXPORT)) {
                manager.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_EXPORT));
            }
            MenuManager openWithMenu = new MenuManager(ActionUtils.findCommandName(ResultSetHandlerOpenWith.CMD_OPEN_WITH));
            openWithMenu.setRemoveAllWhenShown(true);
            openWithMenu.addMenuListener(manager1 -> ResultSetHandlerOpenWith.fillOpenWithMenu(ResultSetViewer.this, manager1));
            manager.add(openWithMenu);

            manager.add(new GroupMarker(NavigatorCommands.GROUP_TOOLS));
            manager.add(new GroupMarker(MENU_GROUP_EXPORT));
        }

        //manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        decorator.fillContributions(manager);

        if (activePresentation != null) {
            activePresentation.fillMenu(manager);
        }

        manager.add(new Separator(MENU_GROUP_ADDITIONS));

        if (dataContainer != null) {
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_REFRESH));
        }
    }

    private void fillColumnViewMenu(IMenuManager viewMenu, @NotNull DBDAttributeBinding attr, @Nullable ResultSetRow row, ResultSetValueController valueController) {
        final DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return;
        }
        // Display format
        {
            if (activePresentation instanceof IResultSetDisplayFormatProvider displayFormatProvider) {
                DBDDisplayFormat defaultDisplayFormat = ((IResultSetDisplayFormatProvider) activePresentation).getDefaultDisplayFormat();
                MenuManager formatsMenu = new MenuManager(DTUIMessages.value_format_selector_value);
                for (DBDDisplayFormat displayFormat : DBDDisplayFormat.values()) {
                    String formatName = switch (displayFormat) {
                        case UI -> DTUIMessages.value_format_selector_display;
                        case EDIT -> DTUIMessages.value_format_selector_editable;
                        default -> DTUIMessages.value_format_selector_database_native;
                    };
                    Action action = new Action(formatName, Action.AS_RADIO_BUTTON) {
                        @Override
                        public void run() {
                            displayFormatProvider.setDefaultDisplayFormat(displayFormat);
                        }
                    };
                    action.setChecked(displayFormat == defaultDisplayFormat);
                    formatsMenu.add(action);
                }
                viewMenu.add(formatsMenu);
                viewMenu.add(new Separator());
            }
        }
        // Transformers
        {
            List<? extends DBDAttributeTransformerDescriptor> transformers =
                DBWorkbench.getPlatform().getValueHandlerRegistry().findTransformers(
                    dataSource, attr, null);
            if (!CommonUtils.isEmpty(transformers)) {
                MenuManager transformersMenu = new MenuManager(NLS.bind(ResultSetMessages.controls_resultset_viewer_action_view_column_type, attr.getName()));
                transformersMenu.setRemoveAllWhenShown(true);
                transformersMenu.addMenuListener(manager12 -> fillAttributeTransformersMenu(manager12, attr));
                viewMenu.add(transformersMenu);
            }
        }
        if (model.isSingleSource()) {
            viewMenu.add(new TransformerSettingsAction(this));
        }
        viewMenu.add(new TransformComplexTypesToggleAction(this));
        if (attr.getDataKind() == DBPDataKind.BINARY || attr.getDataKind() == DBPDataKind.CONTENT) {
            MenuManager binaryFormatMenu = new MenuManager(ResultSetMessages.controls_resultset_viewer_action_binary_format);
            binaryFormatMenu.setRemoveAllWhenShown(true);
            binaryFormatMenu.addMenuListener(manager12 -> fillBinaryFormatMenu(manager12, attr));
            viewMenu.add(binaryFormatMenu);
        }
        if (activePresentationDescriptor.supportsHints()) {
            // Hints
            viewMenu.add(new Separator());
            MenuManager hintsMenu = new MenuManager(ResultSetMessages.controls_resultset_viewer_action_view_hints);
            hintsMenu.setRemoveAllWhenShown(true);
            hintsMenu.addMenuListener(manager12 -> fillAttributeHintsMenu(manager12, attr));
            viewMenu.add(hintsMenu);
        }

        // Row colors
        viewMenu.add(new Separator());
        if (model.getDocumentAttribute() == null) {
            if (valueController != null) {
                String bindingText = attr.getName()
                    + " = "
                    + UITextUtils.getShortText(this.getControl(), CommonUtils.toString(valueController.getValue()), 100);
                String msgSelectRowColor = NLS.bind(ResultSetMessages.actions_name_color_by, bindingText);
                // select row color
                viewMenu.add(ActionUtils.makeCommandContribution(this.getSite(),
                    ResultSetHandlerMain.CMD_SELECT_ROW_COLOR, msgSelectRowColor, null));
                for (DBVColorOverride mapping : getColorOverrides(attr, valueController.getValue())) {
                    viewMenu.add(new ResetRowColorAction(this, mapping, valueController.getValue()));
                }
            }
            viewMenu.add(new CustomizeColorsAction(this, attr, row));
            if (hasColorOverrides()) {
                viewMenu.add(new ResetAllColorAction(this));
            }
        }
        viewMenu.add(new ColorizeDataTypesToggleAction(this));
        viewMenu.add(new Separator());
        viewMenu.add(new DataFormatsPreferencesAction(this));
    }

    private void fillAttributeHintsMenu(IMenuManager menuManager, DBDAttributeBinding attr) {
        for (ValueHintProviderDescriptor hd : ValueHintRegistry.getInstance().getHintDescriptors()) {
            menuManager.add(new HintEnablementAction(this, hd));
        }
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

        if (dataSource.getInfo().supportsReferentialIntegrity()) {
            possibleActions.add(new VirtualForeignKeyEditAction(this));

            possibleActions.add(new VirtualUniqueKeyEditAction(this, true));
            possibleActions.add(new VirtualUniqueKeyEditAction(this, false));
        }

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
                editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_EDIT));
                editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_EDIT_INLINE));
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
        }
    }

    private void fillLayoutMenu(IMenuManager layoutMenu, DBDAttributeBinding attr) {
        if (activePresentationDescriptor != null && activePresentationDescriptor.supportsRecordMode()) {
            layoutMenu.add(new ToggleModeAction(this));
        }
        if ((getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_PANELS) != 0) {
            layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_PANELS));
            layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ACTIVATE_PANELS));
            layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_TOGGLE_LAYOUT));
        }
        if ((getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_PRESENTATIONS) != 0) {
            layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_SWITCH_PRESENTATION));
        }
        if ((getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_PANELS) != 0) {
            MenuManager panelsMenu = new MenuManager(
                ResultSetMessages.controls_resultset_viewer_action_panels,
                DBeaverIcons.getImageDescriptor(UIIcon.PANEL_CUSTOMIZE),
                "result_panels"); //$NON-NLS-1$
            layoutMenu.add(panelsMenu);
            for (IContributionItem item : fillPanelsMenu()) {
                panelsMenu.add(item);
            }
        }
        if ((getDecorator().getDecoratorFeatures() & IResultSetDecorator.FEATURE_PRESENTATIONS) != 0) {
            layoutMenu.add(new Separator());
            for (ResultSetPresentationDescriptor pd : getAvailablePresentations()) {
                Action psAction = new Action(pd.getLabel(), Action.AS_CHECK_BOX) {
                    final ResultSetPresentationDescriptor presentation;
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

        if (attr != null) {
            layoutMenu.add(new Separator());
            layoutMenu.add(new ToggleSelectionStatAction(this, ResultSetPreferences.RESULT_SET_SHOW_SEL_ROWS,
                ResultSetMessages.controls_resultset_viewer_action_show_selected_row_count));
            layoutMenu.add(new ToggleSelectionStatAction(this, ResultSetPreferences.RESULT_SET_SHOW_SEL_COLUMNS,
                ResultSetMessages.controls_resultset_viewer_action_show_selected_column_count));
            layoutMenu.add(new ToggleSelectionStatAction(this, ResultSetPreferences.RESULT_SET_SHOW_SEL_CELLS,
                ResultSetMessages.controls_resultset_viewer_action_show_selected_cell_count));
        }

        layoutMenu.add(new Separator());
        layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ZOOM_IN));
        layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ZOOM_OUT));
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
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_GO_TO_ROW));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_GO_TO_COLUMN));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_FIRST));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_PREVIOUS));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_NEXT));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_ROW_LAST));
        navigateMenu.add(new Separator());
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FETCH_PAGE));
        navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FETCH_ALL));
        if (isHasMoreData() && getDataContainer() != null && getDataContainer().isFeatureSupported(DBSDataContainer.FEATURE_DATA_COUNT)) {
            navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_COUNT));
        }
        navigateMenu.add(new Separator());
        navigateMenu.add(new ToggleRefreshOnScrollingAction(this));
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

    @NotNull
    List<DBVColorOverride> getColorOverrides(@NotNull DBDAttributeBinding binding, @Nullable Object value) {
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return Collections.emptyList();
        }
        final DBVEntity virtualEntity = DBVUtils.getVirtualEntity(dataContainer, false);
        if (virtualEntity == null) {
            return Collections.emptyList();
        }
        return virtualEntity.getColorOverrides().stream()
            .filter(override -> binding.getName().equals(override.getAttributeName()))
            .filter(override -> override.getOperator() == DBCLogicalOperator.EQUALS)
            .filter(override -> override.getOperator().evaluate(value, override.getAttributeValues()))
            .collect(Collectors.toList());
    }

    boolean hasColorOverrides() {
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return false;
        }
        final DBVEntity virtualEntity = DBVUtils.getVirtualEntity(dataContainer, false);
        if (virtualEntity == null) {
            return false;
        }
        return !virtualEntity.getColorOverrides().isEmpty();
    }

    boolean hasColumnTransformers() {
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return false;
        }
        final DBVEntity virtualEntity = DBVUtils.getVirtualEntity(dataContainer, false);
        if (virtualEntity == null) {
            return false;
        }
        if (virtualEntity.getTransformSettings() != null && virtualEntity.getTransformSettings().hasValuableData()) {
            return true;
        }
        List<DBVEntityAttribute> vAttrs = virtualEntity.getEntityAttributes();
        if (vAttrs != null) {
            for (DBVEntityAttribute vAttr : vAttrs) {
                if (vAttr.getTransformSettings() != null && vAttr.getTransformSettings().hasValuableData()) {
                    return true;
                }
            }
        }
        return false;
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
                this,
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
                    this,
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
            manager.add(new TransformerAction(this, attr, "Settings ...", IAction.AS_UNSPECIFIED, false) {
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
                manager.add(new TransformerAction(this, attr, descriptor.getName(), IAction.AS_CHECK_BOX, checked) {
                    @Override
                    public void run() {
                        getTransformSettings().enableTransformer(descriptor, !isChecked());
                        saveTransformerSettings();
                    }
                });
            }
        }
    }

    private void fillBinaryFormatMenu(@NotNull IMenuManager manager, @Nullable DBDAttributeBinding attribute) {
        if (attribute != null) {
            manager.add(new Separator());
            for (DBDBinaryFormatter formatter : DBConstants.BINARY_FORMATS) {
                manager.add(new BinaryFormatAction(this, formatter, attribute));
            }
        }
    }

    private void fillFiltersMenu(@NotNull IMenuManager filtersMenu, @Nullable DBDAttributeBinding attribute, @Nullable ResultSetRow row)
    {
        if (attribute != null && supportsDataFilter()) {
            {
                filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_MENU_DISTINCT));

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
        if (getDataContainer() instanceof DBSEntity) {
            filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_SAVE_SETTING));
        }
        filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_CLEAR_SETTING));
        filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetHandlerMain.CMD_FILTER_EDIT_SETTINGS));
    }

    private void fillOrderingsMenu(@NotNull IMenuManager filtersMenu, @Nullable DBDAttributeBinding attribute, @Nullable ResultSetRow row)
    {
        if (attribute != null) {
            filtersMenu.add(new Separator());
            filtersMenu.add(new OrderByAttributeAction(this, attribute, ColumnOrder.ASC));
            filtersMenu.add(new OrderByAttributeAction(this, attribute, ColumnOrder.DESC));
            DBDAttributeConstraint constraint = getModel().getDataFilter().getConstraint(attribute);
            if (constraint != null && constraint.getOrderPosition() > 0) {
                filtersMenu.add(new OrderByAttributeAction(this, attribute, ColumnOrder.NONE));
            }
        }
    }

    @Override
    public void navigateAssociation(
        @NotNull DBRProgressMonitor monitor,
        @NotNull ResultSetModel bindingsModel,
        @NotNull DBSEntityAssociation association,
        @NotNull List<? extends DBDValueRow> rows,
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
                DBWorkbench.getPlatformUI().showError("Cannot navigate", "Attribute " + ownAttr.getAttribute() + " is missing in result set");
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
    public void navigateReference(@NotNull DBRProgressMonitor monitor, @NotNull ResultSetModel bindingsModel, @NotNull DBSEntityAssociation association, @NotNull List<? extends DBDValueRow> rows, boolean newWindow)
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

    private void createFilterConstraint(@NotNull List<? extends DBDValueRow> rows, DBDAttributeBinding attrBinding, DBDAttributeConstraint constraint) {
        if (rows.size() == 1) {
            Object keyValue = model.getCellValue(new ResultSetCellLocation(attrBinding, (ResultSetRow) rows.get(0)));
            constraint.setOperator(DBCLogicalOperator.EQUALS);
            constraint.setValue(keyValue);
        } else {
            Object[] keyValues = new Object[rows.size()];
            for (int k = 0; k < rows.size(); k++) {
                keyValues[k] = model.getCellValue(new ResultSetCellLocation(attrBinding, (ResultSetRow) rows.get(k)));
            }
            DBCLogicalOperator[] supportedOperators =
                attrBinding.getValueHandler().getSupportedOperators(attrBinding);
            if (ArrayUtils.contains(supportedOperators, DBCLogicalOperator.IN)) {
                constraint.setOperator(DBCLogicalOperator.IN);
            } else {
                constraint.setOperator(DBCLogicalOperator.EQUALS);
            }
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
                    int result = ConfirmationDialog.confirmAction(
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
        ToolBar toolBar = panelToolBar.getControl();
        toolBar.setRedraw(false);
        IResultSetPanel visiblePanel = getVisiblePanel();
        panelToolBar.removeAll();
        if (visiblePanel != null) {
            visiblePanel.contributeActions(panelToolBar);
        }
        addDefaultPanelActions();
        panelToolBar.update(false);

        if (this.panelFolder != null) {
            Point toolBarSize = toolBar.getParent().computeSize(SWT.DEFAULT, SWT.DEFAULT);
            this.panelFolder.setTabHeight(toolBarSize.y);
        }
        toolBar.setRedraw(true);
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
        if (isDirty()) {
            int checkResult = new UITask<Integer>() {
                @Override
                protected Integer runTask() {
                    return promptToSaveOnClose();
                }
            }.execute();
            switch (checkResult) {
                case ISaveablePart2.CANCEL:
                    UIUtils.asyncExec(() -> updatePanelsContent(true));
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
            RuntimeUtils.runTask(restoreTask, "Restore data filter", 10000);
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
        if (!verifyQuerySafety() || !checkForChanges()) {
            autoRefreshControl.scheduleAutoRefresh(false);
            return false;
        }

        DataEditorFeatures.RESULT_SET_REFRESH.use();

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
                        attr.lateBinding(session, rows);
                    }
                }
            } catch (Exception e) {
                log.error("Error binding custom attributes", e);
            }
        }
        model.updateMetaData(newAttributes);
        model.updateDataFilter();

        redrawData(true, false);
        updatePanelsContent(true);

        return true;
    }


    public void readNextSegment() {
        if (!verifyQuerySafety()) {
            return;
        }
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        if (nextSegmentReadingBlocked) {
            return;
        }

        DataEditorFeatures.RESULT_SET_SCROLL.use();

        nextSegmentReadingBlocked = true;
        UIUtils.asyncExec(() -> {
            if (isRefreshInProgress() || !checkForChanges()) {
                nextSegmentReadingBlocked = false;
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
                    () -> nextSegmentReadingBlocked = false);
            }
        });
    }

    private boolean verifyQuerySafety() {
        if (container.getDataContainer() == null || !container.getDataContainer().isFeatureSupported(DBSDataContainer.FEATURE_DATA_MODIFIED_ON_REFRESH) ) {
            return true;
        }
        return UIUtils.confirmAction(null, ResultSetMessages.confirm_modifying_query_title, ResultSetMessages.confirm_modifying_query_message, DBIcon.STATUS_WARNING);
    }

    @Override
    public void readAllData() {
        if (!verifyQuerySafety()) {
            return;
        }
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        if (ConfirmationDialog.confirmAction(
            viewerPanel.getShell(),
            ConfirmationDialog.WARNING, ResultSetPreferences.CONFIRM_RS_FETCH_ALL,
            ConfirmationDialog.QUESTION
        ) != IDialogConstants.YES_ID)
        {
            return;
        }
        if (!checkForChanges()){
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
        if (rowCountLabel != null) {
            rowCountLabel.executeAction();
        }
    }

    public void setSelectionStatistics(String stats) {
        if (selectionStatLabel == null || selectionStatLabel.isDisposed()) {
            return;
        }
        if (stats.equals(selectionStatLabel.getText())) {
            return;
        }
        if (CommonUtils.isEmptyTrimmed(stats)) {
            selectionStatLabel.setText(" ");
        } else {
            selectionStatLabel.setText(stats);
        }
        statusBar.layout(true, true);
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
        result[0] = DBUtils.readRowCount(monitor, executionContext, dataContainer, model.getDataFilter(), this);
        model.setTotalRowCount(result[0]);
        return result[0];
    }

    public int getSegmentMaxRows()
    {
        if (getDataContainer() == null) {
            return 0;
        }
        int size;
        if (segmentFetchSize != null && segmentFetchSize > 0) {
            size = segmentFetchSize;
        } else {
            size = getPreferenceStore().getInt(ModelPreferences.RESULT_SET_MAX_ROWS);
        }
        if (size > 0 && size < ResultSetPreferences.MIN_SEGMENT_SIZE) {
            size = ResultSetPreferences.MIN_SEGMENT_SIZE;
        }
        return size;
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
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null || dataContainer.getDataSource() != executionContext.getDataSource()) {
            // This may happen during cross-database entity navigation
            executionContext = DBUtils.getDefaultContext(dataContainer, false);
        }
        if (executionContext == null) {
            if (finalizer != null) finalizer.run();
            UIUtils.syncExec(() ->
                UIUtils.showMessageBox(viewerPanel.getShell(), "Data read", "Can't read data - no active connection", SWT.ICON_WARNING));
            return false;
        }
        // Cancel any refresh jobs
        autoRefreshControl.cancelRefresh();

        // Read data
        Composite progressControl = viewerPanel;
        if (activePresentation.getControl() instanceof Composite) {
            progressControl = (Composite) activePresentation.getControl();
        }

        ResultSetJobDataRead dataPumpJob = new ResultSetDataPumpJob(
            dataContainer,
            new ResultSetExecutionSource(dataContainer, this, this, dataFilter),
            executionContext,
            progressControl,
            focusRow,
            saveHistory,
            scroll,
            finalizer);
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
    void queueDataPump(ResultSetJobAbstract dataPumpJob) {
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
                                ResultSetJobAbstract curJob = dataPumpJobQueue.get(0);
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

    void removeDataPump(ResultSetJobAbstract dataPumpJob) {
        synchronized (dataPumpJobQueue) {
            dataPumpJobQueue.remove(dataPumpJob);
            if (!dataPumpRunning.get()) {
                log.debug("Internal error: data read status is empty");
            }
            dataPumpRunning.set(false);
        }
    }

    void releaseDataReadLock() {
        synchronized (dataPumpJobQueue) {
            if (!dataPumpRunning.get()) {
                log.debug("Internal error: data read status is empty");
            }
            dataPumpRunning.set(false);
        }
    }

    boolean acquireDataReadLock() {
        synchronized (dataPumpJobQueue) {
            if (dataPumpRunning.get()) {
                log.debug("Internal error: multiple data reads started (" + dataPumpJobQueue + ")");
                return false;
            }
            dataPumpRunning.set(true);
        }
        return true;
    }

    public void clearData(boolean clearMetaData)
    {
        this.model.releaseAllData();
        this.model.clearData();
        this.curRow = null;
        this.selectedRecords = new int[0];
        this.activePresentation.clearMetaData();
        if (clearMetaData) {
            this.model.resetMetaData();
        }
    }

    @Override
    public boolean applyChanges(@Nullable DBRProgressMonitor monitor, @NotNull ResultSetSaveSettings settings)
    {
        DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            return false;
        }
        if (getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_CONFIRM_BEFORE_SAVE) ||
            dataSource.getContainer().getConnectionConfiguration().getConnectionType().isConfirmDataChange()
        ) {
            // Show script as confirmation
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
                //fire selection change to update selection statistics in ResultSetStatListener
                fireResultSetSelectionChange(new SelectionChangedEvent(ResultSetViewer.this, getSelection()));
                if (success && getPreferenceStore().getBoolean(ResultSetPreferences.RS_EDIT_REFRESH_AFTER_UPDATE)) {
                    // Refresh updated rows
                    try {
                        persister.refreshInsertedRows();
                    } catch (Throwable e) {
                        log.error("Error refreshing rows after update", e);
                    }
                }
                UIUtils.syncExec(() -> autoRefreshControl.scheduleAutoRefresh(!success));
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
        UIUtils.syncExec(() -> getActivePresentation().rejectChanges());

        //fire selection change to update selection statistics in ResultSetStatListener
        fireResultSetSelectionChange(new SelectionChangedEvent(ResultSetViewer.this, getSelection()));
        try {
            createDataPersister(true).rejectChanges();
            if (model.getAllRows().isEmpty()) {
                curRow = null;
                selectedRecords = new int[0];
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
    public ResultSetRow addNewRow(@NotNull RowPlacement placement, boolean copyCurrent, boolean updatePresentation) {
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            throw new IllegalStateException("Can't add/copy rows in disconnected results");
        }

        // Add new row
        // Copy cell values in new context
        try (DBCSession session = executionContext.openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, ResultSetMessages.controls_resultset_viewer_add_new_row_context_name)) {

            final DBDAttributeBinding docAttribute = model.getDocumentAttribute();
            final DBDAttributeBinding[] attributes = model.getAttributes();

            final List<ResultSetRow> selectedRows = getSelection().getSelectedRows();
            final int[][] partitionedSelectedRows;

            if (selectedRows.isEmpty()) {
                // No rows selected, use zero as the only row number
                partitionedSelectedRows = new int[][]{new int[]{0, 0}};
            } else {
                partitionedSelectedRows = groupConsecutiveRows(
                    selectedRows.stream()
                        .mapToInt(ResultSetRow::getVisualNumber)
                        .toArray()
                );
            }

            int partitionOffset = 0;

            for (final int[] partitionRange : partitionedSelectedRows) {
                final int partitionStart = partitionRange[0];
                final int partitionEnd = partitionRange[1];
                final int partitionLength = partitionEnd - partitionStart + 1;

                int srcRowIndex = partitionOffset + partitionStart;
                int newRowIndex = switch (placement) {
                    case BEFORE_SELECTION -> partitionOffset + partitionStart;
                    case AFTER_SELECTION -> partitionOffset + partitionStart + partitionLength;
                    case AT_END -> model.getRowCount();
                };

                if (newRowIndex > model.getRowCount()) {
                    // May happen if we insert "after" current row and there are no rows at all
                    newRowIndex = model.getRowCount();
                }

                for (int partitionIndex = partitionStart; partitionIndex <= partitionEnd; partitionIndex++) {
                    final Object[] cells;

                    if (docAttribute != null) {
                        cells = new Object[1];

                        if (copyCurrent && srcRowIndex >= 0 && srcRowIndex < model.getRowCount()) {
                            final Object[] origRow = model.getRowData(srcRowIndex);

                            try {
                                cells[0] = docAttribute.getValueHandler().getValueFromObject(session, docAttribute, origRow[0], true, false);
                            } catch (DBCException e) {
                                log.warn(e);
                            }
                        }
                        if (cells[0] == null) {
                            try {
                                cells[0] = docAttribute.getValueHandler().createNewValueObject(session, docAttribute.getAttribute());
                            } catch (DBCException e) {
                                log.warn(e);
                            }
                        }
                    } else {
                        cells = new Object[attributes.length];

                        if (copyCurrent && srcRowIndex >= 0 && srcRowIndex < model.getRowCount()) {
                            final Object[] origRow = model.getRowData(srcRowIndex);

                            for (int index = 0; index < attributes.length; index++) {
                                final DBDAttributeBinding metaAttr = attributes[index];

                                // Skip pseudo and autoincrement attributes
                                if (!metaAttr.isPseudoAttribute() && !metaAttr.isAutoGenerated()) {
                                    final DBSAttributeBase attribute = metaAttr.getAttribute();
                                    try {
                                        cells[index] = metaAttr.getValueHandler().getValueFromObject(session, attribute, origRow[index], true, false);
                                    } catch (DBCException e) {
                                        log.warn(e);
                                    }
                                }
                            }
                        }

                        // Fill leftover cells with null values, if needed
                        for (int index = 0; index < attributes.length; index++) {
                            final DBDAttributeBinding metaAttr = attributes[index];

                            // Skip non-null, pseudo and autoincrement attributes
                            if (cells[index] == null && !metaAttr.isPseudoAttribute() && !metaAttr.isAutoGenerated()) {
                                final DBSAttributeBase attribute = metaAttr.getAttribute();
                                try {
                                    cells[index] = metaAttr.getValueHandler().createNewValueObject(session, attribute);
                                } catch (DBCException e1) {
                                    log.warn(e1);
                                }
                            }
                        }
                    }

                    this.curRow = model.addNewRow(newRowIndex, cells);
                    this.selectedRecords = ArrayUtils.add(this.selectedRecords,
                        this.selectedRecords.length == 0 ?
                            newRowIndex :
                            this.selectedRecords[this.selectedRecords.length - 1] + 1);

                    newRowIndex++;
                    srcRowIndex++;

                    if (placement == RowPlacement.BEFORE_SELECTION) {
                        // Need to account currently inserted row
                        srcRowIndex++;
                    }
                }

                partitionOffset += partitionLength;
            }
        }
        if (updatePresentation) {
            redrawData(false, true);
            updateEditControls();

            activePresentation.scrollToRow(IResultSetPresentation.RowPosition.CURRENT);
        }

        return curRow;
    }

    @Override
    public void copyRowValues(boolean fromRowAbove, boolean updatePresentation) {
        final DBCExecutionContext context = getExecutionContext();
        if (context == null) {
            throw new IllegalStateException("Can't fill rows in disconnected results");
        }

        final DBDAttributeBinding docAttribute = model.getDocumentAttribute();
        final List<ResultSetRow> selectedRows = getSelection().getSelectedRows();
        final List<DBDAttributeBinding> selectedAttributes = getSelection().getSelectedAttributes();
        final int[][] partitionedSelectedRows = groupConsecutiveRows(
            selectedRows.stream()
                .mapToInt(ResultSetRow::getVisualNumber)
                .toArray()
        );

        try (DBCSession session = context.openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, ResultSetMessages.controls_resultset_viewer_add_new_row_context_name)) {
            for (int[] partitionRange : partitionedSelectedRows) {
                final int partitionStart = partitionRange[0];
                final int partitionEnd = partitionRange[1];
                final int sourceRowIndex;

                if (partitionStart == partitionEnd) {
                    // Single row in partition, copy values from row above/below this partition
                    sourceRowIndex = partitionStart + (fromRowAbove ? -1 : 1);
                } else {
                    // Multiple rows in partition, copy values from first/last row of this partition
                    sourceRowIndex = fromRowAbove ? partitionStart : partitionEnd;
                }

                if (sourceRowIndex < 0 || sourceRowIndex >= model.getRowCount()) {
                    break;
                }

                final ResultSetRow sourceRow = model.getRow(sourceRowIndex);

                for (int partitionIndex = partitionStart; partitionIndex <= partitionEnd; partitionIndex++) {
                    if (partitionIndex == sourceRowIndex) {
                        // We don't to override source row
                        continue;
                    }

                    final ResultSetRow targetRow = model.getRow(partitionIndex);

                    if (docAttribute != null) {
                        try {
                            final Object sourceValue = docAttribute.getValueHandler().getValueFromObject(
                                session,
                                docAttribute,
                                model.getCellValue(new ResultSetCellLocation(docAttribute, sourceRow)),
                                true,
                                false
                            );
                            final ResultSetValueController controller = new ResultSetValueController(
                                this,
                                new ResultSetCellLocation(docAttribute, targetRow, null),
                                IValueController.EditType.NONE,
                                null
                            );
                            controller.updateValue(sourceValue, false);
                        } catch (DBCException e) {
                            log.error("Can't extract document value", e);
                        }
                    } else {
                        for (final DBDAttributeBinding metaAttr : selectedAttributes) {
                            if (!metaAttr.isPseudoAttribute() && !metaAttr.isAutoGenerated()) {
                                final DBSAttributeBase attribute = metaAttr.getAttribute();
                                try {
                                    final Object sourceValue = metaAttr.getValueHandler().getValueFromObject(
                                        session,
                                        attribute,
                                        model.getCellValue(new ResultSetCellLocation(metaAttr, sourceRow)),
                                        true,
                                        false
                                    );
                                    final ResultSetValueController controller = new ResultSetValueController(
                                        this,
                                        new ResultSetCellLocation(metaAttr, targetRow),
                                        IValueController.EditType.NONE,
                                        null
                                    );
                                    controller.updateValue(sourceValue, false);
                                } catch (DBCException e) {
                                    log.error("Can't extract cell value", e);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (updatePresentation) {
            redrawData(false, true);
            updateEditControls();
        }
    }

    /**
     * Performs grouping of a continuous indexes.
     *
     * <h3>Example</h3>
     *
     * <pre>
     * {1} &rArr; {[1..1)}
     * {1, 2, 3, 4, 5, 6, 7} &rArr; {[1..7)}
     * {1, 2, 4, 6, 7, 8, 9} &rArr; {[1..2), [4..4), [6..9)}
     * </pre>
     *
     * @param indexes the indexes to group
     * @return grouped indexes
     */
    @NotNull
    private static int[][] groupConsecutiveRows(@NotNull int[] indexes) {
        final List<int[]> ranges = new ArrayList<>();
        for (int index = 1, start = 0, length = indexes.length; index <= length; index++) {
            if (index == length || indexes[index - 1] != indexes[index] - 1) {
                ranges.add(new int[]{indexes[start], indexes[index - 1]});
                start = index;
            }
        }
        return ranges.toArray(new int[0][]);
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

    public void fireResultSetSelectionChange(SelectionChangedEvent event) {
        for (IResultSetListener listener : getListenersCopy()) {
            listener.handleResultSetSelectionChange(event);
        }
    }

    private void fireResultSetModelPrepared() {
        for (IResultSetListener listener : getListenersCopy()) {
            listener.onModelPrepared();
        }
    }

    private void fireQueryExecuted(String query, StatResultSet statistics, String errorMessage) {
        for (IResultSetListener listener : getListenersCopy()) {
            listener.onQueryExecuted(query, statistics, errorMessage);
        }
    }

    private static class SimpleFilterManager implements IResultSetFilterManager {
        private final Map<String, List<String>> filterHistory = new HashMap<>();
        @NotNull
        @Override
        public List<String> getQueryFilterHistory(@Nullable DBCExecutionContext context, @NotNull String query) {
            final List<String> filters = filterHistory.get(query);
            if (filters != null) {
                return filters;
            }
            return Collections.emptyList();
        }

        @Override
        public void saveQueryFilterValue(@Nullable DBCExecutionContext context, @NotNull String query, @NotNull String filterValue) {
            List<String> filters = filterHistory.computeIfAbsent(query, k -> new ArrayList<>());
            filters.add(filterValue);
        }

        @Override
        public void deleteQueryFilterValue(@Nullable DBCExecutionContext context, @NotNull String query, String filterValue) {
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
            return items.toArray(new IContributionItem[0]);
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

    class HistoryStateItem {
        private static final int HISTORY_STATE_ITEM_MAXIMAL_LENGTH = 50;

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
            if (desc != null && desc.length() > HISTORY_STATE_ITEM_MAXIMAL_LENGTH) {
                desc = desc.substring(0, HISTORY_STATE_ITEM_MAXIMAL_LENGTH) + "...";
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

    private class ResultSetDataPumpJob extends ResultSetJobDataRead {
        private final int focusRow;
        private final boolean saveHistory;
        private final boolean scroll;
        private final Object presentationState;
        private final Runnable finalizer;

        ResultSetDataPumpJob(
            @NotNull DBSDataContainer dataContainer,
            @NotNull ResultSetExecutionSource executionSource,
            @NotNull DBCExecutionContext executionContext,
            @NotNull Composite progressControl,
            int focusRow,
            boolean saveHistory,
            boolean scroll,
            @Nullable Runnable finalizer)
        {
            super(dataContainer, executionSource, executionContext, progressControl);
            this.focusRow = focusRow;
            this.saveHistory = saveHistory;
            this.scroll = scroll;
            this.finalizer = finalizer;
            this.presentationState = savePresentationState();
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            if (!acquireDataReadLock()) {
                // Must run finalizer in any case
                if (finalizer != null) {
                    finalizer.run();
                }
                return Status.CANCEL_STATUS;
            }
            beforeDataRead();
            try {
                return super.run(monitor);
            } finally {
                afterDataRead();
                releaseDataReadLock();
            }
        }

        @Override
        public void forceDataReadCancel(Throwable error) {
            setError(error);
            afterDataRead();

            final DBSDataContainer dataContainer = executionSource.getDataContainer();
            if (dataContainer instanceof IQueryExecuteController) {
                ((IQueryExecuteController) dataContainer).forceDataReadCancel(error);
            }
        }

        private void beforeDataRead() {
            dataReceiver.setFocusRow(focusRow);
            // Set explicit target container
            dataReceiver.setTargetDataContainer(executionSource.getDataContainer());

            model.setUpdateInProgress(this);
            model.setStatistics(null);
            if (filtersPanel != null) {
                UIUtils.asyncExec(() -> {
                    filtersPanel.enableFilters(false);
                    getAutoRefresh().enableControls(false);
;               });
            }
        }

        private void afterDataRead() {
            UIUtils.syncExec(this::updateResultsInUI);
        }

        private void updateResultsInUI() {
            final Throwable error = getError();
            if (getStatistics() != null) {
                model.setStatistics(getStatistics());
            }
            try {
                final DBSDataContainer dataContainer = executionSource.getDataContainer();
                final Control control1 = getControl();
                if (control1.isDisposed()) {
                    return;
                }
                model.setUpdateInProgress(null);

                // update history. Do it first otherwise we are in the incorrect state (getDatacontainer() may return wrong value)
                if (saveHistory && error == null) {
                    setNewState(dataContainer, executionSource.getUseDataFilter());
                }

                boolean panelUpdated = false;
                final boolean metadataChanged = !scroll && model.isMetadataChanged();
                if (error != null) {
                    String errorMessage = error.getMessage();
                    setStatus(errorMessage, DBPMessageType.ERROR);

                    String sqlText;
                    SQLScriptElement query = null;
                    if (dataContainer instanceof SQLQueryContainer) {
                        query = ((SQLQueryContainer) dataContainer).getQuery();
                    }

                    if (error instanceof DBSQLException) {
                        sqlText = ((DBSQLException) error).getSqlQuery();
                    } else if (query != null) {
                        sqlText = query.getText();
                    } else {
                        sqlText = getActiveQueryText();
                    }

                    if (CommonUtils.isNotEmpty(errorMessage) && query instanceof SQLQuery) {
                        String extraErrorMessage = ((SQLQuery) query).getExtraErrorMessage();
                        if (CommonUtils.isNotEmpty(extraErrorMessage)) {
                            errorMessage = errorMessage + System.lineSeparator() + extraErrorMessage;
                        }
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
                    /*
                    We allow zero length row list for the situations when we load new an empty resultSet and the last resultSet
                    wasn't empty. Previously we didn't update the selected row count which caused a problem described in #15767
                    Now we call the ResultSetStatListener even if the resultSet is empty.
                     */
                    if (focusRow >= 0 && (focusRow < model.getRowCount() || model.getRowCount() == 0) && model.getVisibleAttributeCount() > 0) {
                        if (getCurrentRow() == null && model.getRowCount() > 0) {
                            setCurrentRow(getModel().getRow(focusRow));
                        }
                        if (getActivePresentation().getCurrentAttribute() == null || model.getRowCount() == 0) {
                            getActivePresentation().setCurrentAttribute(model.getVisibleAttribute(0));
                        }
                    }
                }
                activePresentation.updateValueView();

                if (!scroll) {
                    final DBDDataFilter dataFilter = executionSource.getDataFilter();
                    if (dataFilter != null) {
                        boolean visibilityChanged = !model.getDataFilter().equalVisibility(dataFilter);
                        model.updateDataFilter(dataFilter, true);
                        // New data filter may have different columns visibility
                        redrawData(visibilityChanged, false);
                    }
                }
                if (!panelUpdated) {
                    updatePanelsContent(true);
                }
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
                UIUtils.asyncExec(() -> {
                    if (getControl().isDisposed()) {
                        return;
                    }
                    updateFiltersText(true);
                    updateToolbar();
                });

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
        }
    }

}
