/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLServerDialect extends JDBCSQLDialect {

    private static final String[][] TSQL_BEGIN_END_BLOCK = new String[][]{
        /*{
            "BEGIN^TRANSACTION", "END"
        }*/
    };

    public static final String[][] SQLSERVER_QUOTE_STRINGS = {
            {"[", "]"},
            {"\"", "\""},
    };

    private static String[] EXEC_KEYWORDS =  { "CALL", "EXEC" };

    private JDBCDataSource dataSource;

    public SQLServerDialect() {
        super("SQLServer");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        addSQLKeyword("TOP");
        this.dataSource = dataSource;
    }

    @Override
    public String getScriptDelimiter() {
        return "GO";
    }

    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @Override
    public boolean isDelimiterAfterQuery() {
        return true;
    }

    @Override
    public boolean supportsSubqueries() {
        return true;
    }

    public String[][] getIdentifierQuoteStrings() {
        return SQLSERVER_QUOTE_STRINGS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return TSQL_BEGIN_END_BLOCK;
    }

    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        if (SQLServerUtils.isDriverSqlServer(dataSource.getContainer().getDriver())) {
            if (dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2008_VERSION_MAJOR, 0)) {
                return MultiValueInsertMode.GROUP_ROWS;
            }
            return super.getMultiValueInsertMode();
        } else {
            return super.getMultiValueInsertMode();
        }
    }

    @Override
    public String getColumnTypeModifiers(DBPDataSource dataSource, DBSTypedObject column, String typeName, DBPDataKind dataKind) {
        if (dataKind == DBPDataKind.DATETIME) {
            if (SQLServerConstants.TYPE_DATETIME2.equalsIgnoreCase(typeName)) {
                Integer scale = column.getScale();
                if (scale != null && scale != 0) {
                    return "(" + scale + ')';
                }
            }
        } else if (dataKind == DBPDataKind.STRING) {
            if (SQLServerConstants.TYPE_UNIQUEIDENTIFIER.equalsIgnoreCase(typeName)) {
                return null;
            }
            long maxLength = column.getMaxLength();
            if (maxLength == 0) {
                return null;
            } else if (maxLength == -1) {
                return "(MAX)";
            } else {
                return "(" + maxLength + ")";
            }
        }
        return super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
    }

    @Override
    public void generateStoredProcedureCall(StringBuilder sql, DBSProcedure proc, Collection<? extends DBSProcedureParameter> parameters) {
        List<DBSProcedureParameter> inParameters = new ArrayList<>();
        int maxParamLength = getMaxParameterLength(parameters, inParameters);
        String schemaName = proc.getContainer().getParentObject().getName();
        sql.append("USE [").append(schemaName).append("]\n");
        sql.append("GO\n\n");
        sql.append("DECLARE	@return_value int\n\n");
        sql.append("EXEC\t@return_value = [").append(proc.getContainer().getName()).append("].[").append(proc.getName()).append("]\n");
        for (int i = 0; i < inParameters.size(); i++) {
            String name = inParameters.get(i).getName();
            sql.append("\t\t").append(name).append(" = :").append(CommonUtils.escapeIdentifier(name));
            if (i < (inParameters.size() - 1)) {
                sql.append(", ");
            } else {
                sql.append(" ");
            }
            int width = maxParamLength + 70 - name.length()/2;
            String typeName = inParameters.get(i).getParameterType().getFullTypeName();
            sql.append(CommonUtils.fixedLengthString("-- put the " + name + " parameter value instead of '?' (" + typeName + ")\n", width));
        }
        sql.append("\nSELECT\t'Return Value' = @return_value\n\n");
        sql.append("GO\n\n");
    }
}
