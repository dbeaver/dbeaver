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
package org.jkiss.dbeaver.model.sql.completion;

import org.eclipse.jface.text.IDocument;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;

import java.util.function.Supplier;

public class SQLCompletionRequest {

    public enum QueryType {
        TABLE,
        JOIN,
        COLUMN,
        EXEC
    }

    private final SQLCompletionContext context;
    private final IDocument document;
    private final int documentOffset;
    private final SQLScriptElement activeQuery;
    private final boolean simpleMode;

    private final SQLCompletionActivityTracker stateTracker;

    private final SQLWordPartDetector wordDetector;

    private String wordPart;
    private QueryType queryType;
    private String contentType;

    public SQLCompletionRequest(SQLCompletionContext context, IDocument document, int documentOffset, SQLScriptElement activeQuery, boolean simpleMode) {
        this.context = context;
        this.document = document;
        this.documentOffset = documentOffset;
        this.activeQuery = activeQuery;
        this.simpleMode = simpleMode;
        this.stateTracker = new SQLCompletionActivityTracker(simpleMode);

        this.wordDetector = new SQLWordPartDetector(document, context.getSyntaxManager(), documentOffset);
        this.wordPart = wordDetector.getWordPart();
    }

    public SQLCompletionContext getContext() {
        return context;
    }

    public IDocument getDocument() {
        return document;
    }

    public int getDocumentOffset() {
        return documentOffset;
    }

    public SQLScriptElement getActiveQuery() {
        return activeQuery;
    }

    public boolean isSimpleMode() {
        return simpleMode;
    }

    public SQLCompletionActivityTracker getActivityTracker() {
        return this.stateTracker;
    }

    public SQLWordPartDetector getWordDetector() {
        return wordDetector;
    }

    /** Recreate the word detector in this request's context when the user continues typing. */
    @NotNull
    public SQLWordPartDetector createWordDetector(@NotNull IDocument currentDocument, int currentOffset) {
        return new SQLWordPartDetector(currentDocument, context.getSyntaxManager(), currentOffset);
    }

    public String getWordPart() {
        return wordPart;
    }

    public void setWordPart(String wordPart) {
        this.wordPart = wordPart;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    /** Whether this request contains SQL suitable for semantic completion. */
    public boolean supportsSemanticCompletion() {
        return true;
    }

    /** Whether lexical proposals are required even when semantic completion is enabled. */
    public boolean requiresLegacyCompletion() {
        return false;
    }

    /** Obtain semantic context, allowing embedded SQL to use its own model instead of the editor's model. */
    @Nullable
    public SQLQueryCompletionContext obtainCompletionContext(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Supplier<SQLQueryCompletionContext> editorContext
    ) {
        return editorContext.get();
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

}
