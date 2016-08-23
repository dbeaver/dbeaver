/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.*;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.CImageCombo;
import org.jkiss.dbeaver.ui.controls.resultset.view.EmptyPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.view.StatisticsPresentation;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.struct.EditConstraintDialog;
import org.jkiss.dbeaver.ui.dialogs.struct.EditDictionaryDialog;
import org.jkiss.dbeaver.ui.editors.data.DatabaseDataEditor;
import org.jkiss.dbeaver.ui.preferences.PrefPageDataFormat;
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseGeneral;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ResultSetViewer
 *
 * TODO: fix copy multiple cells - tabulation broken
 * TODO: links in both directions, multiple links support (context menu)
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
    public static final String SETTINGS_SECTION_PRESENTATIONS = "presentations";

    @NotNull
    private static IResultSetFilterManager filterManager = new SimpleFilterManager();

    @NotNull
    private final IWorkbenchPartSite site;
    private final Composite viewerPanel;
    private ResultSetFilterPanel filtersPanel;
    private SashForm viewerSash;
    private CTabFolder panelFolder;
    private ToolBarManager panelToolBar;
    private final Composite presentationPanel;

    private Text statusLabel;

    private final DynamicFindReplaceTarget findReplaceTarget;

    // Presentation
    @NotNull
    private IResultSetPresentation activePresentation;
    private ResultSetPresentationDescriptor activePresentationDescriptor;
    private List<ResultSetPresentationDescriptor> availablePresentations;
    private PresentationSwitchCombo presentationSwitchCombo;
    private final List<ResultSetPanelDescriptor> availablePanels = new ArrayList<>();

    private final Map<ResultSetPresentationDescriptor, PresentationSettings> presentationSettings = new HashMap<>();
    private final Map<String, IResultSetPanel> activePanels = new HashMap<>();

    @NotNull
    private final IResultSetContainer container;
    @NotNull
    private final ResultSetDataReceiver dataReceiver;

    private ToolBarManager mainToolbar;

    // Current row/col number
    @Nullable
    private ResultSetRow curRow;
    // Mode
    private boolean recordMode;

    private final List<IResultSetListener> listeners = new ArrayList<>();

    private volatile ResultSetDataPumpJob dataPumpJob;

    private final ResultSetModel model = new ResultSetModel();
    private HistoryStateItem curState = null;
    private final List<HistoryStateItem> stateHistory = new ArrayList<>();
    private int historyPosition = -1;

    private final IDialogSettings viewerSettings;

    private final Color colorRed;

    private boolean actionsDisabled;

    public ResultSetViewer(@NotNull Composite parent, @NotNull IWorkbenchPartSite site, @NotNull IResultSetContainer container)
    {
        super();

        this.site = site;
        this.recordMode = false;
        this.container = container;
        this.dataReceiver = new ResultSetDataReceiver(this);

        this.colorRed = Display.getDefault().getSystemColor(SWT.COLOR_RED);
        this.viewerSettings = UIUtils.getDialogSettings(ResultSetViewer.class.getSimpleName());
        loadPresentationSettings();

        this.viewerPanel = UIUtils.createPlaceholder(parent, 1);
        UIUtils.setHelp(this.viewerPanel, IHelpContextIds.CTX_RESULT_SET_VIEWER);

        this.filtersPanel = new ResultSetFilterPanel(this);
        this.findReplaceTarget = new DynamicFindReplaceTarget();

        this.viewerSash = UIUtils.createPartDivider(site.getPart(), viewerPanel, SWT.HORIZONTAL | SWT.SMOOTH);
        this.viewerSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        this.presentationPanel = UIUtils.createPlaceholder(this.viewerSash, 1);
        this.presentationPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

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
        this.panelFolder.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                if (!viewerSash.isDisposed()) {
                    int[] weights = viewerSash.getWeights();
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
                showPanels(false);
            }

            @Override
            public void maximize(CTabFolderEvent event) {

            }
        });

        setActivePresentation(new EmptyPresentation());

        createStatusBar();

        this.viewerPanel.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                dispose();
            }
        });

        changeMode(false);
    }

    @Override
    @NotNull
    public IResultSetContainer getContainer() {
        return container;
    }

    ////////////////////////////////////////////////////////////
    // Filters

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

    public void updateFiltersText()
    {
        updateFiltersText(true);
    }

    private void updateFiltersText(boolean resetFilterValue)
    {
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

                if (container.isReadyToRun() && !model.isUpdateInProgress() && model.getVisibleAttributeCount() > 0) {
                    enableFilters = true;
                }
            }
        }
        filtersPanel.enableFilters(enableFilters);
        presentationSwitchCombo.combo.setEnabled(enableFilters);
    }

    public void setDataFilter(final DBDDataFilter dataFilter, boolean refreshData)
    {
        if (!model.getDataFilter().equals(dataFilter)) {
            //model.setDataFilter(dataFilter);
            if (refreshData) {
                refreshWithFilter(dataFilter);
            } else {
                model.setDataFilter(dataFilter);
                activePresentation.refreshData(true, false);
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

    @Override
    public IDialogSettings getViewerSettings() {
        return viewerSettings;
    }

    @NotNull
    @Override
    public Color getDefaultBackground() {
        return filtersPanel.getEditControl().getBackground();
    }

    @NotNull
    @Override
    public Color getDefaultForeground() {
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

    public List<ResultSetPresentationDescriptor> getAvailablePresentations() {
        return availablePresentations;
    }

    @Override
    @NotNull
    public IResultSetPresentation getActivePresentation() {
        return activePresentation;
    }

    void updatePresentation(final DBCResultSet resultSet) {
        try {
            if (resultSet instanceof StatResultSet) {
                // Statistics - let's use special presentation for it
                availablePresentations = Collections.emptyList();
                setActivePresentation(new StatisticsPresentation());
                activePresentationDescriptor = null;
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
                availablePresentations = ResultSetPresentationRegistry.getInstance().getAvailablePresentations(resultSet, context);
                if (!availablePresentations.isEmpty()) {
                    for (ResultSetPresentationDescriptor pd : availablePresentations) {
                        if (pd == activePresentationDescriptor) {
                            // Keep the same presentation
                            return;
                        }
                    }
                    String defaultPresentationId = getPreferenceStore().getString(DBeaverPreferences.RESULT_SET_PRESENTATION);
                    ResultSetPresentationDescriptor newPresentation = null;
                    if (!CommonUtils.isEmpty(defaultPresentationId)) {
                        for (ResultSetPresentationDescriptor pd : availablePresentations) {
                            if (pd.getId().equals(defaultPresentationId)) {
                                newPresentation = pd;
                                break;
                            }
                        }
                    }
                    if (newPresentation == null) {
                        newPresentation = availablePresentations.get(0);
                    }
                    try {
                        IResultSetPresentation instance = newPresentation.createInstance();
                        activePresentationDescriptor = newPresentation;
                        setActivePresentation(instance);
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
            }
        } finally {
            // Update combo
            CImageCombo combo = presentationSwitchCombo.combo;
            combo.setRedraw(false);
            try {
                if (activePresentationDescriptor == null) {
                    combo.setEnabled(false);
                } else {
                    combo.setEnabled(true);
                    combo.removeAll();
                    for (ResultSetPresentationDescriptor pd : availablePresentations) {
                        combo.add(DBeaverIcons.getImage(pd.getIcon()), pd.getLabel(), null, pd);
                    }
                    combo.select(activePresentationDescriptor);
                }
            } finally {
                // Enable redraw
                combo.setRedraw(true);
            }
        }

    }

    private void setActivePresentation(@NotNull IResultSetPresentation presentation) {
        // Dispose previous presentation and panels
        for (Control child : presentationPanel.getChildren()) {
            child.dispose();
        }
        for (CTabItem panelItem : panelFolder.getItems()) {
            panelItem.dispose();
        }

        // Set new presentation
        activePresentation = presentation;
        availablePanels.clear();
        activePanels.clear();
        if (activePresentationDescriptor != null) {
            availablePanels.addAll(ResultSetPresentationRegistry.getInstance().getSupportedPanels(activePresentationDescriptor));
        }
        activePresentation.createPresentation(this, presentationPanel);

        // Activate panels
        {
            boolean panelsVisible = false;
            int[] panelWeights = new int[]{700, 300};

            if (activePresentationDescriptor != null) {
                PresentationSettings settings = getPresentationSettings();
                panelsVisible = settings.panelsVisible;
                if (settings.panelRatio > 0) {
                    panelWeights = new int[] {1000 - settings.panelRatio, settings.panelRatio};
                }
                activateDefaultPanels(settings);
            }
            showPanels(panelsVisible);
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

        if (mainToolbar != null) {
            mainToolbar.update(true);
        }

        // Set focus in presentation control
        // Use async exec to avoid focus switch after user UI interaction (e.g. combo)
        Display display = getControl().getDisplay();
        if (UIUtils.isParent(viewerPanel, display.getFocusControl())) {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    activePresentation.getControl().setFocus();
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
            instance.refreshData(true, false);

            presentationSwitchCombo.combo.select(activePresentationDescriptor);
            // Save in global preferences
            DBeaverCore.getGlobalPreferenceStore().setValue(DBeaverPreferences.RESULT_SET_PRESENTATION, activePresentationDescriptor.getId());
            savePresentationSettings();
        } catch (Throwable e1) {
            UIUtils.showErrorDialog(
                viewerPanel.getShell(),
                "Presentation switch",
                "Can't switch presentation",
                e1);
        }
    }

    private void loadPresentationSettings() {
        IDialogSettings pSections = viewerSettings.getSection(SETTINGS_SECTION_PRESENTATIONS);
        if (pSections != null) {
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
                presentationSettings.put(presentation, settings);
            }
        }
    }

    private PresentationSettings getPresentationSettings() {
        PresentationSettings settings = this.presentationSettings.get(activePresentationDescriptor);
        if (settings == null) {
            settings = new PresentationSettings();
            this.presentationSettings.put(activePresentationDescriptor, settings);
        }
        return settings;
    }

    private void savePresentationSettings() {
        IDialogSettings pSections = UIUtils.getSettingsSection(viewerSettings, SETTINGS_SECTION_PRESENTATIONS);
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
        }
    }

    IResultSetPanel getVisiblePanel() {
        return activePanels.get(getPresentationSettings().activePanelId);
    }

    @Override
    public IResultSetPanel[] getActivePanels() {
        return activePanels.values().toArray(new IResultSetPanel[activePanels.size()]);
    }

    @Override
    public void activatePanel(String id, boolean setActive, boolean showPanels) {
        if (showPanels && !isPanelsVisible()) {
            showPanels(true);
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
                return;
            } else {
                log.warn("Panel '" + id + "' tab not found");
            }
        }
        // Create panel
        ResultSetPanelDescriptor panelDescriptor = getPanelDescriptor(id);
        if (panelDescriptor == null) {
            log.error("Panel '" + id + "' not found");
            return;
        }
        try {
            panel = panelDescriptor.createInstance();
        } catch (DBException e) {
            UIUtils.showErrorDialog(getSite().getShell(), "Can't show panel", "Can't create panel '" + id + "'", e);
            return;
        }
        activePanels.put(id, panel);

        // Create control and tab item
        Control panelControl = panel.createContents(activePresentation, panelFolder);

        boolean firstPanel = panelFolder.getItemCount() == 0;
        CTabItem panelTab = new CTabItem(panelFolder, SWT.CLOSE);
        panelTab.setData(id);
        panelTab.setText(panel.getPanelTitle());
        panelTab.setImage(DBeaverIcons.getImage(panelDescriptor.getIcon()));
        panelTab.setToolTipText(panel.getPanelDescription());
        panelTab.setControl(panelControl);

        if (setActive || firstPanel) {
            panelFolder.setSelection(panelTab);
        }

        presentationSettings.enabledPanelIds.add(id);
        if (setActive) {
            setActivePanel(id);
        }
    }

    private void activateDefaultPanels(PresentationSettings settings) {
        if (settings.enabledPanelIds.isEmpty()) {
            for (ResultSetPanelDescriptor pd : availablePanels) {
                if (pd.isShowByDefault()) {
                    settings.enabledPanelIds.add(pd.getId());
                }
            }
        }
        if (!settings.enabledPanelIds.isEmpty()) {
            for (String panelId : settings.enabledPanelIds) {
                activatePanel(panelId, panelId.equals(settings.activePanelId), false);
            }
        }
    }

    private void setActivePanel(String panelId) {
        PresentationSettings settings = getPresentationSettings();
        if (CommonUtils.equalObjects(settings.activePanelId, panelId)) {
            return;
        }
        settings.activePanelId = panelId;
        IResultSetPanel panel = activePanels.get(panelId);
        if (panel != null) {
            panelToolBar.removeAll();
            panel.activatePanel(panelToolBar);
            addDefaultPanelActions();
            panelToolBar.update(true);
        }
    }

    private void removePanel(String panelId) {
        IResultSetPanel panel = activePanels.remove(panelId);
        if (panel != null) {
            panel.deactivatePanel();
        }
        getPresentationSettings().enabledPanelIds.remove(panelId);
        if (activePanels.isEmpty()) {
            showPanels(false);
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
        for (CTabItem tab : panelFolder.getItems()) {
            if (CommonUtils.equalObjects(tab.getData(), panelId)) {
                return tab;
            }
        }
        return null;
    }

    public boolean isPanelsVisible() {
        return viewerSash.getMaximizedControl() == null;
    }

    public void showPanels(boolean show) {
        if (!show) {
            viewerSash.setMaximizedControl(presentationPanel);
        } else {
            activateDefaultPanels(getPresentationSettings());
            viewerSash.setMaximizedControl(null);
            panelToolBar.removeAll();
            IResultSetPanel panel = getVisiblePanel();
            if (panel != null) {
                panel.activatePanel(panelToolBar);
            }
            addDefaultPanelActions();
            panelToolBar.update(true);
            activePresentation.updateValueView();
        }

        getPresentationSettings().panelsVisible = show;

        // Refresh elements
        ICommandService commandService = getSite().getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(ResultSetCommandHandler.CMD_TOGGLE_PANELS, null);
        }
    }

    private List<IContributionItem> fillPanelsMenu() {
        List<IContributionItem> items = new ArrayList<>();

        for (final ResultSetPanelDescriptor panel : availablePanels) {
            Action panelAction = new Action(panel.getLabel(), Action.AS_CHECK_BOX) {
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
            };
            //panelAction.setImageDescriptor(DBeaverIcons.getImageDescriptor(panel.getIcon()));
            items.add(new ActionContributionItem(panelAction));
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

    public boolean isActionsDisabled() {
        return actionsDisabled;
    }

    @Override
    public void lockActionsByControl(Control lockedBy) {
        if (checkDoubleLock(lockedBy)) {
            return;
        }
        actionsDisabled = true;
        lockedBy.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                actionsDisabled = false;
            }
        });
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
        lockedBy.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                actionsDisabled = false;
            }
        });
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
        if (adapter == IFindReplaceTarget.class) {
            return adapter.cast(findReplaceTarget);
        }
        if (adapter.isAssignableFrom(activePresentation.getClass())) {
            return adapter.cast(activePresentation);
        }
        // Try to get it from adapter
        if (activePresentation instanceof IAdaptable) {
            return ((IAdaptable) activePresentation).getAdapter(adapter);
        }
        return null;
    }

    public void addListener(IResultSetListener listener)
    {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(IResultSetListener listener)
    {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void updateRecordMode()
    {
        //Object state = savePresentationState();
        //this.redrawData(false);
        activePresentation.refreshData(true, false);
        this.updateStatusMessage();
        //restorePresentationState(state);
    }

    public void updateEditControls()
    {
        ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_EDITABLE);
        ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CHANGED);
        updateToolbar();
    }

    /**
     * It is a hack function. Generally all command associated widgets should be updated automatically by framework.
     * Freaking E4 do not do it. I've spent a couple of days fighting it. Guys, you owe me.
     */
    private void updateToolbar()
    {
        UIUtils.updateContributionItems(mainToolbar);
        UIUtils.updateContributionItems(panelToolBar);
    }

    public void redrawData(boolean rowsChanged)
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
                activePresentation.refreshData(false, false);
                this.updateFiltersText();
                this.updateStatusMessage();
            } else {
                this.updateRecordMode();
            }
        } else {
            activePresentation.refreshData(false, false);
        }
    }

    private void createStatusBar()
    {
        UIUtils.createHorizontalLine(viewerPanel);

        Composite statusBar = new Composite(viewerPanel, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        statusBar.setLayoutData(gd);
        GridLayout gl = new GridLayout(4, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
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

        mainToolbar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

        // Add presentation switcher
        presentationSwitchCombo = new PresentationSwitchCombo();
        presentationSwitchCombo.createControl(statusBar);
        //mainToolbar.add(presentationSwitchCombo);
        //mainToolbar.add(new Separator());

        // handle own commands
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_APPLY_CHANGES));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_REJECT_CHANGES));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_GENERATE_SCRIPT));
        mainToolbar.add(new Separator());
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_ADD));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_COPY));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_DELETE));
        mainToolbar.add(new Separator());
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_FIRST));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_PREVIOUS));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_NEXT));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_LAST));
        mainToolbar.add(new Separator());
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_PAGE));
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_ALL));
        // Use simple action for refresh to avoid ambiguous behaviour of F5 shortcut
        mainToolbar.add(new Separator());
//        // FIXME: Link to standard Find/Replace action - it has to be handled by owner site
//        mainToolbar.add(ActionUtils.makeCommandContribution(
//            site,
//            IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE,
//            CommandContributionItem.STYLE_PUSH,
//            UIIcon.FIND_TEXT));

        mainToolbar.add(new Separator());
        mainToolbar.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_MODE, CommandContributionItem.STYLE_CHECK));

        CommandContributionItem panelsAction = new CommandContributionItem(new CommandContributionItemParameter(
            site,
            "org.jkiss.dbeaver.core.resultset.panels",
            ResultSetCommandHandler.CMD_TOGGLE_PANELS,
            CommandContributionItem.STYLE_PULLDOWN));
        //panelsAction.
        mainToolbar.add(panelsAction);
        mainToolbar.add(new Separator());
        mainToolbar.add(new ConfigAction());
        mainToolbar.createControl(statusBar);

        //updateEditControls();
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

    public void toggleMode()
    {
        changeMode(!recordMode);

        // Refresh elements
        ICommandService commandService = site.getService(ICommandService.class);
        if (commandService != null) {
            commandService.refreshElements(ResultSetCommandHandler.CMD_TOGGLE_MODE, null);
        }
    }

    private void changeMode(boolean recordMode)
    {
        //Object state = savePresentationState();
        this.recordMode = recordMode;
        //redrawData(false);
        activePresentation.refreshData(true, false);
        activePresentation.changeMode(recordMode);
        updateStatusMessage();
        //restorePresentationState(state);
    }

    ////////////////////////////////////////////////////////////
    // Misc

    private void dispose()
    {
        savePresentationSettings();

        clearData();

        if (mainToolbar != null) {
            try {
                mainToolbar.dispose();
            } catch (Throwable e) {
                // ignore
                log.debug("Error disposing toolbar", e);
            }
        }
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
        dataFilter = new DBDDataFilter(dataFilter);
        // Search in history
        for (int i = 0; i < stateHistory.size(); i++) {
            HistoryStateItem item = stateHistory.get(i);
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
            statusLabel.setForeground(getDefaultForeground());
        }
        if (status == null) {
            status = "???"; //$NON-NLS-1$
        }
        statusLabel.setText(status);
    }

    public void updateStatusMessage()
    {
        if (model.getRowCount() == 0) {
            if (model.getVisibleAttributeCount() == 0) {
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
     * @param attributes attributes metadata
     */
    void setMetaData(DBDAttributeBinding[] attributes)
    {
        model.setMetaData(attributes);
        activePresentation.clearMetaData();
    }

    void setData(List<Object[]> rows)
    {
        if (viewerPanel.isDisposed()) {
            return;
        }
        this.curRow = null;
        this.model.setData(rows);
        this.curRow = (this.model.getRowCount() > 0 ? this.model.getRow(0) : null);

        {

            if (getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE)) {
                boolean newRecordMode = (rows.size() == 1);
                if (newRecordMode != recordMode) {
                    toggleMode();
//                    ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_TOGGLE);
                }
            }
        }

        this.activePresentation.refreshData(true, false);
        if (recordMode) {
            this.updateRecordMode();
        }
        this.updateFiltersText();
        this.updateStatusMessage();
        this.updateEditControls();
    }

    void appendData(List<Object[]> rows)
    {
        model.appendData(rows);
        //redrawData(true);
        activePresentation.refreshData(false, true);

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
    public boolean isHasMoreData() {
        return getExecutionContext() != null && dataReceiver.isHasMoreData();
    }

    @Override
    public boolean isReadOnly()
    {
        if (model.isUpdateInProgress() || !(activePresentation instanceof IResultSetEditor)) {
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
    public boolean isInsertable()
    {
        return
            !isReadOnly() &&
            model.isSingleSource() &&
            model.getVisibleAttributeCount() > 0;
    }

    public boolean isRefreshInProgress() {
        return dataPumpJob != null;
    }

    ///////////////////////////////////////////////////////
    // Context menu & filters

    @NotNull
    public static IResultSetFilterManager getFilterManager() {
        return filterManager;
    }

    public static void registerFilterManager(@Nullable IResultSetFilterManager filterManager) {
        if (filterManager == null) {
            filterManager = new SimpleFilterManager();
        }
        ResultSetViewer.filterManager = filterManager;
    }

    public void showFiltersMenu() {
        DBDAttributeBinding curAttribute = getActivePresentation().getCurrentAttribute();
        if (curAttribute == null) {
            return;
        }
        Control control = getActivePresentation().getControl();
        Point cursorLocation = getActivePresentation().getCursorLocation();
        Point location = control.getDisplay().map(control, null, cursorLocation);

        MenuManager menuManager = new MenuManager();
        fillFiltersMenu(curAttribute, menuManager);

        final Menu contextMenu = menuManager.createContextMenu(control);
        contextMenu.setLocation(location);
        contextMenu.setVisible(true);
    }

    @Override
    public void fillContextMenu(@NotNull IMenuManager manager, @Nullable final DBDAttributeBinding attr, @Nullable final ResultSetRow row)
    {
        final DBPDataSource dataSource = getDataContainer() == null ? null : getDataContainer().getDataSource();

        // Custom oldValue items
        final ResultSetValueController valueController;
        if (attr != null && row != null) {
            valueController = new ResultSetValueController(
                this,
                attr,
                row,
                IValueController.EditType.NONE,
                null);

            final Object value = valueController.getValue();
            {
                // Standard items
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_CUT));
                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_COPY));

                MenuManager extCopyMenu = new MenuManager(ActionUtils.findCommandName(CoreCommands.CMD_COPY_SPECIAL));
                extCopyMenu.add(ActionUtils.makeCommandContribution(site, CoreCommands.CMD_COPY_SPECIAL));
                extCopyMenu.add(new Action("Copy column name(s)") {
                    @Override
                    public void run() {
                        StringBuilder buffer = new StringBuilder();
                        for (DBDAttributeBinding attr : getSelection().getSelectedAttributes()) {
                            if (buffer.length() > 0) {
                                buffer.append("\t");
                            }
                            buffer.append(attr.getName());
                        }
                        ResultSetUtils.copyToClipboard(buffer.toString());
                    }
                });
                extCopyMenu.add(new Action("Copy row number(s)") {
                    @Override
                    public void run() {
                        StringBuilder buffer = new StringBuilder();
                        for (ResultSetRow row : getSelection().getSelectedRows()) {
                            if (buffer.length() > 0) {
                                buffer.append("\n");
                            }
                            buffer.append(row.getVisualNumber());
                        }
                        ResultSetUtils.copyToClipboard(buffer.toString());
                    }
                });
                manager.add(extCopyMenu);

                manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_PASTE));
                manager.add(ActionUtils.makeCommandContribution(site, CoreCommands.CMD_PASTE_SPECIAL));
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
                                BaseValueManager.makeNullValue(valueController));
                        }
                    });
                }
                manager.add(new GroupMarker(MENU_GROUP_EDIT));
            }

            // Menus from value handler
            try {
                manager.add(new Separator());
                valueController.getValueManager().contributeActions(manager, valueController, null);
            }
            catch (Exception e) {
                log.error(e);
            }

            if (row.isChanged()) {
                Action resetValueAction = new Action(CoreMessages.controls_resultset_viewer_action_reset_value) {
                    @Override
                    public void run() {
                        model.resetCellValue(attr, row);
                        updatePanelsContent();
                    }
                };
                resetValueAction.setAccelerator(SWT.ESC);
                manager.insertAfter(IResultSetController.MENU_GROUP_EDIT, resetValueAction);
            }
        } else {
            valueController = null;
        }

        if (dataSource != null && attr != null && model.getVisibleAttributeCount() > 0 && !model.isUpdateInProgress()) {
            // Filters and View
            manager.add(new Separator());
            {
                String filtersShortcut = ActionUtils.findCommandDescription(ResultSetCommandHandler.CMD_FILTER_MENU, getSite(), true);
                String menuName = CoreMessages.controls_resultset_viewer_action_order_filter;
                if (!CommonUtils.isEmpty(filtersShortcut)) {
                    menuName += " (" + filtersShortcut + ")";
                }
                MenuManager filtersMenu = new MenuManager(
                    menuName,
                    DBeaverIcons.getImageDescriptor(UIIcon.FILTER),
                    "filters"); //$NON-NLS-1$
                filtersMenu.setRemoveAllWhenShown(true);
                filtersMenu.addMenuListener(new IMenuListener() {
                    @Override
                    public void menuAboutToShow(IMenuManager manager) {
                        fillFiltersMenu(attr, manager);
                    }
                });
                manager.add(filtersMenu);
            }
            {
                MenuManager viewMenu = new MenuManager(
                    "View/Format",
                    null,
                    "view"); //$NON-NLS-1$

                List<? extends DBDAttributeTransformerDescriptor> transformers =
                    dataSource.getContainer().getApplication().getValueHandlerRegistry().findTransformers(
                        dataSource, attr, null);
                if (!CommonUtils.isEmpty(transformers)) {
                    MenuManager transformersMenu = new MenuManager("View as");
                    transformersMenu.setRemoveAllWhenShown(true);
                    transformersMenu.addMenuListener(new IMenuListener() {
                        @Override
                        public void menuAboutToShow(IMenuManager manager) {
                            fillAttributeTransformersMenu(manager, attr);
                        }
                    });
                    viewMenu.add(transformersMenu);
                } else {
                    final Action customizeAction = new Action("View as") {};
                    customizeAction.setEnabled(false);
                    viewMenu.add(customizeAction);
                }
                if (getModel().isSingleSource()) {
                    if (valueController != null) {
                        viewMenu.add(new SetRowColorAction(attr, valueController.getValue()));
                        if (getModel().hasColorMapping(attr)) {
                            viewMenu.add(new ResetRowColorAction(attr, valueController.getValue()));
                        }
                    }
                    viewMenu.add(new CustomizeColorsAction(attr, row));
                    viewMenu.add(new Separator());
                }
                viewMenu.add(new Action("Data formats ...") {
                    @Override
                    public void run() {
                        UIUtils.showPreferencesFor(
                            getControl().getShell(),
                            null,
                            PrefPageDataFormat.PAGE_ID);
                    }
                });
                manager.add(viewMenu);
            }

            {
                // Navigate
                MenuManager navigateMenu = new MenuManager(
                    "Navigate",
                    null,
                    "navigate"); //$NON-NLS-1$
                if (ActionUtils.isCommandEnabled(ResultSetCommandHandler.CMD_NAVIGATE_LINK, site)) {
                    navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_NAVIGATE_LINK));
                    navigateMenu.add(new Separator());
                }
                navigateMenu.add(new Separator());
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ITextEditorActionDefinitionIds.LINE_GOTO));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_FIRST));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_NEXT));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_PREVIOUS));
                navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_LAST));

                manager.add(navigateMenu);
            }
            {
                // Layout
                MenuManager layoutMenu = new MenuManager(
                    "Layout",
                    null,
                    "layout"); //$NON-NLS-1$
                layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_MODE));
                layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_PANELS));
                layoutMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_SWITCH_PRESENTATION));
                {
                    layoutMenu.add(new Separator());
                    for (IContributionItem item : fillPanelsMenu()) {
                        layoutMenu.add(item);
                    }
                }
                manager.add(layoutMenu);

            }
            manager.add(new Separator());
        }

        // Fill general menu
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null && model.hasData()) {
            manager.add(new Action(CoreMessages.controls_resultset_viewer_action_export, DBeaverIcons.getImageDescriptor(UIIcon.EXPORT)) {
                @Override
                public void run() {
                    ActiveWizardDialog dialog = new ActiveWizardDialog(
                        site.getWorkbenchWindow(),
                        new DataTransferWizard(
                            new IDataTransferProducer[]{
                                new DatabaseTransferProducer(dataContainer, model.getDataFilter())},
                            null
                        ),
                        getSelection()
                    );
                    dialog.open();
                }
            });
        }
        manager.add(new GroupMarker(CoreCommands.GROUP_TOOLS));
        if (dataContainer != null && model.hasData()) {
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_REFRESH));
        }

        manager.add(new Separator());
        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private class TransformerAction extends Action {
        private final DBDAttributeBinding attrribute;
        public TransformerAction(DBDAttributeBinding attr, String text, int style, boolean checked) {
            super(text, style);
            this.attrribute = attr;
            setChecked(checked);
        }
        @NotNull
        DBVTransformSettings getTransformSettings() {
            final DBVTransformSettings settings = DBVUtils.getTransformSettings(attrribute, true);
            if (settings == null) {
                throw new IllegalStateException("Can't get/create transformer settings for '" + attrribute.getFullQualifiedName() + "'");
            }
            return settings;
        }
        protected void saveTransformerSettings() {
            attrribute.getDataSource().getContainer().persistConfiguration();
            refreshData(null);
        }
    }

    private void fillAttributeTransformersMenu(IMenuManager manager, final DBDAttributeBinding attr) {
        final DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return;
        }
        final DBPDataSource dataSource = dataContainer.getDataSource();
        final DBDRegistry registry = dataSource.getContainer().getApplication().getValueHandlerRegistry();
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

    private void fillFiltersMenu(@NotNull DBDAttributeBinding attribute, @NotNull IMenuManager filtersMenu)
    {
        if (supportsDataFilter()) {
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
        filtersMenu.add(new Separator());
        filtersMenu.add(new ToggleServerSideOrderingAction());
        filtersMenu.add(new ShowFiltersAction(true));
    }

    @Override
    public void navigateAssociation(@NotNull DBRProgressMonitor monitor, @NotNull DBDAttributeBinding attr, @NotNull ResultSetRow row, boolean newWindow)
        throws DBException
    {
        if (getExecutionContext() == null) {
            throw new DBException("Not connected");
        }
        DBSEntityAssociation association = null;
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
            throw new DBException("Entity [" + DBUtils.getObjectFullName(targetEntity) + "] is not a data container");
        }

        // make constraints
        List<DBDAttributeConstraint> constraints = new ArrayList<>();
        int visualPosition = 0;
        // Set conditions
        List<? extends DBSEntityAttributeRef> ownAttrs = CommonUtils.safeList(((DBSEntityReferrer) association).getAttributeReferences(monitor));
        List<? extends DBSEntityAttributeRef> refAttrs = CommonUtils.safeList(((DBSEntityReferrer) refConstraint).getAttributeReferences(monitor));
        if (ownAttrs.size() != refAttrs.size()) {
            throw new DBException(
                "Entity [" + DBUtils.getObjectFullName(targetEntity) + "] association [" + association.getName() +
                    "] columns differs from referenced constraint [" + refConstraint.getName() + "] (" + ownAttrs.size() + "<>" + refAttrs.size() + ")");
        }
        // Add association constraints
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
        }
        DBDDataFilter newFilter = new DBDDataFilter(constraints);

        if (newWindow) {
            openResultsInNewWindow(monitor, targetEntity, newFilter);
        } else {
            runDataPump((DBSDataContainer) targetEntity, newFilter, 0, getSegmentMaxRows(), -1, null);
        }
    }

    private void openResultsInNewWindow(DBRProgressMonitor monitor, DBSEntity targetEntity, final DBDDataFilter newFilter) {
        final DBNDatabaseNode targetNode = getExecutionContext().getDataSource().getContainer().getApplication().getNavigatorModel().getNodeByObject(monitor, targetEntity, false);
        if (targetNode == null) {
            UIUtils.showMessageBox(null, "Open link", "Can't navigate to '" + DBUtils.getObjectFullName(targetEntity) + "' - navigator node not found", SWT.ICON_ERROR);
            return;
        }
        UIUtils.runInDetachedUI(null, new Runnable() {
            @Override
            public void run() {
                openNewDataEditor(targetNode, newFilter);
            }
        });
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

        runDataPump(state.dataContainer, state.filter, 0, segmentSize, state.rowNumber, null);
    }

    @Override
    public void updatePanelsContent() {
        updateEditControls();
        for (IResultSetPanel panel : getActivePanels()) {
            panel.refresh();
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
            if (selection instanceof IResultSetSelection) {
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
    public DBDDataReceiver getDataReceiver() {
        return dataReceiver;
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return container.getExecutionContext();
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

        // Pump data
        ResultSetRow oldRow = curRow;

        DBSDataContainer dataContainer = getDataContainer();
        if (container.isReadyToRun() && dataContainer != null && dataPumpJob == null) {
            int segmentSize = getSegmentMaxRows();
            if (oldRow != null && oldRow.getVisualNumber() >= segmentSize && segmentSize > 0) {
                segmentSize = (oldRow.getVisualNumber() / segmentSize + 1) * segmentSize;
            }
            runDataPump(dataContainer, null, 0, segmentSize, oldRow == null ? -1 : oldRow.getVisualNumber(), new Runnable() {
                @Override
                public void run()
                {
                    activePresentation.formatData(true);
                }
            });
        } else {
            UIUtils.showErrorDialog(
                null,
                "Error executing query",
                dataContainer == null ?
                    "Viewer detached from data source" :
                    dataPumpJob == null ?
                        "Can't refresh after reconnect. Re-execute query." :
                        "Previous query is still running");
        }
    }

    public void refreshWithFilter(DBDDataFilter filter) {
        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null) {
            runDataPump(
                dataContainer,
                filter,
                0,
                getSegmentMaxRows(),
                -1,
                null);
        }
    }

    @Override
    public boolean refreshData(@Nullable Runnable onSuccess) {
        DBSDataContainer dataContainer = getDataContainer();
        if (container.isReadyToRun() && dataContainer != null && dataPumpJob == null) {
            int segmentSize = getSegmentMaxRows();
            if (curRow != null && curRow.getVisualNumber() >= segmentSize && segmentSize > 0) {
                segmentSize = (curRow.getVisualNumber() / segmentSize + 1) * segmentSize;
            }
            return runDataPump(dataContainer, null, 0, segmentSize, -1, onSuccess);
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
                null,
                model.getRowCount(),
                getSegmentMaxRows(),
                -1,//curRow == null ? -1 : curRow.getRowNumber(), // Do not reposition cursor after next segment read!
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
                null,
                model.getRowCount(),
                -1,
                curRow == null ? -1 : curRow.getRowNumber(),
                null);
        }
    }

    int getSegmentMaxRows()
    {
        if (getDataContainer() == null) {
            return 0;
        }
        return getPreferenceStore().getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS);
    }

    synchronized boolean runDataPump(
        @NotNull final DBSDataContainer dataContainer,
        @Nullable final DBDDataFilter dataFilter,
        final int offset,
        final int maxRows,
        final int focusRow,
        @Nullable final Runnable finalizer)
    {
        if (dataPumpJob != null) {
            UIUtils.showMessageBox(viewerPanel.getShell(), "Data read", "Data read is in progress - can't run another", SWT.ICON_WARNING);
            return false;
        }
        // Read data
        final DBDDataFilter useDataFilter = dataFilter != null ? dataFilter :
            (dataContainer == getDataContainer() ? model.getDataFilter() : null);
        Composite progressControl = viewerPanel;
        if (activePresentation.getControl() instanceof Composite) {
            progressControl = (Composite) activePresentation.getControl();
        }
        final Object presentationState = savePresentationState();
        dataPumpJob = new ResultSetDataPumpJob(
            dataContainer,
            useDataFilter,
            this,
            getExecutionContext(),
            progressControl);
        dataPumpJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void aboutToRun(IJobChangeEvent event) {
                model.setUpdateInProgress(true);
                getControl().getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        filtersPanel.enableFilters(false);
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
                final Control control = getControl();
                if (control.isDisposed()) {
                    return;
                }
                control.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run()
                    {
                        try {
                            if (control.isDisposed()) {
                                return;
                            }
                            final Shell shell = control.getShell();
                            if (error != null) {
                                //setStatus(error.getMessage(), true);
                                UIUtils.showErrorDialog(
                                    shell,
                                    "Error executing query",
                                    "Query execution failed",
                                    error);
                            } else if (focusRow >= 0 && focusRow < model.getRowCount() && model.getVisibleAttributeCount() > 0) {
                                // Seems to be refresh
                                // Restore original position
                                curRow = model.getRow(focusRow);
                                //curAttribute = model.getVisibleAttribute(0);
                                if (recordMode) {
                                    updateRecordMode();
                                } else {
                                    updateStatusMessage();
                                }
                                restorePresentationState(presentationState);
                            }
                            activePresentation.updateValueView();

                            if (error == null) {
                                setNewState(dataContainer, dataFilter != null ? dataFilter :
                                    (dataContainer == getDataContainer() ? model.getDataFilter() : null));
                            }

                            model.setUpdateInProgress(false);
                            if (error == null && dataFilter != null) {
                                model.updateDataFilter(dataFilter);
                                activePresentation.refreshData(true, false);
                            }
                            updateFiltersText(error == null);
                            updateToolbar();
                            fireResultSetLoad();
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
    public boolean applyChanges(@Nullable DBRProgressMonitor monitor, @Nullable ResultSetPersister.DataUpdateListener listener)
    {
        try {
            ResultSetPersister persister = createDataPersister(false);
            return persister.applyChanges(monitor, false, listener);
        } catch (DBException e) {
            UIUtils.showErrorDialog(null, "Apply changes error", "Error saving changes in database", e);
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
            UIUtils.showErrorDialog(null, "SQL script generate error", "Error saving changes in database", e);
            return Collections.emptyList();
        }
    }

    @NotNull
    private ResultSetPersister createDataPersister(boolean skipKeySearch)
        throws DBException
    {
        if (!skipKeySearch && !model.isSingleSource()) {
            throw new DBException("Can't save data for result set from multiple sources");
        }
        boolean needPK = false;
        if (!skipKeySearch) {
            for (ResultSetRow row : model.getAllRows()) {
                if (row.getState() == ResultSetRow.STATE_REMOVED || (row.getState() == ResultSetRow.STATE_NORMAL && row.isChanged())) {
                    needPK = true;
                    break;
                }
            }
        }
        if (needPK) {
            // If we have deleted or updated rows then check for unique identifier
            if (!checkEntityIdentifier()) {
                throw new DBException("No unique identifier defined");
            }
        }
        return new ResultSetPersister(this);
    }

    void addNewRow(final boolean copyCurrent, boolean afterCurrent)
    {
        int rowNum = curRow == null ? 0 : curRow.getVisualNumber();
        if (rowNum >= model.getRowCount()) {
            rowNum = model.getRowCount() - 1;
        }
        if (rowNum < 0) {
            rowNum = 0;
        }

        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            return;
        }

        // Add new row
        final DBDAttributeBinding docAttribute = model.getDocumentAttribute();
        final DBDAttributeBinding[] attributes = model.getAttributes();
        final Object[] cells;
        final int currentRowNumber = rowNum;
        // Copy cell values in new context
        try (DBCSession session = executionContext.openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, CoreMessages.controls_resultset_viewer_add_new_row_context_name)) {
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
                        DBSAttributeBase attribute = metaAttr.getAttribute();
                        if (attribute.isAutoGenerated() || attribute.isPseudoAttribute()) {
                            // set pseudo and autoincrement attributes to null
                            cells[i] = null;
                        } else {
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
        }
        curRow = model.addNewRow(afterCurrent ? rowNum + 1 : rowNum, cells);
        redrawData(true);
        updateEditControls();
        fireResultSetChange();
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
        redrawData(rowsRemoved > 0);
        // Move one row down (if we are in grid mode)
        if (!recordMode && lastRowNum < model.getRowCount() - 1) {
            activePresentation.scrollToRow(IResultSetPresentation.RowPosition.NEXT);
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

    boolean checkEntityIdentifier() throws DBException
    {
        DBSEntity entity = model.getSingleSource();
        if (entity == null) {
            UIUtils.showErrorDialog(
                null,
                "Unrecognized entity",
                "Can't detect source entity");
            return false;
        }
        final DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            return false;
        }
        // Check for value locators
        // Probably we have only virtual one with empty attribute set
        final DBDRowIdentifier identifier = getVirtualEntityIdentifier();
        if (identifier != null) {
            if (CommonUtils.isEmpty(identifier.getAttributes())) {
                // Empty identifier. We have to define it
                RunnableWithResult<Boolean> confirmer = new RunnableWithResult<Boolean>() {
                    @Override
                    public void run()
                    {
                        result = ValidateUniqueKeyUsageDialog.validateUniqueKey(ResultSetViewer.this, executionContext);
                    }
                };
                UIUtils.runInUI(null, confirmer);
                return confirmer.getResult();
            }
        }
        {
            // Check attributes of non-virtual identifier
            DBDRowIdentifier rowIdentifier = model.getVisibleAttribute(0).getRowIdentifier();
            if (rowIdentifier == null) {
                // We shouldn't be here ever!
                // Virtual id should be created if we missing natural one
                UIUtils.showErrorDialog(
                    null,
                    "No entity identifier",
                    "Entity " + entity.getName() + " has no unique key");
                return false;
            } else if (CommonUtils.isEmpty(rowIdentifier.getAttributes())) {
                UIUtils.showErrorDialog(
                    null,
                    "No entity identifier",
                    "Attributes of '" + DBUtils.getObjectFullName(rowIdentifier.getUniqueKey()) + "' are missing in result set");
                return false;
            }
        }
        return true;
    }

    boolean editEntityIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        DBDRowIdentifier virtualEntityIdentifier = getVirtualEntityIdentifier();
        if (virtualEntityIdentifier == null) {
            log.warn("No virtual identifier");
            return false;
        }
        DBVEntityConstraint constraint = (DBVEntityConstraint) virtualEntityIdentifier.getUniqueKey();

        EditConstraintDialog dialog = new EditConstraintDialog(
            getControl().getShell(),
            "Define virtual unique identifier",
            constraint);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        Collection<DBSEntityAttribute> uniqueAttrs = dialog.getSelectedAttributes();
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

    void clearEntityIdentifier(DBRProgressMonitor monitor) throws DBException
    {
        DBDAttributeBinding firstAttribute = model.getVisibleAttribute(0);
        DBDRowIdentifier rowIdentifier = firstAttribute.getRowIdentifier();
        if (rowIdentifier != null) {
            DBVEntityConstraint virtualKey = (DBVEntityConstraint) rowIdentifier.getUniqueKey();
            virtualKey.setAttributes(Collections.<DBSEntityAttribute>emptyList());
            rowIdentifier.reloadAttributes(monitor, model.getAttributes());
            virtualKey.getParentObject().setProperty(DBVConstants.PROPERTY_USE_VIRTUAL_KEY_QUIET, null);
        }

        persistConfig();
    }

    public void fireResultSetChange() {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (IResultSetListener listener : listeners) {
                    listener.handleResultSetChange();
                }
            }
        }
    }

    public void fireResultSetLoad() {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (IResultSetListener listener : listeners) {
                    listener.handleResultSetLoad();
                }
            }
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
        public Collection<DBDAttributeBinding> getSelectedAttributes() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<ResultSetRow> getSelectedRows() {
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
                DBeaverUI.getActiveWorkbenchWindow().getActivePage().getActivePart());
            if (rsv == null) {
                return new IContributionItem[0];
            }
            List<IContributionItem> items = rsv.fillPanelsMenu();
            return items.toArray(new IContributionItem[items.size()]);
        }
    }

    private class ConfigAction extends Action implements IMenuCreator {
        public ConfigAction()
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
            menuManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_MODE, CommandContributionItem.STYLE_CHECK));
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
        public ShowFiltersAction(boolean context)
        {
            super(context ? "Customize ..." : "Order/Filter ...", DBeaverIcons.getImageDescriptor(UIIcon.FILTER));
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
                } catch (DBCException e) {
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
        public FilterByAttributeAction(DBCLogicalOperator operator, FilterByAttributeType type, DBDAttributeBinding attribute)
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

    private class FilterResetAttributeAction extends Action {
        private final DBDAttributeBinding attribute;
        public FilterResetAttributeAction(DBDAttributeBinding attribute)
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

    private abstract class ColorAction extends Action {
        protected ColorAction(String name) {
            super(name);
        }
        @NotNull
        protected DBVEntity getVirtualEntity(DBDAttributeBinding binding) {
            final DBSEntity entity = getModel().getSingleSource();
            if (entity == null) {
                throw new IllegalStateException("No virtual entity for multi-source query");
            }
            final DBVEntity vEntity = DBVUtils.findVirtualEntity(entity, true);
            assert vEntity != null;
            return vEntity;
        }

        protected void updateColors(DBVEntity entity) {
            model.updateColorMapping();
            redrawData(false);
            entity.getDataSource().getContainer().persistConfiguration();
        }
    }

    private class SetRowColorAction extends ColorAction {
        private final DBDAttributeBinding attribute;
        private final Object value;
        public SetRowColorAction(DBDAttributeBinding attr, Object value) {
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
            final DBVEntity vEntity = getVirtualEntity(attribute);
            vEntity.setColorOverride(attribute, value, null, StringConverter.asString(color));
            updateColors(vEntity);
        }
    }

    private class ResetRowColorAction extends ColorAction {
        private final DBDAttributeBinding attribute;
        public ResetRowColorAction(DBDAttributeBinding attr, Object value) {
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

    private class CustomizeColorsAction extends ColorAction {
        private final DBDAttributeBinding curAttribute;
        private final ResultSetRow row;

        public CustomizeColorsAction() {
            this(null, null);
        }

        public CustomizeColorsAction(DBDAttributeBinding curAttribute, ResultSetRow row) {
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

        public VirtualKeyEditAction(boolean define)
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
            DBeaverUI.runUIJob("Edit virtual key", new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
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
            EditDictionaryDialog dialog = new EditDictionaryDialog(
                getSite().getShell(),
                "Edit dictionary",
                model.getSingleSource());
            dialog.open();
        }

        @Override
        public boolean isEnabled()
        {
            final DBSEntity singleSource = model.getSingleSource();
            return singleSource != null;
        }
    }

    private class PresentationSwitchCombo extends ContributionItem implements SelectionListener {
        private ToolItem toolitem;
        private CImageCombo combo;

        @Override
        public void fill(ToolBar parent, int index) {
            toolitem = new ToolItem(parent, SWT.SEPARATOR, index);
            Control control = createControl(parent);
            toolitem.setControl(control);
        }

        @Override
        public void fill(Composite parent) {
            createControl(parent);
        }

        protected Control  createControl(Composite parent) {
            combo = new CImageCombo(parent, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
            combo.add(DBeaverIcons.getImage(DBIcon.TYPE_UNKNOWN), "", null, null);
            final int textWidth = parent.getFont().getFontData()[0].getHeight() * 10;
            combo.setWidthHint(textWidth);
            if (toolitem != null) {
                toolitem.setWidth(textWidth);
            }
            combo.addSelectionListener(this);
            combo.setToolTipText(ActionUtils.findCommandDescription(ResultSetCommandHandler.CMD_SWITCH_PRESENTATION, getSite(), false));
            return combo;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            ResultSetPresentationDescriptor selectedPresentation = (ResultSetPresentationDescriptor) combo.getData(combo.getSelectionIndex());
            if (activePresentationDescriptor == selectedPresentation) {
                return;
            }
            switchPresentation(selectedPresentation);
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {

        }
    }

    class HistoryStateItem {
        DBSDataContainer dataContainer;
        DBDDataFilter filter;
        int rowNumber;

        public HistoryStateItem(DBSDataContainer dataContainer, @Nullable DBDDataFilter filter, int rowNumber) {
            this.dataContainer = dataContainer;
            this.filter = filter;
            this.rowNumber = rowNumber;
        }

        public String describeState() {
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
        Set<String> enabledPanelIds = new HashSet<>();
        String activePanelId;
        int panelRatio;
        boolean panelsVisible;
    }

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

}
