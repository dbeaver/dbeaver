/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * JSON Exporter
 */
public class DataExporterJSON extends StreamExporterAbstract {

    private PrintWriter out;
    private List<DBDAttributeBinding> columns;
    private String tableName;
    private int rowNum = 0;
    private boolean printTableName = true;
    private DateFormat dateFormat;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        out = site.getWriter();

        TimeZone tz = TimeZone.getTimeZone("UTC");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(tz);
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
        tableName = getSite().getSource().getName();
        printHeader();
    }

    private void printHeader()
    {
        if (printTableName) {
            out.write("{\n");
            out.write("\"" + tableName + "\": ");
        }
        out.write("[\n");
    }

    @Override
    public void exportRow(DBRProgressMonitor monitor, Object[] row) throws DBException, IOException
    {
        if (rowNum > 0) {
            out.write(",\n");
        }
        rowNum++;
        out.write("\t{\n");
        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns.get(i);
            String columnName = column.getName();
            out.write("\t\t\"" + columnName + "\" : ");
            Object cellValue = row[i];
            if (DBUtils.isNullValue(cellValue)) {
                writeTextCell(null);
            } else if (cellValue instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent) cellValue;
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
                if (cellValue instanceof Number || cellValue instanceof Boolean) {
                    out.write(cellValue.toString());
                } else if (cellValue instanceof Date) {
                    writeTextCell(dateFormat.format(cellValue));
                } else {
                    writeTextCell(super.getValueDisplayString(column, cellValue));
                }
            }
            if (i < row.length - 1) {
                out.write(",");
            }
            out.write("\n");
        }
        out.write("\t}");
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException
    {
        out.write("\n]");
        if (printTableName) {
            out.write("}");
        }
        out.write("\n");
    }

    private void writeTextCell(@Nullable String value)
    {
        if (value != null) {
            out.write("\"" + value + "\"");
        } else {
            out.write("null");
        }
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
                out.write(buffer, 0, count);
            }
        } finally {
            ContentUtils.close(reader);
        }
    }

}
