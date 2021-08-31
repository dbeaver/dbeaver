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
import org.jkiss.dbeaver.ext.postgresql.model.data.type.PostgreTypeHandler;
import org.jkiss.dbeaver.ext.postgresql.model.data.type.PostgreTypeHandlerProvider;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt4;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * PostgreAttribute
 */
public abstract class PostgreAttribute<OWNER extends DBSEntity & PostgreObject> extends JDBCTableColumn<OWNER>
    implements PostgreObject, DBSTypedObjectEx, DBPNamedObject2, DBPHiddenObject, DBPInheritedObject, DBSTypedObjectExt4<PostgreDataType>
{
    private static final Log log = Log.getLog(PostgreAttribute.class);

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
        this.defaultValue = source.defaultValue;
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

    @Nullable
    @Override
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
    public long getMaxLength() {
        final PostgreTypeHandler handler = PostgreTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            final Integer length = handler.getTypeLength(dataType, typeMod);
            if (length != null) {
                return length;
            }
        }
        return PostgreUtils.getDisplaySize(typeId, typeMod);
    }

    @Override
    public void setMaxLength(long maxLength) {
        log.warn("Attribute does not support updating its max length");
    }

    @Override
    public Integer getPrecision() {
        final PostgreTypeHandler handler = PostgreTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            return handler.getTypePrecision(dataType, typeMod);
        }
        return null;
    }

    @Override
    public void setPrecision(Integer precision) {
        log.warn("Attribute does not support updating its precision");
    }

    @Override
    public Integer getScale() {
        final PostgreTypeHandler handler = PostgreTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            return handler.getTypeScale(dataType, typeMod);
        }
        return null;
    }

    @Override
    public void setScale(Integer scale) {
        log.warn("Attribute does not support updating its scale");
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

    @Override
    public void setDefaultValue(@Nullable String defaultValue) {
        this.defaultValue = defaultValue;
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

    public long getTypeId() {
        return typeId;
    }

    public int getTypeMod() {
        return typeMod;
    }

    public void setTypeMod(int typeMod) {
        this.typeMod = typeMod;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
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

    @Override
    public String getTypeName() {
        if (dataType != null) {
            return dataType.getTypeName();
        }
        return typeName;
    }

    @Override
    public void setTypeName(String typeName) throws DBException {
        final PostgreDataType dataType = findDataType(getSchema(), typeName);
        this.typeName = typeName;
        this.typeId = dataType.getTypeID();
        this.dataType = dataType;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = DataTypeListProvider.class)
    public String getFullTypeName() {
        if (dataType == null) {
            return getTypeName();
        }
        final PostgreTypeHandler handler = PostgreTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            return dataType.getTypeName() + handler.getTypeModifiersString(dataType, typeMod);
        }
        return dataType.getTypeName();
    }

    @Override
    public void setFullTypeName(String fullTypeName) throws DBException {
        final Pair<String, String[]> type = DBUtils.getTypeModifiers(fullTypeName);
        final String typeName = type.getFirst();
        final String[] typeMods = type.getSecond();

        final PostgreDataType dataType = findDataType(getSchema(), typeName);
        final PostgreTypeHandler handler = PostgreTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            this.typeMod = handler.getTypeModifiers(dataType, typeName, typeMods);
            this.typeId = dataType.getTypeID();
            this.dataType = dataType;
        } else {
            super.setFullTypeName(fullTypeName);
        }
    }

    @Nullable
    public String[] getForeignTableColumnOptions() {
        return foreignTableColumnOptions;
    }

    @NotNull
    public abstract PostgreSchema getSchema();

    @NotNull
    private static PostgreDataType findDataType(@NotNull PostgreSchema schema, @NotNull String typeName) throws DBException {
        PostgreDataType dataType = schema.getDataSource().getLocalDataType(typeName);
        if (dataType == null) {
            dataType = schema.getDatabase().getDataType(null, typeName);
        }
        if (dataType == null && schema.getDataSource().getServerType().supportsExternalTypes()) {
            log.debug("Can't find specified data type by name: '" + typeName + "', creating a fake type");
            dataType = new PostgreDataType(schema, Types.OTHER, typeName);
            schema.getDataTypeCache().cacheObject(dataType);
        }
        if (dataType == null) {
            throw new DBException("Can't find specified data type by name: '" + typeName + "'");
        }
        return dataType;
    }

    public static class DataTypeListProvider implements IPropertyValueListProvider<PostgreAttribute<?>> {

        @Override
        public boolean allowCustomValue() {
            return true;
        }

        @Override
        public Object[] getPossibleValues(PostgreAttribute<?> column) {
            List<PostgreDataType> types = new ArrayList<>();
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
            return types.stream()
                .map(DBSTypedObject::getTypeName)
                .sorted(Comparator
                    .comparing((String name) -> name.startsWith("_")) // Sort the arrays data types at the end of the list
                    .thenComparing(Function.identity()))
                .toArray(String[]::new);
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
}
