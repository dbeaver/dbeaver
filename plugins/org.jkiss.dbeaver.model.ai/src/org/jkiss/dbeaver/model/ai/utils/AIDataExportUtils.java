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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterCSV;

import java.util.HashMap;
import java.util.Map;

public final class AIDataExportUtils {
    private AIDataExportUtils() {
    }

    @NotNull
    public static DataTransferProcessorDescriptor getProcessor(@NotNull String format) throws DBException {
        DataTransferRegistry registry = DataTransferRegistry.getInstance();
        return getProcessor(format, registry.getNodeByType(StreamTransferConsumer.class));
    }

    @NotNull
    static DataTransferProcessorDescriptor getProcessor(
        @NotNull String format,
        @Nullable DataTransferNodeDescriptor consumer
    ) throws DBException {
        if (consumer != null) {
            // Match formats in the same order as SQLPragmaExport's searchSimilar mode.
            for (DataTransferProcessorDescriptor processor : consumer.getProcessors()) {
                if (format.equalsIgnoreCase(processor.getAIFormatId())) {
                    return validateProcessor(processor);
                }
            }
            DataTransferProcessorDescriptor exactProcessor = consumer.getProcessor("stream." + format);
            if (exactProcessor != null) {
                return validateProcessor(exactProcessor);
            }
            for (DataTransferProcessorDescriptor processor : consumer.getProcessors()) {
                if (format.equalsIgnoreCase(processor.getShortId())) {
                    return validateProcessor(processor);
                }
            }
            for (DataTransferProcessorDescriptor processor : consumer.getProcessors()) {
                if (format.equalsIgnoreCase(processor.getProcessorFileExtension())) {
                    return validateProcessor(processor);
                }
            }
        }
        throw new DBException("Can't find data exporter for format '" + format + "'");
    }

    @NotNull
    private static DataTransferProcessorDescriptor validateProcessor(
        @NotNull DataTransferProcessorDescriptor processor
    ) throws DBException {
        if (processor.isBinaryFormat()) {
            throw new DBException("Format '" + processor.getAIFormatId() + "' is binary and cannot be returned as text");
        }
        return processor;
    }

    @NotNull
    public static IStreamDataExporter createExporter(@NotNull DataTransferProcessorDescriptor processor) throws DBException {
        if (processor.getInstance() instanceof IStreamDataExporter exporter) {
            return exporter;
        }
        throw new DBException("Format '" + processor.getAIFormatId() + "' does not support data export");
    }

    @NotNull
    public static Map<String, Object> getProperties(@NotNull DataTransferProcessorDescriptor processor) {
        Map<String, Object> properties = new HashMap<>();
        for (DBPPropertyDescriptor property : processor.getProperties()) {
            properties.put(property.getId(), property.getDefaultValue());
        }
        properties.put(DataExporterCSV.PROP_ROW_DELIMITER, "\n");
        return properties;
    }
}
