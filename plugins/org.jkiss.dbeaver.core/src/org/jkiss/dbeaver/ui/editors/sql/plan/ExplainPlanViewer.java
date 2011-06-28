/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.plan;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

/**
 * ResultSetViewer
 */
public class ExplainPlanViewer extends Viewer implements IPropertyChangeListener
{
    //static final Log log = LogFactory.getLog(ResultSetViewer.class);

    private SQLEditor editor;
    private SashForm planPanel;
    private PlanNodesTree planTree;

    private DBCQueryPlanner planner;
    private RefreshPlanAction refreshPlanAction;
    private ToggleViewAction toggleViewAction;

    public ExplainPlanViewer(SQLEditor editor, Composite parent)
    {
        super();
        this.editor = editor;

        createActions();

        this.planPanel = UIUtils.createPartDivider(editor, parent, SWT.HORIZONTAL | SWT.SMOOTH);
        final GridLayout gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        this.planPanel.setLayout(gl);
        {
            final Composite ph = UIUtils.createPlaceholder(planPanel, 1);
        }
        this.planTree = new PlanNodesTree(planPanel, SWT.SHEET, editor, editor) {
            @Override
            protected Composite createProgressPanel(Composite container)
            {
                Composite infoGroup = super.createProgressPanel(container);

                ToolBarManager toolBar = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
                toolBar.add(toggleViewAction);
                toolBar.add(refreshPlanAction);

                toolBar.createControl(infoGroup);

                return infoGroup;
            }
        };
        this.planTree.setShowDivider(true);
        this.planTree.createProgressPanel();
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        planTree.setLayoutData(gd);

        planPanel.setWeights(new int[] {30, 70});
        planPanel.setMaximizedControl(planTree);

        planTree.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        planTree.getControl().addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e)
            {
                if (planner == null) {
                    Rectangle bounds = planTree.getBounds();
                    String message;
                    if (getDataSource() != null) {
                        message = "Data provider doesn't support execution plan";
                    } else {
                        message = "Not connected to database";
                    }
                    Point ext = e.gc.textExtent(message);
                    e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + 20);
                }
            }
        });

        this.planTree.getControl().addTraverseListener(new TraverseListener() {
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

    private void createActions()
    {
        this.toggleViewAction = new ToggleViewAction();
        this.toggleViewAction.setEnabled(false);

        this.refreshPlanAction = new RefreshPlanAction();
        this.refreshPlanAction.setEnabled(false);
    }

    private DBPDataSource getDataSource()
    {
        return editor.getDataSource();
    }

    public void dispose()
    {
        if (!planTree.isDisposed()) {
            planTree.dispose();
        }
        //statusLabel.dispose();
    }

    public Control getControl()
    {
        return planPanel;
    }

    public Object getInput()
    {
        return null;
    }

    public void setInput(Object input)
    {
    }

    public ISelection getSelection()
    {
        return planTree.getSelectionProvider().getSelection();
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public void refresh()
    {
        // Refresh plan
        DBPDataSource dataSource = getDataSource();
        planner = DBUtils.getAdapter(DBCQueryPlanner.class, dataSource);
        planTree.clearListData();
        refreshPlanAction.setEnabled(false);
    }

    public void propertyChange(PropertyChangeEvent event)
    {
    }

    public void explainQueryPlan(String query) throws DBCException
    {
        if (planner == null) {
            throw new DBCException("This datasource doesn't support execution plans");
        }

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
            if (planTree != null && planTree.isInitialized()) {
                planTree.loadData();
            }
        }
    }

    private class ToggleViewAction extends Action {
        private ToggleViewAction()
        {
            super("View Source", DBIcon.RS_MODE_GRID.getImageDescriptor());
        }

        @Override
        public void run()
        {
            final Control maxControl = planPanel.getMaximizedControl();
            if (maxControl == null) {
                planPanel.setMaximizedControl(planTree);
            } else {
                planPanel.setMaximizedControl(null);
            }
        }
    }

}