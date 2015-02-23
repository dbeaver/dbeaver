/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.oracle.model.plan;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectType;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablePhysical;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IntKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Oracle execution plan node
 */
public class OraclePlanNode implements DBCPlanNode {

    private final OracleDataSource dataSource;
    private String statement_id;
    private long plan_id;
    private Timestamp timestamp;
    private String remarks;
    private String operation;
    private String options;
    private String objectNode;
    private String objectOwner;
    private String objectName;
    private String objectAlias;
    private long object_instance;
    private String objectType;
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

    public OraclePlanNode(OracleDataSource dataSource, IntKeyMap<OraclePlanNode> prevNodes, ResultSet dbResult) throws SQLException
    {
        this.dataSource = dataSource;
        this.statement_id = JDBCUtils.safeGetString(dbResult, "statement_id");
        this.plan_id = JDBCUtils.safeGetLong(dbResult, "plan_id");
        this.timestamp = JDBCUtils.safeGetTimestamp(dbResult, "timestamp");
        this.remarks = JDBCUtils.safeGetString(dbResult, "remarks");
        this.operation = JDBCUtils.safeGetString(dbResult, "operation");
        this.options = JDBCUtils.safeGetString(dbResult, "options");
        this.objectNode = JDBCUtils.safeGetString(dbResult, "object_node");
        this.objectOwner = JDBCUtils.safeGetString(dbResult, "object_owner");
        this.objectName = JDBCUtils.safeGetString(dbResult, "object_name");
        this.objectAlias = JDBCUtils.safeGetString(dbResult, "object_alias");
        this.object_instance = JDBCUtils.safeGetLong(dbResult, "object_instance");
        this.objectType = JDBCUtils.safeGetString(dbResult, "object_type");
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

    @Override
    public OraclePlanNode getParent()
    {
        return parent;
    }

    @Override
    public Collection<OraclePlanNode> getNested()
    {
        return nested;
    }

    //@Property(name = "ID", order = 0, viewable = true, description = "Node ID")
    public int getId()
    {
        return id;
    }

    @Property(order = 1, viewable = true)
    public String getOperation()
    {
        if (CommonUtils.isEmpty(options)) {
            return operation;
        } else {
            return operation + " (" + options + ")";
        }
    }

    //@Property(name = "Options", order = 2, viewable = true, description = "A variation on the operation described in the Operation column")
    public String getOptions()
    {
        return options;
    }

    //@Property(name = "Type", order = 3, viewable = true, description = "Object type")
    public String getObjectType()
    {
        return objectType;
    }

    //@Property(name = "Owner", order = 4, viewable = true, description = "Object owner (schema)")
//    public Object getObjectOwner()
//    {
//        final OracleSchema schema = dataSource.schemaCache.getCachedObject(objectOwner);
//        return schema == null ? objectOwner : schema;
//    }

    @Property(order = 5, viewable = true, supportsPreview = true)
    public Object getObject(DBRProgressMonitor monitor) throws DBException
    {
        if (monitor == null || CommonUtils.isEmpty(objectOwner)) {
            return objectName == null ? "" : objectName;
        }
        String objectTypeName = objectType;
        final int divPos = objectTypeName == null ? -1 : objectTypeName.indexOf('(');
        if (divPos != -1) {
            objectTypeName = objectTypeName.substring(0, divPos).trim();
        }
        if (OracleObjectType.INDEX.name().equals(objectTypeName)) {
            // Get index from parent table - reading of all indexes takes too much time
            for (OraclePlanNode parentNode = parent; parentNode != null; parentNode = parentNode.getParent()) {
                final Object parentObject = parentNode.getObject(monitor);
                if (parentObject instanceof OracleTablePhysical) {
                    return ((OracleTablePhysical) parentObject).getIndex(monitor, objectName);
                }
            }
            return objectName;
        } else {
            objectTypeName = OracleObjectType.TABLE.name();
        }

        return OracleObjectType.resolveObject(
            monitor,
            dataSource,
            objectNode,
            objectTypeName,
            objectOwner,
            objectName);
    }

    //@Property(name = "Alias", order = 6, viewable = true, description = "Object alias")
    public String getAlias()
    {
        return objectAlias;
    }

    @Property(order = 7, viewable = true)
    public String getOptimizer()
    {
        return optimizer;
    }

    @Property(order = 8, viewable = true)
    public long getCost()
    {
        return cost;
    }

    @Property(order = 9, viewable = true)
    public long getCardinality()
    {
        return cardinality;
    }

    @Property(order = 10, viewable = true)
    public long getBytes()
    {
        return bytes;
    }

    @Override
    public String toString()
    {
        return operation + " " + CommonUtils.toString(options) + " " + CommonUtils.toString(objectName);
    }
}
