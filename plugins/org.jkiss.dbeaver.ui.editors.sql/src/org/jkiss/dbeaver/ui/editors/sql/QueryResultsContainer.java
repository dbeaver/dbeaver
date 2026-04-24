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

import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.transformers.SQLQueryTransformerCount;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ISmartTransactionManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.editors.sql.execute.SQLQueryJob;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class QueryResultsContainer implements
    DBSDataContainer,
    IResultSetContainer,
    IResultSetValueReflector,
    IResultSetListener,
    IResultSetContainerExt,
    SQLQueryContainer,
    ISmartTransactionManager,
    IQueryExecuteController,
    SQLEditor.QueryProcessingComponent {

    protected final QueryProcessor queryProcessor;
    protected final ResultSetViewer viewer;
    protected int resultSetNumber;
    protected final int resultSetIndex;
    protected final boolean singleQuery;
    private SQLScriptElement query = null;
    private SQLScriptElement lastGoodQuery = null;
    // Data container and filter are non-null only in case of associations navigation
    private DBSDataContainer dataContainer;
    private String tabName;
    protected boolean detached;

    protected QueryResultsContainer(
        @NotNull Composite resultSetViewerContainer,
        @NotNull QueryProcessor queryProcessor,
        int resultSetNumber,
        int resultSetIndex,
        boolean singleQuery,
        boolean makeDefault
    ) {
        this.queryProcessor = queryProcessor;
        this.resultSetNumber = resultSetNumber;
        this.resultSetIndex = resultSetIndex;
        this.singleQuery = singleQuery;

        SQLEditor owner = queryProcessor.getOwner();
        this.viewer = new ResultSetViewer(resultSetViewerContainer, owner.getSite(), this);
        this.viewer.addListener(this);

        viewer.getControl().addDisposeListener(e -> {
            QueryResultsContainer.this.queryProcessor.removeResults(QueryResultsContainer.this);
        });
    }

    protected abstract void dispose();

    QueryResultsContainer(
        @NotNull Composite resultSetViewerContainer,
        @NotNull QueryProcessor queryProcessor,
        int resultSetNumber,
        int resultSetIndex,
        @NotNull DBSDataContainer dataContainer,
        boolean singleQuery
    ) {
        this(resultSetViewerContainer, queryProcessor, resultSetNumber, resultSetIndex, singleQuery, false);
        this.dataContainer = dataContainer;
        updateResultsName(queryProcessor.getOwner().getResultsTabName(
            resultSetNumber, 0, dataContainer.getName()), null);
    }

    SQLEditor getOwner() {
        return queryProcessor.getOwner();
    }

    QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    void setLastGoodQuery(SQLScriptElement lastGoodQuery) {
        this.lastGoodQuery = lastGoodQuery;
    }

    public void detach() {
        try {
            detached = true;
            this.getOwner().getSite().getPage().openEditor(
                new SQLResultsEditorInput(this),
                SQLResultsEditor.class.getName(),
                true,
                IWorkbenchPage.MATCH_NONE
            );
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError("Detached results", "Can't open results view", e);
            detached = false;
        }
    }

    public String getTabName() {
        return tabName;
    }

    public int getResultSetIndex() {
        return resultSetIndex;
    }

    public int getQueryIndex() {
        return getOwner().queryProcessors.indexOf(queryProcessor);
    }

    abstract void updateResultsName(String resultSetName, String toolTip);

    @Nullable
    @Override
    public DBPProject getProject() {
        return getOwner().getProject();
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return getOwner().getExecutionContext();
    }

    @Nullable
    @Override
    public ResultSetViewer getResultSetController() {
        return viewer;
    }

    boolean hasData() {
        return viewer != null && viewer.hasData();
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer() {
        return this;
    }

    @Override
    public boolean isReadyToRun() {
        return queryProcessor.getCurJob() == null || queryProcessor.getRunningJobs() <= 0;
    }

    @Override
    public void openNewContainer(
        DBRProgressMonitor monitor,
        @NotNull DBSDataContainer dataContainer,
        @NotNull DBDDataFilter newFilter
    ) {
        UIUtils.syncExec(() -> {
            QueryResultsContainer resultsProvider = queryProcessor.createResultsProvider(dataContainer);
            CTabItem tabItem = resultsProvider.getResultsTab();
            if (tabItem != null) {
                tabItem.getParent().setSelection(tabItem);
            }
            getOwner().setActiveResultsContainer(resultsProvider);
            resultsProvider.viewer.refreshWithFilter(newFilter);
        });
    }

    @Override
    public IResultSetDecorator createResultSetDecorator() {
        return getOwner().createQueryResultsDecorator(singleQuery);
    }

    @NotNull
    @Override
    public String[] getSupportedFeatures() {
        if (dataContainer != null) {
            return dataContainer.getSupportedFeatures();
        }
        List<String> features = new ArrayList<>(3);
        features.add(FEATURE_DATA_SELECT);
        if (query instanceof SQLQuery sqlQuery && sqlQuery.isModifying()) {
            features.add(FEATURE_DATA_MODIFIED_ON_REFRESH);
        }
        features.add(FEATURE_DATA_COUNT);

        if (getQueryResultCounts() <= 1 && lastGoodQuery instanceof SQLQuery) {
            features.add(FEATURE_DATA_FILTER);
        }
        return features.toArray(new String[0]);
    }

    @NotNull
    @Override
    public DBCStatistics readData(
        @Nullable DBCExecutionSource source,
        @NotNull DBCSession session,
        @NotNull DBDDataReceiver dataReceiver,
        DBDDataFilter dataFilter,
        long firstRow,
        long maxRows,
        long flags,
        int fetchSize
    ) throws DBException {
        if (dataContainer != null) {
            return dataContainer.readData(source, session, dataReceiver, dataFilter, firstRow, maxRows, flags, fetchSize);
        }
        final SQLQueryJob job = queryProcessor.getCurJob();
        if (job == null) {
            throw new DBCException("No active query - can't read data");
        }
        if (this.query instanceof SQLQuery sqlQuery) {
            if (sqlQuery.getResultsMaxRows() >= 0) {
                firstRow = sqlQuery.getResultsOffset();
                maxRows = sqlQuery.getResultsMaxRows();
            }
        }
        try {
            if (dataReceiver != viewer.getDataReceiver()) {
                // Some custom receiver. Probably data export
                queryProcessor.setCurDataReceiver(dataReceiver);
            } else {
                queryProcessor.setCurDataReceiver(null);
            }
            // Count number of results for this query. If > 1 then we will refresh them all at once
            int resultCounts = getQueryResultCounts();

            if (resultCounts <= 1 && resultSetNumber > 0) {
                job.setFetchResultSetNumber(resultSetNumber);
            } else {
                job.setFetchResultSetNumber(-1);
            }
            job.setResultSetLimit(firstRow, maxRows);
            job.setDataFilter(dataFilter);
            job.setFetchSize(fetchSize);
            job.setFetchFlags(flags);

            try {
                job.extractData(session, this.query, resultCounts > 1 ? 0 : resultSetNumber, !detached, !detached);
            } finally {
                lastGoodQuery = job.getLastGoodQuery();
            }

            return job.getStatistics();
        } finally {
            // Nullify custom data receiver
            queryProcessor.setCurDataReceiver(null);
        }
    }

    private int getQueryResultCounts() {
        int resultCounts = 0;
        for (QueryResultsContainer qrc : queryProcessor.resultContainers) {
            if (qrc.query == query) {
                resultCounts++;
            }
        }
        return resultCounts;
    }

    @Override
    public long countData(
        @NotNull DBCExecutionSource source,
        @NotNull DBCSession session,
        @Nullable DBDDataFilter dataFilter,
        long flags
    ) throws DBException {
        if (dataContainer != null) {
            return dataContainer.countData(source, session, dataFilter, DBSDataContainer.FLAG_NONE);
        }
        DBPDataSource dataSource = getDataSource();
        if (dataSource == null) {
            throw new DBCException("Query transform is not supported by datasource");
        }
        if (!(query instanceof SQLQuery sqlQuery)) {
            throw new DBCException("Can't count rows for control command");
        }
        try {
            SQLQuery countQuery = new SQLQueryTransformerCount().transformQuery(dataSource, getOwner().getSyntaxManager(), sqlQuery);
            if (!CommonUtils.isEmpty(countQuery.getParameters())) {
                countQuery.setParameters(getOwner().parseQueryParameters(countQuery));
            }
            return DBUtils.countDataFromQuery(source, session, countQuery);
        } catch (DBException e) {
            throw new DBCException("Error executing row count", e);
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        if (dataContainer != null) {
            return dataContainer.getDescription();
        } else {
            return SQLEditorMessages.editors_sql_description;
        }
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return getDataSource();
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return getOwner().getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return dataContainer == null || dataContainer.isPersisted();
    }

    @NotNull
    @Override
    public String getName() {
        if (dataContainer != null) {
            return dataContainer.getName();
        }
        String name = lastGoodQuery != null ?
            lastGoodQuery.getOriginalText() :
            (query == null ? null : query.getOriginalText());
        if (name == null) {
            name = "SQL";
        }
        return name;
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return getOwner().getDataSourceContainer();
    }

    @Override
    public String toString() {
        if (dataContainer != null) {
            return dataContainer.toString();
        }
        return query == null ?
            "SQL Query / " + getOwner().getEditorInput().getName() :
            query.getOriginalText();
    }

    @Override
    public void handleResultSetLoad() {

    }

    @Override
    public void handleResultSetChange() {
        getOwner().updateDirtyFlag();
    }

    @Override
    public void handleResultSetSelectionChange(SelectionChangedEvent event) {

    }

    @Override
    public void onModelPrepared() {
        getOwner().notifyOnDataListeners(this);
    }

    @Override
    public SQLScriptContext getScriptContext() {
        return getOwner().getGlobalScriptContext();
    }

    @Override
    public SQLScriptElement getQuery() {
        return query;
    }

    void setQuery(SQLScriptElement query) {
        this.query = query;
    }

    @Override
    public Map<String, Object> getQueryParameters() {
        return getOwner().getGlobalScriptContext().getAllParameters();
    }

    @Override
    public boolean isSmartAutoCommit() {
        return getOwner().isSmartAutoCommit();
    }

    @Override
    public void setSmartAutoCommit(boolean smartAutoCommit) {
        getOwner().setSmartAutoCommit(smartAutoCommit);
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    @Override
    public void insertCurrentCellValue(DBDAttributeBinding attributeBinding, Object cellValue, String stringValue) {
        StyledText textWidget = getOwner().getTextViewer() == null ? null : getOwner().getTextViewer().getTextWidget();
        if (textWidget != null) {
            String sqlValue;
            if (getDataSource() != null) {
                sqlValue = SQLUtils.convertValueToSQL(getDataSource(), attributeBinding, cellValue);
            } else {
                sqlValue = stringValue;
            }
            textWidget.insert(sqlValue);
            textWidget.setCaretOffset(textWidget.getCaretOffset() + sqlValue.length());
            textWidget.setFocus();
        }
    }

    @Override
    public void forceDataReadCancel(Throwable error) {
        for (QueryProcessor processor : getOwner().queryProcessors) {
            SQLQueryJob job = processor.getCurJob();
            if (job != null) {
                SQLQueryResult currentQueryResult = job.getCurrentQueryResult();
                if (currentQueryResult == null) {
                    currentQueryResult = new SQLQueryResult(new SQLQuery(null, ""));
                }
                currentQueryResult.setError(error);
                job.notifyQueryExecutionEnd(null, currentQueryResult);
            }
        }
    }

    @Override
    public void handleExecuteResult(DBCExecutionResult result) {
        // dump server output only once on one query execution
        // even if it return multiple query results (resultSetNumber > 0)
        if (this.resultSetNumber == 0) {
            getOwner().dumpQueryServerOutput(result);
        }
    }

    @Override
    public void showCurrentError() {
        SQLEditor owner = getOwner();
        if (owner.getLastQueryErrorPosition() > -1) {
            owner.getSelectionProvider().setSelection(new TextSelection(owner.getLastQueryErrorPosition(), 0));
            owner.setFocus();
        }
    }

    public abstract CTabItem getResultsTab();

    public abstract boolean isPinned();

    public abstract void setPinned(boolean pinned);

    protected boolean isTabPinned(CTabItem tabItem) {
        return tabItem != null && !tabItem.isDisposed() && !tabItem.getShowClose();
    }

    protected void setTabPinned(@Nullable CTabItem tabItem, boolean pinned) {
        if (tabItem != null) {
            tabItem.setShowClose(!pinned);
            tabItem.setImage(pinned ? SQLEditor.IMG_DATA_GRID_LOCKED : SQLEditor.IMG_DATA_GRID);
        }
    }

    boolean isStatistics() {
        return query != null && query.getData() == SQLQueryJob.STATS_RESULTS;
    }
}
