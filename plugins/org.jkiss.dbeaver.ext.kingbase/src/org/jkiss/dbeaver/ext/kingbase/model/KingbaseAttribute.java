/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.model.data.type.KingbaseTypeHandler;
import org.jkiss.dbeaver.ext.kingbase.model.data.type.KingbaseTypeHandlerProvider;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPInheritedObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSTypeDescriptor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx2;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt4;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

/**
 * KingbaseAttribute
 */
public abstract class KingbaseAttribute<OWNER extends DBSEntity & KingbaseObject> extends JDBCTableColumn<OWNER>
    implements KingbaseObject, DBSTypedObjectEx, DBPNamedObject2, DBPHiddenObject, DBPInheritedObject, DBSTypedObjectExt4<KingbaseDataType>, DBSTypedObjectEx2
{
    private static final Log log = Log.getLog(KingbaseAttribute.class);

    private KingbaseDataType dataType;
    private String comment;
    private long charLength;
    private int arrayDim;
    private int inheritorsCount;
    private String description;
    @Nullable
    private KingbaseAttributeIdentity identity;
    private boolean isLocal;
    private long collationId;
    private Object acl;
    private long typeId;
    private int typeMod;
    @Nullable
    private String defaultValue;
    @Nullable
    private boolean isGeneratedColumn;
    private long depObjectId;

    protected KingbaseAttribute(
        OWNER table)
    {
        super(table, false);
        this.isLocal = true;
    }

    public KingbaseAttribute(
        DBRProgressMonitor monitor, OWNER table,
        JDBCResultSet dbResult)
        throws DBException
    {
        super(table, true);
        loadInfo(monitor, dbResult);
    }

    public KingbaseAttribute(
        DBRProgressMonitor monitor,
        OWNER table,
        KingbaseAttribute source)
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
    public KingbaseDatabase getDatabase() {
        return getTable().getDatabase();
    }

    @Override
    public long getObjectId() {
        return getOrdinalPosition();
    }

    private void loadInfo(DBRProgressMonitor monitor, JDBCResultSet dbResult)
        throws DBException
    {
        KingbaseDataSource dataSource = getDataSource();
        KingbaseServerExtension serverType = dataSource.getServerType();

        setName(JDBCUtils.safeGetString(dbResult, "attname"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "attnum"));
        setRequired(JDBCUtils.safeGetBoolean(dbResult, "attnotnull"));
        typeId = JDBCUtils.safeGetLong(dbResult, "atttypid");
        defaultValue = JDBCUtils.safeGetString(dbResult, "def_value");
        String serialValuePattern = getParentObject().getName() + "_" + getName() + "_seq";
        //set serial types manually
        if ((typeId == KingbaseOid.INT2 || typeId == KingbaseOid.INT4 || typeId == KingbaseOid.INT8) &&
                (CommonUtils.isNotEmpty(defaultValue) && defaultValue.startsWith("nextval(") && defaultValue.contains(serialValuePattern))) {
            if (typeId == KingbaseOid.INT4) {
                typeId = KingbaseOid.SERIAL;
            } else if (typeId == KingbaseOid.INT2) {
                typeId = KingbaseOid.SMALLSERIAL;
            } else if (typeId == KingbaseOid.INT8) {
                typeId = KingbaseOid.BIGSERIAL;
            }
        }
        if (!CommonUtils.isEmpty(defaultValue) && serverType.supportsGeneratedColumns()) {
            String generatedColumn = JDBCUtils.safeGetString(dbResult, "attgenerated");
            if (!CommonUtils.isEmpty(generatedColumn)) {
                isGeneratedColumn = true;
            }
        }
        //setDefaultValue(defaultValue);
        dataType = getTable().getDatabase().getDataType(monitor, typeId);
        if (dataType == null) {
            log.error("Attribute data type '" + typeId + "' not found. Use " + KingbaseConstants.TYPE_VARCHAR);
            dataType = getTable().getDatabase().getDataType(monitor, KingbaseConstants.TYPE_VARCHAR);
        } else {
            // TODO: [#2824] Perhaps we should just use type names declared in sys_catalog
            // Replacing them with "convenient" types names migh cause some issues
            if (false && dataType.getCanonicalName() != null) {
                // se canonical type names. But only for PG >= 9.6 (because I can't test with earlier versions)
                KingbaseDataType canonicalType = getTable().getDatabase().getDataType(monitor, dataType.getCanonicalName());
                if (canonicalType != null) {
                    this.dataType = canonicalType;
                }
            }
        }
        if (dataType != null) {
            //setTypeName(dataType.getTypeName());
            setValueType(dataType.getTypeID());
        }
        typeMod = JDBCUtils.safeGetInt(dbResult, "atttypmod");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.arrayDim = JDBCUtils.safeGetInt(dbResult, "attndims");
        this.inheritorsCount = JDBCUtils.safeGetInt(dbResult, "attinhcount");
        this.isLocal =
            !serverType.supportsInheritance() ||
            JDBCUtils.safeGetBoolean(dbResult, "attislocal", true);


        String identityStr = JDBCUtils.safeGetString(dbResult, "attidentity");
        if (!CommonUtils.isEmpty(identityStr)) {
            identity = KingbaseAttributeIdentity.getByCode(identityStr);
        }

        // Collation
        if (serverType.supportsCollations()) {
            this.collationId = JDBCUtils.safeGetLong(dbResult, "attcollation");
        }

        if (serverType.supportsAcl()) {
            this.acl = JDBCUtils.safeGetObject(dbResult, "attacl");
        }

        setPersisted(true);

        if (supportsDependencies() && serverType.supportsSequences()) {
            this.depObjectId = JDBCUtils.safeGetLong(dbResult, "objid"); // ID of object which has dependency with this column
        }
    }

    protected boolean supportsDependencies() {
        return false;
    }

    @NotNull
    @Override
    public KingbaseDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    public Object getAcl() {
        return acl;
    }

    @Nullable
    @Override
    public KingbaseDataType getDataType() {
        return dataType;
    }

    @Override
    public void setDataType(@NotNull KingbaseDataType dataType) {
        this.dataType = dataType;
        this.typeName = dataType.getTypeName();
        this.valueType = dataType.getTypeID();
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return dataType == null ? super.getDataKind() : dataType.getDataKind();
    }

    @Override
    public long getMaxLength() {
        final KingbaseTypeHandler handler = KingbaseTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            final Integer length = handler.getTypeLength(dataType, typeMod);
            if (length != null) {
                return length;
            }
        }
        return KingbaseUtils.getDisplaySize(typeId, typeMod);
    }

    @Override
    public void setMaxLength(long maxLength) {
        log.debug("Attribute does not support updating its max length");
    }

    @Nullable
    @Override
    public Integer getPrecision() {
        final KingbaseTypeHandler handler = KingbaseTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            return handler.getTypePrecision(dataType, typeMod);
        }
        return null;
    }

    @Override
    public void setPrecision(@Nullable Integer precision) {
        log.debug("Attribute does not support updating its precision");
    }

    @Override
    public Integer getScale() {
        final KingbaseTypeHandler handler = KingbaseTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            return handler.getTypeScale(dataType, typeMod);
        }
        return null;
    }

    @Override
    public void setScale(@Nullable Integer scale) {
        log.debug("Attribute does not support updating its scale");
    }

    @Nullable
    @Property(viewable = true, editableExpr = "!object.table.view", order = 28)
    public KingbaseAttributeIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(KingbaseAttributeIdentity identity) {
        this.identity = identity;
    }

    @Property(order = 29)
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    @Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 50)
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
    @Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 70)
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

    public long getDepObjectId() {
        return depObjectId;
    }

    @Property(viewable = true, editableExpr = "!object.table.view", order = 30, listProvider = CollationListProvider.class)
    public KingbaseCollation getCollation(DBRProgressMonitor monitor) throws DBException {
        if (collationId <= 0) {
            return null;
        } else {
            return getDatabase().getCollation(monitor, collationId);
        }
    }

    public void setCollation(KingbaseCollation collation) {
        this.collationId = collation == null ? 0 : collation.getObjectId();
    }

    @Override
    public boolean isHidden() {
        if (isPersisted()) {
            return getOrdinalPosition() < 0 || getDataSource().getServerType().isHiddenRowidColumn(this);
        }
        return false;
    }

    @Override
    public boolean isInherited() {
        return !isLocal;
    }

    @NotNull
    @Override
    public String getTypeName() {
        if (dataType != null) {
            return dataType.getTypeName();
        }
        return typeName;
    }

    @Override
    public void setTypeName(@NotNull String typeName) throws DBException {
        final KingbaseDataType dataType = resolveOrCreateDataType(typeName);
        this.typeName = typeName;
        this.typeId = dataType.getTypeID();
        this.dataType = dataType;
    }

    @NotNull
    @Override
    @Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 20, listProvider = DataTypeListProvider.class)
    public String getFullTypeName() {
        if (dataType == null) {
            return getTypeName();
        }
        final KingbaseTypeHandler handler = KingbaseTypeHandlerProvider.getTypeHandler(dataType);
        String typeName = dataType.getFullyQualifiedName(DBPEvaluationContext.DDL);
        if (handler != null) {
            return typeName + handler.getTypeModifiersString(dataType, typeMod);
        }
        return typeName;
    }

    @Override
    public void setFullTypeName(@NotNull String fullTypeName) throws DBException {
        final Pair<String, String[]> type = DBUtils.getTypeModifiers(fullTypeName);
        final String typeName = type.getFirst();
        final String[] typeMods = type.getSecond();

        final KingbaseDataType dataType = resolveOrCreateDataType(typeName);
        final KingbaseTypeHandler handler = KingbaseTypeHandlerProvider.getTypeHandler(dataType);
        if (handler != null) {
            this.typeMod = handler.getTypeModifiers(dataType, typeName, typeMods);
            this.typeId = dataType.getTypeID();
            this.dataType = dataType;
        } else {
            super.setFullTypeName(fullTypeName);
        }
    }

    @NotNull
    public abstract KingbaseSchema getSchema();

    @NotNull
    private KingbaseDataType resolveOrCreateDataType(@NotNull String typeName) throws DBException {
        KingbaseDataType dataType = KingbaseUtils.resolveTypeFullName(new VoidProgressMonitor(), getSchema(), typeName);
        if (dataType == null) {
            // retry search in local schema types and create some data type on failure
            dataType = findDataType(getSchema(), typeName);
        }
        return dataType;
    }
    
    @NotNull
    private static KingbaseDataType findDataType(@NotNull KingbaseSchema schema, @NotNull String typeName) throws DBException {
        KingbaseDataType dataType = schema.getDataSource().getLocalDataType(typeName);
        if (dataType == null) {
            dataType = schema.getDatabase().getDataType(null, typeName);
        }
        if (dataType == null && schema.getDataSource().getServerType().supportsExternalTypes()) {
            log.debug("Can't find specified data type by name: '" + typeName + "', creating a fake type");
            dataType = new KingbaseDataType(schema, Types.OTHER, typeName);
            schema.getDataTypeCache().cacheObject(dataType);
        }
        if (dataType == null) {
            throw new DBException("Can't find specified data type by name: '" + typeName + "'");
        }
        return dataType;
    }

    public static class DataTypeListProvider implements IPropertyValueListProvider<KingbaseAttribute<?>> {

        @Override
        public boolean allowCustomValue() {
            return true;
        }

        @Override
        public Object[] getPossibleValues(KingbaseAttribute<?> column) {
            List<KingbaseDataType> types = new ArrayList<>();
            try {
                Collection<KingbaseSchema> schemas = column.getDatabase().getSchemas(new VoidProgressMonitor());
                for (KingbaseSchema schema : schemas) {
                    List<KingbaseDataType> dataTypes = schema.getDataTypeCache().getCachedObjects();
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

    public static class CollationListProvider implements IPropertyValueListProvider<KingbaseAttribute> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(KingbaseAttribute object) {
            try {
                return object.getDatabase().getCollations(new VoidProgressMonitor()).toArray();
            } catch (DBException e) {
                log.error(e);
                return new Object[0];
            }
        }
    }

    @Nullable
    @Override
    public DBSTypeDescriptor getTypeDescriptor() {
        return this.arrayDim > 0 && this.getDataType() != null
            ? new KingbaseArrayAttrTypeDescriptor(false, this.arrayDim, this.getDataType())
            : null;
    }

    /**
     * Array column type in kingbase can
     *     either be completely indexed through all the dimensions till the single item reflected with its data type,
     *     or sliced with any other way of indexing producing an array of the same structural type.
     * Partial exposure is questionable, didn't find working example for Kingbase, but some other databases supports that.
     */
    private static class KingbaseArrayAttrTypeDescriptor implements DBSTypeDescriptor {
        private final boolean isItemType;
        private final int arrayDim;
        private final DBSDataType itemType;

        public  KingbaseArrayAttrTypeDescriptor(boolean isItemType, int arrayDim, @NotNull DBSDataType itemType) {
            this.isItemType = isItemType;
            this.arrayDim = arrayDim;
            this.itemType = itemType;
        }

        @Nullable
        @Override
        public DBSDataType getUnderlyingType() {
            return isItemType ? itemType : null;
        }

        @Override
        public boolean isIndexable() {
            return !isItemType;
        }

        @NotNull
        @Override
        public String getTypeName() {
            return this.itemType.getFullTypeName() + "[]".repeat(isItemType ? 0 : arrayDim);
        }

        @Override
        public int getIndexableDimensions() {
            return isItemType ? 0 : arrayDim;
        }

        @Nullable
        @Override
        public DBSTypeDescriptor getIndexableItemType(int depth, boolean[] slicingSpecOrNull) {
            if (isItemType) {
                return null;
            } else {
                if (slicingSpecOrNull == null) {
                    return depth == arrayDim ? new KingbaseArrayAttrTypeDescriptor(true, this.arrayDim, this.itemType)
                        : (depth > arrayDim ? null : this);
                } else if (slicingSpecOrNull.length != arrayDim) {
                    return slicingSpecOrNull.length > arrayDim ? null : this;
                } else {
                    for (int i = 0; i < slicingSpecOrNull.length; i++) {
                        if (slicingSpecOrNull[i]) {
                            return this;
                        }
                    }
                    return new KingbaseArrayAttrTypeDescriptor(true, this.arrayDim, this.itemType);
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof KingbaseArrayAttrTypeDescriptor other && ((
                    !this.isItemType && !other.isItemType && this.arrayDim == other.arrayDim && this.itemType.equals(other.itemType)
                ) || (
                    this.isItemType && other.isItemType && this.itemType.equals(other.itemType)
                ));
        }
    }
    
}
