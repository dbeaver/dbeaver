/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLGeneratorProcedureCheck extends SQLGeneratorProcedure {
	
	public void generateStoredProcedureCall(StringBuilder sql, DBSProcedure proc, Collection<? extends DBSProcedureParameter> parameters) {
        List<DBSProcedureParameter> inParameters = new ArrayList<>();
        if (parameters != null) {
            inParameters.addAll(parameters);
        }
        sql.append("select * from plpgsql_check_function('" + proc.getFullyQualifiedName(DBPEvaluationContext.DML) + "(");
        if (!inParameters.isEmpty()) {
            boolean first = true;
            for (DBSProcedureParameter parameter : inParameters) {
                switch (parameter.getParameterKind()) {
                    case IN:
                        if (!first) {
                            sql.append(",");
                        }
                        String typeName = parameter.getParameterType().getFullTypeName();
                        sql.append(typeName);
                        break;
                    case RETURN:
                        continue;
                    default:
                        //if (isStoredProcedureCallIncludesOutParameters()) {
                        //    if (!first) {
                        //        sql.append(",");
                        //    }
                        //    sql.append("?");
                        //}
                        //break;
                        continue;
                }
                first = false;
            }
        }
        sql.append(")')");
        sql.append(";");
        sql.append("\n\n");
    }

    @Override
    protected void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSProcedure proc) throws DBException {
        Collection<? extends DBSProcedureParameter> parameters = proc.getParameters(monitor);
        DBPDataSource dataSource = proc.getDataSource();
        {
        	generateStoredProcedureCall(sql, proc, CommonUtils.safeCollection(parameters));
        }
    }
}
