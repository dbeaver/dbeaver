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

import java.io.*;
import java.time.format.DateTimeFormatter;
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
    private static final String PROP_TIMESTAMP_FORMAT = "timestampFormat";

    enum HeaderPosition {
        none,
        top,
        both
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
                for (;;) {
                    String[] line = csvReader.readNext();
                    if (line == null) {
                        break;
                    }
                    if (line.length == 0) {
                        continue;
                    }
                    for (int i = 0; i < line.length; i++) {
                        String column = line[i];
                        if (headerPosition == HeaderPosition.none) {
                            column = null;
                        } else {
                            column = DBUtils.getUnQuotedIdentifier(entityMapping.getDataSource(), column);
                        }
                        columnsInfo.add(
                            new StreamDataImporterColumnInfo(
                                entityMapping, i, column, "VARCHAR", 1024, DBPDataKind.STRING));
                    }
                    break;
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

    @Override
    public void runImport(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource streamDataSource, @NotNull InputStream inputStream, @NotNull IDataTransferConsumer consumer) throws DBException {
        IStreamDataImporterSite site = getSite();
        StreamEntityMapping entityMapping = site.getSourceObject();
        Map<String, Object> properties = site.getProcessorProperties();
        HeaderPosition headerPosition = getHeaderPosition(properties);
        boolean emptyStringNull = CommonUtils.getBoolean(properties.get(PROP_EMPTY_STRING_NULL), false);
        String nullValueMark = CommonUtils.toString(properties.get(PROP_NULL_STRING));
        DateTimeFormatter tsFormat = null;

        String tsFormatPattern = CommonUtils.toString(properties.get(PROP_TIMESTAMP_FORMAT));
        if (!CommonUtils.isEmpty(tsFormatPattern)) {
            try {
                tsFormat = DateTimeFormatter.ofPattern(tsFormatPattern);
            } catch (Exception e) {
                log.error("Wrong timestamp format: " + tsFormatPattern, e);
            }
            //Map<Object, Object> defTSProps = site.getSourceObject().getDataSource().getContainer().getDataFormatterProfile().getFormatterProperties(DBDDataFormatter.TYPE_NAME_TIMESTAMP);
        }

        DBCExecutionContext context = streamDataSource.getDefaultInstance().getDefaultContext(monitor, false);
        try (DBCSession producerSession = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Transfer stream data")) {
            LocalStatement localStatement = new LocalStatement(producerSession, "SELECT * FROM Stream");
            StreamTransferResultSet resultSet = new StreamTransferResultSet(producerSession, localStatement, entityMapping);
            if (tsFormat != null) {
                resultSet.setDateTimeFormat(tsFormat);
            }

            consumer.fetchStart(producerSession, resultSet, -1, -1);

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
                            for (int i = line.length; i < targetAttrSize - line.length; i++) {
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
