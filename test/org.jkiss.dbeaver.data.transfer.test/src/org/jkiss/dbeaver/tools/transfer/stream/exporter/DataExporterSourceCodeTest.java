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
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataExporterSourceCodeTest extends DBeaverUnitTest {

    private static final String TABLE_NAME = "test_table";
    private static final String COLUMN_NAME = "name";

    @Test
    public void escapePhpStringInSingleQuoteModeEscapesQuoteAndBackslash() {
        // PHP single-quoted strings only recognize \\ and \' as escapes; every
        // other character (including \n, \r, \t) must be emitted literally.
        assertEquals("O\\'Brien", DataExporterSourceCode.escapePhpString("O'Brien", '\''));
        assertEquals("C:\\\\path", DataExporterSourceCode.escapePhpString("C:\\path", '\''));
        assertEquals("line1\nline2",
            DataExporterSourceCode.escapePhpString("line1\nline2", '\''));
    }

    @Test
    public void escapePhpStringInDoubleQuoteModeUsesJsonEscapes() {
        assertEquals("hello \\\"world\\\"",
            DataExporterSourceCode.escapePhpString("hello \"world\"", '"'));
        assertEquals("line1\\nline2",
            DataExporterSourceCode.escapePhpString("line1\nline2", '"'));
        // Single quote needs no escape in a double-quoted string.
        assertEquals("O'Brien",
            DataExporterSourceCode.escapePhpString("O'Brien", '"'));
    }

    @Test
    public void escapePhpStringReturnsNullForNullInput() {
        assertEquals(null, DataExporterSourceCode.escapePhpString(null, '\''));
        assertEquals(null, DataExporterSourceCode.escapePhpString(null, '"'));
    }

    @Test
    public void initDoesNotThrowWhenQuoteCharPropertyIsMissing() throws DBException {
        // Regression guard: DataExporterSourceCode#init used to call
        // `.equals(...)` on the `quoteChar` property without checking for null,
        // so an export invoked with a property map that omits the key (common
        // when the exporter is driven programmatically or from a saved task
        // spec whose properties have been trimmed to the non-default subset)
        // threw NullPointerException during init.
        Map<String, Object> properties = new HashMap<>();
        // deliberately omit PROP_QUOTE_CHAR
        properties.put("language", "PHP 5.4+");
        properties.put("rowDelimiter", "default");

        DataExporterSourceCode exporter = new DataExporterSourceCode();
        exporter.init(newMockSite(properties));
        // If we reach here the init did not throw NPE.
        assertNotNull(exporter);
    }

    @Test
    public void exportRowWithSingleQuoteValueInSingleQuoteModeProducesValidPhp() throws DBException, IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("language", "PHP 5.4+");
        properties.put("rowDelimiter", "default");
        properties.put("quoteChar", "'");

        StringWriter writer = new StringWriter();
        DataExporterSourceCode exporter = new DataExporterSourceCode();
        exporter.init(newMockSite(properties, writer));
        exporter.exportHeader(mock(DBCSession.class));
        exporter.exportRow(mock(DBCSession.class), mock(DBCResultSet.class),
            new Object[]{"O'Brien"});
        exporter.exportFooter(null);

        // The key and value both use single quotes with the apostrophe escaped
        // as \' so the emitted PHP is syntactically valid (no premature string
        // termination).
        String output = writer.toString();
        assertEquals(
            "the row content must escape ' as \\' under single-quote mode",
            true,
            output.contains("'name' => 'O\\'Brien'"));
    }

    @Test
    public void exportRowWithDoubleQuoteValueInDoubleQuoteModeUsesJsonEscapes() throws DBException, IOException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("language", "PHP 5.4+");
        properties.put("rowDelimiter", "default");
        // Omit quoteChar — double quote is the default per plugin.xml.

        StringWriter writer = new StringWriter();
        DataExporterSourceCode exporter = new DataExporterSourceCode();
        exporter.init(newMockSite(properties, writer));
        exporter.exportHeader(mock(DBCSession.class));
        exporter.exportRow(mock(DBCSession.class), mock(DBCResultSet.class),
            new Object[]{"hello \"world\""});
        exporter.exportFooter(null);

        String output = writer.toString();
        assertEquals(
            "the row content must escape \" as \\\" under double-quote mode",
            true,
            output.contains("\"name\" => \"hello \\\"world\\\"\""));
    }

    @NotNull
    private IStreamDataExporterSite newMockSite(@NotNull Map<String, Object> properties) {
        return newMockSite(properties, new StringWriter());
    }

    @NotNull
    private IStreamDataExporterSite newMockSite(
        @NotNull Map<String, Object> properties,
        @NotNull StringWriter writer
    ) {
        DBPNamedObject mockSource = mock(DBPNamedObject.class);
        when(mockSource.getName()).thenReturn(TABLE_NAME);

        DBDAttributeBinding mockBinding = mock(DBDAttributeBinding.class);
        when(mockBinding.getLabel()).thenReturn(COLUMN_NAME);
        when(mockBinding.getName()).thenReturn(COLUMN_NAME);
        when(mockBinding.getValueHandler()).thenReturn(newPassThroughValueHandler());

        IStreamDataExporterSite mockSite = mock(IStreamDataExporterSite.class);
        when(mockSite.getProperties()).thenReturn(properties);
        when(mockSite.getOutputEncoding()).thenReturn("UTF-8");
        when(mockSite.getWriter()).thenReturn(new PrintWriter(writer));
        when(mockSite.getAttributes()).thenReturn(new DBDAttributeBinding[]{mockBinding});
        when(mockSite.getSource()).thenReturn(mockSource);
        return mockSite;
    }

    @NotNull
    private DBDValueHandler newPassThroughValueHandler() {
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
            ) {
                return null;
            }

            @Override
            public void bindValueObject(
                @NotNull DBCSession session,
                @NotNull DBCStatement statement,
                @NotNull DBSTypedObject type,
                int index,
                @Nullable Object value
            ) {
            }

            @Nullable
            @Override
            public Object getValueFromObject(
                @NotNull DBCSession session,
                @NotNull DBSTypedObject type,
                @Nullable Object object,
                boolean copy,
                boolean validateValue
            ) {
                return object;
            }

            @Nullable
            @Override
            public Object createNewValueObject(@NotNull DBCSession session, @NotNull DBSTypedObject type) {
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
            public String getValueDisplayString(
                @NotNull DBSTypedObject column,
                @Nullable Object value,
                @NotNull DBDDisplayFormat format
            ) {
                return value == null ? "" : value.toString();
            }
        };
    }
}
