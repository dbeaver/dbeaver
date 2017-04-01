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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;

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

    private final PrintWriter outputWriter;

    public SQLScriptContext(Writer outputWriter) {
        this.outputWriter = new PrintWriter(outputWriter);
    }

    @NotNull
    public Map<String, Object> getVariables() {
        return variables;
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

}
