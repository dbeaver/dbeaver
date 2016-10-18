/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
    public static final char DEF_DELIMITER = ',';
    public static final String DEF_QUOTE_CHAR = "\"";

    enum HeaderPosition {
        none,
        top,
        bottom,
        both
    }
    private char delimiter;
    private char quoteChar = '"';
    private boolean useQuotes = true;
    private String rowDelimiter;
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
            delimiter = DEF_DELIMITER;
        } else if (delimString.length() == 1) {
            delimiter = delimString.charAt(0);
        } else if (delimString.charAt(0) == '\\') {
            switch (delimString.charAt(1)) {
                case 't': delimiter = '\t'; break;
                case 'n': delimiter = '\n'; break;
                case 'r': delimiter = '\r'; break;
                default: delimiter = DEF_DELIMITER;
            }
        } else {
            delimiter = DEF_DELIMITER;
        }
        Object quoteProp = site.getProperties().get(PROP_QUOTE_CHAR);
        String quoteStr = quoteProp == null ? DEF_QUOTE_CHAR : quoteProp.toString();
        if (!CommonUtils.isEmpty(quoteStr)) {
            quoteChar = quoteStr.charAt(0);
        }
        useQuotes = quoteChar != ' ';
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
        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns.get(i);
            if (DBUtils.isNullValue(row[i])) {
                // just skip it
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
        if (!quote && !value.isEmpty()) {
            if (hasQuotes ||
                value.indexOf(delimiter) != -1 ||
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
