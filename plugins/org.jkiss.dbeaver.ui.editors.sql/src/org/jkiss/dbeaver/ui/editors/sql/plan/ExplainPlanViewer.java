/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.VerticalButton;
import org.jkiss.dbeaver.ui.controls.VerticalFolder;
import org.jkiss.dbeaver.ui.editors.sql.SQLPlanViewProvider;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.editors.sql.plan.registry.SQLPlanViewDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.plan.registry.SQLPlanViewRegistry;
import org.jkiss.utils.CommonUtils;

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
    private final ProgressPageControl planPresentationContainer;
    private final VerticalFolder tabViewFolder;
    private final Composite planViewComposite;

    private PlanViewInfo activeViewInfo;
    private SQLQuery lastQuery;
    private DBCPlan lastPlan;

    public ExplainPlanViewer(final IWorkbenchPart workbenchPart, DBPContextProvider contextProvider, Composite parent)
    {
        this.workbenchPart = workbenchPart;
        this.contextProvider = contextProvider;

        planPresentationContainer = new ProgressPageControl(parent, SWT.SHEET);
        planPresentationContainer.getLayout().numColumns = 2;
        planPresentationContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            tabViewFolder = new VerticalFolder(planPresentationContainer, SWT.LEFT);
            tabViewFolder.setLayoutData(new GridData(GridData.FILL_VERTICAL));

            for (SQLPlanViewDescriptor viewDesc : SQLPlanViewRegistry.getInstance().getPlanViewDescriptors()) {
                VerticalButton treeViewButton = new VerticalButton(tabViewFolder, SWT.LEFT);
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
                    DBWorkbench.getPlatformUI().showError("Plan view", "Error activating plan view '" + activeViewInfo.descriptor.getLabel() + "'", e);
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

    public void explainQueryPlan(SQLQuery query) throws DBCException {
        this.lastQuery = query;

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
            planner = DBUtils.getAdapter(DBCQueryPlanner.class, dataSource);
        } else {
            planner = null;
        }

        if (planner == null) {
            DBWorkbench.getPlatformUI().showError("No SQL Plan","This datasource doesn't support execution plans");
        } else {
            try {
                UIUtils.runInProgressDialog(monitor -> {
                    try {
                        try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Explain '" + lastQuery.getText() + "'")) {
                            DBCPlan plan = planner.planQueryExecution(session, lastQuery.getText());
                            UIUtils.asyncExec(() -> visualizePlan(plan));
                        }
                    } catch (Throwable ex) {
                        throw new InvocationTargetException(ex);
                    }
                });
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError("Explain Plan error","Error explaining SQL execution plan", e.getTargetException());
            }
        }
    }

    private void visualizePlan(DBCPlan plan) {
        lastPlan = plan;
        for (PlanViewInfo viewInfo : getPlanViews()) {
            if (viewInfo.viewer != null) {
                viewInfo.planViewer.visualizeQueryPlan(viewInfo.viewer, lastQuery, plan);
            }
        }
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

}
