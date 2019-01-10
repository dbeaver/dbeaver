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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * SQL script execution context
 */
public class SQLScriptContext {

    private final Map<String, Object> variables = new HashMap<>();
    private final Map<String, Object> pragmas = new HashMap<>();

    private final Map<String, Object> data = new HashMap<>();

    @Nullable
    private final SQLScriptContext parentContext;
    @NotNull
    private final DBPContextProvider contextProvider;
    @Nullable
    private final File sourceFile;
    @NotNull
    private final PrintWriter outputWriter;

    public SQLScriptContext(
            @Nullable SQLScriptContext parentContext,
            @NotNull DBPContextProvider contextProvider,
            @Nullable File sourceFile,
            @NotNull Writer outputWriter) {
        this.parentContext = parentContext;
        this.contextProvider = contextProvider;
        this.sourceFile = sourceFile;
        this.outputWriter = new PrintWriter(outputWriter);
    }

    @NotNull
    public DBCExecutionContext getExecutionContext() {
        return contextProvider.getExecutionContext();
    }

    @Nullable
    public File getSourceFile() {
        return sourceFile;
    }

    @NotNull
    public boolean hasVariable(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        return parentContext != null && parentContext.hasVariable(name);
    }

    @NotNull
    public Object getVariable(String name) {
        Object value = variables.get(name);
        if (value == null && parentContext != null) {
            value = parentContext.getVariable(name);
        }
        return value;
    }

    @NotNull
    public void setVariable(String name, Object value) {
        variables.put(name, value);
        if (parentContext != null) {
            parentContext.setVariable(name, value);
        }
    }

    @NotNull
    public Map<String, Object> getPragmas() {
        return pragmas;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public void setData(String key, Object value) {
        this.data.put(key, value);
    }

    public PrintWriter getOutputWriter() {
        return outputWriter;
    }

    public void copyFrom(SQLScriptContext context) {
        this.variables.clear();
        this.variables.putAll(context.variables);

        this.data.clear();
        this.data.putAll(context.data);

        this.pragmas.clear();
        this.pragmas.putAll(context.pragmas);
    }
}
