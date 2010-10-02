/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.plan;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.plan.DBCExecutionPlanBuilder;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

/**
 * ResultSetViewer
 */
public class ExplainPlanViewer extends Viewer implements IPropertyChangeListener
{
    static final Log log = LogFactory.getLog(ResultSetViewer.class);

    private SQLEditor editor;
    private Composite planPanel;
    private PlanNodesTree planTree;

    private DBCExecutionPlanBuilder planBuilder;

    public ExplainPlanViewer(SQLEditor editor, Composite parent)
    {
        super();
        this.editor = editor;
        this.planPanel = UIUtils.createPlaceholder(parent, 1);

        this.planTree = new PlanNodesTree(planPanel, SWT.NONE, editor, editor);
        GridData gd = new GridData(GridData.FILL_BOTH);
        planTree.setLayoutData(gd);

        planTree.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        planTree.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e)
            {
                if (planBuilder == null) {
                    Rectangle bounds = planTree.getBounds();
                    String message;
                    if (getDataSource() != null) {
                        message = "Data provider doesn't support execution plan";
                    } else {
                        message = "Not connected to database";
                    }
                    Point ext = e.gc.textExtent(message);
                    e.gc.drawText(message, (bounds.width - ext.x) / 2, (bounds.height - 20) / 2);
                }
            }
        });
        planTree.setLayout(new GridLayout(1, true));
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
        return new StructuredSelection(planTree.getItemsViewer().getSelection());
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public void refresh()
    {
        // Refresh plan
        DBPDataSource dataSource = getDataSource();
        if (dataSource instanceof DBCExecutionPlanBuilder) {
            planBuilder = (DBCExecutionPlanBuilder)dataSource;
        } else {
            planBuilder = null;
        }
        planTree.clearData();
    }

    public void propertyChange(PropertyChangeEvent event)
    {
    }

    public void explainQueryPlan(String query) throws DBCException
    {
        if (planBuilder == null) {
            throw new DBCException("This datasource doesn't support execution plans");
        }
        DBCPlan plan = planBuilder.prepareExecutionPlan(query);

        planTree.clearData();
        planTree.fillData(plan);
    }
}