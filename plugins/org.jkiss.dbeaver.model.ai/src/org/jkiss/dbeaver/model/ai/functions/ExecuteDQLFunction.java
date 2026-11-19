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

import net.sf.jsqlparser.statement.select.Select;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.ai.utils.AIDataExportUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecuteDQLFunction extends ExecuteSQLFunction {
    public static final String MAX_ROWS_PARAM = "maxRows";
    public static final String FORMAT_PARAM = "format";

    @NotNull
    @Override
    public AIFunctionResult callFunction(@NotNull AIFunctionContext context, @NotNull Map<String, Object> parameters) throws DBException {
        ExecutionResult result = executeQueries(context, parameters,
            AIDataExportUtils.getProcessor(CommonUtils.toString(parameters.get(FORMAT_PARAM), "csv")),
            CommonUtils.toInt(parameters.get(MAX_ROWS_PARAM), 10));
        return new AIFunctionResult(getResultType(), result.output());
    }

    @Override
    protected void verifyStatement(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull SQLQuery query
    ) throws DBException {
        requireStatement(query.getStatement() instanceof Select && !query.isModifying(), SQLQueryCategory.DQL);
        verifyStatementObjects(context, executionContext, query.getStatement());
    }

    @Override
    protected void verifyQueryText(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String queryText
    ) throws DBException {
        List<String> tokens = tokenize(executionContext.getDataSource().getSQLDialect(), queryText);
        requireStatement(Set.of("SELECT", "VALUES", "TABLE").contains(firstKeyword(tokens)) && !containsKeyword(tokens, Set.of(
            "INSERT", "UPDATE", "DELETE", "MERGE", "INTO", "CREATE", "ALTER", "DROP", "TRUNCATE", "RENAME", "COMMENT",
            "CALL", "EXEC", "EXECUTE", "COPY", "LOAD", "GRANT", "REVOKE", "DENY", "REPLACE", "UPSERT", "DO"
        )), SQLQueryCategory.DQL);
        verifyTextObjects(context, executionContext, tokens, Set.of("FROM", "JOIN", "TABLE"));
    }
}
