/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

public class HANAPlanNode extends AbstractExecutionPlanNode {

    private HANAPlanNode parentNode;
    private Collection<HANAPlanNode> nestedNodes = new ArrayList<>(64);

    private String operatorName;
    private String operatorDetails;
    private String operatorProperties;
    private String executionEngine;
    private String databaseName;
    private String schemaName;
    private String tableName;
    private String tableType;
    private Double tableSize;
    private Double outputSize;
    private Double subtreeCost;
    private int operatorId;
    private int parentOperatorId;
    // private int level;
    // private int position;
    private String host;
    private int port;
    private Timestamp timestamp;

    public HANAPlanNode(ResultSet dbResult) {
         // trim tree style indentation in operatorName, as dbeaver creates a real tree
        this.operatorName = JDBCUtils.safeGetStringTrimmed(dbResult, "OPERATOR_NAME");
        this.operatorDetails = JDBCUtils.safeGetString(dbResult, "OPERATOR_DETAILS");
        this.operatorProperties = JDBCUtils.safeGetString(dbResult, "OPERATOR_PROPERTIES");
        this.executionEngine = JDBCUtils.safeGetString(dbResult, "EXECUTION_ENGINE");
        this.databaseName = JDBCUtils.safeGetString(dbResult, "DATABASE_NAME");
        this.schemaName = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
        this.tableName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        this.tableType = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE");
        this.tableSize = JDBCUtils.safeGetDouble(dbResult, "TABLE_SIZE");
        this.outputSize = JDBCUtils.safeGetDouble(dbResult, "OUTPUT_SIZE");
        this.subtreeCost = JDBCUtils.safeGetDouble(dbResult, "SUBTREE_COST");
        this.operatorId = JDBCUtils.safeGetInt(dbResult, "OPERATOR_ID");
        this.parentOperatorId = JDBCUtils.safeGetInt(dbResult, "PARENT_OPERATOR_ID");
        // this.level = JDBCUtils.safeGetInt(dbResult, "LEVEL");
        // this.position = JDBCUtils.safeGetInt(dbResult, "POSITION");
        this.host = JDBCUtils.safeGetString(dbResult, "HOST");
        this.port = JDBCUtils.safeGetInt(dbResult, "PORT");
        this.timestamp = JDBCUtils.safeGetTimestamp(dbResult, "TIMESTAMP");
    }

    public void addNested(HANAPlanNode node) {
        nestedNodes.add(node);
    }

    public void setParent(HANAPlanNode node) {
        parentNode = node;
    }

    @Override
    public DBCPlanNode getParent() {
        return parentNode;
    }

    @Override
    public Collection<HANAPlanNode> getNested() {
        return nestedNodes;
    }

    @Override
    public String getNodeName() {
        return operatorName;
    }

    @Override
    public String getNodeType() {
        return null;
    }

    @Override
    public String getNodeDescription() {
        return operatorDetails;
    }

    public int getOperatorId() {
        return operatorId;
    }

    public int getParentOperatorId() {
        return parentOperatorId;
    }

    @Property(order = 0, viewable = true)
    public String getOperatorName() {
        return operatorName;
    }

    @Property(order = 1, viewable = true)
    public String getOperatorDetails() {
        return operatorDetails;
    }

    @Property(order = 2, viewable = true)
    public String getOperatorProperties() {
        return operatorProperties;
    }

    @Property(order = 3, viewable = true)
    public String getExecutionEngine() {
        return executionEngine;
    }

    @Property(order = 4, viewable = true)
    public String getDatabaseName() {
        return databaseName;
    }

    @Property(order = 5, viewable = true)
    public String getSchemaName() {
        return schemaName;
    }

    @Property(order = 6, viewable = true)
    public String getTableName() {
        return tableName;
    }

    @Property(order = 7, viewable = true)
    public String getTableType() {
        return tableType;
    }

    @Property(order = 8, viewable = true)
    public Double getTableSize() {
        return tableSize;
    }

    @Property(order = 9, viewable = true)
    public Double getOutputSize() {
        return outputSize;
    }

    @Property(order = 10, viewable = true)
    public Double getSubtreeCost() {
        return subtreeCost;
    }

    @Property(order = 11, viewable = true)
    public String getHost() {
        return host;
    }

    @Property(order = 12, viewable = true)
    public int getPort() {
        return port;
    }

    @Property(order = 13, viewable = true)
    public Timestamp getTimestamp() {
        return timestamp;
    }
}
