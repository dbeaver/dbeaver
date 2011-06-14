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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.sql.ResultSet;

/**
 * OracleTableColumn
 */
public class OracleTableColumn extends JDBCTableColumn<OracleTableBase> implements DBSTableColumn
{
    static final Log log = LogFactory.getLog(OracleTableColumn.class);

    private String comment;
    private String defaultValue;
    private long charLength;

    public OracleTableColumn(OracleTableBase table)
    {
        super(table, false);
    }

    public OracleTableColumn(
        OracleTableBase table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, true);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws DBException
    {
        setName(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COLUMN_NAME));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_COLUMN_ID));
        setTypeName(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DATA_TYPE));
        setValueType(OracleUtils.typeNameToValueType(typeName));
        this.charLength = JDBCUtils.safeGetLong(dbResult, OracleConstants.COL_DATA_LENGTH);
        setNotNull(!"Y".equals(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_NULLABLE)));
        setScale(JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_DATA_SCALE));
        setPrecision(JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_DATA_PRECISION));
        this.defaultValue = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DATA_DEFAULT);
        setComment(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COMMENTS));
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

    @Property(name = "Data Type", viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
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
