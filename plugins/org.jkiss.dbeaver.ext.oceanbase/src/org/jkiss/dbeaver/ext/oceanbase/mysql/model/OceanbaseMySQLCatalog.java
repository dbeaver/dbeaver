package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedureParameter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

public class OceanbaseMySQLCatalog extends MySQLCatalog{
	private final OceanbaseMySQLDataSource dataSource;
	final OceanbaseProceduresCache oceanbaseProceduresCache = new OceanbaseProceduresCache();
	
	static class OceanbaseProceduresCache extends JDBCStructLookupCache<OceanbaseMySQLCatalog, OceanbaseMySQLProcedure, MySQLProcedureParameter> {

		public OceanbaseProceduresCache() {
			super(JDBCConstants.PROCEDURE_NAME);
		}
		
		@Override
		public JDBCStatement prepareLookupStatement(JDBCSession session, OceanbaseMySQLCatalog owner, OceanbaseMySQLProcedure object,
				String objectName) throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement(
	                "SELECT * FROM " + MySQLConstants.META_TABLE_ROUTINES +
	                    "\nWHERE " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?" +
	                    (object == null && objectName == null ? "" : " AND " + MySQLConstants.COL_ROUTINE_NAME + "=?") +
	                    " AND ROUTINE_TYPE" + (object == null ? " IN ('PROCEDURE','FUNCTION')" : "=?") +
	                    "\nORDER BY " + MySQLConstants.COL_ROUTINE_NAME
	            );
	            dbStat.setString(1, owner.getName());
	            if (object != null || objectName != null) {
	                dbStat.setString(2, object != null ? object.getName() : objectName);
	                if (object != null) {
	                    dbStat.setString(3, String.valueOf(object.getProcedureType()));
	                }
	            }
	            return dbStat;
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, OceanbaseMySQLCatalog owner,
				OceanbaseMySQLProcedure procedure) throws SQLException {
			if(procedure.getProcedureType().equals(DBSProcedureType.PROCEDURE)) {
	            return session.getMetaData().getProcedureColumns(
	                owner.getName(),
	                null,
	                procedure == null ? null : JDBCUtils.escapeWildCards(session, procedure.getName()),
	                "%").getSourceStatement();
        	}
        	else {
        		String queryFunctionString = "select * from mysql.proc where db='%s' and type='FUNCTION' and name='%s'";
        		return session.prepareStatement(String.format(queryFunctionString, owner.getName(), procedure.getName()));
        	}
		}

		@Override
		protected MySQLProcedureParameter fetchChild(JDBCSession session, OceanbaseMySQLCatalog owner, OceanbaseMySQLProcedure parent,
				JDBCResultSet dbResult) throws SQLException, DBException {
			if(parent.getProcedureType().equals(DBSProcedureType.PROCEDURE)) {
				String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
	            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
	            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
	            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
	            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
	            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.LENGTH);
	            boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
	            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
	            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
	            DBSProcedureParameterKind parameterType;
	            switch (columnTypeNum) {
	                case DatabaseMetaData.procedureColumnIn: parameterType = DBSProcedureParameterKind.IN; break;
	                case DatabaseMetaData.procedureColumnInOut: parameterType = DBSProcedureParameterKind.INOUT; break;
	                case DatabaseMetaData.procedureColumnOut: parameterType = DBSProcedureParameterKind.OUT; break;
	                case DatabaseMetaData.procedureColumnReturn: parameterType = DBSProcedureParameterKind.RETURN; break;
	                case DatabaseMetaData.procedureColumnResult: parameterType = DBSProcedureParameterKind.RESULTSET; break;
	                default: parameterType = DBSProcedureParameterKind.UNKNOWN; break;
	            }
	            if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterKind.RETURN) {
	                columnName = "RETURN";
	            }
	            return new MySQLProcedureParameter(
	                    parent,
	                    columnName,
	                    typeName,
	                    valueType,
	                    position,
	                    columnSize,
	                    scale, precision, notNull,
	                        parameterType);
			} else {
				String[] paramList = JDBCUtils.safeGetString(dbResult, "returns").split("\\(");
				int columnSize = Integer.parseInt(paramList[1].split("\\)")[0]);
				
				return new MySQLProcedureParameter(
						parent, 
						"RETURN", 
						paramList[0], 
						STRUCT_ATTRIBUTES, 
						0, 
						columnSize, 
						null, 
						null, 
						true, 
						null);
			}
		}

		@Override
		protected OceanbaseMySQLProcedure fetchObject(JDBCSession session, OceanbaseMySQLCatalog owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			return new OceanbaseMySQLProcedure(owner, resultSet);
		}
		
	}

	public OceanbaseMySQLCatalog(OceanbaseMySQLDataSource dataSource, ResultSet dbResult) {
		super(dataSource, dbResult);
		this.dataSource = dataSource;
	}
	
	public OceanbaseProceduresCache getOceanbaseProceduresCache() {
		return this.oceanbaseProceduresCache;
	}
	
	@Override
    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
        if(!getDataSource().supportsInformationSchema()) {
        	return Collections.emptyList();
        }
        List<MySQLProcedure> objects = new ArrayList<>();
        for(OceanbaseMySQLProcedure oceanbaseMySQLProcedure : oceanbaseProceduresCache.getAllObjects(monitor, this)) {
        	objects.add(oceanbaseMySQLProcedure);
        }
        return objects;
    }
	
	@Override
    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return oceanbaseProceduresCache.getObject(monitor, this, procName);
    }
	
	@Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
		super.refreshObject(monitor);
        oceanbaseProceduresCache.clearCache();
        return this;
    }
	
	@NotNull
    @Override
    public MySQLDataSource getDataSource()
    {
        return (OceanbaseMySQLDataSource)dataSource;
    }
	
	

}
