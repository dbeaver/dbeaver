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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQLServerTable
 */
public class SQLServerTable extends SQLServerTableBase
{
    private static final Log log = Log.getLog(SQLServerTable.class);

    private CheckConstraintCache checkConstraintCache = new CheckConstraintCache();
    private String ddl;
    private volatile transient List<SQLServerTableForeignKey> references;

    public SQLServerTable(SQLServerSchema schema)
    {
        super(schema);
    }

    // Copy constructor
    public SQLServerTable(DBRProgressMonitor monitor, SQLServerSchema schema, SQLServerTable source) throws DBException {
        super(monitor, schema, source);

        DBSObjectCache<SQLServerTableBase, SQLServerTableColumn> colCache = getContainer().getTableCache().getChildrenCache(this);
        // Copy columns
        for (DBSEntityAttribute srcColumn : CommonUtils.safeCollection(source.getAttributes(monitor))) {
            if (DBUtils.isHiddenObject(srcColumn)) {
                continue;
            }
            SQLServerTableColumn column = new SQLServerTableColumn(monitor, this, srcColumn);
            colCache.cacheObject(column);
        }
    }

    public SQLServerTable(
        SQLServerSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Nullable
    @Override
    @Association
    public Collection<SQLServerTableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().getUniqueConstraintCache().getObjects(monitor, getSchema(), this);
    }

    @Nullable
    @Association
    public synchronized Collection<SQLServerTableCheckConstraint> getCheckConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return checkConstraintCache.getAllObjects(monitor, this);
    }

    public CheckConstraintCache getCheckConstraintCache() {
        return checkConstraintCache;
    }

    @Override
    @Association
    public List<SQLServerTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (references != null) {
            return references;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this,  "Read table references")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.schema_id as schema_id,t.name as table_name,fk.name as key_name\n" +
                    "FROM " +
                    SQLServerUtils.getSystemTableName(getDatabase(), "tables") + " t, " +
                    SQLServerUtils.getSystemTableName(getDatabase(), "foreign_keys") + " fk, " +
                    SQLServerUtils.getSystemTableName(getDatabase(), "tables") + " tr\n" +
                    "WHERE t.object_id = fk.parent_object_id AND tr.object_id=fk.referenced_object_id AND fk.referenced_object_id=?\n" +
                    "ORDER BY 1,2,3")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<SQLServerTableForeignKey> result = new ArrayList<>();
                    while (dbResult.next()) {
                        long schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
                        String tableName = JDBCUtils.safeGetString(dbResult, "table_name");
                        String fkName = JDBCUtils.safeGetString(dbResult, "key_name");

                        SQLServerSchema schema = getDatabase().getSchema(monitor, schemaId);
                        if (schema != null) {
                            SQLServerTableBase table = schema.getTable(monitor, tableName);
                            if (table != null) {
                                DBSEntityAssociation object = DBUtils.findObject(table.getAssociations(monitor), fkName);
                                if (object instanceof SQLServerTableForeignKey) {
                                    result.add((SQLServerTableForeignKey) object);
                                }
                            }
                        }
                    }
                    this.references = result;
                    return result;
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @Override
    public Collection<SQLServerTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getSchema().getForeignKeyCache().getObjects(monitor, getSchema(), this);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateTableDDL(monitor, this, options, false);
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return OPTION_DDL_ONLY_FOREIGN_KEYS.equals(option) || OPTION_DDL_SKIP_FOREIGN_KEYS.equals(option);
    }

    @Association
    public Collection<SQLServerTableTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        Collection<SQLServerTableTrigger> allTriggers = getSchema().getTriggerCache().getAllObjects(monitor, getSchema());
        return allTriggers
            .stream()
            .filter(p -> p.getTable() == this)
            .collect(Collectors.toList());
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        references = null;
        getContainer().getIndexCache().clearObjectCache(this);
        getContainer().getUniqueConstraintCache().clearObjectCache(this);
        getContainer().getForeignKeyCache().clearObjectCache(this);
        getContainer().getTriggerCache().clearChildrenOf(this);

        return getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public void setObjectDefinitionText(String source) {
        // Nope
    }

    /**
     * Constraint cache implementation
     */
    static class CheckConstraintCache extends JDBCObjectCache<SQLServerTable, SQLServerTableCheckConstraint> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerTable table) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM " + SQLServerUtils.getSystemTableName(table.getDatabase(), "check_constraints") + " WHERE parent_object_id=?");
            dbStat.setLong(1, table.getObjectId());
            return dbStat;
        }

        @Override
        protected SQLServerTableCheckConstraint fetchObject(@NotNull JDBCSession session, @NotNull SQLServerTable table, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerTableCheckConstraint(table, resultSet);
        }
    }

}
