/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.DBPositiveNumberTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.utils.CommonUtils;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * PostgreAttribute
 */
public abstract class PostgreAttribute<OWNER extends DBSEntity & PostgreObject> extends JDBCTableColumn<OWNER>
    implements PostgreObject, DBSTypedObjectEx, DBPNamedObject2, DBPHiddenObject, DBPInheritedObject
{
    private static final Log log = Log.getLog(PostgreAttribute.class);

    @NotNull
    private PostgreDataType dataType;
    private String comment;
    private long charLength;
    private int arrayDim;
    private int inheritorsCount;
    private String description;
    @Nullable
    private PostgreAttributeIdentity identity;
    private boolean isLocal;
    private long objectId;
    private Object acl;

    protected PostgreAttribute(
        OWNER table)
    {
        super(table, false);
    }

    public PostgreAttribute(
        DBRProgressMonitor monitor, OWNER table,
        JDBCResultSet dbResult)
        throws DBException
    {
        super(table, true);
        loadInfo(monitor, dbResult);
    }

    @Override
    public PostgreDatabase getDatabase() {
        return getTable().getDatabase();
    }

    @Property(viewable = false, order = 26)
    @Override
    public long getObjectId() {
        return objectId;
    }

    private void loadInfo(DBRProgressMonitor monitor, JDBCResultSet dbResult)
        throws DBException
    {
        setName(JDBCUtils.safeGetString(dbResult, "attname"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "attnum"));
        setRequired(JDBCUtils.safeGetBoolean(dbResult, "attnotnull"));
        objectId = JDBCUtils.safeGetLong(dbResult, "attr_id");
        final long typeId = JDBCUtils.safeGetLong(dbResult, "atttypid");
        dataType = getTable().getDatabase().getDataType(monitor, typeId);
        if (dataType == null) {
            log.error("Attribute data type '" + typeId + "' not found. Use " + PostgreConstants.TYPE_VARCHAR);
            dataType = getTable().getDatabase().getDataType(monitor, PostgreConstants.TYPE_VARCHAR);
        } else {
            // TODO: [#2824] Perhaps we should just use type names declared in pg_catalog
            // Replacing them with "convenient" types names migh cause some issues
            if (false && dataType.getCanonicalName() != null && getDataSource().isServerVersionAtLeast(9, 6)) {
                // se canonical type names. But only for PG >= 9.6 (because I can't test with earlier versions)
                PostgreDataType canonicalType = getTable().getDatabase().getDataType(monitor, dataType.getCanonicalName());
                if (canonicalType != null) {
                    this.dataType = canonicalType;
                }
            }
        }
        //setTypeName(dataType.getTypeName());
        setValueType(dataType.getTypeID());
        setDefaultValue(JDBCUtils.safeGetString(dbResult, "def_value"));
        int typeMod = JDBCUtils.safeGetInt(dbResult, "atttypmod");
        int maxLength = PostgreUtils.getAttributePrecision(typeId, typeMod);
        DBPDataKind dataKind = dataType.getDataKind();
        if (dataKind == DBPDataKind.NUMERIC || dataKind == DBPDataKind.DATETIME) {
            setMaxLength(0);
        } else {
            if (maxLength <= 0) {
                maxLength = PostgreUtils.getDisplaySize(typeId, typeMod);
            }
            if (maxLength >= 0) {
                setMaxLength(maxLength);
            } else {
                // TypeMod can be anything.
                // It is often used in packed format and has no numeric meaning at all
                //setMaxLength(typeMod);
            }
        }
        setPrecision(maxLength);
        setScale(PostgreUtils.getScale(typeId, typeMod));
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.arrayDim = JDBCUtils.safeGetInt(dbResult, "attndims");
        this.inheritorsCount = JDBCUtils.safeGetInt(dbResult, "attinhcount");
        this.isLocal =
            !getDataSource().getServerType().supportsInheritance() ||
            JDBCUtils.safeGetBoolean(dbResult, "attislocal", true);

        if (getDataSource().isServerVersionAtLeast(10, 0)) {
            String identityStr = JDBCUtils.safeGetString(dbResult, "attidentity");
            if (!CommonUtils.isEmpty(identityStr)) {
                identity = PostgreAttributeIdentity.getByCode(identityStr);
            }
        }

        this.acl = JDBCUtils.safeGetObject(dbResult, "attacl");

        setPersisted(true);
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    public Object getAcl() {
        return acl;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = DataTypeListProvider.class, valueTransformer = DataTypeValueTransformer.class)
    public PostgreDataType getDataType() {
        return dataType;
    }

    public void setDataType(@NotNull PostgreDataType dataType) {
        this.dataType = dataType;
        setTypeName(dataType.getTypeName());
        setValueType(dataType.getTypeID());
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataType.getDataKind();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 21)
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    public String getTypeName()
    {
        return dataType.getTypeName();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 22)
    public Integer getPrecision()
    {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 23)
    public Integer getScale()
    {
        return super.getScale();
    }

    @Nullable
    @Property(viewable = true, editable = true, order = 24)
    public PostgreAttributeIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(PostgreAttributeIdentity identity) {
        this.identity = identity;
    }

    @Property(order = 25)
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 50)
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Override
    public boolean isAutoGenerated()
    {
        if (identity != null) {
            return true;
        }
        // Also check sequence in def value
        final String def = getDefaultValue();
        return def != null && def.contains("nextval(");
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 70)
    public String getDefaultValue()
    {
        return super.getDefaultValue();
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isHidden() {
        return isPersisted() && getOrdinalPosition() < 0;
    }

    @Override
    public boolean isInherited() {
        return !isLocal;
    }

    public String getFullTypeName() {
        String fqtn = dataType.getTypeName();
        if (dataType.getDataKind() != DBPDataKind.CONTENT) {
            return DBUtils.getFullTypeName(this);
        }
        return fqtn;
    }

    public static class DataTypeListProvider implements IPropertyValueListProvider<PostgreAttribute> {

        @Override
        public boolean allowCustomValue()
        {
            return true;
        }

        @Override
        public Object[] getPossibleValues(PostgreAttribute column)
        {
            Set<PostgreDataType> types = new TreeSet<>(Comparator.comparing(JDBCDataType::getTypeName));
            types.addAll(column.getDataSource().getLocalDataTypes());
            return types.toArray(new PostgreDataType[types.size()]);
        }
    }

    public static class DataTypeValueTransformer implements IPropertyValueTransformer<PostgreAttribute, Object> {
        @Override
        public PostgreDataType transform(PostgreAttribute object, Object value) {
            if (value instanceof String) {
                PostgreDataType dataType = object.getDatabase().getDataType(new VoidProgressMonitor(), (String)value);
                if (dataType == null) {
                    throw new IllegalArgumentException("Bad data type name specified: " + value);
                }
                return dataType;
            } else if (value instanceof PostgreDataType) {
                return (PostgreDataType) value;
            } else {
                throw new IllegalArgumentException("Invalid type value: " + value);
            }
        }
    }
}
