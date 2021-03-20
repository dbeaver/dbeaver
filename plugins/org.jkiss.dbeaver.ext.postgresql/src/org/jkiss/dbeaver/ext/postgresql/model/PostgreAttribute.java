/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt4;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * PostgreAttribute
 */
public abstract class PostgreAttribute<OWNER extends DBSEntity & PostgreObject> extends JDBCTableColumn<OWNER>
    implements PostgreObject, DBSTypedObjectEx, DBPNamedObject2, DBPHiddenObject, DBPInheritedObject, DBSTypedObjectExt4<PostgreDataType>
{
    private static final Log log = Log.getLog(PostgreAttribute.class);

    @Nullable
    private PostgreDataType dataType;
    private String comment;
    private long charLength;
    private int arrayDim;
    private int inheritorsCount;
    private String description;
    @Nullable
    private PostgreAttributeIdentity identity;
    private boolean isLocal;
    private long collationId;
    private Object acl;
    private long typeId;
    private int typeMod;
    @Nullable
    private String[] foreignTableColumnOptions;
    @Nullable
    private String defaultValue;
    @Nullable
    private boolean isGeneratedColumn;

    protected PostgreAttribute(
        OWNER table)
    {
        super(table, false);
        this.isLocal = true;
    }

    public PostgreAttribute(
        DBRProgressMonitor monitor, OWNER table,
        JDBCResultSet dbResult)
        throws DBException
    {
        super(table, true);
        loadInfo(monitor, dbResult);
    }

    public PostgreAttribute(
        DBRProgressMonitor monitor,
        OWNER table,
        PostgreAttribute source)
        throws DBException
    {
        super(table, source, true);

        this.dataType = source.dataType;
        this.comment = source.comment;
        this.charLength = source.charLength;
        this.arrayDim = source.arrayDim;
        this.inheritorsCount = source.inheritorsCount;
        this.description = source.description;
        this.identity = source.identity;
        this.isLocal = source.isLocal;
        this.collationId = source.collationId;
        this.acl = source.acl;
        this.typeId = source.typeId;
        this.typeMod = source.typeMod;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return getTable().getDatabase();
    }

    @Override
    public long getObjectId() {
        return getOrdinalPosition();
    }

    @Override
    public void setMaxLength(long maxLength) {
        super.setMaxLength(maxLength);
        if (getDataKind() == DBPDataKind.STRING && this.precision != -1) {
            this.precision = (int)maxLength;
        }
    }

    @Override
    public void setPrecision(Integer precision) {
        super.setPrecision(precision);
        if (getDataKind() == DBPDataKind.STRING) {
            this.maxLength = CommonUtils.toInt(precision);
        }
    }

    private void loadInfo(DBRProgressMonitor monitor, JDBCResultSet dbResult)
        throws DBException
    {
        PostgreDataSource dataSource = getDataSource();
        PostgreServerExtension serverType = dataSource.getServerType();

        setName(JDBCUtils.safeGetString(dbResult, "attname"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "attnum"));
        setRequired(JDBCUtils.safeGetBoolean(dbResult, "attnotnull"));
        typeId = JDBCUtils.safeGetLong(dbResult, "atttypid");
        defaultValue = JDBCUtils.safeGetString(dbResult, "def_value");
        String serialValuePattern = getParentObject().getName() + "_" + getName() + "_seq";
        //set serial types manually
        if ((typeId == PostgreOid.INT2 || typeId == PostgreOid.INT4 || typeId == PostgreOid.INT8) &&
                (CommonUtils.isNotEmpty(defaultValue) && defaultValue.startsWith("nextval(") && defaultValue.contains(serialValuePattern))) {
            if (typeId == PostgreOid.INT4) {
                typeId = PostgreOid.SERIAL;
            } else if (typeId == PostgreOid.INT2) {
                typeId = PostgreOid.SMALLSERIAL;
            } else if (typeId == PostgreOid.INT8) {
                typeId = PostgreOid.BIGSERIAL;
            }
        }
        if (!CommonUtils.isEmpty(defaultValue) && serverType.supportsGeneratedColumns()) {
            String generatedColumn = JDBCUtils.safeGetString(dbResult, "attgenerated");
            // PostgreSQL 12/13 documentation says: "If a zero byte (''), then not a generated column. Otherwise, s = stored. (Other values might be added in the future)"
            if (!CommonUtils.isEmpty(generatedColumn)) {
                isGeneratedColumn = true;
            }
        }
        //setDefaultValue(defaultValue);
        dataType = getTable().getDatabase().getDataType(monitor, typeId);
        if (dataType == null) {
            log.error("Attribute data type '" + typeId + "' not found. Use " + PostgreConstants.TYPE_VARCHAR);
            dataType = getTable().getDatabase().getDataType(monitor, PostgreConstants.TYPE_VARCHAR);
        } else {
            // TODO: [#2824] Perhaps we should just use type names declared in pg_catalog
            // Replacing them with "convenient" types names migh cause some issues
            if (false && dataType.getCanonicalName() != null && dataSource.isServerVersionAtLeast(9, 6)) {
                // se canonical type names. But only for PG >= 9.6 (because I can't test with earlier versions)
                PostgreDataType canonicalType = getTable().getDatabase().getDataType(monitor, dataType.getCanonicalName());
                if (canonicalType != null) {
                    this.dataType = canonicalType;
                }
            }
        }
        //setTypeName(dataType.getTypeName());
        setValueType(dataType.getTypeID());
        typeMod = JDBCUtils.safeGetInt(dbResult, "atttypmod");
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
            !serverType.supportsInheritance() ||
            JDBCUtils.safeGetBoolean(dbResult, "attislocal", true);

        if (dataSource.isServerVersionAtLeast(10, 0)) {
            String identityStr = JDBCUtils.safeGetString(dbResult, "attidentity");
            if (!CommonUtils.isEmpty(identityStr)) {
                identity = PostgreAttributeIdentity.getByCode(identityStr);
            }
        }

        // Collation
        if (serverType.supportsCollations()) {
            this.collationId = JDBCUtils.safeGetLong(dbResult, "attcollation");
        }

        this.acl = JDBCUtils.safeGetObject(dbResult, "attacl");

        if (getTable() instanceof PostgreTableForeign) {
            foreignTableColumnOptions = JDBCUtils.safeGetArray(dbResult, "attfdwoptions");
        }

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

    @Override
    public void setDataType(@NotNull PostgreDataType dataType) {
        this.dataType = dataType;
        this.typeName = dataType.getTypeName();
        this.valueType = dataType.getTypeID();
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataType == null ? super.getDataKind() : dataType.getDataKind();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 25)
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    public String getTypeName()
    {
        return dataType == null ? super.getTypeName() : dataType.getTypeName();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 26)
    public Integer getPrecision()
    {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 27)
    public Integer getScale()
    {
        return super.getScale();
    }

    @Nullable
    @Property(viewable = true, editable = true, order = 28)
    public PostgreAttributeIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(PostgreAttributeIdentity identity) {
        this.identity = identity;
    }

    @Property(order = 29)
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

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 70)
    public String getDefaultValue()
    {
        if (isGeneratedColumn) {
            return null;
        }
        return defaultValue;
    }

    @Nullable
    @Property(order = 80)
    public String getGeneratedValue()
    {
        if (isGeneratedColumn) {
            return defaultValue;
        }
        return null;
    }

    @Property(viewable = true, order = 31, visibleIf = PostgreTableHasIntervalTypeValidator.class)
    public String getIntervalTypeField() {
        if (typeId == PostgreOid.INTERVAL) {
            return PostgreUtils.getIntervalField(typeMod);
        }
        return null;
    }

    public long getTypeId() {
        return typeId;
    }

    public int getTypeMod() {
        return typeMod;
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

    @Property(viewable = true, editable = true, order = 30, listProvider = CollationListProvider.class)
    public PostgreCollation getCollation(DBRProgressMonitor monitor) throws DBException {
        if (collationId <= 0) {
            return null;
        } else {
            return getDatabase().getCollation(monitor, collationId);
        }
    }

    public void setCollation(PostgreCollation collation) {
        this.collationId = collation == null ? 0 : collation.getObjectId();
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
        if (dataType == null) {
            return super.getFullTypeName();
        }
        String fqtn = dataType.getTypeName();
        if (dataType.getDataKind() != DBPDataKind.CONTENT) {
            return DBUtils.getFullTypeName(this);
        }
        return fqtn;
    }

    @Nullable
    public String[] getForeignTableColumnOptions() {
        return foreignTableColumnOptions;
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
            try {
                Collection<PostgreSchema> schemas = column.getDatabase().getSchemas(new VoidProgressMonitor());
                for (PostgreSchema schema : schemas) {
                    List<PostgreDataType> dataTypes = schema.getDataTypeCache().getCachedObjects();
                    types.addAll(dataTypes);
                }
            } catch (DBException e) {
                log.debug("Can't get data types from database schemas", e);
                types.addAll(column.getDatabase().getLocalDataTypes());
            }
            return types.toArray(new PostgreDataType[0]);
        }
    }

    public static class DataTypeValueTransformer implements IPropertyValueTransformer<PostgreAttribute, Object> {
        @Override
        public PostgreDataType transform(PostgreAttribute object, Object value) {
            if (value instanceof String) {
                PostgreDataType dataType = object.getDataSource().getLocalDataType((String)value);
                if (dataType == null) {
                    dataType = object.getDatabase().getDataType(null, (String)value);
                    if (dataType == null) {
                        throw new IllegalArgumentException("Bad data type name specified: " + value);
                    }
                }
                return dataType;
            } else if (value instanceof PostgreDataType) {
                return (PostgreDataType) value;
            } else {
                throw new IllegalArgumentException("Invalid type value: " + value);
            }
        }
    }

    public static class CollationListProvider implements IPropertyValueListProvider<PostgreAttribute> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(PostgreAttribute object) {
            try {
                return object.getDatabase().getCollations(new VoidProgressMonitor()).toArray();
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

    public static class PostgreTableHasIntervalTypeValidator implements IPropertyValueValidator<PostgreAttribute, Object> {

        @Override
        public boolean isValidValue(PostgreAttribute object, Object value) throws IllegalArgumentException {
            return object.getTypeId() == PostgreOid.INTERVAL;
        }
    }
}
