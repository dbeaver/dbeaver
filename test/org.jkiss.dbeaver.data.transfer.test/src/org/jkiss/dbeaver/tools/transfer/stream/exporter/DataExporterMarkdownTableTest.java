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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DataExporterMarkdownTableTest extends DBeaverUnitTest {
    public static final String TEST_COLUMN_NAME = "test_column";
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
    public void multilineValueIsKeptInOneTableRow() throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {"first\nsecond\r\nthird\rfourth"});

         assertOutputMatches("|first<br>second<br>third<br>fourth|");
    }

    @Test
    public void pipeAndMultilineValueAreEscapedTogether() throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {"first|second\nthird"});

         assertOutputMatches("|first&#124;second<br>third|");
    }

    @Test
    public void consecutiveAndTrailingLineBreaksArePreserved() throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {"first\n\nsecond\n"});

         assertOutputMatches("|first<br><br>second<br>|");
    }

    @Test
    public void multilineContentReaderIsKeptInOneTableRow() throws Exception {
        when(content.getContentType()).thenReturn("text/plain");
        when(content.getContents(any())).thenReturn(contentStorage);
        when(contentStorage.getContentReader()).thenReturn(new StringReader("first\r\nsecond\nthird"));

        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {content});

         assertOutputMatches("|first<br>second<br>third|");
    }

    @Test
    public void readerCrLfSplitAcrossBuffersIsWrittenAsOneBreak() throws Exception {
        when(content.getContentType()).thenReturn("text/plain");
        when(content.getContents(any())).thenReturn(contentStorage);
        when(contentStorage.getContentReader()).thenReturn(new ChunkedReader("first\r\nsecond"));

        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[] {content});

         assertOutputMatches("|first<br>second|");
     }

     private void assertOutputMatches(String expectedRow) {
         String expectedOutput = "|" + TEST_COLUMN_NAME + "|" + System.lineSeparator()
             + "|-----------|" + System.lineSeparator()
             + expectedRow + System.lineSeparator();
         assertEquals(expectedOutput, stringWriter.toString());
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
