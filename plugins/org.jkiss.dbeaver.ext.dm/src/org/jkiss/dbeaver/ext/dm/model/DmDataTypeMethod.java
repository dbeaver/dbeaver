package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
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

public class DmDataTypeMethod extends DmDataTypeMember implements DBSEntityMethod, DBSParametrizedObject{
	 private String methodType;
	    private boolean flagFinal;
	    private boolean flagInstantiable;
	    private boolean flagOverriding;

	    private DmDataType resultType;
	    private DmDataTypeModifier resultTypeMod;
	    private final ParameterCache parameterCache;

	    public DmDataTypeMethod(DmDataType dataType)
	    {
	        super(dataType);
	        this.parameterCache = new ParameterCache();
	    }

	    public DmDataTypeMethod(DBRProgressMonitor monitor, DmDataType dataType, ResultSet dbResult)
	    {
	        super(dataType, dbResult);
	        this.name = JDBCUtils.safeGetString(dbResult, "METHOD_NAME");
	        this.number = JDBCUtils.safeGetInt(dbResult, "METHOD_NO");

	        this.methodType = JDBCUtils.safeGetString(dbResult, "METHOD_TYPE");

	        this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", DmConstants.YES);
	        this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", DmConstants.YES);
	        this.flagOverriding = JDBCUtils.safeGetBoolean(dbResult, "OVERRIDING", DmConstants.YES);

	        boolean hasParameters = JDBCUtils.safeGetInt(dbResult, "PARAMETERS") > 0;
	        this.parameterCache = hasParameters ? new ParameterCache() : null;

	        String resultTypeName = JDBCUtils.safeGetString(dbResult, "RESULT_TYPE_NAME");
	        if (!CommonUtils.isEmpty(resultTypeName)) {
	            this.resultType = DmDataType.resolveDataType(
	                monitor,
	                getDataSource(),
	                JDBCUtils.safeGetString(dbResult, "RESULT_TYPE_OWNER"),
	                resultTypeName);
	            this.resultTypeMod = DmDataTypeModifier.resolveTypeModifier(
	                JDBCUtils.safeGetString(dbResult, "RESULT_TYPE_MOD"));
	        }
	    }

	    @Property(viewable = true, editable = true, order = 5)
	    public String getMethodType()
	    {
	        return methodType;
	    }

	    @Property(id = "dataType", viewable = true, order = 6)
	    public DmDataType getResultType()
	    {
	        return resultType;
	    }

	    @Property(id = "dataTypeMod", viewable = true, order = 7)
	    public DmDataTypeModifier getResultTypeMod()
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
	    public Collection<DmDataTypeMethodParameter> getParameters(DBRProgressMonitor monitor)
	        throws DBException
	    {
	        return parameterCache == null ? null : parameterCache.getAllObjects(monitor, this);
	    }

	    private class ParameterCache extends JDBCObjectCache<DmDataTypeMethod, DmDataTypeMethodParameter> {
	        @NotNull
	        @Override
	        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DmDataTypeMethod owner) throws SQLException
	        {
	            final JDBCPreparedStatement dbStat = session.prepareStatement(
	                "SELECT PARAM_NAME,PARAM_NO,PARAM_MODE,PARAM_TYPE_OWNER,PARAM_TYPE_NAME,PARAM_TYPE_MOD " +
	                "FROM ALL_METHOD_PARAMS " +
	                "WHERE OWNER=? AND TYPE_NAME=? AND METHOD_NAME=? AND METHOD_NO=?");
	            DmDataType dataType = getOwnerType();
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
	        protected DmDataTypeMethodParameter fetchObject(@NotNull JDBCSession session, @NotNull DmDataTypeMethod owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
	        {
	            return new DmDataTypeMethodParameter(
	                session.getProgressMonitor(),
	                DmDataTypeMethod.this,
	                resultSet);
	        }
	    }
}
