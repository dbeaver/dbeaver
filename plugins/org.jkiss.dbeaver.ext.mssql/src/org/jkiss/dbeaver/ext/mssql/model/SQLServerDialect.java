/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
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
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class SQLServerDialect extends JDBCSQLDialect {

    private static final String[][] TSQL_BEGIN_END_BLOCK = new String[][]{
        /*{
            "BEGIN^TRANSACTION", "END"
        }*/
    };

    private static String[] SQLSERVER_EXTRA_KEYWORDS = new String[]{
        "TOP",
        "SYNONYM",
    };

    private static final String[][] SQLSERVER_QUOTE_STRINGS = {
            {"[", "]"},
            {"\"", "\""},
    };
    private static final String[][] SYBASE_LEGACY_QUOTE_STRINGS = {
        {"\"", "\""},
    };


    private static String[] EXEC_KEYWORDS =  { "CALL", "EXEC" };

    private static String[] PLAIN_TYPE_NAMES = {
        SQLServerConstants.TYPE_GEOGRAPHY,
        SQLServerConstants.TYPE_GEOMETRY,
        SQLServerConstants.TYPE_TIMESTAMP,
        SQLServerConstants.TYPE_IMAGE,
    };

    private static String[] SQLSERVER_FUNCTIONS_DATETIME = new String[]{
            "CURRENT_TIMEZONE",
            "DATEPART",
            "DATEADD",
            "DATEDIFF",
            "DATEDIFF_BIG",
            "DATEFROMPARTS",
            "DATENAME",
            "DATETIMEFROMPARTS",
            "EOMONTH",
            "GETDATE",
            "GETUTCDATE",
            "ISDATE",
            "SYSDATETIMEOFFSET",
            "SYSUTCDATETIME",
            "SMALLDATETIMEFROMPARTS",
            "SWITCHOFFSET",
            "TIMEFROMPARTS",
            "TODATETIMEOFFSET"
    };

    private JDBCDataSource dataSource;
    private boolean isSqlServer;

    public SQLServerDialect() {
        super("SQLServer");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        super.addSQLKeywords(Arrays.asList(SQLSERVER_EXTRA_KEYWORDS));
        this.dataSource = dataSource;
        this.isSqlServer = SQLServerUtils.isDriverSqlServer(dataSource.getContainer().getDriver());

        addFunctions(Arrays.asList(SQLSERVER_FUNCTIONS_DATETIME));
    }

    @NotNull
    @Override
    public String getScriptDelimiter() {
        return "GO";
    }

    @Override
    public boolean validIdentifierPart(char c, boolean quoted) {
        // SQL Server: All extra characters can be used in unquoted form
        return Character.isLetter(c) || Character.isDigit(c) || c == '_' || validCharacters.indexOf(c) != -1;

    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return EXEC_KEYWORDS;
    }

    @NotNull
    @Override
    public String[] getParametersPrefixes() {
        return super.getParametersPrefixes();
        // Do not use @ as prefix - it can be used as a regular SQL construct (#5674)
        //return new String[] { "@" };
    }

    @Override
    public boolean isDelimiterAfterQuery() {
        return isSqlServer;
    }

    @Override
    public boolean supportsSubqueries() {
        return true;
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    public String[][] getIdentifierQuoteStrings() {
        if (dataSource == null || (!isSqlServer && !dataSource.isServerVersionAtLeast(12, 6))) {
            // Old Sybase doesn't support square brackets - #7755
            return SYBASE_LEGACY_QUOTE_STRINGS;
        }
        return SQLSERVER_QUOTE_STRINGS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return TSQL_BEGIN_END_BLOCK;
    }

    @NotNull
    @Override
    public MultiValueInsertMode getMultiValueInsertMode() {
        if (isSqlServer) {
            if (dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2008_VERSION_MAJOR, 0)) {
                return MultiValueInsertMode.GROUP_ROWS;
            }
            return super.getMultiValueInsertMode();
        } else {
            return super.getMultiValueInsertMode();
        }
    }

    @Override
    public String getColumnTypeModifiers(DBPDataSource dataSource, @NotNull DBSTypedObject column, @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        if (dataKind == DBPDataKind.DATETIME) {
            if (SQLServerConstants.TYPE_DATETIME2.equalsIgnoreCase(typeName) ||
                    SQLServerConstants.TYPE_TIME.equalsIgnoreCase(typeName) ||
                    SQLServerConstants.TYPE_DATETIMEOFFSET.equalsIgnoreCase(typeName)) {
                Integer scale = column.getScale();
                if (scale != null && scale != 0) {
                    return "(" + scale + ')';
                }
            }
        } else if (dataKind == DBPDataKind.STRING) {
            switch (typeName) {
                case SQLServerConstants.TYPE_CHAR:
                case SQLServerConstants.TYPE_NCHAR:
                case SQLServerConstants.TYPE_VARCHAR:
                case SQLServerConstants.TYPE_NVARCHAR:
                case SQLServerConstants.TYPE_SQL_VARIANT:
                case SQLServerConstants.TYPE_VARBINARY:{
                    long maxLength = column.getMaxLength();
                    if (maxLength == 0) {
                        return null;
                    } else if (maxLength == -1) {
                        return "(MAX)";
                    } else {
                        return "(" + maxLength + ")";
                    }
                }
                case SQLServerConstants.TYPE_TEXT:
                case SQLServerConstants.TYPE_NTEXT:
                    // text and ntext don't have max length
                default:
                    return null;
            }
        } else if (ArrayUtils.contains(PLAIN_TYPE_NAMES , typeName)) {
            return null;
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
