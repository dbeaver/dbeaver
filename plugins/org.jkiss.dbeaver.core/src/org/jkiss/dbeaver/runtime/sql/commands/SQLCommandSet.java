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
package org.jkiss.dbeaver.runtime.sql.commands;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.sql.SQLControlCommand;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.eval.ScriptEvaluateEngine;
import org.jkiss.dbeaver.runtime.sql.SQLControlCommandHandler;

/**
 * Control command handler
 */
public class SQLCommandSet implements SQLControlCommandHandler {

    @Override
    public boolean handleCommand(SQLControlCommand command, SQLScriptContext scriptContext) throws DBException {
        String parameter = command.getParameter();
        int divPos = parameter.indexOf('=');
        if (divPos == -1) {
            throw new DBCException("Bad set syntax. Expected syntax:\n@set varName = value or expression");
        }
        String varName = parameter.substring(0, divPos).trim();
        String varValue = parameter.substring(divPos + 1).trim();
        scriptContext.setVariable(varName, varValue);

        return true;
    }

    ;

}
