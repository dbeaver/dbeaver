/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;

import java.util.ArrayList;
import java.util.Collection;

/**
 * DB2 Plan Node
 * 
 * @author Denis Forveille
 */
public abstract class DB2PlanNode extends AbstractExecutionPlanNode {

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
    @Override
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
