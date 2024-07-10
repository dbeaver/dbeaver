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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.Base64;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Locale;

/**
 * DbUnit Dataset Exporter
 * 
 * DbUnit is a framework for populating a database with test data before
 * running an integration test. This export uses the format used by FlatXmlDataSet/ReplacementDataSet
 * described at http://dbunit.sourceforge.net/components.html
 */
public class DataExporterDbUnit extends StreamExporterAbstract {

    private static final String PROP_UPPER_CASE_TABLE_NAME = "upperCaseTableName";
    private static final String PROP_NULL_VALUE_STRING = "nullValueString";
    private static final String PROP_UPPER_CASE_COLUMN_NAMES = "upperCaseColumnNames";
    private static final String PROP_INCLUDE_NULL_VALUES = "includeNullValues";

    private DBDAttributeBinding[] columns;
    private String tableName;
    private boolean upperCaseTableName;
    private boolean upperCaseColumnNames;
    private boolean includeNullValues;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        upperCaseTableName = CommonUtils.getBoolean(site.getProperties().get(PROP_UPPER_CASE_TABLE_NAME), true);
        upperCaseColumnNames = CommonUtils.getBoolean(site.getProperties().get(PROP_UPPER_CASE_COLUMN_NAMES), true);
        includeNullValues = CommonUtils.getBoolean(site.getProperties().get(PROP_INCLUDE_NULL_VALUES), true);
    }

    @Override
    public void dispose()
    {
        tableName = null;
        columns = null;
        super.dispose();
    }

    private String getTableName()
    {
        DBPNamedObject sourceObject = getSite().getSource();
        String result = sourceObject.getName();
        result = CommonUtils.escapeIdentifier(result);
        if (upperCaseTableName) {
            result = result.toUpperCase();
        }
        return result;
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException
    {
        PrintWriter out = getWriter();
        columns = getSite().getAttributes();
        tableName = getTableName();
        String outputEncoding = getSite().getOutputEncoding();
        out.append("<?xml version=\"1.0\" encoding=\"").append(outputEncoding).append("\"?>").append(CommonUtils.getLineSeparator());
        out.append("<dataset>").append(CommonUtils.getLineSeparator());
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException
    {
        PrintWriter out = getWriter();
        out.write("    <" + tableName);
        for (int i = 0; i < row.length; i++) {
            if (DBUtils.isNullValue(row[i]) && !includeNullValues) {
                continue;
            }
            DBDAttributeBinding column = columns[i];
            String columnName = escapeXmlElementName(column.getName());
            if (columnName != null && upperCaseColumnNames) {
                columnName = columnName.toUpperCase();
            }
            out.write(" " + columnName + "=\"");
            Object columnValue = row[i];
            if (DBUtils.isNullValue(columnValue)) {
                writeTextCell("" + getSite().getProperties().get(PROP_NULL_VALUE_STRING));
            } else if (columnValue instanceof Float || columnValue instanceof Double || columnValue instanceof BigDecimal) {
                int scale = column.getMetaAttribute().getScale() != null && column.getMetaAttribute().getScale() > 0 ? column.getMetaAttribute().getScale() : 1;
                try {
                    out.write(String.format(Locale.ROOT, "%." + scale + "f", columnValue));
                } catch (Exception e) {
                    out.write(columnValue.toString());
                }
            } else if (columnValue instanceof Boolean) {
                out.write(columnValue.toString());
            } else if (columnValue instanceof Number) {
                out.write(columnValue.toString());
            } else if (columnValue instanceof Timestamp) {
                try {
                    int nanoseconds = ((Timestamp) columnValue).getNanos();
                    out.write(String.format(Locale.ROOT, "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%2$d", columnValue, nanoseconds));
                } catch (Exception e) {
                    out.write(columnValue.toString());
                }
            } else if (columnValue instanceof Time) {
                try {
                    out.write(String.format(Locale.ROOT, "%1$tH:%1$tM:%1$tS", columnValue));
                } catch (Exception e) {
                    out.write(columnValue.toString());
                }
            } else if (columnValue instanceof Date) {
                try {
                    out.write(String.format(Locale.ROOT, "%1$tY-%1$tm-%1$td", columnValue));
                } catch (Exception e) {
                    out.write(columnValue.toString());
                }
            } else if (columnValue instanceof DBDContent) {
                // Content
                // Inline textual content and handle binaries in some special way
                DBDContent content = (DBDContent) columnValue;
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs != null) {
                        if (ContentUtils.isTextContent(content)) {
                            try (Reader reader = cs.getContentReader()) {
                                writeCellValue(reader);
                            }
                        } else {
                            try (final InputStream stream = cs.getContentStream()) {
                                Base64.encode(stream, cs.getContentLength(), getSite().getWriter());
                            }
                        }
                    }
                }
                finally {
                    DTUtils.closeContents(resultSet, content);
                }
            } else {
                writeTextCell(super.getValueDisplayString(column, columnValue));
            }
            out.write("\"");
        }
        out.write("/>" + CommonUtils.getLineSeparator());
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) {
        getWriter().write("</dataset>\n");
    }

    private void writeTextCell(@Nullable String value)
    {
        if (value != null) {
            value = value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
            getWriter().write(value);
        }
    }

    private void writeCellValue(Reader reader) throws IOException
    {
        PrintWriter out = getWriter();
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
