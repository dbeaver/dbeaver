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
package org.jkiss.dbeaver.model.sql.commands;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.rules.ScriptParameterRule;

/**
 * Control command handler
 */
public class SQLCommandUnset implements SQLControlCommandHandler {

    @NotNull
    @Override
    public SQLControlResult handleCommand(@NotNull DBRProgressMonitor monitor, @NotNull SQLControlCommand command, @NotNull SQLScriptContext scriptContext) throws DBException {
        SQLDialect sqlDialect = scriptContext.getExecutionContext().getDataSource().getSQLDialect();
        
        String parameter = command.getParameter().trim();
        int varNameEnd = ScriptParameterRule.tryConsumeParameterName(sqlDialect, parameter, 0);
        if (varNameEnd != parameter.length()) {
            throw new DBCException("Invalid Unset command. Expected syntax:\n@unset varName");
        }
        
        String varName = SQLCommandSet.prepareVarName(sqlDialect, parameter.substring(0, varNameEnd));
        scriptContext.removeVariable(varName);

        return SQLControlResult.success();
    }

}
