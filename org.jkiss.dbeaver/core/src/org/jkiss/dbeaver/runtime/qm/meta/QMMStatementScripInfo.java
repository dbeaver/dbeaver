/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * Query script info
 */
public class QMMStatementScripInfo extends QMMObject {

    private final QMMSessionInfo session;

    QMMStatementScripInfo(QMMSessionInfo session)
    {
        this.session = session;
    }

    protected void close()
    {
        super.close();
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

}
