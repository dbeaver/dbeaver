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
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;
import org.jkiss.dbeaver.runtime.qm.QMMetaListener;
import org.jkiss.dbeaver.runtime.qm.meta.QMMObject;
import org.jkiss.dbeaver.runtime.qm.meta.QMMStatementExecuteInfo;

import java.util.ArrayList;
import java.util.Date;

/**
 * QueryLogViewer
 */
public class QueryLogViewer extends Viewer implements IPropertyChangeListener, QMMetaListener {

    static final Log log = LogFactory.getLog(QueryLogViewer.class);

    private static abstract class LogColumn {
        private final String title;
        private final String toolTip;
        private final int widthHint;
        private LogColumn(String title, String toolTip, int widthHint)
        {
            this.title = title;
            this.toolTip = toolTip;
            this.widthHint = widthHint;
        }
        abstract String getText(QMMObject object);
    }

    private static class ColumnDescriptor {
        LogColumn logColumn;
        TableColumn tableColumn;

        public ColumnDescriptor(LogColumn logColumn, TableColumn tableColumn)
        {
            this.logColumn = logColumn;
            this.tableColumn = tableColumn;
        }
    }

    private static LogColumn[] ALL_COLUMNS = new LogColumn[] {
        new LogColumn("Time", "Time at which statement was executed", 120) {
            String getText(QMMObject object)
            {
                return new Date(object.getOpenTime()).toString();
            }
        },
        new LogColumn("Type", "Event type", 120) {
            String getText(QMMObject object)
            {
                return object.getClass().getSimpleName();
            }
        },
        new LogColumn("Text", "SQL statement text/description", 120) {
            String getText(QMMObject object)
            {
                if (object instanceof QMMStatementExecuteInfo) {
                    return ((QMMStatementExecuteInfo)object).getQueryString();
                }
                return "";
            }
        },
        new LogColumn("Execution Time", "Execution Time", 120) {
            String getText(QMMObject object)
            {
                if (object instanceof QMMStatementExecuteInfo) {
                    //return ((QMMStatementExecuteInfo)object).getCloseTime();
                }
                return "";
            }
        },
        new LogColumn("Rows", "Number of rows processed by statement", 120) {
            String getText(QMMObject object)
            {
                return "";
            }
        },
        new LogColumn("Result", "Execution result", 120) {
            String getText(QMMObject object)
            {
                return "";
            }
        },
    };

    private Table logTable;
    private java.util.List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
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

        createColumns();

        logTable.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });

        QMUtils.registerMetaListener(this);
    }

    private void createColumns()
    {
        for (TableColumn tableColumn : logTable.getColumns()) {
            tableColumn.dispose();
        }
        columns.clear();

        for (LogColumn logColumn : ALL_COLUMNS) {
            TableColumn tableColumn = new TableColumn(logTable, SWT.NONE);
            tableColumn.setText(logColumn.title);
            tableColumn.setToolTipText(logColumn.toolTip);
            tableColumn.setWidth(logColumn.widthHint);

            ColumnDescriptor cd = new ColumnDescriptor(logColumn, tableColumn);
            columns.add(cd);
        }
    }

    private void dispose()
    {
        QMUtils.unregisterMetaListener(this);
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

    public void metaInfoChanged(QMMetaEvent event)
    {
        if (filter != null && !filter.accept(event)) {
            return;
        }
        if (event.getObject() instanceof QMMStatementExecuteInfo && event.getAction() == QMMetaEvent.Action.BEGIN) {
            TableItem item = new TableItem(logTable, SWT.NONE);
            for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
                ColumnDescriptor cd = columns.get(i);
                item.setText(i, cd.logColumn.getText(event.getObject()));
            }
        }
    }

}