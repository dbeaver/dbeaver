/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.exasol.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Karl Griesser
 */
public class ExasolPlanNode implements DBCPlanNode {

    private ExasolPlanNode parent;
    private Collection<ExasolPlanNode> listNestedNodes = new ArrayList<>(64);

    public Collection<ExasolPlanNode> getListNestedNodes() {
        return listNestedNodes;
    }

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
