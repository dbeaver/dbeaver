/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.stream.IDocumentDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

/**
 * JSON Exporter
 */
public class DataExporterJSON extends StreamExporterAbstract implements IDocumentDataExporter {

    public static final String PROP_FORMAT_DATE_ISO = "formatDateISO";
    public static final String PROP_PRINT_TABLE_NAME = "printTableName";
    public static final String PROP_EXPORT_JSON_VALUES = "exportJsonValues";
    public static final String PROP_EXPORT_JSON_VALUES_AS_STRING = "string";
    public static final String PROP_EXPORT_JSON_VALUES_AS_JSON = "json";


    private DBDAttributeBinding[] columns;
    private String tableName;
    private int rowNum = 0;

    private boolean printTableName = true;
    private boolean formatDateISO = true;
    private String exportJsonAs = PROP_EXPORT_JSON_VALUES_AS_STRING;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
        super.init(site);
        formatDateISO = CommonUtils.getBoolean(site.getProperties().get(PROP_FORMAT_DATE_ISO), true);
        printTableName = CommonUtils.getBoolean(site.getProperties().get(PROP_PRINT_TABLE_NAME), true);
        exportJsonAs = (String) site.getProperties().getOrDefault(PROP_EXPORT_JSON_VALUES, PROP_EXPORT_JSON_VALUES_AS_STRING);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException {
        columns = getSite().getAttributes();
        tableName = getSite().getSource().getName();
        printHeader();
    }

    private void printHeader() {
        PrintWriter out = getWriter();
        if (printTableName) {
            out.write("{\n");
            out.write("\"" + JSONUtils.escapeJsonString(tableName) + "\": ");
        }
        out.write("[\n");
        rowNum = 0;
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException {
        PrintWriter out = getWriter();
        if (rowNum > 0) {
            out.write(",\n");
        }
        rowNum++;
        if (isJsonDocumentResults(row)) {
            writeDocument(session, (DBDDocument) row[0]);
        } else {
            out.write("\t{\n");

            for (int i = 0; i < columns.length; i++) {
                DBDAttributeBinding column = columns[i];
                String columnName = CommonUtils.isEmpty(column.getLabel()) ? column.getName() : column.getLabel();
                out.write("\t\t\"" + JSONUtils.escapeJsonString(columnName) + "\" : ");

                Object cellValue = row[i];
                if (DBUtils.isNullValue(cellValue)) {
                    writeTextCell(null, true);
                } else if (cellValue instanceof DBDContent content) {
                    writeContentValue(session, resultSet, content);
                } else if (cellValue instanceof Number || cellValue instanceof Boolean) {
                    out.write(cellValue.toString());
                } else if (cellValue instanceof Date && formatDateISO) {
                    writeTextCell(JSONUtils.formatDate((Date) cellValue), true);
                } else if (PROP_EXPORT_JSON_VALUES_AS_JSON.equalsIgnoreCase(exportJsonAs) && hasJsonDataType(column)) {
                    writeTextCell(super.getValueDisplayString(column, cellValue), false);
                } else {
                    writeTextCell(super.getValueDisplayString(column, cellValue), true);
                }

                if (i < columns.length - 1) {
                    out.write(",");
                }

                out.write("\n");
            }
            out.write("\t}");
        }
    }

    private boolean isJsonDocumentResults(@NotNull Object[] row) {
        if (!ArrayUtils.isEmpty(columns)) {
            DBPDataKind dataKind = columns[0].getDataKind();
            if (columns.length == 1 &&
                // STRUCT Kind - this is the case from Couchbase, it can contains JSON document also
                (dataKind == DBPDataKind.DOCUMENT || dataKind == DBPDataKind.STRUCT)) {
                if (row.length > 0 && !DBUtils.isNullValue(row[0]) && row[0] instanceof DBDDocument document) {
                    return MimeTypes.TEXT_JSON.equalsIgnoreCase(document.getDocumentContentType());
                }
            }
        }
        return false;
    }

    private void writeDocument(DBCSession session, DBDDocument document) throws DBException, IOException {
        document.serializeDocument(
            session.getProgressMonitor(),
            getOutputStream(),
            StandardCharsets.UTF_8
        );
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws IOException {
        PrintWriter out = getWriter();
        out.write("\n]");
        if (printTableName) {
            out.write("}");
        }
        out.write("\n");
    }

    private void writeTextCell(@Nullable String value, boolean escape) {
        if (value == null) {
            getWriter().write("null");
        } else if (escape) {
            getWriter().write("\"" + JSONUtils.escapeJsonString(value) + "\"");
        } else {
            getWriter().write(value);
        }
    }

    private void writeContentValue(
        DBCSession session,
        DBCResultSet resultSet,
        DBDContent content
    ) throws DBCException, IOException {
        // Content
        // Inline textual content and handle binaries in some special way

        try {
            DBDContentStorage cs = content.getContents(session.getProgressMonitor());
            if (cs != null) {
                if (ContentUtils.isTextContent(content)) {
                    writeClob(content, cs);
                } else {
                    writeBlob(cs);
                }
            }
        } finally {
            DTUtils.closeContents(resultSet, content);
        }
    }

    private void writeClob(
        DBDContent content,
        DBDContentStorage cs
    ) throws IOException {
        try (Reader in = cs.getContentReader()) {
            if (PROP_EXPORT_JSON_VALUES_AS_JSON.equalsIgnoreCase(exportJsonAs) && ContentUtils.isJSON(content)) {
                writeCellValue(in, false);
            } else {
                getWriter().write("\"");
                writeCellValue(in, true);
                getWriter().write("\"");
            }
        }
    }

    private void writeBlob(DBDContentStorage cs) throws IOException {
        getWriter().write("\"");
        getSite().writeBinaryData(cs);
        getWriter().write("\"");
    }

    private void writeCellValue(Reader reader, boolean escape) throws IOException {
        // Copy reader
        char[] buffer = new char[2000];
        while (true) {
            int count = reader.read(buffer);
            if (count <= 0) {
                break;
            }

            String chunk = new String(buffer, 0, count);

            getWriter().write(
                escape ? JSONUtils.escapeJsonString(chunk) : chunk
            );
        }
    }

    private boolean hasJsonDataType(@NotNull DBDAttributeBinding column) {
        DBCAttributeMetaData metaAttribute = column.getMetaAttribute();
        if (metaAttribute == null) {
            return false;
        }

        return metaAttribute.getTypeName().toLowerCase(Locale.ROOT).contains("json");
    }
}
