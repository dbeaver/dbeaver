/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

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

    QMMStatementExecuteInfo beginExecution(String queryString)
    {
        return this.execution = new QMMStatementExecuteInfo(
            this,
            session.getTransaction().getSavepoint(),
            queryString,
            this.execution);
    }

    void endExecution(long rowCount, Throwable error)
    {
        execution.endExecution(rowCount, error);
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public DBCStatement getReference()
    {
        return reference.get();
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
