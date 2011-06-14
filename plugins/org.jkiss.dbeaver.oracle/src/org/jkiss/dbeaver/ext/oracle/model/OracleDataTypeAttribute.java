/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.OracleUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

/**
 * Oracle data type attribute
 */
public class OracleDataTypeAttribute implements DBSObject {

    static final Log log = LogFactory.getLog(OracleDataTypeAttribute.class);

    private OracleDataType dataType;
    private int attrNo;
    private String attrName;
    private DBSDataType attrType;
    private OracleDataTypeModifier attrTypeMod;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private boolean inherited;
    private boolean persisted;

    public OracleDataTypeAttribute(OracleDataType dataType)
    {
        this.dataType = dataType;
        this.persisted = false;
    }

    public OracleDataTypeAttribute(DBRProgressMonitor monitor, OracleDataType dataType, ResultSet dbResult)
    {
        this.dataType = dataType;
        this.attrName = JDBCUtils.safeGetString(dbResult, "ATTR_NAME");
        this.attrNo = JDBCUtils.safeGetInt(dbResult, "ATTR_NO");
        this.attrType = OracleUtils.resolveDataType(
            monitor,
            getDataSource(),
            JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_OWNER"),
            JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_NAME"));
        this.attrTypeMod = OracleUtils.resolveTypeModifier(JDBCUtils.safeGetString(dbResult, "ATTR_TYPE_MOD"));
        this.length = JDBCUtils.safeGetInteger(dbResult, "LENGTH");
        this.precision = JDBCUtils.safeGetInteger(dbResult, "PRECISION");
        this.scale = JDBCUtils.safeGetInteger(dbResult, "SCALE");
        this.inherited = JDBCUtils.safeGetBoolean(dbResult, "SCALE", OracleConstants.YES);
        this.persisted = true;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return dataType;
    }

    public OracleDataSource getDataSource()
    {
        return dataType.getDataSource();
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    @Property(name = "Name", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return attrName;
    }

    @Property(name = "Number", viewable = true, order = 2)
    public int getAttrNo()
    {
        return attrNo;
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

    @Property(name = "Inherited", viewable = true, order = 8)
    public boolean isInherited()
    {
        return inherited;
    }
}
