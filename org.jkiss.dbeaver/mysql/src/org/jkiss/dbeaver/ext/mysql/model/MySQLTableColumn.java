package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.meta.AbstractColumn;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.sql.ResultSet;

/**
 * GenericTable
 */
public class MySQLTableColumn extends AbstractColumn implements DBSTableColumn
{
    private MySQLTable table;
    private String defaultValue;
    private int charLength;

    public MySQLTableColumn(
        MySQLTable table,
        ResultSet dbResult)
        throws DBException
    {
        this.table = table;
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws DBException
    {
        setName(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ORDINAL_POSITION));
        String typeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DATA_TYPE);
        setTypeName(typeName);
        DBSDataType dataType = getDataSource().getInfo().getSupportedDataType(typeName.toUpperCase());
        this.charLength = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH);
        if (this.charLength <= 0) {
            if (dataType != null) {
                setMaxLength(dataType.getPrecision());
            }
        } else {
            setMaxLength(this.charLength);
        }
        setDescription(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_COMMENT));
        setNullable("YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_IS_NULLABLE)));
        setScale(JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NUMERIC_SCALE));
        setPrecision(JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NUMERIC_PRECISION));
        this.defaultValue = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_DEFAULT);
    }

    public DBSObject getParentObject()
    {
        return getTable();
    }

    public MySQLDataSource getDataSource()
    {
        return table.getDataSource();
    }

    public MySQLTable getTable()
    {
        return table;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setOrdinalPosition(int ordinalPosition)
    {
        this.ordinalPosition = ordinalPosition;
    }

    public int getCharLength()
    {
        return charLength;
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }

}
