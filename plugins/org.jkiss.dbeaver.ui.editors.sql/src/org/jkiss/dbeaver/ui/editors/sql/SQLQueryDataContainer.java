package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.sql.execute.SQLQueryJob;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

class SQLQueryDataContainer implements DBSDataContainer, IDataSourceContainerProvider, SQLQueryContainer, DBPContextProvider {

    private SQLQueryJob queryJob;
    private SQLQueryResultsConsumer resultsConsumer;
    private SQLQuery query = null;
    private int resultSetNumber;

    private DBDDataReceiver dataReceiver;

    SQLQueryDataContainer(SQLQueryJob queryJob, SQLQueryResultsConsumer resultsConsumer, SQLQuery query, int resultSetNumber)
    {
        this.queryJob = queryJob;
        this.resultsConsumer = resultsConsumer;
        this.query = query;
        this.resultSetNumber = resultSetNumber;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return queryJob.getExecutionContext();
    }

    @Override
    public int getSupportedFeatures() {
        return DATA_SELECT;
    }

    @NotNull
    @Override
    public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize) throws DBCException
    {
        SQLQuery query = (SQLQuery) this.query;
        if (query.getResultsMaxRows() >= 0) {
            firstRow = query.getResultsOffset();
            maxRows = query.getResultsMaxRows();
        }

        resultsConsumer.setDataReceiver(dataReceiver);

        // Count number of results for this query. If > 1 then we will refresh them all at once
        if (resultSetNumber > 0) {
            queryJob.setFetchResultSetNumber(resultSetNumber);
        } else {
            queryJob.setFetchResultSetNumber(-1);
        }
        queryJob.setResultSetLimit(firstRow, maxRows);
        queryJob.setReadFlags(flags);
        queryJob.setDataFilter(dataFilter);

        queryJob.extractData(session, this.query, resultSetNumber);

        return queryJob.getStatistics();
    }

    @Override
    public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter, long flags)
        throws DBCException
    {
        return -1;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return SQLEditorMessages.editors_sql_description;
    }

    @Nullable
    @Override
    public DBSObject getParentObject()
    {
        return getDataSource();
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource()
    {
        return queryJob.getDataSourceContainer().getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return false;
    }

    @NotNull
    @Override
    public String getName()
    {
        String name = query.getOriginalText();
        if (name == null) {
            name = "SQL";
        }
        return name;
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return queryJob.getDataSourceContainer();
    }

    @Override
    public String toString() {
        return query.getOriginalText();
    }

    @Override
    public SQLScriptElement getQuery() {
        return query;
    }

}
