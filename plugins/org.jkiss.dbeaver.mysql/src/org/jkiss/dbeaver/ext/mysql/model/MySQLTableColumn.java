/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQLTableColumn
 */
public class MySQLTableColumn extends JDBCTableColumn<MySQLTable> implements DBSTableColumn
{
    static final Log log = LogFactory.getLog(MySQLTableColumn.class);

    private static Pattern enumPattern = Pattern.compile("'([^']*)'");

    public static enum KeyType implements JDBCColumnKeyType {
        PRI,
        UNI,
        MUL;

        public boolean isInUniqueKey()
        {
            return this == PRI || this == UNI;
        }

        public boolean isInReferenceKey()
        {
            return this == MUL;
        }
    }

    private String comment;
    private String defaultValue;
    private long charLength;
    private boolean autoIncrement;
    private MySQLCollation collation;
    private KeyType keyType;

    private List<String> enumValues;

    public MySQLTableColumn(MySQLTable table)
    {
        super(table, false);
    }

    public MySQLTableColumn(
        MySQLTable table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, true);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws DBException
    {
        setName(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ORDINAL_POSITION));
        String typeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DATA_TYPE);
        String keyTypeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_KEY);
        if (!CommonUtils.isEmpty(keyTypeName)) {
            try {
                keyType = KeyType.valueOf(keyTypeName);
            } catch (IllegalArgumentException e) {
                log.debug(e);
            }
        }
        setTypeName(typeName);
        setValueType(MySQLUtils.typeNameToValueType(typeName));
        DBSDataType dataType = getDataSource().getInfo().getSupportedDataType(typeName.toUpperCase());
        this.charLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH);
        if (this.charLength <= 0) {
            if (dataType != null) {
                setMaxLength(dataType.getPrecision());
            }
        } else {
            setMaxLength(this.charLength);
        }
        this.comment = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_COMMENT);
        setNotNull(!"YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_IS_NULLABLE)));
        setScale(JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NUMERIC_SCALE));
        setPrecision(JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NUMERIC_PRECISION));
        this.defaultValue = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_DEFAULT);
        this.collation = getDataSource().getCollation(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLLATION_NAME));

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
        return getTable().getDataSource();
    }

    public long getCharLength()
    {
        return charLength;
    }

    @Property(name = "Auto Increment", viewable = true, editable = true, order = 51)
    public boolean isAutoIncrement()
    {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement)
    {
        this.autoIncrement = autoIncrement;
    }

    @Property(name = "Default", viewable = true, editable = true, order = 70)
    public String getDefaultValue()
    {
        return defaultValue;
    }

    @Property(name = "Key", viewable = true, order = 80)
    public KeyType getKeyType()
    {
        return keyType;
    }

    public List<String> getEnumValues()
    {
        return enumValues;
    }

    @Property(name = "Charset", viewable = false, editable = true, listProvider = CharsetListProvider.class, order = 81)
    public MySQLCharset getCharset()
    {
        return collation == null ? null : collation.getCharset();
    }

    public void setCharset(MySQLCharset charset)
    {
        this.collation = charset == null ? null : charset.getDefaultCollation();
    }

    @Property(name = "Collation", viewable = false, editable = true, listProvider = CollationListProvider.class, order = 82)
    public MySQLCollation getCollation()
    {
        return collation;
    }

    public void setCollation(MySQLCollation collation)
    {
        this.collation = collation;
    }

    @Property(name = "Comment", viewable = true, editable = true, order = 100)
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public static class CharsetListProvider implements IPropertyValueListProvider<MySQLTableColumn> {
        public boolean allowCustomValue()
        {
            return false;
        }
        public Object[] getPossibleValues(MySQLTableColumn object)
        {
            return object.getDataSource().getCharsets().toArray();
        }
    }

    public static class CollationListProvider implements IPropertyValueListProvider<MySQLTableColumn> {
        public boolean allowCustomValue()
        {
            return false;
        }
        public Object[] getPossibleValues(MySQLTableColumn object)
        {
            if (object.getCharset() == null) {
                return null;
            } else {
                return object.getCharset().getCollations().toArray();
            }
        }
    }
}
