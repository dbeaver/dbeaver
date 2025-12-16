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
package org.jkiss.dbeaver.model.lsp.context;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.jkiss.code.NotNull;


/**
 * Provides services for initialization of a document context in the Language Server Protocol (LSP) implementation.
 */
public interface DocumentContextService {

    /**
     * Initializes the context for a specific document within the Language Server Protocol (LSP) framework.
     * This method associates the given document with the specified project and data source,
     * enabling context-dependent functionality such as SQL dialect interpretation and content assist.
     *
     * @param documentId  the identifier of the document for which the context is being initialized; must not be null
     * @param projectId   the unique identifier of the project that the document belongs to; must not be null
     * @param dataSourceId the unique identifier of the data source associated with the document; must not be null
     */
    @JsonNotification("textDocument/initContext")
    void initContext(
        @NotNull TextDocumentIdentifier documentId,
        @NotNull String projectId,
        @NotNull String dataSourceId
    );
}
