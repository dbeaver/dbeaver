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
package org.jkiss.dbeaver.ui.editors.sql.plan;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.exec.plan.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.VerticalButton;
import org.jkiss.dbeaver.ui.controls.VerticalFolder;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanSaveProvider;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanViewProvider;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.plan.registry.SQLPlanViewDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.plan.registry.SQLPlanViewRegistry;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;

/**
 * ResultSetViewer
 */
public class ExplainPlanViewer extends Viewer implements IAdaptable
{
    static final Log log = Log.getLog(ExplainPlanViewer.class);

    private static class PlanViewInfo {
        private SQLPlanViewDescriptor descriptor;
        private SQLPlanViewProvider planViewer;
        private Viewer viewer;

        public PlanViewInfo(SQLPlanViewDescriptor descriptor) {
            this.descriptor = descriptor;
        }
    };


    private final IWorkbenchPart workbenchPart;
    private final DBPContextProvider contextProvider;
    private final ProgressControl planPresentationContainer;
    private final VerticalFolder tabViewFolder;
    private final Composite planViewComposite;

    private PlanViewInfo activeViewInfo;
    private SQLQuery lastQuery;
    private Object lastQueryId;
    private DBCPlan lastPlan;
    private int planNumber;

    private RefreshPlanAction refreshPlanAction;
    
    private String curFolder;

    public ExplainPlanViewer(final IWorkbenchPart workbenchPart, DBPContextProvider contextProvider, Composite parent, int planNumber)
    {
        this.workbenchPart = workbenchPart;
        this.contextProvider = contextProvider;
        this.planNumber = planNumber;

        this.refreshPlanAction = new RefreshPlanAction();
        this.refreshPlanAction.setEnabled(false);

        this.planPresentationContainer = new ProgressControl(parent);
        this.planPresentationContainer.getLayout().numColumns = 2;
        this.planPresentationContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            tabViewFolder = new VerticalFolder(planPresentationContainer, SWT.LEFT);
            ((GridLayout)tabViewFolder.getLayout()).marginTop = 20;
            tabViewFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));

            for (SQLPlanViewDescriptor viewDesc : SQLPlanViewRegistry.getInstance().getPlanViewDescriptors()) {
                VerticalButton treeViewButton = new VerticalButton(tabViewFolder, SWT.LEFT | SWT.RADIO);
                treeViewButton.setText(viewDesc.getLabel());
                if (!CommonUtils.isEmpty(viewDesc.getDescription())) {
                    treeViewButton.setToolTipText(viewDesc.getDescription());
                    if (viewDesc.getIcon() != null) {
                        treeViewButton.setImage(DBeaverIcons.getImage(viewDesc.getIcon()));
                    }
                }
                treeViewButton.setData(new PlanViewInfo(viewDesc));
                treeViewButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            }
            tabViewFolder.addListener(SWT.Selection, event -> {
                try {
                    changeActiveView(tabViewFolder.getSelection());
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError(
                        SQLEditorMessages.editors_sql_error_execution_plan_title,
                        SQLEditorMessages.editors_sql_error_execution_plan_message,
                        e);
                }
            });
        }
        {
            planViewComposite = new Composite(planPresentationContainer, SWT.NONE);
            planViewComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            planViewComposite.setLayout(new StackLayout());
        }

        planPresentationContainer.setShowDivider(false);
        Composite progressPanel = planPresentationContainer.createProgressPanel();
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        progressPanel.setLayoutData(gd);

        IDialogSettings settings = getPlanViewSettings();
        VerticalButton curItem = null;
        String activeViewId = settings.get("activeView");
        for (VerticalButton item : tabViewFolder.getItems()) {
            PlanViewInfo data = (PlanViewInfo) item.getData();
            if (curItem == null) {
                curItem = item;
            } else if (activeViewId != null && activeViewId.equals(data.descriptor.getId())) {
                curItem = item;
            }
        }
        tabViewFolder.setSelection(curItem);
    }

    public SQLQuery getQuery() {
        return lastQuery;
    }

    public int getPlanNumber() {
        return planNumber;
    }

    public boolean loadQueryPlan(DBCQueryPlanner planner, Viewer viewer) {
        if (planner instanceof DBCQueryPlannerSerializable) {

            FileDialog fd = new FileDialog(viewer.getControl().getShell(), SWT.OPEN | SWT.SINGLE);
            fd.setText(UIMessages.save_execution_plan);
            fd.setFilterPath(curFolder);
            fd.setFilterExtensions(SQLPlanSaveProvider.EXT);
            String selected = fd.open();
            if (selected != null) {

                curFolder = fd.getFilterPath();

                try (Reader r = new FileReader(selected)) {
                    lastPlan = ((DBCQueryPlannerSerializable) planner).deserialize(r);
                    lastQuery = new SQLQuery(
                        contextProvider.getExecutionContext().getDataSource(),
                            lastPlan.getQueryString());
                    lastQueryId = lastPlan.getQueryString();

                    refresh();

                    return true;

                } catch (IOException | InvocationTargetException e) {
                    DBWorkbench.getPlatformUI().showError("Load plan", "Error loading plan ",GeneralUtils.getRootCause(e));
                }
            }
        }
        return false;
    }

    public void explainQueryPlan(SQLQuery query, Object queryId) {
        this.lastQuery = query;
        this.lastQueryId = queryId;

        refresh();
    }

    private PlanViewInfo[] getPlanViews() {
        VerticalButton[] items = tabViewFolder.getItems();
        PlanViewInfo[] infos = new PlanViewInfo[items.length];
        for (int i = 0; i < items.length; i++) {
            infos[i] = (PlanViewInfo) items[i].getData();
        }
        return infos;
    }

    private void changeActiveView(VerticalButton viewButton) throws DBException {
        activeViewInfo = (PlanViewInfo) viewButton.getData();
        if (activeViewInfo.planViewer == null) {
            activeViewInfo.planViewer = activeViewInfo.descriptor.createInstance();
            activeViewInfo.viewer = activeViewInfo.planViewer.createPlanViewer(workbenchPart, planViewComposite);
            if (lastPlan != null) {
                activeViewInfo.planViewer.visualizeQueryPlan(activeViewInfo.viewer, lastQuery, lastPlan);
            }
        }
        if (activeViewInfo.planViewer != null) {
            ((StackLayout) planViewComposite.getLayout()).topControl = activeViewInfo.viewer.getControl();
            planViewComposite.layout();

            getPlanViewSettings().put("activeView", activeViewInfo.descriptor.getId());

        } else {
            activeViewInfo = null;
        }

        planPresentationContainer.refreshActions();
    }

    private IDialogSettings getPlanViewSettings() {
        return UIUtils.getSettingsSection(SQLEditorActivator.getDefault().getDialogSettings(), getClass().getSimpleName());
    }

    /////////////////////////////////////////////////////////
    // Viewer

    @Override
    public Control getControl() {
        return planPresentationContainer;
    }

    @Override
    public Object getInput() {
        return activeViewInfo == null ? null : activeViewInfo.viewer.getInput();
    }

    @Override
    public ISelection getSelection() {
        return activeViewInfo == null ? null : activeViewInfo.viewer.getSelection();
    }

    @Override
    public void refresh() {
        DBCQueryPlanner planner;
        DBCExecutionContext executionContext = contextProvider.getExecutionContext();
        if (executionContext != null) {
            DBPDataSource dataSource = executionContext.getDataSource();
            planner = GeneralUtils.adapt(dataSource, DBCQueryPlanner.class);
        } else {
            planner = null;
        }

        if (planner == null) {
            DBWorkbench.getPlatformUI().showError("No SQL Plan","This datasource doesn't support execution plans");
        } else {
            LoadingJob<DBCPlan> service = LoadingJob.createService(
                new ExplainPlanService(planner, executionContext, lastQuery.getText(), lastQueryId),
                planPresentationContainer.createVisualizer());
            service.schedule();
        }
    }

    private void visualizePlan(DBCPlan plan) {
        this.lastPlan = plan;
        this.refreshPlanAction.setEnabled(true);

        for (PlanViewInfo viewInfo : getPlanViews()) {
            if (viewInfo.viewer != null) {
                viewInfo.planViewer.visualizeQueryPlan(viewInfo.viewer, lastQuery, plan);
            }
        }
        planPresentationContainer.refreshActions();
    }

    @Override
    public void setInput(Object input) {
        if (activeViewInfo != null) {
            activeViewInfo.viewer.setInput(input);
        }
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        if (activeViewInfo != null) {
            activeViewInfo.viewer.setSelection(selection, reveal);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (activeViewInfo != null && activeViewInfo.viewer instanceof IAdaptable) {
            return ((IAdaptable) activeViewInfo.viewer).getAdapter(adapter);
        }
        return null;
    }

    private class ProgressControl extends ProgressPageControl {
        ProgressControl(Composite parent) {
            super(parent, SWT.SHEET);
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            if (activeViewInfo != null && activeViewInfo.viewer != null) {
                activeViewInfo.planViewer.contributeActions(activeViewInfo.viewer, contributionManager, lastQuery, lastPlan);
            }
            contributionManager.add(refreshPlanAction);
        }

        PlanLoadVisualizer createVisualizer() {
            return new PlanLoadVisualizer();
        }

        class PlanLoadVisualizer extends ProgressVisualizer<DBCPlan> {
            @Override
            public void completeLoading(DBCPlan plan) {
                super.completeLoading(plan);
                if (plan != null) {
                    visualizePlan(plan);
                }
            }
        }
    }

    public static class ExplainPlanService extends DatabaseLoadService<DBCPlan> {

        private final DBCQueryPlanner planner;
        private final DBCExecutionContext executionContext;
        private final String query;
        private final Object savedQueryId;
        private DBCPlan plan;

        ExplainPlanService(DBCQueryPlanner planner, DBCExecutionContext executionContext, String query, Object savedQueryId)
        {
            super("Explain plan", planner.getDataSource());
            this.planner = planner;
            this.executionContext = executionContext;
            this.query = query;
            this.savedQueryId = savedQueryId;
        }

        @Override
        public DBCPlan evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException {
            try {
                DBCQueryPlannerConfiguration configuration = makeExplainPlanConfiguration(monitor, planner);
                if (configuration == null) {
                    return null;
                }

                DBExecUtils.tryExecuteRecover(monitor, executionContext.getDataSource(), param -> {
                    try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Explain '" + query + "'")) {
                        try {
                            if (savedQueryId != null && planner instanceof DBCSavedQueryPlanner) {
                                plan = ((DBCSavedQueryPlanner) planner).readSavedQueryExecutionPlan(session, savedQueryId);
                            } else {
                                plan = planner.planQueryExecution(session, query, configuration);
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (Throwable ex) {
                throw new InvocationTargetException(ex);
            }
            return plan;
        }

    }

    private class RefreshPlanAction extends Action {
        private RefreshPlanAction()
        {
            super("Reevaluate", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH));
        }

        @Override
        public void run()
        {
            ExplainPlanViewer.this.refresh();
        }
    }

    public static DBCQueryPlannerConfiguration makeExplainPlanConfiguration(DBRProgressMonitor monitor, DBCQueryPlanner planner) {
        DBCQueryPlannerConfiguration configuration = new DBCQueryPlannerConfiguration();
        DBEObjectConfigurator<DBCQueryPlannerConfiguration> plannerConfigurator = GeneralUtils.adapt(planner, DBEObjectConfigurator.class);
        if (plannerConfigurator != null) {
            return plannerConfigurator.configureObject(monitor, planner, configuration);
        }
        return configuration;
    }

}
