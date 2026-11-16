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
package org.jkiss.dbeaver.model.ai.utils;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterCSV;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterJSON;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

class AIDataExportUtilsTest {
    @Test
    void resolvesFormatIdsShortIdsAndExtensionsInExportOrder() throws DBException {
        DataTransferNodeDescriptor consumer = Mockito.mock(DataTransferNodeDescriptor.class);
        DataTransferProcessorDescriptor processor = Mockito.mock(DataTransferProcessorDescriptor.class);
        Mockito.when(consumer.getProcessors()).thenReturn(new DataTransferProcessorDescriptor[] {processor});
        Mockito.when(consumer.getProcessor("stream.custom")).thenReturn(processor);
        Mockito.when(processor.getAIFormatId()).thenReturn("json");
        Mockito.when(processor.getShortId()).thenReturn("js");
        Mockito.when(processor.getProcessorFileExtension()).thenReturn("jsonl");

        for (String format : List.of("JSON", "custom", "JS", "JSONL")) {
            Assertions.assertSame(processor, AIDataExportUtils.getProcessor(format, consumer));
        }
        Assertions.assertThrows(DBException.class, () -> AIDataExportUtils.getProcessor("unknown", consumer));
        Assertions.assertThrows(DBException.class, () -> AIDataExportUtils.getProcessor("csv", null));

        DataTransferProcessorDescriptor exact = Mockito.mock(DataTransferProcessorDescriptor.class);
        Mockito.when(consumer.getProcessor("stream.json")).thenReturn(exact);
        Assertions.assertSame(processor, AIDataExportUtils.getProcessor("json", consumer));

        Mockito.when(processor.isBinaryFormat()).thenReturn(true);
        Assertions.assertThrows(DBException.class, () -> AIDataExportUtils.getProcessor("json", consumer));
    }

    @Test
    void exportsCsvWithDefaultsAndUnicode() throws Exception {
        DataTransferProcessorDescriptor processor = Mockito.mock(DataTransferProcessorDescriptor.class);
        Mockito.when(processor.getInstance()).thenAnswer(invocation -> new DataExporterCSV());
        DBPPropertyDescriptor[] properties = {
            property(DataExporterCSV.PROP_DELIMITER, ";"),
            property(DataExporterCSV.PROP_QUOTE_CHAR, "\""),
            property("headerCase", "as is")
        };
        Mockito.when(processor.getProperties()).thenReturn(properties);

        String output = exportRow(processor, "Montréal;東京");
        Assertions.assertTrue(output.startsWith("\"value\"\n"), output);
        Assertions.assertTrue(output.contains("\"Montréal;東京\""), output);
        Assertions.assertFalse(output.contains("INSERT"), output);
    }

    @Test
    void exportsJsonInsteadOfSql() throws Exception {
        DataTransferProcessorDescriptor processor = Mockito.mock(DataTransferProcessorDescriptor.class);
        Mockito.when(processor.getInstance()).thenAnswer(invocation -> new DataExporterJSON());
        DBPPropertyDescriptor[] properties = {
            property(DataExporterJSON.PROP_PRINT_TABLE_NAME, false)
        };
        Mockito.when(processor.getProperties()).thenReturn(properties);

        String output = exportRow(processor, "Montréal\n東京");
        Assertions.assertTrue(output.startsWith("[\n"), output);
        Assertions.assertTrue(output.contains("\"value\" : \"Montréal\\n東京\""), output);
        Assertions.assertTrue(output.endsWith("]\n"), output);
    }

    @NotNull
    private static DBPPropertyDescriptor property(@NotNull String name, @NotNull Object value) {
        DBPPropertyDescriptor property = Mockito.mock(DBPPropertyDescriptor.class);
        Mockito.when(property.getId()).thenReturn(name);
        Mockito.when(property.getDefaultValue()).thenReturn(value);
        return property;
    }

    @NotNull
    private static String exportRow(
        @NotNull DataTransferProcessorDescriptor processor,
        @NotNull String value
    ) throws Exception {
        Map<String, Object> properties = AIDataExportUtils.getProperties(processor);
        DBDAttributeBinding column = Mockito.mock(DBDAttributeBinding.class);
        Mockito.when(column.getName()).thenReturn("value");
        Mockito.when(column.getDataKind()).thenReturn(DBPDataKind.STRING);
        Mockito.when(column.getValueHandler()).thenReturn(DefaultValueHandler.INSTANCE);
        IStreamDataExporterSite site = Mockito.mock(IStreamDataExporterSite.class);
        DBPNamedObject source = () -> "sample";
        StringWriter output = new StringWriter();
        Mockito.when(site.getSource()).thenReturn(source);
        Mockito.when(site.getAttributes()).thenReturn(new DBDAttributeBinding[] {column});
        Mockito.when(site.getProperties()).thenReturn(properties);
        Mockito.when(site.getExportFormat()).thenReturn(DBDDisplayFormat.NATIVE);
        Mockito.when(site.getWriter()).thenReturn(new PrintWriter(output));

        IStreamDataExporter exporter = AIDataExportUtils.createExporter(processor);
        try {
            exporter.init(site);
            exporter.exportHeader(null);
            exporter.exportRow(null, null, new Object[] {value});
            exporter.exportFooter(new VoidProgressMonitor());
            site.getWriter().flush();
            return output.toString();
        } finally {
            exporter.dispose();
        }
    }
}
