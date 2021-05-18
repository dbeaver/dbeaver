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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBPositiveNumberTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt4;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQLServerTableColumn
 */
public class SQLServerTableColumn extends JDBCTableColumn<SQLServerTableBase> implements
    DBSTableColumn, DBSTypedObjectEx, DBPNamedObject2, DBPOrderedObject, DBPHiddenObject,
    SQLServerObject, JDBCColumnKeyType, DBSTypedObjectExt4<SQLServerDataType> {
    private static final Log log = Log.getLog(SQLServerTableColumn.class);

    private long objectId;
    private int userTypeId;
    private SQLServerDataType dataType;
    private int bytesMaxLength;
    private String collationName;
    private String description;
    private boolean hidden;
    private boolean computedPersisted;
    private String computedDefinition;
    private IdentityInfo identityInfo = new IdentityInfo();

    public static class IdentityInfo {
        private long seedValue;
        private long incrementValue;
        private long lastValue;
        private boolean loaded;

        public long getSeedValue() {
            return seedValue;
        }

        public long getIncrementValue() {
            return incrementValue;
        }

        @Property(viewable = false, order = 60)
        public long getLastValue() {
            return lastValue;
        }
    }

    public static class IdentityInfoValidator implements IPropertyCacheValidator<SQLServerTableColumn> {
        @Override
        public boolean isPropertyCached(SQLServerTableColumn object, Object propertyId) {
            return object.identityInfo.loaded;
        }
    }

    public SQLServerTableColumn(SQLServerTableBase table) {
        super(table, false);
    }

    public SQLServerTableColumn(
        DBRProgressMonitor monitor,
        SQLServerTableBase table,
        ResultSet dbResult)
        throws DBException {
        super(table, true);
        loadInfo(monitor, dbResult);
    }

    // Copy constructor
    public SQLServerTableColumn(
        DBRProgressMonitor monitor,
        SQLServerTableBase table,
        DBSEntityAttribute source)
        throws DBException {
        super(table, source, false);
        this.description = source.getDescription();
        if (source instanceof SQLServerTableColumn) {
            SQLServerTableColumn mySource = (SQLServerTableColumn) source;
            // Copy
        }
    }

    private void loadInfo(DBRProgressMonitor monitor, ResultSet dbResult)
        throws DBException {
        this.objectId = JDBCUtils.safeGetLong(dbResult, "column_id");

        setName(JDBCUtils.safeGetString(dbResult, "name"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "column_id"));

        this.userTypeId = JDBCUtils.safeGetInt(dbResult, "user_type_id");
        this.dataType = getTable().getDatabase().getDataTypeByUserTypeId(monitor, userTypeId);
        if (this.dataType == null) {
            throw new DBCException("Data type '" + userTypeId + "' not found for column '" + getName() + "'");
        }
        this.bytesMaxLength = JDBCUtils.safeGetInt(dbResult, "max_length");
        this.maxLength = getDataSource().supportsColumnProperty() ? JDBCUtils.safeGetLong(dbResult, "char_max_length") : 0;
        if (this.maxLength == 0) {
            this.maxLength = this.bytesMaxLength;
            String typeName = getTypeName();
            if (typeName.equals(SQLServerConstants.TYPE_NVARCHAR) || typeName.equals(SQLServerConstants.TYPE_NCHAR)) {
                this.maxLength /= 2; // Divide by 2.
            }
        }
        setRequired(JDBCUtils.safeGetInt(dbResult, "is_nullable") == 0);
        setScale(JDBCUtils.safeGetInteger(dbResult, "scale"));
        setPrecision(JDBCUtils.safeGetInteger(dbResult, "precision"));
        setAutoGenerated(JDBCUtils.safeGetInt(dbResult, "is_identity") != 0);

        if (this.getName().equals(SQLServerConstants.TYPE_TIMESTAMP)) {
            setAutoGenerated(true);
        }
        setValueType(dataType.getTypeID());

        this.hidden = JDBCUtils.safeGetInt(dbResult, "is_hidden") != 0;
        this.collationName = JDBCUtils.safeGetString(dbResult, "collation_name");
        String dv = JDBCUtils.safeGetString(dbResult, "default_definition");
        if (!CommonUtils.isEmpty(dv)) {
            // Remove redundant brackets
            while (dv.startsWith("(") && dv.endsWith(")")) {
                dv = dv.substring(1, dv.length() - 1);
            }
            this.setDefaultValue(dv);
        }
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.computedPersisted = JDBCUtils.safeGetInt(dbResult, "is_persisted") != 0;
        this.computedDefinition = JDBCUtils.safeGetString(dbResult, "computed_definition");
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return getTable().getDataSource();
    }

    //@Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
    public String getFullTypeName() {
        if (dataType == null) {
            return String.valueOf(userTypeId);
        }

        String typeName = dataType.getName();
        String typeModifiers = SQLUtils.getColumnTypeModifiers(getDataSource(), this, typeName, dataType.getDataKind());
        return typeModifiers == null ? typeName : (typeName + CommonUtils.notEmpty(typeModifiers));
    }

    @Override
    public String getTypeName() {
        return dataType == null ? String.valueOf(userTypeId) : dataType.getTypeName();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 40, listProvider = DataTypeListProvider.class)
    public DBSDataType getDataType() {
        return dataType;
    }

    @Override
    public void setDataType(SQLServerDataType dataType) {
        this.dataType = dataType;
        this.typeName = dataType.getTypeName();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 41)
    public long getMaxLength() {
        return super.getMaxLength();
    }

    @Property(order = 42)
    public int getBytesMaxLength() {
        return bytesMaxLength;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 43)
    public Integer getScale() {
        return super.getScale();
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 44)
    public Integer getPrecision() {
        return super.getPrecision();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 50)
    @Override
    public boolean isRequired() {
        return super.isRequired();
    }

    @Override
    public boolean isAutoGenerated() {
        // Computed columns are "auto-generated" (from supplied definition)
        return super.isAutoGenerated() || CommonUtils.isNotEmpty(getComputedDefinition());
    }

    @Property(viewable = true, editable = true, order = 55)
    public boolean isIdentity() {
        // We have to do that to avoid treating computed column as an identity column (see #isAutoGenerated)
        return super.isAutoGenerated();
    }

    public void setIdentity(boolean identity) {
        super.setAutoGenerated(identity);
    }

    @Property(viewable = true, editable = true, updatable = true, order = 70)
    @Override
    public String getDefaultValue() {
        return super.getDefaultValue();
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataType == null ? DBPDataKind.UNKNOWN : dataType.getDataKind();
    }

    @Property(viewable = true, editable = true, updatable = true, order = 75)
    public String getCollationName() {
        return collationName;
    }

    public void setCollationName(String collationName) {
        this.collationName = collationName;
    }

    @Property(editable = true, order = 76)
    public String getComputedDefinition() {
        return computedDefinition;
    }

    public void setComputedDefinition(String computedDefinition) {
        this.computedDefinition = computedDefinition;
    }

    @Property(editable = true, order = 77)
    public boolean isComputedPersisted() {
        return computedPersisted;
    }

    public void setComputedPersisted(boolean computedPersisted) {
        this.computedPersisted = computedPersisted;
    }

    @Property(viewable = false, order = 80)
    @Override
    public boolean isHidden() {
        return hidden;
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

    @Override
    @Property(viewable = false, order = 80)
    public long getObjectId() {
        return objectId;
    }

    @Override
    protected JDBCColumnKeyType getKeyType() {
        return this;
    }

    @Override
    public boolean isInUniqueKey() {
        // Mark all identity columns as unique keys
        return isAutoGenerated();
    }

    @Override
    public boolean isInReferenceKey() {
        return false;
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = IdentityInfoValidator.class)
    public IdentityInfo getIdentityInfo(DBRProgressMonitor monitor) throws DBCException {
        if (!identityInfo.loaded) {
            if (!isAutoGenerated()) {
                identityInfo.loaded = true;
            } else {
                loadIdentityInfo(monitor);
            }
        }
        return identityInfo;
    }

    private void loadIdentityInfo(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load column identity info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT seed_value,increment_value,last_value FROM " +
                    SQLServerUtils.getSystemTableName(getTable().getDatabase(), "identity_columns") + " WHERE object_id=?")) {
                dbStat.setLong(1, getTable().getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        identityInfo.seedValue = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "seed_value"));
                        identityInfo.incrementValue = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "increment_value"));
                        identityInfo.lastValue = CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "last_value"));
                    }
                    identityInfo.loaded = true;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    public static class DataTypeListProvider implements IPropertyValueListProvider<SQLServerTableColumn> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(SQLServerTableColumn object) {
            List<SQLServerDataType> allTypes = new ArrayList<>(object.getDataSource().getLocalDataTypes());
            try {
                List<SQLServerDataType> schemaTypes = object.getTable().getSchema().getDataTypes(new VoidProgressMonitor())
                        .stream().filter(type -> !type.isTableType()).collect(Collectors.toList()); //do not show table types in types list
                allTypes.addAll(schemaTypes);
            } catch (DBException e) {
                log.debug("Error getting schema data types", e);
            }
            return allTypes.toArray();
        }
    }
}
