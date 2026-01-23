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
package org.jkiss.dbeaver.model.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.Pair;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DBLServer implements LanguageServer, LanguageClientAware {
    private static final Log log = Log.getLog(DBLServer.class);

    private static final String SERVER_NAME = "DBeaver language server"; //NON-NLS

    @Nullable
    private ClientInfo clientInfo;
    @NotNull
    private final DBLTextDocumentService textDocumentService;
    @NotNull
    private final WorkspaceService workspaceService;

    public DBLServer(@Nullable DBLServerSessionProvider sessionProvider) {
        this.textDocumentService = new DBLTextDocumentService(sessionProvider);
        this.workspaceService = new DBLWorkspaceService();
    }

    @NotNull
    @Override
    public CompletableFuture<InitializeResult> initialize(@NotNull InitializeParams params) {
        clientInfo = params.getClientInfo();
        log.info("LSP client sent an initialize request. " + clientInfo); //NON-NLS
        log.debug(params);

        //https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_synchronization
        TextDocumentSyncOptions textDocumentSyncOptions = new TextDocumentSyncOptions();
        textDocumentSyncOptions.setOpenClose(true);
        textDocumentSyncOptions.setChange(TextDocumentSyncKind.Full);
        textDocumentSyncOptions.setWillSave(false);
        textDocumentSyncOptions.setWillSaveWaitUntil(false);
        textDocumentSyncOptions.setSave(false);

        String serverVersion = GeneralUtils.getPlainVersion(); //NON-NLS
        ServerInfo serverInfo = new ServerInfo(SERVER_NAME, serverVersion);

        ServerCapabilities serverCapabilities = buildServerCapabilities(textDocumentSyncOptions);
        return CompletableFuture.completedFuture(new InitializeResult(serverCapabilities, serverInfo));
    }

    @NotNull
    private static ServerCapabilities buildServerCapabilities(@NotNull TextDocumentSyncOptions options) {
        ServerCapabilities serverCapabilities = new ServerCapabilities();
        serverCapabilities.setTextDocumentSync(options);
        //https://microsoft.github.io/language-server-protocol/specifications/specification-current/#textDocument_formatting
        serverCapabilities.setDocumentFormattingProvider(new DocumentFormattingOptions());

        //https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_completion
        serverCapabilities.setCompletionProvider(new CompletionOptions());

        //https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_semanticTokens
        List<String> supportedTokens = DBLTextDocumentService.SUPPORTED_TOKEN_TYPES.values().stream().map(Pair::getSecond).toList();
        List<String> supportedModifiers = DBLTextDocumentService.SUPPORTED_TOKEN_MODIFIERS;
        SemanticTokensLegend legend = new SemanticTokensLegend(supportedTokens, supportedModifiers);
        SemanticTokensWithRegistrationOptions tokenProvider = new SemanticTokensWithRegistrationOptions(legend);
        serverCapabilities.setSemanticTokensProvider(tokenProvider);

        return serverCapabilities;
    }

    @NotNull
    @Override
    public CompletableFuture<Object> shutdown() {
        log.info("shutdown request received by the language server. " + clientInfo); //NON-NLS
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public void exit() {
        /*
         * TODO:
         * The spec says: "A notification to ask the server to exit its process.
         * The server should exit with success code 0 if the shutdown request has been received before; otherwise with error code 1."
         * Let's ignore it for now as it's not clear at this point what to do.
         */
        log.info("exit notification received by the language server. " + clientInfo); // NON-NLS
    }

    @NotNull
    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @NotNull
    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(@NotNull LanguageClient client) {
        log.info("LSP client connected: " + client); // NON-NLS
        if (textDocumentService instanceof LanguageClientAware clientAwareTextDocumentService) {
            clientAwareTextDocumentService.connect(client);
        }
    }
}
