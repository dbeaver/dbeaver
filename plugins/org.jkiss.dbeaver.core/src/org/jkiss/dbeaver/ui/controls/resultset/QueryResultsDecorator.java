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
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.resultset.view.StatisticsPresentation;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.preferences.PrefPageDataFormat;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

/**
 * Decorator for query results
 */
public abstract class QueryResultsDecorator implements IResultSetDecorator
{
    private static final Log log = Log.getLog(QueryResultsDecorator.class);

    private static final String TOOLBAR_GROUP_NAVIGATION = "navigation";
    private static final String TOOLBAR_GROUP_PRESENTATIONS = "presentations";
    private static final String TOOLBAR_GROUP_ADDITIONS = IWorkbenchActionConstants.MB_ADDITIONS;

    private static final String SETTINGS_SECTION_PRESENTATIONS = "presentations";

    private static final DecimalFormat ROW_COUNT_FORMAT = new DecimalFormat("###,###,###,###,###,##0");

    private IResultSetController controller;

    private IResultSetFilterManager filterManager;
    @NotNull
    private ResultSetFilterPanel filtersPanel;

    private ToolBarManager panelToolBar;
    private ToolBar presentationSwitchToolbar;
    private final List<ToolBarManager> toolbarList = new ArrayList<>();
    private Composite statusBar;
    private StatusLabel statusLabel;
    private ActiveStatusMessage rowCountLabel;

/*
    @Override
    public void attach(IResultSetController controller) {
        this.controller = controller;
    }

    @Override
    public Composite createTopBar(Composite parent) {
        this.filtersPanel = new ResultSetFilterPanel((ResultSetViewer) controller);

        return this.filtersPanel;
    }

    @Override
    public void setFocus() {
        filtersPanel.getEditControl().setFocus();
    }

    private void updateFiltersText()
    {
        updateFiltersText(true);
    }

    public void updateFiltersText(boolean resetFilterValue)
    {
        if (this.controller.getControl().isDisposed()) {
            return;
        }

        this.controller.getControl().setRedraw(false);
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

                    if (controller.getContainer().isReadyToRun() && !controller.getModel().isUpdateInProgress()) {
                        enableFilters = true;
                    }
                }
            }
            filtersPanel.enableFilters(enableFilters);
            //presentationSwitchToolbar.setEnabled(enableFilters);
        } finally {
            this.controller.getControl().setRedraw(true);
        }
    }

    public void updateEditControls()
    {
        updateToolbar();
    }

    */
/**
     * It is a hack function. Generally all command associated widgets should be updated automatically by framework.
     * Freaking E4 do not do it. I've spent a couple of days fighting it. Guys, you owe me.
     *//*

    private void updateToolbar()
    {
        for (ToolBarManager tb : toolbarList) {
            UIUtils.updateContributionItems(tb);
        }
        UIUtils.updateContributionItems(panelToolBar);
    }

    @Override
    public Composite createBottomBar(Composite parent) {
        IWorkbenchPartSite site = controller.getSite();

        UIUtils.createHorizontalLine(parent);

        statusBar = new Composite(parent, SWT.NONE);
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
            addToolbar.add(new Separator(TOOLBAR_GROUP_PRESENTATIONS));
            addToolbar.add(new Separator(TOOLBAR_GROUP_ADDITIONS));
            final IMenuService menuService = getSite().getService(IMenuService.class);
            if (menuService != null) {
                menuService.populateContributionManager(addToolbar, "toolbar:org.jkiss.dbeaver.ui.controls.resultset.status");
            }
            addToolbar.update(true);
            addToolbar.createControl(statusBar);
            toolbarList.add(addToolbar);
        }

        {
            final int fontHeight = UIUtils.getFontHeight(statusBar);
            statusLabel = new StatusLabel(statusBar, SWT.NONE, viewer);
            statusLabel.setLayoutData(new RowData(40 * fontHeight, SWT.DEFAULT));

            rowCountLabel = new ActiveStatusMessage(statusBar, DBeaverIcons.getImage(UIIcon.RS_REFRESH), "Calculate total row count", viewer) {
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

    private void dispose()
    {
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

                if (valueController != null) {
                    manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.EDIT_PASTE));
                    manager.add(ActionUtils.makeCommandContribution(site, CoreCommands.CMD_PASTE_SPECIAL));

                    {
                        manager.add(new Separator());
                        MenuManager editMenu = new MenuManager(
                            CoreMessages.actions_menu_edit,
                            DBeaverIcons.getImageDescriptor(UIIcon.ROW_EDIT),
                            MENU_ID_EDIT); //$NON-NLS-1$

                        // Edit items
                        editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT));
                        editMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_ROW_EDIT_INLINE));
                        if (!valueController.isReadOnly() && !DBUtils.isNullValue(value)*/
/* && !attr.isRequired()*//*
) {
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
                    "View/Format",
                    null,
                    MENU_ID_VIEW); //$NON-NLS-1$

                List<? extends DBDAttributeTransformerDescriptor> transformers =
                    dataSource.getContainer().getPlatform().getValueHandlerRegistry().findTransformers(
                        dataSource, attr, null);
                if (!CommonUtils.isEmpty(transformers)) {
                    MenuManager transformersMenu = new MenuManager("View as");
                    transformersMenu.setRemoveAllWhenShown(true);
                    transformersMenu.addMenuListener(manager12 -> fillAttributeTransformersMenu(manager12, attr));
                    viewMenu.add(transformersMenu);
                } else {
                    final Action customizeAction = new Action("View as") {};
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
                viewMenu.add(new Action("Data formats ...") {
                    @Override
                    public void run() {
                        UIUtils.showPreferencesFor(
                            getControl().getShell(),
                            QueryResultsDecorator.this,
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
                boolean hasNavTables = false;
                if (ActionUtils.isCommandEnabled(ResultSetCommandHandler.CMD_NAVIGATE_LINK, site)) {
                    // Foreign key to some external table
                    navigateMenu.add(ActionUtils.makeCommandContribution(site, ResultSetCommandHandler.CMD_NAVIGATE_LINK));
                    hasNavTables = true;
                }
                if (model.isSingleSource()) {
                    // Add menu for referencing tables
                    MenuManager refTablesMenu = createRefTablesMenu(row);
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

                manager.add(navigateMenu);
            }
            {
                // Layout
                MenuManager layoutMenu = new MenuManager(
                    "Layout",
                    null,
                    "layout"); //$NON-NLS-1$
                layoutMenu.add(new ToggleModeAction());
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
    }

    @Nullable
    private MenuManager createRefTablesMenu(ResultSetRow row) {
        DBSEntity singleSource = model.getSingleSource();
        if (singleSource == null) {
            return null;
        }
        String menuName = ActionUtils.findCommandName(ResultSetCommandHandler.CMD_REFERENCES_MENU);

        MenuManager refTablesMenu = new MenuManager(menuName, null, "ref-tables");
        refTablesMenu.setActionDefinitionId(ResultSetCommandHandler.CMD_REFERENCES_MENU);
        refTablesMenu.add(NOREFS_ACTION);
        refTablesMenu.addMenuListener(manager -> fillRefTablesActions(row, singleSource, manager));

        return refTablesMenu;
    }

    private void fillRefTablesActions(ResultSetRow row, DBSEntity singleSource, IMenuManager manager) {

        DBRRunnableWithResult<List<DBSEntityAssociation>> refCollector = new DBRRunnableWithResult<List<DBSEntityAssociation>>() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    result = new ArrayList<>();
                    Collection<? extends DBSEntityAssociation> refs = singleSource.getReferences(monitor);
                    if (refs != null) {
                        for (DBSEntityAssociation ref : refs) {
                            boolean allMatch = true;
                            DBSEntityConstraint ownConstraint = ref.getReferencedConstraint();
                            if (ownConstraint instanceof DBSEntityReferrer) {
                                for (DBSEntityAttributeRef ownAttrRef : ((DBSEntityReferrer) ownConstraint).getAttributeReferences(monitor)) {
                                    if (model.getAttributeBinding(ownAttrRef.getAttribute()) == null) {
                                        // Attribute is not in the list - skip this association
                                        allMatch = false;
                                        break;
                                    }
                                }
                            }
                            if (allMatch) {
                                result.add(ref);
                            }
                        }
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        };
        try {
            DBeaverUI.runInProgressService(refCollector);
        } catch (InvocationTargetException e) {
            log.error("Error reading referencing tables for '" + singleSource.getName() + "'", e.getTargetException());
        } catch (InterruptedException e) {
            // Do nothing
        }
        manager.removeAll();
        if (CommonUtils.isEmpty(refCollector.getResult())) {
            manager.add(NOREFS_ACTION);
            return;
        }

        manager.add(REFS_TITLE_ACTION);
        manager.add(new Separator());
        for (DBSEntityAssociation refAssociation : refCollector.getResult()) {
            DBSEntity refTable = refAssociation.getParentObject();
            manager.add(new Action(
                DBUtils.getObjectFullName(refTable, DBPEvaluationContext.UI),
                DBeaverIcons.getImageDescriptor(DBSEntityType.TABLE.getIcon()))
            {
                @Override
                public void run() {
                    new AbstractJob("Navigate reference") {
                        @Override
                        protected IStatus run(DBRProgressMonitor monitor) {
                            try {
                                navigateReference(new VoidProgressMonitor(), refAssociation, row, false);
                            } catch (DBException e) {
                                return GeneralUtils.makeExceptionStatus(e);
                            }
                            return Status.OK_STATUS;
                        }
                    }.schedule();
                }
            });
        }
    }
*/

}
