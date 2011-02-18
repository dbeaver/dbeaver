/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import java.util.Collection;

/**
 * Execution plan node
 */
public interface DBCPlanNode {

    DBCPlanNode getParent();

    Collection<? extends DBCPlanNode> getNested();

}