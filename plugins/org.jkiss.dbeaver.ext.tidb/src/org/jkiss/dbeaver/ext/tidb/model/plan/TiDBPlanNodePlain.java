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

package org.jkiss.dbeaver.ext.tidb.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TiDBPlanNodePlain extends MySQLPlanNode {
    protected String id;
    // Before 4.0 estRows named "count"
    protected String estRows;
    protected String task;
    protected String accessObject;
    protected String operatorInfo;

    protected TiDBPlanNodePlain parent;
    protected List<TiDBPlanNodePlain> nested;

    public TiDBPlanNodePlain(List<TiDBPlanNodePlain> nodes) {
        // Root node
        id = "<plan>";
        if (!nodes.isEmpty()) {
            this.estRows = nodes.get(0).estRows;
        }
        this.nested = nodes;
    }

    public TiDBPlanNodePlain(TiDBPlanNodePlain parent, ResultSet dbResult) {
        this.parent = parent;
        this.id = JDBCUtils.safeGetString(dbResult, "id");
        this.estRows = JDBCUtils.safeGetString(dbResult, "estRows");
        this.task = JDBCUtils.safeGetString(dbResult, "task");
        this.accessObject = JDBCUtils.safeGetString(dbResult, "access object");
        this.operatorInfo = JDBCUtils.safeGetString(dbResult, "operator info");
    }

    @Override
    public TiDBPlanNodePlain getParent() {
        return parent;
    }

    @Override
    public Number getNodeCost() {
        return null;
    }

    @Override
    public Number getNodePercent() {
        return null;
    }

    @Override
    public Number getNodeDuration() {
        return null;
    }

    @Override
    public Number getNodeRowCount() {
        return Double.parseDouble(this.estRows);
    }

    @Override
    public String getNodeName() {
        return this.accessObject;
    }

    /**
     * getNodeType
     * The result of explain will be like this:
     * +-------------------------+---------+-----------+---------------+--------------------------------+
     * | id                      | estRows | task      | access object | operator info                  |
     * +-------------------------+---------+-----------+---------------+--------------------------------+
     * | Delete                  | N/A     | root      |               | N/A                            |
     * | └─TableReader           | 0.00    | root      |               | data:Selection                 |
     * |   └─Selection           | 0.00    | cop[tikv] |               | eq(test.t1.c1, 3)              |
     * |     └─TableFullScan     | 3.00    | cop[tikv] | table:t1      | keep order:false, stats:pseudo |
     * +-------------------------+---------+-----------+---------------+--------------------------------+
     * So, If you want get the operator name, like "Delete" or "TableReader".
     * You need to replace the other chars.
     *
     * @return node type
     */
    @Override
    public String getNodeType() {
        return this.id.trim().replaceAll("└", "").replaceAll("─", "");
    }

    @Override
    public Collection<? extends DBCPlanNode> getNested() {
        return this.nested;
    }

    @Property(order = 0, viewable = true)
    public String getId() {
        return id;
    }

    @Property(order = 1, viewable = true)
    public String getEstRows() {
        return estRows;
    }

    @Property(order = 2, viewable = true)
    public String getTask() {
        return task;
    }

    @Property(order = 3, viewable = true)
    public String getAccessObject() {
        return accessObject;
    }

    @Property(order = 4, viewable = true)
    public String getOperatorInfo() {
        return operatorInfo;
    }

    void setParent(TiDBPlanNodePlain node) {
        if (this.parent != null && this.parent.nested != null) {
            this.parent.nested.remove(this);
        }
        this.parent = node;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private void addChild(TiDBPlanNodePlain node) {
        if (this.nested == null) {
            this.nested = new ArrayList<>();
        }
        this.nested.add(node);

    }
}
