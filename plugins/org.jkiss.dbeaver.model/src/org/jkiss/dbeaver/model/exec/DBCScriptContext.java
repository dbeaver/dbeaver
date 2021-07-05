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
package org.jkiss.dbeaver.model.exec;

import java.io.PrintWriter;
import java.util.List;

/**
 * Script context.
 * The same for series of queries executed as a single script
 */
public interface DBCScriptContext {

    enum VariableType {
        PARAMETER("Parameter"),
        VARIABLE("Variable"),
        QUERY("Query");

        private final String title;

        VariableType(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    class VariableInfo {
        public String name;
        public Object value;
        public VariableType type;

        public VariableInfo(String name, Object value, VariableType type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }
    }

    boolean hasVariable(String name);

    Object getVariable(String name);

    void setVariable(String name, Object value);

    void removeVariable(String name);

    List<VariableInfo> getVariables();

    <T> T getData(String key);

    void setData(String key, Object value);

    PrintWriter getOutputWriter();

    void addListener(DBCScriptContextListener listener);

    void removeListener(DBCScriptContextListener listener);

}
