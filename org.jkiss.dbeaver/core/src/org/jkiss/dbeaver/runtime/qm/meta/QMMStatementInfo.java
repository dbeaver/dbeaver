/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCStatement;

import java.lang.ref.SoftReference;

/**
 * DBCStatement meta info
 */
public class QMMStatementInfo extends QMMObject {

    private final QMMSessionInfo session;
    private SoftReference<DBCStatement> reference;
    private final QMMStatementScripInfo script;
    private final QMMStatementInfo previous;
    private final long openTime;
    private long closeTime;

    private QMMStatementExecuteInfo execution;

    QMMStatementInfo(QMMSessionInfo session, DBCStatement reference, QMMStatementScripInfo script, QMMStatementInfo previous)
    {
        this.session = session;
        this.reference = new SoftReference<DBCStatement>(reference);
        this.script = script;
        this.previous = previous;
        this.openTime = getTimeStamp();
    }

    void close()
    {
        this.closeTime = getTimeStamp();
        this.reference.clear();
        this.reference = null;
    }

    boolean isClosed()
    {
        return closeTime > 0;
    }

    QMMStatementExecuteInfo beginExecution(DBCStatement statement)
    {
        String queryString = statement.getQueryString();
        if (queryString == null) {
            queryString = statement.getDescription();
        }
        return this.execution = new QMMStatementExecuteInfo(
            this,
            session.isTransactional() ? session.getTransaction().getSavepoint() : null,
            queryString,
            this.execution);
    }

    void endExecution(long rowCount, Throwable error)
    {
        execution.endExecution(rowCount, error);
    }

    void beginFetch(DBCResultSet resultSet)
    {
        if (execution == null) {
            beginExecution(resultSet.getSource());
        }
        execution.beginFetch();
    }

    void endFetch(long rowCount)
    {
        execution.endFetch(rowCount);
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public DBCStatement getReference()
    {
        return reference == null ? null : reference.get();
    }

    public QMMStatementScripInfo getScript()
    {
        return script;
    }

    public QMMStatementInfo getPrevious()
    {
        return previous;
    }

    public long getOpenTime()
    {
        return openTime;
    }

    public long getCloseTime()
    {
        return closeTime;
    }

    public QMMStatementExecuteInfo getExecution()
    {
        return execution;
    }

}
