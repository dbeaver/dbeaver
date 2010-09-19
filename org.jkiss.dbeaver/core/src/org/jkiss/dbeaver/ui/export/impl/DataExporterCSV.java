/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.export.IDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

/**
 * CSV Exporter
 */
public class DataExporterCSV extends DataExporterAbstract {

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
    private List<DBDColumnBinding> columns;

    @Override
    public void init(IDataExporterSite site) throws DBException
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
        rowDelimiter = System.getProperty("line.separator");
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

    public void exportHeader(DBRProgressMonitor monitor) throws DBException, IOException
    {
        columns = getSite().getColumns();
        if (headerPosition == HeaderPosition.top || headerPosition == HeaderPosition.both) {
            printHeader();
        }
    }

    private void printHeader()
    {
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            DBDColumnBinding column = columns.get(i);
            writeCellValue(column.getColumn().getName(), true);
            if (i < columnsSize - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

    public void exportRow(DBRProgressMonitor monitor, Object[] row) throws DBException, IOException
    {
        for (int i = 0; i < row.length; i++) {
            DBDColumnBinding column = columns.get(i);
            if (DBUtils.isNullValue(row[i])) {
                // just skip it
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
                DBDContentStorage cs = content.getContents(monitor);
                try {
                    if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cs.getContentReader());
                    } else {
                        getSite().writeBinaryData(cs.getContentStream(), cs.getContentLength());
                    }
                }
                finally {
                    cs.release();
                }
            } else {
                String stringValue = column.getValueHandler().getValueDisplayString(column.getColumn(), row[i]);
                writeCellValue(stringValue, false);
            }
            if (i < row.length - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

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
            if (Character.isDigit(value.charAt(0)) || value.indexOf(delimiter) != -1 || value.indexOf(rowDelimiter) != -1) {
                quote = true;
            }
        }
        if (value.indexOf(quoteChar) != -1) {
            // escape quotes with double quotes
            StringBuilder buf = new StringBuilder(value.length() + 5);
            for (int i = 0; i <value.length(); i++) {
                char c = value.charAt(i);
                if (c == quoteChar) {
                    buf.append(quoteChar);
                }
                buf.append(c);
            }
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
