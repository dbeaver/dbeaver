/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.ui.export.IDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

/**
 * SQL Exporter
 */
public class DataExporterSQL extends DataExporterAbstract {

    private static final String PROP_ESCAPE = "escape";
    private static final String PROP_ROWS_IN_STATEMENT = "rowsInStatement";
    private static final char DEF_ESCAPE_CHAR = '\\';
    private static final char STRING_QUOTE = '\'';

    private char escapeChar = DEF_ESCAPE_CHAR;
    private String rowDelimiter;
    private int rowsInStatement;
    private PrintWriter out;
    private String tableName;
    private List<DBDColumnBinding> columns;

    private transient StringBuilder sqlBuffer = new StringBuilder(100);
    private transient long rowCount;

    @Override
    public void init(IDataExporterSite site) throws DBException
    {
        super.init(site);
        String escapeString = String.valueOf(site.getProperties().get(PROP_ESCAPE));
        if (escapeString == null || escapeString.isEmpty()) {
            escapeChar = DEF_ESCAPE_CHAR;
        } else if (escapeString.length() == 1) {
            escapeChar = escapeString.charAt(0);
        } else if (escapeString.charAt(0) == DEF_ESCAPE_CHAR) {
            switch (escapeString.charAt(1)) {
                case 't': escapeChar = '\t'; break;
                case 'n': escapeChar = '\n'; break;
                case 'r': escapeChar = '\r'; break;
                default: escapeChar = DEF_ESCAPE_CHAR;
            }
        } else {
            escapeChar = DEF_ESCAPE_CHAR;
        }
        try {
            rowsInStatement = Integer.parseInt(String.valueOf(site.getProperties().get(PROP_ROWS_IN_STATEMENT)));
        } catch (NumberFormatException e) {
            rowsInStatement = 10;
        }
        out = site.getWriter();
        rowDelimiter = ContentUtils.getDefaultLineSeparator();
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
        DBPNamedObject source = getSite().getSource();
        if (source instanceof DBSTable) {
            tableName = ((DBSTable)source).getFullQualifiedName();
        } else {
            throw new DBException("SQL export may be done only from table object");
        }
        rowCount = 0;
        // do nothing
    }

    public void exportRow(DBRProgressMonitor monitor, Object[] row) throws DBException, IOException
    {
        int columnsSize = columns.size();
        boolean firstRow = false;
        if (rowCount % rowsInStatement == 0) {
            sqlBuffer.setLength(0);
            if (rowCount > 0) {
                sqlBuffer.append(");").append(rowDelimiter);
            }
            sqlBuffer.append("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < columnsSize; i++) {
                DBDColumnBinding column = columns.get(i);
                if (i > 0) {
                    sqlBuffer.append(',');
                }
                sqlBuffer.append(column.getColumn().getName());
            }
            sqlBuffer.append(") VALUES (");
            if (rowsInStatement > 1) {
                sqlBuffer.append(rowDelimiter);
            }
            out.write(sqlBuffer.toString());
            firstRow = true;
        }
        rowCount++;
        for (int i = 0; i < columnsSize; i++) {
            if (!firstRow || i > 0) {
                out.write(',');
            }
            Object value = row[i];

            DBDColumnBinding column = columns.get(i);
            if (DBUtils.isNullValue(value)) {
                // just skip it
                out.write("NULL");
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
                try {
                    DBDContentStorage cs = content.getContents(monitor);
                    if (ContentUtils.isTextContent(content)) {
                        writeStringValue(cs.getContentReader());
                    } else {
                        getSite().writeBinaryData(cs.getContentStream(), cs.getContentLength());
                    }
                }
                finally {
                    content.release();
                }
            } else if (value instanceof File) {
                out.write("@");
                out.write(((File)value).getAbsolutePath());
            } else if (value instanceof String) {
                writeStringValue((String) value);
            } else {
                String stringValue = column.getValueHandler().getValueDisplayString(column.getColumn(), row[i]);
                out.write(stringValue);
            }
        }
        out.write(rowDelimiter);
    }

    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException
    {
        // do nothing
        if (rowCount > 0) {
            out.write(");");
        }
    }

    private void writeStringValue(String value)
    {
        // check for needed quote
        if (value.indexOf(STRING_QUOTE) != -1) {
            // escape quotes with double quotes
            StringBuilder buf = new StringBuilder(value.length() + 5);
            for (int i = 0; i <value.length(); i++) {
                char c = value.charAt(i);
                if (c == STRING_QUOTE) {
                    buf.append(escapeChar);
                }
                buf.append(c);
            }
        }
        out.write(STRING_QUOTE);
        out.write(value);
        out.write(STRING_QUOTE);
    }

    private void writeStringValue(Reader reader) throws IOException
    {
        try {
            out.write(STRING_QUOTE);
            // Copy reader
            char buffer[] = new char[2000];
            for (;;) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                for (int i = 0; i < count; i++) {
                    if (buffer[i] == STRING_QUOTE) {
                        out.write(escapeChar);
                    }
                    out.write(buffer[i]);
                }
            }
            out.write(STRING_QUOTE);
        } finally {
            ContentUtils.close(reader);
        }
    }

}
