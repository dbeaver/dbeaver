/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.polardbx.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PolarDBXPlanNodePlain extends MySQLPlanNode {
    protected String id;
    protected String estRows;
    protected String task;
    protected String accessObject;
    protected String operatorInfo;

    protected PolarDBXPlanNodePlain parent;
    protected List<PolarDBXPlanNodePlain> nested;

    public PolarDBXPlanNodePlain(@NotNull List<PolarDBXPlanNodePlain> nodes) {
        id = "<plan>";
        if (!nodes.isEmpty()) {
            this.estRows = nodes.get(0).estRows;
        }
        this.nested = nodes;
    }

    public PolarDBXPlanNodePlain(@Nullable PolarDBXPlanNodePlain parent, @NotNull ResultSet dbResult) {
        this.parent = parent;
        this.id = JDBCUtils.safeGetString(dbResult, "id");
        this.estRows = JDBCUtils.safeGetString(dbResult, "estRows");
        this.task = JDBCUtils.safeGetString(dbResult, "task");
        this.accessObject = JDBCUtils.safeGetString(dbResult, "access object");
        this.operatorInfo = JDBCUtils.safeGetString(dbResult, "operator info");
    }

    @Nullable
    @Override
    public PolarDBXPlanNodePlain getParent() {
        return parent;
    }

    @Nullable
    @Override
    public Number getNodeCost() {
        return null;
    }

    @Nullable
    @Override
    public Number getNodePercent() {
        return null;
    }

    @Nullable
    @Override
    public Number getNodeDuration() {
        return null;
    }

    @Nullable
    @Override
    public Number getNodeRowCount() {
        if (estRows == null) {
            return null;
        }
        try {
            return Double.parseDouble(estRows);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    @Override
    public String getNodeName() {
        return this.accessObject;
    }

    @Nullable
    @Override
    public String getNodeType() {
        return id == null ? null : id.trim().replaceAll("└", "").replaceAll("─", "");
    }

    @NotNull
    @Override
    public Collection<? extends DBCPlanNode> getNested() {
        return nested == null ? List.of() : nested;
    }

    @Property(order = 0, viewable = true)
    @Nullable
    public String getId() {
        return id;
    }

    @Property(order = 1, viewable = true)
    @Nullable
    public String getEstRows() {
        return estRows;
    }

    @Property(order = 2, viewable = true)
    @Nullable
    public String getTask() {
        return task;
    }

    @Property(order = 3, viewable = true)
    @Nullable
    public String getAccessObject() {
        return accessObject;
    }

    @Property(order = 4, viewable = true)
    @Nullable
    public String getOperatorInfo() {
        return operatorInfo;
    }

    void setParent(@Nullable PolarDBXPlanNodePlain node) {
        if (this.parent != null && this.parent.nested != null) {
            this.parent.nested.remove(this);
        }
        this.parent = node;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private void addChild(@NotNull PolarDBXPlanNodePlain node) {
        if (this.nested == null) {
            this.nested = new ArrayList<>();
        }
        this.nested.add(node);
    }
}
