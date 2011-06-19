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
import org.jkiss.dbeaver.model.struct.DBSProcedure;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumn;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;
import org.jkiss.utils.IntKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericProcedure
 */
public abstract class OracleProcedureBase extends OracleSchemaObject implements DBSProcedure, OracleSourceObject
{
    //static final Log log = LogFactory.getLog(OracleProcedure.class);

    private DBSProcedureType procedureType;
    private final ArgumentsCache argumentsCache = new ArgumentsCache();

    public OracleProcedureBase(
        OracleSchema schema,
        String name,
        DBSProcedureType procedureType)
    {
        super(schema, name, true);
        this.procedureType = procedureType;
    }

    @Property(name = "Procedure Type", viewable = true, editable = true, order = 3)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    public abstract int getOverloadNumber();

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
                "WHERE OWNER=? AND OBJECT_NAME=? " +
                (procedure.getContainer() instanceof OraclePackage ? "AND PACKAGE_NAME=? AND OVERLOAD=? " : "AND PACKAGE_NAME IS NULL ") +
                "\nORDER BY SEQUENCE");
            dbStat.setString(1, procedure.getSchema().getName());
            dbStat.setString(2, procedure.getName());
            if (procedure.getContainer() instanceof OraclePackage) {
                dbStat.setString(3, procedure.getContainer().getName());
                dbStat.setInt(4, procedure.getOverloadNumber());
            }
            return dbStat;
        }

        @Override
        protected OracleProcedureArgument fetchObject(JDBCExecutionContext context, OracleProcedureBase procedure, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleProcedureArgument(context.getProgressMonitor(), procedure, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, Iterator<OracleProcedureArgument> objectIter)
        {
            IntKeyMap<OracleProcedureArgument> argStack = new IntKeyMap<OracleProcedureArgument>();
            while (objectIter.hasNext()) {
                OracleProcedureArgument argument = objectIter.next();
                final int curDataLevel = argument.getDataLevel();
                argStack.put(curDataLevel, argument);
                if (curDataLevel > 0) {
                    objectIter.remove();
                    OracleProcedureArgument parentArgument = argStack.get(curDataLevel - 1);
                    if (parentArgument == null) {
                        log.error("Broken arguments structure for '" + argument.getProcedure().getFullQualifiedName() + "' - no parent argument for argument " + argument.getSequence());
                    } else {
                        parentArgument.addAttribute(argument);
                    }
                }
            }
        }

    }

}
