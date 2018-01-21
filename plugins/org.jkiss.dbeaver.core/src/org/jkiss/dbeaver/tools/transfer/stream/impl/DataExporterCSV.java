/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Date;
import java.util.List;

/**
 * CSV Exporter
 */
public class DataExporterCSV extends StreamExporterAbstract {

    private static final String PROP_DELIMITER = "delimiter";
    private static final String PROP_HEADER = "header";
    private static final String PROP_QUOTE_CHAR = "quoteChar";
    private static final String PROP_QUOTE_ALWAYS = "quoteAlways";
    private static final String PROP_NULL_STRING = "nullString";
    private static final char DEF_DELIMITER = ',';
    private static final String DEF_QUOTE_CHAR = "\"";

    enum HeaderPosition {
        none,
        top,
        bottom,
        both
    }
    private String delimiter;
    private char quoteChar = '"';
    private boolean useQuotes = true;
    private boolean quoteAlways = true;
    private String rowDelimiter;
    private String nullString;
    private HeaderPosition headerPosition;
    private PrintWriter out;
    private List<DBDAttributeBinding> columns;

    private final StringBuilder buffer = new StringBuilder();

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        String delimString = String.valueOf(site.getProperties().get(PROP_DELIMITER));
        if (delimString == null || delimString.isEmpty()) {
            delimiter = String.valueOf(DEF_DELIMITER);
        } else {
            delimiter = delimString
                    .replace("\\t", "\t")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r");
        }
        Object quoteProp = site.getProperties().get(PROP_QUOTE_CHAR);
        String quoteStr = quoteProp == null ? DEF_QUOTE_CHAR : quoteProp.toString();
        if (!CommonUtils.isEmpty(quoteStr)) {
            quoteChar = quoteStr.charAt(0);
        }
        Object nullStringProp = site.getProperties().get(PROP_NULL_STRING);
        nullString = nullStringProp == null ? null : nullStringProp.toString();
        useQuotes = quoteChar != ' ';
        quoteAlways = CommonUtils.toBoolean(site.getProperties().get(PROP_QUOTE_ALWAYS));
        out = site.getWriter();
        rowDelimiter = GeneralUtils.getDefaultLineSeparator();
        try {
            headerPosition = HeaderPosition.valueOf(String.valueOf(site.getProperties().get(PROP_HEADER)));
        } catch (Exception e) {
            headerPosition = HeaderPosition.top;
        }
    }

    @Override
    public void dispose()
    {
        out = null;
        super.dispose();
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException
    {
        columns = getSite().getAttributes();
        if (headerPosition == HeaderPosition.top || headerPosition == HeaderPosition.both) {
            printHeader();
        }
    }

    private void printHeader()
    {
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            DBDAttributeBinding column = columns.get(i);
            String colName = column.getLabel();
            if (CommonUtils.isEmpty(colName)) {
                colName = column.getName();
            }
            writeCellValue(colName, true);
            if (i < columnsSize - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

    @Override
    public void exportRow(DBCSession session, Object[] row) throws DBException, IOException
    {
        for (int i = 0; i < row.length && i < columns.size(); i++) {
            DBDAttributeBinding column = columns.get(i);
            if (DBUtils.isNullValue(row[i])) {
                if (!CommonUtils.isEmpty(nullString)) {
                    out.write(nullString);
                }
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs == null) {
                        writeCellValue(DBConstants.NULL_VALUE_LABEL, false);
                    } else if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cs.getContentReader());
                    } else {
//                        out.write(quoteChar);
                        getSite().writeBinaryData(cs);
//                        out.write(quoteChar);
                    }
                }
                finally {
                    content.release();
                }
            } else {
                String stringValue = super.getValueDisplayString(column, row[i]);
                boolean quote = false;
                if (!stringValue.isEmpty() && !(row[i] instanceof Number) && !(row[i] instanceof Date) && Character.isDigit(stringValue.charAt(0))) {
                    // Quote string values which starts from number
                    quote = true;
                }
                writeCellValue(stringValue, quote);
            }
            if (i < row.length - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException
    {
        if (headerPosition == HeaderPosition.bottom || headerPosition == HeaderPosition.both) {
            printHeader();
        }
    }

    private void writeCellValue(String value, boolean quote)
    {
        if (!useQuotes) {
            quote = false;
        }
        // check for needed quote
        final boolean hasQuotes = useQuotes && value.indexOf(quoteChar) != -1;
        if (quoteAlways) {
            quote = true;
        } else if (!quote && !value.isEmpty()) {
            if (hasQuotes ||
                value.contains(delimiter) ||
                value.indexOf('\r') != -1 ||
                value.indexOf('\n') != -1 ||
                value.contains(rowDelimiter))
            {
                quote = true;
            }
        }
        if (quote && hasQuotes) {
            // escape quotes with double quotes
            buffer.setLength(0);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == quoteChar) {
                    buffer.append(quoteChar);
                }
                buffer.append(c);
            }
            value = buffer.toString();
        }
        if (quote) out.write(quoteChar);
        out.write(value);
        if (quote) out.write(quoteChar);
    }

    private void writeCellValue(Reader reader) throws IOException
    {
        try {
            if (useQuotes) out.write(quoteChar);
            // Copy reader
            char buffer[] = new char[2000];
            for (;;) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                for (int i = 0; i < count; i++) {
                    if (useQuotes && buffer[i] == quoteChar) {
                        out.write(quoteChar);
                    }
                    out.write(buffer[i]);
                }
            }
            if (useQuotes) out.write(quoteChar);
        } finally {
            ContentUtils.close(reader);
        }
    }

    private void writeDelimiter()
    {
        out.write(delimiter);
    }

    private void writeRowLimit()
    {
        out.write(rowDelimiter);
    }

}
