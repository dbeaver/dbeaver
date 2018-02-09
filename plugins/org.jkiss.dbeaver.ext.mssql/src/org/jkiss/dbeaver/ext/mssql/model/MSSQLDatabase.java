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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
* SQL Server database
*/
public class MSSQLDatabase implements DBSCatalog, DBPRefreshableObject {

    private static final Log log = Log.getLog(MSSQLDatabase.class);

    private final MSSQLDataSource dataSource;
    private String catalogName;
    private String remarks;
    private List<MSSQLSchema> schemas;

    public MSSQLDatabase(MSSQLDataSource dataSource, JDBCResultSet dbResult) {
        this(dataSource, JDBCUtils.safeGetString(dbResult, 1));
        this.remarks = JDBCUtils.safeGetString(dbResult, "remarks");
    }

    public MSSQLDatabase(MSSQLDataSource dataSource, String catalogName) {
        this.dataSource = dataSource;
        this.catalogName = catalogName;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getName() {
        return catalogName;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public String getDescription() {
        return remarks;
    }

    @Association
    public Collection<MSSQLSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        if (schemas == null) {
            try (JDBCSession session = dataSource.getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.UTIL, "Load schemas")) {
                schemas = loadSchemas(session);
            }
        }
        return schemas;
    }

    private List<MSSQLSchema> loadSchemas(JDBCSession session) throws DBException {
        boolean showAllSchemas = dataSource.isShowAllSchemas();
        final DBSObjectFilter schemaFilters = dataSource.getContainer().getObjectFilter(GenericSchema.class, this, false);

        String sysSchema = DBUtils.getQuotedIdentifier(this) + ".sys";
        String sql;
        if (showAllSchemas) {
            if (dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR ,0)) {
                sql = "SELECT name FROM " + DBUtils.getQuotedIdentifier(this) + ".sys.schemas";
            } else {
                sql = "SELECT name FROM " + DBUtils.getQuotedIdentifier(this) + ".dbo.sysusers";
            }
        } else {
            sql = "SELECT DISTINCT s.name\n" +
                "FROM " + sysSchema + ".schemas s, " + sysSchema + ".sysobjects o\n" +
                "WHERE s.schema_id=o.uid\n" +
                "ORDER BY 1";
        }

        List<MSSQLSchema> result = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    String name = JDBCUtils.safeGetString(dbResult, 1);
                    if (name == null) {
                        continue;
                    }
                    name = name.trim();
                    if (schemaFilters != null && !schemaFilters.matches(name)) {
                        // Doesn't match filter
                        continue;
                    }

                    MSSQLSchema schema = new MSSQLSchema(this, name);
                    result.add(schema);
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
        if (result.isEmpty()) {
            if (!showAllSchemas) {
                // Perhaps all schemas were filtered out
                result.add(new MSSQLSchema(this, SQLServerConstants.DEFAULT_SCHEMA_NAME));
            } else {
                // Maybe something went wrong. LEt's try to use native function
                log.warn("Schema read failed: empty list returned. Try generic method.");
                try (JDBCResultSet dbResult = session.getMetaData().getSchemas()) {
                    while (dbResult.next()) {
                        result.add(new MSSQLSchema(this, dbResult.getString("TABLE_SCHEM")));
                    }
                } catch (SQLException e) {
                    log.error("Error reading schemas from database metadata", e);
                }
            }
        }
        return result;
    }

    ////////////////////////////////////////////////////
    // Children

    @Override
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
        return getSchemas(monitor);
    }

    @Override
    public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
        return DBUtils.findObject(getSchemas(monitor), childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException {
        return MSSQLSchema.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        getSchemas(monitor);
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        this.schemas = null;
        return this;
    }

}
