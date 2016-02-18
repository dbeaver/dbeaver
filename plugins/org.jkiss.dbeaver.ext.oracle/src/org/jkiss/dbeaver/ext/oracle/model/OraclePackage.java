/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObjectEx;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericProcedure
 */
public class OraclePackage extends OracleSchemaObject
    implements OracleSourceObjectEx, DBSObjectContainer, DBPRefreshableObject, DBSProcedureContainer
{
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private boolean valid;
    private String sourceDeclaration;
    private String sourceDefinition;

    public OraclePackage(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), true);
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
    }

    public OraclePackage(OracleSchema schema, String name)
    {
        super(schema, name, false);
    }

    @Property(viewable = true, order = 3)
    public boolean isValid()
    {
        return valid;
    }

    @Override
    public OracleSourceType getSourceType()
    {
        return OracleSourceType.PACKAGE;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBCException
    {
        if (sourceDeclaration == null && monitor != null) {
            sourceDeclaration = OracleUtils.getSource(monitor, this, false, true);
        }
        return sourceDeclaration;
    }

    public void setObjectDefinitionText(String sourceDeclaration)
    {
        this.sourceDeclaration = sourceDeclaration;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectBodyDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (sourceDefinition == null && monitor != null) {
            sourceDefinition = OracleUtils.getSource(monitor, this, true, true);
        }
        return sourceDefinition;
    }

    @Override
    public void setObjectBodyDefinitionText(String source)
    {
        this.sourceDefinition = source;
    }

    @Association
    public Collection<OracleProcedurePackaged> getProcedures(DBRProgressMonitor monitor) throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public OracleProcedurePackaged getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
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
        return OracleProcedurePackaged.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
        proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        this.proceduresCache.clearCache();
        this.sourceDeclaration = null;
        this.sourceDefinition = null;
        return true;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = OracleUtils.getObjectStatus(monitor, this, OracleObjectType.PACKAGE);
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        List<DBEPersistAction> actions = new ArrayList<>();
        /*if (!CommonUtils.isEmpty(sourceDeclaration)) */{
            actions.add(
                new OracleObjectPersistAction(
                    OracleObjectType.PACKAGE,
                    "Compile package",
                    "ALTER PACKAGE " + getFullQualifiedName() + " COMPILE"
                ));
        }
        if (!CommonUtils.isEmpty(sourceDefinition)) {
            actions.add(
                new OracleObjectPersistAction(
                    OracleObjectType.PACKAGE_BODY,
                    "Compile package body",
                    "ALTER PACKAGE " + getFullQualifiedName() + " COMPILE BODY"
                ));
        }
        return actions.toArray(new DBEPersistAction[actions.size()]);
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    static class ProceduresCache extends JDBCObjectCache<OraclePackage, OracleProcedurePackaged> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OraclePackage owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT P.*,CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'FUNCTION' END as PROCEDURE_TYPE FROM ALL_PROCEDURES P\n" +
                "LEFT OUTER JOIN ALL_ARGUMENTS A ON A.OWNER=P.OWNER AND A.PACKAGE_NAME=P.OBJECT_NAME AND A.OBJECT_NAME=P.PROCEDURE_NAME AND A.ARGUMENT_NAME IS NULL AND A.DATA_LEVEL=0\n" +
                "WHERE P.OWNER=? AND P.OBJECT_NAME=?\n" +
                "ORDER BY P.PROCEDURE_NAME");
            dbStat.setString(1, owner.getSchema().getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleProcedurePackaged fetchObject(@NotNull JDBCSession session, @NotNull OraclePackage owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleProcedurePackaged(owner, dbResult);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OraclePackage owner, Iterator<OracleProcedurePackaged> objectIter)
        {
            Map<String, OracleProcedurePackaged> overloads = new HashMap<>();
            while (objectIter.hasNext()) {
                final OracleProcedurePackaged proc = objectIter.next();
                if (CommonUtils.isEmpty(proc.getName())) {
                    // Skip procedures with empty names
                    // Oracle 11+ has dummy procedure with subprogram_id=0 and empty name
                    objectIter.remove();
                    continue;
                }
                final OracleProcedurePackaged overload = overloads.get(proc.getName());
                if (overload == null) {
                    overloads.put(proc.getName(), proc);
                } else {
                    if (overload.getOverloadNumber() == null) {
                        overload.setOverload(1);
                    }
                    proc.setOverload(overload.getOverloadNumber() + 1);
                    overloads.put(proc.getName(), proc);
                }
            }
        }
    }

}
