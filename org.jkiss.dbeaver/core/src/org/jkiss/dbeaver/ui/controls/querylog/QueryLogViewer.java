/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.querylog;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.LongKeyMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.*;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.swt.IFocusService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;
import org.jkiss.dbeaver.runtime.qm.QMMetaListener;
import org.jkiss.dbeaver.runtime.qm.meta.*;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * QueryLogViewer
 */
public class QueryLogViewer extends Viewer implements QMMetaListener {

    static final Log log = LogFactory.getLog(QueryLogViewer.class);

    private static final String QUERY_LOG_CONTROL_ID = "org.jkiss.dbeaver.ui.qm.log";

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

    private static LogColumn COLUMN_TIME = new LogColumn("Time", "Time at which statement was executed", 80) {
        private DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String getText(QMMObject object)
        {
            return timeFormat.format(new Date(object.getOpenTime()));
        }
    };
    private static LogColumn COLUMN_TYPE = new LogColumn("Type", "Event type", 100) {
        String getText(QMMObject object)
        {
            return getObjectType(object);
        }
    };
    private static LogColumn COLUMN_TEXT = new LogColumn("Text", "SQL statement text/description", 400) {
        String getText(QMMObject object)
        {
            if (object instanceof QMMStatementExecuteInfo) {
                return SQLUtils.stripTransformations(((QMMStatementExecuteInfo)object).getQueryString());
            } else if (object instanceof QMMTransactionInfo) {
                if (((QMMTransactionInfo)object).isCommited()) {
                    return "Commit";
                } else {
                    return "Rollback";
                }
            } else if (object instanceof QMMTransactionSavepointInfo) {
                if (((QMMTransactionSavepointInfo)object).isCommited()) {
                    return "Commit";
                } else {
                    return "Rollback";
                }
            } else if (object instanceof QMMSessionInfo) {
                DBSDataSourceContainer container = ((QMMSessionInfo) object).getContainer();
                if (!object.isClosed()) {
                    return "Connected to \"" + (container == null ? "?" : container.getName()) + "\"";
                } else {
                    return "Disconnected from \"" + (container == null ? "?" : container.getName()) + "\"";
                }
            }
            return "";
        }
    };
    private static LogColumn COLUMN_DURATION = new LogColumn("Duration", "Operation execution time", 100) {
        String getText(QMMObject object)
        {
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo)object;
                if (exec.isClosed() && !exec.isFetching()) {
                    return String.valueOf(exec.getCloseTime() - exec.getOpenTime()) + " ms";
                } else {
                    return "";
                }
            } else if (object instanceof QMMTransactionInfo) {
                QMMTransactionInfo txn = (QMMTransactionInfo)object;
                if (txn.isClosed()) {
                    return formatMinutes(txn.getCloseTime() - txn.getOpenTime());
                } else {
                    return "";
                }
            } else if (object instanceof QMMTransactionSavepointInfo) {
                QMMTransactionSavepointInfo sp = (QMMTransactionSavepointInfo)object;
                if (sp.isClosed()) {
                    return formatMinutes(sp.getCloseTime() - sp.getOpenTime());
                } else {
                    return "";
                }
            } else if (object instanceof QMMSessionInfo) {
                QMMSessionInfo session = (QMMSessionInfo)object;
                if (session.isClosed()) {
                    return formatMinutes(session.getCloseTime() - session.getOpenTime());
                } else {
                    return "";
                }
            }
            return "";
        }
    };
    private static LogColumn COLUMN_ROWS = new LogColumn("Rows", "Number of rows processed by statement", 120) {
        String getText(QMMObject object)
        {
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo)object;
                if (exec.isClosed() && !exec.isFetching()) {
                    return String.valueOf(exec.getRowCount());
                }
            }
            return "";
        }
    };
    private static LogColumn COLUMN_RESULT = new LogColumn("Result", "Execution result", 120) {
        String getText(QMMObject object)
        {
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo)object;
                if (exec.isClosed()) {
                    if (exec.hasError()) {
                        if (exec.getErrorCode() == 0) {
                            return exec.getErrorMessage();
                        } else if (exec.getErrorMessage() == null) {
                            return "Error [" + exec.getErrorCode() + "]";
                        } else {
                            return "[" + exec.getErrorCode() + "] " + exec.getErrorMessage();
                        }
                    } else {
                        return "Success";
                    }
                }
            }
            return "";
        }
    };
    private static LogColumn[] ALL_COLUMNS = new LogColumn[] {
        COLUMN_TIME,
        COLUMN_TYPE,
        COLUMN_TEXT,
        COLUMN_DURATION,
        COLUMN_ROWS,
        COLUMN_RESULT,
    };

    private final IWorkbenchPartSite site;
    private Table logTable;
    private java.util.List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
    private LongKeyMap<TableItem> objectToItemMap = new LongKeyMap<TableItem>();
    private IQueryLogFilter filter;

    private final Color colorLightGreen;
    private final Color colorLightRed;
    private final Color colorLightYellow;
    private final Color colorGray;
    private final Font boldFont;

    public QueryLogViewer(Composite parent, IWorkbenchPartSite site, IQueryLogFilter filter, boolean loadPastEvents)
    {
        super();

        this.site = site;

        // Prepare colors
        ISharedTextColors sharedColors = DBeaverCore.getInstance().getSharedTextColors();

        colorLightGreen = sharedColors.getColor(new RGB(0xE4, 0xFF, 0xB5));
        colorLightRed = sharedColors.getColor(new RGB(0xFF, 0x63, 0x47));
        colorLightYellow = sharedColors.getColor(new RGB(0xFF, 0xE4, 0xB5));
        colorGray = sharedColors.getColor(new RGB(0x50, 0x50, 0x50));
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        // Create log table
        logTable = new Table(
            parent,
            SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        logTable.setData(this);
        logTable.setLinesVisible(true);
        logTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        logTable.setLayoutData(gd);

        createColumns();

        {
            // Register control in focus service (to provide handlers binding)
            final IFocusService focusService = (IFocusService) site.getService(IFocusService.class);
            focusService.addFocusTracker(logTable, QUERY_LOG_CONTROL_ID);

            logTable.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent e)
                {
                    // Unregister from focus service
                    focusService.removeFocusTracker(logTable);
                    dispose();
                }
            });
        }

        createContextMenu();
        addDragAndDropSupport();

        this.filter = filter;
        if (loadPastEvents) {
            metaInfoChanged(QMUtils.getPastMetaEvents());
        }
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
        if (!boldFont.isDisposed()) {
            boldFont.dispose();
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

    public IStructuredSelection getSelection()
    {
        TableItem[] items = logTable.getSelection();
        QMMObject[] data = new QMMObject[items.length];
        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
            data[i] = (QMMObject)items[i].getData();
        }
        return new StructuredSelection(data);
    }

    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    public void refresh()
    {
        // Refresh plan
    }

    static String getObjectType(QMMObject object)
    {
        if (object instanceof QMMSessionInfo) {
            return "Connection";
        } else if (object instanceof QMMStatementInfo || object instanceof QMMStatementExecuteInfo) {
            return "SQL";
        } else if (object instanceof QMMStatementScripInfo) {
            return "Script";
        } else if (object instanceof QMMTransactionInfo) {
            return "Transaction";
        } else if (object instanceof QMMTransactionSavepointInfo) {
            return "Savepoint";
        }
        return "";
    }

    Font getObjectFont(QMMObject object)
    {
        if (object instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo)object;
            if (!exec.isClosed() || exec.isFetching()) {
                return boldFont;
            }
        }
        return null;
    }

    Color getObjectForeground(QMMObject object)
    {
        if (object instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo)object;
            switch (exec.getStatement().getPurpose()) {
                case USER_SCRIPT:
                case USER:
                    return null;
                default:
                    return colorGray;
            }
        }
        return null;
    }

    Color getObjectBackground(QMMObject object)
    {
        if (object instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo)object;
            if (exec.hasError()) {
                return colorLightRed;
            }
            QMMTransactionSavepointInfo savepoint = exec.getSavepoint();
            if (savepoint == null) {
                return colorLightGreen;
            } else if (savepoint.isClosed()) {
                return savepoint.isCommited() ? colorLightGreen : colorLightRed;
            } else {
                return null;
            }
        } else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo) {
            return colorLightYellow;
        }
        return null;
    }

    public void metaInfoChanged(Collection<QMMetaEvent> events)
    {
        if (logTable.isDisposed()) {
            return;
        }
        logTable.setRedraw(false);
        try {
            for (QMMetaEvent event : events) {
                if (filter != null && !filter.accept(event)) {
                    return;
                }
                QMMObject object = event.getObject();
                if (object instanceof QMMStatementExecuteInfo) {
                    createOrUpdateItem(object);
                } else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo) {
                    createOrUpdateItem(object);
                    // Update all dependent statements
                    if (object instanceof QMMTransactionInfo) {
                        for (QMMTransactionSavepointInfo savepoint = ((QMMTransactionInfo)object).getCurrentSavepoint(); savepoint != null; savepoint = savepoint.getPrevious()) {
                            updateExecutions(savepoint);
                        }

                    } else {
                        updateExecutions((QMMTransactionSavepointInfo)object);
                    }
                } else if (object instanceof QMMSessionInfo) {
                    QMMetaEvent.Action action = event.getAction();
                    if (action == QMMetaEvent.Action.BEGIN || action == QMMetaEvent.Action.END) {
                        TableItem item = new TableItem(logTable, SWT.NONE, 0);
                        updateItem(object, item);
                    }
                }
            }
        }
        finally {
            logTable.setRedraw(true);
        }
    }

    private void updateExecutions(QMMTransactionSavepointInfo savepoint)
    {
        for (Iterator<QMMStatementExecuteInfo> i = savepoint.getExecutions(); i.hasNext(); ) {
            QMMStatementExecuteInfo exec = i.next();
            TableItem item = objectToItemMap.get(exec.getObjectId());
            if (item != null) {
                item.setFont(getObjectFont(exec));
                item.setForeground(getObjectForeground(exec));
                item.setBackground(getObjectBackground(exec));
            }
        }
    }

    private void createOrUpdateItem(QMMObject object)
    {
        TableItem item = objectToItemMap.get(object.getObjectId());
        if (item == null) {
            item = new TableItem(logTable, SWT.NONE, 0);
            objectToItemMap.put(object.getObjectId(), item);
        }
        updateItem(object, item);
    }

    private void updateItem(QMMObject object, TableItem item)
    {
        item.setData(object);
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            ColumnDescriptor cd = columns.get(i);
            item.setText(i, cd.logColumn.getText(object));
        }
        item.setFont(getObjectFont(object));
        item.setForeground(getObjectForeground(object));
        item.setBackground(getObjectBackground(object));
    }

    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(logTable);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action("Copy") {
                    public void run()
                    {
                        copySelectionToClipboard(false);
                    }
                };
                copyAction.setEnabled(logTable.getSelectionCount() > 0);
                copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

                IAction copyAllAction = new Action("Copy All Fields") {
                    public void run()
                    {
                        copySelectionToClipboard(true);
                    }
                };
                copyAllAction.setEnabled(logTable.getSelectionCount() > 0);
                copyAllAction.setActionDefinitionId(ICommandIds.CMD_COPY_SPECIAL);

                IAction selectAllAction = new Action("Select All") {
                    public void run()
                    {
                        selectAll();
                    }
                };
                selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

                IAction clearLogAction = new Action("Clear Log") {
                    public void run()
                    {
                        clearLog();
                    }
                };

                manager.add(copyAction);
                manager.add(copyAllAction);
                manager.add(selectAllAction);
                manager.add(clearLogAction);
                manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        logTable.setMenu(menu);
        site.registerContextMenu(menuMgr, this);
    }

    public void addDragAndDropSupport()
    {
        Transfer[] types = new Transfer[] {TextTransfer.getInstance()};
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        final DragSource source = new DragSource(logTable, operations);
        source.setTransfer(types);
        source.addDragListener (new DragSourceListener() {

            public void dragStart(DragSourceEvent event) {
            }
            public void dragSetData (DragSourceEvent event) {
                String tdt = getSelectedText(false);
                if (!CommonUtils.isEmpty(tdt)) {
                    event.data = tdt;
                } else {
                    event.data = "";
                }
            }
            public void dragFinished(DragSourceEvent event) {
            }
        });
    }

    private void clearLog()
    {
        logTable.removeAll();
    }

    void selectAll()
    {
        if (!logTable.isDisposed()) {
            logTable.selectAll();
        }
    }

    void copySelectionToClipboard(boolean extraInfo)
    {
        String tdt = getSelectedText(extraInfo);
        if (CommonUtils.isEmpty(tdt)) {
            return;
        }

        if (tdt.length() > 0) {
            TextTransfer textTransfer = TextTransfer.getInstance();
            Clipboard clipboard = new Clipboard(logTable.getDisplay());
            clipboard.setContents(
                new Object[]{tdt.toString()},
                new Transfer[]{textTransfer});
        }
    }

    private String getSelectedText(boolean extraInfo)
    {
        IStructuredSelection selection = getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        StringBuilder tdt = new StringBuilder();
        for (Iterator i = selection.iterator(); i.hasNext(); ) {
            QMMObject item = (QMMObject)i.next();
            if (tdt.length() > 0) {
                tdt.append(ContentUtils.getDefaultLineSeparator());
            }
            if (extraInfo) {
                for (int i1 = 0, columnsSize = columns.size(); i1 < columnsSize; i1++) {
                    ColumnDescriptor cd = columns.get(i1);
                    String text = cd.logColumn.getText(item);
                    if (i1 > 0) {
                        tdt.append('\t');
                    }
                    tdt.append(text);
                }
            } else {
                String text = COLUMN_TEXT.getText(item);
                tdt.append(text);
            }
        }
        return tdt.toString();
    }


    private static String formatMinutes(long ms)
    {
        long min = ms / 1000 / 60;
        long sec = (ms - min * 1000 * 60) / 1000;
        return String.valueOf(min) + " min " + String.valueOf(sec) + " sec";
    }
}