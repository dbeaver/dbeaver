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

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.stm.STMTreeNode;

public class SQLQueryRecognitionProblemInfo {

    public static final int PER_QUERY_LIMIT = 50;

    public enum Severity {
        ERROR(2), // IMarker.SEVERITY_ERROR
        WARNING(1); // IMarker.SEVERITY_WARNING

        public final int markerSeverity;

        Severity(int markerSeverity) {
            this.markerSeverity = markerSeverity;
        }
    }

    @NotNull
    private final Severity severity;
    @NotNull
    private final STMTreeNode syntaxNode;
    @Nullable
    private final SQLQuerySymbolEntry symbol;
    @NotNull
    private final String message;
    @Nullable
    private final DBException exception;

    public SQLQueryRecognitionProblemInfo(
        @NotNull Severity severity,
        @NotNull STMTreeNode syntaxNode,
        @Nullable SQLQuerySymbolEntry symbol,
        @NotNull String message,
        @Nullable DBException exception
    ) {
        this.severity = severity;
        this.syntaxNode = syntaxNode;
        this.symbol = symbol;
        this.message = message;
        this.exception = exception;
    }

    @NotNull
    public Severity getSeverity() {
        return this.severity;
    }

    @NotNull
    public String getMessage() {
        return this.message;
    }

    @NotNull
    public Interval getInterval() {
        return this.syntaxNode.getRealInterval();
    }

    public String getExceptionMessage() {
        Throwable ex = this.exception;
        if (ex != null) {
            StringBuilder sb = new StringBuilder();
            while (ex != null) {
                sb.append(ex.getMessage()).append("\n");
                ex = ex.getCause();
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.syntaxNode.getRealInterval().toString()).append(": ");
        if (this.symbol != null) {
            sb.append(this.symbol.getName()).append(": ");
        } else {
            sb.append(this.syntaxNode.getTextContent()).append(": ");
        }
        sb.append(this.message);
        if (this.exception != null) {
            sb.append(": ").append(this.exception.toString());
        }
        return super.toString() + "[" + sb.toString() + "]";
    }
}
