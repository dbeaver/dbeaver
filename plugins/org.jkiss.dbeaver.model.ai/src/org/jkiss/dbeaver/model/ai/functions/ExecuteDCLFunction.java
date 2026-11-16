/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.functions;

import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.grant.Grant;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExecuteDCLFunction extends ExecuteSQLFunction {
    @NotNull
    @Override
    public AIFunctionResult callFunction(@NotNull AIFunctionContext context, @NotNull Map<String, Object> parameters) throws DBException {
        return completionResult(executeQueries(context, parameters, null, 0));
    }

    @Override
    protected void verifyStatement(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull SQLQuery query
    ) throws DBException {
        if (query.getStatement() instanceof Grant grant) {
            if (CommonUtils.isEmpty(grant.getObjectNameParts())) {
                verifyDatasourceScope(context);
            } else {
                verifyDefinitionObject(context, executionContext, String.join(".", grant.getObjectNameParts()), "OBJECT", false);
            }
        } else if (query.getStatement() instanceof SetStatement set) {
            String name = CommonUtils.toString(set.getName());
            requireStatement(set.getCount() == 1 && (name.equalsIgnoreCase("ROLE")
                || name.equalsIgnoreCase("AUTHORIZATION") && "SESSION".equalsIgnoreCase(set.getEffectParameter())), SQLQueryCategory.DCL);
            verifyDatasourceScope(context);
        } else {
            requireStatement(false, SQLQueryCategory.DCL);
        }
    }

    @Override
    protected void verifyQueryText(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String queryText
    ) throws DBException {
        List<String> tokens = tokenize(queryText);
        String command = firstKeyword(tokens);
        boolean defaultPrivileges = tokens.size() > 2 && command.equals("ALTER") && tokens.get(1).equalsIgnoreCase("DEFAULT")
            && tokens.get(2).equalsIgnoreCase("PRIVILEGES");
        boolean roleChange = tokens.size() > 1 && command.equals("SET") && (tokens.get(1).equalsIgnoreCase("ROLE")
            || tokens.size() > 2 && tokens.get(1).equalsIgnoreCase("SESSION") && tokens.get(2).equalsIgnoreCase("AUTHORIZATION"));
        requireStatement(Set.of("GRANT", "REVOKE", "DENY").contains(command) || defaultPrivileges || roleChange, SQLQueryCategory.DCL);
        boolean foundTarget = false;
        for (int i = 1; i < tokens.size(); i++) {
            if (!tokens.get(i).equalsIgnoreCase("ON") && !tokens.get(i).equalsIgnoreCase("SCHEMA")) {
                continue;
            }
            String type = tokens.get(i).equalsIgnoreCase("SCHEMA") ? "SCHEMA" : "OBJECT";
            int start = i + 1;
            if (start < tokens.size() && Set.of("ALL", "TABLES", "SEQUENCES", "FUNCTIONS", "PROCEDURES", "ROUTINES", "TYPES")
                .contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
                continue;
            }
            if (start < tokens.size() && Set.of("OBJECT", "TABLE", "VIEW", "SCHEMA", "DATABASE", "SEQUENCE", "FUNCTION", "PROCEDURE", "TYPE")
                .contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
                type = tokens.get(start++).toUpperCase(Locale.ROOT);
            }
            if (start + 1 < tokens.size() && tokens.get(start).equals(":") && tokens.get(start + 1).equals(":")) {
                start += 2;
            }
            Identifier target = readIdentifier(tokens, start);
            while (target != null) {
                foundTarget = true;
                verifyDefinitionObject(context, executionContext, target.name(), type, false);
                start = target.end();
                if (start < tokens.size() && tokens.get(start).equals("(")) {
                    start = skipParentheses(tokens, start);
                }
                if (start >= tokens.size() || !tokens.get(start).equals(",")) {
                    break;
                }
                target = readIdentifier(tokens, start + 1);
            }
        }
        if (!foundTarget) {
            verifyDatasourceScope(context);
        }
    }
}
