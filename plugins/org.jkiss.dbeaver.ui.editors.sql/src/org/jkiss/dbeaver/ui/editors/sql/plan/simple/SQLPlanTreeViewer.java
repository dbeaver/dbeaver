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
package org.jkiss.dbeaver.ui.editors.sql.plan.simple;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

/**
 * SQLPlanTreeViewer
 */
public class SQLPlanTreeViewer extends Viewer
{
    //static final Log log = Log.getLog(ResultSetViewer.class);

    private SashForm planPanel;
    private Text sqlText;
    private PlanNodesTree planTree;
    private PropertyTreeViewer planProperties;

    private SQLQuery query;
    private ToggleViewAction toggleViewAction;
    private final SashForm leftPanel;

    private transient Object selectedElement;

    public SQLPlanTreeViewer(final IWorkbenchPart workbenchPart, Composite parent)
    {
        super();
        createActions();

        Composite composite = UIUtils.createPlaceholder(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        this.planPanel = UIUtils.createPartDivider(workbenchPart, composite, SWT.HORIZONTAL);
        this.planPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        //this.planPanel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        final GridLayout gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        this.planPanel.setLayout(gl);
        {
            leftPanel = UIUtils.createPartDivider(workbenchPart, planPanel, SWT.VERTICAL);
            leftPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            this.planTree = new PlanNodesTree(leftPanel, SWT.SHEET, workbenchPart.getSite());
            //this.planTree.setShowDivider(true);
            //this.planTree.createProgressPanel(composite);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            planTree.setLayoutData(gd);

            sqlText = new Text(leftPanel, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);

            leftPanel.setWeights(new int[] {80, 20});
            //leftPanel.setMaximizedControl(planTree);
        }
        {
            planProperties = new PropertyTreeViewer(planPanel, SWT.H_SCROLL | SWT.V_SCROLL);
        }

        planPanel.setWeights(new int[] {70, 30});
        //planPanel.setMaximizedControl(planTree);

        planTree.getControl().addPaintListener(e -> {
            String message = null;
            if (query == null) {
                message = null;//"No connection or data source doesn't support execution plan";
            } else if (CommonUtils.isEmpty(sqlText.getText())) {
                message = "Select a query and run " + ActionUtils.findCommandDescription(
                    SQLEditorCommands.CMD_EXPLAIN_PLAN,
                    workbenchPart.getSite(), false);
            }
            if (message != null) {
                Rectangle bounds = planTree.getBounds();
                Point ext = e.gc.textExtent(message);
                e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + 20);
            }
        });
        planTree.getItemsViewer().addSelectionChangedListener(event -> showPlanNode());

        this.planTree.getControl().addTraverseListener(e -> {
            if (toggleViewAction.isEnabled() &&
                (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS))
            {
                toggleViewAction.run();
                e.doit = false;
                e.detail = SWT.TRAVERSE_NONE;
            }
        });
    }

    public SQLQuery getQuery() {
        return query;
    }

    private void showPlanNode()
    {
        ISelection selection = planTree.getItemsViewer().getSelection();
        if (selection.isEmpty()) {
            planProperties.clearProperties();
        } else if (selection instanceof IStructuredSelection) {
            Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element != selectedElement) {
                PropertyCollector propertySource = new PropertyCollector(element, true);
                propertySource.collectProperties();
                planProperties.loadProperties(propertySource);
                selectedElement = element;
            }
        }
    }

    private void createActions()
    {
        this.toggleViewAction = new ToggleViewAction();
        this.toggleViewAction.setEnabled(false);
    }

    public Control getControl()
    {
        return planPanel.getParent();
    }

    public Viewer getViewer()
    {
        return planTree.getItemsViewer();
    }

    /////////////////////////////////////////////////
    // Viewer

    @Override
    public Object getInput() {
        return null;
    }

    @Override
    public ISelection getSelection() {
        return null;
    }

    @Override
    public void refresh() {

    }

    @Override
    public void setInput(Object input) {

    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {

    }

    void showPlan(SQLQuery query, DBCPlan plan) {
        this.query = query;
        this.sqlText.setText(query.getText());
        this.toggleViewAction.setEnabled(true);
        this.planTree.showPlan(query.getDataSource(), plan);
    }

    void contributeActions(IContributionManager contributionManager, SQLQuery lastQuery, DBCPlan lastPlan) {
        contributionManager.add(toggleViewAction);
    }

    /////////////////////////////////////////////////
    // Actions

    private class ToggleViewAction extends Action {
        private ToggleViewAction()
        {
            super("View Source", DBeaverIcons.getImageDescriptor(UIIcon.SQL_TEXT));
        }

        @Override
        public void run()
        {
            final Control maxControl = leftPanel.getMaximizedControl();
            if (maxControl == null) {
                leftPanel.setMaximizedControl(planTree);
            } else {
                leftPanel.setMaximizedControl(null);
            }
        }
    }

}