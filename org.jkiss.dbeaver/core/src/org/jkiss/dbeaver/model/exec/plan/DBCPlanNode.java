/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import java.util.Collection;

/**
 * Execution plan node
 */
public interface DBCPlanNode {

    String getType();

    DBCPlanNode getParent();

    Collection<DBCPlanNode> getNested();

}