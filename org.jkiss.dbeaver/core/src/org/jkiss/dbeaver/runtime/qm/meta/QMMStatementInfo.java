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

    QMMStatementInfo(QMMSessionInfo session, DBCStatement reference, QMMStatementScripInfo script, QMMStatementInfo previous)
    {
        this.session = session;
        this.reference = new SoftReference<DBCStatement>(reference);
        this.script = script;
        this.previous = previous;
    }

    protected void close()
    {
        super.close();
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

    @Override
    public String toString()
    {
        return "STATEMENT";
    }
}
