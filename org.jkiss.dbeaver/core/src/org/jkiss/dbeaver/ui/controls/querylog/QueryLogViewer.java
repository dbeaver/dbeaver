/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.querylog;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * QueryLogViewer
 */
public class QueryLogViewer extends Viewer implements IPropertyChangeListener
{
    static final Log log = LogFactory.getLog(QueryLogViewer.class);

    private Table logTable;
    private IQueryLogFilter filter;

    public QueryLogViewer(Composite parent)
    {
        super();
        logTable = new Table(
            parent,
            SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        logTable.setLinesVisible(true);
        logTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        logTable.setLayoutData(gd);

        TableColumn column = new TableColumn(logTable, SWT.NONE);
        column.setText("At");
        column.setToolTipText("Time at which statement was executed");

        column = new TableColumn(logTable, SWT.NONE);
        column.setText("SQL");
        column.setToolTipText("SQL statement text");

        column = new TableColumn(logTable, SWT.NONE);
        column.setText("Execution time");
        column.setToolTipText("Execution time");

        column = new TableColumn(logTable, SWT.NONE);
        column.setText("Rows processed");
        column.setToolTipText("Number of rows processed by statement");

        for (TableColumn col : logTable.getColumns()) {
            col.pack();
        }
        logTable.pack();

        logTable.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
    }

    private void dispose()
    {
        if (!logTable.isDisposed()) {
            logTable.dispose();
        }
    }

    public IQueryLogFilter getFilter()
    {
        return filter;
    }

    public void setFilter(IQueryLogFilter filter)
    {
        this.filter = filter;
    }

    public Control getControl()
    {
        return logTable;
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
        return new StructuredSelection(logTable.getSelection());
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