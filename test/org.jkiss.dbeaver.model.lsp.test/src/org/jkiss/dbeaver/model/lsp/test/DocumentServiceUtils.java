/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsp.test;

import org.eclipse.lsp4j.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;

public class DocumentServiceUtils {

    public static final String BASIC_SQL_URI = "sql/scripts/basic.sql";

    @NotNull
    public static DocumentFormattingParams setupDocumentAndBuildFormattingParams(
        @NotNull DBLTextDocumentService service,
        @NotNull String sql
    ) {
        TextDocumentItem textDocument = new TextDocumentItem();
        textDocument.setText(sql);
        textDocument.setUri(BASIC_SQL_URI);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));
        DocumentFormattingParams formattingParams = new DocumentFormattingParams();
        formattingParams.setTextDocument(new TextDocumentIdentifier(BASIC_SQL_URI));
        FormattingOptions formattingOptions = new FormattingOptions();
        formattingParams.setOptions(formattingOptions);
        return formattingParams;
    }
}
