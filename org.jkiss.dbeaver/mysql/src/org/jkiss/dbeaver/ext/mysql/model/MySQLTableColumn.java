/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericTable
 */
public class MySQLTableColumn extends JDBCColumn implements DBSTableColumn
{
    private static Pattern enumPattern = Pattern.compile("'([^']*)'");

    private MySQLTable table;
    private String defaultValue;
    private int charLength;
    private boolean autoIncrement;

    private List<String> enumValues;

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
        setValueType(MySQLUtils.typeNameToValueType(typeName));
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
        String extra = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_EXTRA);
        this.autoIncrement = extra != null && extra.contains(MySQLConstants.EXTRA_AUTO_INCREMENT);

        String typeDesc = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_TYPE);
        if (!CommonUtils.isEmpty(typeDesc) &&
            (typeName.equalsIgnoreCase(MySQLConstants.TYPE_NAME_ENUM) || typeName.equalsIgnoreCase(MySQLConstants.TYPE_NAME_SET)))
        {
            enumValues = new ArrayList<String>();
            Matcher enumMatcher = enumPattern.matcher(typeDesc);
            while (enumMatcher.find()) {
                String enumStr = enumMatcher.group(1);
                enumValues.add(enumStr);
            }
        }
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

    public int getCharLength()
    {
        return charLength;
    }

    @Property(name = "Auto Increment", viewable = true, order = 10)
    public boolean isAutoIncrement()
    {
        return autoIncrement;
    }

    public List<String> getEnumValues()
    {
        return enumValues;
    }
}
