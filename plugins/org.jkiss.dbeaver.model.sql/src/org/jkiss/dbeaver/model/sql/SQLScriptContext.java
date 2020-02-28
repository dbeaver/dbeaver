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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCScriptContext;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandHandlerDescriptor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL script execution context
 */
public class SQLScriptContext implements DBCScriptContext {

    private final Map<String, Object> variables = new HashMap<>();
    private final Map<String, Object> defaultParameters = new HashMap<>();
    private final Map<String, Object> pragmas = new HashMap<>();
    private Map<String, Object> statementPragmas;

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
        Object value = variables.get(name);
        if (value == null && parentContext != null) {
            value = parentContext.getVariable(name);
        }
        return value;
    }

    @Override
    public void setVariable(String name, Object value) {
        variables.put(name, value);
        if (parentContext != null) {
            parentContext.setVariable(name, value);
        }
    }

    @Override
    public void removeVariable(String name) {
        variables.remove(name);
        if (parentContext != null) {
            parentContext.removeVariable(name);
        }
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables.clear();
        this.variables.putAll(variables);
    }

    public Object getParameterDefaultValue(String name) {
        return defaultParameters.get(name);
    }

    public void setParameterDefaultValue(String name, Object value) {
        defaultParameters.put(name, value);
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
        Map<String, Object> params = new LinkedHashMap<>();
        params.putAll(defaultParameters);
        params.putAll(variables);
        return params;
    }

}
