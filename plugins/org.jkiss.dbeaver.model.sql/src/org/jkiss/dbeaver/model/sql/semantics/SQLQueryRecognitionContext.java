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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Accumulates the statistics about recognition process
 */
public class SQLQueryRecognitionContext {

    @NotNull
    private final DBRProgressMonitor monitor;

    @Nullable
    private final DBCExecutionContext executionContext;

    private final boolean useRealMetadata;

    @NotNull
    private final SQLSyntaxManager syntaxManager;

    @NotNull
    private final SQLDialect dialect;

    @NotNull
    private final Deque<SQLQueryRecognitionProblemInfo> problems = new LinkedList<>();

    private boolean errorsAsWarnings = false;

    public SQLQueryRecognitionContext(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBCExecutionContext executionContext,
        boolean useRealMetadata,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull SQLDialect dialect
    ) {
        this.monitor = monitor;
        this.executionContext = executionContext;
        this.useRealMetadata = useRealMetadata;
        this.syntaxManager = syntaxManager;
        this.dialect = dialect;
    }

    public void setTreatErrorAsWarnings(boolean errorsAsWarnings) {
        this.errorsAsWarnings = errorsAsWarnings;
    }

    public boolean isTreatErrorsAsWarnings() {
        return this.errorsAsWarnings;
    }

    @NotNull
    public DBRProgressMonitor getMonitor() {
        return this.monitor;
    }

    @Nullable
    DBCExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public boolean useRealMetadata() {
        return this.useRealMetadata;
    }

    @NotNull
    SQLSyntaxManager getSyntaxManager() {
        return this.syntaxManager;
    }

    @NotNull
    SQLDialect getDialect() {
        return this.dialect;
    }

    @NotNull
    public List<SQLQueryRecognitionProblemInfo> getProblems() {
        return new ArrayList<>(this.problems);
    }


    private SQLQueryRecognitionProblemInfo makeError(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry symbol,
        @NotNull String message,
        @Nullable DBException exception
    ) {
        SQLQueryRecognitionProblemInfo.Severity severity = this.errorsAsWarnings
            ? SQLQueryRecognitionProblemInfo.Severity.WARNING
            : SQLQueryRecognitionProblemInfo.Severity.ERROR;
        return new SQLQueryRecognitionProblemInfo(severity, syntaxNode, symbol, message, exception);
    }

    private SQLQueryRecognitionProblemInfo makeWarning(
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry symbol,
        @NotNull String message,
        @Nullable DBException exception
    ) {
        return new SQLQueryRecognitionProblemInfo(SQLQueryRecognitionProblemInfo.Severity.WARNING, syntaxNode, symbol, message, exception);
    }

    private void registerProblem(SQLQueryRecognitionProblemInfo problemInfo) {
        if (this.problems.size() < SQLQueryRecognitionProblemInfo.PER_QUERY_LIMIT) {
            this.problems.addLast(problemInfo);
        }
    }

    public void appendError(@NotNull SQLQuerySymbolEntry symbol, @NotNull String error, @NotNull DBException ex) {
        this.registerProblem(this.makeError(symbol.getSyntaxNode(), symbol, error, ex));
    }

    public void appendError(@NotNull SQLQuerySymbolEntry symbol, @NotNull String error) {
        this.registerProblem(this.makeError(symbol.getSyntaxNode(), symbol, error, null));
    }

    public void appendError(@NotNull STMTreeNode treeNode, @NotNull String error) {
        this.registerProblem(this.makeError(treeNode, null, error, null));
    }

    public void appendWarning(@NotNull SQLQuerySymbolEntry symbol, @NotNull String error) {
        this.registerProblem(this.makeWarning(symbol.getSyntaxNode(), symbol, error, null));
    }

    public void appendWarning(@NotNull STMTreeNode treeNode, @NotNull String error) {
        this.registerProblem(this.makeWarning(treeNode, null, error, null));
    }

    public void appendError(@NotNull STMTreeNode treeNode, @NotNull String error, @NotNull DBException ex) {
        this.registerProblem(this.makeError(treeNode, null, error, ex));
    }

    public void reset() {
        this.problems.clear();
    }
}
