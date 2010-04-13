/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.jkiss.dbeaver.runtime.sql.SQLStatementInfo;

/**
 * SQLQueryListener
 */
public interface SQLQueryListener {

    void onStartJob();

    void onStartQuery(SQLStatementInfo query);

    void onEndQuery(SQLQueryResult result);

    void onEndJob(boolean hasErrors);
}
