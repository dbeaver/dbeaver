/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

/**
 * SQLQueryListener
 */
public interface ISQLQueryListener {

    void onStartJob();

    void onStartQuery(SQLStatementInfo query);

    void onEndQuery(SQLQueryResult result);

    void onEndJob(boolean hasErrors);
}
