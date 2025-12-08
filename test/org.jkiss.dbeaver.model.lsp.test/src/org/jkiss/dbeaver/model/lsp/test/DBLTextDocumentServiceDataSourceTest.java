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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DBLTextDocumentServiceDataSourceTest extends DBeaverUnitTest {
    private final DBLTextDocumentService service = new DBLTextDocumentService();

    private PostgreDataSource testDataSource;

    @Mock
    DBPDataSourceContainer mockDataSourceContainer;

    @Before
    public void setUp() throws Exception {
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProjects().getFirst();
        Mockito.when(mockDataSourceContainer.getProject()).thenReturn(project);
        Mockito.when(mockDataSourceContainer.getPreferenceStore()).thenReturn(DBWorkbench.getPlatform().getPreferenceStore());
        Mockito.when(mockDataSourceContainer.getId()).thenReturn("test_data_source");
        Mockito.when(mockDataSourceContainer.getDataSource()).thenReturn(testDataSource);
        Mockito.when(mockDataSourceContainer.getDriver()).thenReturn(DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("postgresql"));

        testDataSource = new PostgreDataSource(mockDataSourceContainer, "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return major <= 10;
            }

            @Nullable
            @Override
            public PostgreDataType getLocalDataType(String typeName) {
                return super.getLocalDataType(typeName);
            }
        };

        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase testDatabase = testDataSource.createDatabaseImpl(
            new VoidProgressMonitor(), "testdb", testUser, null, null, null
        );
        PostgreSchema testSchema = new PostgreSchema(testDatabase, "test_schema", testUser);

        PostgreTableRegular testTable = new PostgreTableRegular(testSchema);
        testTable.setName("test_table");
        testTable.setPartition(false);
        testTable.setPersisted(true);

        PostgreTableColumn column = new PostgreTableColumn(testTable);
        column.setName("column1");
        column.setTypeName("int4");
        column.setOrdinalPosition(1);
        List<PostgreTableColumn> cachedAttributes = (List<PostgreTableColumn>) testTable.getCachedAttributes();
        cachedAttributes.add(column);

        PostgreView testView = new PostgreView(testSchema);
        testView.setName("testView");
        testView.setPersisted(true);
    }

    @Test
    public void shouldFormatPostgresSqlQuery() throws ExecutionException, InterruptedException {
        String query = """
            DO $$ BEGIN CREATE TABLE logs (id serial PRIMARY KEY,message text,created_at timestamptz DEFAULT now());ELSE RAISE NOTICE 'Table "logs" already exists.';END IF;END $$;
            """.trim();
        TextDocumentItem textDocument = new TextDocumentItem();
        textDocument.setText(query);
        textDocument.setUri(DocumentServiceUtils.BASIC_SQL_URI);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));
        DocumentFormattingParams formattingParams = new DocumentFormattingParams();
        formattingParams.setTextDocument(new TextDocumentIdentifier(DocumentServiceUtils.BASIC_SQL_URI));
        FormattingOptions formattingOptions = new FormattingOptions();
        DBPDataSourceContainer container = testDataSource.getContainer();
        formattingOptions.putString(DBLTextDocumentService.PROJECT_ID_OPTION, container.getProject().getId());
        formattingOptions.putString(DBLTextDocumentService.DATA_SOURCE_ID_OPTION, container.getId());
        formattingParams.setOptions(formattingOptions);

        CompletableFuture<List<? extends TextEdit>> future = service.formatting(formattingParams);

        TextEdit textEdit = future.get().getFirst();
        String expectedQuery = """
            DO $$
            BEGIN
                CREATE TABLE logs (id serial PRIMARY KEY,
                message text,
                created_at timestamptz DEFAULT now());
            ELSE RAISE NOTICE 'Table "logs" already exists.';
            END IF;
            END $$;
            """.trim();

        Assert.assertEquals(expectedQuery.trim(), textEdit.getNewText());
    }
}
