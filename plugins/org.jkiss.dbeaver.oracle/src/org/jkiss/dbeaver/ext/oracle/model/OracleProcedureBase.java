/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.IntKeyMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

/**
 * GenericProcedure
 */
public abstract class OracleProcedureBase<PARENT extends DBSObjectContainer> extends OracleObject<PARENT> implements DBSProcedure
{
    //static final Log log = Log.getLog(OracleProcedure.class);

    private DBSProcedureType procedureType;
    private final ArgumentsCache argumentsCache = new ArgumentsCache();

    public OracleProcedureBase(
        PARENT parent,
        String name,
        long objectId,
        DBSProcedureType procedureType)
    {
        super(parent, name, objectId, true);
        this.procedureType = procedureType;
    }

    @Override
    @Property(viewable = true, editable = true, order = 3)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    @Override
    public DBSObjectContainer getContainer()
    {
        return getParentObject();
    }

    public abstract OracleSchema getSchema();

    public abstract Integer getOverloadNumber();

    @Override
    public Collection<OracleProcedureArgument> getParameters(DBRProgressMonitor monitor) throws DBException
    {
        return argumentsCache.getObjects(monitor, this);
    }

    static class ArgumentsCache extends JDBCObjectCache<OracleProcedureBase, OracleProcedureArgument> {

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleProcedureBase procedure) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_ARGUMENTS " +
                "WHERE " +
                (procedure.getObjectId() <= 0  ? "OWNER=? AND OBJECT_NAME=? AND PACKAGE_NAME=? " : "OBJECT_ID=? ") +
                (procedure.getOverloadNumber() != null ? "AND OVERLOAD=? " : "AND OVERLOAD IS NULL ") +
                "\nORDER BY SEQUENCE");
            int paramNum = 1;
            if (procedure.getObjectId() <= 0) {
                dbStat.setString(paramNum++, procedure.getSchema().getName());
                dbStat.setString(paramNum++, procedure.getName());
                dbStat.setString(paramNum++, procedure.getContainer().getName());
            } else {
                dbStat.setLong(paramNum++, procedure.getObjectId());
            }
            if (procedure.getOverloadNumber() != null) {
                dbStat.setInt(paramNum, procedure.getOverloadNumber());
            }
            return dbStat;
        }

        @Override
        protected OracleProcedureArgument fetchObject(JDBCSession session, OracleProcedureBase procedure, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleProcedureArgument(session.getProgressMonitor(), procedure, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OracleProcedureBase owner, Iterator<OracleProcedureArgument> objectIter)
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
                        log.error("Broken arguments structure for '" + argument.getParentObject().getFullQualifiedName() + "' - no parent argument for argument " + argument.getSequence());
                    } else {
                        parentArgument.addAttribute(argument);
                    }
                }
            }
        }

    }

}
