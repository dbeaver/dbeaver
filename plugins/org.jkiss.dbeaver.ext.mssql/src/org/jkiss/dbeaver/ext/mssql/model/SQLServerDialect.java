/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.text.rules.IRule;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.runtime.sql.SQLRuleProvider;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLServerDialect extends GenericSQLDialect implements SQLRuleProvider {

    private static final String[][] TSQL_BEGIN_END_BLOCK = new String[][]{
        /*{
            "BEGIN^TRANSACTION", "END"
        }*/
    };

    public static final String[][] SQLSERVER_QUOTE_STRINGS = {
            {"[", "]"},
            {"\"", "\""},
    };

    public SQLServerDialect() {
        super("SQLServer");
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        addSQLKeyword("TOP");
    }

    public String[][] getIdentifierQuoteStrings() {
        return SQLSERVER_QUOTE_STRINGS;
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return TSQL_BEGIN_END_BLOCK;
    }

    @Override
    public void extendRules(@NotNull List<IRule> rules, @NotNull RulePosition position) {
    }

    @Override
    public void generateStoredProcedureCall(StringBuilder sql, DBSProcedure proc, Collection<? extends DBSProcedureParameter> parameters) {
        List<DBSProcedureParameter> inParameters = new ArrayList<>();
        int maxParamLength = getMaxParameterLength(parameters, inParameters);
        String schemaName = proc.getParentObject().getName();
        schemaName = proc.getContainer().getParentObject().getName();
        sql.append("USE [" + schemaName + "]\n");
        sql.append("GO\n\n");
        sql.append("DECLARE	@return_value int\n\n");
        sql.append("EXEC\t@return_value = [" + proc.getContainer().getName() +"].[" + proc.getName() + "]\n");
        for (int i = 0; i < inParameters.size(); i++) {
            String name = inParameters.get(i).getName();
            sql.append("\t\t" + name + " = ?");
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
