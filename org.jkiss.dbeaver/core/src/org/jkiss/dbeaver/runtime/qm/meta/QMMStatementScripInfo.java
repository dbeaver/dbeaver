/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * Query script info
 */
public class QMMStatementScripInfo extends QMMObject {

    private final QMMSessionInfo session;
    private final long beginTime;
    private long endTime;

    QMMStatementScripInfo(QMMSessionInfo session)
    {
        this.session = session;
        this.beginTime = getTimeStamp();
    }

    void endScript()
    {
        this.endTime = getTimeStamp();
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public long getBeginTime()
    {
        return beginTime;
    }

    public long getEndTime()
    {
        return endTime;
    }

}
