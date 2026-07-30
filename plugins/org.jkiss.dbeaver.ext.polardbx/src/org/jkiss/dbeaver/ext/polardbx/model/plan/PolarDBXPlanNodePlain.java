/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.jkiss.dbeaver.ext.polardbx.model.plan;

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

    public PolarDBXPlanNodePlain(List<PolarDBXPlanNodePlain> nodes) {
        id = "<plan>";
        if (!nodes.isEmpty()) {
            this.estRows = nodes.get(0).estRows;
        }
        this.nested = nodes;
    }

    public PolarDBXPlanNodePlain(PolarDBXPlanNodePlain parent, ResultSet dbResult) {
        this.parent = parent;
        this.id = JDBCUtils.safeGetString(dbResult, "id");
        this.estRows = JDBCUtils.safeGetString(dbResult, "estRows");
        this.task = JDBCUtils.safeGetString(dbResult, "task");
        this.accessObject = JDBCUtils.safeGetString(dbResult, "access object");
        this.operatorInfo = JDBCUtils.safeGetString(dbResult, "operator info");
    }

    @Override
    public PolarDBXPlanNodePlain getParent() {
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

    void setParent(PolarDBXPlanNodePlain node) {
        if (this.parent != null && this.parent.nested != null) {
            this.parent.nested.remove(this);
        }
        this.parent = node;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private void addChild(PolarDBXPlanNodePlain node) {
        if (this.nested == null) {
            this.nested = new ArrayList<>();
        }
        this.nested.add(node);
    }
}