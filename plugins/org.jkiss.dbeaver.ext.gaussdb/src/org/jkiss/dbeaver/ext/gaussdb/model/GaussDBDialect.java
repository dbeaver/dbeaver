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

package org.jkiss.dbeaver.ext.gaussdb.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDialect;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class GaussDBDialect extends PostgreDialect {

    @Override
    public boolean isDelimiterAfterBlock() {
        return true;
    }

    @Override
    public void generateStoredProcedureCall(StringBuilder sql, DBSProcedure proc,
        Collection<? extends DBSProcedureParameter> parameters, boolean castParams) {
        List<DBSProcedureParameter> inParameters = new ArrayList<>();
        if (parameters != null) {
            inParameters.addAll(parameters);
        }
        DBPPreferenceStore prefStore;
        DBPDataSource dataSource = proc.getDataSource();
        if (dataSource != null) {
            prefStore = dataSource.getContainer().getPreferenceStore();
        } else {
            prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        }
        String namedParameterPrefix = prefStore.getString(ModelPreferences.SQL_NAMED_PARAMETERS_PREFIX);
        boolean useBrackets = useBracketsForExec(proc);
        if (useBrackets)
            sql.append("{ ");
        sql.append(getStoredProcedureCallInitialClause(proc)).append("(");
        if (!inParameters.isEmpty()) {
            inParametersProc(sql, castParams, inParameters, namedParameterPrefix);
        }
        sql.append(")");
        String callEndClause = getProcedureCallEndClause(proc);
        if (!CommonUtils.isEmpty(callEndClause)) {
            sql.append(" ").append(callEndClause);
        }
        if (!useBrackets) {
            sql.append(";");
        } else {
            sql.append(" }");
        }
        sql.append("\n\n");
    }

    private void inParametersProc(StringBuilder sql, boolean castParams, List<DBSProcedureParameter> inParameters,
        String namedParameterPrefix) {
        boolean first = true;
        for (DBSProcedureParameter parameter : inParameters) {
            String typeName = parameter.getParameterType().getFullTypeName();
            switch (parameter.getParameterKind())
            {
            case INOUT:
            case IN:
                if (!first) {
                    sql.append(", ");
                }
                if (castParams) {
                    sql.append("cast(").append(namedParameterPrefix).append(CommonUtils.escapeIdentifier(parameter.getName()))
                        .append(" as ").append(typeName).append(")");
                } else {
                    sql.append(namedParameterPrefix).append(CommonUtils.escapeIdentifier(parameter.getName()));
                }
                break;
            case RETURN:
                continue;
            default:
                if (isStoredProcedureCallIncludesOutParameters()) {
                    if (!first) {
                        sql.append(", ");
                    }
                    if (castParams) {
                        sql.append("cast(?").append(" as ").append(typeName).append(")");
                    } else {
                        sql.append("?");
                    }
                }
                break;
            }
            first = false;
        }
    }
}
