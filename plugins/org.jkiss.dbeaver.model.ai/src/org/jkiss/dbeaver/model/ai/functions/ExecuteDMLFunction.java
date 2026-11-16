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

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecuteDMLFunction extends ExecuteSQLFunction {
    @NotNull
    @Override
    public AIFunctionResult callFunction(@NotNull AIFunctionContext context, @NotNull Map<String, Object> parameters) throws DBException {
        ExecutionResult result = executeQueries(context, parameters, null, 0);
        return createResult(result.statistics());
    }

    @NotNull
    AIFunctionResult createResult(@NotNull DBCStatistics statistics) {
        return new AIFunctionResult(getResultType(), statistics.getRowsUpdated());
    }

    @Override
    protected void verifyStatement(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull SQLQuery query
    ) throws DBException {
        Statement statement = query.getStatement();
        requireStatement(statement instanceof Insert || statement instanceof Update || statement instanceof Delete
            || statement instanceof Merge || statement instanceof Upsert || statement instanceof Execute, SQLQueryCategory.DML);
        if (statement instanceof Execute execute) {
            verifyRoutine(context, executionContext, execute.getName());
            if (execute.getExprList() != null) {
                verifyExpressionObjects(context, executionContext, execute.getExprList());
            }
        } else {
            Table target = statement instanceof Insert insert ? insert.getTable()
                : statement instanceof Update update ? update.getTable()
                : statement instanceof Delete delete ? delete.getTable()
                : statement instanceof Merge merge ? merge.getTable()
                : ((Upsert) statement).getTable();
            if (target != null) {
                // A modification target is a physical object even if a CTE has the same unqualified name.
                verifyObject(context, executionContext, target.getFullyQualifiedName());
            }
            verifyStatementObjects(context, executionContext, statement);
        }
    }

    @Override
    protected void verifyQueryText(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String queryText
    ) throws DBException {
        List<String> tokens = tokenize(executionContext.getDataSource().getSQLDialect(), queryText);
        requireStatement(Set.of("INSERT", "UPDATE", "DELETE", "MERGE", "REPLACE", "UPSERT", "CALL", "COPY", "LOAD")
            .contains(firstKeyword(tokens)), SQLQueryCategory.DML);
        if (Set.of("COPY", "LOAD").contains(firstKeyword(tokens))) {
            // File/program endpoints are not bounded by a table-level database scope.
            verifyDatasourceScope(context);
        }
        if (firstKeyword(tokens).equals("CALL")) {
            Identifier routine = readIdentifier(tokens, 1);
            if (routine == null) {
                throw new DBException("Cannot determine routine name");
            }
            verifyRoutine(context, executionContext, routine.name());
            for (String source : findTextObjects(tokens, Set.of("FROM", "JOIN"))) {
                verifyObject(context, executionContext, source);
            }
            verifyTextRoutines(context, executionContext, tokens, Set.of(routine.name()));
        } else {
            verifyTextObjects(context, executionContext, tokens, Set.of("FROM", "JOIN", "INTO", "UPDATE", "USING", "COPY", "TABLE"));
        }
    }
}
