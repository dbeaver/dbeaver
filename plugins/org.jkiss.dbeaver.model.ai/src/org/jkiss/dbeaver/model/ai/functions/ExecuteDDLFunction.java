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

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterSession;
import net.sf.jsqlparser.statement.alter.AlterSystemStatement;
import net.sf.jsqlparser.statement.alter.RenameTableStatement;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.synonym.CreateSynonym;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.refresh.RefreshMaterializedViewStatement;
import net.sf.jsqlparser.statement.truncate.Truncate;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionResult;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLObjectOperation;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryCategory;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExecuteDDLFunction extends ExecuteSQLFunction {
    @NotNull
    @Override
    public AIFunctionResult callFunction(@NotNull AIFunctionContext context, @NotNull Map<String, Object> parameters) throws DBException {
        return completionResult(executeQueries(context, parameters, null, 0));
    }

    @Override
    protected void verifyStatement(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull SQLQuery query
    ) throws DBException {
        Statement statement = query.getStatement();
        SQLObjectOperation operation = query.getObjectOperation();
        requireStatement(operation != null || statement instanceof Truncate || statement instanceof RenameTableStatement
            || statement instanceof Comment || statement instanceof RefreshMaterializedViewStatement
            || statement instanceof AlterSystemStatement || statement instanceof AlterSession, SQLQueryCategory.DDL);
        if (statement instanceof CreateSynonym synonym && synonym.isPublicSynonym()) {
            verifyDatasourceScope(context);
        }
        if (operation != null) {
            verifyDefinitionObject(context, executionContext, String.join(".", operation.qualifiedNameParts()),
                operation.objectKind().name(), operation.operation() == SQLObjectOperation.Operation.CREATE);
        }
        if (statement instanceof CreateTable create) {
            if (create.getSelect() != null) {
                verifyStatementObjects(context, executionContext, create.getSelect());
            }
            if (create.getLikeTable() != null) {
                verifyObject(context, executionContext, create.getLikeTable().getFullyQualifiedName());
            }
            for (var index : CommonUtils.safeCollection(create.getIndexes())) {
                if (index instanceof ForeignKeyIndex foreignKey) {
                    verifyObject(context, executionContext, foreignKey.getTable().getFullyQualifiedName());
                }
            }
            for (var column : CommonUtils.safeCollection(create.getColumnDefinitions())) {
                List<String> specs = CommonUtils.safeList(column.getColumnSpecs());
                for (int i = 0; i < specs.size(); i++) {
                    if (specs.get(i).equalsIgnoreCase("REFERENCES")) {
                        Identifier reference = readIdentifier(specs, i + 1);
                        if (reference == null) {
                            throw new DBException("Cannot determine referenced table");
                        }
                        verifyObject(context, executionContext, reference.name());
                    }
                }
            }
        } else if (statement instanceof CreateIndex create) {
            verifyObject(context, executionContext, create.getTable().getFullyQualifiedName());
        } else if (statement instanceof CreateSynonym create) {
            verifyObject(context, executionContext, create.getFor());
        } else if (statement instanceof CreateView create) {
            verifyStatementObjects(context, executionContext, create.getSelect());
        } else if (statement instanceof Alter alter) {
            for (var expression : CommonUtils.safeCollection(alter.getAlterExpressions())) {
                if (expression.getFkSourceTable() != null) {
                    verifyObject(context, executionContext, expression.getFkSourceSchema() == null ? expression.getFkSourceTable()
                        : expression.getFkSourceSchema() + "." + expression.getFkSourceTable());
                }
                if (expression.getNewTableName() != null) {
                    verifyDefinitionObject(context, executionContext, expression.getNewTableName(), "TABLE", true);
                }
            }
        } else if (statement instanceof Truncate truncate) {
            for (var table : truncate.getTables()) {
                verifyObject(context, executionContext, table.getFullyQualifiedName());
            }
        } else if (statement instanceof RenameTableStatement rename) {
            for (var pair : rename.getTableNames()) {
                verifyObject(context, executionContext, pair.getKey().getFullyQualifiedName());
                verifyDefinitionObject(context, executionContext, pair.getValue().getFullyQualifiedName(), "TABLE", true);
            }
        } else if (statement instanceof Comment comment) {
            requireStatement(comment.getTable() != null || comment.getView() != null || comment.getColumn() != null, SQLQueryCategory.DDL);
            var table = comment.getTable() != null ? comment.getTable()
                : comment.getView() != null ? comment.getView() : comment.getColumn().getTable();
            verifyObject(context, executionContext, table.getFullyQualifiedName());
        } else if (statement instanceof RefreshMaterializedViewStatement refresh) {
            verifyObject(context, executionContext, refresh.getView().getFullyQualifiedName());
        } else if (statement instanceof AlterSystemStatement || statement instanceof AlterSession) {
            verifyDatasourceScope(context);
        }
    }

    @Override
    protected void verifyQueryText(
        @NotNull AIFunctionContext context, @NotNull DBCExecutionContext executionContext, @NotNull String queryText
    ) throws DBException {
        List<String> tokens = tokenize(executionContext.getDataSource().getSQLDialect(), queryText);
        String command = firstKeyword(tokens);
        requireStatement(Set.of("CREATE", "ALTER", "DROP", "TRUNCATE", "RENAME", "COMMENT", "REFRESH").contains(command)
            && !(command.equals("ALTER") && tokens.size() > 2 && tokens.get(1).equalsIgnoreCase("DEFAULT")
                && tokens.get(2).equalsIgnoreCase("PRIVILEGES")), SQLQueryCategory.DDL);
        int start = 1;
        while (start < tokens.size() && Set.of("OR", "REPLACE", "TEMP", "TEMPORARY", "UNLOGGED", "UNIQUE", "MATERIALIZED", "ON")
            .contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
            start++;
        }
        if (start + 1 < tokens.size() && tokens.get(start).equalsIgnoreCase("PUBLIC") && tokens.get(start + 1).equalsIgnoreCase("SYNONYM")) {
            verifyDatasourceScope(context);
            start++;
        }
        String type = start < tokens.size() ? tokens.get(start++).toUpperCase(Locale.ROOT) : "";
        if (command.equals("TRUNCATE") && !type.equals("TABLE")) {
            type = "TABLE";
            start--;
        }
        while (start < tokens.size() && Set.of("IF", "NOT", "EXISTS", "ONLY").contains(tokens.get(start).toUpperCase(Locale.ROOT))) {
            start++;
        }
        Identifier target = readIdentifier(tokens, start);
        if (target == null) {
            throw new DBException("Cannot determine DDL target");
        }
        verifyDefinitionObject(context, executionContext, target.name(), type, command.equals("CREATE"));
        if (command.equals("DROP") || command.equals("TRUNCATE")) {
            int end = target.end();
            while (end < tokens.size() && tokens.get(end).equals(",")) {
                Identifier extra = readIdentifier(tokens, end + 1);
                if (extra == null) {
                    throw new DBException("Cannot determine DDL target");
                }
                verifyDefinitionObject(context, executionContext, extra.name(), type, false);
                end = extra.end();
            }
        }
        Set<String> prefixes = new HashSet<>(Set.of("FROM", "JOIN", "REFERENCES"));
        if (type.equals("TABLE") && command.equals("CREATE")) {
            prefixes.add("LIKE");
        }
        if (type.equals("INDEX") || type.equals("TRIGGER")) {
            prefixes.add("ON");
        }
        if (type.equals("SYNONYM")) {
            prefixes.add("FOR");
        }
        for (String source : findTextObjects(tokens, prefixes)) {
            if (!source.equals(target.name())) {
                verifyObject(context, executionContext, source);
            }
        }
        verifyTextRoutines(context, executionContext, tokens, Set.of(target.name()));
    }
}
