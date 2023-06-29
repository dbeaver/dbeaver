/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model.plan;

import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AltibasePlanNode extends AbstractExecutionPlanNode {

    private int depth;
    private String plan;
    AltibasePlanNode parent;
    private List<AltibasePlanNode> nested;

    public AltibasePlanNode(int depth, String plan, AltibasePlanNode parent) {
        this.depth = depth;
        this.plan = plan;
        this.parent = parent;
        if (this.parent != null) {
            this.parent.addChildNode(this);
        }

        nested = new ArrayList<AltibasePlanNode>();
    }

    public void addChildNode(AltibasePlanNode node) {
        nested.add(node);
    }

    @Override
    public String getNodeName() {
        return plan;
    }

    @Override
    public String getNodeType() {
        return "Plan";
    }

    @Override
    public DBCPlanNode getParent() {
        return parent;
    }

    @Override
    public Collection<? extends DBCPlanNode> getNested() {
        return nested;
    }

    public int getDepth() { 
        return depth; 
    }

    public String toString() {
        return plan;
    }

    public String toString4Debug() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("[depth:%3d] ", depth));

        for (int i = 0; i < depth; i++) {
            sb.append("-");
        }

        sb.append(plan).append(AltibaseConstants.NEW_LINE);
        for (AltibasePlanNode node : nested) {
            sb.append(node.toString4Debug());
        }

        return sb.toString();
    }

    // in case of depth < this.depth
    public AltibasePlanNode getParentNodeAtDepth(int depth) {
        if (this.depth > depth) {
            return this.parent.getParentNodeAtDepth(depth);
        } else if (this.depth == depth) {
            return this.parent;
        } else {
            throw new IllegalArgumentException("Argument depth: " + depth + ", this.depth: " + this.depth);
        }

    }
}
