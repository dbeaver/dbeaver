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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;
import org.jkiss.dbeaver.model.lsp.context.ContextAwareDocument;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

public class DocumentServiceTestUtils {

    private static final String H2_DRIVER_ID = "h2_embedded_v2";
    public static final String BASIC_RESOURCE_PATH = "scripts/basic.sql";
    public static final String BASIC_URI = "lsp://project-id/" + BASIC_RESOURCE_PATH;
    public static final String SQL_LANGUAGE_ID = "SQL";

    @Nullable
    public static ContextAwareDocument getDocument(@NotNull DBLTextDocumentService service, @NotNull String uri) {
        try {
            Field documentsField = service.getClass().getDeclaredField("documentCache");
            documentsField.setAccessible(true);
            Map<String, ContextAwareDocument> documents =
                (Map<String, ContextAwareDocument>) documentsField.get(service);
            ContextAwareDocument document = documents.get(uri);
            documentsField.setAccessible(false);
            return document;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearDocuments(@NotNull DBLTextDocumentService service) {
        try {
            Field documentsField = service.getClass().getDeclaredField("documentCache");
            documentsField.setAccessible(true);
            Map<String, ContextAwareDocument> documents =
                (Map<String, ContextAwareDocument>) documentsField.get(service);
            documents.clear();
            documentsField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static TextDocumentItem createQueryDocument(@NotNull String text) {
        return new TextDocumentItem(BASIC_URI, SQL_LANGUAGE_ID, 0, text);
    }

    @NotNull
    public static ContextAwareDocument createAndSaveDocument(
        @NotNull DBLTextDocumentService service,
        @NotNull String text,
        @NotNull String projectId,
        @NotNull String resourcePath
    ) {
        String uri = String.format("lsp://%s/%s", projectId, resourcePath);
        TextDocumentItem document = new TextDocumentItem(uri, SQL_LANGUAGE_ID, 0, text);
        service.didOpen(new DidOpenTextDocumentParams(document));
        return Objects.requireNonNull(getDocument(service, uri));
    }

    @NotNull
    public static DocumentFormattingParams setupDocumentAndBuildFormattingParams(
        @NotNull DBLTextDocumentService service,
        @NotNull String query
    ) {
        TextDocumentItem textDocument = createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));
        DocumentFormattingParams formattingParams = new DocumentFormattingParams();
        formattingParams.setTextDocument(new TextDocumentIdentifier(BASIC_URI));
        FormattingOptions formattingOptions = new FormattingOptions();
        formattingParams.setOptions(formattingOptions);
        return formattingParams;
    }

    @NotNull
    public static DBPDataSourceContainer createDataSource(
        @NotNull DBRProgressMonitor monitor
    ) throws DBException {
        final DBPDriver driver = DataSourceProviderRegistry.getInstance().findDriver(H2_DRIVER_ID);
        if (driver == null) {
            throw new DBException("Could not find H2 driver: " + H2_DRIVER_ID);
        }

        final DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:h2:mem:");

        final DataSourceDescriptor dataSourceDescriptor = new DataSourceDescriptor(
            Objects.requireNonNull(DBWorkbench.getPlatform().getWorkspace().getActiveProject()).getDataSourceRegistry(),
            DataSourceDescriptor.generateNewId(driver),
            driver,
            configuration
        );
        dataSourceDescriptor.setName("Test DB");
        dataSourceDescriptor.setSavePassword(true);
        dataSourceDescriptor.setTemporary(true);
        dataSourceDescriptor.connect(monitor, true, true);

        return dataSourceDescriptor;
    }
}
