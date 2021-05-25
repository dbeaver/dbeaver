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
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SQLServerExtendedProperty implements SQLServerObject, DBPUniqueObject, DBPRefreshableObject, DBPScriptObject, DBPNamedObject2 {

    private static final Log log = Log.getLog(SQLServerExtendedProperty.class);

    private final SQLServerExtendedPropertyOwner owner;
    private String name;
    private String value;
    private SQLServerDataType type;
    private final boolean persisted;

    public SQLServerExtendedProperty(@NotNull DBRProgressMonitor monitor, @NotNull SQLServerExtendedPropertyOwner owner, @NotNull ResultSet dbResult) throws DBException {
        final SQLServerObjectClass objectClass = CommonUtils.valueOf(SQLServerObjectClass.class, JDBCUtils.safeGetStringTrimmed(dbResult, "class_desc"));
        final long majorId = JDBCUtils.safeGetLong(dbResult, "major_id");
        final long minorId = JDBCUtils.safeGetLong(dbResult, "minor_id");

        if (objectClass != owner.getExtendedPropertyObjectClass() || majorId != owner.getMajorObjectId() || minorId != owner.getMinorObjectId()) {
            throw new DBException("Extended property owner mismatch");
        }

        this.owner = owner;
        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.value = JDBCUtils.safeGetString(dbResult, "value");
        this.persisted = true;

        SQLServerDataType type = owner.getDatabase().getDataTypeByUserTypeId(monitor, JDBCUtils.safeGetInt(dbResult, "value_type"));
        if (type == null) {
            type = getDataSource().getLocalDataType(SQLServerConstants.TYPE_NVARCHAR);
        }
        this.type = type;
    }

    public SQLServerExtendedProperty(@NotNull SQLServerExtendedPropertyOwner owner, @NotNull SQLServerDataType type, @NotNull String name, @NotNull String value) {
        this.owner = owner;
        this.name = name;
        this.value = value;
        this.type = type;
        this.persisted = false;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public String getValue() {
        return CommonUtils.toString(value, null);
    }

    public void setValue(@Nullable String value) {
        this.value = value;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 3, listProvider = DataTypeListProvider.class)
    public SQLServerDataType getValueType() {
        return type;
    }

    public void setValueType(@NotNull SQLServerDataType type) {
        this.type = type;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return owner.getDataSource();
    }

    @NotNull
    @Override
    public SQLServerExtendedPropertyOwner getParentObject() {
        return owner;
    }

    @Override
    public long getObjectId() {
        return owner.getMinorObjectId();
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @NotNull
    @Override
    public String getUniqueName() {
        return name + ':' + owner.getMajorObjectId() + ':' + owner.getMinorObjectId();
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return owner.getExtendedPropertyCache().refreshObject(monitor, owner, this);
    }

    @Nullable
    public String getObjectDefinitionText(DBRProgressMonitor monitor, boolean update, boolean delete) throws DBException {
        if (update && delete) {
            throw new DBException("Can't get object definition text for both 'update' and 'delete'");
        }

        final Pair<String, SQLServerObject> level0 = owner.getExtendedPropertyObject(monitor, 0);
        final Pair<String, SQLServerObject> level1 = owner.getExtendedPropertyObject(monitor, 1);
        final Pair<String, SQLServerObject> level2 = owner.getExtendedPropertyObject(monitor, 2);

        if (level0 == null || (level1 == null && level2 == null)) {
            log.debug("Can't get definition for extended property of class '" + owner.getExtendedPropertyObjectClass().getClassName() + "'");
            return null;
        }

        final SQLDialect dialect = SQLUtils.getDialectFromObject(this);
        final StringBuilder ddl = new StringBuilder("EXEC ");

        ddl.append(SQLServerUtils.getSystemTableName(
            owner.getDatabase(),
            update ? "sp_updateextendedproperty" : delete ? "sp_dropextendedproperty" : "sp_addextendedproperty"
        ));

        ddl.append(" @name=").append(dialect.getQuotedString(name));

        if (!delete) {
            ddl.append(", @value=").append(SQLUtils.convertValueToSQL(getDataSource(), type, value));
        }

        appendLevelDefinitionText(ddl, dialect, level0, 0);

        if (level1 != null) {
            appendLevelDefinitionText(ddl, dialect, level1, 1);
        }

        if (level2 != null) {
            appendLevelDefinitionText(ddl, dialect, level2, 2);
        }

        return ddl.toString();
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getObjectDefinitionText(monitor, false, false);
    }

    private static void appendLevelDefinitionText(@NotNull StringBuilder ddl, @NotNull SQLDialect dialect, @NotNull Pair<String, SQLServerObject> level, int index) {
        ddl.append(", @level").append(index).append("type=").append(dialect.getQuotedString(level.getFirst()));
        ddl.append(", @level").append(index).append("name=").append(dialect.getQuotedString(level.getSecond().getName()));
    }

    public static class DataTypeListProvider implements IPropertyValueListProvider<SQLServerExtendedProperty> {
        private static final Set<String> RESTRICTED_TYPE_NAMES;

        static {
            // https://docs.microsoft.com/en-us/sql/t-sql/data-types/sql-variant-transact-sql#restrictions
            RESTRICTED_TYPE_NAMES = new HashSet<>();
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_DATETIMEOFFSET);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_GEOGRAPHY);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_GEOMETRY);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_HIERARCHYID);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_IMAGE);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_NTEXT);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_TEXT);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_SQL_VARIANT);
            RESTRICTED_TYPE_NAMES.add(SQLServerConstants.TYPE_XML);
        }

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(SQLServerExtendedProperty object) {
            return object.getDataSource().getLocalDataTypes().stream()
                .filter(type -> !RESTRICTED_TYPE_NAMES.contains(type.getName()))
                .toArray();
        }
    }
}
