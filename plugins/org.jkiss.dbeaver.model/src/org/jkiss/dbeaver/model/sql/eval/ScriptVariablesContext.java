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
package org.jkiss.dbeaver.model.sql.eval;

import org.apache.commons.jexl2.JexlContext;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;

/**
 * ScriptVariablesContext
 */
public class ScriptVariablesContext implements JexlContext {

    private final SQLScriptContext scriptContext;

    public ScriptVariablesContext(SQLScriptContext scriptContext) {
        this.scriptContext = scriptContext;
    }

    @Override
    public Object get(String name) {
        return scriptContext.getVariable(name);
    }

    @Override
    public void set(String name, Object value) {
        scriptContext.setVariable(name, value);
    }

    @Override
    public boolean has(String name) {
        return scriptContext.hasVariable(name);
    }
}
