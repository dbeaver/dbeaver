/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;

import java.util.List;
import java.util.function.Supplier;

public abstract class SQLParametersProviderBase implements SQLParametersProvider {

    @Nullable
    @Override
    public Boolean prepareStatementParameters(
        @NotNull SQLScriptContext scriptContext,
        @NotNull SQLQuery sqlStatement,
        @NotNull List<SQLQueryParameter> parameters,
        @NotNull Supplier<DBDDataReceiver> dataReceiverSupplier,
        boolean useDefaults
    ) {
        for (SQLQueryParameter param : parameters) {
            String paramName = param.getName();
            Object defValue = useDefaults ? scriptContext.getParameterDefaultValue(paramName) : null;
            if (defValue != null || scriptContext.hasVariable(paramName)) {
                assignVariable(scriptContext, param, paramName, defValue);
            } else {
                paramName = param.getVarName();
                defValue = useDefaults ? scriptContext.getParameterDefaultValue(paramName) : null;
                if (defValue != null || scriptContext.hasVariable(paramName)) {
                    assignVariable(scriptContext, param, paramName, defValue);
                } else {
                    if (!useDefaults) {
                        param.setVariableSet(false);
                    }
                }
            }
        }
        boolean allSet = true;
        for (SQLQueryParameter param : parameters) {
            if (!param.isVariableSet()) {
                allSet = false;
            }
        }
        if (allSet) {
            return true;
        }

        return collectAndAssignVariables(scriptContext, sqlStatement, parameters, dataReceiverSupplier);
    }

    @Nullable
    protected abstract Boolean collectAndAssignVariables(
        @NotNull SQLScriptContext scriptContext,
        @NotNull SQLQuery sqlStatement,
        @NotNull List<SQLQueryParameter> parameters,
        @NotNull Supplier<DBDDataReceiver> dataReceiverSupplier
    );

    protected void assignVariable(@NotNull SQLScriptContext scriptContext, SQLQueryParameter param, String paramName, Object defValue) {
        Object varValue = defValue != null ? defValue : scriptContext.getVariable(paramName);
        String strValue = varValue == null ? null : varValue.toString();
        param.setValue(strValue);
        param.setVariableSet(true);
    }
}
