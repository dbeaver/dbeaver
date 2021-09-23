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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContextListener;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.sql.registry.SQLVariablesRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

/**
 * SQL script execution context
 */
public class SQLScriptContext implements DBCScriptContext {

    private final Map<String, VariableInfo> variables = new LinkedHashMap<>();
    private final Map<String, Object> defaultParameters = new HashMap<>();
    private final Map<String, Object> pragmas = new HashMap<>();
    private Map<String, Object> statementPragmas;

    private DBCScriptContextListener[] listeners = null;

    private final Map<String, Object> data = new HashMap<>();

    @Nullable
    private final SQLScriptContext parentContext;
    @NotNull
    private final DBPContextProvider contextProvider;
    @Nullable
    private final File sourceFile;
    @NotNull
    private final PrintWriter outputWriter;

    private SQLParametersProvider parametersProvider;
    private boolean ignoreParameters;

    public SQLScriptContext(
        @Nullable SQLScriptContext parentContext,
        @NotNull DBPContextProvider contextProvider,
        @Nullable File sourceFile,
        @NotNull Writer outputWriter,
        @Nullable SQLParametersProvider parametersProvider)
    {
        this.parentContext = parentContext;
        this.contextProvider = contextProvider;
        this.sourceFile = sourceFile;
        this.outputWriter = new PrintWriter(outputWriter);
        this.parametersProvider = parametersProvider;
    }

    @Nullable
    public SQLScriptContext getParentContext() {
        return parentContext;
    }

    @NotNull
    public DBCExecutionContext getExecutionContext() {
        return contextProvider.getExecutionContext();
    }

    @Nullable
    public File getSourceFile() {
        return sourceFile;
    }

    @Override
    public boolean hasVariable(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        return parentContext != null && parentContext.hasVariable(name);
    }

    @Override
    public Object getVariable(String name) {
        VariableInfo variableInfo = variables.get(name);
        if (variableInfo == null && parentContext != null) {
            return parentContext.getVariable(name);
        }
        return variableInfo == null ? null : variableInfo.value;
    }

    @Override
    public void setVariable(String name, Object value) {
        VariableInfo v = new VariableInfo(name, value, VariableType.VARIABLE);
        VariableInfo ov = variables.put(name, v);

        notifyListeners(ov == null ? DBCScriptContextListener.ContextAction.ADD : DBCScriptContextListener.ContextAction.UPDATE, v);

        if (parentContext != null) {
            parentContext.setVariable(name, value);
        }
    }

    @Override
    public void removeVariable(String name) {
        VariableInfo v = variables.remove(name);
        if (v != null) {
            notifyListeners(DBCScriptContextListener.ContextAction.DELETE, v);
        }

        if (parentContext != null) {
            parentContext.removeVariable(name);
        }
    }

    @Override
    public List<VariableInfo> getVariables() {
        return new ArrayList<>(variables.values());
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables.clear();
        for (Map.Entry<String, Object> ve : variables.entrySet()) {
            VariableInfo v = new VariableInfo(ve.getKey(), ve.getValue(), VariableType.VARIABLE);
            VariableInfo ov = this.variables.put(
                ve.getKey(),
                v);

            notifyListeners(ov == null ? DBCScriptContextListener.ContextAction.ADD : DBCScriptContextListener.ContextAction.UPDATE, v);
        }

        if (parentContext != null) {
            parentContext.setVariables(variables);
        }
    }

    public Object getParameterDefaultValue(String name) {
        return defaultParameters.get(name);
    }

    public void setParameterDefaultValue(String name, Object value) {
        Object op = defaultParameters.put(name, value);

        notifyListeners(op == null ? DBCScriptContextListener.ContextAction.ADD : DBCScriptContextListener.ContextAction.UPDATE, name, value);

        if (parentContext != null) {
            parentContext.setParameterDefaultValue(name, value);
        }
    }

    @NotNull
    public Map<String, Object> getPragmas() {
        return pragmas;
    }

    public void setStatementPragma(String name, Object value) {
        if (statementPragmas == null) {
            statementPragmas = new LinkedHashMap<>();
        }
        statementPragmas.put(name, value);
    }

    public Object getStatementPragma(String name) {
        return statementPragmas == null ? null : statementPragmas.get(name);
    }

    @Override
    public <T> T getData(String key) {
        return (T)data.get(key);
    }

    @Override
    public void setData(String key, Object value) {
        this.data.put(key, value);
    }

    @Override
    @NotNull
    public PrintWriter getOutputWriter() {
        return outputWriter;
    }

    public void clearStatementContext() {
        statementPragmas = null;
    }

    public boolean isIgnoreParameters() {
        return ignoreParameters;
    }

    public void setIgnoreParameters(boolean ignoreParameters) {
        this.ignoreParameters = ignoreParameters;
    }

    public boolean executeControlCommand(SQLControlCommand command) throws DBException {
        if (command.isEmptyCommand()) {
            return true;
        }
        SQLCommandHandlerDescriptor commandHandler = SQLCommandsRegistry.getInstance().getCommandHandler(command.getCommandId());
        if (commandHandler == null) {
            throw new DBException("Command '" + command.getCommand() + "' not supported");
        }
        return commandHandler.createHandler().handleCommand(command, this);
    }

    public void copyFrom(SQLScriptContext context) {
        this.variables.clear();
        this.variables.putAll(context.variables);

        this.data.clear();
        this.data.putAll(context.data);

        this.pragmas.clear();
        this.pragmas.putAll(context.pragmas);
    }

    public boolean fillQueryParameters(SQLQuery query, boolean useDefaults) {
        if (ignoreParameters) {
            return true;
        }

        // Bind parameters
        List<SQLQueryParameter> parameters = query.getParameters();
        if (CommonUtils.isEmpty(parameters)) {
            return true;
        }

        if (parametersProvider != null) {
            // Resolve parameters (only if it is the first fetch)
            Boolean paramsResult = parametersProvider.prepareStatementParameters(this, query, parameters, useDefaults);
            if (paramsResult == null) {
                ignoreParameters = true;
                return true;
            } else if (!paramsResult) {
                return false;
            }
        } else {
            for (SQLQueryParameter parameter : parameters) {
                Object varValue = variables.get(parameter.getVarName());
                if (varValue == null) {
                    varValue = defaultParameters.get(parameter.getVarName());
                } else {
                    varValue = ((VariableInfo)varValue).value;
                }
                if (varValue != null) {
                    parameter.setValue(CommonUtils.toString(varValue));
                }
            }
        }

        SQLUtils.fillQueryParameters(query, parameters);

        return true;
    }

    public Map<String, Object> getAllParameters() {
        Map<String, Object> params = new LinkedHashMap<>(defaultParameters.size() + variables.size());
        params.putAll(defaultParameters);
        for (Map.Entry<String, VariableInfo> v : variables.entrySet()) {
            params.put(v.getKey(), v.getValue().value);
        }
        return params;
    }

    ////////////////////////////////////////////////////
    // Persistence

    public void loadVariables(DBPDriver driver, DBPDataSourceContainer dataSource) {
        synchronized (variables) {
            variables.clear();
            List<VariableInfo> varList;
            if (dataSource != null) {
                varList = SQLVariablesRegistry.getInstance().getDataSourceVariables(dataSource);
            } else if (driver != null) {
                varList = SQLVariablesRegistry.getInstance().getDriverVariables(driver);
            } else {
                varList = new ArrayList<>();
            }
            for (VariableInfo v : varList) {
                variables.put(v.name, v);
            }
        }

    }

    public void saveVariables(DBPDriver driver, DBPDataSourceContainer dataSource) {
        ArrayList<VariableInfo> vCopy;
        synchronized (variables) {
            vCopy = new ArrayList<>(this.variables.values());
        }
        SQLVariablesRegistry.getInstance().updateVariables(driver, dataSource, vCopy);
    }

    public void clearVariables() {
        synchronized (variables) {
            variables.clear();
        }
    }

    ////////////////////////////////////////////////////
    // Listeners

    @Override
    public synchronized void addListener(DBCScriptContextListener listener) {
        if (listeners == null) {
            listeners = new DBCScriptContextListener[] { listener };
        } else {
            listeners = ArrayUtils.add(DBCScriptContextListener.class, listeners, listener);
        }
    }

    @Override
    public synchronized void removeListener(DBCScriptContextListener listener) {
        if (listeners != null) {
            listeners = ArrayUtils.remove(DBCScriptContextListener.class, listeners, listener);
        }
    }

    @Nullable
    private DBCScriptContextListener[] getListenersCopy() {
        synchronized (this) {
            if (listeners != null) {
                return Arrays.copyOf(listeners, listeners.length);
            }
        }
        return null;
    }


    private void notifyListeners(DBCScriptContextListener.ContextAction contextAction, VariableInfo variableInfo) {
        DBCScriptContextListener[] lc = getListenersCopy();
        if (lc != null) {
            for (DBCScriptContextListener l : lc) l.variableChanged(contextAction, variableInfo);
        }
    }
    private void notifyListeners(DBCScriptContextListener.ContextAction contextAction, String paramName, Object paramValue) {
        DBCScriptContextListener[] lc = getListenersCopy();
        if (lc != null) {
            for (DBCScriptContextListener l : lc) l.parameterChanged(contextAction, paramName, paramValue);
        }
    }

}
