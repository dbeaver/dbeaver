/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.*;
import org.jkiss.dbeaver.model.qm.filters.QMCursorFilter;
import org.jkiss.dbeaver.model.qm.filters.QMEventCriteria;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.qm.DefaultEventFilter;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.controls.TableColumnSortListener;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.BaseSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.log.SQLLogFilter;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.LongKeyMap;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

/**
 * QueryLogViewer
 */
public class QueryLogViewer extends Viewer implements QMMetaListener, DBPPreferenceListener {

    private static final Log log = Log.getLog(QueryLogViewer.class);

    private static final String QUERY_LOG_CONTROL_ID = "org.jkiss.dbeaver.ui.qm.log"; //$NON-NLS-1$
    private static final String VIEWER_ID = "DBeaver.QM.LogViewer"; //$NON-NLS-1$
    private static final int MIN_ENTRIES_PER_PAGE = 1;

    private static abstract class LogColumn {
        private final String id;
        private final String title;
        private final String toolTip;
        private final int widthHint;

        private LogColumn(String id, String title, String toolTip, int widthHint) {
            this.id = id;
            this.title = title;
            this.toolTip = toolTip;
            this.widthHint = widthHint;
        }

        abstract String getText(QMEvent event, boolean briefInfo);

        String getToolTipText(QMEvent event) {
            return getText(event, true);
        }

        @Nullable
        Comparator<QMEvent> getComparator() {
            return null;
        }
    }

    private static class ColumnDescriptor {
        LogColumn logColumn;
        TableColumn tableColumn;

        ColumnDescriptor(LogColumn logColumn, TableColumn tableColumn) {
            this.logColumn = logColumn;
            this.tableColumn = tableColumn;
        }
    }

    private final LogColumn COLUMN_TIME = new LogColumn("time", ModelMessages.controls_querylog_column_time_name, ModelMessages.controls_querylog_column_time_tooltip, 80) { //$NON-NLS-1$
        private final DateFormat timeFormat = new SimpleDateFormat("MMM-dd HH:mm:ss", Locale.getDefault()); //$NON-NLS-1$
        private final DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()); //$NON-NLS-1$

        @Override
        String getText(QMEvent event, boolean briefInfo) {
            return timeFormat.format(QMUtils.getObjectEventTime(event.getObject(), event.getAction()));
        }

        String getToolTipText(QMEvent event) {
            return timestampFormat.format(event.getObject().getOpenTime());
        }

        @Nullable
        @Override
        Comparator<QMEvent> getComparator() {
            return Comparator.comparingLong(e -> QMUtils.getObjectEventTime(e.getObject(), e.getAction()));
        }
    };
    private static final LogColumn COLUMN_TYPE = new LogColumn("type", ModelMessages.controls_querylog_column_type_name, ModelMessages.controls_querylog_column_type_tooltip, 100) { //$NON-NLS-1$
        @Override
        String getText(QMEvent event, boolean briefInfo) {
            return getObjectType(event.getObject());
        }
    };
    private static final LogColumn COLUMN_TEXT = new LogColumn("text", ModelMessages.controls_querylog_column_text_name, ModelMessages.controls_querylog_column_text_tooltip, 400) { //$NON-NLS-1$
        @Override
        String getText(QMEvent event, boolean briefInfo) {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo statement) {
                //return SQLUtils.stripTransformations(statement.getQueryString());
                String text = CommonUtils.notEmpty(statement.getQueryString());
                if (briefInfo) {
                    text = CommonUtils.truncateString(text, 4000);
                }
                return text;
            } else if (object instanceof QMMTransactionInfo) {
                if (((QMMTransactionInfo) object).isCommitted()) {
                    return ModelMessages.controls_querylog_commit;
                } else {
                    return ModelMessages.controls_querylog_rollback;
                }
            } else if (object instanceof QMMTransactionSavepointInfo) {
                if (((QMMTransactionSavepointInfo) object).isCommitted()) {
                    return ModelMessages.controls_querylog_commit;
                } else {
                    return ModelMessages.controls_querylog_rollback;
                }
            } else if (object instanceof QMMConnectionInfo conInfo) {
                String containerName = conInfo.getContainerName();
                String instanceId = conInfo.getInstanceId();
                String contextName = conInfo.getContextName();
                String containerFullName = containerName;
                if (!CommonUtils.equalObjects(containerName, instanceId)) {
                    containerFullName += " <" + instanceId + ">";
                }
                //containerFullName += " {" + contextName + "}";
                return switch (event.getAction()) {
                    case BEGIN -> ModelMessages.controls_querylog_connected_to + containerFullName + "\""; //$NON-NLS-1$
                    case END ->
                        ModelMessages.controls_querylog_disconnected_from + containerFullName + "\""; //$NON-NLS-1$
                    default -> "?"; //$NON-NLS-1$
                };
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static final LogColumn COLUMN_DURATION = new LogColumn("duration", ModelMessages.controls_querylog_column_duration_name, ModelMessages.controls_querylog_column_duration_tooltip, 100) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        @Override
        String getText(QMEvent event, boolean briefInfo) {
            QMMObject object = event.getObject();
            if (object.isClosed()) {
                return RuntimeUtils.formatExecutionTime(object.getDuration());
            } else {
                return ""; //$NON-NLS-1$
            }
        }

        @Override
        Comparator<QMEvent> getComparator() {
            return Comparator.comparingLong(e -> e.getObject().getDuration());
        }
    };
    private static final LogColumn COLUMN_ROWS = new LogColumn("rows", ModelMessages.controls_querylog_column_rows_name, ModelMessages.controls_querylog_column_rows_tooltip, 120) { //$NON-NLS-1$
        @Override
        String getText(QMEvent event, boolean briefInfo) {
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo exec) {
                if (exec.isClosed() && !exec.isFetching()) {
                    long updateRowCount = exec.getUpdateRowCount();
                    long fetchRowCount = exec.getFetchRowCount();
                    if (updateRowCount < 0 && fetchRowCount <= 0) {
                        return ""; //$NON-NLS-1$
                    } else if (updateRowCount < 0) {
                        return String.valueOf(fetchRowCount);
                    } else if (fetchRowCount <= 0) {
                        return String.valueOf(updateRowCount);
                    } else {
                        return fetchRowCount + "/" + updateRowCount;
                    }
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static final LogColumn COLUMN_RESULT = new LogColumn("result", ModelMessages.controls_querylog_column_result_name, ModelMessages.controls_querylog_column_result_tooltip, 120) { //$NON-NLS-1$
        @Override
        String getText(QMEvent event, boolean briefInfo) {
            if (event.getObject() instanceof QMMStatementExecuteInfo exec) {
                if (exec.isClosed()) {
                    if (exec.hasError()) {
                        if (exec.getErrorCode() == 0) {
                            return exec.getErrorMessage();
                        } else if (exec.getErrorMessage() == null) {
                            return ModelMessages.controls_querylog_error + exec.getErrorCode() + "]"; //$NON-NLS-1$
                        } else {
                            return "[" + exec.getErrorCode() + "] " + exec.getErrorMessage(); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    } else {
                        return ModelMessages.controls_querylog_success;
                    }
                }
            }
            return ""; //$NON-NLS-1$
        }
    };
    private static final LogColumn COLUMN_DATA_SOURCE = new LogColumn("datasource", ModelMessages.controls_querylog_column_connection_name, ModelMessages.controls_querylog_column_connection_tooltip, 150) { //$NON-NLS-1$
        @Override
        String getText(QMEvent event, boolean briefInfo) {
            QMMObject object = event.getObject();
            String containerName = null;
            if (object instanceof QMMConnectionInfo) {
                containerName = ((QMMConnectionInfo) object).getContainerName();
            } else if (object instanceof QMMTransactionInfo) {
                containerName = object.getConnection().getContainerName();
            } else if (object instanceof QMMTransactionSavepointInfo) {
                containerName = ((QMMTransactionSavepointInfo) object).getTransaction().getConnection().getContainerName();
            } else if (object instanceof QMMStatementInfo) {
                containerName = object.getConnection().getContainerName();
            } else if (object instanceof QMMStatementExecuteInfo) {
                containerName = ((QMMStatementExecuteInfo) object).getStatement().getConnection().getContainerName();
            }
            return containerName == null ? "?" : containerName; //$NON-NLS-1$
        }
    };
    private static final LogColumn COLUMN_CONTEXT = new LogColumn("context", ModelMessages.controls_querylog_column_context_name, ModelMessages.controls_querylog_column_context_tooltip, 150) { //$NON-NLS-1$
        @Override
        String getText(QMEvent event, boolean briefInfo) {
            QMMObject object = event.getObject();
            String contextName = null;
            if (object instanceof QMMConnectionInfo) {
                contextName = ((QMMConnectionInfo) object).getContextName();
            } else if (object instanceof QMMTransactionInfo) {
                contextName = object.getConnection().getContextName();
            } else if (object instanceof QMMTransactionSavepointInfo) {
                contextName = ((QMMTransactionSavepointInfo) object).getTransaction().getConnection().getContextName();
            } else if (object instanceof QMMStatementInfo) {
                contextName = object.getConnection().getContextName();
            } else if (object instanceof QMMStatementExecuteInfo) {
                contextName = ((QMMStatementExecuteInfo) object).getStatement().getConnection().getContextName();
            }
            if (contextName == null) {
                return "?"; //$NON-NLS-1$
            }
            return contextName;
        }
    };
    private final LogColumn[] ALL_COLUMNS = new LogColumn[]{
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
    private final Table logTable;
    private final List<ColumnDescriptor> columns = new ArrayList<>();
    private final LongKeyMap<TableItem> objectToItemMap = new LongKeyMap<>();

    private QMEventFilter defaultFilter = new DefaultEventFilter();
    private QMEventFilter filter;
    private boolean useDefaultFilter = true;
    private final boolean currentSessionOnly;

    private DragSource dndSource;

    private volatile boolean reloadInProgress = false;

    private int entriesPerPage = MIN_ENTRIES_PER_PAGE;

    public QueryLogViewer(Composite parent, IWorkbenchPartSite site, QMEventFilter filter, boolean showConnection, boolean currentSessionOnly) {
        super();

        this.site = site;
        this.currentSessionOnly = currentSessionOnly;

        // Prepare colors
        boolean inDialog = UIUtils.isInDialog(parent);
        // Search field
        this.searchText = new Text(parent, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
        this.searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.searchText.setMessage(SQLEditorMessages.editor_query_log_viewer_draw_text_type_qury_part);
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
                return column.getToolTipText((QMEvent) item.getData());
            }
        };

        createColumns(showConnection);


        {
            // Register control in focus service (to provide handlers binding)
            UIUtils.addFocusTracker(site, QUERY_LOG_CONTROL_ID, logTable);

            logTable.addDisposeListener(e -> dispose());
        }

        createContextMenu();
        addDragAndDropSupport();
        logTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                //TableItem item = (TableItem)e.item;
                showEventDetails((QMEvent) e.item.getData());
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

        UIUtils.installAndUpdateMainFont(parent);
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

    private void showEventDetails(QMEvent event) {
        EventViewDialog dialog = new EventViewDialog(event);
        dialog.open();
    }

    private void createColumns(boolean showConnection) {
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
            final String colWidth = dialogSettings.get("column-" + logColumn.id); //$NON-NLS-1$
            if (colWidth != null) {
                tableColumn.setWidth(Integer.parseInt(colWidth));
            } else {
                tableColumn.setWidth(logColumn.widthHint);
            }
            tableColumn.setToolTipText(logColumn.toolTip);

            final ColumnDescriptor cd = new ColumnDescriptor(logColumn, tableColumn);
            columns.add(cd);

            Comparator<QMEvent> comparator = logColumn.getComparator();
            TableColumnSortListener sorter = comparator != null
                ? new TableColumnSortListener(logTable, Comparator.comparing(item -> ((QMEvent) item.getData()), comparator))
                : new TableColumnSortListener(logTable, colIndex);
            tableColumn.addListener(SWT.Selection, sorter);
            tableColumn.addListener(SWT.Resize, event -> {
                final int width = tableColumn.getWidth();
                dialogSettings.put("column-" + logColumn.id, String.valueOf(width)); //$NON-NLS-1$
            });

            colIndex++;
        }
    }

    private void dispose() {
        DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(this);
        QMUtils.unregisterMetaListener(this);
        UIUtils.dispose(dndSource);
        UIUtils.dispose(logTable);
    }

    public Text getSearchText() {
        return searchText;
    }

    @Override
    public Table getControl() {
        return logTable;
    }

    @Override
    public Object getInput() {
        return null;
    }

    @Override
    public void setInput(Object input) {
    }

    @Override
    public IStructuredSelection getSelection() {
        TableItem[] items = logTable.getSelection();
        QMEvent[] data = new QMEvent[items.length];
        for (int i = 0, itemsLength = items.length; i < itemsLength; i++) {
            data[i] = (QMEvent) items[i].getData();
        }
        return new StructuredSelection(data);
    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
    }

    @Override
    public void refresh() {
        reloadEvents(searchText.getText());
    }

    private static String getObjectType(QMMObject object) {
        if (object instanceof QMMConnectionInfo) {
            return ""; //$NON-NLS-1$
        } else if (object instanceof QMMStatementInfo || object instanceof QMMStatementExecuteInfo) {
            QMMStatementInfo statement;
            if (object instanceof QMMStatementInfo) {
                statement = (QMMStatementInfo) object;
            } else {
                statement = ((QMMStatementExecuteInfo) object).getStatement();
            }
            return "SQL" + (statement == null ? "" : " / " + CommonUtils.capitalizeWord(statement.getPurpose().getTitle())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//        } else if (object instanceof QMMStatementScripInfo) {
//            return ModelMessages.controls_querylog_script;
        } else if (object instanceof QMMTransactionInfo) {
            return ModelMessages.controls_querylog_transaction;
        } else if (object instanceof QMMTransactionSavepointInfo) {
            return ModelMessages.controls_querylog_savepoint;
        }
        return ""; //$NON-NLS-1$
    }

    private Font getObjectFont(QMEvent event) {
        if (event.getObject() instanceof QMMStatementExecuteInfo exec) {
            if (!exec.isClosed() || exec.isFetching()) {
                return BaseThemeSettings.instance.baseFontBold;
            }
        }
        return null;
    }

    private Color getObjectForeground(QMEvent event) {
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

    private Color getObjectBackground(QMEvent event) {
        if (event.getObject() instanceof QMMStatementExecuteInfo exec) {
            if (exec.hasError()) {
                return QueryLogThemeSettings.instance.colorReverted;
            }
            QMMTransactionSavepointInfo savepoint = exec.getSavepoint();
            if (savepoint == null) {
                return null;
            } else if (savepoint.isClosed()) {
                return savepoint.isCommitted() ?
                    QueryLogThemeSettings.instance.colorUncommitted : QueryLogThemeSettings.instance.colorTransaction;
            } else {
                return QueryLogThemeSettings.instance.colorUncommitted;
            }
        } else if (event.getObject() instanceof QMMTransactionInfo || event.getObject() instanceof QMMTransactionSavepointInfo) {
            QMMTransactionSavepointInfo savepoint;
            if (event.getObject() instanceof QMMTransactionInfo) {
                savepoint = ((QMMTransactionInfo) event.getObject()).getCurrentSavepoint();
            } else {
                savepoint = (QMMTransactionSavepointInfo) event.getObject();
            }
            return savepoint.isCommitted() ? null : QueryLogThemeSettings.instance.colorTransaction;
        }
        return null;
    }

    private void reloadEvents(@Nullable String searchString) {
        if (reloadInProgress) {
            log.debug("Event reload is in progress. Skip"); //$NON-NLS-1$
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
    public void metaInfoChanged(@NotNull DBRProgressMonitor monitor, @NotNull final List<QMMetaEvent> events) {
        if (DBWorkbench.getPlatform().isShuttingDown()) {
            return;
        }
        // Run in UI thread
        UIUtils.asyncExec(() -> updateMetaInfo(events));
    }

    private synchronized void updateMetaInfo(final List<? extends QMEvent> events) {
        if (logTable.isDisposed()) {
            return;
        }
        logTable.setRedraw(false);
        try {
            // Add events in reverse order
            int itemIndex = 0;
            for (QMEvent qmEvent : events) {
                if (useDefaultFilter && itemIndex >= entriesPerPage) {
                    // Do not add remaining (older) events - they don't fit page anyway
                    break;
                }
                if ((filter != null && !filter.accept(qmEvent)) || (useDefaultFilter && !defaultFilter.accept(qmEvent))) {
                    // Filter the same second time?
                    continue;
                }
                QMMObject object = qmEvent.getObject();
                if (object instanceof QMMStatementExecuteInfo) {
                    if (CommonUtils.isEmpty(((QMMStatementExecuteInfo) object).getQueryString())) {
                        // Ignore empty statements
                        continue;
                    }
                    itemIndex = createOrUpdateItem(qmEvent, itemIndex);
                } else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo) {
                    itemIndex = createOrUpdateItem(qmEvent, itemIndex);
                    // Update all dependent statements
                    if (object instanceof QMMTransactionInfo) {
                        for (QMMTransactionSavepointInfo savepoint = ((QMMTransactionInfo) object).getCurrentSavepoint(); savepoint != null; savepoint = savepoint.getPrevious()) {
                            updateExecutions(qmEvent, savepoint);
                        }

                    } else {
                        updateExecutions(qmEvent, (QMMTransactionSavepointInfo) object);
                    }
                } else if (object instanceof QMMConnectionInfo) {
                    QMEventAction action = qmEvent.getAction();
                    if (action == QMEventAction.BEGIN || action == QMEventAction.END) {
                        TableItem item = new TableItem(logTable, SWT.NONE, itemIndex++);
                        updateItem(qmEvent, item);
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
            log.error("Error updating Query Log", e); //$NON-NLS-1$
        } finally {
            if (!logTable.isDisposed()) {
                logTable.setRedraw(true);
            }
        }
    }

    private void updateExecutions(QMEvent event, QMMTransactionSavepointInfo savepoint) {
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

    private int createOrUpdateItem(QMEvent event, int itemIndex) {
        TableItem item = objectToItemMap.get(event.getObject().getObjectId());
        if (item == null) {
            item = new TableItem(logTable, SWT.NONE, itemIndex++);
            objectToItemMap.put(event.getObject().getObjectId(), item);
        }
        updateItem(event, item);
        return itemIndex;
    }

    private void updateItem(QMEvent event, TableItem item) {
        if (item.isDisposed()) {
            return;
        }
        item.setData(event);
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            ColumnDescriptor cd = columns.get(i);
            item.setText(i, CommonUtils.getSingleLineString(cd.logColumn.getText(event, true)));
        }
        item.setFont(getObjectFont(event));
        item.setForeground(getObjectForeground(event));
        item.setBackground(getObjectBackground(event));
    }

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(logTable);
        menuMgr.addMenuListener(manager -> {
            IAction editorAction = new Action("Open in SQL console", DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE)) { //$NON-NLS-1$
                @Override
                public void run() {
                    openSelectionInEditor();
                }
            };
            IAction copyAction = new Action(ModelMessages.controls_querylog_action_copy) {
                @Override
                public void run() {
                    copySelectionToClipboard(false);
                }
            };
            copyAction.setEnabled(logTable.getSelectionCount() > 0);
            copyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

            IAction deleteSelectionAction = new Action(ModelMessages.controls_querylog_action_delete) {
                @Override
                public void run() {
                    deleteSelectedItems();
                }
            };
            deleteSelectionAction.setEnabled(logTable.getSelectionCount() > 0);
            deleteSelectionAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);

            IAction copyAllAction = new Action(ModelMessages.controls_querylog_action_copy_all_fields) {
                @Override
                public void run() {
                    copySelectionToClipboard(true);
                }
            };
            copyAllAction.setEnabled(logTable.getSelectionCount() > 0);
            copyAllAction.setActionDefinitionId(IActionConstants.CMD_COPY_SPECIAL);

            IAction selectAllAction = new Action(ModelMessages.controls_querylog_action_select_all) {
                @Override
                public void run() {
                    selectAll();
                }
            };
            selectAllAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);

            IAction clearLogAction = new Action(ModelMessages.controls_querylog_action_clear_log) {
                @Override
                public void run() {
                    clearLog();
                }
            };

            boolean hasStatements = false;
            for (TableItem item : logTable.getSelection()) {
                if (((QMEvent) item.getData()).getObject() instanceof QMMStatementExecuteInfo) {
                    hasStatements = true;
                    break;
                }
            }
            if (hasStatements) {
                manager.add(editorAction);
                manager.add(new Separator());
            }
            manager.add(copyAction);
            manager.add(deleteSelectionAction);
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
        manager.add(new Separator());
        manager.add(ActionUtils.makeCommandContribution(site, "org.jkiss.dbeaver.core.qm.filter"));
    }

    private void openSelectionInEditor() {
        DBPDataSourceContainer dsContainer = null;
        StringBuilder sql = new StringBuilder();
        TableItem[] items = logTable.getSelection();
        for (TableItem item : items) {
            QMEvent event = (QMEvent) item.getData();
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo stmtExec) {
                if (dsContainer == null) {
                    dsContainer = getDataSourceContainer(stmtExec);
                }
                String queryString = stmtExec.getQueryString();
                if (!CommonUtils.isEmptyTrimmed(queryString)) {
                    if (!sql.isEmpty()) {
                        sql.append("\n"); //$NON-NLS-1$
                    }
                    queryString = queryString.trim();
                    sql.append(queryString);
                    if (!queryString.endsWith(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
                        sql.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER).append("\n"); //$NON-NLS-1$
                    }
                }
            }
        }
        if (!sql.isEmpty()) {
            SQLEditorHandlerOpenEditor.openSQLConsole(
                UIUtils.getActiveWorkbenchWindow(),
                new SQLNavigatorContext(dsContainer),
                "QueryManager", //$NON-NLS-1$
                sql.toString()
            );
        }
    }

    private void addDragAndDropSupport() {
        Transfer[] types = new Transfer[]{TextTransfer.getInstance()};
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

        dndSource = new DragSource(logTable, operations);
        dndSource.setTransfer(types);
        dndSource.addDragListener(new DragSourceListener() {

            @Override
            public void dragStart(DragSourceEvent event) {
            }

            @Override
            public void dragSetData(DragSourceEvent event) {
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

    public synchronized void deleteSelectedItems() {
        for (TableItem tableItem : logTable.getSelection()) {
            objectToItemMap.remove(((QMEvent) tableItem.getData()).getObject().getObjectId());
        }
        int[] selectionIndices = logTable.getSelectionIndices();
        logTable.remove(selectionIndices);
    }

    public synchronized void clearLog() {
        logTable.removeAll();
        objectToItemMap.clear();
    }

    public void selectAll() {
        if (!logTable.isDisposed()) {
            logTable.selectAll();
        }
    }

    public void copySelectionToClipboard(boolean extraInfo) {
        String tdt = getSelectedText(extraInfo);
        if (CommonUtils.isEmpty(tdt)) {
            return;
        }

        if (!tdt.isEmpty()) {
            UIUtils.setClipboardContents(logTable.getDisplay(), TextTransfer.getInstance(), tdt);
        }
    }

    private String getSelectedText(boolean extraInfo) {
        IStructuredSelection selection = getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        StringBuilder tdt = new StringBuilder();
        for (Object o : selection) {
            QMEvent item = (QMEvent) o;
            if (!tdt.isEmpty()) {
                tdt.append(GeneralUtils.getDefaultLineSeparator());
            }
            if (extraInfo) {
                for (int i1 = 0, columnsSize = columns.size(); i1 < columnsSize; i1++) {
                    ColumnDescriptor cd = columns.get(i1);
                    String text = cd.logColumn.getText(item, true);
                    if (i1 > 0) {
                        tdt.append('\t');
                    }
                    tdt.append(text);
                }
            } else {
                String text = COLUMN_TEXT.getText(item, true);
                tdt.append(text);
            }
        }
        return tdt.toString();
    }

    private LogRefreshJob logRefreshJob = null;

    @Override
    public synchronized void preferenceChange(PreferenceChangeEvent event) {
        if (event.getProperty().startsWith(QMConstants.PROP_PREFIX)) {
            scheduleLogRefresh();
        }
    }

    private class LogRefreshJob extends AbstractUIJob {
        LogRefreshJob() {
            super(ModelMessages.controls_querylog_job_refresh);
        }

        @Override
        protected IStatus runInUIThread(DBRProgressMonitor monitor) {
            refresh();
            return Status.OK_STATUS;
        }
    }

    private DBPDataSourceContainer getDataSourceContainer(QMMStatementExecuteInfo stmtExec) {
        QMMConnectionInfo session = stmtExec.getStatement().getConnection();
        String projectName = session.getProjectInfo() == null ? null : session.getProjectInfo().getName();
        if (CommonUtils.isEmpty(projectName)) {
            projectName = session.getProjectInfo() == null ? null : session.getProjectInfo().getId();
        }
        String containerId = session.getContainerId();
        return DBUtils.findDataSource(projectName, containerId);
    }

    private class EventViewDialog extends BaseSQLDialog {

        private static final String DIALOG_ID = "DBeaver.QM.EventViewDialog";//$NON-NLS-1$

        private final QMEvent object;

        EventViewDialog(QMEvent object) {
            super(QueryLogViewer.this.getControl().getShell(), QueryLogViewer.this.site, "Event", null); //$NON-NLS-1$
            setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.MAX | SWT.RESIZE);
            this.object = object;
        }

        @Override
        protected IDialogSettings getDialogBoundsSettings() {
            return UIUtils.getDialogSettings(DIALOG_ID);
        }

        @Override
        protected boolean isWordWrap() {
            return true;
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText(ModelMessages.controls_querylog_shell_text + COLUMN_TYPE.getText(object, true));
        }

        @Override
        protected Composite createDialogArea(Composite parent) {

            final Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(1, false));
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            final Composite topFrame = UIUtils.createComposite(composite, 2);
            topFrame.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createLabelText(topFrame, ModelMessages.controls_querylog_label_time, COLUMN_TIME.getText(object, true), SWT.READ_ONLY);
            UIUtils.createLabelText(topFrame, ModelMessages.controls_querylog_label_type, COLUMN_TYPE.getText(object, true), SWT.BORDER | SWT.READ_ONLY);

            final Label messageLabel = UIUtils.createControlLabel(topFrame, ModelMessages.controls_querylog_label_text);
            messageLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            Control msg;
            if (object.getObject() instanceof QMMStatementExecuteInfo qmmStatementExecuteInfo) {
                msg = createSQLPanel(topFrame);
                Composite sqlDetailsPanel = UIUtils.createComposite(topFrame, 4);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 2;
                sqlDetailsPanel.setLayoutData(gd);

                String catalogTerm = null;
                String schemaTerm = null;
                DBPDataSourceContainer ds = getDataSourceContainer(qmmStatementExecuteInfo);
                if (ds != null) {
                    DBPDataSource dataSource = ds.getDataSource();
                    if (dataSource != null) {
                        catalogTerm = dataSource.getInfo().getCatalogTerm();
                        schemaTerm = dataSource.getInfo().getSchemaTerm();
                    }
                }
                if (catalogTerm == null) {
                    catalogTerm = ModelMessages.controls_querylog_column_catalog_name;
                }
                if (schemaTerm == null) {
                    schemaTerm = ModelMessages.controls_querylog_column_schema_name;
                }
                UIUtils.createLabelText(sqlDetailsPanel, catalogTerm,
                    qmmStatementExecuteInfo.getCatalog(), SWT.BORDER | SWT.READ_ONLY, new GridData(GridData.FILL_HORIZONTAL));
                UIUtils.createLabelText(sqlDetailsPanel, schemaTerm,
                    qmmStatementExecuteInfo.getSchema(), SWT.BORDER | SWT.READ_ONLY, new GridData(GridData.FILL_HORIZONTAL));
            } else {
                final Text messageText = new Text(topFrame, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
                messageText.setText(COLUMN_TEXT.getText(object, true));
                msg = messageText;
            }
            GridData gd = new GridData(GridData.FILL_BOTH);
            //gd.heightHint = 40;
            gd.widthHint = 500;
            msg.setLayoutData(gd);

            final Label resultLabel = UIUtils.createControlLabel(topFrame, ModelMessages.controls_querylog_label_result);
            resultLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

            String resultMessage = COLUMN_RESULT.getText(object, true);
            boolean isMultilineResult = resultMessage.contains("\n");
            final Text resultText = new Text(
                topFrame,
                SWT.BORDER | SWT.READ_ONLY | (isMultilineResult ?
                        SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL : SWT.NONE));
            resultText.setText(resultMessage);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            if (isMultilineResult) {
                gd.heightHint = 60;
                gd.widthHint = 300;
            }
            resultText.setLayoutData(gd);

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(@NotNull Composite parent, int alignment) {
            if (alignment == SWT.LEAD) {
                createCopyButton(parent);
                createExecuteButton(parent);
            } else {
                createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            }
        }

        @Override
        protected void buttonPressed(int buttonId) {
            if (buttonId == IDialogConstants.PROCEED_ID && object.getObject() instanceof QMMStatementExecuteInfo info) {
                final SQLEditor editor = ((SQLLogFilter) filter).getEditor();
                final SQLQuery query = new SQLQuery(editor.getDataSource(), info.getQueryString());
                editor.processQueries(List.of(query), false, true, false, true, null, null);
            } else {
                super.buttonPressed(buttonId);
            }
        }

        @Override
        protected SQLDialect getSQLDialect() {
            if (object.getObject() instanceof QMMStatementExecuteInfo executeInfo) {
                var container = getDataSourceContainer(executeInfo);
                var sqlDialect = getSqlDialectFromContainer(container);
                if (getSqlDialectFromContainer(container) != null) {
                    return sqlDialect;
                }
            }
            return super.getSQLDialect();
        }

        @Nullable
        private SQLDialect getSqlDialectFromContainer(DBPDataSourceContainer container) {
            if (container == null) {
                return null;
            }
            var dataSource = container.getDataSource();
            if (dataSource != null) {
                return container.getDataSource().getSQLDialect();
            }
            DBPDriver driver = DataSourceProviderRegistry.getInstance().findDriver(container.getDriver().getId());
            if (driver == null) {
                return null;
            }
            try {
                return driver.getScriptDialect().createInstance();
            } catch (DBException e) {
                return null;
            }
        }

        @Override
        protected DBCExecutionContext getExecutionContext() {
            return null;
        }

        @Override
        protected String getSQLText() {
            return COLUMN_TEXT.getText(object, false);
        }

        @Override
        protected boolean isLabelVisible() {
            return false;
        }
        
        private boolean isQueryLinkedWithEditor(QMMStatementExecuteInfo execInfo) {
            DBCExecutionPurpose purpose = execInfo.getStatement().getPurpose();
            return (purpose == DBCExecutionPurpose.USER_SCRIPT || purpose == DBCExecutionPurpose.USER)
                && object.getObject() instanceof QMMStatementExecuteInfo && filter != null && filter instanceof SQLLogFilter
                && ((SQLLogFilter) filter).getEditor() != null;
        }

        private void createExecuteButton(@NotNull Composite parent) {
            if (object.getObject() instanceof QMMStatementExecuteInfo info) {
                if (isQueryLinkedWithEditor(info) && SQLEditorUtils.isOpenSeparateConnection(getDataSourceContainer(info))) {
                    createButton(parent, IDialogConstants.PROCEED_ID, SQLEditorMessages.editor_query_log_viewer_reexecute_query_button_text, false);
                }
            }
        }
    }

    class EventHistoryReadService extends AbstractLoadService<List<QMEvent>> {

        private static final int RETRIES_QM_WAITING = 60;
        private static final int WAITING_QM_SESSION_SECONDS_PER_TRY = 1;
        @Nullable
        private final String searchString;

        protected EventHistoryReadService(@Nullable String searchString) {
            super("Load query history"); //$NON-NLS-1$
            this.searchString = searchString;
        }

        @Override
        public List<QMEvent> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            final List<QMEvent> events = new ArrayList<>();
            QMEventBrowser eventBrowser = QMUtils.getEventBrowser(currentSessionOnly);
            if (eventBrowser != null) {
                QMEventCriteria criteria = QMUtils.createDefaultCriteria(DBWorkbench.getPlatform().getPreferenceStore());
                criteria.setSearchString(CommonUtils.isEmptyTrimmed(searchString) ? null : searchString.trim());
                criteria.setFetchingSize(entriesPerPage);

                monitor.beginTask("Load query history", 1); //$NON-NLS-1$
                if (!CommonUtils.isEmpty(searchString)) {
                    monitor.subTask("Search queries: " + searchString); //$NON-NLS-1$
                } else {
                    monitor.subTask("Load all queries"); //$NON-NLS-1$
                }

                String qmSessionId = null;
                if (DBWorkbench.getPlatform().getApplication() instanceof QMSessionProvider provider) {
                    int tries = 0;
                    qmSessionId = provider.getQmSessionId();
                    while (qmSessionId == null && tries < RETRIES_QM_WAITING) {
                        if (DBWorkbench.getPlatform().isShuttingDown()) {
                            break;
                        }
                        RuntimeUtils.pause(WAITING_QM_SESSION_SECONDS_PER_TRY * 1000);
                        qmSessionId = provider.getQmSessionId();
                        tries++;
                    }
                }
                var cursorFilter = new QMCursorFilter(
                    qmSessionId,
                    criteria,
                    filter != null ? filter : (useDefaultFilter ? defaultFilter : null)
                );
                try (QMEventCursor cursor = eventBrowser.getQueryHistoryCursor(cursorFilter)) {
                    while (events.size() < entriesPerPage && cursor.hasNextEvent(monitor)) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        events.add(cursor.nextEvent(monitor));
                        //monitor.subTask(events.get(events.size() - 1).toString());
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

    private class EvenHistoryReadVisualizer extends ProgressLoaderVisualizer<List<QMEvent>> {
        EvenHistoryReadVisualizer(EventHistoryReadService loadingService) {
            super(loadingService, logTable);
        }

        @Override
        public void visualizeLoading() {
            reloadInProgress = true;
            super.visualizeLoading();
        }

        @Override
        public void completeLoading(List<QMEvent> result) {
            try {
                super.completeLoading(result);
                super.visualizeLoading();
                if (logTable.isDisposed()) {
                    return;
                }
                if (result != null) {
                    updateMetaInfo(result);
                }
                // Apply sort (if any)
                TableColumn sortColumn = logTable.getSortColumn();
                if (sortColumn != null) {
                    Listener[] sortListeners = sortColumn.getListeners(SWT.Selection);
                    if (sortListeners != null) {
                        for (Listener listener : sortListeners) {
                            Event event = new Event();
                            event.widget = sortColumn;
                            event.doit = false; // Disable sort toggle
                            listener.handleEvent(event);
                        }
                    }
                }

            } finally {
                reloadInProgress = false;
            }
        }
    }

}