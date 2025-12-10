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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.h2.model.H2SQLDialect;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;
import org.jkiss.dbeaver.model.lsp.context.ContextAwareDocument;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DBLTextDocumentServiceContextTest extends DBeaverUnitTest {
    private final DBLTextDocumentService service = new DBLTextDocumentService();

    private DBPDataSourceContainer dataSourceContainer;
    private DBPProject project;
    private JDBCSession databaseSession;
    private final DBRProgressMonitor monitor = new LoggingProgressMonitor();

    @Before
    public void setUp() throws DBException {
        PrefUtils.setDefaultPreferenceValue(
            DBWorkbench.getPlatform().getPreferenceStore(),
            ModelPreferences.UI_DRIVERS_HOME,
            Path.of("../../../dbeaver-resources-drivers-jdbc/binaries")
        );

        dataSourceContainer = DocumentServiceTestUtils.createDataSource(monitor);
        databaseSession = DBUtils.openUtilSession(monitor, dataSourceContainer, "Internal test session");
        project = DBWorkbench.getPlatform().getWorkspace().getProjects().getFirst();
        project.getDataSourceRegistry().addDataSource(dataSourceContainer);

        try (JDBCStatement stmt = databaseSession.createStatement()) {
            Assert.assertFalse(stmt.execute("CREATE TABLE TEST_TABLE1 (id IDENTITY NOT NULL PRIMARY KEY, a VARCHAR, b INT)"));
            Assert.assertFalse(stmt.execute("CREATE TABLE TEST_TABLE2 (id IDENTITY NOT NULL PRIMARY KEY, a VARCHAR, b INT)"));
            /*for (int i = 0; i < 100; i++) {
                assertFalse(stmt.execute("INSERT INTO TEST_TABLE1 (a, b) VALUES ('test" + i + "', " + i + ")"));
                assertFalse(stmt.execute("INSERT INTO TEST_TABLE2 (a, b) VALUES ('test" + i + "', " + i + ")"));
            }*/
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void after() {
        DocumentServiceTestUtils.clearDocuments(service);
    }

    @Test
    public void shouldInitH2Context() {
        TextDocumentItem document = DocumentServiceTestUtils.createAndSaveDocument(service, "select * from table");

        service.initContext(
            new TextDocumentIdentifier(document.getUri()),
            project.getId(),
            dataSourceContainer.getId()
        );

        ContextAwareDocument contextedDocument = DocumentServiceTestUtils.getDocument(service, document.getUri());
        Assert.assertNotNull(contextedDocument);
        Assert.assertEquals(dataSourceContainer.getDataSource(), contextedDocument.getDataSource());
        Assert.assertNotNull(contextedDocument.getExecutionContext());
        Assert.assertEquals(dataSourceContainer.getDataSource(), contextedDocument.getExecutionContext().getDataSource());
        Assert.assertTrue(contextedDocument.getSyntaxManager().getDialect() instanceof H2SQLDialect);
        Assert.assertNotNull(contextedDocument.getRuleManager());
    }

    @Test
    public void shouldFormatQuery() throws ExecutionException, InterruptedException {
        String query = """
            INSERT INTO users (id, profile) VALUES (1,'{"name": "JohnDoe"}'::jsonb) ON CONFLICT (id)
            DO UPDATE SET profile = users.profile || EXCLUDED.profile RETURNING id, profile->>'name' AS name;
            """.trim();
        DocumentFormattingParams formattingParams = DocumentServiceTestUtils.setupDocumentAndBuildFormattingParams(service, query);
        service.initContext(formattingParams.getTextDocument(), project.getId(), dataSourceContainer.getId());

        CompletableFuture<List<? extends TextEdit>> future = service.formatting(formattingParams);

        TextEdit edit = future.get().getFirst();
        String expectedQuery = """
            INSERT
                INTO
                users (id,
                profile)
            VALUES (1,
            '{"name": "JohnDoe"}'::jsonb) ON
            CONFLICT (id)
            DO
            UPDATE
            SET
                profile = users.profile || EXCLUDED.profile RETURNING id,
                profile->>'name' AS name;
            """.trim();

        Assert.assertEquals(expectedQuery.trim(), edit.getNewText());
        Position start = edit.getRange().getStart();
        Assert.assertEquals(0, start.getLine());
        Assert.assertEquals(0, start.getCharacter());

        Position end = edit.getRange().getEnd();
        Assert.assertEquals(1, end.getLine());
        Assert.assertEquals(97, end.getCharacter());
    }

    @Test
    public void shouldSuggestKeywordCompletion() throws ExecutionException, InterruptedException {
        String query = "SEL";
        ContextAwareDocument document = DocumentServiceTestUtils.createAndSaveDocument(service, query);
        TextDocumentIdentifier documentId = new TextDocumentIdentifier(document.getUri());
        service.initContext(documentId, project.getId(), dataSourceContainer.getId());
        CompletionParams completionParams = new CompletionParams(documentId, new Position(0, 3));

        CompletionList completions = service.completion(completionParams).get().getRight();

        Assert.assertNotNull(completions);
        Assert.assertFalse(completions.getItems().isEmpty());
        Assert.assertEquals("SELECT", completions.getItems().getFirst().getLabel());
    }

    @Test
    public void shouldSuggestMultilineKeywordCompletion() throws ExecutionException, InterruptedException {
        String query = """
            SELECT * FROM TEST_TABLE1
                WH
            """;
        ContextAwareDocument document = DocumentServiceTestUtils.createAndSaveDocument(service, query);
        TextDocumentIdentifier documentId = new TextDocumentIdentifier(document.getUri());
        service.initContext(documentId, project.getId(), dataSourceContainer.getId());
        CompletionParams completionParams = new CompletionParams(documentId, new Position(1, 6));

        CompletionList completions = service.completion(completionParams).get().getRight();

        Assert.assertNotNull(completions);
        Assert.assertFalse(completions.getItems().isEmpty());
        Assert.assertEquals("WHERE", completions.getItems().getFirst().getLabel());
    }

    @Test
    public void shouldSuggestTableNameCompletion() throws ExecutionException, InterruptedException {
        String query = "SELECT * FROM TEST_";
        ContextAwareDocument document = DocumentServiceTestUtils.createAndSaveDocument(service, query);
        TextDocumentIdentifier documentId = new TextDocumentIdentifier(document.getUri());
        service.initContext(documentId, project.getId(), dataSourceContainer.getId());
        CompletionParams completionParams = new CompletionParams(documentId, new Position(0, 19));

        CompletionList completions = service.completion(completionParams).get().getRight();

        Assert.assertNotNull(completions);
        List<String> items = completions.getItems().stream().map(CompletionItem::getLabel).toList();
        Assert.assertEquals(2, items.size());
        Assert.assertTrue(items.contains("TEST_TABLE1"));
        Assert.assertTrue(items.contains("TEST_TABLE2"));
    }
}
