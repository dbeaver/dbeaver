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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.Pair;

import java.sql.ResultSet;
import java.util.Map;

public class SQLServerExtendedProperty implements SQLServerObject, DBPUniqueObject, DBPRefreshableObject, DBPScriptObject {

    private static final Log log = Log.getLog(SQLServerExtendedProperty.class);

    private final SQLServerTableBase table;
    private final SQLServerObjectClass clazz;
    private final long majorId;
    private final long minorId;
    private final String name;
    private final Object value;
    private final SQLServerDataType type;
    private final boolean persisted;

    public SQLServerExtendedProperty(@NotNull DBRProgressMonitor monitor, @NotNull SQLServerTableBase table, @NotNull ResultSet dbResult) throws DBException {
        this.table = table;
        this.clazz = SQLServerObjectClass.valueOf(JDBCUtils.safeGetStringTrimmed(dbResult, "class_desc"));
        this.majorId = JDBCUtils.safeGetLong(dbResult, "major_id");
        this.minorId = JDBCUtils.safeGetLong(dbResult, "minor_id");
        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.value = JDBCUtils.safeGetObject(dbResult, "value");
        this.persisted = true;

        SQLServerDataType type = table.getDatabase().getDataTypeByUserTypeId(monitor, JDBCUtils.safeGetInt(dbResult, "value_type"));
        if (type == null) {
            type = getDataSource().getLocalDataType(SQLServerConstants.TYPE_NVARCHAR);
        }
        this.type = type;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @NotNull
    @Property(order = 2)
    public String getClazz() {
        return clazz.getClassName();
    }

    @Property(order = 3)
    public long getMajorId() {
        return majorId;
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public SQLServerObject getMajorIdObject() {
        return table;
    }

    @Property(order = 5)
    public long getMinorId() {
        return minorId;
    }

    @Nullable
    @Property(viewable = true, order = 6)
    public SQLServerObject getMinorIdObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (clazz == SQLServerObjectClass.OBJECT_OR_COLUMN) {
            return minorId > 0 ? table.getAttribute(monitor, minorId) : table;
        }
        return null;
    }

    @Nullable
    @Property(viewable = true, order = 7)
    public Object getValue() {
        return value;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return table.getDataSource();
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return table;
    }

    @Override
    public long getObjectId() {
        return minorId;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @NotNull
    @Override
    public String getUniqueName() {
        return name + ':' + majorId + ':' + minorId;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return table.getExtendedPropertyCache().refreshObject(monitor, table, this);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        final SQLDialect dialect = SQLUtils.getDialectFromObject(this);

        final Pair<String, SQLServerObject> level0 = getLevel1Object();
        final Pair<String, SQLServerObject> level1 = getLevel2Object();
        final Pair<String, SQLServerObject> level2 = getLevel3Object(monitor);

        if (level0 == null || (level1 == null && level2 == null)) {
            log.debug("Can't get definition for extended property of class '" + clazz.getClassName() + "'");
            return null;
        }

        final StringBuilder sql = new StringBuilder()
            .append("EXEC sp_addextendedproperty")
            .append(" @name=").append(dialect.getQuotedString(name))
            .append(", @value=").append(SQLUtils.convertValueToSQL(getDataSource(), type, value))
            .append(", @level0type=").append(dialect.getQuotedString(level0.getFirst()))
            .append(", @level0name=").append(dialect.getQuotedString(level0.getSecond().getName()));

        if (level1 != null) {
            sql.append(", @level1type=").append(dialect.getQuotedString(level1.getFirst()))
                .append(", @level1name=").append(dialect.getQuotedString(level1.getSecond().getName()));
        }

        if (level2 != null) {
            sql.append(", @level2type=").append(dialect.getQuotedString(level2.getFirst()))
                .append(", @level2name=").append(dialect.getQuotedString(level2.getSecond().getName()));
        }

        return sql.toString();
    }

    @Nullable
    private Pair<String, SQLServerObject> getLevel1Object() {
        if (clazz == SQLServerObjectClass.OBJECT_OR_COLUMN) {
            return new Pair<>("Schema", table.getSchema());
        }
        return null;
    }

    @Nullable
    private Pair<String, SQLServerObject> getLevel2Object() {
        if (clazz == SQLServerObjectClass.OBJECT_OR_COLUMN) {
            return new Pair<>("Table", table);
        }
        return null;
    }

    @Nullable
    private Pair<String, SQLServerObject> getLevel3Object(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (clazz == SQLServerObjectClass.OBJECT_OR_COLUMN && minorId > 0) {
            return new Pair<>("Column", table.getAttribute(monitor, minorId));
        }
        return null;
    }
}
