/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityMethod;
import org.jkiss.dbeaver.model.struct.DBSParametrizedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * Oracle data type attribute
 */
public class OracleDataTypeMethod extends OracleDataTypeMember implements DBSEntityMethod, DBSParametrizedObject {

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

    @Property(viewable = true, editable = true, order = 5)
    public String getMethodType()
    {
        return methodType;
    }

    @Property(id = "dataType", viewable = true, order = 6)
    public OracleDataType getResultType()
    {
        return resultType;
    }

    @Property(id = "dataTypeMod", viewable = true, order = 7)
    public OracleDataTypeModifier getResultTypeMod()
    {
        return resultTypeMod;
    }

    @Property(viewable = true, order = 8)
    public boolean isFinal()
    {
        return flagFinal;
    }

    @Property(viewable = true, order = 9)
    public boolean isInstantiable()
    {
        return flagInstantiable;
    }

    @Property(viewable = true, order = 10)
    public boolean isOverriding()
    {
        return flagOverriding;
    }

    @Association
    public Collection<OracleDataTypeMethodParameter> getParameters(DBRProgressMonitor monitor)
        throws DBException
    {
        return parameterCache == null ? null : parameterCache.getAllObjects(monitor, this);
    }

    private class ParameterCache extends JDBCObjectCache<OracleDataTypeMethod, OracleDataTypeMethodParameter> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataTypeMethod owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT PARAM_NAME,PARAM_NO,PARAM_MODE,PARAM_TYPE_OWNER,PARAM_TYPE_NAME,PARAM_TYPE_MOD " +
                "FROM ALL_METHOD_PARAMS " +
                "WHERE OWNER=? AND TYPE_NAME=? AND METHOD_NAME=? AND METHOD_NO=?");
            OracleDataType dataType = getOwnerType();
            if (dataType.getSchema() == null) {
                dbStat.setNull(1, Types.VARCHAR);
            } else {
                dbStat.setString(1, dataType.getSchema().getName());
            }
            dbStat.setString(2, dataType.getName());
            dbStat.setString(3, getName());
            dbStat.setInt(4, getNumber());
            return dbStat;
        }

        @Override
        protected OracleDataTypeMethodParameter fetchObject(@NotNull JDBCSession session, @NotNull OracleDataTypeMethod owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataTypeMethodParameter(
                session.getProgressMonitor(),
                OracleDataTypeMethod.this,
                resultSet);
        }
    }
}
