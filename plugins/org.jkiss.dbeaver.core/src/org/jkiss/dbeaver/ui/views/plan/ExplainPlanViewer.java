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
package org.jkiss.dbeaver.ui.views.plan;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

/**
 * ResultSetViewer
 */
public class ExplainPlanViewer implements IPropertyChangeListener
{
    //static final Log log = Log.getLog(ResultSetViewer.class);
    private SashForm planPanel;
    private Text sqlText;
    private PlanNodesTree planTree;
    private PropertyTreeViewer planProperties;

    private DBCExecutionContext executionContext;
    private SQLQuery query;
    private DBCQueryPlanner planner;
    private RefreshPlanAction refreshPlanAction;
    private ToggleViewAction toggleViewAction;
    private final SashForm leftPanel;

    public ExplainPlanViewer(final IWorkbenchPart workbenchPart, Composite parent)
    {
        super();
        createActions();

        Composite composite = UIUtils.createPlaceholder(parent, 1);

        this.planPanel = UIUtils.createPartDivider(workbenchPart, composite, SWT.HORIZONTAL);
        this.planPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        this.planPanel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        final GridLayout gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        this.planPanel.setLayout(gl);
        {
            leftPanel = UIUtils.createPartDivider(workbenchPart, planPanel, SWT.VERTICAL);
            leftPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            this.planTree = new PlanNodesTree(leftPanel, SWT.SHEET, workbenchPart.getSite()) {
                @Override
                protected void fillCustomActions(IContributionManager contributionManager) {
                    contributionManager.add(toggleViewAction);
                    contributionManager.add(refreshPlanAction);
                }
            };
            this.planTree.setShowDivider(true);
            this.planTree.createProgressPanel(composite);
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

        planTree.getControl().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e)
            {
                String message = null;
                if (planner == null) {
                    message = "No connection or data source doesn't support execution plan";
                } else if (CommonUtils.isEmpty(sqlText.getText())) {

                    message = "Select a query and run " + ActionUtils.findCommandDescription(
                        CoreCommands.CMD_EXPLAIN_PLAN,
                        workbenchPart.getSite(), false);
                }
                if (message != null) {
                    Rectangle bounds = planTree.getBounds();
                    Point ext = e.gc.textExtent(message);
                    e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + 20);
                }
            }
        });
        planTree.getItemsViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                showPlanNode();
            }
        });

        this.planTree.getControl().addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                if (toggleViewAction.isEnabled() &&
                    (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS))
                {
                    toggleViewAction.run();
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
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
            PropertyCollector propertySource = new PropertyCollector(element, true);
            propertySource.collectProperties();
            planProperties.loadProperties(propertySource);
        }
    }

    private void createActions()
    {
        this.toggleViewAction = new ToggleViewAction();
        this.toggleViewAction.setEnabled(false);

        this.refreshPlanAction = new RefreshPlanAction();
        this.refreshPlanAction.setEnabled(false);
    }

    public Control getControl()
    {
        return planPanel.getParent();
    }

    public Viewer getViewer()
    {
        return planTree.getItemsViewer();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
    }

    public void explainQueryPlan(DBCExecutionContext executionContext, SQLQuery query) throws DBCException
    {
        this.executionContext = executionContext;
        this.query = query;
        if (this.executionContext != null) {
            DBPDataSource dataSource = executionContext.getDataSource();
            planner = DBUtils.getAdapter(DBCQueryPlanner.class, dataSource);
        } else {
            planner = null;
        }
        planTree.clearListData();
        refreshPlanAction.setEnabled(false);

        if (planner == null) {
            throw new DBCException("This datasource doesn't support execution plans");
        }
        if (planTree.isLoading()) {
            UIUtils.showMessageBox(
                getControl().getShell(),
                "Can't explain plan",
                "Explain plan already running",
                SWT.ICON_ERROR);
            return;
        }
        sqlText.setText(query.getQuery());
        planTree.init(this.executionContext, planner, query.getQuery());
        planTree.loadData();

        refreshPlanAction.setEnabled(true);
        toggleViewAction.setEnabled(true);
    }

    private class RefreshPlanAction extends Action {
        private RefreshPlanAction()
        {
            super("Reevaluate", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH));
        }

        @Override
        public void run()
        {
            if (planTree != null) {
                planTree.loadData();
            }
        }
    }

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