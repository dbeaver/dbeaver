/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.SQLControlCommand;
import org.jkiss.dbeaver.model.sql.SQLControlCommandHandler;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.sql.parser.rules.ScriptParameterRule;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * Control command handler
 */
public class SQLCommandSet implements SQLControlCommandHandler {

    @Override
    public boolean handleCommand(SQLControlCommand command, SQLScriptContext scriptContext) throws DBException {
        SQLDialect sqlDialect = scriptContext.getExecutionContext().getDataSource().getSQLDialect();
        String parameter = command.getParameter().stripLeading();
        int varNameEnd = ScriptParameterRule.tryConsumeParameterName(sqlDialect, parameter, 0);
        if (varNameEnd == -1) {
            throw new DBCException("Missing variable name. Expected syntax:\n@set varName = value or expression");
        }
        String varName = prepareVarName(sqlDialect, parameter.substring(0, varNameEnd));
        int divPos = parameter.indexOf('=', varNameEnd);
        if (divPos == -1) {
            throw new DBCException("Bad set syntax. Expected syntax:\n@set varName = value or expression");
        }
        String shouldBeEmpty = parameter.substring(varNameEnd, divPos).trim();
        if (shouldBeEmpty.length() > 0) {
            throw new DBCException(
                "Unexpected characters " + shouldBeEmpty + " after the variable name " + varName + ". " +
                "Expected syntax:\n@set varName = value or expression"
            );
        }
        String varValue = parameter.substring(divPos + 1).trim();
        varValue = GeneralUtils.replaceVariables(varValue, name -> CommonUtils.toString(scriptContext.getVariable(name)));
        scriptContext.setVariable(varName, varValue);

        return true;
    }

    /*
     * Unquotes variable name if it was quoted, otherwise converts case to upper
     */
    @NotNull
    public static String prepareVarName(@NotNull SQLDialect sqlDialect, @NotNull String rawName) {
        if (sqlDialect.isQuotedIdentifier(rawName)) {
            return sqlDialect.getUnquotedIdentifier(rawName, true);
        } else {
            return rawName.toUpperCase(Locale.ENGLISH);
        }
    }
}
