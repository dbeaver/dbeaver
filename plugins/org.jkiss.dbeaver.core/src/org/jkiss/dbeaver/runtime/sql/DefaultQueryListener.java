/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

/**
 * DefaultQueryListener
 */
public class DefaultQueryListener implements ISQLQueryListener
{
    @Override
    public void onStartJob()
    {
    }

    @Override
    public void onStartQuery(SQLStatementInfo query)
    {
    }

    @Override
    public void onEndQuery(SQLQueryResult result)
    {
    }

    @Override
    public void onEndJob(boolean hasErrors)
    {
    }
}
