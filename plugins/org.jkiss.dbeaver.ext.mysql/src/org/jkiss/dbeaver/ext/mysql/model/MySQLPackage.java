/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * GenericProcedure
 */
public class MySQLPackage
    implements DBPScriptObject, DBPScriptObjectExt, DBSObjectContainer, DBPRefreshableObject, DBSProcedureContainer, DBSPackage, DBPQualifiedObject
{
    private MySQLCatalog catalog;
    private String name;
    private String description;
    private boolean persisted;
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private String sourceDeclaration;
    private String sourceDefinition;

    public MySQLPackage(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        this.catalog = catalog;
        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.description = JDBCUtils.safeGetString(dbResult, "comment");
        this.persisted = true;
    }

    public MySQLPackage(MySQLCatalog catalog, String name)
    {
        this.catalog = catalog;
        this.name = name;
        this.persisted = false;
    }

    @Override
    public DBPDataSource getDataSource() {
        return catalog.getDataSource();
    }

    @Override
    public DBSObject getParentObject() {
        return catalog;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Property(viewable = true, order = 1)
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getCatalog(),
            this);
    }

    @Property(viewable = true, order = 100)
    @Override
    public String getDescription() {
        return description;
    }

    public MySQLCatalog getCatalog() {
        return catalog;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException
    {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = readSource(monitor, false);
        }
        return sourceDeclaration;
    }

    public void setObjectDefinitionText(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (sourceDefinition == null && monitor != null) {
            sourceDefinition = readSource(monitor, true);
        }
        return sourceDefinition;
    }

    public void setExtendedDefinitionText(String source)
    {
        this.sourceDefinition = source;
    }

    @Association
    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return proceduresCache.getObject(monitor, this, uniqueName);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException
    {
        return proceduresCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return MySQLProcedure.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
        proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        this.proceduresCache.clearCache();
        this.sourceDeclaration = null;
        this.sourceDefinition = null;
        return this;
    }

    private String readSource(DBRProgressMonitor monitor, boolean isBody) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read package declaration")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW CREATE PACKAGE" + (isBody ? " BODY" : "") + " " + getFullyQualifiedName(DBPEvaluationContext.DML))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return JDBCUtils.safeGetString(dbResult, (isBody ? "Create Package Body" : "Create Package"));
                    } else {
                        throw new DBCException("Package '" + getName() + "' not found");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, getDataSource());
        }
    }

    static class ProceduresCache extends JDBCObjectCache<MySQLPackage, MySQLProcedure> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLPackage owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT P.*,CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'FUNCTION' END as PROCEDURE_TYPE FROM ALL_PROCEDURES P\n" +
                "LEFT OUTER JOIN ALL_ARGUMENTS A ON A.OWNER=P.OWNER AND A.PACKAGE_NAME=P.OBJECT_NAME AND A.OBJECT_NAME=P.PROCEDURE_NAME AND A.ARGUMENT_NAME IS NULL AND A.DATA_LEVEL=0\n" +
                "WHERE P.OWNER=? AND P.OBJECT_NAME=?\n" +
                "ORDER BY P.PROCEDURE_NAME");
            dbStat.setString(1, owner.getCatalog().getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Override
        protected MySQLProcedure fetchObject(@NotNull JDBCSession session, @NotNull MySQLPackage owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLProcedure(owner.getCatalog(), dbResult);
        }

    }

}
