/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * OracleProcedureArgument
 */
public class OracleProcedureArgument implements DBSProcedureColumn
{
    private final OracleProcedureBase procedure;
    private String name;
    private int position;
    private int dataLevel;
    private int sequence;
    private OracleParameterMode mode;
    private OracleDataType type;
    private OracleDataType dataType;
    private int dataLength;
    private int dataScale;
    private int dataPrecision;
    private List<OracleProcedureArgument> attributes;

    public OracleProcedureArgument(
        DBRProgressMonitor monitor,
        OracleProcedureBase procedure,
        ResultSet dbResult)
    {
        this.procedure = procedure;
        this.name = JDBCUtils.safeGetString(dbResult, "ARGUMENT_NAME");
        this.position = JDBCUtils.safeGetInt(dbResult, "POSITION");
        this.dataLevel = JDBCUtils.safeGetInt(dbResult, "DATA_LEVEL");
        this.sequence = JDBCUtils.safeGetInt(dbResult, "SEQUENCE");
        this.mode = OracleParameterMode.getMode(JDBCUtils.safeGetString(dbResult, "IN_OUT"));
        this.type = OracleDataType.resolveDataType(
            monitor,
            procedure.getDataSource(),
            null,
            JDBCUtils.safeGetString(dbResult, "DATA_TYPE"));
        final String dataTypeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
        if (!CommonUtils.isEmpty(dataTypeName)) {
            this.dataType = OracleDataType.resolveDataType(
                monitor,
                procedure.getDataSource(),
                JDBCUtils.safeGetString(dbResult, "TYPE_OWNER"),
                dataTypeName);
        }
        this.dataLength = JDBCUtils.safeGetInt(dbResult, "DATA_LENGTH");
        this.dataScale = JDBCUtils.safeGetInt(dbResult, "DATA_SCALE");
        this.dataPrecision = JDBCUtils.safeGetInt(dbResult, "DATA_PRECISION");
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return getProcedure();
    }

    public OracleDataSource getDataSource()
    {
        return procedure.getDataSource();
    }

    public OracleProcedureBase getProcedure()
    {
        return procedure;
    }

    public boolean isPersisted()
    {
        return true;
    }

    @Property(name = "Name", viewable = true, order = 10)
    public String getName()
    {
        if (CommonUtils.isEmpty(name)) {
            if (dataLevel == 0) {
                // Function result
                return "RESULT";
            } else {
                // Collection element
                return "ELEMENT";
            }
        }
        return name;
    }

    @Property(name = "Position", viewable = true, order = 11)
    public int getPosition()
    {
        return position;
    }

    @Property(name = "In/Out", viewable = true, order = 20)
    public DBSProcedureColumnType getColumnType()
    {
        return mode == null ? null : mode.getColumnType();
    }

    @Property(name = "Type", viewable = true, order = 21)
    public OracleDataType getType()
    {
        return dataType == null ? type : dataType;
    }

    public boolean isNotNull()
    {
        return false;
    }

    @Property(name = "Length", viewable = true, order = 30)
    public long getMaxLength()
    {
        return dataLength;
    }

    public String getTypeName()
    {
        return type.getName();
    }

    public int getValueType()
    {
        return type.getValueType();
    }

    @Property(name = "Scale", viewable = true, order = 40)
    public int getScale()
    {
        return dataScale;
    }

    @Property(name = "Precision", viewable = true, order = 50)
    public int getPrecision()
    {
        return dataPrecision;
    }

    public int getDataLevel()
    {
        return dataLevel;
    }

    public int getSequence()
    {
        return sequence;
    }

    @Association
    public List<OracleProcedureArgument> getAttributes()
    {
        return attributes;
    }

    void addAttribute(OracleProcedureArgument attribute)
    {
        if (attributes == null) {
            attributes = new ArrayList<OracleProcedureArgument>();
        }
        attributes.add(attribute);
    }

    public boolean hasAttributes()
    {
        return !CommonUtils.isEmpty(attributes);
    }
}
