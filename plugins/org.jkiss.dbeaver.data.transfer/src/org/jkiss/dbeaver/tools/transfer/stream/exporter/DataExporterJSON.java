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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IDocumentDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JSON Exporter
 */
public class DataExporterJSON extends StreamExporterAbstract implements IDocumentDataExporter {

    public static final String PROP_FORMAT_DATE_ISO = "formatDateISO";
    public static final String PROP_PRINT_TABLE_NAME = "printTableName";

    private DBDAttributeBinding[] columns;
    private String tableName;
    private int rowNum = 0;

    private boolean printTableName = true;
    private boolean formatDateISO = true;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        formatDateISO = CommonUtils.getBoolean(site.getProperties().get(PROP_FORMAT_DATE_ISO), true);
        printTableName = CommonUtils.getBoolean(site.getProperties().get(PROP_PRINT_TABLE_NAME), true);
    }

    @Override
    public void dispose()
    {
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
        PrintWriter out = getWriter();
        if (printTableName) {
            out.write("{\n");
            out.write("\"" + JSONUtils.escapeJsonString(tableName) + "\": ");
        }
        out.write("[\n");
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException
    {
        PrintWriter out = getWriter();
        if (rowNum > 0) {
            out.write(",\n");
        }
        rowNum++;
        if (isJsonDocumentResults(session.getProgressMonitor(), row)) {
            DBDDocument document = (DBDDocument) row[0];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            document.serializeDocument(session.getProgressMonitor(), buffer, StandardCharsets.UTF_8);
            String jsonText = buffer.toString(StandardCharsets.UTF_8.name());
            out.write(jsonText);
        } else {
            out.write("\t{\n");
            for (int i = 0; i < columns.length; i++) {
                DBDAttributeBinding column = columns[i];
                String columnName = column.getLabel();
                if (CommonUtils.isEmpty(columnName)) {
                    columnName = column.getName();
                }
                out.write("\t\t\"" + JSONUtils.escapeJsonString(columnName) + "\" : ");
                Object cellValue = row[column.getOrdinalPosition()];
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
                    } finally {
                        content.release();
                    }
                } else {
                    if (cellValue instanceof Number || cellValue instanceof Boolean) {
                        out.write(cellValue.toString());
                    } else if (cellValue instanceof Date && formatDateISO) {
                        writeTextCell(JSONUtils.formatDate((Date) cellValue));
                    } else {
                        writeTextCell(super.getValueDisplayString(column, cellValue));
                    }
                }
                if (i < columns.length - 1) {
                    out.write(",");
                }
                out.write("\n");
            }
            out.write("\t}");
        }
    }

    private boolean isJsonDocumentResults(DBRProgressMonitor progressMonitor, Object[] row) {
        if (columns.length == 1 && columns[0].getDataKind() == DBPDataKind.DOCUMENT) {
            if (row.length > 0 && !DBUtils.isNullValue(row[0]) && row[0] instanceof DBDDocument) {
                DBDDocument document = (DBDDocument) row[0];
                if (MimeTypes.TEXT_JSON.equalsIgnoreCase(document.getDocumentContentType())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException
    {
        PrintWriter out = getWriter();
        out.write("\n]");
        if (printTableName) {
            out.write("}");
        }
        out.write("\n");
    }

    private void writeTextCell(@Nullable String value)
    {
        if (value != null) {
            getWriter().write("\"" + JSONUtils.escapeJsonString(value) + "\"");
        } else {
            getWriter().write("null");
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
            getWriter().write(JSONUtils.escapeJsonString(new String(buffer, 0, count)));
        }
    }

}
