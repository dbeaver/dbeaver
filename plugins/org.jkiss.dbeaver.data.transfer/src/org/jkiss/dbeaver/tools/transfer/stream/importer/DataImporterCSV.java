/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import au.com.bytecode.opencsv.CSVReader;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.local.LocalStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.stream.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.*;
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
    private static final int MAX_COLUMN_LENGTH = 1024;

    private static final int MAX_DATA_TYPE_SAMPLES = 1000;
    private static final Pair<DBPDataKind, String> DATA_TYPE_UNKNOWN = new Pair<>(DBPDataKind.UNKNOWN, null);
    private static final Pair<DBPDataKind, String> DATA_TYPE_INTEGER = new Pair<>(DBPDataKind.NUMERIC, "INTEGER");
    private static final Pair<DBPDataKind, String> DATA_TYPE_REAL = new Pair<>(DBPDataKind.NUMERIC, "REAL");
    private static final Pair<DBPDataKind, String> DATA_TYPE_BOOLEAN = new Pair<>(DBPDataKind.BOOLEAN, "BOOLEAN");
    private static final Pair<DBPDataKind, String> DATA_TYPE_STRING = new Pair<>(DBPDataKind.STRING, "VARCHAR");

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

        try (Reader reader = openStreamReader(inputStream, processorProperties)) {
            try (CSVReader csvReader = openCSVReader(reader, processorProperties)) {
                String[] header = getNextLine(csvReader);
                if (header == null) {
                    return columnsInfo;
                }

                for (int i = 0; i < header.length; i++) {
                    String column = header[i];
                    if (headerPosition == HeaderPosition.none) {
                        column = "Column" + (i + 1);
                    } else {
                        column = DBUtils.getUnQuotedIdentifier(entityMapping.getDataSource(), column);
                    }
                    StreamDataImporterColumnInfo columnInfo = new StreamDataImporterColumnInfo(entityMapping, i, column, null, MAX_COLUMN_LENGTH, DBPDataKind.UNKNOWN);
                    columnInfo.setMappingMetadataPresent(headerPosition != HeaderPosition.none);
                    columnsInfo.add(columnInfo);
                }

                for (int sample = 0; sample < MAX_DATA_TYPE_SAMPLES; sample++) {
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
                        Pair<DBPDataKind, String> dataType = getDataType(line[i]);
                        StreamDataImporterColumnInfo columnInfo = columnsInfo.get(i);

                        switch (dataType.getFirst()) {
                            case STRING:
                                columnInfo.setDataKind(dataType.getFirst());
                                columnInfo.setTypeName(dataType.getSecond());
                                break;
                            case NUMERIC:
                            case BOOLEAN:
                                if (columnInfo.getDataKind() == DBPDataKind.UNKNOWN) {
                                    columnInfo.setDataKind(dataType.getFirst());
                                    columnInfo.setTypeName(dataType.getSecond());
                                }
                                break;
                        }
                    }
                }

                for (StreamDataImporterColumnInfo columnInfo : columnsInfo) {
                    if (columnInfo.getDataKind() == DBPDataKind.UNKNOWN) {
                        log.warn("Cannot guess data type for column '" + columnInfo.getName() + "', defaulting to VARCHAR");
                        columnInfo.setDataKind(DBPDataKind.STRING);
                        columnInfo.setTypeName("VARCHAR");
                    }
                }
            }
        } catch (IOException e) {
            throw new DBException("IO error reading CSV", e);
        }

        return columnsInfo;
    }

    private HeaderPosition getHeaderPosition(Map<String, Object> processorProperties) {
        return CommonUtils.valueOf(HeaderPosition.class, CommonUtils.toString(processorProperties.get(PROP_HEADER)), HeaderPosition.top);
    }

    private CSVReader openCSVReader(Reader reader, Map<String, Object> processorProperties) {
        String delimiter = StreamTransferUtils.getDelimiterString(processorProperties, PROP_DELIMITER);
        String quoteChar = CommonUtils.toString(processorProperties.get(PROP_QUOTE_CHAR));
        if (CommonUtils.isEmpty(quoteChar)) {
            quoteChar = "'";
        }
        String escapeChar = CommonUtils.toString(processorProperties.get(PROP_ESCAPE_CHAR));
        if (CommonUtils.isEmpty(escapeChar)) {
            escapeChar = "\\";
        }
        return new CSVReader(reader, delimiter.charAt(0), quoteChar.charAt(0), escapeChar.charAt(0));
    }

    private InputStreamReader openStreamReader(InputStream inputStream, Map<String, Object> processorProperties) throws UnsupportedEncodingException {
        String encoding = CommonUtils.toString(processorProperties.get(PROP_ENCODING), GeneralUtils.UTF8_ENCODING);
        return new InputStreamReader(inputStream, encoding);
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

    private Pair<DBPDataKind, String> getDataType(String value) {
        if (CommonUtils.isEmpty(value)) {
            return DATA_TYPE_UNKNOWN;
        }

        try {
            Integer.parseInt(value);
            return DATA_TYPE_INTEGER;
        } catch (NumberFormatException ignored) {
        }

        try {
            Double.parseDouble(value);
            return DATA_TYPE_REAL;
        } catch (NumberFormatException ignored) {
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return DATA_TYPE_BOOLEAN;
        }

        return DATA_TYPE_STRING;
    }

    @Override
    public void runImport(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource streamDataSource, @NotNull InputStream inputStream, @NotNull IDataTransferConsumer consumer) throws DBException {
        IStreamDataImporterSite site = getSite();
        StreamEntityMapping entityMapping = site.getSourceObject();
        Map<String, Object> properties = site.getProcessorProperties();
        HeaderPosition headerPosition = getHeaderPosition(properties);
        boolean emptyStringNull = CommonUtils.getBoolean(properties.get(PROP_EMPTY_STRING_NULL), false);
        String nullValueMark = CommonUtils.toString(properties.get(PROP_NULL_STRING));

        DBCExecutionContext context = streamDataSource.getDefaultInstance().getDefaultContext(monitor, false);
        try (DBCSession producerSession = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Transfer stream data")) {
            LocalStatement localStatement = new LocalStatement(producerSession, "SELECT * FROM Stream");
            StreamTransferResultSet resultSet = new StreamTransferResultSet(producerSession, localStatement, entityMapping);

            consumer.fetchStart(producerSession, resultSet, -1, -1);

            applyTransformHints(resultSet, consumer, properties, PROP_TIMESTAMP_FORMAT, PROP_TIMESTAMP_ZONE);

            try (Reader reader = openStreamReader(inputStream, properties)) {
                try (CSVReader csvReader = openCSVReader(reader, properties)) {

                    int maxRows = site.getSettings().getMaxRows();
                    int targetAttrSize = entityMapping.getStreamColumns().size();
                    boolean headerRead = false;
                    for (int lineNum = 0; ; ) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String[] line = csvReader.readNext();
                        if (line == null) {
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

                        if (lineNum % 1000 == 0) {
                            monitor.subTask(String.valueOf(lineNum) + " rows processed");
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
