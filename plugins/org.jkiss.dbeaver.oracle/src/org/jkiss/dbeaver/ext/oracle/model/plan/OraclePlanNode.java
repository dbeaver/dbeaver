/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IntKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle execution plan node
 */
public class OraclePlanNode implements DBCPlanNode {

    private String statement_id;
    private long plan_id;
    private Timestamp timestamp;
    private String remarks;
    private String operation;
    private String options;
    private String object_node;
    private String object_owner;
    private String object_name;
    private String object_alias;
    private long object_instance;
    private String object_type;
    private String optimizer;
    private long search_columns;
    private int id;
    private int parent_id;
    private int depth;
    private int position;
    private long cost;
    private long cardinality;
    private long bytes;
    private String other_tag;
    private String partition_start;
    private String partition_stop;
    private long partition_id;
    private String other;
    private String distribution;
    private long cpu_cost;
    private long io_cost;
    private long temp_space;
    private String access_predicates;
    private String filter_predicates;
    private String projection;
    private long time;
    private String qblock_name;
    private String other_xml;

    private OraclePlanNode parent;
    private List<OraclePlanNode> nested;

    public OraclePlanNode(IntKeyMap<OraclePlanNode> prevNodes, ResultSet dbResult) throws SQLException
    {
        this.statement_id = JDBCUtils.safeGetString(dbResult, "statement_id");
        this.plan_id = JDBCUtils.safeGetLong(dbResult, "plan_id");
        this.timestamp = JDBCUtils.safeGetTimestamp(dbResult, "timestamp");
        this.remarks = JDBCUtils.safeGetString(dbResult, "remarks");
        this.operation = JDBCUtils.safeGetString(dbResult, "operation");
        this.options = JDBCUtils.safeGetString(dbResult, "options");
        this.object_node = JDBCUtils.safeGetString(dbResult, "object_node");
        this.object_owner = JDBCUtils.safeGetString(dbResult, "object_owner");
        this.object_name = JDBCUtils.safeGetString(dbResult, "object_name");
        this.object_alias = JDBCUtils.safeGetString(dbResult, "object_alias");
        this.object_instance = JDBCUtils.safeGetLong(dbResult, "object_instance");
        this.object_type = JDBCUtils.safeGetString(dbResult, "object_type");
        this.optimizer = JDBCUtils.safeGetString(dbResult, "optimizer");
        this.search_columns = JDBCUtils.safeGetLong(dbResult, "search_columns");
        this.id = JDBCUtils.safeGetInt(dbResult, "id");
        this.depth = JDBCUtils.safeGetInt(dbResult, "depth");
        this.position = JDBCUtils.safeGetInt(dbResult, "position");
        this.cost = JDBCUtils.safeGetLong(dbResult, "cost");
        this.cardinality = JDBCUtils.safeGetLong(dbResult, "cardinality");
        this.bytes = JDBCUtils.safeGetLong(dbResult, "bytes");
        this.other_tag = JDBCUtils.safeGetString(dbResult, "other_tag");
        this.partition_start = JDBCUtils.safeGetString(dbResult, "partition_start");
        this.partition_stop = JDBCUtils.safeGetString(dbResult, "partition_stop");
        this.partition_id = JDBCUtils.safeGetLong(dbResult, "partition_id");
        this.other = JDBCUtils.safeGetString(dbResult, "other");
        this.distribution = JDBCUtils.safeGetString(dbResult, "distribution");
        this.cpu_cost = JDBCUtils.safeGetLong(dbResult, "cpu_cost");
        this.io_cost = JDBCUtils.safeGetLong(dbResult, "io_cost");
        this.temp_space = JDBCUtils.safeGetLong(dbResult, "temp_space");
        this.access_predicates = JDBCUtils.safeGetString(dbResult, "access_predicates");
        this.filter_predicates = JDBCUtils.safeGetString(dbResult, "filter_predicates");
        this.projection = JDBCUtils.safeGetString(dbResult, "projection");
        this.time = JDBCUtils.safeGetLong(dbResult, "time");
        this.qblock_name = JDBCUtils.safeGetString(dbResult, "qblock_name");
        this.other_xml = JDBCUtils.safeGetString(dbResult, "other_xml");

        Integer parent_id = JDBCUtils.safeGetInteger(dbResult, "parent_id");
        if (parent_id != null) {
            parent = prevNodes.get(parent_id);
        }
        if (parent != null) {
            if (parent.nested == null) {
                parent.nested = new ArrayList<OraclePlanNode>();
            }
            parent.nested.add(this);
        }
    }

    public DBCPlanNode getParent()
    {
        return parent;
    }

    public List<OraclePlanNode> getNested()
    {
        return nested;
    }

    //@Property(name = "ID", order = 0, viewable = true, description = "Node ID")
    public int getId()
    {
        return id;
    }

    @Property(name = "Operation", order = 1, viewable = true, description = "Operation")
    public String getOperation()
    {
        return operation;
    }

    @Property(name = "Options", order = 2, viewable = true, description = "Options")
    public String getOptions()
    {
        return options;
    }

    @Property(name = "Object", order = 3, viewable = true, description = "Object")
    public String getObjectName()
    {
        return object_name;
    }

    @Override
    public String toString()
    {
        return getOperation() + " " + CommonUtils.toString(getOptions()) + " " + CommonUtils.toString(getObjectName());
    }
}