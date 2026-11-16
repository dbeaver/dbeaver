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
package org.jkiss.dbeaver.model.ai.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.utils.AIDataExportUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterSQL;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

public class SampleDataProvider {
    private static final Log log = Log.getLog(SampleDataProvider.class);

    private static final Set<JDBCType> EXCLUSIONS = Set.of(
        JDBCType.ARRAY,
        JDBCType.BINARY,
        JDBCType.VARBINARY,
        JDBCType.LONGVARBINARY,
        JDBCType.BLOB,
        JDBCType.CLOB,
        JDBCType.NCLOB,
        JDBCType.SQLXML
    );

    private record SampleDataKey(@NotNull DBSDataContainer container, int rowCount, @NotNull String format) {
    }

    private final Map<SampleDataKey, String> tablesData = new ConcurrentHashMap<>();
    /**
     * Cache: table, row count and format → last computed checksum
     */
    private final Map<SampleDataKey, Long> tablesChecksum = new ConcurrentHashMap<>();

    /**
     * Returns sample data for the given data container.
     */
    @NotNull
    public String getSampleData(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSDataContainer dataContainer,
        int rowCount
    ) throws DBException {
        return getSampleData(monitor, dataContainer, rowCount, AIDataExportUtils.getProcessor("sql"));
    }

    @NotNull
    public String getSampleData(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSDataContainer dataContainer,
        int rowCount,
        @NotNull DataTransferProcessorDescriptor processor
    ) throws DBException {
        SampleDataKey key = new SampleDataKey(dataContainer, rowCount, processor.getFullId());

        // Compute checksum only if the container is an entity
        long checksum = 0;
        if (dataContainer instanceof DBSEntity entity) {
            checksum = computeChecksum(entity, monitor);
        }

        Long previous = tablesChecksum.get(key);
        // If no previous checksum or it changed, re-fetch
        if (previous == null || previous != checksum) {
            String sampleData = fetchSampleData(monitor, dataContainer, rowCount, processor);
            tablesData.put(key, sampleData);
            tablesChecksum.put(key, checksum);
            return sampleData;
        }

        // Otherwise return cached
        return tablesData.get(key);
    }

    @NotNull
    private static String fetchSampleData(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSDataContainer dataContainer,
        int rowCount,
        @NotNull DataTransferProcessorDescriptor processor
    ) throws DBException {
        try (SampleDataCollector dataReceiver = new SampleDataCollector(dataContainer, processor)) {
            DBExecUtils.tryExecuteRecover(monitor, dataContainer.getDataSource(), param -> {
                try (DBCSession session = DBUtils.openUtilSession(monitor, dataContainer, "Fetch sample data")) {
                    dataContainer.readData(
                        null,
                        session,
                        dataReceiver,
                        null,
                        0,
                        rowCount,
                        DBSDataContainer.FLAG_NONE,
                        100
                    );
                }
            });

            return dataReceiver.getSampleData();
        }
    }

    private static class SampleDataCollector implements DBDDataReceiver {
        private final IStreamDataExporter dataExporter;
        private final Map<String, Object> properties;
        private final StringWriter stringWriter = new StringWriter();
        private final AtomicInteger sampleCount = new AtomicInteger(0);

        private final DBSDataContainer dataContainer;

        public SampleDataCollector(
            @NotNull DBSDataContainer dataContainer,
            @NotNull DataTransferProcessorDescriptor processor
        ) throws DBException {
            this.dataContainer = dataContainer;
            this.dataExporter = AIDataExportUtils.createExporter(processor);
            this.properties = AIDataExportUtils.getProperties(processor);
            if (dataExporter instanceof DataExporterSQL) {
                properties.put(DataExporterSQL.PROP_IDENTIFIER_CASE, DataExporterSQL.CASE_AS_IS);
                properties.put(DataExporterSQL.PROP_ROWS_IN_STATEMENT, AIConstants.DEFAULT_SAMPLE_DATA_ROW_COUNT);
                properties.put(DataExporterSQL.PROP_DATA_NATIVE_FORMAT, true);
            }
        }

        @NotNull
        public String getSampleData() {
            if (sampleCount.get() <= 0) {
                return "";
            }

            return stringWriter.toString();
        }

        @Override
        public void fetchStart(
            @NotNull DBCSession session,
            @NotNull DBCResultSet resultSet,
            long offset,
            long maxRows
        ) throws DBCException {
            List<? extends DBCAttributeMetaData> attributes = resultSet.getMeta().getAttributes();
            DBDAttributeBinding[] attributeBindings = attributes.stream()
                .map(it -> new DBDAttributeBindingMeta(dataContainer, session, it))
                .toArray(DBDAttributeBinding[]::new);

            try {
                dataExporter.init(new SampleDataExporterSite(
                    this.dataContainer,
                    attributeBindings,
                    new PrintWriter(stringWriter, true),
                    properties
                ));
                dataExporter.exportHeader(session);
            } catch (Exception e) {
                log.error("Error initializing data exporter", e);
                throw new DBCException("Error initializing data exporter", e);
            }
        }

        @Override
        public void fetchRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) throws DBCException {
            try {
                List<? extends DBCAttributeMetaData> attributes = resultSet.getMeta().getAttributes();
                Object[] row = new Object[attributes.size()];

                boolean shouldBeSkipped = true; // if all attributes are excluded
                for (int i = 0; i < attributes.size(); i++) {
                    DBCAttributeMetaData attr = attributes.get(i);
                    JDBCType type;
                    try {
                        type = JDBCType.valueOf(attr.getTypeID());
                    } catch (Throwable e) {
                        type = null;
                    }
                    if (type != null && EXCLUSIONS.contains(type)) {
                        row[i] = null;
                    } else {
                        row[i] = resultSet.getAttributeValue(attr.getName());
                        shouldBeSkipped = false;
                    }
                }

                if (shouldBeSkipped) {
                    return;
                }

                sampleCount.incrementAndGet();
                dataExporter.exportRow(session, resultSet, row);
            } catch (Exception e) {
                log.error("Error exporting row", e);
                throw new DBCException("Error exporting row", e);
            }
        }

        @Override
        public void fetchEnd(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) throws DBCException {
            // Finalize the sample data collection
            try {
                dataExporter.exportFooter(session.getProgressMonitor());
            } catch (DBException | IOException e) {
                throw new DBCException("Error finalizing data export", e);
            }
        }

        @Override
        public void close() throws DBException {
            // Clean up resources if needed
            try {
                dataExporter.dispose();
            } catch (IOException e) {
                throw new DBException("Error disposing data exporter", e);
            }
        }
    }

    private static long computeChecksum(@NotNull DBSEntity entity, @NotNull DBRProgressMonitor monitor) throws DBException {
        List<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
        if (attributes == null || attributes.isEmpty()) {
            return 0;
        }

        CRC32 crc32 = new CRC32();
        for (DBSEntityAttribute attribute : attributes) {
            if (DBUtils.isHiddenObject(attribute)) {
                continue;
            }
            crc32.update(attribute.getName().getBytes(StandardCharsets.UTF_8));
            crc32.update(attribute.getTypeID());
        }

        return crc32.getValue();
    }

    private static class SampleDataExporterSite implements IStreamDataExporterSite {
        private final DBPNamedObject source;
        private final DBDAttributeBinding[] attributes;
        private final PrintWriter writer;
        private final Map<String, Object> properties;

        private SampleDataExporterSite(
            @NotNull DBPNamedObject source,
            @NotNull DBDAttributeBinding[] attributes,
            @NotNull PrintWriter writer,
            @NotNull Map<String, Object> properties
        ) {
            this.source = source;
            this.attributes = attributes;
            this.writer = writer;
            this.properties = properties;
        }

        @Override
        @NotNull
        public DBPNamedObject getSource() {
            return source;
        }

        @Override
        @NotNull
        public DBDDisplayFormat getExportFormat() {
            return DBDDisplayFormat.NATIVE;
        }

        @Override
        @NotNull
        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        @NotNull
        public DBDAttributeBinding[] getAttributes() {
            return attributes;
        }

        @Override
        @Nullable
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        @Nullable
        public Path getOutputFile() {
            return null;
        }

        @Override
        @NotNull
        public PrintWriter getWriter() {
            return writer;
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void writeBinaryData(@NotNull DBDContentStorage cs) throws IOException {
            // No-op
        }

        @NotNull
        @Override
        public String getOutputEncoding() {
            return StandardCharsets.UTF_8.displayName();
        }
    }
}
