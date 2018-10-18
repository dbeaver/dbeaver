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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
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
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.autorefresh.AutoRefreshControl;
import org.jkiss.dbeaver.ui.controls.resultset.valuefilter.FilterValueEditDialog;
import org.jkiss.dbeaver.ui.controls.resultset.valuefilter.FilterValueEditPopup;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.view.StatisticsPresentation;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.ui.preferences.PrefPageDataFormat;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseGeneral;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

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
    implements DBPContextProvider, IResultSetController, ISaveablePart2, IAdaptable
{
    private static final Log log = Log.getLog(ResultSetViewer.class);

    private static final String TOOLBAR_GROUP_NAVIGATION = "navigation";
    private static final String TOOLBAR_GROUP_PRESENTATIONS = "presentations";
    private static final String TOOLBAR_GROUP_ADDITIONS = IWorkbenchActionConstants.MB_ADDITIONS;

    private static final String SETTINGS_SECTION_PRESENTATIONS = "presentations";

    private static final String TOOLBAR_CONTRIBUTION_ID = "toolbar:org.jkiss.dbeaver.ui.controls.resultset.status";

    static final String CONTROL_ID = ResultSetViewer.class.getSimpleName();

    private static final DecimalFormat ROW_COUNT_FORMAT = new DecimalFormat("###,###,###,###,###,##0");
    private static final IResultSetListener[] EMPTY_LISTENERS = new IResultSetListener[0];

    private IResultSetFilterManager filterManager;
    @NotNull
    private final IWorkbenchPartSite site;
    private final Composite viewerPanel;
    private final IResultSetDecorator decorator;
    @Nullable
    private ResultSetFilterPanel filtersPanel;
    private SashForm viewerSash;

    private CTabFolder panelFolder;
    private ToolBarManager panelToolBar;

    private final Composite presentationPanel;

    private final List<ToolBarManager> toolbarList = new ArrayList<>();
    private Composite statusBar;
    private StatusLabel statusLabel;
    private ActiveStatusMessage rowCountLabel;

    private final DynamicFindReplaceTarget findReplaceTarget;

    // Presentation
    private IResultSetPresentation activePresentation;
    private ResultSetPresentationDescriptor activePresentationDescriptor;
    private List<ResultSetPresentationDescriptor> availablePresentations;
    private ToolBar presentationSwitchToolbar;
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

    private volatile ResultSetJobDataRead dataPumpJob;

    private final ResultSetModel model = new ResultSetModel();
    private HistoryStateItem curState = null;
    private final List<HistoryStateItem> stateHistory = new ArrayList<>();
    private int historyPosition = -1;

    private AutoRefreshControl autoRefreshControl;
    private boolean actionsDisabled;

    private Color defaultBackground, defaultForeground;

    public ResultSetViewer(@NotNull Composite parent, @NotNull IWorkbenchPartSite site, @NotNull IResultSetContainer container)
    {
        super();

        this.site = site;
        this.recordMode = false;
        this.container = container;
        this.decorator = container.createResultSetDecorator();
        this.dataReceiver = new ResultSetDataReceiver(this);

        this.filterManager = Adapters.adapt(this, IResultSetFilterManager.class);
        if (this.filterManager == null) {
            this.filterManager = new SimpleFilterManager();
        }

        loadPresentationSettings();

        this.defaultBackground = EditorUtils.getDefaultTextBackground();
        this.defaultForeground = EditorUtils.getDefaultTextForeground();

        this.viewerPanel = UIUtils.createPlaceholder(parent, 1);
        this.viewerPanel.setData(CONTROL_ID, this);
        UIUtils.setHelp(this.viewerPanel, IHelpContextIds.CTX_RESULT_SET_VIEWER);
        this.viewerPanel.setRedraw(false);

        try {
            this.autoRefreshControl = new AutoRefreshControl(
                this.viewerPanel, ResultSetViewer.class.getSimpleName(), monitor -> refreshData(null));

            if ((decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_FILTERS) != 0) {
                this.filtersPanel = new ResultSetFilterPanel(this);
            }
            this.findReplaceTarget = new DynamicFindReplaceTarget();

            this.viewerSash = UIUtils.createPartDivider(site.getPart(), viewerPanel, SWT.HORIZONTAL | SWT.SMOOTH);
            this.viewerSash.setLayoutData(new GridData(GridData.FILL_BOTH));

            this.presentationPanel = UIUtils.createPlaceholder(this.viewerSash, 1);
            this.presentationPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (supportsPanels()) {
                this.panelFolder = new CTabFolder(this.viewerSash, SWT.FLAT | SWT.TOP);
                this.panelFolder.marginWidth = 0;
                this.panelFolder.marginHeight = 0;
                this.panelFolder.setMinimizeVisible(true);
                this.panelFolder.setMRUVisible(true);
                this.panelFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

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
                    if (!viewerSash.isDisposed()) {
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
                        showPanels(false, true);
                    }

                    @Override
                    public void maximize(CTabFolderEvent event) {

                    }
                });
            }

            setEmptyPresentation();

            if (supportsStatusBar()) {
                createStatusBar();
            }

            this.viewerPanel.addDisposeListener(e -> dispose());

            changeMode(false);
        } finally {
            this.viewerPanel.setRedraw(true);
        }

        updateFiltersText();
    }

    @Override
    @NotNull
    public IResultSetContainer getContainer() {
        return container;
    }

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
        return (decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_PANELS) != 0;
    }

    private boolean supportsStatusBar() {
        return (decorator.getDecoratorFeatures() & IResultSetDecorator.FEATURE_STATUS_BAR) != 0;
    }

    boolean supportsDataFilter()
    {
        DBSDataContainer dataContainer = getDataContainer();
        return dataContainer != null &&
            (dataContainer.getSupportedFeatures() & DBSDataContainer.DATA_FILTER) == DBSDataContainer.DATA_FILTER;
    }

    public void resetDataFilter(boolean refresh)
    {
        setDataFilter(model.createDataFilter(), refresh);
    }

    public void saveDataFilter()
    {
        DBCExecutionContext context = getExecutionContext();
        if (context == null) {
            log.error("Can't save data filter with null context");
            return;
        }
        DataFilterRegistry.getInstance().saveDataFilter(getDataContainer(), model.getDataFilter());
    }

    void switchFilterFocus() {
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
        return DBeaverCore.getGlobalPreferenceStore();
    }

    @NotNull
    @Override
    public Color getDefaultBackground() {
        if (filtersPanel == null) {
            return defaultBackground;
        }
        return filtersPanel.getEditControl().getBackground();
    }

    @NotNull
    @Override
    public Color getDefaultForeground() {
        if (filtersPanel == null) {
            return defaultForeground;
        }
        return filtersPanel.getEditControl().getForeground();
    }

    private void persistConfig() {
        DBCExecutionContext context = getExecutionContext();
        if (context != null) {
            context.getDataSource().getContainer().persistConfiguration();
        }
    }

    ////////////////////////////////////////
    // Presentation & panels

    List<ResultSetPresentationDescriptor> getAvailablePresentations() {
        return availablePresentations;
    }

    @Override
    @NotNull
    public IResultSetPresentation getActivePresentation() {
        return activePresentation;
    }

    @Override
    public void setEmptyPresentation() {
        setActivePresentation(new EmptyPresentation());
        activePresentationDescriptor = null;
    }

    void updatePresentation(final DBCResultSet resultSet) {
        if (getControl().isDisposed()) {
            return;
        }
        boolean changed = false;
        try {
            if (resultSet instanceof StatResultSet) {
                // Statistics - let's use special presentation for it
                availablePresentations = Collections.emptyList();
                setActivePresentation(new StatisticsPresentation());
                activePresentationDescriptor = null;
                changed = true;
            } else {
                // Regular results
                IResultSetContext context = new IResultSetContext() {
                    @Override
                    public boolean supportsAttributes() {
                        DBDAttributeBinding[] attrs = model.getAttributes();
                        return attrs.length > 0 &&
                            (attrs[0].getDataKind() != DBPDataKind.DOCUMENT || !CommonUtils.isEmpty(attrs[0].getNestedBindings()));
                    }

                    @Override
                    public boolean supportsDocument() {
                        return model.getDocumentAttribute() != null;
                    }

                    @Override
                    public String getDocumentContentType() {
                        DBDAttributeBinding docAttr = model.getDocumentAttribute();
                        return docAttr == null ? null : docAttr.getValueHandler().getValueContentType(docAttr);
                    }
                };
                final List<ResultSetPresentationDescriptor> newPresentations = ResultSetPresentationRegistry.getInstance().getAvailablePresentations(resultSet, context);
                changed = CommonUtils.isEmpty(this.availablePresentations) || !newPresentations.equals(this.availablePresentations);
                this.availablePresentations = newPresentations;
                if (!this.availablePresentations.isEmpty()) {
                    for (ResultSetPresentationDescriptor pd : this.availablePresentations) {
                        if (pd == activePresentationDescriptor) {
                            // Keep the same presentation
                            return;
                        }
                    }
                    String defaultPresentationId = getPreferenceStore().getString(DBeaverPreferences.RESULT_SET_PRESENTATION);
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
                        log.error(e);
                    }
                }
            }
        } finally {
            if (changed && presentationSwitchToolbar != null) {
                updatePresentationInToolbar();
            }
        }

    }

    private void updatePresentationInToolbar() {
        // Update combo
        viewerPanel.setRedraw(false);
        try {
            boolean pVisible = activePresentationDescriptor != null;
            ((RowData) presentationSwitchToolbar.getLayoutData()).exclude = !pVisible;
            presentationSwitchToolbar.setVisible(pVisible);
            if (!pVisible) {
                presentationSwitchToolbar.setEnabled(false);
            } else {
                presentationSwitchToolbar.setEnabled(true);
                for (ToolItem item : presentationSwitchToolbar.getItems()) item.dispose();
                for (ResultSetPresentationDescriptor pd : availablePresentations) {
                    ToolItem item = new ToolItem(presentationSwitchToolbar, SWT.CHECK);
                    item.setImage(DBeaverIcons.getImage(pd.getIcon()));
                    item.setText(pd.getLabel());
                    item.setToolTipText(pd.getDescription());
                    item.setData(pd);
                    if (pd == activePresentationDescriptor) {
                        item.setSelection(true);
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
            }
            statusBar.layout();
            viewerPanel.layout();
        } catch (Exception e) {
            log.debug("Error updating presentation toolbar", e);
        } finally {
            // Enable redraw
            viewerPanel.setRedraw(true);
        }
    }

    private void setActivePresentation(@NotNull IResultSetPresentation presentation) {
        boolean focusInPresentation = UIUtils.isParent(presentationPanel, viewerPanel.getDisplay().getFocusControl());

        // Dispose previous presentation and panels
        for (Control child : presentationPanel.getChildren()) {
            child.dispose();
        }
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
        if (activePresentationDescriptor != null) {
            availablePanels.addAll(ResultSetPresentationRegistry.getInstance().getSupportedPanels(
                    getDataSource(), activePresentationDescriptor.getId(), activePresentationDescriptor.getPresentationType()));
        } else {
            // Stats presentation
            availablePanels.addAll(ResultSetPresentationRegistry.getInstance().getSupportedPanels(
                    getDataSource(), null, IResultSetPresentation.PresentationType.COLUMNS));
        }
        activePresentation.createPresentation(this, presentationPanel);

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
            showPanels(panelsVisible, false);
            viewerSash.setOrientation(verticalLayout ? SWT.VERTICAL : SWT.HORIZONTAL);
            viewerSash.setWeights(panelWeights);
        }

        presentationPanel.layout();

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
            ((ISelectionProvider) presentation).addSelectionChangedListener(event -> fireResultSetSelectionChange(event));
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

    /**
     * Switch to the next presentation
     */
    void switchPresentation() {
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

    private void switchPresentation(ResultSetPresentationDescriptor selectedPresentation) {
        try {
            IResultSetPresentation instance = selectedPresentation.createInstance();
            activePresentationDescriptor = selectedPresentation;
            setActivePresentation(instance);
            instance.refreshData(true, false, false);

            for (ToolItem item : presentationSwitchToolbar.getItems()) {
                item.setSelection(item.getData() == activePresentationDescriptor);
            }
            // Save in global preferences
            DBeaverCore.getGlobalPreferenceStore().setValue(DBeaverPreferences.RESULT_SET_PRESENTATION, activePresentationDescriptor.getId());
            savePresentationSettings();
        } catch (Throwable e1) {
            DBUserInterface.getInstance().showError(
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

    public IResultSetPanel getVisiblePanel() {
        return isPanelsVisible() ? activePanels.get(getPresentationSettings().activePanelId) : null;
    }

    @Override
    public IResultSetPanel[] getActivePanels() {
        return activePanels.values().toArray(new IResultSetPanel[activePanels.size()]);
    }

    @Override
    public boolean activatePanel(String id, boolean setActive, boolean showPanels) {
        if (!supportsPanels()) {
            return false;
        }
        if (showPanels && !isPanelsVisible()) {
            showPanels(true, false);
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
            DBUserInterface.getInstance().showError("Can't show panel", "Can't create panel '" + id + "'", e);
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
            showPanels(false, true);
        }
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

    boolean isPanelsVisible() {
        return viewerSash != null && viewerSash.getMaximizedControl() == null;
    }

    void showPanels(boolean show, boolean saveSettings) {
        if (!supportsPanels() || show == isPanelsVisible()) {
            return;
        }
        CTabItem activePanelTab = panelFolder.getSelection();

        if (!show) {
            viewerSash.setMaximizedControl(presentationPanel);
            if (activePanelTab != null && !activePanelTab.getControl().isDisposed() && UIUtils.hasFocus(activePanelTab.getControl())) {
                // Set focus to presentation
                activePresentation.getControl().setFocus();
            }
        } else {
            activateDefaultPanels(getPresentationSettings());
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
    }

    void toggleVerticalLayout() {
        PresentationSettings settings = getPresentationSettings();
        settings.verticalLayout = !settings.verticalLayout;
        viewerSash.setOrientation(settings.verticalLayout ? SWT.VERTICAL : SWT.HORIZONTAL);
        savePresentationSettings();
    }

    private List<IContributionItem> fillPanelsMenu() {
        List<IContributionItem> items = new ArrayList<>();

        for (final ResultSetPanelDescriptor panel : availablePanels) {
            items.add(new ActionContributionItem(new PanelToggleAction(panel)));
        }
        return items;
    }

    private void addDefaultPanelActions() {
        panelToolBar.add(new Action("View Menu", ImageDescriptor.createFromImageData(DBeaverIcons.getViewMenuImage().getImageData())) {
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
                        return;
                    }
                }
            }
        });
    }

    ////////////////////////////////////////
    // Actions

    boolean isActionsDisabled() {
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
        this.updateStatusMessage();
    }

    private void createStatusBar()
    {
        UIUtils.createHorizontalLine(viewerPanel);

        statusBar = new Composite(viewerPanel, SWT.NONE);
        statusBar.setBackgroundMode(SWT.INHERIT_FORCE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        statusBar.setLayoutData(gd);
        RowLayout toolbarsLayout = new RowLayout(SWT.HORIZONTAL);
        toolbarsLayout.marginTop = 0;
        toolbarsLayout.marginBottom = 0;
        toolbarsLayout.center = true;
        toolbarsLayout.wrap = true;
        toolbarsLayout.pack = true;
        //toolbarsLayout.fill = true;
        statusBar.setLayout(toolbarsLayout);

        {
            ToolBarManager editToolbar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);

            // handle own commands
            editToolbar.add(new Separator());
            editToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_APPLY_CHANGES, "Save", null, null, true));
            editToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_REJECT_CHANGES, "Cancel", null, null, true));
            editToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_GENERATE_SCRIPT, "Script", null, null, true));
            editToolbar.add(new Separator());
            editToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT));
            editToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_ADD));
            editToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_COPY));
            editToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_DELETE));

            editToolbar.createControl(statusBar);
            toolbarList.add(editToolbar);
        }
        {
            ToolBarManager navToolbar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            navToolbar.add(new Separator());
            navToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_FIRST));
            navToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_PREVIOUS));
            navToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_NEXT));
            navToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_LAST));
            navToolbar.add(new Separator());
            navToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_PAGE));
            navToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_ALL));
            navToolbar.createControl(statusBar);
            navToolbar.add(new Separator(TOOLBAR_GROUP_NAVIGATION));
            toolbarList.add(navToolbar);
        }
        {
            ToolBarManager configToolbar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            configToolbar.add(new Separator());
            {
                //configToolbar.add(new ToggleModeAction());
                ActionContributionItem item = new ActionContributionItem(new ToggleModeAction());
                item.setMode(ActionContributionItem.MODE_FORCE_TEXT);
                configToolbar.add(item);
            }

            {
                CommandContributionItemParameter ciParam = new CommandContributionItemParameter(
                    site,
                    "org.jkiss.dbeaver.core.resultset.panels",
                    ResultSetCommandHandler.CMD_TOGGLE_PANELS,
                    CommandContributionItem.STYLE_PULLDOWN);
                ciParam.label = "Panels";
                ciParam.mode = CommandContributionItem.MODE_FORCE_TEXT;
                configToolbar.add(new CommandContributionItem(ciParam));
            }
            configToolbar.add(new Separator());
            configToolbar.add(new ConfigAction());
            configToolbar.add(new Separator());
            configToolbar.createControl(statusBar);
            toolbarList.add(configToolbar);
        }

        {
            presentationSwitchToolbar = new ToolBar(statusBar, SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            RowData rd = new RowData();
            rd.exclude = true;
            presentationSwitchToolbar.setLayoutData(rd);
        }

        {
            ToolBarManager addToolbar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
            addToolbar.add(new GroupMarker(TOOLBAR_GROUP_PRESENTATIONS));
            addToolbar.add(new GroupMarker(TOOLBAR_GROUP_ADDITIONS));
            final IMenuService menuService = getSite().getService(IMenuService.class);
            if (menuService != null) {
                menuService.populateContributionManager(addToolbar, TOOLBAR_CONTRIBUTION_ID);
            }
            addToolbar.update(true);
            addToolbar.createControl(statusBar);
            toolbarList.add(addToolbar);
        }

        {
            final int fontHeight = UIUtils.getFontHeight(statusBar);
            statusLabel = new StatusLabel(statusBar, SWT.NONE, this);
            statusLabel.setLayoutData(new RowData(40 * fontHeight, SWT.DEFAULT));

            rowCountLabel = new ActiveStatusMessage(statusBar, DBeaverIcons.getImage(UIIcon.RS_REFRESH), CoreMessages.controls_resultset_viewer_calculate_row_count, this) {
                @Override
                protected boolean isActionEnabled() {
                    return hasData();
                }

                @Override
                protected ILoadService<String> createLoadService() {
                    return new DatabaseLoadService<String>("Load row count", getExecutionContext()) {
                        @Override
                        public String evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
            rowCountLabel.setLayoutData(new RowData(10 * fontHeight, SWT.DEFAULT));
            rowCountLabel.setMessage("Row Count");
        }
    }

    @Nullable
    public DBSDataContainer getDataContainer()
    {
        return curState != null ? curState.dataContainer : container.getDataContainer();
    }

    ////////////////////////////////////////////////////////////
    // Grid/Record mode

    @Override
    public boolean isRecordMode() {
        return recordMode;
    }

    void toggleMode()
    {
        changeMode(!recordMode);

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
    }

    public boolean isAttributeReadOnly(DBDAttributeBinding attribute)
    {
        if (isReadOnly()) {
            return true;
        }
        if (!model.isAttributeReadOnly(attribute)) {
            return false;
        }
        boolean newRow = (curRow != null && curRow.getState() == ResultSetRow.STATE_ADDED);
        return !newRow;
    }

    private Object savePresentationState() {
        if (activePresentation instanceof IStatefulControl) {
            return ((IStatefulControl) activePresentation).saveState();
        } else {
            return null;
        }
    }

    private void restorePresentationState(Object state) {
        if (activePresentation instanceof IStatefulControl) {
            ((IStatefulControl) activePresentation).restoreState(state);
        }
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
    }

    public void updateStatusMessage()
    {
        String statusMessage;
        if (model.getRowCount() == 0) {
            if (model.getVisibleAttributeCount() == 0) {
                statusMessage = CoreMessages.controls_resultset_viewer_status_empty + getExecutionTimeMessage();
            } else {
                statusMessage = CoreMessages.controls_resultset_viewer_status_no_data + getExecutionTimeMessage();
            }
        } else {
            if (recordMode) {
                statusMessage =
                    CoreMessages.controls_resultset_viewer_status_row + (curRow == null ? 0 : curRow.getVisualNumber() + 1) +
                        "/" + model.getRowCount() +
                    (curRow == null ? getExecutionTimeMessage() : "");
            } else {
                statusMessage =
                    String.valueOf(model.getRowCount()) +
                    CoreMessages.controls_resultset_viewer_status_rows_fetched + getExecutionTimeMessage();
            }
        }
        boolean hasWarnings = !dataReceiver.getErrorList().isEmpty();
        if (hasWarnings) {
            statusMessage += " - " + dataReceiver.getErrorList().size() + " warning(s)";
        }
        if (getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_SHOW_CONNECTION_NAME)) {
            DBSDataContainer dataContainer = getDataContainer();
            if (dataContainer != null) {
                DBPDataSource dataSource = dataContainer.getDataSource();
                if (dataSource != null) {
                    statusMessage += " [" + dataSource.getContainer().getName() + "]";
                }
            }
        }
        setStatus(statusMessage, hasWarnings ? DBPMessageType.WARNING : DBPMessageType.INFORMATION);

        if (rowCountLabel != null) {
            // Update row count label
            if (!hasData()) {
                rowCountLabel.setMessage("No Data");
            } else if (!isHasMoreData()) {
                rowCountLabel.setMessage(ROW_COUNT_FORMAT.format(model.getRowCount()));
            } else {
                if (model.getTotalRowCount() == null) {
                    rowCountLabel.setMessage(ROW_COUNT_FORMAT.format(model.getRowCount()) + "+");
                } else {
                    // We know actual row count
                    rowCountLabel.setMessage(ROW_COUNT_FORMAT.format(model.getTotalRowCount()));
                }
            }
            rowCountLabel.updateActionState();
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
                if (ConfirmationDialog.showConfirmDialogEx(
                    viewerPanel.getShell(),
                    DBeaverPreferences.CONFIRM_ORDER_RESULTSET,
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

            if (model.isMetadataChanged() && getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE)) {
                boolean newRecordMode = (rows.size() == 1);
                if (newRecordMode != recordMode) {
                    toggleMode();
                }
            }
        }
    }

    void appendData(List<Object[]> rows)
    {
        model.appendData(rows);
        activePresentation.refreshData(false, true, true);

        setStatus(NLS.bind(CoreMessages.controls_resultset_viewer_status_rows_size, model.getRowCount(), rows.size()) + getExecutionTimeMessage());

        updateEditControls();
    }

    @Override
    public int promptToSaveOnClose()
    {
        if (!isDirty()) {
            return ISaveablePart2.YES;
        }
        int result = ConfirmationDialog.showConfirmDialog(
            viewerPanel.getShell(),
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

    public void doSave(DBRProgressMonitor monitor)
    {
        applyChanges(monitor);
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
    public boolean hasData()
    {
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
            executionContext.getDataSource().getContainer().isConnectionReadOnly() ||
            executionContext.getDataSource().getInfo().isReadOnlyData();
    }

    /**
     * Checks that current state of result set allows to insert new rows
     * @return true if new rows insert is allowed
     */
    boolean isInsertable()
    {
        return
            !isReadOnly() &&
            model.isSingleSource() &&
            model.getVisibleAttributeCount() > 0;
    }

    public boolean isRefreshInProgress() {
        return dataPumpJob != null;
    }

    public DataSourceJob getDataReadJob() {
        return dataPumpJob;
    }
///////////////////////////////////////////////////////
    // Context menu & filters

    @NotNull
    IResultSetFilterManager getFilterManager() {
        return filterManager;
    }

    void showFiltersMenu() {
        DBDAttributeBinding curAttribute = getActivePresentation().getCurrentAttribute();
        if (curAttribute == null) {
            return;
        }
        MenuManager menuManager = new MenuManager();
        fillFiltersMenu(curAttribute, menuManager);
        showContextMenuAtCursor(menuManager);
    }

    @Override
    public void showDistinctFilter(DBDAttributeBinding curAttribute) {
        showFiltersDistinctMenu(curAttribute, false);
    }

    void showFiltersDistinctMenu(DBDAttributeBinding curAttribute, boolean atKeyboardCursor) {
        Collection<ResultSetRow> selectedRows = getSelection().getSelectedRows();
        ResultSetRow[] rows = selectedRows.toArray(new ResultSetRow[selectedRows.size()]);

        FilterValueEditPopup menu = new FilterValueEditPopup(getSite().getShell(), ResultSetViewer.this, curAttribute, rows);

        Point location;
        if (atKeyboardCursor) {
            location = getKeyboardCursorLocation();
        } else {
            location = getSite().getWorkbenchWindow().getWorkbench().getDisplay().getCursorLocation();
        }
        if (location != null) {
            menu.setLocation(location);
        }

        if (menu.open() == IDialogConstants.OK_ID) {
            Object value = menu.getValue();

            DBDDataFilter filter = new DBDDataFilter(model.getDataFilter());
            DBDAttributeConstraint constraint = filter.getConstraint(curAttribute);
            if (constraint != null) {
                constraint.setOperator(DBCLogicalOperator.EQUALS);
                constraint.setValue(value);
                setDataFilter(filter, true);
            }
        }
    }

    void showReferencesMenu(boolean openInNewWindow) {
        ResultSetRow currentRow = getCurrentRow();
        if (currentRow == null || currentRow.getRowNumber() < 0) {
            return;
        }
        MenuManager menuManager = createRefTablesMenu(currentRow, openInNewWindow);
        if (menuManager != null) {
            showContextMenuAtCursor(menuManager);
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

    @Override
    public void fillContextMenu(@NotNull IMenuManager manager, @Nullable final DBDAttributeBinding attr, @Nullable final ResultSetRow row)
    {
        final DBPDataSource dataSource = getDataSource();

        // Custom oldValue items
        final ResultSetValueController valueController;
        final Object value;
        if (attr != null && row != null) {
            valueController = new ResultSetValueController(
                this,
                attr,
                row,
                IValueController.EditType.NONE,
                null);
            value = valueController.getValue();
        } else {
            valueController = null;
            value = null;
        }

        {
            {
                // Standard items
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_CUT));
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_COPY));

                MenuManager extCopyMenu = new MenuManager(ActionUtils.findCommandName(ResultSetCopySpecialHandler.CMD_COPY_SPECIAL));
                extCopyMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCopySpecialHandler.CMD_COPY_SPECIAL));
                extCopyMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCopySpecialHandler.CMD_COPY_COLUMN_NAMES));
                if (row != null) {
                    extCopyMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_COPY_ROW_NAMES));
                }
                manager.add(extCopyMenu);
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_PASTE));
                manager.add(ActionUtils.makeCommandContribution(site, CoreCommands.CMD_PASTE_SPECIAL));
                manager.add(new Separator());

                if (valueController != null) {

                    {
                        MenuManager editMenu = new MenuManager(
                            CoreMessages.actions_menu_edit,
                            DBeaverIcons.getImageDescriptor(UIIcon.ROW_EDIT),
                            MENU_ID_EDIT); //$NON-NLS-1$

                        // Edit items
                        editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT));
                        editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT_INLINE));
                        if (!valueController.isReadOnly() && !DBUtils.isNullValue(value)/* && !attr.isRequired()*/) {
                            editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_CELL_SET_NULL));
                        }
                        if (row.getState() == ResultSetRow.STATE_REMOVED || (row.changes != null && row.changes.containsKey(attr))) {
                            editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_CELL_RESET));
                        }

                        // Menus from value handler
                        try {
                            valueController.getValueManager().contributeActions(editMenu, valueController, null);
                        } catch (Exception e) {
                            log.error(e);
                        }

                        editMenu.add(new Separator());
                        editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_ADD));
                        editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_COPY));
                        editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_DELETE));

                        manager.add(editMenu);
                    }
                }
            }
        }
        manager.add(new GroupMarker(MENU_GROUP_EDIT));

        // Filters and View
        {
            MenuManager filtersMenu = new MenuManager(
                CoreMessages.controls_resultset_viewer_action_order_filter,
                DBeaverIcons.getImageDescriptor(UIIcon.FILTER),
                MENU_ID_FILTERS); //$NON-NLS-1$
            filtersMenu.setActionDefinitionId(ResultSetCommandHandler.CMD_FILTER_MENU);
            filtersMenu.setRemoveAllWhenShown(true);
            filtersMenu.addMenuListener(manager1 -> fillFiltersMenu(attr, manager1));
            manager.add(filtersMenu);
        }
        if (dataSource != null && attr != null && model.getVisibleAttributeCount() > 0 && !model.isUpdateInProgress()) {
            {
                MenuManager viewMenu = new MenuManager(
                    CoreMessages.controls_resultset_viewer_action_view_format,
                    null,
                    MENU_ID_VIEW); //$NON-NLS-1$

                List<? extends DBDAttributeTransformerDescriptor> transformers =
                    dataSource.getContainer().getPlatform().getValueHandlerRegistry().findTransformers(
                        dataSource, attr, null);
                if (!CommonUtils.isEmpty(transformers)) {
                    MenuManager transformersMenu = new MenuManager(CoreMessages.controls_resultset_viewer_action_view_as);
                    transformersMenu.setRemoveAllWhenShown(true);
                    transformersMenu.addMenuListener(manager12 -> fillAttributeTransformersMenu(manager12, attr));
                    viewMenu.add(transformersMenu);
                } else {
                    final Action customizeAction = new Action(CoreMessages.controls_resultset_viewer_action_view_as) {};
                    customizeAction.setEnabled(false);
                    viewMenu.add(customizeAction);
                }
                viewMenu.add(new TransformComplexTypesToggleAction());
                viewMenu.add(new ColorizeDataTypesToggleAction());
                if (getModel().isSingleSource()) {
                    if (valueController != null) {
                        viewMenu.add(new SetRowColorAction(attr, valueController.getValue()));
                        if (getModel().hasColorMapping(attr)) {
                            viewMenu.add(new ResetRowColorAction(attr, valueController.getValue()));
                        }
                    }
                    viewMenu.add(new CustomizeColorsAction(attr, row));
                    if (getModel().getSingleSource() != null && getModel().hasColorMapping(getModel().getSingleSource())) {
                        viewMenu.add(new ResetAllColorAction());
                    }
                    viewMenu.add(new Separator());
                }
                viewMenu.add(new Action(CoreMessages.controls_resultset_viewer_action_data_formats) {
                    @Override
                    public void run() {
                        UIUtils.showPreferencesFor(
                            getControl().getShell(),
                            ResultSetViewer.this,
                            PrefPageDataFormat.PAGE_ID);
                    }
                });

                manager.add(viewMenu);
            }

            {
                // Navigate
                MenuManager navigateMenu = new MenuManager(
                    CoreMessages.controls_resultset_viewer_action_navigate,
                    null,
                    "navigate"); //$NON-NLS-1$
                boolean hasNavTables = false;
                if (ActionUtils.isCommandEnabled(ResultSetCommandHandler.CMD_NAVIGATE_LINK, site)) {
                    // Foreign key to some external table
                    navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_NAVIGATE_LINK));
                    hasNavTables = true;
                }
                if (model.isSingleSource()) {
                    // Add menu for referencing tables
                    MenuManager refTablesMenu = createRefTablesMenu(row, false);
                    if (refTablesMenu != null) {
                        navigateMenu.add(refTablesMenu);
                        hasNavTables = true;
                    }
                }
                if (hasNavTables) {
                    navigateMenu.add(new Separator());
                }

                navigateMenu.add(new Separator());
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FOCUS_FILTER));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ITextEditorActionDefinitionIds.LINE_GOTO));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_FIRST));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_NEXT));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_PREVIOUS));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_LAST));
                navigateMenu.add(new Separator());
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_PAGE));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_ALL));
                if (isHasMoreData() && getDataContainer() != null &&  (getDataContainer().getSupportedFeatures() & DBSDataContainer.DATA_COUNT) != 0) {
                    navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_COUNT));
                }
                navigateMenu.add(new Separator());
                navigateMenu.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.NAVIGATE_BACKWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_BACK));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.NAVIGATE_FORWARD_HISTORY, CommandContributionItem.STYLE_PUSH, UIIcon.RS_FORWARD));

                manager.add(navigateMenu);
            }
        }

        {
            // Layout
            MenuManager layoutMenu = new MenuManager(
                CoreMessages.controls_resultset_viewer_action_layout,
                null,
                "layout"); //$NON-NLS-1$
            layoutMenu.add(new ToggleModeAction());
            layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_PANELS));
            layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_LAYOUT));
            layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_SWITCH_PRESENTATION));
            {
                MenuManager panelsMenu = new MenuManager(
                    CoreMessages.controls_resultset_viewer_action_panels,
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

/*
                {
                    MenuManager toolBarsMenu = new MenuManager(
                        "Toolbars",
                        null,
                        "result_toolbars"); //$NON-NLS-1$
                    layoutMenu.add(toolBarsMenu);
                    toolBarsMenu.add(new ToolbarToggleAction("sample", "Sample"));
                }
*/
            manager.add(layoutMenu);
            manager.add(new Separator());
        }

        // Fill general menu
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null && model.hasData()) {
            manager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_EXPORT));
        }
        manager.add(new GroupMarker("results_export"));
        manager.add(new GroupMarker(CoreCommands.GROUP_TOOLS));
        if (dataContainer != null && model.hasData()) {
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_REFRESH));
        }

        manager.add(new Separator());
        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

        decorator.fillContributions(manager);
    }

    @Nullable
    private MenuManager createRefTablesMenu(ResultSetRow row, boolean openInNewWindow) {
        DBSEntity singleSource = model.getSingleSource();
        if (singleSource == null) {
            return null;
        }
        String menuName = ActionUtils.findCommandName(ResultSetCommandHandler.CMD_REFERENCES_MENU);

        MenuManager refTablesMenu = new MenuManager(menuName, null, "ref-tables");
        refTablesMenu.setActionDefinitionId(ResultSetCommandHandler.CMD_REFERENCES_MENU);
        refTablesMenu.add(ResultSetReferenceMenu.NOREFS_ACTION);
        refTablesMenu.addMenuListener(manager -> ResultSetReferenceMenu.fillRefTablesActions(this, row, singleSource, manager, openInNewWindow));

        return refTablesMenu;
    }


    @Nullable
    private DBPDataSource getDataSource() {
        return getDataContainer() == null ? null : getDataContainer().getDataSource();
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

    private void fillAttributeTransformersMenu(IMenuManager manager, final DBDAttributeBinding attr) {
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return;
        }
        final DBPDataSource dataSource = dataContainer.getDataSource();
        final DBDRegistry registry = dataSource.getContainer().getPlatform().getValueHandlerRegistry();
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
                "Default",
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
                        if (isChecked()) {
                            final DBVTransformSettings settings = getTransformSettings();
                            final String oldCustomTransformer = settings.getCustomTransformer();
                            settings.setCustomTransformer(descriptor.getId());
                            TransformerSettingsDialog settingsDialog = new TransformerSettingsDialog(
                                ResultSetViewer.this, attr, settings);
                            if (settingsDialog.open() == IDialogConstants.OK_ID) {
                                // If there are no options - save settings without opening dialog
                                saveTransformerSettings();
                            } else {
                                settings.setCustomTransformer(oldCustomTransformer);
                            }
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
                        ResultSetViewer.this, attr, transformSettings);
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

    private void fillFiltersMenu(@Nullable DBDAttributeBinding attribute, @NotNull IMenuManager filtersMenu)
    {
        if (attribute != null && supportsDataFilter()) {
            filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FILTER_MENU_DISTINCT));

            //filtersMenu.add(new FilterByListAction(operator, type, attribute));
            DBCLogicalOperator[] operators = attribute.getValueHandler().getSupportedOperators(attribute);
            // Operators with multiple inputs
            for (DBCLogicalOperator operator : operators) {
                if (operator.getArgumentCount() < 0) {
                    filtersMenu.add(new FilterByAttributeAction(operator, FilterByAttributeType.INPUT, attribute));
                }
            }
            filtersMenu.add(new Separator());
            // Operators with no inputs
            for (DBCLogicalOperator operator : operators) {
                if (operator.getArgumentCount() == 0) {
                    filtersMenu.add(new FilterByAttributeAction(operator, FilterByAttributeType.NONE, attribute));
                }
            }
            // Operators with single input
            for (FilterByAttributeType type : FilterByAttributeType.values()) {
                if (type == FilterByAttributeType.NONE) {
                    // Value filters are available only if certain cell is selected
                    continue;
                }
                filtersMenu.add(new Separator());
                if (type.getValue(this, attribute, DBCLogicalOperator.EQUALS, true) == null) {
                    // Null cell value - no operators can be applied
                    continue;
                }
                for (DBCLogicalOperator operator : operators) {
                    if (operator.getArgumentCount() > 0) {
                        filtersMenu.add(new FilterByAttributeAction(operator, type, attribute));
                    }
                }
            }
            filtersMenu.add(new Separator());
            DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(attribute);
            if (constraint != null && constraint.hasCondition()) {
                filtersMenu.add(new FilterResetAttributeAction(attribute));
            }
        }
        if (attribute != null) {
            filtersMenu.add(new Separator());
            filtersMenu.add(new OrderByAttributeAction(attribute, true));
            filtersMenu.add(new OrderByAttributeAction(attribute, false));
            filtersMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_ORDER));
        }
        filtersMenu.add(new Separator());
        filtersMenu.add(new ToggleServerSideOrderingAction());
        filtersMenu.add(new ShowFiltersAction(true));
    }

    @Override
    public void navigateAssociation(@NotNull DBRProgressMonitor monitor, @Nullable DBSEntityAssociation association, @Nullable DBDAttributeBinding attr, @NotNull ResultSetRow row, boolean newWindow)
        throws DBException
    {
        if (!confirmProceed()) {
            return;
        }
        if (!newWindow && !confirmPanelsReset()) {
            return;
        }

        if (getExecutionContext() == null) {
            throw new DBException("Not connected");
        }
        if (association == null) {
            List<DBSEntityReferrer> referrers = attr.getReferrers();
            if (referrers != null) {
                for (final DBSEntityReferrer referrer : referrers) {
                    if (referrer instanceof DBSEntityAssociation) {
                        association = (DBSEntityAssociation) referrer;
                        break;
                    }
                }
            }
            if (association == null) {
                throw new DBException("Association not found in attribute [" + attr.getName() + "]");
            }
        }

        DBSEntityConstraint refConstraint = association.getReferencedConstraint();
        if (refConstraint == null) {
            throw new DBException("Broken association (referenced constraint missing)");
        }
        if (!(refConstraint instanceof DBSEntityReferrer)) {
            throw new DBException("Referenced constraint [" + refConstraint + "] is not a referrer");
        }
        DBSEntity targetEntity = refConstraint.getParentObject();
        if (targetEntity == null) {
            throw new DBException("Null constraint parent");
        }
        if (!(targetEntity instanceof DBSDataContainer)) {
            throw new DBException("Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] is not a data container");
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<>();
        int visualPosition = 0;
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
            DBDAttributeBinding ownBinding = model.getAttributeBinding(ownAttr.getAttribute());
            if (ownBinding == null) {
                DBUserInterface.getInstance().showError("Can't navigate", "Attribute " + ownAttr.getAttribute() + " is missing in result set");
                return;
            }

            DBDAttributeConstraint constraint = new DBDAttributeConstraint(refAttr.getAttribute(), visualPosition++);
            constraint.setVisible(true);
            constraints.add(constraint);

            Object keyValue = model.getCellValue(ownBinding, row);
            constraint.setOperator(DBCLogicalOperator.EQUALS);
            constraint.setValue(keyValue);
        }
        // Save cur data filter in state
        curState.filter = new DBDDataFilter(model.getDataFilter());
        navigateEntity(monitor, newWindow, targetEntity, constraints);
    }

    @Override
    public void navigateReference(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAssociation association, @NotNull ResultSetRow row, boolean newWindow)
        throws DBException
    {
        if (!confirmProceed()) {
            return;
        }

        if (getExecutionContext() == null) {
            throw new DBException("Not connected");
        }

        DBSEntity targetEntity = association.getParentObject();
        if (targetEntity == null) {
            throw new DBException("Null constraint parent");
        }
        if (!(targetEntity instanceof DBSDataContainer)) {
            throw new DBException("Referencing entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] is not a data container");
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<>();
        int visualPosition = 0;
        // Set conditions
        DBSEntityConstraint refConstraint = association.getReferencedConstraint();
        List<? extends DBSEntityAttributeRef> ownAttrs = CommonUtils.safeList(((DBSEntityReferrer) association).getAttributeReferences(monitor));
        List<? extends DBSEntityAttributeRef> refAttrs = CommonUtils.safeList(((DBSEntityReferrer) refConstraint).getAttributeReferences(monitor));
        if (ownAttrs.size() != refAttrs.size()) {
            throw new DBException(
                "Entity [" + DBUtils.getObjectFullName(targetEntity, DBPEvaluationContext.UI) + "] association [" + association.getName() +
                    "] columns differs from referenced constraint [" + refConstraint.getName() + "] (" + ownAttrs.size() + "<>" + refAttrs.size() + ")");
        }
        // Add association constraints
        for (int i = 0; i < refAttrs.size(); i++) {
            DBSEntityAttributeRef refAttr = refAttrs.get(i);

            DBDAttributeBinding attrBinding = model.getAttributeBinding(refAttr.getAttribute());
            if (attrBinding == null) {
                log.error("Can't find attribute binding for ref attribute '" + refAttr.getAttribute().getName() + "'");
            } else {
                // Constrain use corresponding own attr
                DBSEntityAttributeRef ownAttr = ownAttrs.get(i);
                DBDAttributeConstraint constraint = new DBDAttributeConstraint(ownAttr.getAttribute(), visualPosition++);
                constraint.setVisible(true);
                constraints.add(constraint);

                Object keyValue = model.getCellValue(attrBinding, row);
                constraint.setOperator(DBCLogicalOperator.EQUALS);
                constraint.setValue(keyValue);
            }
        }
        navigateEntity(monitor, newWindow, targetEntity, constraints);
    }

    private void navigateEntity(@NotNull DBRProgressMonitor monitor, boolean newWindow, DBSEntity targetEntity, List<DBDAttributeConstraint> constraints) {
        DBDDataFilter newFilter = new DBDDataFilter(constraints);

        if (newWindow) {
            openResultsInNewWindow(monitor, targetEntity, newFilter);
        } else {
            DBSDataContainer targetDataContainer = (DBSDataContainer) targetEntity;
            // Workaround for script results
            // In script mode history state isn't updated so we check for it here
            if (curState == null) {
                setNewState(targetDataContainer, model.getDataFilter());
            }
            runDataPump(targetDataContainer, newFilter, 0, getSegmentMaxRows(), -1, true, false, null);
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
                        viewerPanel.getShell(),
                        DBeaverPreferences.CONFIRM_RS_PANEL_RESET,
                        ConfirmationDialog.CONFIRM);
                    if (result == IDialogConstants.CANCEL_ID) {
                        return false;
                    }
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

        runDataPump(state.dataContainer, state.filter, 0, segmentSize, state.rowNumber, true, false, null);
    }

    @Override
    public void updatePanelsContent(boolean forceRefresh) {
        updateEditControls();
        for (IResultSetPanel panel : getActivePanels()) {
            panel.refresh(forceRefresh);
        }
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
                    applyChanges(null, success -> {
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
        DBDDataFilter dataFilter = restoreDataFilter(dataContainer);

        if (container.isReadyToRun() && dataContainer != null && dataPumpJob == null) {
            int segmentSize = getSegmentMaxRows();
            Runnable finalizer = () -> {
                if (activePresentation.getControl() != null && !activePresentation.getControl().isDisposed()) {
                    activePresentation.formatData(true);
                }
            };

            dataReceiver.setNextSegmentRead(false);
            runDataPump(dataContainer, dataFilter, 0, segmentSize, -1, true, false, finalizer);
        } else {
            DBUserInterface.getInstance().showError(
                    "Error executing query",
                dataContainer == null ?
                    "Viewer detached from data source" :
                    dataPumpJob == null ?
                        "Can't refresh after reconnect. Re-execute query." :
                        "Previous query is still running");
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
                null);
        }
    }

    @Override
    public boolean refreshData(@Nullable Runnable onSuccess) {
        if (!checkForChanges()) {
            return false;
        }

        DBSDataContainer dataContainer = getDataContainer();
        if (container.isReadyToRun() && dataContainer != null && dataPumpJob == null) {
            int segmentSize = getSegmentMaxRows();
            if (curRow != null && curRow.getVisualNumber() >= segmentSize && segmentSize > 0) {
                segmentSize = (curRow.getVisualNumber() / segmentSize + 1) * segmentSize;
            }
            dataReceiver.setNextSegmentRead(false);
            return runDataPump(dataContainer, null, 0, segmentSize, curRow == null ? 0 : curRow.getRowNumber(), false, false, onSuccess);
        } else {
            return false;
        }
    }

    public synchronized void readNextSegment()
    {
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null && !model.isUpdateInProgress() && dataPumpJob == null) {
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
                null);
        }
    }

    @Override
    public void readAllData() {
        if (!dataReceiver.isHasMoreData()) {
            return;
        }
        if (ConfirmationDialog.showConfirmDialogEx(
            viewerPanel.getShell(),
            DBeaverPreferences.CONFIRM_RS_FETCH_ALL,
            ConfirmationDialog.QUESTION,
            ConfirmationDialog.WARNING) != IDialogConstants.YES_ID)
        {
            return;
        }

        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null && !model.isUpdateInProgress() && dataPumpJob == null) {
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
                null);
        }
    }

    void updateRowCount() {
        rowCountLabel.executeAction();
    }

    /**
     * Reads row count and sets value in status label
     */
    private long readRowCount(DBRProgressMonitor monitor) throws DBException {
        final DBCExecutionContext executionContext = getExecutionContext();
        DBSDataContainer dataContainer = getDataContainer();
        if (executionContext == null || dataContainer == null) {
            throw new DBException("Not connected");
        }
        try (DBCSession session = executionContext.openSession(
            monitor,
            DBCExecutionPurpose.USER,
            "Read total row count"))
        {
            long rowCount = dataContainer.countData(
                new AbstractExecutionSource(dataContainer, executionContext, this),
                session,
                model.getDataFilter(),
                DBSDataContainer.FLAG_NONE);
            model.setTotalRowCount(rowCount);
            return rowCount;
        }
    }

    private int getSegmentMaxRows()
    {
        if (getDataContainer() == null) {
            return 0;
        }
        return getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS);
    }

    private synchronized boolean runDataPump(
        @NotNull final DBSDataContainer dataContainer,
        @Nullable final DBDDataFilter dataFilter,
        final int offset,
        final int maxRows,
        final int focusRow,
        final boolean saveHistory,
        final boolean scroll,
        @Nullable final Runnable finalizer)
    {
        if (dataPumpJob != null) {
            UIUtils.showMessageBox(viewerPanel.getShell(), "Data read", "Data read is in progress - can't run another", SWT.ICON_WARNING);
            return false;
        }
        if (viewerPanel.isDisposed()) {
            return false;
        }
        DBCExecutionContext executionContext = getExecutionContext();
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
        dataReceiver.setFocusRow(focusRow);
        // Set explicit target container
        dataReceiver.setTargetDataContainer(dataContainer);
        dataPumpJob = new ResultSetJobDataRead(
            dataContainer,
            useDataFilter,
            this,
            executionContext,
            progressControl);
        dataPumpJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void aboutToRun(IJobChangeEvent event) {
                model.setUpdateInProgress(true);
                model.setStatistics(null);
                if (filtersPanel != null) {
                    UIUtils.syncExec(() -> filtersPanel.enableFilters(false));
                }
            }

            @Override
            public void done(IJobChangeEvent event) {
                final ResultSetJobDataRead job = (ResultSetJobDataRead)event.getJob();
                final Throwable error = job.getError();
                if (job.getStatistics() != null) {
                    model.setStatistics(job.getStatistics());
                }
                final Control control = getControl();
                if (control.isDisposed()) {
                    return;
                }
                UIUtils.syncExec(() -> {
                    try {
                        final Control control1 = getControl();
                        if (control1.isDisposed()) {
                            return;
                        }
                        model.setUpdateInProgress(false);
                        final boolean metadataChanged = model.isMetadataChanged();
                        if (error != null) {
                            setStatus(error.getMessage(), DBPMessageType.ERROR);
                            DBUserInterface.getInstance().showError(
                                    "Error executing query",
                                "Query execution failed",
                                error);
                        } else {
                            if (!metadataChanged && focusRow >= 0 && focusRow < model.getRowCount() && model.getVisibleAttributeCount() > 0) {
                                // Seems to be refresh
                                // Restore original position
                                restorePresentationState(presentationState);
                            }
                        }
                        if (metadataChanged) {
                            activePresentation.updateValueView();
                        }
                        updatePanelsContent(false);

                        if (!scroll) {
                            // Add new history item
                            if (saveHistory && error == null) {
                                setNewState(dataContainer, dataFilter);
                            }

                            if (dataFilter != null) {
                                model.updateDataFilter(dataFilter);
                                // New data filter may have different columns visibility
                                redrawData(true, false);
                            }
                        }
                        if (job.getStatistics() == null || !job.getStatistics().isEmpty()) {
                            if (error == null) {
                                // Update status (update execution statistics)
                                updateStatusMessage();
                            }
                            updateFiltersText(true);
                            updateToolbar();
                            fireResultSetLoad();
                        }
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

                        dataPumpJob = null;
                    }
                });
            }
        });
        dataPumpJob.setOffset(offset);
        dataPumpJob.setMaxRows(maxRows);
        dataPumpJob.schedule();

        return true;
    }

    private void clearData()
    {
        this.model.clearData();
        this.curRow = null;
        this.activePresentation.clearMetaData();
    }

    @Override
    public boolean applyChanges(@Nullable DBRProgressMonitor monitor)
    {
        return applyChanges(monitor, null);
    }

    /**
     * Saves changes to database
     * @param monitor monitor. If null then save will be executed in async job
     * @param listener finish listener (may be null)
     */
    private boolean applyChanges(@Nullable final DBRProgressMonitor monitor, @Nullable final ResultSetPersister.DataUpdateListener listener)
    {
        UIUtils.syncExec(() -> getActivePresentation().applyChanges());
        try {
            final ResultSetPersister persister = createDataPersister(false);
            final ResultSetPersister.DataUpdateListener applyListener = success -> {
                if (listener != null) {
                    listener.onUpdate(success);
                }
                if (success && getPreferenceStore().getBoolean(DBeaverPreferences.RS_EDIT_REFRESH_AFTER_UPDATE)) {
                    // Refresh updated rows
                    try {
                        persister.refreshInsertedRows();
                    } catch (Throwable e) {
                        log.error("Error refreshing rows after update", e);
                    }
                }
            };
            return persister.applyChanges(monitor, false, applyListener);
        } catch (DBException e) {
            DBUserInterface.getInstance().showError("Apply changes error", "Error saving changes in database", e);
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
        } catch (DBException e) {
            log.debug(e);
        }
    }

    @Override
    public List<DBEPersistAction> generateChangesScript(@NotNull DBRProgressMonitor monitor)
    {
        try {
            ResultSetPersister persister = createDataPersister(false);
            persister.applyChanges(monitor, true, null);
            return persister.getScript();
        } catch (DBException e) {
            DBUserInterface.getInstance().showError("SQL script generate error", "Error saving changes in database", e);
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
            checkEntityIdentifiers(persister);
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
        try (DBCSession session = executionContext.openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, CoreMessages.controls_resultset_viewer_add_new_row_context_name)) {

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
                            cells[0] = docAttribute.getValueHandler().getValueFromObject(session, docAttribute, origRow[0], true);
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
                                    cells[i] = metaAttr.getValueHandler().getValueFromObject(session, attribute, origRow[i], true);
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

    void deleteSelectedRows()
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
    DBDRowIdentifier getVirtualEntityIdentifier()
    {
        if (!model.isSingleSource() || model.getVisibleAttributeCount() == 0) {
            return null;
        }
        DBDRowIdentifier rowIdentifier = model.getVisibleAttribute(0).getRowIdentifier();
        DBSEntityReferrer identifier = rowIdentifier == null ? null : rowIdentifier.getUniqueKey();
        if (identifier != null && identifier instanceof DBVEntityConstraint) {
            return rowIdentifier;
        } else {
            return null;
        }
    }

    private void checkEntityIdentifiers(ResultSetPersister persister) throws DBException
    {

        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            throw new DBCException("Can't persist data - not connected to database");
        }

        boolean needsSingleEntity = persister.hasInserts() || persister.hasDeletes();

        DBSEntity entity = model.getSingleSource();
        if (needsSingleEntity) {
            if (entity == null) {
                throw new DBCException("Can't detect source entity");
            }
        }

        if (entity != null) {
            // Check for value locators
            // Probably we have only virtual one with empty attribute set
            DBDRowIdentifier identifier = getVirtualEntityIdentifier();
            if (identifier != null) {
                if (CommonUtils.isEmpty(identifier.getAttributes())) {
                    // Empty identifier. We have to define it
                    if (!new UIConfirmation() {
                        @Override
                        public Boolean runTask() {
                            return ValidateUniqueKeyUsageDialog.validateUniqueKey(ResultSetViewer.this, executionContext);
                        }
                    }.confirm())
                    {
                        throw new DBCException("No unique key defined");
                    }
                }
            }
        }

        List<DBDAttributeBinding> updatedAttributes = persister.getUpdatedAttributes();
        if (persister.hasDeletes()) {
            DBDRowIdentifier defIdentifier = persister.getDefaultRowIdentifier();
            if (defIdentifier == null) {
                throw new DBCException("No unique row identifier is result set. Cannot proceed with row(s) delete.");
            } else if (CommonUtils.isEmpty(defIdentifier.getAttributes())) {
                throw new DBCException("Attributes of unique key '" + DBUtils.getObjectFullName(defIdentifier.getUniqueKey(), DBPEvaluationContext.UI) + "' are missing in result set. Cannot proceed with row(s) delete.");
            }
        }

        {
            for (DBDAttributeBinding attr : updatedAttributes) {
                // Check attributes of non-virtual identifier
                DBDRowIdentifier rowIdentifier = attr.getRowIdentifier();
                if (rowIdentifier == null) {
                    // We shouldn't be here ever!
                    // Virtual id should be created if we missing natural one
                    throw new DBCException("Attribute " + attr.getName() + " was changed but it hasn't associated unique key");
                } else if (CommonUtils.isEmpty(rowIdentifier.getAttributes())) {
                    throw new DBCException(
                        "Can't update attribute '" + attr.getName() +
                            "' - attributes of key '" + DBUtils.getObjectFullName(rowIdentifier.getUniqueKey(), DBPEvaluationContext.UI) + "' are missing in result set");
                }
            }
        }
    }

    boolean editEntityIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        DBDRowIdentifier virtualEntityIdentifier = getVirtualEntityIdentifier();
        if (virtualEntityIdentifier == null) {
            log.warn("No virtual identifier");
            return false;
        }
        DBVEntityConstraint constraint = (DBVEntityConstraint) virtualEntityIdentifier.getUniqueKey();

        EditConstraintPage page = new EditConstraintPage(
            "Define virtual unique identifier",
            constraint);
        if (!page.edit()) {
            return false;
        }

        Collection<DBSEntityAttribute> uniqueAttrs = page.getSelectedAttributes();
        constraint.setAttributes(uniqueAttrs);
        virtualEntityIdentifier = getVirtualEntityIdentifier();
        if (virtualEntityIdentifier == null) {
            log.warn("No virtual identifier defined");
            return false;
        }
        virtualEntityIdentifier.reloadAttributes(monitor, model.getAttributes());
        persistConfig();

        return true;
    }

    private void clearEntityIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        DBDAttributeBinding firstAttribute = model.getVisibleAttribute(0);
        DBDRowIdentifier rowIdentifier = firstAttribute.getRowIdentifier();
        if (rowIdentifier != null) {
            DBVEntityConstraint virtualKey = (DBVEntityConstraint) rowIdentifier.getUniqueKey();
            virtualKey.setAttributes(Collections.emptyList());
            rowIdentifier.reloadAttributes(monitor, model.getAttributes());
            virtualKey.getParentObject().setProperty(DBVConstants.PROPERTY_USE_VIRTUAL_KEY_QUIET, null);
        }

        persistConfig();
    }

    @NotNull
    private IResultSetListener[] getListenersCopy() {
        IResultSetListener[] listenersCopy;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return EMPTY_LISTENERS;
            }
            listenersCopy = listeners.toArray(new IResultSetListener[listeners.size()]);
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
        public List<String> getQueryFilterHistory(@NotNull String query) throws DBException {
            final List<String> filters = filterHistory.get(query);
            if (filters != null) {
                return filters;
            }
            return Collections.emptyList();
        }

        @Override
        public void saveQueryFilterValue(@NotNull String query, @NotNull String filterValue) throws DBException {
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
            final ResultSetViewer rsv = (ResultSetViewer) ResultSetCommandHandler.getActiveResultSet(
                UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart());
            if (rsv == null) {
                return new IContributionItem[0];
            }
            List<IContributionItem> items = rsv.fillPanelsMenu();
            return items.toArray(new IContributionItem[items.size()]);
        }
    }

    private class ConfigAction extends Action implements IMenuCreator {
        ConfigAction()
        {
            super(CoreMessages.controls_resultset_viewer_action_options, IAction.AS_DROP_DOWN_MENU);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
        }

        @Override
        public IMenuCreator getMenuCreator()
        {
            return this;
        }

        @Override
        public void runWithEvent(Event event)
        {
            Menu menu = getMenu(activePresentation.getControl());
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
            menuManager.add(new ShowFiltersAction(false));
            menuManager.add(new CustomizeColorsAction());
            menuManager.add(new Separator());
            menuManager.add(new VirtualKeyEditAction(true));
            menuManager.add(new VirtualKeyEditAction(false));
            menuManager.add(new DictionaryEditAction());
            menuManager.add(new Separator());
            menuManager.add(new ToggleModeAction());
            activePresentation.fillMenu(menuManager);
            if (!CommonUtils.isEmpty(availablePresentations) && availablePresentations.size() > 1) {
                menuManager.add(new Separator());
                for (final ResultSetPresentationDescriptor pd : availablePresentations) {
                    Action action = new Action(pd.getLabel(), IAction.AS_RADIO_BUTTON) {
                        @Override
                        public boolean isEnabled() {
                            return !isRefreshInProgress();
                        }
                        @Override
                        public boolean isChecked() {
                            return pd == activePresentationDescriptor;
                        }

                        @Override
                        public void run() {
                            switchPresentation(pd);
                        }
                    };
                    if (pd.getIcon() != null) {
                        //action.setImageDescriptor(ImageDescriptor.createFromImage(pd.getIcon()));
                    }
                    menuManager.add(action);
                }
            }
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
        ShowFiltersAction(boolean context)
        {
            super(context ? "Customize ..." : "Order/Filter ...", DBeaverIcons.getImageDescriptor(UIIcon.FILTER));
        }

        @Override
        public void run()
        {
            new FilterSettingsDialog(ResultSetViewer.this).open();
        }

        @Override
        public boolean isEnabled() {
            return getModel().hasData();
        }
    }

    private class ToggleServerSideOrderingAction extends Action {
        ToggleServerSideOrderingAction()
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
            DBPPreferenceStore preferenceStore = getPreferenceStore();
            preferenceStore.setValue(
                DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE,
                !preferenceStore.getBoolean(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE));
        }
    }

    private enum FilterByAttributeType {
        VALUE(UIIcon.FILTER_VALUE) {
            @Override
            Object getValue(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attribute, @NotNull DBCLogicalOperator operator, boolean useDefault)
            {
                final ResultSetRow row = viewer.getCurrentRow();
                if (attribute == null || row == null) {
                    return null;
                }
                Object cellValue = viewer.model.getCellValue(attribute, row);
                if (operator == DBCLogicalOperator.LIKE && cellValue != null) {
                    cellValue = "%" + cellValue + "%";
                }
                return cellValue;
            }
        },
        INPUT(UIIcon.FILTER_INPUT) {
            @Override
            Object getValue(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attribute, @NotNull DBCLogicalOperator operator, boolean useDefault)
            {
                if (useDefault) {
                    return "..";
                } else {
                    ResultSetRow[] rows = null;
                    if (operator.getArgumentCount() < 0) {
                        Collection<ResultSetRow> selectedRows = viewer.getSelection().getSelectedRows();
                        rows = selectedRows.toArray(new ResultSetRow[selectedRows.size()]);
                    } else {
                        ResultSetRow focusRow = viewer.getCurrentRow();
                        if (focusRow != null) {
                            rows = new ResultSetRow[] { focusRow };
                        }
                    }
                    if (rows == null || rows.length == 0) {
                        return null;
                    }
                    FilterValueEditDialog dialog = new FilterValueEditDialog(viewer, attribute, rows, operator);
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        return dialog.getValue();
                    } else {
                        return null;
                    }
                }
            }
        },
        CLIPBOARD(UIIcon.FILTER_CLIPBOARD) {
            @Override
            Object getValue(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attribute, @NotNull DBCLogicalOperator operator, boolean useDefault)
            {
                try {
                    return ResultSetUtils.getAttributeValueFromClipboard(attribute);
                } catch (Exception e) {
                    log.debug("Error copying from clipboard", e);
                    return null;
                }
            }
        },
        NONE(UIIcon.FILTER_VALUE) {
            @Override
            Object getValue(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attribute, @NotNull DBCLogicalOperator operator, boolean useDefault)
            {
                return null;
            }
        };

        final ImageDescriptor icon;

        FilterByAttributeType(DBPImage icon)
        {
            this.icon = DBeaverIcons.getImageDescriptor(icon);
        }
        @Nullable
        abstract Object getValue(@NotNull ResultSetViewer viewer, @NotNull DBDAttributeBinding attribute, @NotNull DBCLogicalOperator operator, boolean useDefault);
    }

    private String translateFilterPattern(DBCLogicalOperator operator, FilterByAttributeType type, DBDAttributeBinding attribute)
    {
        Object value = type.getValue(this, attribute, operator, true);
        DBCExecutionContext executionContext = getExecutionContext();
        String strValue = executionContext == null ? String.valueOf(value) : attribute.getValueHandler().getValueDisplayString(attribute, value, DBDDisplayFormat.UI);
        strValue = strValue.trim();
        strValue = TextUtils.cutExtraLines(strValue, 1);
        strValue = CommonUtils.truncateString(strValue, 30);
        if (operator.getArgumentCount() == 0) {
            return operator.getStringValue();
        } else {
            return operator.getStringValue() + " " + CommonUtils.truncateString(strValue, 64);
        }
    }


    private class FilterByAttributeAction extends Action {
        private final DBCLogicalOperator operator;
        private final FilterByAttributeType type;
        private final DBDAttributeBinding attribute;
        FilterByAttributeAction(DBCLogicalOperator operator, FilterByAttributeType type, DBDAttributeBinding attribute)
        {
            super(attribute.getName() + " " + translateFilterPattern(operator, type, attribute), type.icon);
            this.operator = operator;
            this.type = type;
            this.attribute = attribute;
        }

        @Override
        public void run()
        {
            Object value = type.getValue(ResultSetViewer.this, attribute, operator, false);
            if (operator.getArgumentCount() != 0 && value == null) {
                return;
            }
            DBDDataFilter filter = new DBDDataFilter(model.getDataFilter());
            DBDAttributeConstraint constraint = filter.getConstraint(attribute);
            if (constraint != null) {
                constraint.setOperator(operator);
                constraint.setValue(value);
                setDataFilter(filter, true);
            }
        }
    }


    private class FilterByValueAction extends Action {
    	private final DBCLogicalOperator operator;
        private final FilterByAttributeType type;
        private final DBDAttributeBinding attribute;
        private final Object value;


        FilterByValueAction(DBCLogicalOperator operator, FilterByAttributeType type, DBDAttributeBinding attribute, Object value)
        {
            super(attribute.getName() + " = " + CommonUtils.truncateString(String.valueOf(value), 64), null);
            this.operator = operator;
            this.type = type;
            this.attribute = attribute;
            this.value = value;
        }


        @Override
        public void run()
        {

            if (operator.getArgumentCount() != 0 && value == null) {
                return;
            }
            DBDDataFilter filter = new DBDDataFilter(model.getDataFilter());
            DBDAttributeConstraint constraint = filter.getConstraint(attribute);
            if (constraint != null) {
                constraint.setOperator(operator);
                constraint.setValue(value);
                setDataFilter(filter, true);
            }
        }
    }



    private class FilterResetAttributeAction extends Action {
        private final DBDAttributeBinding attribute;
        FilterResetAttributeAction(DBDAttributeBinding attribute)
        {
            super("Remove filter for '" + attribute.getName() + "'", DBeaverIcons.getImageDescriptor(UIIcon.REVERT));
            this.attribute = attribute;
        }

        @Override
        public void run()
        {
            DBDDataFilter dataFilter = new DBDDataFilter(model.getDataFilter());
            DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
            if (constraint != null) {
                constraint.setCriteria(null);
                setDataFilter(dataFilter, true);
            }
        }
    }

    private class OrderByAttributeAction extends Action {
        private final DBDAttributeBinding attribute;
        private final boolean ascending;

        public OrderByAttributeAction(DBDAttributeBinding attribute, boolean ascending) {
            super("Order by " + attribute.getName() + " " + (ascending ? "ASC" : "DESC"));
            this.attribute = attribute;
            this.ascending = ascending;
        }

        @Override
        public void run()
        {
            toggleSortOrder(attribute, ascending, !ascending);
        }
    }

    private class TransformComplexTypesToggleAction extends Action {
        TransformComplexTypesToggleAction()
        {
            super("Structurize complex types", AS_CHECK_BOX);
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
            refreshData(null);
        }

    }

    private class ColorizeDataTypesToggleAction extends Action {
        ColorizeDataTypesToggleAction()
        {
            super("Colorize Data Types", AS_CHECK_BOX);
            setToolTipText("Set different foreground color for data types");
        }

        @Override
        public boolean isChecked() {
            DBPDataSource dataSource = getDataContainer().getDataSource();
            return dataSource != null &&
                dataSource.getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
        }

        @Override
        public void run()
        {
            DBPDataSource dataSource = getDataContainer().getDataSource();
            if (dataSource == null) {
                return;
            }
            DBPPreferenceStore dsStore = dataSource.getContainer().getPreferenceStore();
            boolean curValue = dsStore.getBoolean(DBeaverPreferences.RESULT_SET_COLORIZE_DATA_TYPES);
            // Set local setting to default
            dsStore.setValue(DBeaverPreferences.RESULT_SET_COLORIZE_DATA_TYPES, !curValue);
            refreshData(null);
        }

    }

    private abstract class ColorAction extends Action {
        ColorAction(String name) {
            super(name);
        }
        @NotNull
        DBVEntity getVirtualEntity(DBDAttributeBinding binding)
            throws IllegalStateException
        {
            final DBSEntity entity = getModel().getSingleSource();
            if (entity == null) {
                throw new IllegalStateException("No virtual entity for multi-source query");
            }
            final DBVEntity vEntity = DBVUtils.findVirtualEntity(entity, true);
            assert vEntity != null;
            return vEntity;
        }

        void updateColors(DBVEntity entity) {
            model.updateColorMapping();
            redrawData(false, false);
            entity.getDataSource().getContainer().persistConfiguration();
        }
    }

    private class SetRowColorAction extends ColorAction {
        private final DBDAttributeBinding attribute;
        private final Object value;
        SetRowColorAction(DBDAttributeBinding attr, Object value) {
            super("Color by " + attr.getName());
            this.attribute = attr;
            this.value = value;
        }

        @Override
        public void run() {
            RGB color;
            final Shell shell = UIUtils.createCenteredShell(getControl().getShell());
            try {
                ColorDialog cd = new ColorDialog(shell);
                color = cd.open();
                if (color == null) {
                    return;
                }
            } finally {
                shell.dispose();
            }
            try {
                final DBVEntity vEntity = getVirtualEntity(attribute);
                vEntity.setColorOverride(attribute, value, null, StringConverter.asString(color));
                updateColors(vEntity);
            } catch (IllegalStateException e) {
                DBUserInterface.getInstance().showError(
                        "Row color",
                    "Can't set row color",
                    e);
            }
        }
    }

    private class ResetRowColorAction extends ColorAction {
        private final DBDAttributeBinding attribute;
        ResetRowColorAction(DBDAttributeBinding attr, Object value) {
            super("Reset color by " + attr.getName());
            this.attribute = attr;
        }

        @Override
        public void run() {
            final DBVEntity vEntity = getVirtualEntity(attribute);
            vEntity.removeColorOverride(attribute);
            updateColors(vEntity);
        }
    }

    private class ResetAllColorAction extends ColorAction {
        ResetAllColorAction() {
            super("Reset all colors");
        }

        @Override
        public void run() {
            final DBVEntity vEntity = getVirtualEntity(getModel().getAttributes()[0]);
            if (!UIUtils.confirmAction("Reset all row coloring", "Are you sure you want to reset all color settings for '" + vEntity.getName() + "'?")) {
                return;
            }
            vEntity.removeAllColorOverride();
            updateColors(vEntity);
        }
    }

    private class CustomizeColorsAction extends ColorAction {
        private final DBDAttributeBinding curAttribute;
        private final ResultSetRow row;

        CustomizeColorsAction() {
            this(null, null);
        }

        CustomizeColorsAction(DBDAttributeBinding curAttribute, ResultSetRow row) {
            super("Row colors ...");
            this.curAttribute = curAttribute;
            this.row = row;
        }

        @Override
        public void run() {
            ColorSettingsDialog dialog = new ColorSettingsDialog(ResultSetViewer.this, curAttribute, row);
            if (dialog.open() != IDialogConstants.OK_ID) {
                return;
            }
            final DBVEntity vEntity = getVirtualEntity(curAttribute);
            //vEntity.removeColorOverride(attribute);
            updateColors(vEntity);
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    private class VirtualKeyEditAction extends Action {
        private boolean define;

        VirtualKeyEditAction(boolean define)
        {
            super(define ? "Define virtual unique key" : "Clear virtual unique key");
            this.define = define;
        }

        @Override
        public boolean isEnabled()
        {
            DBDRowIdentifier identifier = getVirtualEntityIdentifier();
            return identifier != null && (define || !CommonUtils.isEmpty(identifier.getAttributes()));
        }

        @Override
        public void run()
        {
            UIUtils.runUIJob("Edit virtual key", monitor -> {
                try {
                    if (define) {
                        editEntityIdentifier(monitor);
                    } else {
                        clearEntityIdentifier(monitor);
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
    }

    private class DictionaryEditAction extends Action {
        DictionaryEditAction()
        {
            super("Define dictionary");
        }

        @Override
        public void run()
        {
            EditDictionaryPage page = new EditDictionaryPage(
                "Edit dictionary",
                model.getSingleSource());
            page.edit();
        }

        @Override
        public boolean isEnabled()
        {
            final DBSEntity singleSource = model.getSingleSource();
            return singleSource != null;
        }
    }

    private class ToggleModeAction extends Action {
        {
            setActionDefinitionId(ResultSetCommandHandler.CMD_TOGGLE_MODE);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.RS_DETAILS));
        }

        ToggleModeAction() {
            super("Record", Action.AS_CHECK_BOX);
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

    private class PanelToggleAction extends Action {
        private final ResultSetPanelDescriptor panel;

        public PanelToggleAction(ResultSetPanelDescriptor panel) {
            super(panel.getLabel(), Action.AS_CHECK_BOX);
            this.panel = panel;
            setToolTipText(panel.getDescription());
            // Icons turns menu into mess - checkboxes are much better
            //setImageDescriptor(DBeaverIcons.getImageDescriptor(panel.getIcon()));
        }

        @Override
        public boolean isChecked() {
            return activePanels.containsKey(panel.getId());
        }

        @Override
        public void run() {
            if (isPanelsVisible() && isChecked()) {
                CTabItem panelTab = getPanelTab(panel.getId());
                if (panelTab != null) {
                    panelTab.dispose();
                    removePanel(panel.getId());
                }
            } else {
                activatePanel(panel.getId(), true, true);
            }
        }
    }

    private class ToolbarToggleAction extends Action {
        private final String toolbarId;

        public ToolbarToggleAction(String toolbarId, String toolbarLabel) {
            super(toolbarLabel, Action.AS_CHECK_BOX);
            this.toolbarId = toolbarId;
            //setToolTipText(panel.getDescription());
        }

        @Override
        public boolean isChecked() {
            return activePanels.containsKey(toolbarId);
        }

        @Override
        public void run() {
/*
            if (isPanelsVisible() && isChecked()) {
                CTabItem panelTab = getPanelTab(panel.getId());
                if (panelTab != null) {
                    panelTab.dispose();
                    removePanel(panel.getId());
                }
            } else {
                activatePanel(panel.getId(), true, true);
            }
*/
        }
    }

    /*
    public static void openNewDataEditor(DBNDatabaseNode targetNode, DBDDataFilter newFilter) {
        IEditorPart entityEditor = NavigatorHandlerObjectOpen.openEntityEditor(
            targetNode,
            DatabaseDataEditor.class.getName(),
            Collections.<String, Object>singletonMap(DatabaseDataEditor.ATTR_DATA_FILTER, newFilter),
            DBeaverUI.getActiveWorkbenchWindow()
        );

        if (entityEditor instanceof MultiPageEditorPart) {
            Object selectedPage = ((MultiPageEditorPart) entityEditor).getSelectedPage();
            if (selectedPage instanceof IResultSetContainer) {
                ResultSetViewer rsv = (ResultSetViewer) ((IResultSetContainer) selectedPage).getResultSetController();
                if (rsv != null && !rsv.isRefreshInProgress() && !newFilter.equals(rsv.getModel().getDataFilter())) {
                    // Set filter directly
                    rsv.refreshWithFilter(newFilter);
                }
            }
        }
    }
*/

}
