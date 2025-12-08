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
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.model.sql.format.tokenized.SQLFormatterTokenized;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DBLTextDocumentService implements TextDocumentService, LanguageClientAware {
    private static final Log log = Log.getLog(DBLTextDocumentService.class);

    public static final String PROJECT_ID_OPTION = "projectId";
    public static final String DATA_SOURCE_ID_OPTION = "dataSourceId";

    private final Map<String, String> textCache = new ConcurrentHashMap<>();

    @Nullable
    private LanguageClient languageClient;

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        log.debug("didOpen with params: " + params);

        TextDocumentItem textDocument = params.getTextDocument();
        textCache.put(textDocument.getUri(), textDocument.getText());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        log.debug("didChange with params: " + params);

        VersionedTextDocumentIdentifier textDocument = params.getTextDocument();
        List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
        if (contentChanges.size() != 1) {
            // There should be exactly one change since we use TextDocumentSyncKind.Full
            throw new IllegalArgumentException("Unexpected number of document changes: " + contentChanges.size());
        }
        textCache.put(textDocument.getUri(), contentChanges.getFirst().getText());
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        log.debug("\"didClose with params: \"" + params);

        textCache.remove(params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        log.debug("\"didSave with params: \"" + params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        log.debug("\"formatting with params: \"" + params);

        return CompletableFutures.computeAsync(cancelChecker -> formatting(params, cancelChecker));
    }

    // TODO: think about creating an incremental formatter instead of replacing the whole document
    private List<? extends TextEdit> formatting(DocumentFormattingParams params, CancelChecker cancelChecker) {
        cancelChecker.checkCanceled();
        String documentUri = params.getTextDocument().getUri();
        String text = textCache.get(documentUri);
        if (text == null) {
            log.warn("Formatting requested for an unknown document " + documentUri);
            return List.of();
        }

        DBPDataSource dataSource = resolveDataSource(params.getOptions());
        SQLDialect dialect = BasicSQLDialect.INSTANCE;
        if (dataSource != null) {
            dialect = dataSource.getSQLDialect();
        }

        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        SQLFormatterConfiguration sqlFormatterConfiguration = new SQLFormatterConfiguration(dataSource, syntaxManager);
        SQLFormatter sqlFormatter = new SQLFormatterTokenized();
        String formattedText = sqlFormatter.format(text, sqlFormatterConfiguration);
        Position startPosition = new Position(0, 0);
        Range range = new Range(startPosition, lastTextPosition(text));
        return List.of(new TextEdit(range, formattedText));
    }

    private Position lastTextPosition(String text) {
        int numberOfLines = 0;
        int indexOfLastLineSeparator = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                numberOfLines++;
                indexOfLastLineSeparator = i;
            }
        }
        int startOfTheLastLine = indexOfLastLineSeparator + 1;
        if (startOfTheLastLine == text.length()) {
            return new Position(numberOfLines, 0);
        }
        return new Position(numberOfLines, text.substring(startOfTheLastLine).length());
    }

    @Nullable
    private DBPDataSource resolveDataSource(@Nullable FormattingOptions options) {
        String projectId = Optional.ofNullable(options)
            .map(o -> o.get(PROJECT_ID_OPTION))
            .map(Either3::getFirst)
            .orElse(null);
        String dataSourceId = Optional.ofNullable(options)
            .map(o -> o.get(DATA_SOURCE_ID_OPTION))
            .map(Either3::getFirst)
            .orElse(null);
        return DBWorkbench.getPlatform().getWorkspace().getProjects().stream()
            .filter(project -> Objects.equals(project.getId(), projectId))
            .flatMap(project -> project.getDataSourceRegistry().getDataSources().stream())
            .filter(dataSource -> Objects.equals(dataSource.getId(), dataSourceId))
            .findFirst()
            .map(DBPDataSourceContainer::getDataSource)
            .orElse(null);
    }

    @Override
    public void connect(LanguageClient client) {
        languageClient = client;
    }

    public String getText(String uri) {
        return textCache.get(uri);
    }
}
