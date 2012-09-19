/*
 * Copyright (C) 2010-2012 Serge Rieder
 * eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ui.export.data.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.export.data.IDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Date;
import java.util.List;

/**
 * CSV Exporter
 */
public class DataExporterHTML extends DataExporterAbstract {

    private PrintWriter out;
    private List<DBDAttributeBinding> columns;
    private int rowCount = 0;

    @Override
    public void init(IDataExporterSite site) throws DBException
    {
        super.init(site);
        out = site.getWriter();
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
        columns = getSite().getColumns();
        printHeader();
    }

    private void printHeader()
    {
        out.write("<html>");
        out.write("<head><style>" +
                "table {font-family:\"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;font-size:12px;text-align:left;border-collapse:collapse;margin:10px;} " +
                "th{font-size:14px;font-weight:normal;color:#039;padding:10px 8px;} " +
                "td{color:#669;padding:8px;}" +
                ".odd{background:#e8edff;}" +
                "</style></head>");
        out.write("<body><table>");
        out.write("<tr>");
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            writeCellValue(columns.get(i).getAttribute().getName(), true, false);
        }
        out.write("</tr>");
    }

    @Override
    public void exportRow(DBRProgressMonitor monitor, Object[] row) throws DBException, IOException
    {
        out.write("<tr" + (rowCount++ % 2 == 0 ? " class=\"odd\"" : "") + ">");
        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns.get(i);
            if (DBUtils.isNullValue(row[i])) {
                writeCellValue(null, false, false);
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
                try {
                    DBDContentStorage cs = content.getContents(monitor);
                    out.write("<td>");
                    if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cs.getContentReader());
                    } else {
                        getSite().writeBinaryData(cs.getContentStream(), cs.getContentLength());
                    }
                    out.write("</td>");
                }
                finally {
                    content.release();
                }
            } else {
                String stringValue = super.getValueDisplayString(column, row[i]);
                boolean isImage = row[i] instanceof File && stringValue != null && stringValue.endsWith(".jpg");
                if (isImage) {
                    stringValue = "files/" + stringValue.substring(stringValue.lastIndexOf(File.separator));
                }
                writeCellValue(stringValue, false, isImage);
            }
        }
        out.write("</tr>");
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException
    {
        out.write("</table></body></html>");
    }

    private void writeCellValue(String value, boolean header, boolean image)
    {
        out.write(header ? "<th>" : "<td>");
        if (value == null) {
            out.write("&nbsp;");
        }
        else {
            if (image) {
                out.write("<img src=\"" + value + "\" hspace=\"10\" vspace=\"10\" />");
            }
            else {
                value = value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
                out.write(value);
            }
        }
        out.write(header ? "</th>" : "</td>");
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
                    if (buffer[i] == '<') {
                        out.write("&lt;");
                    }
                    else if (buffer[i] == '>') {
                        out.write("&gt;");
                    }
                    if (buffer[i] == '&') {
                        out.write("&amp;");
                    }
                    out.write(buffer[i]);
                }
            }
        } finally {
            ContentUtils.close(reader);
        }
    }

    public boolean saveBinariesAsImages()
    {
        return true;
    }
}
