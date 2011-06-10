/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;

import java.sql.ResultSet;
import java.util.List;

/**
 * GenericProcedure
 */
public class OracleProcedure extends AbstractProcedure<OracleDataSource, OracleSchema>
{
    //static final Log log = LogFactory.getLog(OracleProcedure.class);

    private DBSProcedureType procedureType;
    private String resultType;
    private String bodyType;
    private String body;
    private String charset;
    private List<OracleProcedureColumn> columns;

    public OracleProcedure(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
    {
        setName(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ROUTINE_NAME));
        setDescription(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ROUTINE_COMMENT));
        this.procedureType = DBSProcedureType.valueOf(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ROUTINE_TYPE).toUpperCase());
        this.resultType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DTD_IDENTIFIER);
        this.bodyType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ROUTINE_BODY);
        this.body = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ROUTINE_DEFINITION);
        this.charset = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CHARACTER_SET_CLIENT);
    }

    @Property(name = "Procedure Type", order = 2)
    public DBSProcedureType getProcedureType()
    {
        return procedureType ;
    }

    @Property(name = "Result Type", order = 2)
    public String getResultType()
    {
        return resultType;
    }

    @Property(name = "Body Type", order = 3)
    public String getBodyType()
    {
        return bodyType;
    }

    //@Property(name = "Body", order = 4)
    public String getBody()
    {
        return body;
    }

    //@Property(name = "Client Charset", order = 4)
    public String getCharset()
    {
        return charset;
    }

    public List<OracleProcedureColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            getContainer().getProceduresCache().loadChildren(monitor, this);
        }
        return columns;
    }

    public boolean isColumnsCached()
    {
        return this.columns != null;
    }

    public void cacheColumns(List<OracleProcedureColumn> columns)
    {
        this.columns = columns;
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }
}
