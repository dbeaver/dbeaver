/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Karl Griesser
 */
public class ExasolPlanNode extends AbstractExecutionPlanNode {

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

    public ExasolPlanNode(ExasolPlanNode parent, ResultSet dbResult) {
        this.parent = parent;
        this.stmtId = JDBCUtils.safeGetInt(dbResult, "STMT_ID");
        this.commandName = JDBCUtils.safeGetString(dbResult, "COMMAND_NAME");
        this.commandClass = JDBCUtils.safeGetString(dbResult, "COMMAND_CLASS");
        this.partId = JDBCUtils.safeGetInt(dbResult, "PART_ID");
        this.partName = JDBCUtils.safeGetString(dbResult, "PART_NAME");
        this.partInfo = JDBCUtils.safeGetString(dbResult, "PART_INFO");
        this.objectSchema = JDBCUtils.safeGetString(dbResult, "OBJECT_SCHEMA");
        this.objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.objectRows = JDBCUtils.safeGetDouble(dbResult, "OBJECT_ROWS");
        this.outRows = JDBCUtils.safeGetDouble(dbResult, "OUT_ROWS");
        this.duration = JDBCUtils.safeGetDouble(dbResult, "DURATION");
        this.cpu = JDBCUtils.safeGetDouble(dbResult, "CPU");
        this.tempDbRamPeak = JDBCUtils.safeGetDouble(dbResult, "TEMP_DB_RAM_PEAK");
        this.hddRead = JDBCUtils.safeGetDouble(dbResult, "HDD_READ");
        this.hddWrite = JDBCUtils.safeGetDouble(dbResult, "HDD_WRITE");
        this.netTransfer = JDBCUtils.safeGetDouble(dbResult, "NET");
        this.detailInfo = JDBCUtils.safeGetString(dbResult, "REMARKS");


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


}
