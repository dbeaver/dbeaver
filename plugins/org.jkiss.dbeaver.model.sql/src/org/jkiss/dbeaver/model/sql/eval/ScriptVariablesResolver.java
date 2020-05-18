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
package org.jkiss.dbeaver.model.sql.eval;

import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.utils.CommonUtils;

/**
 * ScriptVariablesResolver
 */
public class ScriptVariablesResolver implements IVariableResolver {

    private final SQLScriptContext scriptContext;

    public ScriptVariablesResolver(SQLScriptContext scriptContext) {
        this.scriptContext = scriptContext;
    }

    @Override
    public String get(String name) {
        return CommonUtils.toString(scriptContext.getVariable(name));
    }
}
