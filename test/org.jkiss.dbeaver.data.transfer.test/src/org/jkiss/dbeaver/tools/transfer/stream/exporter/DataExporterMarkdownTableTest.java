/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Answers;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DataExporterMarkdownTableTest extends DBeaverUnitTest {
    public static final String TEST_COLUMN_NAME = "test_column";
    public static final String BR = DataExporterMarkdownTable.NEW_LINE_ESCAPE;
    private DataExporterMarkdownTable exporter;
    private StringWriter stringWriter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DBCSession session;

    @Mock
    private DBCResultSet resultSet;

    @Mock
    private DBDContent content;

    @Mock
    private DBDContentStorage contentStorage;

    @BeforeEach
    public void setUp() throws DBException {
        stringWriter = new StringWriter();
        IStreamDataExporterSite site = ExporterTestsUtils.getIStreamDataExporterSiteMock(
            "test_table", TEST_COLUMN_NAME, stringWriter, "UTF-8");

        exporter = new DataExporterMarkdownTable();
        exporter.init(site);
    }

    @Test
    public void simpleCase() throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(
            session, resultSet, new Object[] {
                "first"
            }
        );

        assertOutputMatches("|first|");
    }

    @ParameterizedTest
    @ArgumentsSource(LineSeparatorArgumentsProvider.class)
    public void doubleLineSeparatorsAreConvertedToMarkdownBreaks(@NotNull String lineSeparator) throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(
            session, resultSet, new Object[] {
                "first" + lineSeparator + "second" + lineSeparator.repeat(2) + "third" + lineSeparator
            }
        );

        assertOutputMatches("|first" + BR + "second" + BR.repeat(2) + "third" + BR + "|");
    }

    @ParameterizedTest
    @ArgumentsSource(LineSeparatorArgumentsProvider.class)
    public void lineSeparatorsAreConvertedToMarkdownBreaks(@NotNull String lineSeparator) throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(
            session, resultSet, new Object[] {
                lineSeparator + "first" + lineSeparator + "second" + lineSeparator
            }
        );

        assertOutputMatches("|" + BR + "first" + BR + "second" + BR + "|");
    }

    @ParameterizedTest
    @ArgumentsSource(LineSeparatorArgumentsProvider.class)
    public void pipeAndMultilineValueAreEscapedTogether(@NotNull String lineSeparator) throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {"first|second" + lineSeparator + "third"});

        assertOutputMatches("|first&#124;second" + BR + "third|");
    }

    @ParameterizedTest
    @ArgumentsSource(LineSeparatorArgumentsProvider.class)
    public void multilineContentReaderIsKeptInOneTableRow(@NotNull String lineSeparator) throws Exception {
        when(content.getContentType()).thenReturn("text/plain");
        when(content.getContents(any())).thenReturn(contentStorage);
        when(contentStorage.getContentReader()).thenReturn(new StringReader("first" + lineSeparator + "second" + lineSeparator + "third"));

        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {content});

        assertOutputMatches("|first" + BR + "second" + BR + "third|");
    }

    @ParameterizedTest
    @ArgumentsSource(LineSeparatorArgumentsProvider.class)
    public void readerLineSeparatorSplitAcrossBuffersIsWrittenAsOneBreak(@NotNull String lineSeparator) throws Exception {
        when(content.getContentType()).thenReturn("text/plain");
        when(content.getContents(any())).thenReturn(contentStorage);
        when(contentStorage.getContentReader()).thenReturn(new ChunkedReader("first" + lineSeparator + "second"));

        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {content});

        assertOutputMatches("|first" + BR + "second|");
    }

    private void assertOutputMatches(@NotNull String expectedRow) {
        String expectedOutput = "|" + TEST_COLUMN_NAME + "|" + System.lineSeparator()
            + "|-----------|" + System.lineSeparator()
            + expectedRow + System.lineSeparator();
        assertEquals(expectedOutput, stringWriter.toString());
    }

    private static class LineSeparatorArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of("\n", "\r\n", "\r").map(Arguments::of);
        }
    }

    private static class ChunkedReader extends Reader {
        private final String value;
        private int offset;

        private ChunkedReader(String value) {
            this.value = value;
        }

        @Override
        public int read(char[] target, int targetOffset, int length) {
            if (offset >= value.length()) {
                return -1;
            }
            target[targetOffset] = value.charAt(offset++);
            return 1;
        }

        @Override
        public void close() {
        }
    }
}
