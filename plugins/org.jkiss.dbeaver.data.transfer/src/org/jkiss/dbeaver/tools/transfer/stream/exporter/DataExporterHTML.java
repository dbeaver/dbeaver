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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

/**
 * HTML Exporter
 */
public class DataExporterHTML extends StreamExporterAbstract {

    private static final int IMAGE_FRAME_SIZE = 200;

    private PrintWriter out;
    private List<DBDAttributeBinding> columns;
    private int rowCount = 0;

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
        out.write("<html>");
        out.write("\t<head>" +
            "\n\t<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\">" +
            "\n\t<style>" +
                "table {font-family:\"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;font-size:12px;text-align:left;border-collapse:collapse;margin:10px;} " +
                "th{font-size:14px;font-weight:normal;color:#039;padding:10px 8px;} " +
                "td{color:#669;padding:8px;}" +
                ".odd{background:#e8edff;}" +
                "img{padding:5px; border:solid; border-color: #dddddd #aaaaaa #aaaaaa #dddddd; border-width: 1px 2px 2px 1px; background-color:white;}" +
                "</style>\n</head>");
        out.write("<body><table>\n");
        out.write("<tr>");
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            String colName = columns.get(i).getLabel();
            if (CommonUtils.isEmpty(colName)) {
                colName = columns.get(i).getName();
            }
            writeTextCell(colName, true);
        }
        out.write("</tr>");
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException
    {
        out.write("<tr" + (rowCount++ % 2 == 0 ? " class=\"odd\"" : "") + ">");
        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns.get(i);
            if (DBUtils.isNullValue(row[i])) {
                writeTextCell(null, false);
            } else if (row[i] instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent)row[i];
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
                }
                finally {
                    content.release();
                }
            } else {
                String stringValue = super.getValueDisplayString(column, row[i]);
                boolean isImage = row[i] instanceof File && stringValue != null && stringValue.endsWith(".jpg");
                if (isImage) {
                    writeImageCell((File) row[i]);
                }
                else {
                    writeTextCell(stringValue, false);
                }
            }
        }
        out.write("</tr>\n");
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException
    {
        out.write("</table></body></html>");
    }

    private void writeTextCell(String value, boolean header)
    {
        out.write(header ? "<th>" : "<td>");
        if (value == null) {
            out.write("&nbsp;");
        }
        else {
            value = value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
            out.write(value);
        }
        out.write(header ? "</th>" : "</td>");
    }

    private void writeImageCell(File file) throws DBException
    {
        out.write("<td>");
        if (file == null || !file.exists()) {
            out.write("&nbsp;");
        }
        else {
            Image image = null;
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                throw new DBException("Can't read an exported image " + image, e);
            }

            if (image != null) {
                String imagePath = file.getAbsolutePath();
                imagePath = "files/" + imagePath.substring(imagePath.lastIndexOf(File.separator));

                int width = ((BufferedImage) image).getWidth();
                int height = ((BufferedImage) image).getHeight();
                int rwidth = width;
                int rheight = height;

                if (width > IMAGE_FRAME_SIZE || height > IMAGE_FRAME_SIZE) {
                    float scale = 1;
                    if (width > height) {
                        scale = IMAGE_FRAME_SIZE /(float)width;
                    }
                    else {
                        scale = IMAGE_FRAME_SIZE /(float)height;
                    }
                    rwidth = (int) (rwidth * scale);
                    rheight = (int) (rheight * scale);
                }
                out.write("<a href=\"" + imagePath + "\">");
                out.write("<img src=\"" + imagePath + "\" width=\"" + rwidth + "\" height=\"" + rheight + "\" />");
                out.write("</a>");
            }
            else {
                out.write("&nbsp;");
            }
        }
        out.write("</td>");
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
