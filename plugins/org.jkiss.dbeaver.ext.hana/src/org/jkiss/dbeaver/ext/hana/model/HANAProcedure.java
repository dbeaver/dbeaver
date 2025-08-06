/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HANAProcedure extends GenericProcedure {

    static final String DATA_TYPE_NAME_TABLE_TYPE = "TABLE_TYPE";
    static final String DATA_TYPE_NAME_ANY_TABLE_TYPE = "ANY_TABLE_TYPE";
    private static final String PARAMETER_TYPE_IN = "IN";
    private static final String PARAMETER_TYPE_INOUT = "INOUT";
    private static final String PARAMETER_TYPE_OUT = "OUT";
    private static final String PARAMETER_TYPE_RETURN = "RETURN";
    
    Map<String, List<HANAInplaceTableTypeColumn> > inplaceTableTypes;

    public HANAProcedure(GenericStructContainer container, String procedureName, String specificName,
            String description, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        super(container, procedureName, specificName, description, procedureType, functionResultType);
    }
    
    private void loadInplaceTableTypes(DBRProgressMonitor monitor) throws DBException {
        inplaceTableTypes = new HashMap<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read procedure parameter columns")) {
            String stmt = "SELECT PARAMETER_NAME, COLUMN_NAME, DATA_TYPE_NAME, LENGTH, SCALE";
            if (DBSProcedureType.PROCEDURE == getProcedureType()) {
                stmt += " FROM SYS.PROCEDURE_PARAMETER_COLUMNS"+
                        " WHERE SCHEMA_NAME=? AND PROCEDURE_NAME=?"+
                        " ORDER BY PARAMETER_NAME, POSITION";
            } else {
                stmt += " FROM SYS.FUNCTION_PARAMETER_COLUMNS"+
                        " WHERE SCHEMA_NAME=? AND FUNCTION_NAME=?"+
                        " ORDER BY PARAMETER_NAME, POSITION";
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
                dbStat.setString(1, getParentObject().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String parameterName = JDBCUtils.safeGetString(dbResult, 1);
                        String columnName = JDBCUtils.safeGetString(dbResult, 2);
                        String typeName = JDBCUtils.safeGetString(dbResult, 3);
                        int length = JDBCUtils.safeGetInt(dbResult, 4);
                        int scale = JDBCUtils.safeGetInt(dbResult, 5);
                        
                        List<HANAInplaceTableTypeColumn> inplaceTableType = inplaceTableTypes.get(parameterName);
                        if (inplaceTableType == null) {
                            inplaceTableType = new LinkedList<HANAInplaceTableTypeColumn>();
                            inplaceTableTypes.put(parameterName, inplaceTableType);
                        }
                        inplaceTableType.add(new HANAInplaceTableTypeColumn(this, columnName, typeName, inplaceTableType.size() + 1, length, scale));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
    }

    @Override
    public void loadProcedureColumns(DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read procedure parameter")) {
            String stmt;
            stmt = "SELECT PARAMETER_NAME, DATA_TYPE_NAME, DATA_TYPE_ID, LENGTH, SCALE, POSITION,"+
                   " TABLE_TYPE_SCHEMA, TABLE_TYPE_NAME, IS_INPLACE_TYPE,"+
                   " PARAMETER_TYPE, HAS_DEFAULT_VALUE";
            if (DBSProcedureType.PROCEDURE == getProcedureType()) {
                stmt += " FROM SYS.PROCEDURE_PARAMETERS"+
                        " WHERE SCHEMA_NAME=? AND PROCEDURE_NAME=?"+
                        " ORDER BY POSITION";
            } else {
                stmt += " FROM SYS.FUNCTION_PARAMETERS"+
                        " WHERE SCHEMA_NAME=? AND FUNCTION_NAME=?"+
                        " ORDER BY POSITION";
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
                dbStat.setString(1, getParentObject().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String columnName = JDBCUtils.safeGetString(dbResult, "PARAMETER_NAME");
                        String typeName = JDBCUtils.safeGetString(dbResult, "DATA_TYPE_NAME");
                        int typeId = JDBCUtils.safeGetInt(dbResult, "DATA_TYPE_ID");
                        int columnSize = JDBCUtils.safeGetInt(dbResult, "LENGTH");
                        int scale = JDBCUtils.safeGetInt(dbResult, "SCALE");
                        int position = JDBCUtils.safeGetInt(dbResult, "POSITION");
                        String parameterTypeStr = JDBCUtils.safeGetString(dbResult, "PARAMETER_TYPE");
                        boolean hasInplaceTableType = JDBCUtils.safeGetBoolean(dbResult, "IS_INPLACE_TYPE", HANAConstants.SYS_BOOLEAN_TRUE);
                        boolean hasDefaultValue = JDBCUtils.safeGetBoolean(dbResult, "HAS_DEFAULT_VALUE", HANAConstants.SYS_BOOLEAN_TRUE);
                        
                        DBSProcedureParameterKind parameterType;
                        switch(parameterTypeStr) {
	                    	case PARAMETER_TYPE_IN:     parameterType = DBSProcedureParameterKind.IN; break; 
	                    	case PARAMETER_TYPE_INOUT:  parameterType = DBSProcedureParameterKind.INOUT; break; 
	                    	case PARAMETER_TYPE_OUT:    parameterType = DBSProcedureParameterKind.OUT; break; 
	                    	case PARAMETER_TYPE_RETURN: parameterType = DBSProcedureParameterKind.RETURN; break; 
	                    	default:                    parameterType = DBSProcedureParameterKind.UNKNOWN; break; 
                        }
                        DBSObject tableType = null;
                        List<HANAInplaceTableTypeColumn> inplaceTableType = null;
                        if(DATA_TYPE_NAME_TABLE_TYPE.equals(typeName)) {
                            if (hasInplaceTableType) {
                                if (inplaceTableTypes == null) {
                                    loadInplaceTableTypes(monitor);
                                }
                                inplaceTableType = inplaceTableTypes.get(columnName);
                            } else {
                                String tableTypeSchema = dbResult.getString("TABLE_TYPE_SCHEMA");
                                String tableTypeName = dbResult.getString("TABLE_TYPE_NAME");
                                GenericSchema schema = getDataSource().getSchema(tableTypeSchema);
                                if (schema != null) {
                                    tableType = schema.getTable(monitor, tableTypeName);
                                }
                            }
                        }
                        addColumn(new HANAProcedureParameter(
                                this, columnName, typeName, typeId, position, columnSize, scale, parameterType,
                                tableType, inplaceTableType, hasDefaultValue));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
    }

    @Association
    public List<HANADependency> getDependencies(DBRProgressMonitor monitor) throws DBException {
        return HANADependency.readDependencies(monitor, this);
    }

    @Property(hidden = true, labelProvider = GenericCatalog.CatalogNameTermProvider.class)
    public GenericCatalog getCatalog() { return super.getCatalog(); }

    @Property(hidden = true)
    public GenericPackage getPackage() { return super.getPackage(); }
    
    // hide, as not properly filled by driver. type is anyway obvious from RETURN parameter
    @Property(hidden = true)
    public GenericFunctionResultType getFunctionResultType() { return super.getFunctionResultType(); }

}
