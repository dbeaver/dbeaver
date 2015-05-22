/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

/**
 * SQL Exporter
 */
public class DataExporterSQL extends StreamExporterAbstract {

    private static final String PROP_OMIT_SCHEMA = "omitSchema";
    private static final String PROP_ROWS_IN_STATEMENT = "rowsInStatement";
    private static final char STRING_QUOTE = '\'';

    private String rowDelimiter;
    private boolean omitSchema;
    private int rowsInStatement;
    private PrintWriter out;
    private String tableName;
    private List<DBDAttributeBinding> columns;

    private transient StringBuilder sqlBuffer = new StringBuilder(100);
    private transient long rowCount;
    private SQLDialect dialect;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        if (site.getProperties().containsKey(PROP_OMIT_SCHEMA)) {
            omitSchema = CommonUtils.toBoolean(site.getProperties().get(PROP_OMIT_SCHEMA));
        }
        try {
            rowsInStatement = Integer.parseInt(String.valueOf(site.getProperties().get(PROP_ROWS_IN_STATEMENT)));
        } catch (NumberFormatException e) {
            rowsInStatement = 10;
        }
        out = site.getWriter();
        rowDelimiter = GeneralUtils.getDefaultLineSeparator();
        dialect = SQLUtils.getDialectFromObject(site.getSource());
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
        if (source instanceof DBSObject) {
            tableName = omitSchema ?
                DBUtils.getQuotedIdentifier((DBSObject) source) :
                DBUtils.getObjectFullName(source);
        } else {
            throw new DBException("SQL export may be done only from table object");
        }
        rowCount = 0;
    }

    @Override
    public void exportRow(DBRProgressMonitor monitor, Object[] row) throws DBException, IOException
    {
        SQLDialect.MultiValueInsertMode insertMode = getMultiValueInsertMode();
        int columnsSize = columns.size();
        boolean firstRow = false;
        if (insertMode == SQLDialect.MultiValueInsertMode.NOT_SUPPORTED || rowCount % rowsInStatement == 0) {
            sqlBuffer.setLength(0);
            if (rowCount > 0) {
                if (insertMode == SQLDialect.MultiValueInsertMode.PLAIN) {
                    sqlBuffer.append(");").append(rowDelimiter);
                } else if (insertMode == SQLDialect.MultiValueInsertMode.GROUP_ROWS) {
                    sqlBuffer.append(";").append(rowDelimiter);
                }
            }
            sqlBuffer.append("INSERT INTO ").append(tableName).append(" (");
            for (int i = 0; i < columnsSize; i++) {
                DBDAttributeBinding column = columns.get(i);
                if (i > 0) {
                    sqlBuffer.append(',');
                }
                sqlBuffer.append(column.getName());
            }
            sqlBuffer.append(") VALUES ");
            if (insertMode != SQLDialect.MultiValueInsertMode.GROUP_ROWS) {
                sqlBuffer.append("(");
            }
            if (rowsInStatement > 1) {
                sqlBuffer.append(rowDelimiter);
            }
            out.write(sqlBuffer.toString());
            firstRow = true;
        }
        if (insertMode != SQLDialect.MultiValueInsertMode.NOT_SUPPORTED && !firstRow) {
            out.write(",");
        }
        if (insertMode == SQLDialect.MultiValueInsertMode.GROUP_ROWS) {
            out.write("(");
        }
        rowCount++;
        for (int i = 0; i < columnsSize; i++) {
            if (i > 0) {
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
        if (insertMode != SQLDialect.MultiValueInsertMode.PLAIN) {
            out.write(")");
        }
        if (insertMode == SQLDialect.MultiValueInsertMode.NOT_SUPPORTED) {
            out.write(";");
        }
        out.write(rowDelimiter);
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException
    {
        switch (getMultiValueInsertMode()) {
            case GROUP_ROWS:
                if (rowCount > 0) {
                    out.write(";");
                }
                break;
            case PLAIN:
                if (rowCount > 0) {
                    out.write(");");
                }
                break;
            default:
                break;
        }
    }

    private void writeStringValue(String value)
    {
        out.write(STRING_QUOTE);
        if (dialect != null) {
            out.write(dialect.escapeString(value));
        } else {
            out.write(value);
        }
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
                if (dialect != null) {
                    out.write(dialect.escapeString(String.valueOf(buffer, 0, count)));
                } else {
                    out.write(buffer, 0, count);
                }
            }
            out.write(STRING_QUOTE);
        } finally {
            ContentUtils.close(reader);
        }
    }

    private SQLDialect.MultiValueInsertMode getMultiValueInsertMode() {
        SQLDialect.MultiValueInsertMode insertMode = SQLDialect.MultiValueInsertMode.NOT_SUPPORTED;
        if (dialect != null) {
            insertMode = dialect.getMultiValueInsertMode();
        }
        return insertMode;
    }

}
