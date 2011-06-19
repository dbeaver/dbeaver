/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * GenericProcedure
 */
public class OraclePackage extends OracleObject implements OracleSourceObject,DBSEntityContainer
{
    private final ProceduresCache proceduresCache = new ProceduresCache();

    public OraclePackage(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
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

    static class ProceduresCache extends JDBCObjectCache<OraclePackage, OracleProcedurePackaged> {

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OraclePackage owner)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT P.*,CASE WHEN A.DATA_TYPE IS NULL THEN 'PROCEDURE' ELSE 'FUNCTION' END as PROCEDURE_TYPE FROM ALL_PROCEDURES P\n" +
                "LEFT OUTER JOIN ALL_ARGUMENTS A ON A.OWNER=P.OWNER AND A.PACKAGE_NAME=P.OBJECT_NAME AND A.OBJECT_NAME=P.PROCEDURE_NAME AND A.POSITION=0\n" +
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
        protected void invalidateObjects(DBRProgressMonitor monitor, Collection<OracleProcedurePackaged> objectList)
        {
            Map<String, Integer> overloads = new HashMap<String, Integer>();
            for (final OracleProcedurePackaged proc : objectList) {
                final Integer overload = overloads.get(proc.getName());
                if (overload == null) {
                    overloads.put(proc.getName(), 1);
                } else {
                    proc.setOverload(overload + 1);
                    overloads.put(proc.getName(), overload + 1);
                }
            }
        }
    }

}
