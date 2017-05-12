/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream.impl;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

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

    public static final String PROP_FORMAT_DATE_ISO = "formatDateISO";
    public static final String PROP_PRINT_TABLE_NAME = "printTableName";

    private PrintWriter out;
    private List<DBDAttributeBinding> columns;
    private String tableName;
    private int rowNum = 0;
    private DateFormat dateFormat;

    private boolean printTableName = true;
    private boolean formatDateISO = true;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        out = site.getWriter();
        formatDateISO = CommonUtils.getBoolean(site.getProperties().get(PROP_FORMAT_DATE_ISO), true);
        printTableName = CommonUtils.getBoolean(site.getProperties().get(PROP_PRINT_TABLE_NAME), true);

        TimeZone tz = TimeZone.getTimeZone("UTC");
        dateFormat = new SimpleDateFormat(DBConstants.DEFAULT_ISO_TIMESTAMP_FORMAT);
        dateFormat.setTimeZone(tz);
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
        tableName = getSite().getSource().getName();
        printHeader();
    }

    private void printHeader()
    {
        if (printTableName) {
            out.write("{\n");
            out.write("\"" + escapeJsonString(tableName) + "\": ");
        }
        out.write("[\n");
    }

    @Override
    public void exportRow(DBCSession session, Object[] row) throws DBException, IOException
    {
        if (rowNum > 0) {
            out.write(",\n");
        }
        rowNum++;
        out.write("\t{\n");
        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns.get(i);
            String columnName = column.getName();
            out.write("\t\t\"" + escapeJsonString(columnName) + "\" : ");
            Object cellValue = row[i];
            if (DBUtils.isNullValue(cellValue)) {
                writeTextCell(null);
            } else if (cellValue instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent) cellValue;
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs != null) {
                        if (ContentUtils.isTextContent(content)) {
                            try (Reader in = cs.getContentReader()) {
                                out.write("\"");
                                writeCellValue(in);
                                out.write("\"");
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
                if (cellValue instanceof Number || cellValue instanceof Boolean) {
                    out.write(cellValue.toString());
                } else if (cellValue instanceof Date && formatDateISO) {
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
            out.write("\"" + escapeJsonString(value) + "\"");
        } else {
            out.write("null");
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
            out.write(escapeJsonString(new String(buffer, 0, count)));
        }
    }

    private static String escapeJsonString(String str) {
        if (str == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '"':
                case '\\':
                case '/':
                    result.append("\\").append(c);
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString();
    }

}
