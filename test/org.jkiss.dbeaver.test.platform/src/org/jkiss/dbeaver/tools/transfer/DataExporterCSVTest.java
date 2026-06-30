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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStringValueHandler;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterCSV;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.mockito.Mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataExporterCSVTest extends DBeaverUnitTest {
    // must be used in expectedTemplate as separator
    private static final String DEFAULT_VALUE_SEPARATOR = ",";
    // must be used in expectedTemplate as quote
    private static final String DEFAULT_QUOTE = "\"";

    private static final String ALTERNATIVE_VALUE_SEPARATOR = ";";
    private static final String ALTERNATIVE_QUOTE = "~";

    private DataExporterCSV dataExporterCSV;
    private StringWriter stringWriter;
    private IStreamDataExporterSite site;

    @Mock
    private DBCSession dbcSession;

    @Mock
    private DBCResultSet resultSetMock;

    private Map<String, Object> properties;

    private String rowsSeparator;

    private DBDAttributeBinding[] columns;

    @BeforeEach
    public void setUp() {
        properties = new HashMap<>();
        stringWriter = new StringWriter();
        columns = new DBDAttributeBinding[]{};
        rowsSeparator = "\n";
        PrintWriter printWriter = new PrintWriter(stringWriter);

        site = mock(IStreamDataExporterSite.class);
        when(site.getWriter()).thenReturn(printWriter);
        when(site.getProperties()).thenReturn(properties);
        when(site.getAttributes()).thenReturn(columns);
    }

    @Test
    public void testExportHeader() throws DBException, IOException {
        // given
        addColumn("ID", "Identifier", JDBCStringValueHandler.INSTANCE);
        addColumn("NAME", "Name", JDBCStringValueHandler.INSTANCE);
        addColumn("AGE", "Age", JDBCStringValueHandler.INSTANCE);
        // when
        initExporter();
        dataExporterCSV.exportHeader(dbcSession);
        // then
        String expectedHeader = "\"IDENTIFIER\",\"NAME\",\"AGE\"" + rowsSeparator;
        assertEquals(expectedHeader, stringWriter.toString());
    }

    @Test
    public void testNotThrowsIfQuoteAndSeparatorAreSameCharAndQuoteNever() throws DBException {
        // given
        properties.put(DataExporterCSV.PROP_DELIMITER, ALTERNATIVE_VALUE_SEPARATOR);
        properties.put(DataExporterCSV.PROP_QUOTE_CHAR, ALTERNATIVE_VALUE_SEPARATOR);
        properties.put(DataExporterCSV.PROP_QUOTE_NEVER, true);
        dataExporterCSV = new DataExporterCSV();

        // then
        assertDoesNotThrow(() -> dataExporterCSV.init(site));
    }

    @Test
    public void testThrowsIfQuoteAndSeparatorAreSameChar() {
        // given
        properties.put(DataExporterCSV.PROP_DELIMITER, ALTERNATIVE_VALUE_SEPARATOR);
        properties.put(DataExporterCSV.PROP_QUOTE_CHAR, ALTERNATIVE_VALUE_SEPARATOR);
        // then
        dataExporterCSV = new DataExporterCSV();
        assertThrows(IllegalArgumentException.class, () -> dataExporterCSV.init(site));
    }

    @Test
    public void testThrowsIfQuoteAndSeparatorAreSameString() {
        // given
        properties.put(DataExporterCSV.PROP_DELIMITER, ALTERNATIVE_QUOTE.repeat(2));
        properties.put(DataExporterCSV.PROP_QUOTE_CHAR, ALTERNATIVE_QUOTE.repeat(2));
        // then
        dataExporterCSV = new DataExporterCSV();
        assertThrows(IllegalArgumentException.class, () -> dataExporterCSV.init(site));
    }


    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testOneRowExport(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        // given
        Object[][] rows = {{"a", "b", "c"}};
        // then
        assertRowsEquals("a,b,c", valueSeparator, quoteSeparator, rows);
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testMultipleRowsExport(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"a", "b", "c"},
            {"d", "e", "f"},
            {"g", "h", "i"}
        };

        assertRowsEquals(
            """
                a,b,c
                d,e,f
                g,h,i""", valueSeparator, quoteSeparator, rows
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testEmptyFields(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"", "", ""}
        };

        assertRowsEquals(
            "\"\",\"\",\"\"", valueSeparator, quoteSeparator, rows
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuotedComma(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"a,b", "c", "d"}
        };

        assertRowsEquals("\"a,b\",c,d", valueSeparator, quoteSeparator, rows);
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuotedQuote(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"a\"b", "c", "d"}
        };

        assertRowsEquals("\"a\"\"b\",c,d", valueSeparator, quoteSeparator, rows);
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuotedNewLine(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"a\nb", "c", "d"}
        };

        assertRowsEquals(
            """
                "a
                b",c,d""", valueSeparator, quoteSeparator, rows
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuoteAndComma(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"a,\"b", "c", "d"}
        };

        assertRowsEquals("\"a,\"\"b\",c,d", valueSeparator, quoteSeparator, rows);
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testOnlyQuote(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"\""}
        };

        assertRowsEquals("\"\"\"\"", valueSeparator, quoteSeparator, rows);
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testOnlyComma(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {","}
        };

        assertRowsEquals("\",\"", valueSeparator, quoteSeparator, rows);
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuoteNever(
        @NotNull String valueSeparator,
        @NotNull String quoteSeparator
    ) throws DBException, IOException {
        // given
        properties.put(DataExporterCSV.PROP_QUOTE_NEVER, true);
        // then
        assertRowsEquals(
            "a,b,c", valueSeparator, quoteSeparator,
            new Object[][]{{"a", "b", "c"}}
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuoteNeverComma(
        @NotNull String valueSeparator,
        @NotNull String quoteSeparator
    ) throws DBException, IOException {
        // given
        properties.put(DataExporterCSV.PROP_QUOTE_NEVER, true);

        // then
        assertRowsEquals(
            "a,b,c",
            valueSeparator,
            quoteSeparator,
            new Object[][]{{"a,b", "c"}}
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuoteNeverQuote(
        @NotNull String valueSeparator,
        @NotNull String quoteSeparator
    ) throws DBException, IOException {
        // given
        properties.put(DataExporterCSV.PROP_QUOTE_NEVER, true);

        // then
        assertRowsEquals(
            "a\"b,c",
            valueSeparator,
            quoteSeparator,
            new Object[][]{{"a\"b", "c"}}
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuoteNeverNewLine(
        @NotNull String valueSeparator,
        @NotNull String quoteSeparator
    ) throws DBException, IOException {
        // given
        properties.put(DataExporterCSV.PROP_QUOTE_NEVER, true);

        // then
        assertRowsEquals(
            """
                a
                b,c""",
            valueSeparator,
            quoteSeparator,
            new Object[][]{{"a\nb", "c"}}
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testQuoteNeverMultipleRows(
        @NotNull String valueSeparator,
        @NotNull String quoteSeparator
    ) throws DBException, IOException {
        // given
        properties.put(DataExporterCSV.PROP_QUOTE_NEVER, true);

        // then
        assertRowsEquals(
            """
                a,b,c
                d,e,f""",
            valueSeparator,
            quoteSeparator,
            new Object[][]{
                {"a", "b", "c"},
                {"d", "e", "f"}
            }
        );
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testLeadingAndTrailingSpaces(@NotNull String valueSeparator, @NotNull String quoteSeparator)
    throws DBException, IOException {
        Object[][] rows = {
            {" a ", "b ", " c"}
        };

        assertRowsEquals(" a ,b , c", valueSeparator, quoteSeparator, rows);
    }

    @ParameterizedTest
    @ArgumentsSource(SeparatorProvides.class)
    public void testUnicode(@NotNull String valueSeparator, @NotNull String quoteSeparator) throws DBException, IOException {
        Object[][] rows = {
            {"Привет", "こんにちは", "😀"}
        };

        assertRowsEquals("Привет,こんにちは,😀", valueSeparator, quoteSeparator, rows);
    }

    private void assertRowsEquals(
        @NotNull String expectedRowsTemplate,
        @NotNull String customSeparator,
        @NotNull String customQuote,
        @NotNull Object[][] rowsTemplate,
        @NotNull DBDValueHandler... handlers
    )
    throws DBException, IOException {
        // given
        properties.put(DataExporterCSV.PROP_DELIMITER, customSeparator);
        properties.put(DataExporterCSV.PROP_QUOTE_CHAR, customQuote);

        if (handlers.length == 0) {
            handlers = new DBDValueHandler[rowsTemplate[0].length];
            Arrays.fill(handlers, JDBCStringValueHandler.INSTANCE);
        }
        for (int i = 0; i < handlers.length; i++) {
            addColumn("Col" + i, "Label" + i, handlers[i]);
        }
        // when
        initExporter();
        dataExporterCSV.exportHeader(dbcSession);
        for (Object[] row : rowsTemplate) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] instanceof String rowTemplate) {
                    row[i] = replaceTemplateQuotesAndSeparator(rowTemplate, customSeparator, customQuote);
                }
            }
            dataExporterCSV.exportRow(dbcSession, resultSetMock, row);
        }
        // then
        // strip header and following empty line
        String resultOnlyRows = stringWriter.toString().replaceFirst(".*" + rowsSeparator, "");
        resultOnlyRows = resultOnlyRows.endsWith(rowsSeparator)
            ? resultOnlyRows.substring(0, resultOnlyRows.length() - 1)
            : resultOnlyRows;
        assertEquals(replaceTemplateQuotesAndSeparator(expectedRowsTemplate, customSeparator, customQuote), resultOnlyRows);
    }

    @NotNull
    private String replaceTemplateQuotesAndSeparator(
        @NotNull String template,
        @NotNull String customSeparator,
        @NotNull String customQuote
    ) {
        // sorted replace to replace correctly
        return customSeparator.length() > customQuote.length()
            ? template.replace(DEFAULT_VALUE_SEPARATOR, customSeparator).replace(DEFAULT_QUOTE, customQuote)
            : template.replace(DEFAULT_QUOTE, customQuote).replace(DEFAULT_VALUE_SEPARATOR, customSeparator);
    }

    @NotNull
    private DBDAttributeBinding addColumn(@NotNull String name, @NotNull String label, @NotNull DBDValueHandler handler) {
        DBDAttributeBinding dbdAttributeBinding = mock(DBDAttributeBinding.class);
        when(dbdAttributeBinding.getName()).thenReturn(name);
        when(dbdAttributeBinding.getLabel()).thenReturn(label);
        when(dbdAttributeBinding.getValueHandler()).thenReturn(handler);
        columns = ArrayUtils.add(DBDAttributeBinding.class, columns, dbdAttributeBinding);
        when(site.getAttributes()).thenReturn(columns);
        return dbdAttributeBinding;
    }


    private void initExporter() throws DBException {
        dataExporterCSV = new DataExporterCSV();
        properties.put(DataExporterCSV.PROP_ROW_DELIMITER, rowsSeparator);
        dataExporterCSV.init(site);
    }

    public static class SeparatorProvides implements ArgumentsProvider {

        @NotNull
        @Override
        public Stream<? extends Arguments> provideArguments(@Nullable ParameterDeclarations parameters, @Nullable ExtensionContext context)
        throws Exception {
            return Stream.of(
                Arguments.of(DEFAULT_VALUE_SEPARATOR, DEFAULT_QUOTE),
                Arguments.of(ALTERNATIVE_VALUE_SEPARATOR, ALTERNATIVE_QUOTE),
                // multichars separators
                Arguments.of(DEFAULT_VALUE_SEPARATOR, DEFAULT_QUOTE + ALTERNATIVE_QUOTE),
                Arguments.of(DEFAULT_VALUE_SEPARATOR + ALTERNATIVE_VALUE_SEPARATOR, DEFAULT_QUOTE),
                Arguments.of(DEFAULT_VALUE_SEPARATOR + ALTERNATIVE_VALUE_SEPARATOR, DEFAULT_QUOTE + ALTERNATIVE_QUOTE),
                // starts same char
                Arguments.of(DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR + DEFAULT_QUOTE),
                Arguments.of(DEFAULT_QUOTE + DEFAULT_VALUE_SEPARATOR, DEFAULT_QUOTE)
            );
        }
    }
}
