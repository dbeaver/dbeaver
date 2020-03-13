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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;

/**
 * Markdown Table Exporter
 */
public class DataExporterMarkdownTable extends StreamExporterAbstract {

    private static final String PROP_NULL_STRING = "nullString";
    private static final String PROP_FORMAT_NUMBERS = "formatNumbers";
    private static final String PROP_SHOW_HEADER_SEPARATOR = "showHeaderSeparator";
    private static final String PROP_CONFLUENCE_FORMAT = "confluenceFormat";

    private static final String PIPE_ESCAPE = "&#124;";

    private String rowDelimiter;
    private String nullString;
    private boolean showHeaderSeparator;
    private boolean confluenceFormat;
    private DBDAttributeBinding[] columns;

    private final StringBuilder buffer = new StringBuilder();

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);

        Object nullStringProp = site.getProperties().get(PROP_NULL_STRING);
        nullString = nullStringProp == null ? null : nullStringProp.toString();
        rowDelimiter = GeneralUtils.getDefaultLineSeparator();
        showHeaderSeparator = CommonUtils.getBoolean(site.getProperties().get(PROP_SHOW_HEADER_SEPARATOR), true);
        confluenceFormat = CommonUtils.getBoolean(site.getProperties().get(PROP_CONFLUENCE_FORMAT), false);
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    protected DBDDisplayFormat getValueExportFormat(DBDAttributeBinding column) {
        if (column.getDataKind() == DBPDataKind.NUMERIC && !Boolean.TRUE.equals(getSite().getProperties().get(PROP_FORMAT_NUMBERS))) {
            return DBDDisplayFormat.NATIVE;
        }
        return super.getValueExportFormat(column);
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException
    {
        columns = getSite().getAttributes();
        // Print separator line
        printHeader(false);
        if (showHeaderSeparator && !confluenceFormat) {
            printHeader(true);
        }
    }

    private void printHeader(boolean separator)
    {
        if (confluenceFormat) writeDelimiter();
        writeDelimiter();
        for (int i = 0, columnsSize = columns.length; i < columnsSize; i++) {
            DBDAttributeBinding column = columns[i];
            String colName = column.getLabel();
            if (CommonUtils.isEmpty(colName)) {
                colName = column.getName();
            }
            if (!separator) {
                writeCellValue(colName);
            } else {
                for (int k = 0; k < colName.length(); k++) {
                    getWriter().write('-');
                }
            }
            writeDelimiter();
            if (confluenceFormat) writeDelimiter();
        }
        writeRowLimit();
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException
    {
        writeDelimiter();
        for (int i = 0; i < row.length && i < columns.length; i++) {
            DBDAttributeBinding column = columns[i];
            if (DBUtils.isNullValue(row[i])) {
                if (!CommonUtils.isEmpty(nullString)) {
                    getWriter().write(nullString);
                }
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs == null) {
                        writeCellValue(DBConstants.NULL_VALUE_LABEL);
                    } else if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cs.getContentReader());
                    } else {
                        getSite().writeBinaryData(cs);
                    }
                }
                finally {
                    content.release();
                }
            } else {
                writeCellValue(super.getValueDisplayString(column, row[i]));
            }
            writeDelimiter();
        }
        writeRowLimit();
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException
    {
    }

    private void writeCellValue(String value)
    {
        // escape quotes with double quotes
        buffer.setLength(0);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '|') {
                buffer.append(PIPE_ESCAPE);
            } else {
                buffer.append(c);
            }
        }
        value = buffer.toString();

        getWriter().write(value);
    }

    private void writeCellValue(Reader reader) throws IOException
    {
        try {
            // Copy reader
            char buffer[] = new char[2000];
            for (;;) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                for (int i = 0; i < count; i++) {
                    if (buffer[i] == '|') {
                        getWriter().write(PIPE_ESCAPE);
                    } else {
                        getWriter().write(buffer[i]);
                    }
                }
            }
        } finally {
            ContentUtils.close(reader);
        }
    }

    private void writeDelimiter()
    {
        getWriter().write('|');
    }

    private void writeRowLimit()
    {
        getWriter().write(rowDelimiter);
    }

}
