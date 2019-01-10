/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

/**
 * XML Exporter
 */
public class DataExporterXML extends StreamExporterAbstract {

    private PrintWriter out;
    private List<DBDAttributeBinding> columns;
    private String tableName;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
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
    public void exportHeader(DBCSession session) throws DBException, IOException
    {
        columns = getSite().getAttributes();
        printHeader();
    }

    private void printHeader()
    {
        out.write("<?xml version=\"1.0\" ?>\n");
        tableName = escapeXmlElementName(getSite().getSource().getName());
        out.write("<!DOCTYPE " + tableName + " [\n");
        out.write("  <!ELEMENT " + tableName + " (DATA_RECORD*)>\n");
        out.write("  <!ELEMENT DATA_RECORD (");
        int columnsSize = columns.size();
        for (int i = 0; i < columnsSize; i++) {
            String colName = columns.get(i).getLabel();
            if (CommonUtils.isEmpty(colName)) {
                colName = columns.get(i).getName();
            }
            out.write(escapeXmlElementName(colName) + "?");
            if (i < columnsSize - 1) {
                out.write(",");
            }
        }
        out.write(")+>\n");
        for (int i = 0; i < columnsSize; i++) {
            out.write("  <!ELEMENT " + escapeXmlElementName(columns.get(i).getName()) + " (#PCDATA)>\n");
        }
        out.write("]>\n");
        out.write("<" + tableName + ">\n");
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException
    {
        out.write("  <DATA_RECORD>\n");
        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns.get(i);
            String columnName = escapeXmlElementName(column.getName());
            out.write("    <" + columnName + ">");
            if (DBUtils.isNullValue(row[i])) {
                writeTextCell(null);
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs != null) {
                        if (ContentUtils.isTextContent(content)) {
                            try (Reader reader = cs.getContentReader()) {
                                writeCellValue(reader);
                            }
                        } else {
                            getSite().writeBinaryData(cs);
                        }
                    }
                }
                finally {
                    content.release();
                }
            } else {
                writeTextCell(super.getValueDisplayString(column, row[i]));
            }
            out.write("</" + columnName + ">\n");
        }
        out.write("  </DATA_RECORD>\n");
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException
    {
        out.write("</" + tableName + ">\n");
    }

    private void writeTextCell(@Nullable String value)
    {
        if (value != null) {
            value = value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
            out.write(value);
        }
    }

    private void writeImageCell(File file) throws DBException
    {
        if (file != null && file.exists()) {
            Image image = null;
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                throw new DBException("Can't read an exported image " + image, e);
            }

            if (image != null) {
                String imagePath = file.getAbsolutePath();
                imagePath = "files/" + imagePath.substring(imagePath.lastIndexOf(File.separator));
                out.write(imagePath);
            }
        }
    }

    private void writeCellValue(Reader reader) throws IOException
    {
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
                } else if (buffer[i] == '&') {
                    out.write("&amp;");
                } else {
                    out.write(buffer[i]);
                }
            }
        }
    }

    private String escapeXmlElementName(String name) {
        return name.replaceAll("[^\\p{Alpha}\\p{Digit}]+","_");
    }
}
