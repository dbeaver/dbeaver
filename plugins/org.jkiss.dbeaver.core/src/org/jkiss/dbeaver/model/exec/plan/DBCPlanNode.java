/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.plan;

import org.jkiss.dbeaver.model.DBPObject;

import java.util.Collection;

/**
 * Execution plan node
 */
public interface DBCPlanNode extends DBPObject {

    DBCPlanNode getParent();

    Collection<? extends DBCPlanNode> getNested();

}