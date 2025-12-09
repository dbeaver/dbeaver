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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DBLTextDocumentServiceDataSourceTest extends DBeaverUnitTest {
    private final DBRProgressMonitor monitor = new VoidProgressMonitor();
    private DBPDataSourceContainer dataSourceContainer;

    private final DBLTextDocumentService service = new DBLTextDocumentService();

    @Before
    public void setUp() throws DBException, NoSuchFieldException, IllegalAccessException {
        var driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("postgresql");
        var configuration = new DBPConnectionConfiguration();
        dataSourceContainer = new DataSourceDescriptor(
            DBWorkbench.getPlatform().getWorkspace().getActiveProject().getDataSourceRegistry(),
            DataSourceDescriptor.generateNewId(driver),
            driver,
            configuration
        );
        dataSourceContainer.setName("Test DB");
        dataSourceContainer.setTemporary(true);

        var testDataSource = new PostgreDataSource(dataSourceContainer, "PG Test", "postgres") {
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

        Field dataSourceField = dataSourceContainer.getClass().getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        dataSourceField.set(dataSourceContainer, testDataSource);

        var project = DBWorkbench.getPlatform().getWorkspace().getProjects().getFirst();
        project.getDataSourceRegistry().addDataSource(dataSourceContainer);

//        PostgreRole testUser = new PostgreRole(null, "tester", "test", true);
//        PostgreDatabase testDatabase = testDataSource.createDatabaseImpl(
//            monitor, "testdb", testUser, null, null, null
//        );
//        PostgreSchema testSchema = new PostgreSchema(testDatabase, "test_schema", testUser);
//
//        PostgreTableRegular testTable = new PostgreTableRegular(testSchema);
//        testTable.setName("test_table");
//        testTable.setPartition(false);
//        testTable.setPersisted(true);
//
//        PostgreTableColumn column = new PostgreTableColumn(testTable);
//        column.setName("column1");
//        column.setTypeName("int4");
//        column.setOrdinalPosition(1);
//        List<PostgreTableColumn> cachedAttributes = (List<PostgreTableColumn>) testTable.getCachedAttributes();
//        cachedAttributes.add(column);
    }

    @Test
    public void shouldFormatPostgresSqlQuery() throws ExecutionException, InterruptedException {
        String query = """
            INSERT INTO users (id, profile) VALUES (1,'{"name": "JohnDoe"}'::jsonb) ON CONFLICT (id) DO UPDATE SET profile = users.profile || EXCLUDED.profile RETURNING id, profile->>'name' AS name;
            """.trim();
        TextDocumentItem textDocument = new TextDocumentItem();
        textDocument.setText(query);
        textDocument.setUri(DocumentServiceUtils.BASIC_SQL_URI);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));
        DocumentFormattingParams formattingParams = new DocumentFormattingParams();
        formattingParams.setTextDocument(new TextDocumentIdentifier(DocumentServiceUtils.BASIC_SQL_URI));
        FormattingOptions formattingOptions = new FormattingOptions();
        formattingOptions.putString(DBLTextDocumentService.PROJECT_ID_OPTION, dataSourceContainer.getProject().getId());
        formattingOptions.putString(DBLTextDocumentService.DATA_SOURCE_ID_OPTION, dataSourceContainer.getId());
        formattingParams.setOptions(formattingOptions);

        CompletableFuture<List<? extends TextEdit>> future = service.formatting(formattingParams);

        TextEdit textEdit = future.get().getFirst();
        String expectedQuery = """
            insert
                into
                users (id,
                profile)
            values (1,
            '{"name": "JohnDoe"}'::jsonb) on
            CONFLICT (id) DO
            update
            set
                profile = users.profile || EXCLUDED.profile RETURNING id,
                profile->>'name' as name;
            """.trim();

        Assert.assertEquals(expectedQuery.trim(), textEdit.getNewText());
    }
}
