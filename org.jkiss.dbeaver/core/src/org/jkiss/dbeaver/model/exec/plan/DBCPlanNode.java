/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import java.util.Collection;
import java.util.List;

/**
 * Execution plan node
 */
public interface DBCPlanNode {

    String getObjectName();

    String getType();

    DBCPlanNode getParent();

    Collection<? extends DBCPlanNode> getNested();

}