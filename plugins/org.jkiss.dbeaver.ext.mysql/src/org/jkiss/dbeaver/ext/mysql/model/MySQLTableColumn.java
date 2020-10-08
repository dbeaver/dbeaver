/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPOrderedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt3;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQLTableColumn
 */
public class MySQLTableColumn extends JDBCTableColumn<MySQLTableBase> implements DBSTableColumn, DBSTypedObjectExt3, DBPNamedObject2, DBPOrderedObject
{
    private static final Log log = Log.getLog(MySQLTableColumn.class);

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
    private MySQLCollation collation;
    private KeyType keyType;
    private String extraInfo;
    private String genExpression;

    private String fullTypeName;
    private List<String> enumValues;

    public MySQLTableColumn(MySQLTableBase table)
    {
        super(table, false);
    }

    public MySQLTableColumn(
        MySQLTableBase table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, true);
        loadInfo(dbResult);
    }

    // Copy constructor
    public MySQLTableColumn(
        DBRProgressMonitor monitor, MySQLTableBase table,
        DBSEntityAttribute source)
        throws DBException
    {
        super(table, source, false);
        this.comment = source.getDescription();
        if (source instanceof MySQLTableColumn) {
            MySQLTableColumn mySource = (MySQLTableColumn)source;
            this.charLength = mySource.charLength;
            this.collation = mySource.collation;
            this.keyType = mySource.keyType;
            this.extraInfo = mySource.extraInfo;
            this.genExpression = mySource.genExpression;
            this.fullTypeName = mySource.fullTypeName;
            if (mySource.enumValues != null) {
                this.enumValues = new ArrayList<>(mySource.enumValues);
            }
        } else {
            this.collation = table.getContainer().getAdditionalInfo(monitor).getDefaultCollation();
            this.fullTypeName = DBUtils.getFullTypeName(this);
        }
    }

    private void loadInfo(ResultSet dbResult)
        throws DBException
    {
        name = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
        ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ORDINAL_POSITION);
        String typeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DATA_TYPE);
        assert typeName != null;
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
        DBSDataType dataType = getDataSource().getLocalDataType(typeName);
        this.charLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_CHARACTER_MAXIMUM_LENGTH);
        if (this.charLength <= 0) {
            if (dataType != null) {
                setMaxLength(CommonUtils.toInt(dataType.getPrecision()));
            }
        } else {
            setMaxLength(this.charLength);
        }
        this.comment = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_COMMENT);
        this.required = !"YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_IS_NULLABLE));
        this.setScale(JDBCUtils.safeGetInteger(dbResult, MySQLConstants.COL_NUMERIC_SCALE));
        this.setPrecision(JDBCUtils.safeGetInteger(dbResult, MySQLConstants.COL_NUMERIC_PRECISION));
        String defaultValue = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_DEFAULT);
        if (defaultValue != null) {
            switch (getDataKind()) {
                case STRING:
                    // Escape if it is not NULL (#1913)
                    // Although I didn't reproduce that locally - perhaps depends on server config.
                    if (!SQLConstants.NULL_VALUE.equals(defaultValue) && !SQLUtils.isStringQuoted(defaultValue)) {
                        defaultValue = SQLUtils.quoteString(getDataSource(), defaultValue);
                    }
                    break;
                case DATETIME:
                    if (!defaultValue.isEmpty() && Character.isDigit(defaultValue.charAt(0))) {
                        defaultValue = "'" + defaultValue + "'";
                    }
                    break;

            }
            setDefaultValue(defaultValue);
        }
        this.collation = getDataSource().getCollation(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLLATION_NAME));

        this.extraInfo = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_EXTRA);
        this.autoGenerated = extraInfo != null && extraInfo.contains(MySQLConstants.EXTRA_AUTO_INCREMENT);

        this.fullTypeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_TYPE);
        if (!CommonUtils.isEmpty(fullTypeName) && (isTypeEnum() || isTypeSet())) {
            enumValues = parseEnumValues(fullTypeName);
        }

        if (!getDataSource().isMariaDB() && getDataSource().isServerVersionAtLeast(5, 7)) {
            genExpression = JDBCUtils.safeGetString(dbResult, "GENERATION_EXPRESSION");
        }
    }

    private static List<String> parseEnumValues(String typeName) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        int pos = 0;
        while (true) {
            int startPos = typeName.indexOf('\'', pos);
            if (startPos < 0) {
                break;
            }
            int endPos = -1;
            for (int i = startPos + 1; i < typeName.length(); i++) {
                char c = typeName.charAt(i);
                if (c == '\'') {
                    if (i < typeName.length() - 2 && typeName.charAt(i + 1) == '\'') {
                        // Quote escape
                        value.append(c);
                        i++;
                        continue;
                    }
                    endPos = i;
                    break;
                }
                value.append(c);
            }
            if (endPos < 0) {
                break;
            }
            values.add(value.toString());
            pos = endPos + 1;
            value.setLength(0);
        }
        return values;
    }

    @NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    public String getFullTypeName() {
        return fullTypeName;
    }

    @Override
    public void setFullTypeName(String fullTypeName) throws DBException {
        super.setFullTypeName(fullTypeName);
        this.fullTypeName = fullTypeName;
    }

    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    public boolean isTypeSet() {
        return typeName.equalsIgnoreCase(MySQLConstants.TYPE_NAME_SET);
    }

    public boolean isTypeEnum() {
        return typeName.equalsIgnoreCase(MySQLConstants.TYPE_NAME_ENUM);
    }

    //@Property(viewable = true, editable = true, updatable = true, order = 40)
    @Override
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    //@Property(viewable = true, order = 41)
    public Integer getScale()
    {
        return super.getScale();
    }

    @Override
    //@Property(viewable = true, order = 42)
    public Integer getPrecision()
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
    public void setAutoGenerated(boolean autoGenerated) {
        if (autoGenerated) {
            extraInfo = (CommonUtils.notEmpty(extraInfo) + " " + MySQLConstants.EXTRA_AUTO_INCREMENT).trim();
        } else {
            if (extraInfo != null) {
                extraInfo = extraInfo.replace(MySQLConstants.EXTRA_AUTO_INCREMENT, " ").trim();
            }
        }
        super.setAutoGenerated(autoGenerated);
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

    @Property(viewable = true, editable = true, updatable = true, order = 72)
    public String getGenExpression() {
        return genExpression;
    }

    public void setGenExpression(String genExpression) {
        this.genExpression = genExpression;
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

    @Property(viewable = false, editable = true, updatable = true, listProvider = CharsetListProvider.class, order = 81)
    public MySQLCharset getCharset()
    {
        return collation == null ? null : collation.getCharset();
    }

    public void setCharset(MySQLCharset charset)
    {
        this.collation = charset == null ? null : charset.getDefaultCollation();
    }

    @Property(viewable = false, editable = true, updatable = true, listProvider = CollationListProvider.class, order = 82)
    public MySQLCollation getCollation()
    {
        return collation;
    }

    public void setCollation(MySQLCollation collation)
    {
        this.collation = collation;
    }

    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
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

    public static class CharsetListProvider implements IPropertyValueListProvider<MySQLTableColumn> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(MySQLTableColumn object)
        {
            return object.getDataSource().getCharsets().toArray();
        }
    }

    public static class CollationListProvider implements IPropertyValueListProvider<MySQLTableColumn> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(MySQLTableColumn object)
        {
            if (object.getCharset() == null) {
                return new Object[0];
            } else {
                return object.getCharset().getCollations().toArray();
            }
        }
    }
}
