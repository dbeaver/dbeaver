/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.plan;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * ResultSetViewer
 */
public class ExplainPlanViewer extends Viewer implements IPropertyChangeListener
{
    static final Log log = LogFactory.getLog(ResultSetViewer.class);

    private Tree planTree;
    private Label statusLabel;

    private ToolItem itemNext;
    private ToolItem itemPrevious;
    private ToolItem itemFirst;
    private ToolItem itemLast;
    private ToolItem itemRefresh;

    public ExplainPlanViewer(Composite parent)
    {
        super();
        planTree = new Tree(
            parent,
            SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);

        createStatusBar(planTree);

        planTree.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
    }

    private void createStatusBar(Composite parent)
    {
        Composite statusBar = new Composite(parent, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        statusBar.setLayoutData(gd);
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 5;
        gl.marginHeight = 0;
        statusBar.setLayout(gl);

        statusLabel = new Label(statusBar, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        statusLabel.setLayoutData(gd);

        {
            ToolBar toolBar = new ToolBar(statusBar, SWT.FLAT | SWT.HORIZONTAL);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            toolBar.setLayoutData(gd);
            new ToolItem(toolBar, SWT.SEPARATOR);
/*
            itemFirst = createToolItem(toolBar, "First", "/icons/sql/resultset_first.png", new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
            itemPrevious = createToolItem(toolBar, "Previous", "/icons/sql/resultset_previous.png", new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
            itemNext = createToolItem(toolBar, "Next", "/icons/sql/resultset_next.png", new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
            itemLast = createToolItem(toolBar, "Last", "/icons/sql/resultset_last.png", new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
*/
            new ToolItem(toolBar, SWT.SEPARATOR);
            itemRefresh = UIUtils.createToolItem(toolBar, "Refresh", DBIcon.RS_REFRESH, new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e)
                {
                    refresh();
                }
            });
        }
    }

    public void dispose()
    {
        if (!planTree.isDisposed()) {
            planTree.dispose();
        }
        statusLabel.dispose();
    }

    public void setStatus(String status)
    {
        setStatus(status, false);
    }

    public void setStatus(String status, boolean error)
    {
        statusLabel.setText(status);
    }

    public boolean isEditable()
    {
        return false;
    }

    public boolean isInsertable()
    {
        return false;
    }

    public Control getControl()
    {
        return planTree;
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
        return new StructuredSelection(planTree.getSelection());
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public void refresh()
    {
        // Refresh plan
    }

    public void propertyChange(PropertyChangeEvent event)
    {
    }
}