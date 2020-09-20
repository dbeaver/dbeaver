/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Map;

/**
 * HTML Exporter
 */
public class DataExporterHTML extends StreamExporterAbstract {

    private static final String PROP_HEADER = "header";

    private String name;
    private static final int IMAGE_FRAME_SIZE = 200;

    private DBDAttributeBinding[] columns;
    private int rowCount = 0;

    private boolean outputHeader = true;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
        super.init(site);

        Map<String, Object> properties = site.getProperties();
        outputHeader = CommonUtils.getBoolean(properties.get(PROP_HEADER), outputHeader);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException {
        name = getSite().getSource().getName();
        columns = getSite().getAttributes();
        printHeader();
    }

    private void printHeader() {
        PrintWriter out = getWriter();
        out.write("<!DOCTYPE html>\n<html>\n");
        out.write("<head>\n" +
            "<meta charset=\"" + getSite().getOutputEncoding() + "\"/>" +
            "<style>\n" +
            "table {border: medium solid #6495ed;" +
            "border-collapse: collapse;" +
            "width: 100%;} " +
            "th{font-family: monospace;" +
            "border: thin solid #6495ed;" +
//              "width: 50%;" +
            "padding: 5px;" +
            "background-color: #D0E3FA;}" +
            "td{font-family: sans-serif;" +
            "border: thin solid #6495ed;" +
//              "width: 50%;" +
            "padding: 5px;" +
            "text-align: center;}" +
            ".odd{background:#e8edff;}" +
            "img{padding:5px; border:solid; border-color: #dddddd #aaaaaa #aaaaaa #dddddd; border-width: 1px 2px 2px 1px; background-color:white;}" +
            "</style>\n</head>\n");
        out.write("<body>\n<table>");

        if (outputHeader) {
            out.write("<tr>");
            writeTableTitle(name, columns.length);
            out.write("</tr>");
            out.write("<tr>");
            for (DBDAttributeBinding column : columns) {
                String colName = column.getLabel();
                if (CommonUtils.isEmpty(colName)) {
                    colName = column.getName();
                }
                writeTextCell(colName, true);
            }
            out.write("</tr>");
        }
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException {
        PrintWriter out = getWriter();
        out.write("<tr" + (rowCount++ % 2 == 0 ? " class=\"odd\"" : "") + ">");
        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns[i];
            if (DBUtils.isNullValue(row[i])) {
                writeTextCell(null, false);
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent) row[i];
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    out.write("<td>");
                    if (cs != null) {
                        if (ContentUtils.isTextContent(content)) {
                            writeCellValue(cs.getContentReader());
                        } else {
                            getSite().writeBinaryData(cs);
                        }
                    }
                    out.write("</td>");
                } finally {
                    content.release();
                }
            } else {
                String stringValue = super.getValueDisplayString(column, row[i]);
                boolean isImage = row[i] instanceof File && stringValue != null && stringValue.endsWith(".jpg");
                if (isImage) {
                    writeImageCell((File) row[i]);
                } else {
                    writeTextCell(stringValue, false);
                }
            }
        }
        out.write("</tr>\n");
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) {
        getWriter().write("</table></body></html>");
    }

    private void writeTableTitle(String value, int columns) {
        PrintWriter out = getWriter();
        out.write(String.format("<th colspan=\"%d\">", columns));
        if (value == null) {
            out.write("&nbsp;");
        } else {
            value = value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
            out.write(value);
        }
        out.write("</th>");
    }

    private void writeTextCell(String value, boolean header) {
        PrintWriter out = getWriter();
        out.write(header ? "<th>" : "<td>");
        if (value == null) {
            out.write("&nbsp;");
        } else {
            value = value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
            out.write(value);
        }
        out.write(header ? "</th>" : "</td>");
    }

    private void writeImageCell(File file) throws DBException {
        PrintWriter out = getWriter();
        out.write("<td>");
        if (file == null || !file.exists()) {
            out.write("&nbsp;");
        } else {
            BufferedImage image = null;
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                throw new DBException("Can't read an exported image " + image, e);
            }

            if (image != null) {
                String imagePath = file.getAbsolutePath();
                imagePath = "files/" + imagePath.substring(imagePath.lastIndexOf(File.separator));

                int width = image.getWidth();
                int height = image.getHeight();
                int rwidth = width;
                int rheight = height;

                if (width > IMAGE_FRAME_SIZE || height > IMAGE_FRAME_SIZE) {
                    float scale;
                    if (width > height) {
                        scale = IMAGE_FRAME_SIZE / (float) width;
                    } else {
                        scale = IMAGE_FRAME_SIZE / (float) height;
                    }
                    rwidth = (int) (rwidth * scale);
                    rheight = (int) (rheight * scale);
                }
                out.write("<a href=\"" + imagePath + "\">");
                out.write("<img src=\"" + imagePath + "\" width=\"" + rwidth + "\" height=\"" + rheight + "\" />");
                out.write("</a>");
            } else {
                out.write("&nbsp;");
            }
        }
        out.write("</td>");
    }

    private void writeCellValue(Reader reader) throws IOException {
        try {
            PrintWriter out = getWriter();
            // Copy reader
            char[] buffer = new char[2000];
            for (; ; ) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                for (int i = 0; i < count; i++) {
                    if (buffer[i] == '<') {
                        out.write("&lt;");
                    } else if (buffer[i] == '>') {
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

    public boolean saveBinariesAsImages() {
        return true;
    }
}
