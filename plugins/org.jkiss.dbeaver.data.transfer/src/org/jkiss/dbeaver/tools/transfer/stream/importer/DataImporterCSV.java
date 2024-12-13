/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream.importer;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBFetchProgress;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.local.LocalStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferUtils;
import org.jkiss.dbeaver.tools.transfer.stream.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;
import org.jkiss.utils.csv.CSVParser;
import org.jkiss.utils.csv.CSVReader;
import org.jkiss.utils.io.BOMInputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV importer
 */
public class DataImporterCSV extends StreamImporterAbstract {
    private static final Log log = Log.getLog(DataImporterCSV.class);

    private static final String PROP_ENCODING = "encoding";
    private static final String PROP_HEADER = "header";
    private static final String PROP_DELIMITER = "delimiter";
    private static final String PROP_QUOTE_CHAR = "quoteChar";
    private static final String PROP_NULL_STRING = "nullString";
    private static final String PROP_EMPTY_STRING_NULL = "emptyStringNull";
    private static final String PROP_ESCAPE_CHAR = "escapeChar";
    private static final String PROP_TRIM_WHITESPACES = "trimWhitespaces";
    public static final int READ_BUFFER_SIZE = 255 * 1024;

    public enum HeaderPosition {
        none,
        top,
    }

    public DataImporterCSV() {
    }

    @NotNull
    @Override
    public List<StreamDataImporterColumnInfo> readColumnsInfo(StreamEntityMapping entityMapping, @NotNull InputStream inputStream) throws DBException {
        List<StreamDataImporterColumnInfo> columnsInfo = new ArrayList<>();
        Map<String, Object> processorProperties = getSite().getProcessorProperties();
        HeaderPosition headerPosition = getHeaderPosition(processorProperties);

        final String encoding = CommonUtils.toString(processorProperties.get(PROP_ENCODING), GeneralUtils.UTF8_ENCODING);
        final int columnSamplesCount = Math.max(CommonUtils.toInt(processorProperties.get(PROP_COLUMN_TYPE_SAMPLES), 100), 0);
        final int columnMinimalLength = Math.max(CommonUtils.toInt(processorProperties.get(PROP_COLUMN_TYPE_LENGTH), 1), 1);
        final boolean columnIsByteLength = CommonUtils.getBoolean(processorProperties.get(PROP_COLUMN_IS_BYTE_LENGTH), false);

        try (Reader reader = openStreamReader(inputStream, processorProperties, true)) {
            try (CSVReader csvReader = openCSVReader(reader, processorProperties)) {
                String[] header = getNextLine(csvReader);
                if (header == null) {
                    return columnsInfo;
                }

                for (int i = 0; i < header.length; i++) {
                    String column = null;
                    if (headerPosition == HeaderPosition.top) {
                        column = DBUtils.getUnQuotedIdentifier(entityMapping.getDataSource(), header[i]);
                    }
                    if (CommonUtils.isEmptyTrimmed(column)) {
                        column = "Column" + (i + 1);
                    }
                    StreamDataImporterColumnInfo columnInfo = new StreamDataImporterColumnInfo(entityMapping, i, column, null, columnMinimalLength, DBPDataKind.UNKNOWN);
                    columnInfo.setMappingMetadataPresent(headerPosition != HeaderPosition.none);
                    columnsInfo.add(columnInfo);
                }

                for (int sample = 0; sample < columnSamplesCount; sample++) {
                    String[] line;

                    if (sample == 0 && headerPosition == HeaderPosition.none) {
                        // Include first line (header that does not exist) for sampling
                        line = header;
                    } else {
                        line = getNextLine(csvReader);
                        if (line == null) {
                            break;
                        }
                    }

                    for (int i = 0; i < Math.min(line.length, header.length); i++) {
                        Pair<DBPDataKind, String> dataType = DatabaseTransferUtils.getDataType(line[i]);
                        StreamDataImporterColumnInfo columnInfo = columnsInfo.get(i);

                        switch (dataType.getFirst()) {
                            case STRING:
                                columnInfo.updateMaxLength(
                                    entityMapping.getDataSource(),
                                    columnIsByteLength ? line[i].getBytes(encoding).length : line[i].length());
                                /* fall-through */
                            case NUMERIC:
                            case BOOLEAN:
                                columnInfo.updateType(dataType.getFirst(), dataType.getSecond());
                                break;
                            default:
                                break;
                        }
                    }
                }

                for (StreamDataImporterColumnInfo columnInfo : columnsInfo) {
                    if (columnInfo.getDataKind() == DBPDataKind.UNKNOWN) {
                        log.warn("Cannot guess data type for column '" + columnInfo.getName() + "', defaulting to VARCHAR");
                        columnInfo.updateType(DBPDataKind.STRING, "VARCHAR");
                    }
                }
            }
        } catch (IOException e) {
            throw new DBException("IO error reading CSV", e);
        }

        return columnsInfo;
    }

    private int roundToNextPowerOf2(int value) {
        int power = 1;
        while(power < value)
            power*=2;
        return power;
    }

    private HeaderPosition getHeaderPosition(Map<String, Object> processorProperties) {
        return CommonUtils.valueOf(HeaderPosition.class, CommonUtils.toString(processorProperties.get(PROP_HEADER)), HeaderPosition.top);
    }

    private CSVReader openCSVReader(Reader reader, Map<String, Object> processorProperties) {
        String delimiter = StreamTransferUtils.getDelimiterString(processorProperties, PROP_DELIMITER);
        String quoteChar = CommonUtils.toString(processorProperties.get(PROP_QUOTE_CHAR));
        if (CommonUtils.isEmpty(quoteChar)) {
            quoteChar = String.valueOf(CSVParser.NULL_CHARACTER);
        }
        String escapeChar = CommonUtils.toString(processorProperties.get(PROP_ESCAPE_CHAR));
        if (CommonUtils.isEmpty(escapeChar)) {
            escapeChar = String.valueOf(CSVParser.NULL_CHARACTER);
        }
        return new CSVReader(reader, delimiter.charAt(0), quoteChar.charAt(0), escapeChar.charAt(0));
    }

    private Reader openStreamReader(InputStream inputStream, Map<String, Object> processorProperties, boolean useBufferedStream) throws UnsupportedEncodingException {
        final String encoding = CommonUtils.toString(processorProperties.get(PROP_ENCODING), GeneralUtils.UTF8_ENCODING);
        final Charset charset = Charset.forName(encoding);
        if (useBufferedStream) {
            inputStream = new BufferedInputStream(inputStream, READ_BUFFER_SIZE);
        }
        try {
            inputStream = new BOMInputStream(inputStream, charset);
        } catch (IllegalArgumentException ignored) {
            // This charset does not have BOM, suppress and continue
        }
        return new InputStreamReader(inputStream, charset);
    }

    private String[] getNextLine(CSVReader csvReader) throws IOException {
        while (true) {
            String[] line = csvReader.readNext();
            if (line == null) {
                return null;
            }
            if (line.length == 0) {
                continue;
            }
            return line;
        }
    }

    @Override
    public void runImport(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource streamDataSource, @NotNull InputStream inputStream, @NotNull IDataTransferConsumer consumer) throws DBException {
        IStreamDataImporterSite site = getSite();
        StreamEntityMapping entityMapping = site.getSourceObject();
        Map<String, Object> properties = site.getProcessorProperties();
        HeaderPosition headerPosition = getHeaderPosition(properties);
        boolean emptyStringNull = CommonUtils.getBoolean(properties.get(PROP_EMPTY_STRING_NULL), false);
        boolean trimWhitespaces = CommonUtils.getBoolean(properties.get(PROP_TRIM_WHITESPACES), false);
        String nullValueMark = CommonUtils.toString(properties.get(PROP_NULL_STRING));

        DBCExecutionContext context = streamDataSource.getDefaultInstance().getDefaultContext(monitor, false);
        try (DBCSession producerSession = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Transfer stream data")) {
            LocalStatement localStatement = new LocalStatement(producerSession, "SELECT * FROM Stream");
            StreamTransferResultSet resultSet = new StreamTransferResultSet(producerSession, localStatement, entityMapping);

            consumer.fetchStart(producerSession, resultSet, -1, -1);

            applyTransformHints(resultSet, consumer, properties, PROP_TIMESTAMP_FORMAT, PROP_TIMESTAMP_ZONE);

            try (Reader reader = openStreamReader(inputStream, properties, true)) {
                try (CSVReader csvReader = openCSVReader(reader, properties)) {

                    int maxRows = site.getSettings().getMaxRows();
                    int targetAttrSize = entityMapping.getStreamColumns().size();
                    boolean headerRead = false;
                    for (long lineNum = 0; ; ) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String[] line = csvReader.readNext();
                        if (line == null) {
                            if (csvReader.getParser().isPending()) {
                                throw new IOException("Un-terminated quote sequence was detected");
                            }
                            break;
                        }
                        if (line.length == 0) {
                            continue;
                        }
                        if (headerPosition != HeaderPosition.none && !headerRead) {
                            // First line is a header
                            headerRead = true;
                            continue;
                        }
                        if (maxRows > 0 && lineNum >= maxRows) {
                            break;
                        }

                        if (line.length < targetAttrSize) {
                            // Stream row may be shorter than header
                            String[] newLine = new String[targetAttrSize];
                            System.arraycopy(line, 0, newLine, 0, line.length);
                            for (int i = line.length; i < targetAttrSize; i++) {
                                newLine[i] = null;
                            }
                            line = newLine;
                        }
                        if (trimWhitespaces) {
                            for (int i = 0; i < line.length; i++) {
                                line[i] = line[i].trim();
                            }
                        }
                        if (emptyStringNull) {
                            for (int i = 0; i < line.length; i++) {
                                if ("".equals(line[i])) {
                                    line[i] = null;
                                }
                            }
                        }
                        if (!CommonUtils.isEmpty(nullValueMark)) {
                            for (int i = 0; i < line.length; i++) {
                                if (nullValueMark.equals(line[i])) {
                                    line[i] = null;
                                }
                            }
                        }

                        resultSet.setStreamRow(line);
                        consumer.fetchRow(producerSession, resultSet);
                        lineNum++;

                        if (DBFetchProgress.monitorFetchProgress(lineNum)) {
                            monitor.subTask(Long.toUnsignedString(lineNum) + " rows processed");
                        }
                    }
                }
            } catch (IOException e) {
                throw new DBException("IO error reading CSV", e);
            } finally {
                try {
                    consumer.fetchEnd(producerSession, resultSet);
                } finally {
                    consumer.close();
                }
            }
        }

    }

}
