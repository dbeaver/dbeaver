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
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import java.io.IOException;
import java.io.StringWriter;
import java.util.stream.IntStream;

public class DataExporterDbUnitExpertRowTest extends DBeaverUnitTest {

    private String outputEncoding = "UTF-8";

    private String tableName = "test_table";

    private String columnName = "test_column";

    private DataExporterDbUnit exporter;

    private StringWriter stringWriter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DBCSession session;

    @Mock
    private DBCResultSet resultSet;

    @Test
    public void simpleTextRowNoSubstitutions() throws DBException, IOException {
        // given
        String simpleTextRow = "simple text row";
        // when
        writeRow(simpleTextRow);
        // then
        assertOutputMatches(simpleTextRow);
    }

    @Test
    public void textRowWithSingleQuoteShouldBeEscaped() throws DBException, IOException {
        // given
        String textRowWithSingleQuote = "'";
        String expectedRow = "&#39;";

        // when
        writeRow(textRowWithSingleQuote);

        // then
        assertOutputMatches(expectedRow);
    }

    @Test
    public void textRowWithAllSpecialSymbolsShouldBeReplaced() throws DBException, IOException {
        // given
        String textRowWithSpecialXmlChars = """
            <>&"'abc'""";
        String expectedRow = "&lt;&gt;&amp;&quot;&#39;abc&#39;";

        // when
        writeRow(textRowWithSpecialXmlChars);

        // then
        assertOutputMatches(expectedRow);
    }

    @Test
    public void textRowWithQuotesShouldBeReplaced() throws DBException, IOException {
        // given
        String textRowWithSpecialXmlChars = """
            {"delivery":"express","type":"'vip'"}""";

        String expectedRow =
            "{&quot;delivery&quot;:&quot;express&quot;,&quot;type&quot;:&quot;&#39;vip&#39;&quot;}";

        // when
        writeRow(textRowWithSpecialXmlChars);

        // then
        assertOutputMatches(expectedRow);
    }

    @BeforeEach
    public void setUp() throws DBException {
        stringWriter = new StringWriter();
        IStreamDataExporterSite mockSite = ExporterTestsUtils.getIStreamDataExporterSiteMock(
            tableName,
            columnName,
            stringWriter,
            outputEncoding
        );

        exporter = new DataExporterDbUnit();
        exporter.init(mockSite);
    }

    private void writeRow(@NotNull String row) throws DBException, IOException {
        exporter.exportHeader(session);
        exporter.exportRow(session, resultSet, new Object[]{row});
        exporter.exportFooter(null);
    }

    private void assertOutputMatches(@NotNull String expectedRow) {
        //EOT independent comparison
        var actualOutput = stringWriter.toString().lines().toList();
        var expectedOutput = constructExpectedOutput(expectedRow).lines().toList();
        IntStream.range(0, actualOutput.size())
            .forEach(i -> Assertions.assertEquals(expectedOutput.get(i), actualOutput.get(i)));
    }

    private String constructExpectedOutput(@NotNull String row) {
        return """
            <?xml version="1.0" encoding="%s"?>
            <dataset>
                <%s %s="%s"/>
            </dataset>
            """.formatted(outputEncoding, tableName.toUpperCase(), columnName.toUpperCase(), row);
    }
}
