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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContextListener;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.impl.OutputWriterAdapter;
import org.jkiss.dbeaver.model.impl.sql.AbstractSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.sql.registry.SQLQueryParameterRegistry;
import org.jkiss.dbeaver.model.sql.registry.SQLVariablesRegistry;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

/**
 * SQL script execution context
 */
public class SQLScriptContext implements DBCScriptContext {

    private final Map<String, VariableInfo> variables = new LinkedHashMap<>();
    private final Map<String, Object> defaultParameters = new HashMap<>();
    private final Map<String, Map<String, Object>> pragmas = new HashMap<>();

    private DBCScriptContextListener[] listeners = null;

    private final Map<String, Object> data = new HashMap<>();

    @Nullable
    private final SQLScriptContext parentContext;
    @NotNull
    private final DBPContextProvider contextProvider;
    @Nullable
    private final Path sourceFile;
    @NotNull
    private final DBCOutputWriter outputWriter;

    private final SQLParametersProvider parametersProvider;
    private boolean ignoreParameters;
    private IVariableResolver contextVarResolver;

    public SQLScriptContext(
        @Nullable SQLScriptContext parentContext,
        @NotNull DBPContextProvider contextProvider,
        @Nullable Path sourceFile,
        @NotNull Writer outputWriter,
        @Nullable SQLParametersProvider parametersProvider)
    {
        this(parentContext, contextProvider, sourceFile, new OutputWriterAdapter(new PrintWriter(outputWriter)), parametersProvider);
    }

    public SQLScriptContext(
        @Nullable SQLScriptContext parentContext,
        @NotNull DBPContextProvider contextProvider,
        @Nullable Path sourceFile,
        @NotNull DBCOutputWriter outputWriter,
        @Nullable SQLParametersProvider parametersProvider
    ) {
        this.parentContext = parentContext;
        this.contextProvider = contextProvider;
        this.sourceFile = sourceFile;
        this.outputWriter = outputWriter;
        this.parametersProvider = parametersProvider;

        DBCExecutionContext executionContext = contextProvider.getExecutionContext();
        DBPDataSourceContainer dataSourceContainer = executionContext == null ? null : executionContext.getDataSource().getContainer();
        this.contextVarResolver = new DataSourceVariableResolver(
            dataSourceContainer,
            dataSourceContainer == null ? null : dataSourceContainer.getConnectionConfiguration());
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
    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public boolean hasVariable(String name) {
        if (variables.containsKey(getNormalizedVarName(name))) {
            return true;
        }
        if (parentContext != null) {
            if (parentContext.hasVariable(name)) {
                return true;
            }
        }
        return contextVarResolver.get(name) != null;
    }

    @Override
    public boolean hasDefaultParameterValue(String name) {
        if (defaultParameters.containsKey(getNormalizedVarName(name))){
            return true;
        }
        return parentContext != null && parentContext.hasDefaultParameterValue(name);
    }

    @Override
    public Object getVariable(String name) {
        VariableInfo variableInfo = variables.get(getNormalizedVarName(name));
        if (variableInfo != null) {
            return variableInfo.value;
        }
        if (parentContext != null) {
            Object value = parentContext.getVariable(name);
            if (value != null) {
                return value;
            }
        }
        return contextVarResolver.get(name);
    }

    @Override
    public void setVariable(String name, Object value) {
        VariableInfo v = new VariableInfo(name, value, VariableType.VARIABLE);
        VariableInfo ov = variables.put(getNormalizedVarName(name), v);

        notifyListeners(ov == null ? DBCScriptContextListener.ContextAction.ADD : DBCScriptContextListener.ContextAction.UPDATE, v);

        if (parentContext != null) {
            parentContext.setVariable(name, value);
        }
    }

    @Override
    public void removeVariable(String name) {
        VariableInfo v = variables.remove(getNormalizedVarName(name));
        if (v != null) {
            notifyListeners(DBCScriptContextListener.ContextAction.DELETE, v);
        }

        if (parentContext != null) {
            parentContext.removeVariable(name);
        }
    }

    @Override
    public void removeDefaultParameterValue(String name) {
        final SQLQueryParameterRegistry instance = SQLQueryParameterRegistry.getInstance();
        Object p = defaultParameters.remove(getNormalizedVarName(name));
        instance.deleteParameter(name);
        instance.save();
        notifyListeners(DBCScriptContextListener.ContextAction.DELETE, name, p);
        if (parentContext != null) parentContext.removeDefaultParameterValue(name);
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
                getNormalizedVarName(ve.getKey()),
                v);

            notifyListeners(ov == null ? DBCScriptContextListener.ContextAction.ADD : DBCScriptContextListener.ContextAction.UPDATE, v);
        }

        if (parentContext != null) {
            parentContext.setVariables(variables);
        }
    }

    public Object getParameterDefaultValue(String name) {
        return defaultParameters.get(getNormalizedVarName(name));
    }

    public void setParameterDefaultValue(String name, Object value) {
        Object op = defaultParameters.put(getNormalizedVarName(name), value);

        notifyListeners(op == null ? DBCScriptContextListener.ContextAction.ADD : DBCScriptContextListener.ContextAction.UPDATE, name, value);

        if (parentContext != null) {
            parentContext.setParameterDefaultValue(name, value);
        }
    }

    @NotNull
    public Map<String, Map<String, Object>> getPragmas() {
        if (parentContext != null) {
            return parentContext.getPragmas();
        } else {
            return pragmas;
        }
    }

    public void setPragma(@NotNull String id, @Nullable Map<String, Object> params) {
        if (parentContext != null) {
            parentContext.setPragma(id, params);
        } else {
            pragmas.put(id, params);
        }
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
    public DBCOutputWriter getOutputWriter() {
        return outputWriter;
    }

    public void clearStatementContext() {
        pragmas.clear();
    }

    public boolean isIgnoreParameters() {
        return ignoreParameters;
    }

    public void setIgnoreParameters(boolean ignoreParameters) {
        this.ignoreParameters = ignoreParameters;
    }

    @NotNull
    public SQLControlResult executeControlCommand(DBRProgressMonitor monitor, SQLControlCommand command) throws DBException {
        if (command.isEmptyCommand()) {
            return SQLControlResult.success();
        }
        SQLCommandHandlerDescriptor commandHandler = SQLCommandsRegistry.getInstance().getCommandHandler(command.getCommandId());
        if (commandHandler == null) {
            throw new DBException("Command '" + command.getCommand() + "' not supported");
        }
        return commandHandler.createHandler().handleCommand(monitor, command, this);
    }

    public void copyFrom(SQLScriptContext context) {
        this.variables.clear();
        this.variables.putAll(context.variables);

        this.data.clear();
        this.data.putAll(context.data);

        this.pragmas.clear();
        this.pragmas.putAll(context.pragmas);
    }

    public boolean fillQueryParameters(
        @NotNull SQLQuery query,
        @NotNull Supplier<DBDDataReceiver> dataReceiverSupplier,
        boolean useDefaults
    ) {
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
            Boolean paramsResult = parametersProvider.prepareStatementParameters(
                this,
                query,
                parameters,
                dataReceiverSupplier,
                useDefaults
            );
            if (paramsResult == null) {
                ignoreParameters = true;
                return true;
            } else if (!paramsResult) {
                return false;
            }
        } else {
            for (SQLQueryParameter parameter : parameters) {
                String normalizedVarName = getNormalizedVarName(parameter.getVarName());
                String normalizedParamName = getNormalizedVarName(parameter.getName());
                Object varValue = variables.get(normalizedVarName);
                if (varValue == null) {
                    varValue = variables.get(normalizedParamName);
                }
                if (varValue == null) {
                    varValue = defaultParameters.get(normalizedParamName);
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
        for (VariableInfo v : variables.values()) {
            params.put(v.name, v.value);
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
                variables.put(getNormalizedVarName(v.name), v);
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

    private String getNormalizedVarName(String name) {
        String unquoted = null;
        if (contextProvider.getExecutionContext() != null) {
            unquoted = DBUtils.getUnQuotedIdentifier(contextProvider.getExecutionContext().getDataSource(), name);
        } else {
            unquoted = DBUtils.getUnQuotedIdentifier(name, AbstractSQLDialect.DEFAULT_IDENTIFIER_QUOTES);
        }
        if (!unquoted.equals(name)) {
            return unquoted;
        } else {
            // Convert unquoted identifiers to uppercase
            return name.toUpperCase();
        }
    }

}
