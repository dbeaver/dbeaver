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
package org.jkiss.dbeaver.ext.oracle.model.plan;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectType;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablePhysical;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IntKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Oracle execution plan node
 */
public class OraclePlanNode extends AbstractExecutionPlanNode implements DBCPlanCostNode {

    public final static String CAT_DETAILS = "Details";
    
    private final static List<String> allowedKind = new ArrayList<>( 
              Arrays.asList("result",
                            "project",
                            "filter",
                            "collector",
                            "index",
                            "hash",
                            "foregin",
                            "aggregate",
                            "modify",
                            "inset",
                            "update",
                            "delete",
                            "loop",
                            "join",
                            "merge",
                            "sort",
                            "merge",
                            "group",
                            "materialize",
                            "function"));

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
    protected final List<OraclePlanNode> nested = new ArrayList<>();
    


    private String aGetString(Map<String,String> attributes,String name) {
        return attributes.containsKey(name) ? attributes.get(name).toString() : "";
    }

    private long aGetLong(Map<String,String> attributes,String name) {
        if (attributes.containsKey(name)) {
            try {
                return Long.parseLong(attributes.get(name));
            } catch (Exception e) {
                return 0L;
            }  		
        } else {
            return 0L;
        }
    }

    private Timestamp aGetTimestamp(Map<String,String> attributes,String name) {
        if (attributes.containsKey(name)) {
            try {
                Long inst =  Long.parseLong(attributes.get(name));
                return Timestamp.from(Instant.ofEpochMilli(inst));
            } catch (Exception e) {
                return new Timestamp(0);
            }  		
        } else {
            return new Timestamp(0);
        }
    }

    private int aGetInt(Map<String,String> attributes,String name) {
        if (attributes.containsKey(name)) {
            try {
                return Integer.parseInt(attributes.get(name));
            } catch (Exception e) {
                return 0;
            }  		
        } else {
            return 0;
        }
    }
    
     @Override
    public DBCPlanNodeKind getNodeKind() {

        String op = operation.toLowerCase();

        for (String kind : allowedKind) {
            if (op.contains(kind)) {

                switch (kind) {

                case "result":
                    return DBCPlanNodeKind.RESULT;

                case "project":
                    return DBCPlanNodeKind.SET;

                case "filter":
                    return DBCPlanNodeKind.FILTER;

                case "collector":
                    return DBCPlanNodeKind.AGGREGATE;

                case "index":
                    return DBCPlanNodeKind.INDEX_SCAN;

                case "hash":
                    return DBCPlanNodeKind.HASH;

                case "foregin":
                    return DBCPlanNodeKind.TABLE_SCAN;

                case "aggregate":
                    return DBCPlanNodeKind.AGGREGATE;

                case "modify":
                    return DBCPlanNodeKind.MODIFY;

                case "insert":
                    return DBCPlanNodeKind.MODIFY;

                case "update":
                    return DBCPlanNodeKind.MODIFY;

                case "delete":
                    return DBCPlanNodeKind.MODIFY;

                case "loop":
                    return DBCPlanNodeKind.JOIN;

                case "join":
                    return DBCPlanNodeKind.JOIN;

                case "merge":
                    return DBCPlanNodeKind.MERGE;

                case "sort":
                    return DBCPlanNodeKind.SORT;

                case "group":
                    return DBCPlanNodeKind.GROUP;

                case "materialize":
                    return DBCPlanNodeKind.MATERIALIZE;

                case "function":
                    return DBCPlanNodeKind.FUNCTION;

                default:
                    return DBCPlanNodeKind.DEFAULT;

                }

            }
        }

        return DBCPlanNodeKind.DEFAULT;
    }

    public OraclePlanNode(OracleDataSource dataSource, IntKeyMap<OraclePlanNode> prevNodes, Map<String,String> attributes) {
        this.dataSource = dataSource;

        this.statementId = aGetString(attributes,"statement_id");
        this.plan_id = aGetLong(attributes,"plan_id");
        this.timestamp = aGetTimestamp(attributes, "timestamp");
        this.remarks = aGetString(attributes,"remarks");
        this.operation = aGetString(attributes,"operation");
        this.options = aGetString(attributes,"options");
        this.objectNode = aGetString(attributes,"object_node");
        this.objectOwner = aGetString(attributes,"object_owner");
        this.objectName = aGetString(attributes,"object_name");
        this.objectAlias = aGetString(attributes,"object_alias");
        this.objectInstance = aGetLong(attributes,"object_instance");
        this.objectType = aGetString(attributes,"object_type");
        this.optimizer = aGetString(attributes,"optimizer");
        this.searchColumns = aGetLong(attributes,"search_columns");
        this.id = aGetInt(attributes, "id");
        this.depth = aGetInt(attributes,"depth");
        this.position =  aGetInt(attributes,"position");
        this.cost = aGetLong(attributes,"cost");
        this.cardinality = aGetLong(attributes,"cardinality");
        this.bytes = aGetLong(attributes,"bytes");
        this.otherTag = aGetString(attributes,"other_tag");
        this.partitionStart = aGetString(attributes,"partition_start");
        this.partitionStop = aGetString(attributes,"partition_stop");
        this.partitionId = aGetLong(attributes,"partition_id");
        this.other = aGetString(attributes,"other");
        this.distribution = aGetString(attributes,"distribution");
        this.cpuCost = aGetLong(attributes,"cpu_cost");
        this.ioCost = aGetLong(attributes,"io_cost");
        this.tempSpace = aGetLong(attributes,"temp_space");
        this.accessPredicates = aGetString(attributes,"access_predicates");
        this.filterPredicates = aGetString(attributes,"filter_predicates");
        this.projection = aGetString(attributes,"projection");
        this.time = aGetLong(attributes,"time");
        this.qblockName = aGetString(attributes,"qblock_name");
        this.otherXml = aGetString(attributes,"other_xml");

        Integer parent_id =  aGetInt(attributes,"parent_id");

        if (parent_id != null) {
            parent = prevNodes.get(parent_id);
        }
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public OraclePlanNode(OracleDataSource dataSource, IntKeyMap<OraclePlanNode> prevNodes, ResultSet dbResult) throws SQLException {
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
            parent.addChild(this);
        }
    }

    private void addChild(OraclePlanNode node) {
         this.nested.add(node);
    }

    @Override
    public OraclePlanNode getParent() {
        return parent;
    }

    @Override
    public Collection<OraclePlanNode> getNested() {
        return nested;
    }

    @Override
    public String getNodeName() {
        return objectName;
    }

    @Override
    public String getNodeType() {
        return operation;
    }

    @Override
    public String getNodeDescription() {
        return null;
    }

    //@Property(name = "ID", order = 0, viewable = true, description = "Node ID")
    public int getId() {
        return id;
    }

    @Property(order = 1, viewable = true)
    public String getOperation() {
        if (CommonUtils.isEmpty(options)) {
            return operation;
        } else {
            return operation + " (" + options + ")";
        }
    }

    //@Property(name = "Options", order = 2, viewable = true, description = "A variation on the operation described in the Operation column")
    public String getOptions() {
        return options;
    }

    //@Property(name = "Type", order = 3, viewable = true, description = "Object type")
    public String getObjectType() {
        return objectType;
    }

    //@Property(name = "Owner", order = 4, viewable = true, description = "Object owner (schema)")
    //    public Object getObjectOwner()
    //    {
    //        final OracleSchema schema = dataSource.schemaCache.getCachedObject(objectOwner);
    //        return schema == null ? objectOwner : schema;
    //    }

    @Property(order = 5, viewable = true, supportsPreview = true)
    public Object getObject(DBRProgressMonitor monitor) throws DBException {
        if (monitor == null || CommonUtils.isEmpty(objectOwner) || CommonUtils.isEmpty(objectName)) {
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
    public String getAlias() {
        return objectAlias;
    }

    @Property(category = CAT_DETAILS, order = 7, viewable = true)
    public String getOptimizer() {
        return optimizer;
    }

    @Property(order = 8, viewable = true)
    public long getCost() {
        return cost;
    }

    @Property(order = 9, viewable = true)
    public long getCardinality() {
        return cardinality;
    }

    @Property(category = CAT_DETAILS, order = 10, viewable = true)
    public long getBytes() {
        return bytes;
    }

    @Property(category = CAT_DETAILS, order = 20)
    public String getPartitionStart() {
        return partitionStart;
    }

    @Property(category = CAT_DETAILS, order = 21)
    public String getPartitionStop() {
        return partitionStop;
    }

    @Property(category = CAT_DETAILS, order = 22)
    public long getPartitionId() {
        return partitionId;
    }

    @Property(category = CAT_DETAILS, order = 23)
    public String getDistribution() {
        return distribution;
    }

    @Property(category = CAT_DETAILS, order = 24)
    public long getCpuCost() {
        return cpuCost;
    }

    @Property(category = CAT_DETAILS, order = 25)
    public long getIoCost() {
        return ioCost;
    }

    @Property(category = CAT_DETAILS, order = 26)
    public long getTempSpace() {
        return tempSpace;
    }

    @Property(category = CAT_DETAILS, order = 27)
    public String getAccessPredicates() {
        return accessPredicates;
    }

    @Property(category = CAT_DETAILS, order = 28)
    public String getFilterPredicates() {
        return filterPredicates;
    }

    @Property(category = CAT_DETAILS, order = 29)
    public String getProjection() {
        return projection;
    }

    @Property(category = CAT_DETAILS, order = 30)
    public long getTime() {
        return time;
    }

    @Property(category = CAT_DETAILS, order = 31)
    public String getQblockName() {
        return qblockName;
    }

    @Override
    public String toString() {
        return operation + " " + CommonUtils.toString(options) + " " + CommonUtils.toString(objectName);
    }

    @Override
    public Number getNodeCost() {
        return cost;
    }

    @Override
    public Number getNodePercent() {
        return null;
    }

    @Override
    public Number getNodeDuration() {
        return (double) cpuCost / 1000;
    }

    @Override
    public Number getNodeRowCount() {
        return cardinality;
    }

    public String getStatementId() {
        return statementId;
    }

    public long getPlanId() {
        return plan_id;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getObjectNode() {
        return objectNode;
    }

    public String getObjectOwner() {
        return objectOwner;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectAlias() {
        return objectAlias;
    }

    public long getObjectInstance() {
        return objectInstance;
    }

    public long getSearchColumns() {
        return searchColumns;
    }

    public int getParentId() {
        return parentId;
    }

    public int getDepth() {
        return depth;
    }

    public int getPosition() {
        return position;
    }

    public String getOtherTag() {
        return otherTag;
    }

    public String getOther() {
        return other;
    }

    public String getOtherXml() {
        return otherXml;
    }

    public void updateCosts() {
        if (nested != null) {
            for (OraclePlanNode child : nested) {
                child.updateCosts();
            }
        }
        if (this.cost == 0 && this.cpuCost == 0 && nested != null) {
            for (OraclePlanNode child : nested) {
                this.cost += child.cost;
                this.cpuCost += child.cpuCost;
            }
        }
    }


}
