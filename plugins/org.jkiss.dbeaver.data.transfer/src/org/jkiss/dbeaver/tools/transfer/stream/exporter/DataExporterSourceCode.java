/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.stream.IDocumentDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Date;
import java.util.Map;

public class DataExporterSourceCode extends StreamExporterAbstract implements IDocumentDataExporter {

    private static final String PROP_FORMAT_DATE_ISO = "formatDateISOPHP";
    private static final String PROP_LANGUAGE = "language";

    private DBDAttributeBinding[] columns;
    private int rowNum = 0;

    private boolean formatDateISO = true;
    private ProgramLanguages language;

    enum ProgramLanguages {
        PHP_VERSION_LESS_5_and_4 ("php version less 5.4"), PHP_VERSION_AT_LEAST_5_AND_4("php version at least 5.4");
        private final String value;
        ProgramLanguages(String v) {
            value = v;
        }
        public String value() {
            return value;
        }
        public static ProgramLanguages fromValue(String v) {
            for (ProgramLanguages s : ProgramLanguages.values()) {
                if (s.value.equals(v)) {
                    return s;
                }
            }
            return PHP_VERSION_LESS_5_and_4;
        }
    }

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        Map<Object, Object> properties = site.getProperties();
        formatDateISO = CommonUtils.getBoolean(site.getProperties().get(PROP_FORMAT_DATE_ISO), true);
        language = ProgramLanguages.fromValue(CommonUtils.toString(properties.get(PROP_LANGUAGE)));
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException {
        columns = getSite().getAttributes();
        String tableName = getSite().getSource().getName();
        PrintWriter out = getWriter();
        out.write("<?php\n");
        out.write("$" + tableName + " = ");
        if(language == ProgramLanguages.PHP_VERSION_LESS_5_and_4) {
            out.write("array(\n");
        } else {
            out.write("[\n");
        }
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException {
        PrintWriter out = getWriter();
        if (rowNum > 0) {
            out.write(",\n");
        }
        rowNum++;
        if(language == ProgramLanguages.PHP_VERSION_LESS_5_and_4) {
            out.write("\tarray(\n");
        } else {
            out.write("\t[\n");
        }
        for (int i = 0; i < columns.length; i++) {
            DBDAttributeBinding column = columns[i];
            String columnName = column.getLabel();
            if (CommonUtils.isEmpty(columnName)) {
                columnName = column.getName();
            }
            out.write("\t\t\"" + JSONUtils.escapeJsonString(columnName) + "\" => ");
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
        if(language == ProgramLanguages.PHP_VERSION_LESS_5_and_4) {
            out.write("\t)");
        } else {
            out.write("\t]");
        }

    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) throws DBException, IOException {
        PrintWriter out = getWriter();
        if(language == ProgramLanguages.PHP_VERSION_LESS_5_and_4) {
            out.write("\n);");
        } else {
            out.write("\n];");
        }
        out.write("\n?>");
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
