/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

/**
 * Execution plan builder
 */
public interface DBCExecutionPlanBuilder {

    DBCPlan prepareExecutionPlan(String query)
        throws DBCException;

}
