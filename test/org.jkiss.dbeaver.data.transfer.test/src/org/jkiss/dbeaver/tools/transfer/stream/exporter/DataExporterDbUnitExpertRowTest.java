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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void textRowWithAllSpecialSymbolsShouldBeReplaced() throws DBException, IOException {
        // given
        String textRowWithSpecialXmlChars = """
        <>&"abc'""";
        String expectedRow = "&lt;&gt;&amp;&quot;abc'";
        // when
        writeRow(textRowWithSpecialXmlChars);
        // then
        assertOutputMatches(expectedRow);
    }

    @Test
    public void textRowWithQuotesShouldBeReplaced() throws DBException, IOException {
        // given
        String textRowWithSpecialXmlChars = """
        {"delivery":"express"}""";
        String expectedRow = "{&quot;delivery&quot;:&quot;express&quot;}";
        // when
        writeRow(textRowWithSpecialXmlChars);
        // then
        assertOutputMatches(expectedRow);
    }

    @Before
    public void setUp() throws DBException {
        stringWriter = new StringWriter();
        IStreamDataExporterSite mockSite = getIStreamDataExporterSiteMock();

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
            .forEach(i -> assertEquals(expectedOutput.get(i), actualOutput.get(i)));
    }

    private String constructExpectedOutput(@NotNull String row) {
        return """
            <?xml version="1.0" encoding="%s"?>
            <dataset>
                <%s %s="%s"/>
            </dataset>
            """.formatted(outputEncoding, tableName.toUpperCase(), columnName.toUpperCase(), row);
    }

    @NotNull
    private IStreamDataExporterSite getIStreamDataExporterSiteMock() {
        DBPNamedObject mockSource = mock(DBPNamedObject.class);
        when(mockSource.getName()).thenReturn(tableName);

        DBDValueHandler valueHandler = getDbdValueHandlerMock();

        DBDAttributeBinding mockBinding = mock(DBDAttributeBinding.class);
        when(mockBinding.getName()).thenReturn(columnName);
        when(mockBinding.getValueHandler()).thenReturn(mock(DBDValueHandler.class));
        when(mockBinding.getValueHandler()).thenReturn(valueHandler);


        PrintWriter pw = new PrintWriter(stringWriter);

        IStreamDataExporterSite mockSite = mock(IStreamDataExporterSite.class);
        when(mockSite.getOutputEncoding()).thenReturn(outputEncoding);
        when(mockSite.getWriter()).thenReturn(pw);
        when(mockSite.getAttributes()).thenReturn(new DBDAttributeBinding[]{mockBinding});
        when(mockSite.getSource()).thenReturn(mockSource);
        return mockSite;
    }

    @NotNull
    private DBDValueHandler getDbdValueHandlerMock() {
        return new DBDValueHandler() {
            @NotNull
            @Override
            public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
                return String.class;
            }

            @Nullable
            @Override
            public String getValueContentType(@NotNull DBSTypedObject attribute) {
                return null;
            }

            @Nullable
            @Override
            public Object fetchValueObject(
                @NotNull DBCSession session,
                @NotNull DBCResultSet resultSet,
                @NotNull DBSTypedObject type,
                int index
            ) throws DBCException {
                return null;
            }

            @Override
            public void bindValueObject(
                @NotNull DBCSession session,
                @NotNull DBCStatement statement,
                @NotNull DBSTypedObject type,
                int index,
                @Nullable Object value
            ) throws DBCException {

            }

            @Nullable
            @Override
            public Object getValueFromObject(
                @NotNull DBCSession session,
                @NotNull DBSTypedObject type,
                @Nullable Object object,
                boolean copy,
                boolean validateValue
            ) throws DBCException {
                return object;
            }

            @Nullable
            @Override
            public Object createNewValueObject(@NotNull DBCSession session, @NotNull DBSTypedObject type) throws DBCException {
                return null;
            }

            @Override
            public void releaseValueObject(@Nullable Object value) {

            }

            @NotNull
            @Override
            public DBCLogicalOperator[] getSupportedOperators(@NotNull DBSTypedObject attribute) {
                return new DBCLogicalOperator[0];
            }

            @NotNull
            @Override
            public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
                return value == null ? "" : value.toString();
            }
        };
    }
}
