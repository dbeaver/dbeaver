/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * Execution plan builder
 */
public interface DBCQueryPlanner {

    DBPDataSource getDataSource();

    DBCPlan planQueryExecution(DBCExecutionContext context, String query)
        throws DBCException;

}
