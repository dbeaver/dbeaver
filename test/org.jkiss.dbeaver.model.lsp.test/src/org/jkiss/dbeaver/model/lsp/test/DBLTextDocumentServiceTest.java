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
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;
import org.jkiss.dbeaver.model.lsp.context.ContextAwareDocument;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Test scenarios to cover DBLTextDocumentService
 */
public class DBLTextDocumentServiceTest extends DBeaverUnitTest {
    private DBLTextDocumentService service;

    @Before
    public void setUp() {
        service = new DBLTextDocumentService();
    }

    @Test
    public void shouldOpenDocumentWithArbitraryURI() {
        String query = "SELECT * FROM table";
        String uri = "file:///Users/username/script.sql";
        TextDocumentItem textDocument = new TextDocumentItem(
            uri, DocumentServiceTestUtils.SQL_LANGUAGE_ID, 0, query
        );
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocument);

        service.didOpen(params);

        ContextAwareDocument document = DocumentServiceTestUtils.getDocument(service, uri);
        Assert.assertNotNull(document);
        Assert.assertEquals(document.getSyntaxManager().getDialect(), BasicSQLDialect.INSTANCE);
        Assert.assertNull(document.getExecutionContext());
    }

    @Test
    public void shouldOpenDocument() {
        String query = "SELECT * FROM table";
        TextDocumentItem textDocument = DocumentServiceTestUtils.createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        ContextAwareDocument savedDocument = DocumentServiceTestUtils.getDocument(service, textDocument.getUri());
        Assert.assertNotNull(savedDocument);
        Assert.assertEquals(query, savedDocument.getText());

        SQLSyntaxManager syntaxManager = savedDocument.getSyntaxManager();
        Assert.assertNotNull(syntaxManager);
        Assert.assertEquals(BasicSQLDialect.INSTANCE, syntaxManager.getDialect());
        SQLRuleManager ruleManager = savedDocument.getRuleManager();
        Assert.assertNotNull(ruleManager);
    }

    @Test
    public void shouldInitDefaultSyntax() {
        TextDocumentItem textDocument = DocumentServiceTestUtils.createQueryDocument("SELECT * FROM table");
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        ContextAwareDocument savedDocument = Objects.requireNonNull(DocumentServiceTestUtils.getDocument(service, textDocument.getUri()));
        SQLSyntaxManager syntaxManager = savedDocument.getSyntaxManager();
        Assert.assertNotNull(syntaxManager);
        Assert.assertEquals(BasicSQLDialect.INSTANCE, syntaxManager.getDialect());
        SQLRuleManager ruleManager = savedDocument.getRuleManager();
        Assert.assertNotNull(ruleManager);
    }

    @Test
    public void shouldOpenAndChangeDocument() {
        String query = "SELECT * FROM table";
        TextDocumentItem textDocument = DocumentServiceTestUtils.createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        String updatedSql = "SELECT DISTINCT * FROM table";
        VersionedTextDocumentIdentifier textDocumentChange = new VersionedTextDocumentIdentifier(textDocument.getUri(), 0);
        TextDocumentContentChangeEvent event = new TextDocumentContentChangeEvent(updatedSql);
        List<TextDocumentContentChangeEvent> contentChanges = List.of(event);
        service.didChange(new DidChangeTextDocumentParams(textDocumentChange, contentChanges));

        ContextAwareDocument updatedDocument = DocumentServiceTestUtils.getDocument(service, textDocument.getUri());
        Assert.assertNotNull(updatedDocument);
        Assert.assertEquals(updatedSql, updatedDocument.getText());
    }

    @Test
    public void shouldFailSubmittingMultipleChangesToDocument() {
        String query = "SELECT * FROM table";
        TextDocumentItem textDocument = DocumentServiceTestUtils.createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        String updatedSql1 = "SELECT DISTINCT * FROM table";
        String updatedSql2 = "DROP TABLE IF EXISTS table";
        VersionedTextDocumentIdentifier textDocumentChange = new VersionedTextDocumentIdentifier(textDocument.getUri(), 0);
        TextDocumentContentChangeEvent event1 = new TextDocumentContentChangeEvent(updatedSql1);
        TextDocumentContentChangeEvent event2 = new TextDocumentContentChangeEvent(updatedSql2);
        List<TextDocumentContentChangeEvent> contentChanges = List.of(event1, event2);

        Assert.assertThrows(
            "Unexpected number of document changes: 2",
            IllegalArgumentException.class,
            () -> service.didChange(new DidChangeTextDocumentParams(textDocumentChange, contentChanges))
        );
    }

    @Test
    public void shouldOpenAndCloseDocument() {
        String query = "SELECT * FROM table";
        TextDocumentItem textDocument = DocumentServiceTestUtils.createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        TextDocumentIdentifier textDocumentId = new TextDocumentIdentifier(textDocument.getUri());
        DidCloseTextDocumentParams closeParams = new DidCloseTextDocumentParams(textDocumentId);
        service.didClose(closeParams);

        ContextAwareDocument updatedDocument = DocumentServiceTestUtils.getDocument(service, textDocument.getUri());
        Assert.assertNull(updatedDocument);
    }

    @Test
    public void shouldFormatSingleLineQuery() throws ExecutionException, InterruptedException {
        String query = "sElEcT dIsTiNcT * fRoM tablename As alias;";
        var formattingParams = DocumentServiceTestUtils.setupDocumentAndBuildFormattingParams(service, query);

        CompletableFuture<List<? extends TextEdit>> future = service.formatting(formattingParams);

        TextEdit textEdit = future.get().getFirst();
        String expectedQuery = """
            SELECT
                DISTINCT *
            FROM
                tablename AS alias;
            """;
        Assert.assertEquals(expectedQuery.trim(), textEdit.getNewText());

        Position start = textEdit.getRange().getStart();
        Assert.assertEquals(0, start.getCharacter());
        Assert.assertEquals(0, start.getLine());

        Position end = textEdit.getRange().getEnd();
        Assert.assertEquals(0, end.getLine());
        Assert.assertEquals(42, end.getCharacter());
    }

    @Test
    public void shouldFormatMultilineQuery() throws ExecutionException, InterruptedException {
        String query = """
            select dbname1.schemaname1.tablename1.columnname1, schemaname2.tablename2.columnname2,
                tablename3.columnname3 from
            dbname1.schemaname1.tablename1,dbname2.schemaname2.tablename2,schemaname3.tablename3
            ;
            """.trim();
        var formattingParams = DocumentServiceTestUtils.setupDocumentAndBuildFormattingParams(service, query);

        CompletableFuture<List<? extends TextEdit>> future = service.formatting(formattingParams);

        TextEdit textEdit = future.get().getFirst();
        String expectedQuery = """
            SELECT
                dbname1.schemaname1.tablename1.columnname1,
                schemaname2.tablename2.columnname2,
                tablename3.columnname3
            FROM
                dbname1.schemaname1.tablename1,
                dbname2.schemaname2.tablename2,
                schemaname3.tablename3
            ;
            """.trim();
        Assert.assertEquals(expectedQuery.trim(), textEdit.getNewText());

        Position start = textEdit.getRange().getStart();
        Assert.assertEquals(0, start.getCharacter());
        Assert.assertEquals(0, start.getLine());

        Position end = textEdit.getRange().getEnd();
        Assert.assertEquals(3, end.getLine());
        Assert.assertEquals(1, end.getCharacter());
    }

    @Test
    public void shouldFormatDialectSpecificQuery() throws ExecutionException, InterruptedException {
        String query = """
            DO $$ BEGIN CREATE TABLE logs (id serial PRIMARY KEY,message text,created_at timestamptz DEFAULT now());
            ELSE RAISE NOTICE 'Table "logs" already exists.';END IF;END $$;
            """.trim();
        var formattingParams = DocumentServiceTestUtils.setupDocumentAndBuildFormattingParams(service, query);

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
        Position end = textEdit.getRange().getEnd();
        Assert.assertEquals(1, end.getLine());
        Assert.assertEquals(63, end.getCharacter());
    }

    @Test
    public void shouldReturnKeywordTokenData() throws ExecutionException, InterruptedException {
        String query = "SELECT";
        TextDocumentItem document = DocumentServiceTestUtils.createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(document));

        SemanticTokensParams params = new SemanticTokensParams(new TextDocumentIdentifier(document.getUri()));
        Integer[] tokensData = service.semanticTokensFull(params).get().getData().toArray(new Integer[0]);

        Assert.assertArrayEquals(
            new Integer[] {0, 0, 6, 0, 0},
            tokensData
        );
    }

    @Test
    public void shouldReturnMultipleTokensData() throws ExecutionException, InterruptedException {
        String query = "SELECT name FROM users WHERE surname = 'Doe'";
        TextDocumentItem document = DocumentServiceTestUtils.createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(document));

        SemanticTokensParams params = new SemanticTokensParams(new TextDocumentIdentifier(document.getUri()));
        Integer[] tokensData = service.semanticTokensFull(params).get().getData().toArray(new Integer[0]);

        Integer[] expectedData = {
            0, 0, 6, 0, 0,   // SELECT
            0, 12, 4, 0, 0,  // FROM
            0, 23, 5, 0, 0,  // WHERE
            0, 39, 5, 1, 0   // 'Doe'
        };
        Assert.assertArrayEquals(
            expectedData,
            tokensData
        );
    }

    @Test
    public void shouldReturnMultilineTokensData() throws ExecutionException, InterruptedException {
        String query = """
            SELECT name
            FROM
                users
            WHERE
                surname = 'Doe';
            """;
        TextDocumentItem document = DocumentServiceTestUtils.createQueryDocument(query);
        service.didOpen(new DidOpenTextDocumentParams(document));

        SemanticTokensParams params = new SemanticTokensParams(new TextDocumentIdentifier(document.getUri()));
        Integer[] tokensData = service.semanticTokensFull(params).get().getData().toArray(new Integer[0]);

        Integer[] expectedData = {
            0, 0, 6, 0, 0,   // SELECT
            1, 0, 4, 0, 0,   // FROM
            3, 0, 5, 0, 0,   // WHERE
            4, 14, 5, 1, 0   // 'Doe'
        };
        Assert.assertArrayEquals(
            expectedData,
            tokensData
        );
    }
}
