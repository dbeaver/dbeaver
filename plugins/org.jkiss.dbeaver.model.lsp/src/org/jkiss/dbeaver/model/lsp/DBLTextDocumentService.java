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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Region;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsp.context.ContextAwareDocument;
import org.jkiss.dbeaver.model.lsp.context.LspSQLCompletionContext;
import org.jkiss.dbeaver.model.lsp.context.LspSQLCompletionContextParser;
import org.jkiss.dbeaver.model.lsp.context.LspSQLRuleManager;
import org.jkiss.dbeaver.model.lsp.utils.LSPUtils;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLFormatterTokenized;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenDefault;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The DBLTextDocumentService class proves services for managing, manipulating,
 * and analyzing text documents in the context of a language server.
 * URI format: lsp://{projectId}/{resourcePath}
 */
public class DBLTextDocumentService implements TextDocumentService, LanguageClientAware {
    private static final Log log = Log.getLog(DBLTextDocumentService.class);

    public static final Map<SQLTokenType, Pair<Integer, String>> SUPPORTED_TOKEN_TYPES = Map.of(
        SQLTokenType.T_KEYWORD, new Pair<>(0, SemanticTokenTypes.Keyword),
        SQLTokenType.T_STRING, new Pair<>(1, SemanticTokenTypes.String)
    );
    public static final List<String> SUPPORTED_TOKEN_MODIFIERS = List.of(
        "declaration"
    );

    private final Map<String, ContextAwareDocument> documentCache = new ConcurrentHashMap<>();

    @Nullable
    private final DBLServerSessionProvider sessionProvider;

    public DBLTextDocumentService() {
        this.sessionProvider = null;
    }

    public DBLTextDocumentService(@Nullable DBLServerSessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Override
    public void didOpen(@NotNull DidOpenTextDocumentParams params) {
        log.debug("didOpen with params: " + params);

        TextDocumentItem document = params.getTextDocument();

        documentCache.put(document.getUri(), ContextAwareDocument.from(document));
        try {
            DocumentURI uri = new DocumentURI(document.getUri());
            initContext(uri);
        } catch (IllegalArgumentException e) {
            log.error("Error initiating document context. Proceeding with default. ", e);
        }
    }

    @Override
    public void didChange(@NotNull DidChangeTextDocumentParams params) {
        log.debug("didChange with params: " + params);

        VersionedTextDocumentIdentifier document = params.getTextDocument();
        List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
        if (contentChanges.size() != 1) {
            // There should be exactly one change since we use TextDocumentSyncKind.Full
            throw new IllegalArgumentException("Unexpected number of document changes: " + contentChanges.size());
        }

        ContextAwareDocument existingDocument = documentCache.get(document.getUri());
        if (existingDocument == null) {
            log.warn(String.format("Change registered for an unknown document %s, Skipping", document.getUri()));
        } else {
            existingDocument.setText(contentChanges.getFirst().getText());
            existingDocument.setVersion(document.getVersion());
        }
    }

    @Override
    public void didClose(@NotNull DidCloseTextDocumentParams params) {
        log.debug("\"didClose with params: \"" + params);

        documentCache.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(@NotNull DidSaveTextDocumentParams params) {
        log.debug("\"didSave with params: \"" + params);
    }

    @NotNull
    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(@NotNull DocumentFormattingParams params) {
        log.debug("\"formatting with params: \"" + params);

        return CompletableFutures.computeAsync(cancelChecker -> formatting(params, cancelChecker));
    }

    // TODO: think about creating an incremental formatter instead of replacing the whole document
    @NotNull
    private List<? extends TextEdit> formatting(
        @NotNull DocumentFormattingParams params,
        @NotNull CancelChecker cancelChecker
    ) {
        cancelChecker.checkCanceled();

        String documentUri = params.getTextDocument().getUri();
        ContextAwareDocument document = documentCache.get(documentUri);
        if (document == null) {
            log.warn("Formatting requested for an unknown document " + documentUri);
            return List.of();
        }

        SQLFormatterConfiguration sqlFormatterConfiguration = new SQLFormatterConfiguration(
            document.getDataSource(),
            document.getSyntaxManager()
        );
        SQLFormatter sqlFormatter = new SQLFormatterTokenized();
        String formattedText = sqlFormatter.format(document.getText(), sqlFormatterConfiguration);
        Position startPosition = new Position(0, 0);
        Range range = new Range(startPosition, LSPUtils.lastTextPosition(document.getText()));
        return List.of(new TextEdit(range, formattedText));
    }

    @NotNull
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(@NotNull CompletionParams params) {
        log.debug("\"completion with params: \"" + params);

        return CompletableFutures.computeAsync(cancelChecker -> {
            try {
                return completion(params, cancelChecker);
            } catch (Exception e) {
                log.error("Error when getting completion items: ", e);
                return Either.forRight(new CompletionList());
            }
        });
    }

    @NotNull
    private Either<List<CompletionItem>, CompletionList> completion(
        @NotNull CompletionParams params,
        @NotNull CancelChecker cancelChecker
    ) throws InterruptedException, InvocationTargetException, DBException {
        cancelChecker.checkCanceled();

        String documentUri = params.getTextDocument().getUri();
        ContextAwareDocument document = documentCache.get(documentUri);
        if (document == null) {
            log.error(String.format("Completion requested for an unknown document %s", documentUri));
            return Either.forRight(new CompletionList());
        } else if (document.getExecutionContext() == null) {
            log.error(String.format("Completion requested for a document with no execution context %s", documentUri));
            return Either.forRight(new CompletionList());
        }

        int offset = LSPUtils.positionToOffset(document.getText(), params.getPosition());
        SQLCompletionContext completionContext = new LspSQLCompletionContext(
            document.getDataSource(),
            document.getExecutionContext(),
            document.getSyntaxManager(),
            document.getRuleManager()
        );
        return Either.forRight(new CompletionList(
            LspSQLCompletionContextParser.createCompletionsList(document, offset, completionContext)
        ));
    }

    // TODO: think about implementing semanticTokensFullDelta and/or semanticTokensRange
    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(@NotNull SemanticTokensParams params) {
        log.debug("\"semanticTokensFull with params: \"" + params);

        return CompletableFutures.computeAsync(cancelChecker -> semanticTokensFull(params, cancelChecker));
    }

    private SemanticTokens semanticTokensFull(
        @NotNull SemanticTokensParams params,
        @NotNull CancelChecker cancelChecker
    ) {
        cancelChecker.checkCanceled();

        String documentUri = params.getTextDocument().getUri();
        ContextAwareDocument document = documentCache.get(documentUri);
        if (document == null) {
            log.error("Semantic tokens requested for an unknown document " + documentUri);
            return new SemanticTokens();
        }

        LspSQLIndentifierDetector detector = new LspSQLIndentifierDetector(document.getSyntaxManager().getDialect());
        List<Pair<TPToken, Region>> tokens = detector.extractAllTokens(
            new Document(document.getText()),
            document.getRuleManager()
        );

        List<Integer> data = new ArrayList<>();
        int lineOffset = 0;
        int charOffset = 0;
        for (Pair<TPToken, Region> pair : tokens) {
            SQLTokenType sqlTokenType;
            Region region = pair.getSecond();
            TPToken tpToken = pair.getFirst();
            if (tpToken.isNewline()) {
                lineOffset++;
                charOffset = 0;
                continue;
            } else if (tpToken instanceof TPTokenDefault tpTokenDefault) {
                sqlTokenType = (SQLTokenType) tpTokenDefault.getData();
            } else {
                sqlTokenType = SQLTokenType.T_OTHER;
            }
            Pair<Integer, String> tokenDefinition = SUPPORTED_TOKEN_TYPES.get(sqlTokenType);
            if (tokenDefinition == null || tokenDefinition.getFirst() == null) {
                charOffset += region.getLength();
                continue;
            }

            // This is a chunk of token data describing one token
            data.add(lineOffset);
            data.add(charOffset);
            data.add(region.getLength());
            data.add(tokenDefinition.getFirst());
            data.add(0); // Only "declaration" modifier is supported now. Update if more modifiers are added.

            charOffset += region.getLength();
        }

        return new SemanticTokens(data);
    }

    private void initContext(@NotNull DocumentURI documentUri) {
        ContextAwareDocument document = documentCache.get(documentUri.getValue());
        if (document == null) {
            log.warn(String.format("Unknown document %s, Skipping context init", documentUri));
            return;
        }

        String projectId = documentUri.getProjectId();
        DBPProject project = sessionProvider != null ?
            sessionProvider.getWorkspace().getProject(projectId) :
            DBWorkbench.getPlatform().getWorkspace().getProject(projectId);

        DBPDataSourceContainer dataSourceContainer = null;
        if (project != null) {
            // Note: default datasource id is defined as a resource property:
            // in Cloudbeaver - from front-end - LocalResourceController#setResourceProperty
            // in Desktop - EditorUtils#setInputDataSource
            String dataSourceId = String.valueOf(
                project.getResourceProperty(documentUri.getResourcePath(), DBConstants.PROP_RESOURCE_DEFAULT_DATASOURCE)
            );
            if (dataSourceId != null) {
                dataSourceContainer = project.getDataSourceRegistry().getDataSource(dataSourceId);
            }
        }

        try {
            document.setExecutionContext(DBUtils.getOrOpenDefaultContext(dataSourceContainer, false));
        } catch (DBCException e) {
            log.warn(String.format(
                "Failed to determine default execution context for document %s. Proceeding without it.", documentUri
            ));
        }

        SQLSyntaxManager syntaxManager = resolveSyntaxManager(dataSourceContainer);
        document.setSyntaxManager(syntaxManager);

        LspSQLRuleManager ruleManager = new LspSQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSourceContainer, false);
        document.setRuleManager(ruleManager);

        log.debug("Initialized context for text document " + document);
    }

    @NotNull
    private SQLSyntaxManager resolveSyntaxManager(@Nullable DBPDataSourceContainer dataSourceContainer) {
        SQLDialect dialect = BasicSQLDialect.INSTANCE;
        if (dataSourceContainer != null && dataSourceContainer.getDataSource() != null) {
            dialect = dataSourceContainer.getDataSource().getSQLDialect();
        }
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());

        return syntaxManager;
    }

    @Override
    public void connect(@NotNull LanguageClient client) {
    }
}
