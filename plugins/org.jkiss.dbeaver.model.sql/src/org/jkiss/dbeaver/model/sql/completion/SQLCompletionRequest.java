/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;

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

    public SQLWordPartDetector getWordDetector() {
        return wordDetector;
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

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

}
