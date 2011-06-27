/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import java.util.Collection;

/**
 * Execution plan
 */
public interface DBCPlan {

    String getQueryString();

    Collection<? extends DBCPlanNode> getPlanNodes();

}
