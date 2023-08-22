package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
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
import java.util.Objects;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public abstract class YashanDBProcedureBase<PARENT extends DBSObjectContainer> extends YashanDBObject<PARENT> implements DBSProcedure {
    static final Log log = Log.getLog(YashanDBProcedureBase.class);

    private DBSProcedureType procedureType;
    private final ArgumentsCache argumentsCache = new ArgumentsCache();

    public YashanDBProcedureBase(
            PARENT parent,
            String name,
            long objectId,
            DBSProcedureType procedureType) {
        super(parent, name, objectId, true);
        this.procedureType = procedureType;
    }

    @Override
    @Property(viewable = true, editable = true, order = 3)
    public DBSProcedureType getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(DBSProcedureType procedureType) {
        this.procedureType = procedureType;
    }

    @Override
    public DBSObjectContainer getContainer() {
        return getParentObject();
    }

    public abstract YashanDBSchema getSchema();

    public abstract Integer getOverloadNumber();

    @Override
    public Collection<YashanDBProcedureArgument> getParameters(DBRProgressMonitor monitor) throws DBException {
        return argumentsCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<YashanDBDependencyGroup> getDependencies(DBRProgressMonitor monitor) {
        return YashanDBDependencyGroup.of(this);
    }

    static class ArgumentsCache extends JDBCObjectCache<YashanDBProcedureBase, YashanDBProcedureArgument> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBProcedureBase procedure) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM ALL_ARGUMENTS " +
                            "WHERE " +
                            (procedure.getObjectId() <= 0 ? "OWNER=? AND OBJECT_NAME=? " : "OBJECT_ID=? ") +
                            (procedure.getOverloadNumber() != null ? "AND OVERLOAD=? " : "AND OVERLOAD IS NULL ") +
                            "\nORDER BY SEQUENCE");
            int paramNum = 1;
            if (procedure.getObjectId() <= 0) {
                dbStat.setString(paramNum++, procedure.getSchema().getName());
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
        protected YashanDBProcedureArgument fetchObject(@NotNull JDBCSession session, @NotNull YashanDBProcedureBase procedure, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new YashanDBProcedureArgument(session.getProgressMonitor(), procedure, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, YashanDBProcedureBase owner, Iterator<YashanDBProcedureArgument> objectIter) {
            IntKeyMap<YashanDBProcedureArgument> argStack = new IntKeyMap<>();
            while (objectIter.hasNext()) {
                YashanDBProcedureArgument argument = objectIter.next();
                final int curDataLevel = argument.getDataLevel();
                argStack.put(curDataLevel, argument);
                if (curDataLevel > 0) {
                    objectIter.remove();
                    YashanDBProcedureArgument parentArgument = argStack.get(curDataLevel - 1);
                    if (parentArgument == null) {
                        log.error("Broken arguments structure for '" + argument.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + "' - no parent argument for argument " + argument.getSequence());
                    } else {
                        parentArgument.addAttribute(argument);
                    }
                }
            }
        }

    }
}
