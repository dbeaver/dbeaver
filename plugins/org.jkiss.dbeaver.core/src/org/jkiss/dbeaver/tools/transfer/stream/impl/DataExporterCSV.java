/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.transfer.stream.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
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
    public static final char DEF_DELIMITER = ',';

    enum HeaderPosition {
        none,
        top,
        bottom,
        both
    }
    private char delimiter;
    private char quoteChar = '"';
    private String rowDelimiter;
    private HeaderPosition headerPosition;
    private PrintWriter out;
    private List<DBDAttributeBinding> columns;

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
        out = site.getWriter();
        rowDelimiter = ContentUtils.getDefaultLineSeparator();
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
    public void exportHeader(DBRProgressMonitor monitor) throws DBException, IOException
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
    public void exportRow(DBRProgressMonitor monitor, Object[] row) throws DBException, IOException
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
                    DBDContentStorage cs = content.getContents(monitor);
                    if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cs.getContentReader());
                    } else {
                        getSite().writeBinaryData(cs.getContentStream(), cs.getContentLength());
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
        // check for needed quote
        if (!quote && !value.isEmpty()) {
            if (value.indexOf(delimiter) != -1 || value.contains(rowDelimiter)) {
                quote = true;
            }
        }
        if (quote && value.indexOf(quoteChar) != -1) {
            // escape quotes with double quotes
            StringBuilder buf = new StringBuilder(value.length() + 5);
            for (int i = 0; i <value.length(); i++) {
                char c = value.charAt(i);
                if (c == quoteChar) {
                    buf.append(quoteChar);
                }
                buf.append(c);
            }
            value = buf.toString();
        }
        if (quote) out.write(quoteChar);
        out.write(value);
        if (quote) out.write(quoteChar);
    }

    private void writeCellValue(Reader reader) throws IOException
    {
        try {
            out.write(quoteChar);
            // Copy reader
            char buffer[] = new char[2000];
            for (;;) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                for (int i = 0; i < count; i++) {
                    if (buffer[i] == quoteChar) {
                        out.write(quoteChar);
                    }
                    out.write(buffer[i]);
                }
            }
            out.write(quoteChar);
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
