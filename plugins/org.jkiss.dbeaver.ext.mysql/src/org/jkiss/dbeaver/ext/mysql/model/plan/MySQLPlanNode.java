/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MySQL execution plan node
 */
public class MySQLPlanNode implements DBCPlanNode, DBCPlanCostNode {

    private long id;
    private String selectType;
    private String table;
    private String type;
    private String possibleKeys;
    private String key;
    private String keyLength;
    private String ref;
    private long rowCount;
    private double filtered;
    private String extra;

    private MySQLPlanNode parent;
    private List<MySQLPlanNode> nested;

    public MySQLPlanNode(List<MySQLPlanNode> nodes) {
        // Root node
        type = "<plan>";
        if (!nodes.isEmpty()) {
            this.rowCount = nodes.get(0).rowCount;
        }
        this.nested = nodes;
    }

    public MySQLPlanNode(MySQLPlanNode parent, ResultSet dbResult) {
        this.parent = parent;
        this.id = JDBCUtils.safeGetLong(dbResult, "id");
        this.selectType = JDBCUtils.safeGetString(dbResult, "select_type");
        this.table = JDBCUtils.safeGetString(dbResult, "table");
        this.type = JDBCUtils.safeGetString(dbResult, "type");
        this.possibleKeys = JDBCUtils.safeGetString(dbResult, "possible_keys");
        this.key = JDBCUtils.safeGetString(dbResult, "key");
        this.keyLength = JDBCUtils.safeGetString(dbResult, "key_len");
        this.ref = JDBCUtils.safeGetString(dbResult, "ref");
        this.rowCount = JDBCUtils.safeGetLong(dbResult, "rows");
        this.filtered = JDBCUtils.safeGetDouble(dbResult, "filtered");
        this.extra = JDBCUtils.safeGetString(dbResult, "extra");
    }

    @Override
    public DBCPlanNode getParent() {
        return parent;
    }

    @Override
    public String getNodeName() {
        return table;
    }

    @Override
    public String getNodeDescription() {
        return ref;
    }

    @Override
    @Property(order = 3, viewable = true)
    public String getNodeType() {
        return type;
    }

    @Override
    public List<MySQLPlanNode> getNested() {
        return nested;
    }

    @Property(order = 0, viewable = true)
    public long getId() {
        return id;
    }

    @Property(order = 1, viewable = true)
    public String getSelectType() {
        return selectType;
    }

    @Property(order = 2, viewable = true)
    public String getTable() {
        return table;
    }

    @Property(order = 4, viewable = true)
    public String getPossibleKeys() {
        return possibleKeys;
    }

    @Property(order = 5, viewable = true)
    public String getKey() {
        return key;
    }

    @Property(order = 6, viewable = true)
    public String getKeyLength() {
        return keyLength;
    }

    @Property(order = 7, viewable = true)
    public String getRef() {
        return ref;
    }

    @Property(order = 8, viewable = true)
    public long getRowCount() {
        return rowCount;
    }

    @Property(order = 9, viewable = true)
    public double getFiltered() {
        return filtered;
    }

    @Property(order = 10, viewable = true)
    public String getExtra() {
        return extra;
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
        return rowCount;
    }

    @Override
    public String toString() {
        return table + " " + type + " " + key;
    }
}
