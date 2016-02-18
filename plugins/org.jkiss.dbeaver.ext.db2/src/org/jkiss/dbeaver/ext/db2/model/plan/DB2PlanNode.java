/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

import java.util.ArrayList;
import java.util.Collection;

/**
 * DB2 Plan Node
 * 
 * @author Denis Forveille
 */
public abstract class DB2PlanNode implements DBCPlanNode {

    private DB2PlanNode parent;
    private Collection<DB2PlanNode> listNestedNodes = new ArrayList<>(64);

    protected DB2PlanNode() {
    }

    protected DB2PlanNode(DB2PlanNode node) {
        this.parent = node.parent;
        this.listNestedNodes.addAll(node.listNestedNodes);
    }

    // --------------------
    // DB2PlanNode Contract
    // --------------------
    public abstract String getNodeName();

    public abstract Double getEstimatedCardinality();

    public void setEstimatedCardinality(Double estimatedCardinality)
    {
        // Not supported by every kind of DB2PlanNode
    }

    public String getDetails()
    {
        return "";
    }

    public void setParent(DB2PlanNode parent)
    {
        this.parent = parent;
    }

    // ----------------------
    // Methods from Interface
    // ---------------------
    @Override
    public DB2PlanNode getParent()
    {
        return parent;
    }

    @Override
    public Collection<DB2PlanNode> getNested()
    {
        return listNestedNodes;
    }

}
