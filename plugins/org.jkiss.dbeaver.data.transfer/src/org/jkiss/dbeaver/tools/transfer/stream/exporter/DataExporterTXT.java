/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IAppendableDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;

/**
 * TXT Exporter
 */
public class DataExporterTXT extends StreamExporterAbstract implements IAppendableDataExporter {
    
    private class ExportTextTarget {
        private final PrintWriter writer;
        private final DBRProgressMonitor monitor;
        private final StringBuilder stringBuilder = new StringBuilder();

        public ExportTextTarget(PrintWriter writer, DBRProgressMonitor monitor) {
            this.writer = writer;
            this.monitor = monitor;
        }

        /**
         * Write string builder content to print writer
         */
        public void flush() {
            writer.write(stringBuilder.toString());
            stringBuilder.setLength(0);
        }
        
        /**
         * Append character to string builder
         * Return the length of appended characters
         */
        public int append(char ch) {
            stringBuilder.append(ch);
            return 1;
        }

        /**
         * Appends string to string builder
         * Returns the length of appended string
         */
        public int append(String text) {
            stringBuilder.append(text);
            return text.length();
        }

        /**
         * Appends content to string builder
         * Returns the length of appended content
         */
        public int append(DBDContent content) throws IOException, DBCException {
            DBDContentStorage cs = content.getContents(monitor);
            if (cs == null) {
                return this.append(DBConstants.NULL_VALUE_LABEL);
            } else {
                try {
                    this.append(RAW_BLOB_OPEN);
                    // flush all the buffered data to the underlying output stream before writing raw data
                    this.flush();
                    writer.flush();
                    getSite().writeBinaryData(cs);
                    this.append(RAW_BLOB_CLOSE);
                    return (int) Math.min(Integer.MAX_VALUE, RAW_BLOB_OPEN.length() + cs.getContentLength() + RAW_BLOB_CLOSE.length());
                } finally {
                    cs.release();
                }
            }
        }
    }
    
    private interface CellValue {

        int getTextLength();

        int exportTo(ExportTextTarget target) throws DBCException, IOException;
    }
    
    private static class CellTextValue implements CellValue {
        public final String text;

        public CellTextValue(String text) {
            this.text = text;
        } 
        
        @Override
        public int getTextLength() {
            return text.length();
        }

        @Override
        public int exportTo(ExportTextTarget target) {
            return target.append(text);
        }
    }
    
    private class CellContentValue implements CellValue {
        public final DBDContent content;

        public CellContentValue(DBDContent content) {
            this.content = content;
        }
        
        @Override
        public int getTextLength() {
            try {
                long length = content.getContentLength() + RAW_BLOB_OPEN.length() + RAW_BLOB_CLOSE.length();
                return (int) Math.min(Integer.MAX_VALUE, length);
            } catch (DBCException ex) {
                return 0;
            }
        }

        @Override
        public int exportTo(ExportTextTarget target) throws DBCException, IOException {
            try {
                return target.append(content);
            } finally {
                content.release();
            }
        }
    }

    private static final String PROP_BATCH_SIZE = "batchSize";
    private static final String PROP_MIN_COLUMN_LENGTH = "minColumnLength";
    private static final String PROP_MAX_COLUMN_LENGTH = "maxColumnLength";
    private static final String PROP_SHOW_NULLS = "showNulls";
    private static final String PROP_DELIM_LEADING = "delimLeading";
    private static final String PROP_DELIM_HEADER = "delimHeader";
    private static final String PROP_DELIM_TRAILING = "delimTrailing";
    private static final String PROP_DELIM_BETWEEN = "delimBetween";
    private static final String PROP_SHOW_HEADER = "showHeader";

    private int batchSize = 200;
    private int maxColumnSize = 0;
    private int minColumnSize = 1;
    private boolean showHeader;
    private boolean showNulls;
    private boolean delimLeading;
    private boolean delimHeader;
    private boolean delimTrailing;
    private boolean delimBetween;
    private Deque<CellValue[]> batchQueue;

    // The followings may be a setting some time
    private static final boolean QUOTE_BLOBS = true;
    private static final char QUOTE_BLOB_CHAR = '"';
    private static final String QUOTE_BLOB_REPLACEMENT = "\\\"";
    private static final String BLOB_OVERFLOW_MARK = " [BLOB is too large]";
    private static final int BLOB_CONTENT_MIN_LENGTH = 20;
    private static final String RAW_BLOB_OPEN = "[BLOB[";
    private static final String RAW_BLOB_CLOSE = "]]";
    
    private final StringBuilder blobContentBuffer = new StringBuilder();
    private DBDAttributeBinding[] columns;
    private int[] colWidths;
    private int blobContentMaxLength = 0;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
        super.init(site);
        Map<String, Object> properties = site.getProperties();
        this.batchSize = Math.max(CommonUtils.toInt(properties.get(PROP_BATCH_SIZE), 200), 200);
        this.minColumnSize = Math.max(CommonUtils.toInt(properties.get(PROP_MIN_COLUMN_LENGTH), 1), 1);
        this.maxColumnSize = Math.max(CommonUtils.toInt(properties.get(PROP_MAX_COLUMN_LENGTH), 0), 0);
        this.showNulls = CommonUtils.getBoolean(properties.get(PROP_SHOW_NULLS), false);
        this.delimLeading = CommonUtils.getBoolean(properties.get(PROP_DELIM_LEADING), true);
        this.delimHeader = CommonUtils.getBoolean(properties.get(PROP_DELIM_HEADER), true);
        this.delimTrailing = CommonUtils.getBoolean(properties.get(PROP_DELIM_TRAILING), true);
        this.delimBetween = CommonUtils.getBoolean(properties.get(PROP_DELIM_BETWEEN), true);
        this.showHeader = CommonUtils.getBoolean(properties.get(PROP_SHOW_HEADER), true);
        this.batchQueue = new ArrayDeque<>(this.batchSize);
        if (this.maxColumnSize > 0) {
            this.maxColumnSize = Math.max(this.maxColumnSize, this.minColumnSize);
        }
        if (this.maxColumnSize == 0) {
            this.blobContentMaxLength = Integer.MAX_VALUE - BLOB_OVERFLOW_MARK.length();
        } else {
            this.blobContentMaxLength = Math.min(this.maxColumnSize, Integer.MAX_VALUE) - BLOB_OVERFLOW_MARK.length();
            if (this.blobContentMaxLength < 0) {
                this.blobContentMaxLength = this.BLOB_CONTENT_MIN_LENGTH;
            }
        }
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException {
        columns = getSite().getAttributes();
        colWidths = new int[columns.length];
        Arrays.fill(colWidths, minColumnSize);

        if (showHeader) {
            final CellValue[] header = new CellValue[columns.length];

            for (int index = 0; index < columns.length; index++) {
                String cell = getAttributeName(columns[index]);
                if (maxColumnSize > 0) {
                    cell = CommonUtils.truncateString(cell, maxColumnSize);
                }
                header[index] = new CellTextValue(cell);
            }

            appendRow(header, session.getProgressMonitor());
        }
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException {
        final CellValue[] values = new CellValue[columns.length];

        for (int index = 0; index < columns.length; index++) {
            if (row[index] instanceof DBDContent) {
                DBDContent content = (DBDContent) row[index];
                if (ContentUtils.isTextContent(content)) {
                    try {
                        DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                        if (cs == null) {
                            values[index] = new CellTextValue(DBConstants.NULL_VALUE_LABEL);
                        } else {
                            values[index] = new CellTextValue(stringifyContent(cs.getContentReader()));
                        }
                    } finally {
                        content.release();
                    }
                } else {
                    values[index] = new CellContentValue(content);
                }
            } else {
                String cell = getCellString(columns[index], row[index]);
                if (maxColumnSize > 0) {
                    cell = CommonUtils.truncateString(cell, maxColumnSize);
                }
                values[index] = new CellTextValue(cell);
            }
        }

        appendRow(values, session.getProgressMonitor());
    }

    private String stringifyContent(Reader reader) throws IOException {
        try {
            blobContentBuffer.setLength(0);
            
            int rest = blobContentMaxLength;
            if (QUOTE_BLOBS) {
                blobContentBuffer.append(QUOTE_BLOB_CHAR);
                rest -= 2;
            }
            // Copy reader
            char[] buffer = new char[2000];
            for (;;) {
                int count = reader.read(buffer, 0, buffer.length);
                if (count <= 0) {
                    break;
                }
                if (QUOTE_BLOBS && QUOTE_BLOB_REPLACEMENT != null) {
                    if (QUOTE_BLOB_REPLACEMENT.length() > 1) {
                        for (int i = 0; i < count; i++) {
                            if (buffer[i] == QUOTE_BLOB_CHAR) {
                                if (blobContentBuffer.length() + QUOTE_BLOB_REPLACEMENT.length() > blobContentMaxLength) {
                                    break;
                                } else {
                                    blobContentBuffer.append(QUOTE_BLOB_REPLACEMENT);
                                    rest -= (QUOTE_BLOB_REPLACEMENT.length() - 1);
                                }
                            } else {
                                blobContentBuffer.append(buffer[i]);
                            }
                        }
                    } else {
                        int limit = Math.min(count, rest);
                        for (int i = 0; i < limit; i++) {
                            if (buffer[i] == QUOTE_BLOB_CHAR) {
                                blobContentBuffer.append(QUOTE_BLOB_REPLACEMENT);
                            } else {
                                blobContentBuffer.append(buffer[i]);
                            }
                        }               
                    }
                } else {
                    blobContentBuffer.append(buffer, 0, Math.min(count, rest));
                }
                if (rest < count) {
                    blobContentBuffer.append(BLOB_OVERFLOW_MARK);
                    break;
                } else {
                    rest -= count;
                }
            }
            if (QUOTE_BLOBS) {
                blobContentBuffer.append(QUOTE_BLOB_CHAR);
            }
            return blobContentBuffer.toString();
        } finally {
            ContentUtils.close(reader);
        }
    }
    
    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException {
        writeQueue(monitor);
    }

    @Override
    public void importData(@NotNull IStreamDataExporterSite site) {
        // No pre-initialization process is needed.
    }

    @Override
    public boolean shouldTruncateOutputFileBeforeExport() {
        return false;
    }

    private void appendRow(CellValue[] row, DBRProgressMonitor monitor) throws DBCException, IOException {
        if (batchQueue.size() == batchSize) {
            writeQueue(monitor);
        }

        batchQueue.add(row);
    }

    private void writeQueue(DBRProgressMonitor monitor) throws DBCException, IOException {
        if (batchQueue.isEmpty()) {
            return;
        }
        
        ExportTextTarget target = new ExportTextTarget(getWriter(), monitor);

        for (CellValue[] row : batchQueue) {
            for (int index = 0; index < columns.length; index++) {
                final CellValue value = row[index];
                int valueLength = value.getTextLength();
                if (maxColumnSize > 0 && valueLength > maxColumnSize) {
                    colWidths[index] = maxColumnSize;
                } else if (valueLength > colWidths[index]) {
                    colWidths[index] = valueLength;
                }
            }
        }

        while (!batchQueue.isEmpty()) {
            if (showHeader) {
                writeRow(target, batchQueue.poll(), ' ');
            }

            if (delimHeader) {
                delimHeader = false;
                writeRow(target, null, '-');
            }

            if (!showHeader) {
                writeRow(target, batchQueue.poll(), ' ');
            }
        }

        getWriter().flush();
    }

    private void writeRow(ExportTextTarget target, CellValue[] values, char fill) throws DBCException, IOException {
        if (delimLeading) {
            target.append('|');
        }

        for (int index = 0, length = columns.length; index < length; index++) {
            int actualLength = ArrayUtils.isEmpty(values) ? 0 : values[index].exportTo(target);
            if (actualLength > colWidths[index]) {
                colWidths[index] = Math.min(actualLength, maxColumnSize);
            }

            if (index < length - 1 || delimTrailing || fill != ' ') {
                for (int width = actualLength; width < colWidths[index]; width++) {
                    target.append(fill);
                }
            }

            if (index < length - 1) {
                target.append(delimBetween ? '|' : ' ');
            }
        }

        if (delimTrailing) {
            target.append('|');
        }

        target.append(CommonUtils.getLineSeparator());
        target.flush();
    }

    private String getCellString(DBDAttributeBinding attr, Object value) {
        final String displayString = attr.getValueHandler().getValueDisplayString(attr, value, getValueExportFormat(attr));

        if (DBUtils.isNullValue(value)) {
            return showNulls ? DBConstants.NULL_VALUE_LABEL : "";
        }
        return CommonUtils.getSingleLineString(displayString);
    }

    private static String getAttributeName(DBDAttributeBinding attr) {
        if (CommonUtils.isEmpty(attr.getLabel())) {
            return attr.getName();
        } else {
            return attr.getLabel();
        }
    }
}
