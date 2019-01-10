/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.querylog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.*;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.qm.DefaultEventFilter;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.controls.TableColumnSortListener;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.BaseSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.handlers.OpenHandler;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * QueryLogViewer
 */
public class QueryLogViewer extends Viewer implements QMMetaListener, DBPPreferenceListener {

    private static final Log log = Log.getLog(QueryLogViewer.class);

    private static final String QUERY_LOG_CONTROL_ID = "org.jkiss.dbeaver.ui.qm.log"; //$NON-NLS-1$
    private static final String VIEWER_ID = "DBeaver.QM.LogViewer";
    private static final int MIN_ENTRIES_PER_PAGE = 1;

    public static final String COLOR_UNCOMMITTED = "org.jkiss.dbeaver.txn.color.committed.background";  //= new RGB(0xBD, 0xFE, 0xBF);
    public static final String COLOR_REVERTED = "org.jkiss.dbeaver.txn.color.reverted.background";  // = new RGB(0xFF, 0x63, 0x47);
    public static final String COLOR_TRANSACTION = "org.jkiss.dbeaver.txn.color.transaction.background";  // = new RGB(0xFF, 0xE4, 0xB5);

    private static NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

    private static abstract class LogColumn {
        private final String id;
        private final String title;
        private final String toolTip;
        private final int widthHint;
        private LogColumn(String id, String title, String toolTip, int widthHint)
        {
            this.id = id;
            this.title = title;
            this.toolTip = toolTip;
            this.widthHint = widthHint;
        }
        abstract String getText(QMMetaEvent event);
        String getToolTipText(QMMetaEvent event) {
            return getText(event);
        }
    }

    private static class ColumnDescriptor {
        LogColumn logColumn;
        TableColumn tableColumn;

        ColumnDescriptor(LogColumn logColumn, TableColumn tableColumn)
        {
            this.logColumn = logColumn;
            this.tableColumn = tableColumn;
        }
    }

    private LogColumn COLUMN_TIME = new LogColumn("time", SQLEditorMessages.controls_querylog_column_time_name, SQLEditorMessages.controls_querylog_column_time_tooltip, 80) {
        private final DateFormat timeFormat = new SimpleDateFormat("MMM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
        private final DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()); //$NON-NLS-1$
        @Override
        String getText(QMMetaEvent event)
        {
            return timeFormat.format(event.getObject().getOpenTime());
        }
        String getToolTipText(QMMetaEvent event) {
            return timestampFormat.format(event.getObject().getOpenTime());
        }
    };
    private static LogColumn COLUMN_TYPE = new LogColumn("type", SQLEditorMessages.controls_querylog_column_type_name, SQLEditorMessages.controls_querylog_column_type_tooltip, 100) {
        @Override
        String getText(QMMetaEvent event)
        {
            return getObjectType(event.getObject());
        }
    };
    private static LogColumn COLUMN_TEXT = new LogColumn("text", SQLEditorMessages.controls_querylog_column_text_name, SQLEditorMessages.controls_querylog_column_text_tooltip, 400) {
        @Override
        String getText(QMMetaEvent event)
        {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo statement = (QMMStatementExecuteInfo) object;
                //return SQLUtils.stripTransformations(statement.getQueryString());
                return CommonUtils.truncateString(
                    CommonUtils.notEmpty(statement.getQueryString()), 4000);
            } else if (object instanceof QMMTransactionInfo) {
                if (((QMMTransactionInfo) object).isCommitted()) {
                    return SQLEditorMessages.controls_querylog_commit;
                } else {
                    return SQLEditorMessages.controls_querylog_rollback;
                }
            } else if (object instanceof QMMTransactionSavepointInfo) {
                if (((QMMTransactionSavepointInfo) object).isCommitted()) {
                    return SQLEditorMessages.controls_querylog_commit;
                } else {
                    return SQLEditorMessages.controls_querylog_rollback;
                }
            } else if (object instanceof QMMSessionInfo) {
                String containerName = ((QMMSessionInfo) object).getContainerName();
                switch (event.getAction()) {
                    case BEGIN: return SQLEditorMessages.controls_querylog_connected_to + containerName + "\"";
                    case END:   return SQLEditorMessages.controls_querylog_disconnected_from + containerName + "\"";
                    default:    return "?";
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static LogColumn COLUMN_DURATION = new LogColumn("duration", SQLEditorMessages.controls_querylog_column_duration_name + " (" + SQLEditorMessages.controls_querylog__ms + ")", SQLEditorMessages.controls_querylog_column_duration_tooltip, 100) {
        @Override
        String getText(QMMetaEvent event)
        {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) object;
                if (exec.isClosed()) {
                    final long execTime = exec.getCloseTime() - exec.getOpenTime();
                    final long fetchTime = exec.isFetching() ? 0 : exec.getFetchEndTime() - exec.getFetchBeginTime();
                    return NUMBER_FORMAT.format(execTime + fetchTime);
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
    private static LogColumn COLUMN_ROWS = new LogColumn("rows", SQLEditorMessages.controls_querylog_column_rows_name, SQLEditorMessages.controls_querylog_column_rows_tooltip, 120) {
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
    private static LogColumn COLUMN_RESULT = new LogColumn("result", SQLEditorMessages.controls_querylog_column_result_name, SQLEditorMessages.controls_querylog_column_result_tooltip, 120) {
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
                            return SQLEditorMessages.controls_querylog_error + exec.getErrorCode() + "]";
                        } else {
                            return "[" + exec.getErrorCode() + "] " + exec.getErrorMessage();
                        }
                    } else {
                        return SQLEditorMessages.controls_querylog_success;
                    }
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static LogColumn COLUMN_DATA_SOURCE = new LogColumn("datasource", SQLEditorMessages.controls_querylog_column_connection_name, SQLEditorMessages.controls_querylog_column_connection_tooltip, 150) {
        @Override
        String getText(QMMetaEvent event)
        {
            QMMObject object = event.getObject();
            String containerName = null;
            if (object instanceof QMMSessionInfo) {
                containerName = ((QMMSessionInfo) object).getContainerName();
            } else if (object instanceof QMMTransactionInfo) {
                containerName = ((QMMTransactionInfo) object).getSession().getContainerName();
            } else if (object instanceof QMMTransactionSavepointInfo) {
                containerName = ((QMMTransactionSavepointInfo) object).getTransaction().getSession().getContainerName();
            } else if (object instanceof QMMStatementInfo) {
                containerName = ((QMMStatementInfo) object).getSession().getContainerName();
            } else if (object instanceof QMMStatementExecuteInfo) {
                containerName = ((QMMStatementExecuteInfo) object).getStatement().getSession().getContainerName();
            }
            return containerName == null ? "?" : containerName;
        }
    };
    private static LogColumn COLUMN_CONTEXT = new LogColumn("context", SQLEditorMessages.controls_querylog_column_context_name, SQLEditorMessages.controls_querylog_column_context_tooltip, 150) {
        @Override
        String getText(QMMetaEvent event) {
            QMMObject object = event.getObject();
            String contextName = null;
            if (object instanceof QMMSessionInfo) {
                contextName = ((QMMSessionInfo) object).getContextName();
            } else if (object instanceof QMMTransactionInfo) {
                contextName = ((QMMTransactionInfo) object).getSession().getContextName();
            } else if (object instanceof QMMTransactionSavepointInfo) {
                contextName = ((QMMTransactionSavepointInfo) object).getTransaction().getSession().getContextName();
            } else if (object instanceof QMMStatementInfo) {
                contextName = ((QMMStatementInfo) object).getSession().getContextName();
            } else if (object instanceof QMMStatementExecuteInfo) {
                contextName = ((QMMStatementExecuteInfo) object).getStatement().getSession().getContextName();
            }
            if (contextName == null) {
                return "?";
            }
            return contextName;
        }
    };
    private LogColumn[] ALL_COLUMNS = new LogColumn[] {
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
    private final Text searchText;
    private Table logTable;
    private java.util.List<ColumnDescriptor> columns = new ArrayList<>();
    private LongKeyMap<TableItem> objectToItemMap = new LongKeyMap<>();

    private QMEventFilter defaultFilter = new DefaultEventFilter();
    private QMEventFilter filter;
    private boolean useDefaultFilter = true;
    private boolean currentSessionOnly;

    private final Color colorLightGreen;
    private final Color colorLightRed;
    private final Color colorLightYellow;
    private final Color shadowColor;
    private final Font boldFont;
    private final Font hintFont;
    private DragSource dndSource;

    private volatile boolean reloadInProgress = false;

    private int entriesPerPage = MIN_ENTRIES_PER_PAGE;

    public QueryLogViewer(Composite parent, IWorkbenchPartSite site, QMEventFilter filter, boolean showConnection, boolean currentSessionOnly)
    {
        super();

        this.site = site;
        this.currentSessionOnly = currentSessionOnly;

        // Prepare colors

        ColorRegistry colorRegistry = site.getWorkbenchWindow().getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

        colorLightGreen = colorRegistry.get(COLOR_UNCOMMITTED);
        colorLightRed = colorRegistry.get(COLOR_REVERTED);
        colorLightYellow = colorRegistry.get(COLOR_TRANSACTION);
        shadowColor = parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        hintFont = UIUtils.modifyFont(parent.getFont(), SWT.ITALIC);

        boolean inDialog = UIUtils.isInDialog(parent);
        // Search field
        this.searchText = new Text(parent, SWT.BORDER);
        this.searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.searchText.addPaintListener(e -> {
            if (searchText.isEnabled() && searchText.getCharCount() == 0) {
                e.gc.setForeground(shadowColor);
                e.gc.setFont(hintFont);
                e.gc.drawText("Type query part to search in query history",
                    2, 0, true);
                e.gc.setFont(null);
            }
        });
        this.searchText.addModifyListener(e -> scheduleLogRefresh());
        TextEditorUtils.enableHostEditorKeyBindingsSupport(site, searchText);

        // Create log table
        logTable = new Table(
            parent,
            SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | (inDialog ? SWT.BORDER : SWT.NONE));
        logTable.setData(this);
        //logTable.setLinesVisible(true);
        logTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        logTable.setLayoutData(gd);

        new TableToolTip(logTable) {
            @Override
            public String getItemToolTip(TableItem item, int selectedColumn) {
                LogColumn column = (LogColumn) logTable.getColumn(selectedColumn).getData();
                return column.getToolTipText((QMMetaEvent) item.getData());
            }
        };

        createColumns(showConnection);


        {
            // Register control in focus service (to provide handlers binding)
            UIUtils.addFocusTracker(site, QUERY_LOG_CONTROL_ID, logTable);

            logTable.addDisposeListener(e -> {
                // Unregister from focus service
                UIUtils.removeFocusTracker(QueryLogViewer.this.site, logTable);
                dispose();
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

        // Make sure app is initialized
        DBWorkbench.getPlatformUI();
        // Register QM listener
        QMUtils.registerMetaListener(this);

        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(this);

        logTable.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event event) {
                logTable.removeListener(SWT.Resize, this);
                if (!reloadInProgress) {
                    reloadEvents(null);
                }
            }
        });
    }

    private synchronized void scheduleLogRefresh() {
        // Many properties could be changed at once
        // So here we just schedule single refresh job
        if (logRefreshJob == null) {
            logRefreshJob = new LogRefreshJob();
        }
        logRefreshJob.cancel();
        logRefreshJob.schedule(500);
    }

    public void setFilter(QMEventFilter filter) {
        this.filter = filter;
    }

    public void setUseDefaultFilter(boolean useDefaultFilter) {
        this.useDefaultFilter = useDefaultFilter;
    }

    private void showEventDetails(QMMetaEvent event)
    {
        EventViewDialog dialog = new EventViewDialog(event);
        dialog.open();
    }

    private void createColumns(boolean showConnection)
    {
        for (TableColumn tableColumn : logTable.getColumns()) {
            tableColumn.dispose();
        }
        columns.clear();

        final IDialogSettings dialogSettings = UIUtils.getDialogSettings(VIEWER_ID);

        int colIndex = 0;
        for (final LogColumn logColumn : ALL_COLUMNS) {
            if (!showConnection && (logColumn == COLUMN_DATA_SOURCE || logColumn == COLUMN_CONTEXT)) {
                continue;
            }
            final TableColumn tableColumn = UIUtils.createTableColumn(logTable, SWT.NONE, logColumn.title);
            tableColumn.setData(logColumn);
            final String colWidth = dialogSettings.get("column-" + logColumn.id);
            if (colWidth != null) {
                tableColumn.setWidth(Integer.parseInt(colWidth));
            } else {
                tableColumn.setWidth(logColumn.widthHint);
            }
            tableColumn.setToolTipText(logColumn.toolTip);

            final ColumnDescriptor cd = new ColumnDescriptor(logColumn, tableColumn);
            columns.add(cd);

            tableColumn.addListener(SWT.Selection, new TableColumnSortListener(logTable, colIndex));
            tableColumn.addListener(SWT.Resize, event -> {
                final int width = tableColumn.getWidth();
                dialogSettings.put("column-" + logColumn.id, String.valueOf(width));
            });

            colIndex++;
        }
    }

    private void dispose()
    {
        DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(this);
        QMUtils.unregisterMetaListener(this);
        UIUtils.dispose(dndSource);
        UIUtils.dispose(logTable);
        UIUtils.dispose(boldFont);
        UIUtils.dispose(hintFont);
    }

    @Override
    public Table getControl()
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
        reloadEvents(searchText.getText());
    }

    private static String getObjectType(QMMObject object)
    {
        if (object instanceof QMMSessionInfo) {
            return ModelMessages.model_navigator_Connection;
        } else if (object instanceof QMMStatementInfo || object instanceof QMMStatementExecuteInfo) {
            QMMStatementInfo statement;
            if (object instanceof QMMStatementInfo) {
                statement = (QMMStatementInfo) object;
            } else {
                statement = ((QMMStatementExecuteInfo)object).getStatement();
            }
            return "SQL" + (statement == null ? "" : " / " + CommonUtils.capitalizeWord(statement.getPurpose().getTitle())); //$NON-NLS-1$
//        } else if (object instanceof QMMStatementScripInfo) {
//            return SQLEditorMessages.controls_querylog_script;
        } else if (object instanceof QMMTransactionInfo) {
            return SQLEditorMessages.controls_querylog_transaction;
        } else if (object instanceof QMMTransactionSavepointInfo) {
            return SQLEditorMessages.controls_querylog_savepoint;
        }
        return ""; //$NON-NLS-1$
    }

    private Font getObjectFont(QMMetaEvent event)
    {
        if (event.getObject() instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) event.getObject();
            if (!exec.isClosed() || exec.isFetching()) {
                return boldFont;
            }
        }
        return null;
    }

    private Color getObjectForeground(QMMetaEvent event)
    {
/*
        if (getObjectBackground(event) != null) {
            return colorGray;
        }
*/
/*
        if (event.getObject() instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) event.getObject();
            if (exec.getStatement().getPurpose().isUser()) {
                return null;
            } else {
                return colorGray;
            }
        }
*/
        return null;
    }

    private Color getObjectBackground(QMMetaEvent event)
    {
        if (event.getObject() instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo exec = (QMMStatementExecuteInfo) event.getObject();
            if (exec.hasError()) {
                return colorLightRed;
            }
            QMMTransactionSavepointInfo savepoint = exec.getSavepoint();
            if (savepoint == null) {
                return null;
            } else if (savepoint.isClosed()) {
                return savepoint.isCommitted() ? colorLightGreen : colorLightYellow;
            } else {
                return colorLightGreen;
            }
        } else if (event.getObject() instanceof QMMTransactionInfo || event.getObject() instanceof QMMTransactionSavepointInfo) {
            QMMTransactionSavepointInfo savepoint;
            if (event.getObject() instanceof QMMTransactionInfo) {
                savepoint = ((QMMTransactionInfo) event.getObject()).getCurrentSavepoint();
            } else {
                savepoint = (QMMTransactionSavepointInfo) event.getObject();
            }
            return savepoint.isCommitted() ? null : colorLightYellow;
        }
        return null;
    }

    private void reloadEvents(@Nullable String searchString)
    {
        if (reloadInProgress) {
            log.debug("Event reload is in progress. Skip");
            return;
        }
        reloadInProgress = true;
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        this.entriesPerPage = Math.max(MIN_ENTRIES_PER_PAGE, store.getInt(QMConstants.PROP_ENTRIES_PER_PAGE));
        this.defaultFilter = new DefaultEventFilter();

        clearLog();

        // Extract events

        EventHistoryReadService loadingService = new EventHistoryReadService(searchString);
        LoadingJob.createService(
            loadingService,
            new EvenHistoryReadVisualizer(loadingService))
            .schedule();
    }

    @Override
    public void metaInfoChanged(DBRProgressMonitor monitor, @NotNull final List<QMMetaEvent> events) {
        if (DBWorkbench.getPlatform().isShuttingDown()) {
            return;
        }
        // Run in UI thread
        UIUtils.asyncExec(() -> updateMetaInfo(events));
    }

    private synchronized void updateMetaInfo(final List<QMMetaEvent> events)
    {
        if (logTable.isDisposed()) {
            return;
        }
        logTable.setRedraw(false);
        try {
            // Add events in reverse order
            int itemIndex = 0;
            for (int i = 0; i < events.size(); i++) {
                if (useDefaultFilter && itemIndex >= entriesPerPage) {
                    // Do not add remaining (older) events - they don't fit page anyway
                    break;
                }
                QMMetaEvent event = events.get(i);
                if ((filter != null && !filter.accept(event)) || (useDefaultFilter && !defaultFilter.accept(event))) {
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
            if (exec.hasError()) {
                // Do not update color of failed executions (it has to be red)
                continue;
            }
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
        if (item.isDisposed()) {
            return;
        }
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
        menuMgr.addMenuListener(manager -> {
            IAction editorAction = new Action("Open in SQL console", DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE)) {
                @Override
                public void run()
                {
                    openSelectionInEditor();
                }
            };
            IAction copyAction = new Action(SQLEditorMessages.controls_querylog_action_copy) {
                @Override
                public void run()
                {
                    copySelectionToClipboard(false);
                }
            };
            copyAction.setEnabled(logTable.getSelectionCount() > 0);
            copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

            IAction copyAllAction = new Action(SQLEditorMessages.controls_querylog_action_copy_all_fields) {
                @Override
                public void run()
                {
                    copySelectionToClipboard(true);
                }
            };
            copyAllAction.setEnabled(logTable.getSelectionCount() > 0);
            copyAllAction.setActionDefinitionId(IActionConstants.CMD_COPY_SPECIAL);

            IAction selectAllAction = new Action(SQLEditorMessages.controls_querylog_action_select_all) {
                @Override
                public void run()
                {
                    selectAll();
                }
            };
            selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

            IAction clearLogAction = new Action(SQLEditorMessages.controls_querylog_action_clear_log) {
                @Override
                public void run()
                {
                    clearLog();
                }
            };

            boolean hasStatements = false;
            for (TableItem item : logTable.getSelection()) {
                if (((QMMetaEvent)item.getData()).getObject() instanceof QMMStatementExecuteInfo) {
                    hasStatements = true;
                    break;
                }
            }
            if (hasStatements) {
                manager.add(editorAction);
                manager.add(new Separator());
            }
            manager.add(copyAction);
            manager.add(copyAllAction);
            manager.add(selectAllAction);
            manager.add(clearLogAction);
            manager.add(ActionUtils.makeCommandContribution(site, IWorkbenchCommandConstants.FILE_REFRESH));
            //manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

            manager.add(new Separator());
            createFiltersMenu(manager);
        });
        menuMgr.setRemoveAllWhenShown(true);
        logTable.setMenu(menu);
        site.registerContextMenu(menuMgr, this);
    }

    private void createFiltersMenu(IMenuManager manager) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        QMEventCriteria criteria = QMUtils.createDefaultCriteria(store);
        for (DBCExecutionPurpose purpose : DBCExecutionPurpose.values()) {
            IAction toggleAction = new Action(purpose.getTitle(), Action.AS_CHECK_BOX) {
                @Override
                public boolean isChecked() {
                    return criteria.hasQueryType(purpose);
                }
                @Override
                public void run() {
                    DBCExecutionPurpose[] queryTypes = criteria.getQueryTypes();
                    if (isChecked()) {
                        queryTypes = ArrayUtils.remove(DBCExecutionPurpose.class, queryTypes, purpose);
                    } else {
                        queryTypes = ArrayUtils.add(DBCExecutionPurpose.class, queryTypes, purpose);
                    }
                    List<String> typeNames = new ArrayList<>(queryTypes.length);
                    for (DBCExecutionPurpose queryType : queryTypes) typeNames.add(queryType.name());
                    store.setValue(QMConstants.PROP_QUERY_TYPES, CommonUtils.makeString(typeNames, ','));
                    PrefUtils.savePreferenceStore(store);
                    scheduleLogRefresh();
                }
            };
            manager.add(toggleAction);
        }
        manager.add(new Separator());
        for (QMObjectType type : QMObjectType.values()) {
            IAction toggleAction = new Action(type.getTitle(), Action.AS_CHECK_BOX) {
                @Override
                public boolean isChecked() {
                    return criteria.hasObjectType(type);
                }
                @Override
                public void run() {
                    QMObjectType[] objectTypes = criteria.getObjectTypes();
                    if (isChecked()) {
                        objectTypes = ArrayUtils.remove(QMObjectType.class, objectTypes, type);
                    } else {
                        objectTypes = ArrayUtils.add(QMObjectType.class, objectTypes, type);
                    }
                    List<QMObjectType> typeList = new ArrayList<>();
                    Collections.addAll(typeList, objectTypes);
                    store.setValue(QMConstants.PROP_OBJECT_TYPES, QMObjectType.toString(typeList));
                    PrefUtils.savePreferenceStore(store);
                    scheduleLogRefresh();
                }
            };
            manager.add(toggleAction);
        }
    }

    private void openSelectionInEditor() {
        DBPDataSourceContainer dsContainer = null;
        StringBuilder sql = new StringBuilder();
        TableItem[] items = logTable.getSelection();
        for (TableItem item : items) {
            QMMetaEvent event = (QMMetaEvent) item.getData();
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {
                QMMStatementExecuteInfo stmtExec = (QMMStatementExecuteInfo) object;
                if (dsContainer == null) {
                    String containerId = stmtExec.getStatement().getSession().getContainerId();
                    dsContainer = DBUtils.findDataSource(containerId);
                }
                String queryString = stmtExec.getQueryString();
                if (!CommonUtils.isEmptyTrimmed(queryString)) {
                    if (sql.length() > 0) {
                        sql.append("\n");
                    }
                    queryString = queryString.trim();
                    sql.append(queryString);
                    if (!queryString.endsWith(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
                        sql.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER).append("\n");
                    }
                }
            }
        }
        if (sql.length() > 0) {
            OpenHandler.openSQLConsole(
                UIUtils.getActiveWorkbenchWindow(),
                dsContainer,
                "QueryManager",
                sql.toString()
            );
        }
    }

    private void addDragAndDropSupport()
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
        return NLS.bind(SQLEditorMessages.controls_querylog_format_minutes, String.valueOf(min), String.valueOf(sec));
    }

    private LogRefreshJob logRefreshJob = null;

    @Override
    public synchronized void preferenceChange(PreferenceChangeEvent event)
    {
        if (event.getProperty().startsWith(QMConstants.PROP_PREFIX)) {
            scheduleLogRefresh();
        }
    }

    private class LogRefreshJob extends AbstractUIJob {
        LogRefreshJob()
        {
            super(SQLEditorMessages.controls_querylog_job_refresh);
        }
        @Override
        protected IStatus runInUIThread(DBRProgressMonitor monitor)
        {
            refresh();
            return Status.OK_STATUS;
        }
    }


    private class EventViewDialog extends BaseSQLDialog {

        private static final String DIALOG_ID = "DBeaver.QM.EventViewDialog";//$NON-NLS-1$

        private final QMMetaEvent object;

        EventViewDialog(QMMetaEvent object)
        {
            super(QueryLogViewer.this.getControl().getShell(), QueryLogViewer.this.site, "Event", null);
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
            shell.setText(SQLEditorMessages.controls_querylog_shell_text + COLUMN_TYPE.getText(object));
        }

	    @Override
        protected Composite createDialogArea(Composite parent) {

            final Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite.setLayout(new GridLayout(1, false));

            final Composite topFrame = UIUtils.createPlaceholder(composite, 2, 5);
            topFrame.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createLabelText(topFrame, SQLEditorMessages.controls_querylog_label_time, COLUMN_TIME.getText(object), SWT.READ_ONLY);
            UIUtils.createLabelText(topFrame, SQLEditorMessages.controls_querylog_label_type, COLUMN_TYPE.getText(object), SWT.BORDER | SWT.READ_ONLY);

            final Label messageLabel = UIUtils.createControlLabel(topFrame, SQLEditorMessages.controls_querylog_label_text);
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

            final Label resultLabel = UIUtils.createControlLabel(bottomFrame, SQLEditorMessages.controls_querylog_label_result);
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
        protected SQLDialect getSQLDialect() {
            if (object.getObject() instanceof QMMStatementExecuteInfo) {
                SQLDialect dialect = ((QMMStatementExecuteInfo) object.getObject()).getStatement().getSession().getSQLDialect();
                if (dialect != null) {
                    return dialect;
                }
            }
            return super.getSQLDialect();
        }

        @Override
        protected DBCExecutionContext getExecutionContext() {
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

    class EventHistoryReadService extends AbstractLoadService<List<QMMetaEvent>> {

        @Nullable
        private String searchString;

        protected EventHistoryReadService(@Nullable String searchString) {
            super("Load query history");
            this.searchString = searchString;
        }

        @Override
        public List<QMMetaEvent> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            final List<QMMetaEvent> events = new ArrayList<>();
            QMEventBrowser eventBrowser = QMUtils.getEventBrowser(currentSessionOnly);
            if (eventBrowser != null) {
                QMEventCriteria criteria = QMUtils.createDefaultCriteria(DBWorkbench.getPlatform().getPreferenceStore());
                criteria.setSearchString(CommonUtils.isEmptyTrimmed(searchString) ? null : searchString.trim());

                monitor.beginTask("Load query history", 1);
                if (!CommonUtils.isEmpty(searchString)) {
                    monitor.subTask("Search queries: " + searchString);
                } else {
                    monitor.subTask("Load all queries");
                }
                try (QMEventCursor cursor = eventBrowser.getQueryHistoryCursor(monitor, criteria)) {
                    while (events.size() < entriesPerPage && cursor.hasNextEvent(monitor)) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        events.add(cursor.nextEvent(monitor));
                        monitor.subTask(events.get(events.size() - 1).toString());
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
                monitor.done();
            }
            return events;
        }

        @Override
        public Object getFamily() {
            return QueryLogViewer.class;
        }

    }

    private class EvenHistoryReadVisualizer extends ProgressLoaderVisualizer<List<QMMetaEvent>> {
        public EvenHistoryReadVisualizer(EventHistoryReadService loadingService) {
            super(loadingService, logTable);
        }

        @Override
        public void visualizeLoading() {
            reloadInProgress = true;
            super.visualizeLoading();
        }

        @Override
        public void completeLoading(List<QMMetaEvent> result) {
            try {
                super.completeLoading(result);
                super.visualizeLoading();
                if (result != null) {
                    updateMetaInfo(result);
                }
            } finally {
                reloadInProgress = false;
            }
        }
    }

}