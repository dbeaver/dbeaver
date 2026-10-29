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
package org.jkiss.dbeaver.model.sql.semantics.model.statements;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsDataContext;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryRowsSourceContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModelVisitor;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.List;

/** Control-flow bodies are resolved in source order without leaking lexical branch declarations. */
public final class SQLQueryConditionalStatement extends SQLQueryStatementModel {
    public enum Kind { IF, CASE, LOOP, REPEAT, TRY_CATCH, EXCEPTION }

    @NotNull
    private final Kind kind;
    @NotNull
    private final List<SQLQueryStatementModel> branches;

    private SQLQueryConditionalStatement(@NotNull STMTreeNode node, @NotNull Kind kind, @NotNull List<SQLQueryStatementModel> branches) {
        super(node, branches.toArray(SQLQueryStatementModel[]::new));
        this.kind = kind;
        this.branches = List.copyOf(branches);
    }

    @NotNull
    public static SQLQueryConditionalStatement recognize(@NotNull SQLQueryModelRecognizer recognizer, @NotNull STMTreeNode node) {
        Kind kind = switch (node.getNodeKindId()) {
            case SQLStandardParser.RULE_proceduralIf -> Kind.IF;
            case SQLStandardParser.RULE_proceduralCase -> Kind.CASE;
            case SQLStandardParser.RULE_proceduralLoop -> Kind.LOOP;
            case SQLStandardParser.RULE_proceduralRepeat -> Kind.REPEAT;
            case SQLStandardParser.RULE_proceduralTryCatch -> Kind.TRY_CATCH;
            default -> Kind.EXCEPTION;
        };
        List<SQLQueryStatementModel> branches = new ArrayList<>();
        for (STMTreeNode child : node.findNonErrorChildren()) {
            if (child.getNodeKindId() == SQLStandardParser.RULE_proceduralStatements ||
                child.getNodeKindId() == SQLStandardParser.RULE_proceduralStatement) {
                branches.add(SQLQueryStatementModel.recognize(recognizer, child));
            }
        }
        return new SQLQueryConditionalStatement(node, kind, branches);
    }

    @NotNull
    public Kind getKind() {
        return this.kind;
    }

    @NotNull
    public List<SQLQueryStatementModel> getBranches() {
        return this.branches;
    }

    @NotNull
    @Override
    protected SQLQueryRowsSourceContext resolveStatementImpl(
        @NotNull SQLQueryRowsSourceContext context, @NotNull SQLQueryRecognitionContext statistics
    ) {
        SQLQueryRowsSourceContext result = context;
        for (SQLQueryStatementModel branch : this.branches) {
            if (statistics.getMonitor().isCanceled()) {
                break;
            }
            SQLQueryRowsSourceContext resolved = branch.resolveStatement(
                result.withLocalVariables(result.getLocalVariables().withNestedBlock()), statistics);
            result = result.withProgramVariablesFrom(resolved);
        }
        // No arm is guaranteed to execute; neither declarations nor temporary table changes escape.
        return result;
    }

    @Override
    public boolean tryResolveValueRelations(@NotNull SQLQueryRowsDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        for (SQLQueryStatementModel branch : this.branches) {
            if (statistics.getMonitor().isCanceled() || !branch.tryResolveValueRelations(this.valueContext(context), statistics)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected <R, T> R applyImpl(@NotNull SQLQueryNodeModelVisitor<T, R> visitor, T arg) {
        return visitor.visitConditionalStatement(this, arg);
    }
}
