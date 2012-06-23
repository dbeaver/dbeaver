/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * Query analyser
 */
public interface DBCQueryAnalyser {

    DBCPlan analyseQueryExecution(DBCExecutionContext context, String query)
        throws DBCException;

}