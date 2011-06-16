/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * GenericProcedure
 */
public class OracleProcedure extends OracleObject implements DBSProcedure, OracleSourceObject
{
    //static final Log log = LogFactory.getLog(OracleProcedure.class);

    private DBSProcedureType procedureType;
    private List<OracleProcedureArgument> columns;

    public OracleProcedure(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        this.procedureType = DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE"));
    }

    public DBSEntityContainer getContainer()
    {
        return getSchema();
    }

    @Property(name = "Procedure Type", viewable = true, editable = true, order = 3)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    public Collection<? extends DBSProcedureColumn> getColumns(DBRProgressMonitor monitor) throws DBException
    {
        if (!isColumnsCached()) {
            getSchema().getProceduresCache().loadChildren(monitor, getDataSource(), this);
        }
        return columns;
    }

    boolean isColumnsCached()
    {
        synchronized (this) {
            return columns != null;
        }
    }

    void cacheColumns(List<OracleProcedureArgument> columns)
    {
        synchronized (this) {
            this.columns = columns;
        }
    }

    public OracleSchema getSourceOwner()
    {
        return getSchema();
    }

    public OracleSourceType getSourceType()
    {
        return procedureType == DBSProcedureType.PROCEDURE ?
            OracleSourceType.PROCEDURE :
            OracleSourceType.FUNCTION;
    }
}
