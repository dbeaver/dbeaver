/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.ISmartTransactionManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.editors.sql.execute.SQLQueryJob;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

abstract class QueryProcessor implements SQLResultsConsumer, ISmartTransactionManager, SQLEditor.QueryProcessingComponent {
    static final int QUERIES_COUNT_FOR_NO_FETCH_RESULT_SET_CONFIRMATION = 100;


    @NotNull
    private final SQLEditor owner;
    private volatile SQLQueryJob curJob;
    private final boolean singleQuery;
    private final AtomicInteger curJobRunning = new AtomicInteger(0);
    protected final List<QueryResultsContainer> resultContainers = new ArrayList<>();
    private DBDDataReceiver curDataReceiver = null;

    QueryProcessor(@NotNull SQLEditor owner, boolean singleQuery, boolean makeDefault) {
        this.owner = owner;
        this.singleQuery = singleQuery;
        // Create first (default) results provider
        if (makeDefault) {
            owner.queryProcessors.addFirst(this);
        } else {
            owner.queryProcessors.add(this);
        }
        createResultsProvider(0, makeDefault);
    }

    @NotNull
    public SQLEditor getOwner() {
        return owner;
    }

    public SQLQueryJob getCurJob() {
        return curJob;
    }

    int getRunningJobs() {
        return curJobRunning.get();
    }

    public AtomicInteger getCurJobRunning() {
        return curJobRunning;
    }

    public void setCurDataReceiver(DBDDataReceiver curDataReceiver) {
        this.curDataReceiver = curDataReceiver;
    }

    @NotNull
    private QueryResultsContainer createResultsProvider(int resultSetNumber, boolean makeDefault) {
        QueryResultsContainer resultsProvider = createQueryResultsContainer(
            resultSetNumber,
            owner.getMaxResultsTabIndex() + 1,
            singleQuery,
            makeDefault
        );
        resultContainers.add(resultsProvider);
        return resultsProvider;
    }

    @NotNull
    QueryResultsContainer createResultsProvider(@NotNull DBSDataContainer dataContainer) {
        QueryResultsContainer resultsProvider = createQueryResultsContainer(
            resultContainers.size(),
            owner.getMaxResultsTabIndex(),
            dataContainer,
            singleQuery
        );
        resultContainers.add(resultsProvider);
        return resultsProvider;
    }

    @NotNull
    protected abstract QueryResultsContainer createQueryResultsContainer(
        int resultSetNumber,
        int resultSetIndex,
        boolean singleQuery,
        boolean makeDefault
    );

    @NotNull
    protected abstract QueryResultsContainer createQueryResultsContainer(
        int resultSetNumber,
        int resultSetIndex,
        @NotNull DBSDataContainer dataContainer,
        boolean singleQuery
    );

    public boolean hasPinnedTabs() {
        for (QueryResultsContainer container : resultContainers) {
            if (container.isPinned()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    QueryResultsContainer getFirstResults() {
        return resultContainers.getFirst();
    }

    @NotNull
    List<QueryResultsContainer> getResultContainers() {
        return resultContainers;
    }

    void closeJob() {
        final SQLQueryJob job = curJob;
        if (job != null) {
            if (job.getState() == Job.RUNNING) {
                job.cancel();
            }
            curJob = null;
            if (job.isJobOpen()) {
                RuntimeUtils.runTask(monitor -> job.closeJob(), "Close SQL job", 2000, true);
            }
        }
    }

    public void cancelJob() {
        for (QueryResultsContainer rc : resultContainers) {
            rc.viewer.cancelJobs();
        }
        final SQLQueryJob job = curJob;
        if (job != null) {
            if (job.getState() == Job.RUNNING) {
                job.cancel();
            }
        }
    }

    boolean processQueries(
        SQLScriptContext scriptContext,
        final List<SQLScriptElement> queries,
        boolean forceScript,
        boolean fetchResults,
        boolean export,
        boolean closeTabOnError,
        SQLQueryListener queryListener
    ) {
        if (queries.isEmpty()) {
            // Nothing to process
            return false;
        }
        if (curJobRunning.get() > 0) {
            DBWorkbench.getPlatformUI().showError(
                SQLEditorMessages.editors_sql_error_cant_execute_query_title,
                SQLEditorMessages.editors_sql_error_cant_execute_query_message
            );
            return false;
        }
        final DBCExecutionContext executionContext = owner.getExecutionContext();
        if (executionContext == null) {
            DBWorkbench.getPlatformUI().showError(
                SQLEditorMessages.editors_sql_error_cant_execute_query_title,
                ModelMessages.error_not_connected_to_database
            );
            return false;
        }
        final boolean isSingleQuery = !forceScript && (queries.size() == 1);

        // Prepare execution job
        {
            owner.showScriptPositionRuler(owner.getShowScriptRulerOnExecution());
            QueryResultsContainer resultsContainer = getFirstResults();

            SQLEditor.SQLEditorQueryListener listener = new SQLEditor.SQLEditorQueryListener(this, closeTabOnError);
            if (queryListener != null) {
                listener.setExtListener(queryListener);
            }

            if (export) {
                processDataExport(scriptContext, queries);
            } else {
                boolean disableFetchCurrentResultSets;
                if (queries.size() > QUERIES_COUNT_FOR_NO_FETCH_RESULT_SET_CONFIRMATION) {
                    if (owner.getDisableFetchResultSet() == null) {
                        DBPPlatformUI.UserChoiceResponse rs = DBWorkbench.getPlatformUI().showUserChoice(
                            SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_title,
                            SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_question,
                            List.of(
                                SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_yes,
                                SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_no
                            ),
                            List.of(SQLEditorMessages.sql_editor_confirm_no_fetch_result_for_big_script_remember), 0, 0
                        );
                        disableFetchCurrentResultSets = rs.choiceIndex == 0;
                        if (rs.forAllChoiceIndex != null) {
                            owner.setDisableFetchResultSet(disableFetchCurrentResultSets);
                        }
                    } else {
                        disableFetchCurrentResultSets = owner.getDisableFetchResultSet();
                    }
                } else {
                    disableFetchCurrentResultSets = false;
                }
                final SQLQueryJob job = new SQLQueryJob(
                    owner.getSite(),
                    isSingleQuery ? SQLEditorMessages.editors_sql_job_execute_query
                        : SQLEditorMessages.editors_sql_job_execute_script,
                    executionContext, resultsContainer, queries, scriptContext, this, listener,
                    disableFetchCurrentResultSets
                );

                if (isSingleQuery) {
                    resultsContainer.setQuery(queries.getFirst());

                    closeJob();
                    curJob = job;
                    ResultSetViewer rsv = resultsContainer.getResultSetController();
                    if (rsv != null) {
                        rsv.resetDataFilter(false);
                        rsv.resetHistory();
                        rsv.refresh();
                    }
                } else {
                    if (fetchResults) {
                        job.setFetchResultSets(true);
                    }
                    job.schedule();
                    curJob = job;
                }
            }
        }
        return true;
    }

    private void processDataExport(SQLScriptContext scriptContext, List<SQLScriptElement> queries) {
        List<IDataTransferProducer<?>> producers = new ArrayList<>();
        for (SQLScriptElement element : queries) {
            if (element instanceof SQLControlCommand controlCommand) {
                try {
                    SQLControlResult controlResult = scriptContext.executeControlCommand(
                        new LoggingProgressMonitor(SQLEditorBase.log),
                        controlCommand
                    );
                    if (controlResult.getTransformed() != null) {
                        element = controlResult.getTransformed();
                    }
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Command error", "Error processing control command", e);
                    return;
                }
            }
            if (element instanceof SQLScript) {
                DBWorkbench.getPlatformUI()
                    .showError("Embedded scripts are not allowed", "Query contains script element: " + element.getText());
                return;
            }
            if (element instanceof SQLQuery query) {
                scriptContext.fillQueryParameters(query, () -> null, false);

                SQLQueryDataContainer dataContainer = new SQLQueryDataContainer(getOwner(), query, scriptContext, SQLEditorBase.log);
                producers.add(new DatabaseTransferProducer(dataContainer, null));
            }
        }

        DataTransferWizard.openWizard(
            owner.getSite().getWorkbenchWindow(),
            producers,
            null,
            new StructuredSelection(this)
        );
    }

    public boolean isDirty() {
        for (QueryResultsContainer resultsProvider : resultContainers) {
            ResultSetViewer rsv = resultsProvider.getResultSetController();
            if (rsv != null && rsv.isDirty()) {
                return true;
            }
        }
        return false;
    }

    void removeResults(QueryResultsContainer resultsContainer) {
        resultContainers.remove(resultsContainer);
        owner.removeResults(resultsContainer, resultContainers.isEmpty());
    }

    @Nullable
    @Override
    public DBDDataReceiver getDataReceiver(final SQLQuery statement, final int resultSetNumber) {
        if (curDataReceiver != null) {
            return curDataReceiver;
        }
        final boolean isStatsResult = (statement != null && statement.getData() == SQLQueryJob.STATS_RESULTS);
        //            if (isStatsResult) {
        //                // Maybe it was already open
        //                for (QueryResultsProvider provider : resultContainers) {
        //                    if (provider.query != null && provider.query.getData() == SQLQueryJob.STATS_RESULTS) {
        //                        resultSetNumber = provider.resultSetNumber;
        //                        break;
        //                    }
        //                }
        //            }
        if (resultSetNumber >= resultContainers.size() && !owner.isDisposed()) {
            // Open new results processor in UI thread
            UIUtils.syncExec(() -> createResultsProvider(resultSetNumber, false));
        }
        if (resultSetNumber >= resultContainers.size()) {
            // Editor seems to be disposed - no data receiver
            return null;
        }
        final QueryResultsContainer resultsProvider = resultContainers.get(resultSetNumber);

        if (statement != null && !owner.getResultTabsContainer().isDisposed()) {
            resultsProvider.setQuery(statement);
            resultsProvider.setLastGoodQuery(statement);
            String tabName = null;
            String queryText = CommonUtils.truncateString(statement.getText(), 1000);
            DBPDataSourceContainer dataSourceContainer = owner.getDataSourceContainer();

            String dataSourceContainerName = dataSourceContainer == null ? "N/A" : dataSourceContainer.getName();
            String processedQueryText = CommonUtils.isEmpty(queryText) ? "N/A" : queryText;

            String toolTip =
                NLS.bind(SQLEditorMessages.sql_editor_data_receiver_result_name_tooltip_connection, dataSourceContainerName) +
                    GeneralUtils.getDefaultLineSeparator() +
                    NLS.bind(
                        SQLEditorMessages.sql_editor_data_receiver_result_name_tooltip_time,
                        new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT).format(new Date())
                    ) +
                    GeneralUtils.getDefaultLineSeparator() +
                    NLS.bind(SQLEditorMessages.sql_editor_data_receiver_result_name_tooltip_query, processedQueryText);
            // Special statements (not real statements) have their name in data
            if (isStatsResult) {
                tabName = SQLEditorMessages.editors_sql_statistics;
                int queryIndex = owner.queryProcessors.indexOf(QueryProcessor.this);
                tabName += " " + (queryIndex + 1);
            }
            String finalTabName = tabName;
            UIUtils.asyncExec(() -> resultsProvider.updateResultsName(finalTabName, toolTip));
        }
        ResultSetViewer rsv = resultsProvider.getResultSetController();
        return rsv == null ? null : rsv.getDataReceiver();
    }

    @Override
    public boolean isSmartAutoCommit() {
        return owner.isSmartAutoCommit();
    }

    @Override
    public void setSmartAutoCommit(boolean smartAutoCommit) {
        owner.setSmartAutoCommit(smartAutoCommit);
    }
}
