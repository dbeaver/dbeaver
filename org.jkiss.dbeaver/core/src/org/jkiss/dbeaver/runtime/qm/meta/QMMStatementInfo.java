/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCStatement;

import java.lang.ref.SoftReference;

/**
 * DBCStatement meta info
 */
public class QMMStatementInfo {

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
        this.openTime = System.currentTimeMillis();
    }

    void close()
    {
        this.closeTime = System.currentTimeMillis();
        this.reference.clear();
        this.reference = null;
    }

    QMMStatementExecuteInfo beginExecution(QMMSavePointInfo savepoint, String queryString)
    {
        return this.execution = new QMMStatementExecuteInfo(this, savepoint, queryString, this.execution);
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
