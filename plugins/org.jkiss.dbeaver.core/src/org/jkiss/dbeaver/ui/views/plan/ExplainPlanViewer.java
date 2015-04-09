/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.views.plan;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
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

            this.planTree = new PlanNodesTree(leftPanel, SWT.SHEET) {
                @Override
                protected void fillCustomToolbar(ToolBarManager toolbarManager) {
                    toolbarManager.add(toggleViewAction);
                    toolbarManager.add(refreshPlanAction);
                }
            };
            this.planTree.setShowDivider(true);
            this.planTree.createProgressPanel(composite);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            planTree.setLayoutData(gd);

            sqlText = new Text(leftPanel, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);

            leftPanel.setWeights(new int[] {80, 20});
            leftPanel.setMaximizedControl(planTree);
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
                        ICommandIds.CMD_EXPLAIN_PLAN,
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

    public void refresh(DBCExecutionContext executionContext)
    {
        // Refresh plan
        if (executionContext != null) {
            DBPDataSource dataSource = executionContext.getDataSource();
            planner = DBUtils.getAdapter(DBCQueryPlanner.class, dataSource);
        }
        planTree.clearListData();
        refreshPlanAction.setEnabled(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
    }

    public void explainQueryPlan(String query) throws DBCException
    {
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
        sqlText.setText(query);
        planTree.init(planner, query);
        planTree.loadData();

        refreshPlanAction.setEnabled(true);
        toggleViewAction.setEnabled(true);
    }

    private class RefreshPlanAction extends Action {
        private RefreshPlanAction()
        {
            super("Reevaluate", DBIcon.REFRESH.getImageDescriptor());
        }

        @Override
        public void run()
        {
            if (planTree != null) {
                try {
                    explainQueryPlan(sqlText.getText());
                } catch (DBCException e) {
                    UIUtils.showErrorDialog(getControl().getShell(), "Explain plan", "Can't explain execution plan", e);
                }
            }
        }
    }

    private class ToggleViewAction extends Action {
        private ToggleViewAction()
        {
            super("View Source", DBIcon.SQL_TEXT.getImageDescriptor());
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