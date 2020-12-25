/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MySQL execution plan node.
 *
 * Select type:
 *
 SIMPLE – the query is a simple SELECT query without any subqueries or UNIONs
 PRIMARY – the SELECT is in the outermost query in a JOIN
 DERIVED – the SELECT is part of a subquery within a FROM clause
 SUBQUERY – the first SELECT in a subquery
 DEPENDENT SUBQUERY – a subquery which is dependent upon on outer query
 UNCACHEABLE SUBQUERY – a subquery which is not cacheable (there are certain conditions for a query to be cacheable)
 UNION – the SELECT is the second or later statement of a UNION
 DEPENDENT UNION – the second or later SELECT of a UNION is dependent on an outer query
 UNION RESULT – the SELECT is a result of a UNION

 */
public class MySQLPlanNodePlain extends MySQLPlanNode {

    protected Integer id;
    protected String selectType;
    protected String table;
    protected String type;
    protected String possibleKeys;
    protected String key;
    protected String keyLength;
    protected String ref;
    protected Long rowCount;
    protected Long filtered;
    protected String extra;

    protected MySQLPlanNodePlain parent;
    protected List<MySQLPlanNodePlain> nested;

    public MySQLPlanNodePlain(List<MySQLPlanNodePlain> nodes) {
        // Root node
        type = "<plan>";
        if (!nodes.isEmpty()) {
            this.rowCount = nodes.get(0).rowCount;
        }
        this.nested = nodes;
    }

    public MySQLPlanNodePlain(MySQLPlanNodePlain parent, ResultSet dbResult) {
        this.parent = parent;
        this.id = JDBCUtils.safeGetInteger(dbResult, "id");
        this.selectType = JDBCUtils.safeGetString(dbResult, "select_type");
        this.table = JDBCUtils.safeGetString(dbResult, "table");
        this.type = JDBCUtils.safeGetString(dbResult, "type");
        this.possibleKeys = JDBCUtils.safeGetString(dbResult, "possible_keys");
        this.key = JDBCUtils.safeGetString(dbResult, "key");
        this.keyLength = JDBCUtils.safeGetString(dbResult, "key_len");
        this.ref = JDBCUtils.safeGetString(dbResult, "ref");
        this.rowCount = JDBCUtils.safeGetLongNullable(dbResult, "rows");
        this.filtered = JDBCUtils.safeGetLongNullable(dbResult, "filtered");
        this.extra = JDBCUtils.safeGetString(dbResult, "extra");
    }

    public MySQLPlanNodePlain(MySQLPlanNodePlain parent, Map<String, String> props) {
        this.parent = parent;
        this.id = props.containsKey("id") ? CommonUtils.toInt(props.get("id")) : null;
        this.selectType = props.get("select_type");
        this.table = props.get("table");
        this.type = props.get("type");
        this.possibleKeys = props.get("possible_keys");
        this.key = props.get("key");
        this.keyLength = props.get("key_len");
        this.ref = props.get("ref");
        this.rowCount = props.containsKey("rows") ? CommonUtils.toLong(props.get("rows")) : null;
        this.filtered =  props.containsKey("filtered") ? CommonUtils.toLong(props.get("filtered")) : null;
        this.extra = props.get("extra");
    }

    public MySQLPlanNodePlain(MySQLPlanNodePlain parent, String type) {
        this.parent = parent;
        this.selectType = type;
    }

    protected MySQLPlanNodePlain(MySQLPlanNodePlain parent, MySQLPlanNodePlain source) {
        this.id = source.id;
        this.selectType = source.selectType;
        this.table = source.table;
        this.type = source.type;
        this.possibleKeys = source.possibleKeys;
        this.key = source.key;
        this.keyLength = source.keyLength;
        this.ref = source.ref;
        this.rowCount = source.rowCount;
        this.filtered = source.filtered;
        this.extra = source.extra;

        this.parent = parent;
        if (source.nested != null) {
            this.nested = new ArrayList<>(source.nested.size());
            for (MySQLPlanNodePlain srcNode : source.nested) {
                this.nested.add(srcNode.copyNode(this));
            }
        }
    }

    @Override
    public MySQLPlanNodePlain getParent() {
        return parent;
    }

    void setParent(MySQLPlanNodePlain node) {
        if (this.parent != null && this.parent.nested != null) {
            this.parent.nested.remove(this);
        }
        this.parent = node;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private void addChild(MySQLPlanNodePlain node) {
        if (this.nested == null) {
            this.nested = new ArrayList<>();
        }
        this.nested.add(node);

    }

    @Override
    public String getNodeName() {
        return table;
    }

    @Override
    public DBCPlanNodeKind getNodeKind() {
        if ("SIMPLE".equals(selectType)) {
            return DBCPlanNodeKind.SELECT;
        } else if ("JOIN".equals(selectType)) {
            return DBCPlanNodeKind.JOIN;
        } else if ("UNION".equals(selectType)) {
            return DBCPlanNodeKind.UNION;
        }
        return super.getNodeKind();
    }

    @Override
    public String getNodeDescription() {
        return ref;
    }

    @Override
    @Property(order = 3, viewable = true)
    public String getNodeType() {
        return selectType;
    }

    @Override
    public List<MySQLPlanNodePlain> getNested() {
        return nested;
    }

    @Property(order = 0, viewable = true)
    public Integer getId() {
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
    public Long getRowCount() {
        return rowCount;
    }

    @Property(order = 9, viewable = true)
    public Long getFiltered() {
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

    public boolean isCompositeNode() {
        return "PRIMARY".equals(selectType);
    }

    @Override
    public String toString() {
        return id + " " + selectType + " " + table;
    }

    void computeStats() {
        if (nested != null) {
            for (MySQLPlanNodePlain child : nested) {
                child.computeStats();
            }
        }
        if (rowCount == null) {
            if (nested != null) {
                long calcCount = 0;
                for (MySQLPlanNodePlain child : nested) {
                    child.computeStats();
                    calcCount += CommonUtils.toLong(child.getRowCount());
                }
                this.rowCount = calcCount;
            }
        }
    }

    MySQLPlanNodePlain copyNode(MySQLPlanNodePlain parent) {
        return new MySQLPlanNodePlain(parent, this);
    }
}
