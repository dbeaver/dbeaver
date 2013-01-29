/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.tools.data.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.tools.data.IDataExporterSite;
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
    private List<DBDAttributeBinding> columns;

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

    @Override
    public void exportHeader(DBRProgressMonitor monitor) throws DBException, IOException
    {
        columns = getSite().getAttributes();
        DBPNamedObject source = getSite().getSource();
        if (source instanceof DBSTable) {
            tableName = ((DBSTable)source).getFullQualifiedName();
        } else {
            throw new DBException("SQL export may be done only from table object");
        }
        rowCount = 0;
        // do nothing
    }

    @Override
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
                DBDAttributeBinding column = columns.get(i);
                if (i > 0) {
                    sqlBuffer.append(',');
                }
                sqlBuffer.append(column.getMetaAttribute().getName());
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

            DBDAttributeBinding column = columns.get(i);
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
            } else if (value instanceof Number) {
                out.write(value.toString());
            } else {
                out.write(super.getValueDisplayString(column, row[i]));
            }
        }
        out.write(rowDelimiter);
    }

    @Override
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
            value = buf.toString();
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
