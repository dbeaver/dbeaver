/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.OracleUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.sql.ResultSet;

/**
 * Oracle data type attribute
 */
public class OracleDataTypeAttribute extends OracleDataTypeMember {

    private DBSDataType attrType;
    private OracleDataTypeModifier attrTypeMod;
    private Integer length;
    private Integer precision;
    private Integer scale;

    public OracleDataTypeAttribute(OracleDataType dataType)
    {
        super(dataType);
    }

    public OracleDataTypeAttribute(DBRProgressMonitor monitor, OracleDataType dataType, ResultSet dbResult)
    {
        super(dataType, dbResult);
        this.name = JDBCUtils.safeGetString(dbResult, "ATTR_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "ATTR_NO");
        this.attrType = OracleUtils.resolveDataType(
            monitor,
            getDataSource(),
            JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_OWNER"),
            JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_NAME"));
        this.attrTypeMod = OracleUtils.resolveTypeModifier(JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_MOD"));
        this.length = JDBCUtils.safeGetInteger(dbResult, "LENGTH");
        this.precision = JDBCUtils.safeGetInteger(dbResult, "PRECISION");
        this.scale = JDBCUtils.safeGetInteger(dbResult, "SCALE");
    }

    @Property(name = "Type", viewable = true, editable = true, order = 3)
    public DBSDataType getAttrType()
    {
        return attrType;
    }

    @Property(name = "Type Mod", viewable = true, editable = true, order = 4)
    public OracleDataTypeModifier getAttrTypeMod()
    {
        return attrTypeMod;
    }

    @Property(name = "Length", viewable = true, editable = true, order = 5)
    public Integer getLength()
    {
        return length;
    }

    @Property(name = "Precision", viewable = true, editable = true, order = 6)
    public Integer getPrecision()
    {
        return precision;
    }

    @Property(name = "Scale", viewable = true, editable = true, order = 7)
    public Integer getScale()
    {
        return scale;
    }

}
