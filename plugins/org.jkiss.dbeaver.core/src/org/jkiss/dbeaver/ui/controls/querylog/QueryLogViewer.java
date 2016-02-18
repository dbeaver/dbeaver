/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.controls.querylog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPPreferenceListener;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.qm.*;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.qm.DefaultEventFilter;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.dialogs.sql.BaseSQLDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * QueryLogViewer
 */
public class QueryLogViewer extends Viewer implements QMMetaListener, DBPPreferenceListener {

    static final Log log = Log.getLog(QueryLogViewer.class);

    private static final String QUERY_LOG_CONTROL_ID = "org.jkiss.dbeaver.ui.qm.log"; //$NON-NLS-1$
    private static final int MIN_ENTRIES_PER_PAGE = 1;

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
        abstract String getText(QMMetaEvent event);
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

    private static LogColumn COLUMN_TIME = new LogColumn(CoreMessages.controls_querylog_column_time_name, CoreMessages.controls_querylog_column_time_tooltip, 80) {
        private DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
        @Override
        String getText(QMMetaEvent event)
        {
            return timeFormat.format(new Date(event.getObject().getOpenTime()));
        }
    };
    private static LogColumn COLUMN_TYPE = new LogColumn(CoreMessages.controls_querylog_column_type_name, CoreMessages.controls_querylog_column_type_tooltip, 100) {
        @Override
        String getText(QMMetaEvent event)
        {
            return getObjectType(event.getObject());
        }
    };
    private static LogColumn COLUMN_TEXT = new LogColumn(CoreMessages.controls_querylog_column_text_name, CoreMessages.controls_querylog_column_text_tooltip, 400) {
        @Override
        String getText(QMMetaEvent event)
        {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo statement = (QMMStatementExecuteInfo) object;
                //return SQLUtils.stripTransformations(statement.getQueryString());
                return statement.getQueryString();
            } else if (object instanceof QMMTransactionInfo) {
                if (((QMMTransactionInfo) object).isCommited()) {
                    return CoreMessages.controls_querylog_commit;
                } else {
                    return CoreMessages.controls_querylog_rollback;
                }
            } else if (object instanceof QMMTransactionSavepointInfo) {
                if (((QMMTransactionSavepointInfo) object).isCommited()) {
                    return CoreMessages.controls_querylog_commit;
                } else {
                    return CoreMessages.controls_querylog_rollback;
                }
            } else if (object instanceof QMMSessionInfo) {
                DBPDataSourceContainer container = ((QMMSessionInfo) object).getContainer();
                switch (event.getAction()) {
                    case BEGIN: return CoreMessages.controls_querylog_connected_to + (container == null ? "?" : container.getName()) + "\"";
                    case END:   return CoreMessages.controls_querylog_disconnected_from + (container == null ? "?" : container.getName()) + "\"";
                    default:    return "?";
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static LogColumn COLUMN_DURATION = new LogColumn(CoreMessages.controls_querylog_column_duration_name, CoreMessages.controls_querylog_column_duration_tooltip, 100) {
        @Override
        String getText(QMMetaEvent event)
        {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) object;
                if (exec.isClosed()) {
                    final long execTime = exec.getCloseTime() - exec.getOpenTime();
                    final long fetchTime = exec.isFetching() ? 0 : exec.getFetchEndTime() - exec.getFetchBeginTime();
                    return String.valueOf(execTime + fetchTime) + CoreMessages.controls_querylog__ms;
                } else {
                    return ""; //$NON-NLS-1$
                }
            } else if (object instanceof QMMTransactionInfo) {
                QMMTransactionInfo txn = (QMMTransactionInfo) object;
                if (txn.isClosed()) {
                    return formatMinutes(txn.getCloseTime() - txn.getOpenTime());
                } else {
                    return ""; //$NON-NLS-1$
                }
            } else if (object instanceof QMMTransactionSavepointInfo) {
                QMMTransactionSavepointInfo sp = (QMMTransactionSavepointInfo) object;
                if (sp.isClosed()) {
                    return formatMinutes(sp.getCloseTime() - sp.getOpenTime());
                } else {
                    return ""; //$NON-NLS-1$
                }
            } else if (object instanceof QMMSessionInfo) {
                QMMSessionInfo session = (QMMSessionInfo) object;
                if (session.isClosed()) {
                    return formatMinutes(session.getCloseTime() - session.getOpenTime());
                } else {
                    return ""; //$NON-NLS-1$
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static LogColumn COLUMN_ROWS = new LogColumn(CoreMessages.controls_querylog_column_rows_name, CoreMessages.controls_querylog_column_rows_tooltip, 120) {
        @Override
        String getText(QMMetaEvent event)
        {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) object;
                if (exec.isClosed() && !exec.isFetching()) {
                    long rowCount = exec.getRowCount();
                    if (rowCount < 0) {
                        return ""; //$NON-NLS-1$
                    } else {
                        return String.valueOf(rowCount);
                    }
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static LogColumn COLUMN_RESULT = new LogColumn(CoreMessages.controls_querylog_column_result_name, CoreMessages.controls_querylog_column_result_tooltip, 120) {
        @Override
        String getText(QMMetaEvent event)
        {
            if (event.getObject() instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) event.getObject();
                if (exec.isClosed()) {
                    if (exec.hasError()) {
                        if (exec.getErrorCode() == 0) {
                            return exec.getErrorMessage();
                        } else if (exec.getErrorMessage() == null) {
                            return CoreMessages.controls_querylog_error + exec.getErrorCode() + "]";
                        } else {
                            return "[" + exec.getErrorCode() + "] " + exec.getErrorMessage();
                        }
                    } else {
                        return CoreMessages.controls_querylog_success;
                    }
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static LogColumn COLUMN_DATA_SOURCE = new LogColumn(CoreMessages.controls_querylog_column_connection_name, CoreMessages.controls_querylog_column_connection_tooltip, 150) {
        @Override
        String getText(QMMetaEvent event)
        {
            QMMObject object = event.getObject();
            DBPDataSourceContainer container = null;
            if (object instanceof QMMSessionInfo) {
                container = ((QMMSessionInfo) object).getContainer();
            } else if (object instanceof QMMTransactionInfo) {
                container = ((QMMTransactionInfo) object).getSession().getContainer();
            } else if (object instanceof QMMTransactionSavepointInfo) {
                container = ((QMMTransactionSavepointInfo) object).getTransaction().getSession().getContainer();
            } else if (object instanceof QMMStatementInfo) {
                container = ((QMMStatementInfo) object).getSession().getContainer();
            } else if (object instanceof QMMStatementExecuteInfo) {
                container = ((QMMStatementExecuteInfo) object).getStatement().getSession().getContainer();
            }
            return container == null ? "?" : container.getName();
        }
    };
    private static LogColumn COLUMN_CONTEXT = new LogColumn(CoreMessages.controls_querylog_column_context_name, CoreMessages.controls_querylog_column_context_tooltip, 150) {
        @Override
        String getText(QMMetaEvent event) {
            QMMObject object = event.getObject();
            DBCExecutionContext context = null;
            if (object instanceof QMMSessionInfo) {
                context = ((QMMSessionInfo) object).getReference();
            } else if (object instanceof QMMTransactionInfo) {
                context = ((QMMTransactionInfo) object).getSession().getReference();
            } else if (object instanceof QMMTransactionSavepointInfo) {
                context = ((QMMTransactionSavepointInfo) object).getTransaction().getSession().getReference();
            } else if (object instanceof QMMStatementInfo) {
                context = ((QMMStatementInfo) object).getSession().getReference();
            } else if (object instanceof QMMStatementExecuteInfo) {
                context = ((QMMStatementExecuteInfo) object).getStatement().getSession().getReference();
            }
            if (context == null) {
                return "?";
            }
            return context.getContextName();
        }
    };
    private static LogColumn[] ALL_COLUMNS = new LogColumn[] {
        COLUMN_TIME,
        COLUMN_TYPE,
        COLUMN_TEXT,
        COLUMN_DURATION,
        COLUMN_ROWS,
        COLUMN_RESULT,
        COLUMN_DATA_SOURCE,
        COLUMN_CONTEXT,
    };

    private final IWorkbenchPartSite site;
    private Table logTable;
    private java.util.List<ColumnDescriptor> columns = new ArrayList<>();
    private LongKeyMap<TableItem> objectToItemMap = new LongKeyMap<>();
    private QMEventFilter filter;

    private final Color colorLightGreen;
    private final Color colorLightRed;
    private final Color colorLightYellow;
    private final Color colorGray;
    private final Font boldFont;
    private DragSource dndSource;

    private QMEventFilter defaultFilter;

    private int entriesPerPage = MIN_ENTRIES_PER_PAGE;

    public QueryLogViewer(Composite parent, IWorkbenchPartSite site, QMEventFilter filter, boolean showConnection)
    {
        super();

        this.site = site;

        // Prepare colors
        ISharedTextColors sharedColors = DBeaverUI.getSharedTextColors();

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

        new TableToolTip(logTable);

        createColumns(showConnection);

        {
            // Register control in focus service (to provide handlers binding)
            UIUtils.addFocusTracker(site, QUERY_LOG_CONTROL_ID, logTable);

            logTable.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e)
                {
                    // Unregister from focus service
                    UIUtils.removeFocusTracker(QueryLogViewer.this.site, logTable);
                    dispose();
                }
            });
        }

        createContextMenu();
        addDragAndDropSupport();
        logTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                //TableItem item = (TableItem)e.item;
                showEventDetails((QMMetaEvent) e.item.getData());
            }
        });

        this.filter = filter;

        reloadEvents();

        QMUtils.registerMetaListener(this);

        DBeaverCore.getGlobalPreferenceStore().addPropertyChangeListener(this);
    }

    private void showEventDetails(QMMetaEvent event)
    {
        EventViewDialog dialog = new EventViewDialog(event);
        dialog.open();
/*
        EventSelectionProvider eventSelectionProvider = new EventSelectionProvider();
        //TableViewer viewer = new TableViewer(logTable);
        IMemento memento = new XMLMemento(null, null);
        EventDetailsDialogAction detailsAction = new EventDetailsDialogAction(
            logTable.getShell(),
            eventSelectionProvider,
            memento);
        detailsAction.run();
*/
    }

    private void createColumns(boolean showConnection)
    {
        for (TableColumn tableColumn : logTable.getColumns()) {
            tableColumn.dispose();
        }
        columns.clear();

        for (LogColumn logColumn : ALL_COLUMNS) {
            if (!showConnection && (logColumn == COLUMN_DATA_SOURCE || logColumn == COLUMN_CONTEXT)) {
                continue;
            }
            TableColumn tableColumn = UIUtils.createTableColumn(logTable, SWT.NONE, logColumn.title);
            tableColumn.setToolTipText(logColumn.toolTip);
            tableColumn.setWidth(logColumn.widthHint);

            ColumnDescriptor cd = new ColumnDescriptor(logColumn, tableColumn);
            columns.add(cd);
        }
    }

    private void dispose()
    {
        DBeaverCore.getGlobalPreferenceStore().removePropertyChangeListener(this);
        QMUtils.unregisterMetaListener(this);
        UIUtils.dispose(dndSource);
        UIUtils.dispose(logTable);
        UIUtils.dispose(boldFont);
    }

    @Override
    public Control getControl()
    {
        return logTable;
    }

    @Override
    public Object getInput()
    {
        return null;
    }

    @Override
    public void setInput(Object input)
    {
    }

    @Override
    public IStructuredSelection getSelection()
    {
        TableItem[] items = logTable.getSelection();
        QMMetaEvent[] data = new QMMetaEvent[items.length];
        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
            data[i] = (QMMetaEvent)items[i].getData();
        }
        return new StructuredSelection(data);
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal)
    {
    }

    @Override
    public void refresh()
    {
        // Refresh plan
    }

    static String getObjectType(QMMObject object)
    {
        if (object instanceof QMMSessionInfo) {
            return CoreMessages.model_navigator_Connection;
        } else if (object instanceof QMMStatementInfo || object instanceof QMMStatementExecuteInfo) {
            QMMStatementInfo statement;
            if (object instanceof QMMStatementInfo) {
                statement = (QMMStatementInfo) object;
            } else {
                statement = ((QMMStatementExecuteInfo)object).getStatement();
            }
            return "SQL" + (statement == null ? "" : " / " + statement.getPurpose().name()); //$NON-NLS-1$
//        } else if (object instanceof QMMStatementScripInfo) {
//            return CoreMessages.controls_querylog_script;
        } else if (object instanceof QMMTransactionInfo) {
            return CoreMessages.controls_querylog_transaction;
        } else if (object instanceof QMMTransactionSavepointInfo) {
            return CoreMessages.controls_querylog_savepoint;
        }
        return ""; //$NON-NLS-1$
    }

    Font getObjectFont(QMMetaEvent event)
    {
        if (event.getObject() instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) event.getObject();
            if (!exec.isClosed() || exec.isFetching()) {
                return boldFont;
            }
        }
        return null;
    }

    Color getObjectForeground(QMMetaEvent event)
    {
        if (event.getObject() instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) event.getObject();
            if (exec.getStatement().getPurpose().isUser()) {
                return null;
            } else {
                return colorGray;
            }
        }
        return null;
    }

    Color getObjectBackground(QMMetaEvent event)
    {
        if (event.getObject() instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) event.getObject();
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
        } else if (event.getObject() instanceof QMMTransactionInfo || event.getObject() instanceof QMMTransactionSavepointInfo) {
            return colorLightYellow;
        }
        return null;
    }

    private void reloadEvents()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        this.defaultFilter = new DefaultEventFilter();
        this.entriesPerPage = Math.max(MIN_ENTRIES_PER_PAGE, store.getInt(QMConstants.PROP_ENTRIES_PER_PAGE));

        clearLog();
        updateMetaInfo(QMUtils.getPastMetaEvents());
    }

    @Override
    public void metaInfoChanged(final java.util.List<QMMetaEvent> events) {
        // Run in UI thread
        UIUtils.runInUI(null, new Runnable() {
            @Override
            public void run() {
                updateMetaInfo(events);
            }
        });
    }

    private synchronized void updateMetaInfo(final java.util.List<QMMetaEvent> events)
    {
        if (logTable.isDisposed()) {
            return;
        }
        logTable.setRedraw(false);
        try {
            // Add events in reverse order
            int itemIndex = 0;
            for (int i = events.size(); i > 0; i--) {
                if (itemIndex >= entriesPerPage) {
                    // Do not add remaining (older) events - they don't fit page anyway
                    break;
                }
                QMMetaEvent event = events.get(i - 1);
                if (!defaultFilter.accept(event) || (filter != null && !filter.accept(event))) {
                    continue;
                }
                QMMObject object = event.getObject();
                if (object instanceof QMMStatementExecuteInfo) {
                    itemIndex = createOrUpdateItem(event, itemIndex);
                } else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo) {
                    itemIndex = createOrUpdateItem(event, itemIndex);
                    // Update all dependent statements
                    if (object instanceof QMMTransactionInfo) {
                        for (QMMTransactionSavepointInfo savepoint = ((QMMTransactionInfo) object).getCurrentSavepoint(); savepoint != null; savepoint = savepoint.getPrevious()) {
                            updateExecutions(event, savepoint);
                        }

                    } else {
                        updateExecutions(event, (QMMTransactionSavepointInfo) object);
                    }
                } else if (object instanceof QMMSessionInfo) {
                    QMMetaEvent.Action action = event.getAction();
                    if (action == QMMetaEvent.Action.BEGIN || action == QMMetaEvent.Action.END) {
                        TableItem item = new TableItem(logTable, SWT.NONE, itemIndex++);
                        updateItem(event, item);
                    }
                }
            }
            int itemCount = logTable.getItemCount();
            if (itemCount > entriesPerPage) {
                int[] indexes = new int[itemCount - entriesPerPage];
                for (int i = 0; i < itemCount - entriesPerPage; i++) {
                    indexes[i] = entriesPerPage + i;
                    TableItem tableItem = logTable.getItem(entriesPerPage + i);
                    if (tableItem != null && tableItem.getData() instanceof QMMObject) {
                        objectToItemMap.remove(((QMMObject) tableItem.getData()).getObjectId());
                    }
                }
                logTable.remove(indexes);
            }
        } catch (Exception e) {
            log.error("Error updating Query Log", e);
        }
        finally {
            if (!logTable.isDisposed()) {
                logTable.setRedraw(true);
            }
        }
    }

    private void updateExecutions(QMMetaEvent event, QMMTransactionSavepointInfo savepoint)
    {
        for (Iterator<QMMStatementExecuteInfo> i = savepoint.getExecutions(); i.hasNext(); ) {
            QMMStatementExecuteInfo exec = i.next();
            TableItem item = objectToItemMap.get(exec.getObjectId());
            if (item != null && !item.isDisposed()) {
                item.setFont(getObjectFont(event));
                item.setForeground(getObjectForeground(event));
                item.setBackground(getObjectBackground(event));
            }
        }
    }

    private int createOrUpdateItem(QMMetaEvent event, int itemIndex)
    {
        TableItem item = objectToItemMap.get(event.getObject().getObjectId());
        if (item == null) {
            item = new TableItem(logTable, SWT.NONE, itemIndex++);
            objectToItemMap.put(event.getObject().getObjectId(), item);
        }
        updateItem(event, item);
        return itemIndex;
    }

    private void updateItem(QMMetaEvent event, TableItem item)
    {
        item.setData(event);
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            ColumnDescriptor cd = columns.get(i);
            item.setText(i, TextUtils.getSingleLineString(cd.logColumn.getText(event)));
        }
        item.setFont(getObjectFont(event));
        item.setForeground(getObjectForeground(event));
        item.setBackground(getObjectBackground(event));
    }

    private void createContextMenu()
    {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(logTable);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                IAction copyAction = new Action(CoreMessages.controls_querylog_action_copy) {
                    @Override
                    public void run()
                    {
                        copySelectionToClipboard(false);
                    }
                };
                copyAction.setEnabled(logTable.getSelectionCount() > 0);
                copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

                IAction copyAllAction = new Action(CoreMessages.controls_querylog_action_copy_all_fields) {
                    @Override
                    public void run()
                    {
                        copySelectionToClipboard(true);
                    }
                };
                copyAllAction.setEnabled(logTable.getSelectionCount() > 0);
                copyAllAction.setActionDefinitionId(ICommandIds.CMD_COPY_SPECIAL);

                IAction selectAllAction = new Action(CoreMessages.controls_querylog_action_select_all) {
                    @Override
                    public void run()
                    {
                        selectAll();
                    }
                };
                selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

                IAction clearLogAction = new Action(CoreMessages.controls_querylog_action_clear_log) {
                    @Override
                    public void run()
                    {
                        clearLog();
                    }
                };

                manager.add(copyAction);
                manager.add(copyAllAction);
                manager.add(selectAllAction);
                manager.add(clearLogAction);
                //manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
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

        dndSource = new DragSource(logTable, operations);
        dndSource.setTransfer(types);
        dndSource.addDragListener (new DragSourceListener() {

            @Override
            public void dragStart(DragSourceEvent event) {
            }
            @Override
            public void dragSetData (DragSourceEvent event) {
                String tdt = getSelectedText(false);
                if (!CommonUtils.isEmpty(tdt)) {
                    event.data = tdt;
                } else {
                    event.data = ""; //$NON-NLS-1$
                }
            }
            @Override
            public void dragFinished(DragSourceEvent event) {
            }
        });
    }

    public synchronized void clearLog()
    {
        logTable.removeAll();
        objectToItemMap.clear();
    }

    public void selectAll()
    {
        if (!logTable.isDisposed()) {
            logTable.selectAll();
        }
    }

    public void copySelectionToClipboard(boolean extraInfo)
    {
        String tdt = getSelectedText(extraInfo);
        if (CommonUtils.isEmpty(tdt)) {
            return;
        }

        if (tdt.length() > 0) {
            UIUtils.setClipboardContents(logTable.getDisplay(), TextTransfer.getInstance(), tdt);
        }
    }

    private String getSelectedText(boolean extraInfo)
    {
        IStructuredSelection selection = getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        StringBuilder tdt = new StringBuilder();
        for (Iterator<?> i = selection.iterator(); i.hasNext(); ) {
            QMMetaEvent item = (QMMetaEvent)i.next();
            if (tdt.length() > 0) {
                tdt.append(GeneralUtils.getDefaultLineSeparator());
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
        return NLS.bind(CoreMessages.controls_querylog_format_minutes, String.valueOf(min), String.valueOf(sec));
    }

    private ConfigRefreshJob configRefreshJob = null;

    @Override
    public synchronized void preferenceChange(PreferenceChangeEvent event)
    {
        if (event.getProperty().startsWith(QMConstants.PROP_PREFIX)) {
            // Many properties could be changed at once
            // So here we just schedule single refresh job
            if (configRefreshJob == null) {
                configRefreshJob = new ConfigRefreshJob();
                configRefreshJob.schedule(250);
                configRefreshJob.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event)
                    {
                        configRefreshJob = null;
                    }
                });
            }
        }
    }

    private class ConfigRefreshJob extends AbstractUIJob {
        protected ConfigRefreshJob()
        {
            super(CoreMessages.controls_querylog_job_refresh);
        }
        @Override
        protected IStatus runInUIThread(DBRProgressMonitor monitor)
        {
            reloadEvents();
            return Status.OK_STATUS;
        }
    }


    private class EventViewDialog extends BaseSQLDialog {

        private static final String DIALOG_ID = "DBeaver.QM.EventViewDialog";//$NON-NLS-1$

        private final QMMetaEvent object;

        protected EventViewDialog(QMMetaEvent object)
        {
            super(QueryLogViewer.this.site, "Event", null);
            setShellStyle(SWT.SHELL_TRIM);
            this.object = object;
        }

        @Override
        protected IDialogSettings getDialogBoundsSettings() {
            return UIUtils.getDialogSettings(DIALOG_ID);
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText(CoreMessages.controls_querylog_shell_text + COLUMN_TYPE.getText(object));
        }

	    @Override
        protected Composite createDialogArea(Composite parent) {

            final Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite.setLayout(new GridLayout(1, false));

            final Composite topFrame = UIUtils.createPlaceholder(composite, 2, 5);
            topFrame.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createLabelText(topFrame, CoreMessages.controls_querylog_label_time, COLUMN_TIME.getText(object), SWT.READ_ONLY);
            UIUtils.createLabelText(topFrame, CoreMessages.controls_querylog_label_type, COLUMN_TYPE.getText(object), SWT.BORDER | SWT.READ_ONLY);

            final Label messageLabel = UIUtils.createControlLabel(topFrame, CoreMessages.controls_querylog_label_text);
            messageLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            Control msg;
            if (object.getObject() instanceof QMMStatementExecuteInfo) {
                msg = createSQLPanel(topFrame);
            } else {
                final Text messageText = new Text(topFrame, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
                messageText.setText(COLUMN_TEXT.getText(object));
                msg = messageText;
            }
            GridData gd = new GridData(GridData.FILL_BOTH);
            //gd.heightHint = 40;
            gd.widthHint = 500;
            msg.setLayoutData(gd);

            final Composite bottomFrame = UIUtils.createPlaceholder(composite, 1, 5);
            bottomFrame.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            final Label resultLabel = UIUtils.createControlLabel(bottomFrame, CoreMessages.controls_querylog_label_result);
            resultLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            final Text resultText = new Text(bottomFrame, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
            resultText.setText(COLUMN_RESULT.getText(object));
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.heightHint = 60;
            gd.widthHint = 300;
            resultText.setLayoutData(gd);

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }

        @Override
        protected DBCExecutionContext getExecutionContext() {
            if (object.getObject() instanceof QMMStatementExecuteInfo) {
                return ((QMMStatementExecuteInfo) object.getObject()).getStatement().getSession().getReference();
            }
            return null;
        }

        @Override
        protected String getSQLText() {
            return COLUMN_TEXT.getText(object);
        }

        @Override
        protected boolean isLabelVisible() {
            return false;
        }
    }

}