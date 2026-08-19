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
import org.junit.jupiter.api.Nested;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DataExporterMarkdownTableTest extends DBeaverUnitTest {
    public static final String TEST_COLUMN_NAME = "test_column";
    public static final String BR = DataExporterMarkdownTable.NEW_LINE_ESCAPE;
    private DataExporterMarkdownTable exporter;
    private StringWriter stringWriter;
    private Map<String, Object> properties;
    private IStreamDataExporterSite site;

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
        properties = new HashMap<>();
        site = ExporterTestsUtils.getIStreamDataExporterSiteMock(
            "test_table", TEST_COLUMN_NAME, stringWriter, "UTF-8");
        when(site.getProperties()).thenReturn(properties);

        exporter = new DataExporterMarkdownTable();
        exporter.init(site);
    }

    @Nested
    class BasicValueTests {

        @Test
        public void simpleCase() throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"first"});

            assertOutputMatches("|first|");
        }

        @ParameterizedTest
        @ArgumentsSource(LineSeparatorArgumentsProvider.class)
        public void doubleLineSeparatorsAreConvertedToMarkdownBreaks(@NotNull String lineSeparator) throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {
                "first" + lineSeparator + "second" + lineSeparator.repeat(2) + "third" + lineSeparator
            });

            assertOutputMatches("|first" + BR + "second" + BR.repeat(2) + "third" + BR + "|");
        }

        @ParameterizedTest
        @ArgumentsSource(LineSeparatorArgumentsProvider.class)
        public void lineSeparatorsAreConvertedToMarkdownBreaks(@NotNull String lineSeparator) throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {
                lineSeparator + "first" + lineSeparator + "second" + lineSeparator
            });

            assertOutputMatches("|" + BR + "first" + BR + "second" + BR + "|");
        }

        @ParameterizedTest
        @ArgumentsSource(LineSeparatorArgumentsProvider.class)
        public void pipeAndMultilineValueAreEscapedTogether(@NotNull String lineSeparator) throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"first|second" + lineSeparator + "third"});

            assertOutputMatches("|first&#124;second" + BR + "third|");
        }
    }

    @Nested
    class ContentReaderTests {

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
    }

    @Nested
    class EscapeCellValueTest {

        @BeforeEach
        public void enableCellEscaping() throws DBException {
            properties.put(DataExporterMarkdownTable.PROP_ESCAPE_CELL_CONTENT, true);
            exporter.init(site);
        }

        @Test
        public void simpleValueIsWrappedInOneBacktick() throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"ab"});

            assertOutputMatches("|`ab`|");
        }

        @ParameterizedTest
        @ArgumentsSource(LineSeparatorArgumentsProvider.class)
        public void lineBreakSplitsCodeEscaping(@NotNull String lineSeparator) throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"a" + lineSeparator + "b"});

            assertOutputMatches("|`a`" + BR + "`b`|");
        }

        @Test
        public void oneBacktickUsesTwoBacktickDelimiter() throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"a`b"});

            assertOutputMatches("|``a`b``|");
        }

        @Test
        public void twoBackticksUseThreeBacktickDelimiter() throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"a`b``c"});

            assertOutputMatches("|```a`b``c```|");
        }

        @Test
        public void twoBackticksUseThreeBacktickDelimiterReverseOrder() throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"a``b`c"});

            assertOutputMatches("|```a``b`c```|");
        }

        @Test
        public void xmlValueIsWrappedInCodeSpan() throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"<root attr=\"value\">text</root>"});

            assertOutputMatches("|`<root attr=\"value\">text</root>`|");
        }

        @Test
        public void pipeSplitsCodeSpans() throws DBException, IOException {
            exporter.exportHeader(session);
            exporter.exportRow(session, resultSet, new Object[] {"a|b"});

            assertOutputMatches("|`a`&#124;`b`|");
        }

        @ParameterizedTest
        @ArgumentsSource(LineSeparatorArgumentsProvider.class)
        public void mixedLineSeparatorsPipesAndBackticksUseIndependentEscapes(@NotNull String lineSeparator) throws Exception {
            exporter.exportHeader(session);
            exporter.exportRow(
                session, resultSet, new Object[] {
                    "first`" + lineSeparator + "a|b" + lineSeparator + "second``"
                }
            );

            assertOutputMatches("|``first```" + BR + "`a`&#124;`b`" + BR + "```second`````|");
        }
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
