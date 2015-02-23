/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    private Collection<DB2PlanNode> listNestedNodes = new ArrayList<DB2PlanNode>(64);

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
