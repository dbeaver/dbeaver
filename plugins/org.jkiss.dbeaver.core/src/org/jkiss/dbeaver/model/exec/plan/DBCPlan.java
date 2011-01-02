/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

import java.util.Collection;

/**
 * Execution plan
 */
public interface DBCPlan {

    String getQueryString();

    Collection<DBCPlanNode> getPlanNodes();

}
