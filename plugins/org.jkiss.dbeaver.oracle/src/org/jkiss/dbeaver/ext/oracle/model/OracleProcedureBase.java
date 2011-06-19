/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSProcedure;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * GenericProcedure
 */
public abstract class OracleProcedureBase extends OracleObject implements DBSProcedure, OracleSourceObject
{
    //static final Log log = LogFactory.getLog(OracleProcedure.class);

    private DBSProcedureType procedureType;
    private final ArgumentsCache argumentsCache = new ArgumentsCache();

    public OracleProcedureBase(
        OracleSchema schema,
        DBSProcedureType procedureType,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        this.procedureType = procedureType;
    }

    @Property(name = "Procedure Type", viewable = true, editable = true, order = 3)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    public Collection<? extends DBSProcedureColumn> getColumns(DBRProgressMonitor monitor) throws DBException
    {
        return argumentsCache.getObjects(monitor, this);
    }

    public OracleSchema getSourceOwner()
    {
        return getSchema();
    }

    static class ArgumentsCache extends JDBCObjectCache<OracleProcedureBase, OracleProcedureArgument> {

        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleProcedureBase procedure) throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_ARGUMENTS " +
                "WHERE OWNER=? AND OBJECT_ID=? " +
                "ORDER BY POSITION,SEQUENCE");
            dbStat.setString(1, procedure.getSchema().getName());
            dbStat.setLong(2, procedure.getId());
            return dbStat;
        }

        @Override
        protected OracleProcedureArgument fetchObject(JDBCExecutionContext context, OracleProcedureBase procedure, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleProcedureArgument(context.getProgressMonitor(), procedure, resultSet);
        }
    }

}
