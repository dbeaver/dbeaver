/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreTableColumn
 */
public class PostgreTableColumn extends JDBCTableColumn<PostgreTableBase> implements DBSTableColumn, DBPNamedObject2
{
    static final Log log = Log.getLog(PostgreTableColumn.class);

    private static Pattern enumPattern = Pattern.compile("'([^']*)'");

    public enum KeyType implements JDBCColumnKeyType {
        PRI,
        UNI,
        MUL;

        @Override
        public boolean isInUniqueKey()
        {
            return this == PRI || this == UNI;
        }

        @Override
        public boolean isInReferenceKey()
        {
            return this == MUL;
        }
    }

    private String comment;
    private long charLength;
    private KeyType keyType;
    private String extraInfo;
    private String fullTypeName;

    private List<String> enumValues;

    public PostgreTableColumn(PostgreTableBase table)
    {
        super(table, false);
    }

    public PostgreTableColumn(
        PostgreTableBase table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, true);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws DBException
    {
        setName(JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_COLUMN_NAME));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, PostgreConstants.COL_ORDINAL_POSITION));
        String typeName = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_DATA_TYPE);
        assert typeName != null;
        String keyTypeName = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_COLUMN_KEY);
        if (!CommonUtils.isEmpty(keyTypeName)) {
            try {
                keyType = KeyType.valueOf(keyTypeName);
            } catch (IllegalArgumentException e) {
                log.debug(e);
            }
        }
        setTypeName(typeName);
        //setValueType(PostgreUtils.typeNameToValueType(typeName));
        DBSDataType dataType = getDataSource().getDataType(typeName);
        this.charLength = JDBCUtils.safeGetLong(dbResult, PostgreConstants.COL_CHARACTER_MAXIMUM_LENGTH);
        if (this.charLength <= 0) {
            if (dataType != null) {
                setMaxLength(dataType.getPrecision());
            }
        } else {
            setMaxLength(this.charLength);
        }
        this.comment = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_COLUMN_COMMENT);
        setRequired(!"YES".equals(JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_IS_NULLABLE)));
        setScale(JDBCUtils.safeGetInt(dbResult, PostgreConstants.COL_NUMERIC_SCALE));
        setPrecision(JDBCUtils.safeGetInt(dbResult, PostgreConstants.COL_NUMERIC_PRECISION));
        String defaultValue = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_COLUMN_DEFAULT);
        if (defaultValue != null) {
            switch (getDataKind()) {
                case STRING:
                    defaultValue = "'" + defaultValue + "'";
                    break;
                case DATETIME:
                    if (!defaultValue.isEmpty() && Character.isDigit(defaultValue.charAt(0))) {
                        defaultValue = "'" + defaultValue + "'";
                    }
                    break;

            }
            setDefaultValue(defaultValue);
        }

        this.extraInfo = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_COLUMN_EXTRA);
        this.autoGenerated = extraInfo != null && extraInfo.contains(PostgreConstants.EXTRA_AUTO_INCREMENT);

        fullTypeName = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_COLUMN_TYPE);
        if (!CommonUtils.isEmpty(fullTypeName) && (isTypeEnum() || isTypeSet())) {
            enumValues = new ArrayList<>();
            Matcher enumMatcher = enumPattern.matcher(fullTypeName);
            while (enumMatcher.find()) {
                String enumStr = enumMatcher.group(1);
                enumValues.add(enumStr);
            }
        }
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    public String getFullTypeName() {
        return fullTypeName;
    }

    public void setFullTypeName(String fullTypeName) {
        this.fullTypeName = fullTypeName;
        super.setTypeName(fullTypeName.replace("([A-Za-z\\s]+).*", "$1").trim());
    }

    //@Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    public boolean isTypeSet() {
        return typeName.equalsIgnoreCase(PostgreConstants.TYPE_NAME_SET);
    }

    public boolean isTypeEnum() {
        return typeName.equalsIgnoreCase(PostgreConstants.TYPE_NAME_ENUM);
    }

    //@Property(viewable = true, editable = true, updatable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    //@Property(viewable = true, order = 41)
    public int getScale()
    {
        return super.getScale();
    }

    @Override
    //@Property(viewable = true, order = 42)
    public int getPrecision()
    {
        return super.getPrecision();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 50)
    @Override
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 51)
    public boolean isAutoGenerated()
    {
        return autoGenerated;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 70)
    public String getDefaultValue()
    {
        return super.getDefaultValue();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 71)
    public String getExtraInfo()
    {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    @Override
    @Property(viewable = true, order = 60)
    public KeyType getKeyType()
    {
        return keyType;
    }

    public List<String> getEnumValues()
    {
        return enumValues;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 100)
    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    @Nullable
    @Override
    public String getDescription() {
        return getComment();
    }

    public static class CharsetListProvider implements IPropertyValueListProvider<PostgreTableColumn> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(PostgreTableColumn object)
        {
            return object.getDataSource().getCharsets().toArray();
        }
    }

}
