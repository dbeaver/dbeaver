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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.sql.SQLParametersProvider;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.SQLQueryParameterBindDialog;

import java.util.List;

/**
 * SQL Editor params provider
 */
public class SQLEditorParametersProvider implements SQLParametersProvider {

    private IWorkbenchPartSite site;

    public SQLEditorParametersProvider(IWorkbenchPartSite site) {
        this.site = site;
    }

    @Override
    public Boolean prepareStatementParameters(@NotNull SQLScriptContext scriptContext, @NotNull SQLQuery sqlStatement, @NotNull List<SQLQueryParameter> parameters, boolean useDefaults) {
        for (SQLQueryParameter param : parameters) {
            String paramName = param.getVarName();
            Object defValue = useDefaults ? scriptContext.getParameterDefaultValue(paramName) : null;
            if (defValue != null || scriptContext.hasVariable(paramName)) {
                Object varValue = defValue != null ? defValue : scriptContext.getVariable(paramName);
                String strValue = varValue == null ? null : varValue.toString();
                param.setValue(strValue);
                param.setVariableSet(true);
            } else {
                if (!useDefaults) {
                    param.setVariableSet(false);
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

        int paramsResult = UITask.run(() -> {
            SQLQueryParameterBindDialog dialog = new SQLQueryParameterBindDialog(
                site,
                sqlStatement,
                parameters);
            return dialog.open();
        });

        if (paramsResult == IDialogConstants.OK_ID) {
            // Save values back to script context
            for (SQLQueryParameter param : parameters) {
                if (param.isNamed()) {
                    String strValue = param.getValue();
                    if (scriptContext.hasVariable(param.getVarName())) {
                        scriptContext.setVariable(param.getVarName(), strValue);
                    } else {
                        scriptContext.setParameterDefaultValue(param.getVarName(), strValue);
                    }
                }
            }
            return true;
        } else if (paramsResult == IDialogConstants.IGNORE_ID) {
            scriptContext.setIgnoreParameters(true);
            return null;
        }
        return false;
    }

}
