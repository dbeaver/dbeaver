/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.edit.AbstractDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * GenericProcedure
 */
public class OraclePackage extends OracleSchemaObject implements OracleSourceObject,OracleCompileUnit,DBSEntityContainer
{
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private boolean valid;

    public OraclePackage(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), true);
        this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
    }

    @Property(name = "Valid", viewable = true, order = 3)
    public boolean isValid()
    {
        return valid;
    }

    public OracleSchema getSourceOwner()
    {
        return getSchema();
    }

    public OracleSourceType getSourceType()
    {
        return OracleSourceType.PACKAGE;
    }

    @Association
    public Collection<OracleProcedurePackaged> getProcedures(DBRProgressMonitor monitor) throws DBException
    {
        return proceduresCache.getObjects(monitor, this);
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return proceduresCache.getObjects(monitor, this);
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return proceduresCache.getObject(monitor, this, childName);
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return OracleProcedurePackaged.class;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        proceduresCache.getObjects(monitor, this);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        proceduresCache.clearCache();
        return true;
    }

    public IDatabasePersistAction[] getCompileActions()
    {
        return new IDatabasePersistAction[] {
            new AbstractDatabasePersistAction(
                "Compile package",
                "ALTER PACKAGE " + getFullQualifiedName() + " COMPILE"
            )};
    }

    public DBSObjectState getObjectState()
    {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    static class ProceduresCache extends JDBCObjectCache<OraclePackage, OracleProcedurePackaged> {

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OraclePackage owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT P.*,CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'FUNCTION' END as PROCEDURE_TYPE FROM ALL_PROCEDURES P\n" +
                "LEFT OUTER JOIN ALL_ARGUMENTS A ON A.OWNER=P.OWNER AND A.PACKAGE_NAME=P.OBJECT_NAME AND A.OBJECT_NAME=P.PROCEDURE_NAME AND A.ARGUMENT_NAME IS NULL AND A.DATA_LEVEL=0\n" +
                "WHERE P.OWNER=? AND P.OBJECT_NAME=?\n" +
                "ORDER BY P.PROCEDURE_NAME");
            dbStat.setString(1, owner.getSchema().getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        protected OracleProcedurePackaged fetchObject(JDBCExecutionContext context, OraclePackage owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleProcedurePackaged(owner, dbResult);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OraclePackage owner, Iterator<OracleProcedurePackaged> objectIter)
        {
            Map<String, OracleProcedurePackaged> overloads = new HashMap<String, OracleProcedurePackaged>();
            while (objectIter.hasNext()) {
                final OracleProcedurePackaged proc = objectIter.next();
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
