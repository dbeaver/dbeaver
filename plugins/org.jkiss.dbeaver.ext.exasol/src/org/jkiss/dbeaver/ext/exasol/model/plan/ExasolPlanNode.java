/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Karl Griesser
 */
public class ExasolPlanNode extends AbstractExecutionPlanNode {

    private static final String ATTR_STMT_ID = "STMT_ID";
    private static final String ATTR_COMMAND_NAME = "COMMAND_NAME";
    private static final String ATTR_COMMAND_CLASS = "COMMAND_CLASS";
    private static final String ATTR_PART_ID = "PART_ID";
    private static final String ATTR_PART_NAME = "PART_NAME";
    private static final String ATTR_PART_INFO = "PART_INFO";
    private static final String ATTR_OBJECT_SCHEMA = "OBJECT_SCHEMA";
    private static final String ATTR_OBJECT_NAME = "OBJECT_NAME";
    private static final String ATTR_OBJECT_ROWS = "OBJECT_ROWS";
    private static final String ATTR_OUT_ROWS = "OUT_ROWS";
    private static final String ATTR_DURATION = "DURATION";
    private static final String ATTR_CPU = "CPU";
    private static final String ATTR_TEMP_DB_RAM_PEAK = "TEMP_DB_RAM_PEAK";
    private static final String ATTR_HDD_READ = "HDD_READ";
    private static final String ATTR_HDD_WRITE = "HDD_WRITE";
    private static final String ATTR_NET = "NET";
    private static final String ATTR_REMARKS = "REMARKS";

    private ExasolPlanNode parent;
    private Collection<ExasolPlanNode> listNestedNodes = new ArrayList<>(64);

    private int stmtId;
    private String commandName;
    private String commandClass;
    private int partId;
    private String partName;
    private String partInfo;
    private String objectSchema;
    private String objectName;
    private Double objectRows;
    private Double outRows;
    private Double duration;
    private Double cpu;
    private Double tempDbRamPeak;
    private Double hddRead;
    private Double hddWrite;
    private Double netTransfer;
    private String detailInfo;

    public Collection<ExasolPlanNode> getListNestedNodes() {
        return listNestedNodes;
    }

    private Map<String, Object> attributes;

    public ExasolPlanNode(ExasolPlanNode parent, ResultSet dbResult) {
        this.parent = parent;
        this.stmtId = JDBCUtils.safeGetInt(dbResult, ATTR_STMT_ID);
        this.commandName = JDBCUtils.safeGetString(dbResult, ATTR_COMMAND_NAME);
        this.commandClass = JDBCUtils.safeGetString(dbResult, ATTR_COMMAND_CLASS);
        this.partId = JDBCUtils.safeGetInt(dbResult, ATTR_PART_ID);
        this.partName = JDBCUtils.safeGetString(dbResult, ATTR_PART_NAME);
        this.partInfo = JDBCUtils.safeGetString(dbResult, ATTR_PART_INFO);
        this.objectSchema = JDBCUtils.safeGetString(dbResult, ATTR_OBJECT_SCHEMA);
        this.objectName = JDBCUtils.safeGetString(dbResult, ATTR_OBJECT_NAME);
        this.objectRows = JDBCUtils.safeGetDouble(dbResult, ATTR_OBJECT_ROWS);
        this.outRows = JDBCUtils.safeGetDouble(dbResult, ATTR_OUT_ROWS);
        this.duration = JDBCUtils.safeGetDouble(dbResult, ATTR_DURATION);
        this.cpu = JDBCUtils.safeGetDouble(dbResult, ATTR_CPU);
        this.tempDbRamPeak = JDBCUtils.safeGetDouble(dbResult, ATTR_TEMP_DB_RAM_PEAK);
        this.hddRead = JDBCUtils.safeGetDouble(dbResult, ATTR_HDD_READ);
        this.hddWrite = JDBCUtils.safeGetDouble(dbResult, ATTR_HDD_WRITE);
        this.netTransfer = JDBCUtils.safeGetDouble(dbResult, ATTR_NET);
        this.detailInfo = JDBCUtils.safeGetString(dbResult, ATTR_REMARKS);

        fillAttributes();
    }

    public ExasolPlanNode(ExasolPlanNode parent, Map<String, Object> attributes) {
        this.parent = parent;

        this.stmtId = JSONUtils.getInteger(attributes, ATTR_STMT_ID);
        this.commandName = JSONUtils.getString(attributes, ATTR_COMMAND_NAME);
        this.commandClass = JSONUtils.getString(attributes, ATTR_COMMAND_CLASS);
        this.partId = JSONUtils.getInteger(attributes, ATTR_PART_ID);
        this.partName = JSONUtils.getString(attributes, ATTR_PART_NAME);
        this.partInfo = JSONUtils.getString(attributes, ATTR_PART_INFO);
        this.objectSchema = JSONUtils.getString(attributes, ATTR_OBJECT_SCHEMA);
        this.objectName = JSONUtils.getString(attributes, ATTR_OBJECT_NAME);
        this.objectRows = JSONUtils.getDouble(attributes, ATTR_OBJECT_ROWS);
        this.outRows = JSONUtils.getDouble(attributes, ATTR_OUT_ROWS);
        this.duration = JSONUtils.getDouble(attributes, ATTR_DURATION);
        this.cpu = JSONUtils.getDouble(attributes, ATTR_CPU);
        this.tempDbRamPeak = JSONUtils.getDouble(attributes, ATTR_TEMP_DB_RAM_PEAK);
        this.hddRead = JSONUtils.getDouble(attributes, ATTR_HDD_READ);
        this.hddWrite = JSONUtils.getDouble(attributes, ATTR_HDD_WRITE);
        this.netTransfer = JSONUtils.getDouble(attributes, ATTR_NET);
        this.detailInfo = JSONUtils.getString(attributes, ATTR_REMARKS);

        this.attributes = attributes;
    }

    // ----------------------
    // Methods from Interface
    // ---------------------	
    @Override
    public DBCPlanNode getParent() {
        return parent;
    }

    @Override
    public Collection<ExasolPlanNode> getNested() {
        return listNestedNodes;
    }

    @Override
    public String getNodeName() {
        return objectName;
    }

    @Override
    public String getNodeType() {
        return commandName;
    }

    @Override
    public String getNodeDescription() {
        return detailInfo;
    }

    @Property(order = 0, viewable = true)
    public int getStmtId() {
        return stmtId;
    }


    @Property(order = 1, viewable = true)
    public String getCommandName() {
        return commandName;
    }


    @Property(order = 2, viewable = true)
    public String getCommandClass() {
        return commandClass;
    }


    @Property(order = 3, viewable = true)
    public int getPartId() {
        return partId;
    }


    @Property(order = 4, viewable = true)
    public String getPartName() {
        return partName;
    }


    @Property(order = 5, viewable = true)
    public String getPartInfo() {
        return partInfo;
    }


    @Property(order = 6, viewable = true)
    public String getObjectSchema() {
        return objectSchema;
    }


    @Property(order = 7, viewable = true)
    public String getObjectName() {
        return objectName;
    }


    @Property(order = 8, viewable = true)
    public Double getObjectRows() {
        return objectRows;
    }


    @Property(order = 9, viewable = true)
    public Double getOutRows() {
        return outRows;
    }


    @Property(order = 10, viewable = true)
    public Double getDuration() {
        return duration;
    }


    @Property(order = 11, viewable = true)
    public Double getCpu() {
        return cpu;
    }


    @Property(order = 12, viewable = true)
    public Double getTempDbRamPeak() {
        return tempDbRamPeak;
    }


    @Property(order = 13, viewable = true)
    public Double getHddRead() {
        return hddRead;
    }


    @Property(order = 14, viewable = true)
    public Double getHddWrite() {
        return hddWrite;
    }


    @Property(order = 15, viewable = true)
    public Double getNetTransfer() {
        return netTransfer;
    }


    @Property(order = 16, viewable = true)
    public String getDetailInfo() {
        return detailInfo;
    }

    private void fillAttributes() {
        attributes = new HashMap<>();
        attributes.put(ATTR_STMT_ID, stmtId);
        putNotNullStringInMap(ATTR_COMMAND_NAME, commandName);
        putNotNullStringInMap(ATTR_COMMAND_CLASS, commandClass);
        attributes.put(ATTR_PART_ID, partId);
        putNotNullStringInMap(ATTR_PART_NAME, partName);
        putNotNullStringInMap(ATTR_PART_INFO, partInfo);
        putNotNullStringInMap(ATTR_OBJECT_SCHEMA, objectSchema);
        putNotNullStringInMap(ATTR_OBJECT_NAME, objectName);
        attributes.put(ATTR_OBJECT_ROWS, objectRows);
        attributes.put(ATTR_OUT_ROWS, outRows);
        attributes.put(ATTR_DURATION, duration);
        attributes.put(ATTR_CPU, cpu);
        attributes.put(ATTR_TEMP_DB_RAM_PEAK, tempDbRamPeak);
        attributes.put(ATTR_HDD_READ, hddRead);
        attributes.put(ATTR_HDD_WRITE, hddWrite);
        attributes.put(ATTR_NET, netTransfer);
        putNotNullStringInMap(ATTR_REMARKS, detailInfo);
    }

    private void putNotNullStringInMap(@NotNull String key, @Nullable String object) {
        if (CommonUtils.isNotEmpty(object)) {
            attributes.put(key, object);
        }
    }

    Map<String, Object> getAttributes() {
        return attributes;
    }
}
