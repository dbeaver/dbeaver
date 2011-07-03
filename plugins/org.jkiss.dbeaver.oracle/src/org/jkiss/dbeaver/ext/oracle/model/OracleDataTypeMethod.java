/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Oracle data type attribute
 */
public class OracleDataTypeMethod extends OracleDataTypeMember {

    private String methodType;
    private boolean flagFinal;
    private boolean flagInstantiable;
    private boolean flagOverriding;

    private OracleDataType resultType;
    private OracleDataTypeModifier resultTypeMod;
    private final ParameterCache parameterCache;

    public OracleDataTypeMethod(OracleDataType dataType)
    {
        super(dataType);
        this.parameterCache = new ParameterCache();
    }

    public OracleDataTypeMethod(DBRProgressMonitor monitor, OracleDataType dataType, ResultSet dbResult)
    {
        super(dataType, dbResult);
        this.name = JDBCUtils.safeGetString(dbResult, "METHOD_NAME");
        this.number = JDBCUtils.safeGetInt(dbResult, "METHOD_NO");

        this.methodType = JDBCUtils.safeGetString(dbResult, "METHOD_TYPE");

        this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", OracleConstants.YES);
        this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", OracleConstants.YES);
        this.flagOverriding = JDBCUtils.safeGetBoolean(dbResult, "OVERRIDING", OracleConstants.YES);

        boolean hasParameters = JDBCUtils.safeGetInt(dbResult, "PARAMETERS") > 0;
        this.parameterCache = hasParameters ? new ParameterCache() : null;

        String resultTypeName = JDBCUtils.safeGetString(dbResult, "RESULT_TYPE_NAME");
        if (!CommonUtils.isEmpty(resultTypeName)) {
            this.resultType = OracleDataType.resolveDataType(
                monitor,
                getDataSource(),
                JDBCUtils.safeGetString(dbResult, "RESULT_TYPE_OWNER"),
                resultTypeName);
            this.resultTypeMod = OracleDataTypeModifier.resolveTypeModifier(
                JDBCUtils.safeGetString(dbResult, "RESULT_TYPE_MOD"));
        }
    }

    @Property(name = "Method Type", viewable = true, editable = true, order = 5)
    public String getMethodType()
    {
        return methodType;
    }

    @Property(id = "dataType", name = "Result", viewable = true, order = 6)
    public OracleDataType getResultType()
    {
        return resultType;
    }

    @Property(id = "dataTypeMod", name = "Result Mod", viewable = true, order = 7)
    public OracleDataTypeModifier getResultTypeMod()
    {
        return resultTypeMod;
    }

    @Property(name = "Final", viewable = true, order = 8)
    public boolean isFinal()
    {
        return flagFinal;
    }

    @Property(name = "Instantiable", viewable = true, order = 9)
    public boolean isInstantiable()
    {
        return flagInstantiable;
    }

    @Property(name = "Overriding", viewable = true, order = 10)
    public boolean isOverriding()
    {
        return flagOverriding;
    }

    @Association
    public Collection<OracleDataTypeMethodParameter> getParameters(DBRProgressMonitor monitor)
        throws DBException
    {
        return parameterCache == null ? null : parameterCache.getObjects(monitor, this);
    }

    private class ParameterCache extends JDBCObjectCache<OracleDataTypeMethod, OracleDataTypeMethodParameter> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleDataTypeMethod owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT PARAM_NAME,PARAM_NO,PARAM_MODE,PARAM_TYPE_OWNER,PARAM_TYPE_NAME,PARAM_TYPE_MOD " +
                "FROM ALL_METHOD_PARAMS " +
                "WHERE OWNER=? AND TYPE_NAME=? AND METHOD_NAME=? AND METHOD_NO=?");
            dbStat.setString(1, getDataType().getSourceOwner().getName());
            dbStat.setString(2, getDataType().getName());
            dbStat.setString(3, getName());
            dbStat.setInt(4, getNumber());
            return dbStat;
        }

        @Override
        protected OracleDataTypeMethodParameter fetchObject(JDBCExecutionContext context, OracleDataTypeMethod owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeMethodParameter(
                context.getProgressMonitor(),
                OracleDataTypeMethod.this,
                resultSet);
        }
    }
}
