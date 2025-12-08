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
import org.jkiss.dbeaver.model.lsp.DBLTextDocumentService;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Test scenarios to cover DBLTextDocumentService
 */
public class DBLTextDocumentServiceTest extends DBeaverUnitTest {

    public static final String BASIC_SQL_URI = "sql/scripts/basic.sql";
    private final DBLTextDocumentService service = new DBLTextDocumentService();

    @Test
    public void shouldOpenDocument() {
        TextDocumentItem textDocument = new TextDocumentItem();
        String sql = "SELECT * FROM table";
        textDocument.setText(sql);
        textDocument.setUri(BASIC_SQL_URI);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        String savedText = service.getText(textDocument.getUri());
        Assert.assertEquals(sql, savedText);
    }

    @Test
    public void shouldOpenAndChangeDocument() {
        TextDocumentItem textDocument = new TextDocumentItem();
        String sql = "SELECT * FROM table";
        textDocument.setText(sql);
        textDocument.setUri(BASIC_SQL_URI);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        String updatedSql = "SELECT DISTINCT * FROM table";
        VersionedTextDocumentIdentifier textDocumentChange = new VersionedTextDocumentIdentifier(textDocument.getUri(), 0);
        TextDocumentContentChangeEvent event = new TextDocumentContentChangeEvent(updatedSql);
        List<TextDocumentContentChangeEvent> contentChanges = List.of(event);
        service.didChange(new DidChangeTextDocumentParams(textDocumentChange, contentChanges));

        String savedText = service.getText(textDocument.getUri());
        Assert.assertEquals(updatedSql, savedText);
    }

    @Test
    public void shouldFailSubmittingMultipleChangesToDocument() {
        TextDocumentItem textDocument = new TextDocumentItem();
        String sql = "SELECT * FROM table";
        textDocument.setText(sql);
        textDocument.setUri(BASIC_SQL_URI);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        String updatedSql1 = "SELECT DISTINCT * FROM table";
        String updatedSql2 = "DROP TABLE IF EXISTS table";
        VersionedTextDocumentIdentifier textDocumentChange = new VersionedTextDocumentIdentifier(textDocument.getUri(), 0);
        TextDocumentContentChangeEvent event1 = new TextDocumentContentChangeEvent(updatedSql1);
        TextDocumentContentChangeEvent event2 = new TextDocumentContentChangeEvent(updatedSql2);
        List<TextDocumentContentChangeEvent> contentChanges = List.of(event1, event2);

        Assert.assertThrows("Unexpected number of document changes: 2",
            IllegalArgumentException.class,
            () -> service.didChange(new DidChangeTextDocumentParams(textDocumentChange, contentChanges))
        );
    }

    @Test
    public void shouldOpenAndCloseDocument() {
        TextDocumentItem textDocument = new TextDocumentItem();
        String sql = "SELECT * FROM table";
        textDocument.setText(sql);
        textDocument.setUri(BASIC_SQL_URI);
        service.didOpen(new DidOpenTextDocumentParams(textDocument));

        TextDocumentIdentifier textDocumentId = new TextDocumentIdentifier(textDocument.getUri());
        DidCloseTextDocumentParams closeParams = new DidCloseTextDocumentParams(textDocumentId);
        service.didClose(closeParams);

        String text = service.getText(textDocument.getUri());
        Assert.assertNull(text);
    }

    @Test
    public void shouldFormatSingleLineQuery() throws ExecutionException, InterruptedException {
        String sql = "sElEcT dIsTiNcT * fRoM tablename As alias;";
        var formattingParams = setupDocumentAndBuildFormattingParams(sql);

        CompletableFuture<List<? extends TextEdit>> future = service.formatting(formattingParams);

        TextEdit textEdit = future.get().getFirst();
        String expectedSql = """
            SELECT
                DISTINCT *
            FROM
                tablename AS alias;
            """;
        Assert.assertEquals(expectedSql.trim(), textEdit.getNewText());

        Position start = textEdit.getRange().getStart();
        Assert.assertEquals(0, start.getCharacter());
        Assert.assertEquals(0, start.getLine());

        Position end = textEdit.getRange().getEnd();
        Assert.assertEquals(0, end.getLine());
        Assert.assertEquals(42, end.getCharacter());
    }

    @Test
    public void shouldFormatMultilineQuery() throws ExecutionException, InterruptedException {
        String sql = """
            select dbname1.schemaname1.tablename1.columnname1, schemaname2.tablename2.columnname2,
                tablename3.columnname3 from
            dbname1.schemaname1.tablename1,dbname2.schemaname2.tablename2,schemaname3.tablename3
            ;
            """.trim();
        var formattingParams = setupDocumentAndBuildFormattingParams(sql);

        CompletableFuture<List<? extends TextEdit>> future = service.formatting(formattingParams);

        TextEdit textEdit = future.get().getFirst();
        String expectedSql = """
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
        Assert.assertEquals(expectedSql.trim(), textEdit.getNewText());

        Position start = textEdit.getRange().getStart();
        Assert.assertEquals(0, start.getCharacter());
        Assert.assertEquals(0, start.getLine());

        Position end = textEdit.getRange().getEnd();
        Assert.assertEquals(3, end.getLine());
        Assert.assertEquals(1, end.getCharacter());
    }

    private DocumentFormattingParams setupDocumentAndBuildFormattingParams(String sql) {
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
