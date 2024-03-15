/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.IntKeyMap;

import java.util.*;

public class AltibasePlanNode extends AbstractExecutionPlanNode  {

    private final AltibaseDataSource dataSource;

    private static final List<String> allowedKind = new ArrayList<>(
            Arrays.asList("SCAN", 
                    "VIEW", 
                    "PROJECT", 
                    "FILTER", 
                    "SORT", 
                    "HASH", 
                    "AGGREGATION", 
                    "JOIN", 
                    "MERGE", 
                    "UNION", 
                    "CONCATENATION", 
                    "GROUP", 
                    "INSERT", "UPDATE", "DELETE"
                    ));

    private int id;
    private int parentId;

    private int depth;
    private String plan;
    AltibasePlanNode parent;
    private final List<AltibasePlanNode> nested = new ArrayList<AltibasePlanNode>();

    private String operation;
    private String options;

    /**
     * Create plan node from database result.
     */
    public AltibasePlanNode(AltibaseDataSource dataSource, int id, int depth, String plan, AltibasePlanNode parent) {
        this.id = id;
        this.dataSource = dataSource;
        this.depth = depth;
        this.plan = plan;
        this.parent = parent;
        if (this.parent == null) {
            this.parentId = 0;
        } else {
            this.parent.addChildNode(this);
            this.parentId = this.parent.getId();
        }

        // e.g. SCAN ( PARTITION: " )
        setOperation();
    }

    /**
     * Load plan from saved execution plan
     */
    public AltibasePlanNode(AltibaseDataSource dataSource, IntKeyMap<AltibasePlanNode> prevNodes, Map<String, Object> attributes) {
        this.dataSource = dataSource;

        this.id = JSONUtils.getInteger(attributes, "id");
        this.depth = JSONUtils.getInteger(attributes, "depth");
        this.plan = JSONUtils.getString(attributes, "plan");

        setOperation();

        Integer parentIdFromMap =  JSONUtils.getInteger(attributes, "parent_id");

        if (parentIdFromMap != null) {
            parent = prevNodes.get(parentIdFromMap);
        }
        if (parent != null) {
            parent.addChildNode(this);
        }
    }

    /**
     * Set operation type
     * 1. SCAN (PARTITON:...) -> operation: SCAN, options: PARTITION...
     * 2. Otherwise -> set operation only.
     */
    private void setOperation() {
        // e.g. SCAN ( PARTITION: " )
        if (plan.contains("(")) {
            String[] splittedPlan = plan.trim().split("\\(");
            operation = splittedPlan[0].trim();
            options = splittedPlan[1].replace(")", "").trim();
        } else {
            operation = plan.trim();
            options = "";
        }
    }

    /**
     * 
     * Returns its own ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns its parent ID;
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * Add a node as its child.
     */
    public void addChildNode(AltibasePlanNode node) {
        nested.add(node);
    }

    @Override
    public String getNodeName() {
        return operation;
    }

    /**
     * Returns operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Returns options
     */
    public String getOptions() {
        return options;
    }

    @Override
    public DBCPlanNode getParent() {
        return parent;
    }

    @Override
    public Collection<? extends DBCPlanNode> getNested() {
        return nested;
    }

    /**
     * Returns depth
     */
    public int getDepth() { 
        return depth; 
    }

    /**
     * If PREF_PLAN_PREFIX option has checked, then returns prefix + string. 
     * Otherwise, returns plan string only.
     */
    @Property(order = 1, viewable = true)
    public String getPlanString() {
        if (dataSource.getContainer().getPreferenceStore().getBoolean(AltibaseConstants.PREF_PLAN_PREFIX)) {
            return getPrefix(this.depth) + plan;
        } else {
            return plan;
        }
    }

    /**
     * Returns plan as string
     */
    public String getPlan() {
        return plan;
    }

    @Override
    public String toString() {
        return getPlanString();
    }

    /**
     * Returns prefix starts with "└" and append "-" as many as its depth except root node.
     */
    private static String getPrefix(int depth) {
        StringBuilder sb = new StringBuilder();
        
        if (depth > 0) {
            sb.append("└");

            for (int i = 0; i < depth; i++) {
                sb.append('-');
            }
        }
        
        return sb.toString();
    }

    /**
     * Return debugging string for a plan node.
     */
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

    /**
     * Return parent node
     */
    public AltibasePlanNode getParentNodeAtDepth(int depth) {
        if (this.depth > depth) {
            return this.parent.getParentNodeAtDepth(depth);
        } else if (this.depth == depth) {
            return this.parent;
        } else {
            throw new IllegalArgumentException("Argument depth: " + depth + ", this.depth: " + this.depth);
        }
    }

    @Override
    public String getNodeType() {
        return operation;
    }

    @Override
    public DBCPlanNodeKind getNodeKind() {
        for (String kind : allowedKind) {
            if (operation.contains(kind)) {

                switch (kind) {

                    case "SCAN":
                        if (options.contains("FULL SCAN")) {
                            return DBCPlanNodeKind.TABLE_SCAN;
                        } else {
                            return DBCPlanNodeKind.INDEX_SCAN;
                        }

                    case "VIEW":
                        return DBCPlanNodeKind.SELECT;

                    case "PROJECT":
                        return DBCPlanNodeKind.SET;

                    case "FILTER":
                        return DBCPlanNodeKind.FILTER;

                    case "SORT":
                        return DBCPlanNodeKind.SORT;

                    case "HASH":
                        return DBCPlanNodeKind.HASH;  

                    case "GROUP":
                        return DBCPlanNodeKind.GROUP;

                    case "AGGREGATION":
                        return DBCPlanNodeKind.AGGREGATE;

                    case "MERGE":
                        return DBCPlanNodeKind.MERGE;

                    case "JOIN":
                        return DBCPlanNodeKind.JOIN;

                    case "UNION":
                        return DBCPlanNodeKind.UNION;

                    case "INERT":
                    case "UPDATE":
                    case "DELETE":
                        return DBCPlanNodeKind.MODIFY;

                    default:
                        return DBCPlanNodeKind.DEFAULT;

                }
            }
        }

        return DBCPlanNodeKind.DEFAULT;
    }
}
