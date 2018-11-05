/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
    private String statementId;
    private long plan_id;
    private Timestamp timestamp;
    private String remarks;
    private String operation;
    private String options;
    private String objectNode;
    private String objectOwner;
    private String objectName;
    private String objectAlias;
    private long objectInstance;
    private String objectType;
    private String optimizer;
    private long searchColumns;
    private int id;
    private int parentId;
    private int depth;
    private int position;
    private long cost;
    private long cardinality;
    private long bytes;
    private String otherTag;
    private String partitionStart;
    private String partitionStop;
    private long partitionId;
    private String other;
    private String distribution;
    private long cpuCost;
    private long ioCost;
    private long tempSpace;
    private String accessPredicates;
    private String filterPredicates;
    private String projection;
    private long time;
    private String qblockName;
    private String otherXml;

    private OraclePlanNode parent;
    private List<OraclePlanNode> nested;

    public OraclePlanNode(OracleDataSource dataSource, IntKeyMap<OraclePlanNode> prevNodes, ResultSet dbResult) throws SQLException
    {
        this.dataSource = dataSource;
        this.statementId = JDBCUtils.safeGetString(dbResult, "statement_id");
        this.plan_id = JDBCUtils.safeGetLong(dbResult, "plan_id");
        this.timestamp = JDBCUtils.safeGetTimestamp(dbResult, "timestamp");
        this.remarks = JDBCUtils.safeGetString(dbResult, "remarks");
        this.operation = JDBCUtils.safeGetString(dbResult, "operation");
        this.options = JDBCUtils.safeGetString(dbResult, "options");
        this.objectNode = JDBCUtils.safeGetString(dbResult, "object_node");
        this.objectOwner = JDBCUtils.safeGetString(dbResult, "object_owner");
        this.objectName = JDBCUtils.safeGetString(dbResult, "object_name");
        this.objectAlias = JDBCUtils.safeGetString(dbResult, "object_alias");
        this.objectInstance = JDBCUtils.safeGetLong(dbResult, "object_instance");
        this.objectType = JDBCUtils.safeGetString(dbResult, "object_type");
        this.optimizer = JDBCUtils.safeGetString(dbResult, "optimizer");
        this.searchColumns = JDBCUtils.safeGetLong(dbResult, "search_columns");
        this.id = JDBCUtils.safeGetInt(dbResult, "id");
        this.depth = JDBCUtils.safeGetInt(dbResult, "depth");
        this.position = JDBCUtils.safeGetInt(dbResult, "position");
        this.cost = JDBCUtils.safeGetLong(dbResult, "cost");
        this.cardinality = JDBCUtils.safeGetLong(dbResult, "cardinality");
        this.bytes = JDBCUtils.safeGetLong(dbResult, "bytes");
        this.otherTag = JDBCUtils.safeGetString(dbResult, "other_tag");
        this.partitionStart = JDBCUtils.safeGetString(dbResult, "partition_start");
        this.partitionStop = JDBCUtils.safeGetString(dbResult, "partition_stop");
        this.partitionId = JDBCUtils.safeGetLong(dbResult, "partition_id");
        this.other = JDBCUtils.safeGetString(dbResult, "other");
        this.distribution = JDBCUtils.safeGetString(dbResult, "distribution");
        this.cpuCost = JDBCUtils.safeGetLong(dbResult, "cpu_cost");
        this.ioCost = JDBCUtils.safeGetLong(dbResult, "io_cost");
        this.tempSpace = JDBCUtils.safeGetLong(dbResult, "temp_space");
        this.accessPredicates = JDBCUtils.safeGetString(dbResult, "access_predicates");
        this.filterPredicates = JDBCUtils.safeGetString(dbResult, "filter_predicates");
        this.projection = JDBCUtils.safeGetString(dbResult, "projection");
        this.time = JDBCUtils.safeGetLong(dbResult, "time");
        this.qblockName = JDBCUtils.safeGetString(dbResult, "qblock_name");
        this.otherXml = JDBCUtils.safeGetString(dbResult, "other_xml");

        Integer parent_id = JDBCUtils.safeGetInteger(dbResult, "parent_id");
        if (parent_id != null) {
            parent = prevNodes.get(parent_id);
        }
        if (parent != null) {
            if (parent.nested == null) {
                parent.nested = new ArrayList<>();
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
        int divPos = objectTypeName == null ? -1 : objectTypeName.indexOf('(');
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

        if (objectName.startsWith("X$")) {
            // Some internal stuff
            return objectName;
        }
        divPos = objectName.indexOf("(");
        String name = divPos == -1 ? objectName : objectName.substring(0, divPos);

        return OracleObjectType.resolveObject(
            monitor,
            dataSource,
            objectNode,
            objectTypeName,
            objectOwner,
            name.trim());
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

    @Property(order = 20)
    public String getPartitionStart() {
        return partitionStart;
    }

    @Property(order = 21)
    public String getPartitionStop() {
        return partitionStop;
    }

    @Property(order = 22)
    public long getPartitionId() {
        return partitionId;
    }

    @Property(order = 23)
    public String getDistribution() {
        return distribution;
    }

    @Property(order = 24)
    public long getCpuCost() {
        return cpuCost;
    }

    @Property(order = 25)
    public long getIoCost() {
        return ioCost;
    }

    @Property(order = 26)
    public long getTempSpace() {
        return tempSpace;
    }

    @Property(order = 27)
    public String getAccessPredicates() {
        return accessPredicates;
    }

    @Property(order = 28)
    public String getFilterPredicates() {
        return filterPredicates;
    }

    @Property(order = 29)
    public String getProjection() {
        return projection;
    }

    @Property(order = 30)
    public long getTime() {
        return time;
    }

    @Property(order = 31)
    public String getQblockName() {
        return qblockName;
    }

    @Override
    public String toString()
    {
        return operation + " " + CommonUtils.toString(options) + " " + CommonUtils.toString(objectName);
    }
}
