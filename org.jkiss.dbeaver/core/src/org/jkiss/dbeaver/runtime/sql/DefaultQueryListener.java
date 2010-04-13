/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;

/**
 * DefaultQueryListener
 */
public class DefaultQueryListener implements SQLQueryListener
{
    public void onStartJob()
    {
    }

    public void onStartQuery(SQLStatementInfo query)
    {
    }

    public void onEndQuery(SQLQueryResult result)
    {
    }

    public void onEndJob(boolean hasErrors)
    {
    }
}
