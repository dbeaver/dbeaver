/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVConstants;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
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
import org.jkiss.dbeaver.ui.preferences.PrefPageDatabaseGeneral;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ResultSetViewer
 *
 * TODO: fix presentation contributions to toolbar
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
    static final Log log = Log.getLog(ResultSetViewer.class);

    @NotNull
    private static IResultSetFilterManager filterManager = new SimpleFilterManager();

    public static void registerFilterManager(@Nullable IResultSetFilterManager filterManager) {
        if (filterManager == null) {
            filterManager = new SimpleFilterManager();
        }
        ResultSetViewer.filterManager = filterManager;
    }

    @NotNull
    private final IWorkbenchPartSite site;
    private final Composite viewerPanel;
    private ResultSetFilterPanel filtersPanel;
    private final Composite presentationPanel;
    private Text statusLabel;

    private final DynamicFindReplaceTarget findReplaceTarget;

    // Presentation
    private IResultSetPresentation activePresentation;
    private ResultSetPresentationDescriptor activePresentationDescriptor;
    private List<ResultSetPresentationDescriptor> availablePresentations;
    private PresentationSwitchCombo presentationSwitchCombo;

    @NotNull
    private final IResultSetContainer container;
    @NotNull
    private final ResultSetDataReceiver dataReceiver;

    private ToolBarManager toolBarManager;

    // Current row/col number
    @Nullable
    private ResultSetRow curRow;
    // Mode
    private boolean recordMode;

    private final List<IResultSetListener> listeners = new ArrayList<>();

    private volatile ResultSetDataPumpJob dataPumpJob;

    private final ResultSetModel model = new ResultSetModel();
    private StateItem curState = null;
    private final List<StateItem> stateHistory = new ArrayList<>();
    private int historyPosition = -1;

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

        this.viewerPanel = UIUtils.createPlaceholder(parent, 1);
        UIUtils.setHelp(this.viewerPanel, IHelpContextIds.CTX_RESULT_SET_VIEWER);

        this.filtersPanel = new ResultSetFilterPanel(this);
        this.findReplaceTarget = new DynamicFindReplaceTarget();
        this.presentationPanel = UIUtils.createPlaceholder(viewerPanel, 1);
        this.presentationPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

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

                if (container.isReadyToRun() &&
                    !model.isUpdateInProgress() &&
                    (!CommonUtils.isEmpty(whereCondition) || (model.getVisibleAttributeCount() > 0 && supportsDataFilter())))
                {
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

    private void persistConfig() {
        DBCExecutionContext context = getExecutionContext();
        if (context != null) {
            context.getDataSource().getContainer().persistConfiguration();
        }
    }

    @Override
    public Color getDefaultBackground() {
        return filtersPanel.getEditControl().getBackground();
    }

    @Override
    public Color getDefaultForeground() {
        return filtersPanel.getEditControl().getForeground();
    }

    public List<ResultSetPresentationDescriptor> getAvailablePresentations() {
        return availablePresentations;
    }

    @Override
    public IResultSetPresentation getActivePresentation() {
        return activePresentation;
    }

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
    }

    private boolean checkDoubleLock(Control lockedBy) {
        if (actionsDisabled) {
            log.error("Double-lock actions by [" + lockedBy + "]");
            return true;
        }
        return false;
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

    private void setActivePresentation(IResultSetPresentation presentation) {
        // Dispose previous presentation
        for (Control child : presentationPanel.getChildren()) {
            child.dispose();
        }
        // Set new one
        activePresentation = presentation;
        activePresentation.createPresentation(this, presentationPanel);
        presentationPanel.layout();

        // Update dynamic find/replace target
        {
            IFindReplaceTarget nested = null;
            if (presentation instanceof IAdaptable) {
                nested = ((IAdaptable) presentation).getAdapter(IFindReplaceTarget.class);
            }
            findReplaceTarget.setTarget(nested);
        }

        // Update toolbar
        if (toolBarManager != null) {
            // Cleanup previous presentation contributions
            boolean presContributions = false;
            for (IContributionItem item : toolBarManager.getItems()) {
                if (presContributions) {
                    if (IResultSetPresentation.PRES_TOOLS_END.equals(item.getId())) {
                        break;
                    }
                    toolBarManager.remove(item);
                } else if (IResultSetPresentation.PRES_TOOLS_BEGIN.equals(item.getId())) {
                    presContributions = true;
                }
            }


            // fill presentation contributions
            activePresentation.fillToolbar(toolBarManager);
            toolBarManager.update(true);
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
        } catch (Throwable e1) {
            UIUtils.showErrorDialog(
                viewerPanel.getShell(),
                "Presentation switch",
                "Can't switch presentation",
                e1);
        }
    }

    @Nullable
    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == IFindReplaceTarget.class) {
            return findReplaceTarget;
        }
        if (adapter.isAssignableFrom(activePresentation.getClass())) {
            return activePresentation;
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
        if (toolBarManager.isEmpty()) {
            return;
        }
        for (IContributionItem item : toolBarManager.getItems()) {
            item.update();
        }
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

        toolBarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

        // Add presentation switcher
        presentationSwitchCombo = new PresentationSwitchCombo();
        toolBarManager.add(presentationSwitchCombo);
        toolBarManager.add(new Separator());

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
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_PAGE));
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_FETCH_ALL));
        // Use simple action for refresh to avoid ambiguous behaviour of F5 shortcut
        toolBarManager.add(new Separator());
//        // FIXME: Link to standard Find/Replace action - it has to be handled by owner site
//        toolBarManager.add(ActionUtils.makeCommandContribution(
//            site,
//            IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE,
//            CommandContributionItem.STYLE_PUSH,
//            UIIcon.FIND_TEXT));

        toolBarManager.add(new Separator());
        toolBarManager.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_TOGGLE_MODE, CommandContributionItem.STYLE_CHECK));
        toolBarManager.add(new GroupMarker(IResultSetPresentation.PRES_TOOLS_BEGIN));
        toolBarManager.add(new GroupMarker(IResultSetPresentation.PRES_TOOLS_END));

        toolBarManager.add(new ConfigAction());
        toolBarManager.createControl(statusBar);

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
        clearData();

        if (toolBarManager != null) {
            try {
                toolBarManager.dispose();
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

    int getHistoryPosition() {
        return historyPosition;
    }

    List<StateItem> getStateHistory() {
        return stateHistory;
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

    void navigateHistory(int position) {
        StateItem state = stateHistory.get(position);
        int segmentSize = getSegmentMaxRows();
        if (state.rowNumber >= 0 && state.rowNumber >= segmentSize && segmentSize > 0) {
            segmentSize = (state.rowNumber / segmentSize + 1) * segmentSize;
        }

        runDataPump(state.dataContainer, state.filter, 0, segmentSize, state.rowNumber, null);
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
        if (model.isMetadataChanged()) {
            activePresentation.clearMetaData();
        }
    }

    void setData(List<Object[]> rows)
    {
        if (viewerPanel.isDisposed()) {
            return;
        }
        boolean metaChanged = model.isMetadataChanged();

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
        }
        this.activePresentation.refreshData(metaChanged, false);
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

    @Override
    public void fillContextMenu(@NotNull IMenuManager manager, @Nullable final DBDAttributeBinding attr, @Nullable final ResultSetRow row)
    {
        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        // Custom oldValue items
        if (attr != null && row != null) {
            final ResultSetValueController valueController = new ResultSetValueController(
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
                                BaseValueManager.makeNullValue(valueController));
                        }
                    });
                }
                manager.add(new GroupMarker(MENU_GROUP_EDIT));
            }

            // Menus from value handler
            try {
                manager.add(new Separator());
                valueController.getValueManager().contributeActions(manager, valueController);
            }
            catch (Exception e) {
                log.error(e);
            }

            if (row.isChanged()) {
                Action resetValueAction = new Action(CoreMessages.controls_resultset_viewer_action_reset_value) {
                    @Override
                    public void run() {
                        model.resetCellValue(attr, row);
                        updateValueView();
                    }
                };
                resetValueAction.setAccelerator(SWT.ESC);
                manager.insertAfter(IResultSetController.MENU_GROUP_EDIT, resetValueAction);
            }
        }

        if (attr != null && model.getVisibleAttributeCount() > 0 && !model.isUpdateInProgress()) {
            // Export and other utility methods
            manager.add(new Separator());
            MenuManager filtersMenu = new MenuManager(
                CoreMessages.controls_resultset_viewer_action_order_filter,
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
        manager.add(new GroupMarker(ICommandIds.GROUP_TOOLS));
        if (dataContainer != null && model.hasData()) {
            manager.add(new Separator());
            manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_REFRESH));
        }
    }

    private void fillFiltersMenu(@NotNull DBDAttributeBinding attribute, @NotNull IMenuManager filtersMenu)
    {
        if (supportsDataFilter()) {
            DBCLogicalOperator[] operators = attribute.getValueHandler().getSupportedOperators(attribute);
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
        filtersMenu.add(new ShowFiltersAction());
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
        if (!(refConstraint instanceof DBSEntityReferrer)) {
            throw new DBException("Referenced constraint [" + refConstraint + "] is no a referrer");
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
    public void updateValueView() {
        activePresentation.updateValueView();
        updateEditControls();
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
    @Nullable
    public IResultSetSelection getSelection()
    {
        if (activePresentation instanceof ISelectionProvider) {
            ISelection selection = ((ISelectionProvider) activePresentation).getSelection();
            if (selection instanceof IResultSetSelection) {
                return (IResultSetSelection) selection;
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

    @NotNull
    @Override
    public IResultSetFilterManager getFilterManager() {
        return filterManager;
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
                viewerPanel.getShell(),
                "Error executing query",
                dataContainer == null ?
                    "Viewer detached from data source" : "Can't refresh after reconnect. Re-execute query.");
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
    public void refreshData(@Nullable Runnable onSuccess) {
        DBSDataContainer dataContainer = getDataContainer();
        if (container.isReadyToRun() && dataContainer != null && dataPumpJob == null) {
            int segmentSize = getSegmentMaxRows();
            if (curRow != null && curRow.getVisualNumber() >= segmentSize && segmentSize > 0) {
                segmentSize = (curRow.getVisualNumber() / segmentSize + 1) * segmentSize;
            }
            runDataPump(dataContainer, null, 0, segmentSize, -1, onSuccess);
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

    synchronized void runDataPump(
        @NotNull final DBSDataContainer dataContainer,
        @Nullable final DBDDataFilter dataFilter,
        final int offset,
        final int maxRows,
        final int focusRow,
        @Nullable final Runnable finalizer)
    {
        if (dataPumpJob != null) {
            UIUtils.showMessageBox(viewerPanel.getShell(), "Data read", "Data read is in progress - can't run another", SWT.ICON_WARNING);
            return;
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
                                activePresentation.refreshData(false, false);
                            }
                            updateFiltersText(error == null);
                            updateToolbar();
                            fireResultSetLoad();

                            if (finalizer != null) {
                                finalizer.run();
                            }
                        } finally {
                            dataPumpJob = null;
                        }
                    }
                });
            }
        });
        dataPumpJob.setOffset(offset);
        dataPumpJob.setMaxRows(maxRows);
        dataPumpJob.schedule();
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
        if (!model.isSingleSource()) {
            UIUtils.showErrorDialog(getControl().getShell(), "Apply changes error", "Can't save data for result set from multiple sources");
            return false;
        }
        try {
            boolean needPK = false;
            for (ResultSetRow row : model.getAllRows()) {
                if (row.getState() == ResultSetRow.STATE_REMOVED || (row.getState() == ResultSetRow.STATE_NORMAL && row.isChanged())) {
                    needPK = true;
                    break;
                }
            }
            if (needPK) {
                // If we have deleted or updated rows then check for unique identifier
                if (!checkEntityIdentifier()) {
                    //UIUtils.showErrorDialog(getControl().getShell(), "Can't apply changes", "Can't apply data changes - not unique identifier defined");
                    return false;
                }
            }
            return new ResultSetPersister(this).applyChanges(monitor, listener);
        } catch (DBException e) {
            UIUtils.showErrorDialog(getControl().getShell(), "Apply changes error", "Error saving changes in database", e);
            return false;
        }
    }

    @Override
    public void rejectChanges()
    {
        new ResultSetPersister(this).rejectChanges();
    }


    void addNewRow(final boolean copyCurrent)
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
        curRow = model.addNewRow(rowNum, cells);
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
            if (selection != null && !selection.isEmpty()) {
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
                viewerPanel.getShell(),
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
                    viewerPanel.getShell(),
                    "No entity identifier",
                    "Entity " + entity.getName() + " has no unique key");
                return false;
            } else if (CommonUtils.isEmpty(rowIdentifier.getAttributes())) {
                UIUtils.showErrorDialog(
                    viewerPanel.getShell(),
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
        @Override
        public IResultSetController getController() {
            return ResultSetViewer.this;
        }

        @Override
        public Collection<ResultSetRow> getSelectedRows() {
            return Collections.emptyList();
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
            menuManager.add(new ShowFiltersAction());
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
        public ShowFiltersAction()
        {
            super(CoreMessages.controls_resultset_viewer_action_order_filter, DBeaverIcons.getImageDescriptor(UIIcon.FILTER));
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
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding attribute, DBCLogicalOperator operator, boolean useDefault)
            {
                final DBDAttributeBinding attr = attribute;
                final ResultSetRow row = viewer.getCurrentRow();
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
        INPUT(UIIcon.FILTER_INPUT) {
            @Override
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding attribute, DBCLogicalOperator operator, boolean useDefault)
            {
                if (useDefault) {
                    return "..";
                } else {
                    ResultSetRow focusRow = viewer.getCurrentRow();
                    if (focusRow == null) {
                        return null;
                    }
                    FilterValueEditDialog dialog = new FilterValueEditDialog(viewer, attribute, focusRow, operator);
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
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding attribute, DBCLogicalOperator operator, boolean useDefault)
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
            Object getValue(ResultSetViewer viewer, DBDAttributeBinding attribute, DBCLogicalOperator operator, boolean useDefault)
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
        abstract Object getValue(ResultSetViewer viewer, DBDAttributeBinding attribute, DBCLogicalOperator operator, boolean useDefault);
    }

    private String translateFilterPattern(DBCLogicalOperator operator, FilterByAttributeType type, DBDAttributeBinding attribute)
    {
        Object value = type.getValue(this, attribute, operator, true);
        DBCExecutionContext executionContext = getExecutionContext();
        String strValue = executionContext == null ? String.valueOf(value) : SQLUtils.convertValueToSQL(executionContext.getDataSource(), attribute, value);
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
            if (operator.getArgumentCount() > 0 && value == null) {
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
            toolitem.setWidth(100);
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

    class StateItem {
        DBSDataContainer dataContainer;
        DBDDataFilter filter;
        int rowNumber;

        public StateItem(DBSDataContainer dataContainer, @Nullable DBDDataFilter filter, int rowNumber) {
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
                ResultSetViewer rsv = ((IResultSetContainer) selectedPage).getResultSetViewer();
                if (rsv != null && !rsv.isRefreshInProgress() && !newFilter.equals(rsv.getModel().getDataFilter())) {
                    // Set filter directly
                    rsv.refreshWithFilter(newFilter);
                }
            }
        }
    }

}
