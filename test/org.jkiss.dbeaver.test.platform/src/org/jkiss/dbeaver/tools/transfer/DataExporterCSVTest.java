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
import org.mockito.Mock;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataExporterCSVTest extends DBeaverUnitTest {
    // must be used in expectedTemplate as separator
    private static final String DEFAULT_VALUE_SEPARATOR = ",";
    // must be used in expectedTemplate as quote
    private static final String DEFAULT_QUOTE = "\"";

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
    public void testOneRowExport() throws DBException, IOException {
        // given
        Object[][] rows = {{"a", "b", "c"}};
        // then
        assertRowsEquals("a,b,c", ",", "\"", rows);
    }

    @Test
    public void testMultipleRowsExport() throws DBException, IOException {
        Object[][] rows = {
            {"a", "b", "c"},
            {"d", "e", "f"},
            {"g", "h", "i"}
        };

        assertRowsEquals(
            """
                a,b,c
                d,e,f
                g,h,i""", ",", "\"", rows
        );
    }

    @Test
    public void testEmptyFields() throws DBException, IOException {
        Object[][] rows = {
            {"", "", ""}
        };

        assertRowsEquals(
            "\"\",\"\",\"\"", ",", "\"", rows
        );
    }

    @Test
    public void testQuotedComma() throws DBException, IOException {
        Object[][] rows = {
            {"a,b", "c", "d"}
        };

        assertRowsEquals("\"a,b\",c,d", ",", "\"", rows);
    }

    @Test
    public void testQuotedQuote() throws DBException, IOException {
        Object[][] rows = {
            {"a\"b", "c", "d"}
        };

        assertRowsEquals("\"a\"\"b\",c,d", ",", "\"", rows);
    }

    @Test
    public void testQuotedNewLine() throws DBException, IOException {
        Object[][] rows = {
            {"a\nb", "c", "d"}
        };

        assertRowsEquals(
            """
                "a
                b",c,d""", ",", "\"", rows
        );
    }

    @Test
    public void testQuoteAndComma() throws DBException, IOException {
        Object[][] rows = {
            {"a,\"b", "c", "d"}
        };

        assertRowsEquals("\"a,\"\"b\",c,d", ",", "\"", rows);
    }

    @Test
    public void testOnlyQuote() throws DBException, IOException {
        Object[][] rows = {
            {"\""}
        };

        assertRowsEquals("\"\"\"\"", ",", "\"", rows);
    }

    @Test
    public void testOnlyComma() throws DBException, IOException {
        Object[][] rows = {
            {","}
        };

        assertRowsEquals("\",\"", ",", "\"", rows);
    }

    @Test
    public void testLeadingAndTrailingSpaces() throws DBException, IOException {
        Object[][] rows = {
            {" a ", "b ", " c"}
        };

        assertRowsEquals(" a ,b , c", ",", "\"", rows);
    }

    @Test
    public void testUnicode() throws DBException, IOException {
        Object[][] rows = {
            {"Привет", "こんにちは", "😀"}
        };

        assertRowsEquals("Привет,こんにちは,😀", ",", "\"", rows);
    }


    private void assertRowsEquals(
        @NotNull String expectedRowsTemplate,
        @NotNull String customSeparator,
        @NotNull String customQuote,
        @NotNull Object[][] rows,
        @NotNull DBDValueHandler... handlers
    )
    throws DBException, IOException {
        // given
        properties.put(DataExporterCSV.PROP_DELIMITER, customSeparator);
        properties.put(DataExporterCSV.PROP_QUOTE_CHAR, customQuote);

        if (handlers.length == 0) {
            handlers = new DBDValueHandler[rows[0].length];
            Arrays.fill(handlers, JDBCStringValueHandler.INSTANCE);
        }
        for (int i = 0; i < handlers.length; i++) {
            addColumn("Col" + i, "Label" + i, handlers[i]);
        }
        // when
        initExporter();
        dataExporterCSV.exportHeader(dbcSession);
        for (Object[] row : rows) {
            dataExporterCSV.exportRow(dbcSession, resultSetMock, row);
        }
        // then

        // strip header and following empty line
        String resultOnlyRows = stringWriter.toString().replaceFirst(".*" + rowsSeparator, "");
        resultOnlyRows = resultOnlyRows.endsWith(rowsSeparator)
            ? resultOnlyRows.substring(0, resultOnlyRows.length() - 1)
            : resultOnlyRows;
        assertEquals(
            expectedRowsTemplate
                .replace(DEFAULT_VALUE_SEPARATOR, customSeparator)
                .replace(DEFAULT_QUOTE, customQuote)
            ,
            resultOnlyRows
        );
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
}
