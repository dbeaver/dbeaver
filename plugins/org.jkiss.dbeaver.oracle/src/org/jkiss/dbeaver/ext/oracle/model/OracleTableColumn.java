/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.OracleUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.sql.ResultSet;

/**
 * OracleTableColumn
 */
public class OracleTableColumn extends JDBCTableColumn<OracleTableBase> implements DBSTableColumn
{
    static final Log log = LogFactory.getLog(OracleTableColumn.class);

    private DBSDataType type;
    private OracleDataTypeModifier typeMod;
    private String comment;
    private String defaultValue;
    private long charLength;

    public OracleTableColumn(OracleTableBase table)
    {
        super(table, false);
    }

    public OracleTableColumn(
        DBRProgressMonitor monitor,
        OracleTableBase table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, true);

        this.name = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COLUMN_NAME);
        this.ordinalPosition = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_COLUMN_ID);
        this.typeName = JDBCUtils.safeGetString(dbResult, "DATA_TYPE");
        this.type = OracleUtils.resolveDataType(
            monitor,
            getDataSource(),
            JDBCUtils.safeGetString(dbResult, "DATA_TYPE_OWNER"),
            this.typeName);
        this.typeMod = OracleUtils.resolveTypeModifier(JDBCUtils.safeGetString(dbResult, "DATA_TYPE_MOD"));
        if (OracleConstants.TYPE_NUMBER.equals(typeName)) {
            // Handle all numbers as decimals
            setValueType(java.sql.Types.DECIMAL);
        } else if (type != null) {
            setValueType(type.getValueType());
        }
        this.charLength = JDBCUtils.safeGetLong(dbResult, OracleConstants.COL_DATA_LENGTH);
        this.notNull = !"Y".equals(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_NULLABLE));
        this.scale = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_DATA_SCALE);
        this.precision = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_DATA_PRECISION);
        this.defaultValue = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DATA_DEFAULT);
        this.comment = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COMMENTS);
    }

    public DBSObject getParentObject()
    {
        return getTable();
    }

    public OracleDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    public long getCharLength()
    {
        return charLength;
    }

    @Property(name = "Data Type", viewable = true, editable = true, updatable = true, order = 20)
    public DBSDataType getType()
    {
        return type;
    }

    //@Property(name = "Data Type", viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    @Property(name = "Length", viewable = true, editable = true, updatable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Property(name = "Not Null", viewable = true, editable = true, updatable = true, order = 50)
    @Override
    public boolean isNotNull()
    {
        return super.isNotNull();
    }

    @Property(name = "Default", viewable = true, editable = true, updatable = true, order = 70)
    public String getDefaultValue()
    {
        return defaultValue;
    }

    public boolean isAutoIncrement()
    {
        return false;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Property(name = "Comment", viewable = true, editable = true, updatable = true, order = 100)
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

}
