/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
 * Copyright (C) 2017 Adolfo Suarez  (agustavo@gmail.com)
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
package org.jkiss.dbeaver.data.office.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.impl.StreamExporterAbstract;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Export XLSX with Apache POI
 */
public class DataExporterXLSX extends StreamExporterAbstract {

    private static final Log log = Log.getLog(DataExporterXLSX.class);

    private static final String PROP_HEADER = "header";
    private static final String PROP_NULL_STRING = "nullString";

    private static final String PROP_ROWNUMBER = "rownumber";
    private static final String PROP_BORDER = "border";
    private static final String PROP_HEADER_FONT = "headerfont";

    private static final String BINARY_FIXED = "[BINARY]";

    private static final String PROP_TRUESTRING = "trueString";
    private static final String PROP_FALSESTRING = "falseString";

    private static final String PROP_EXPORT_SQL = "exportSql";
    private static final String PROP_SPLIT_SQLTEXT = "splitSqlText";

    private static final String PROP_SPLIT_BYROWCOUNT = "splitByRowCount";
    private static final String PROP_SPLIT_BYCOL = "splitByColNum";

    private static final int EXCEL2007MAXROWS = 1048575;
    private boolean showDescription;

    enum FontStyleProp {NONE, BOLD, ITALIC, STRIKEOUT, UNDERLINE}

    private static final int ROW_WINDOW = 100;

    private String nullString;

    private List<DBDAttributeBinding> columns;

    private SXSSFWorkbook wb;

    private boolean printHeader = false;
    private boolean rowNumber = false;
    private String boolTrue = "true";
    private String boolFalse = "false";
    private boolean booleRedefined;
    private boolean exportSql = false;
    private boolean splitSqlText = false;

    private int splitByRowCount = EXCEL2007MAXROWS;
    private int splitByCol = 0;
    private int rowCount = 0;

    private XSSFCellStyle style;
    private XSSFCellStyle styleHeader;

    private HashMap<Object, Worksheet> worksheets;

    public static Map<Object, Object> getDefaultProperties() {
        Map<Object, Object> properties = new HashMap<>();
        properties.put(DataExporterXLSX.PROP_ROWNUMBER, false);
        properties.put(DataExporterXLSX.PROP_BORDER, "THIN");
        properties.put(DataExporterXLSX.PROP_HEADER, true);
        properties.put(DataExporterXLSX.PROP_NULL_STRING, null);
        properties.put(DataExporterXLSX.PROP_HEADER_FONT, "BOLD");
        properties.put(DataExporterXLSX.PROP_TRUESTRING, "true");
        properties.put(DataExporterXLSX.PROP_FALSESTRING, "false");
        properties.put(DataExporterXLSX.PROP_EXPORT_SQL, false);
        properties.put(DataExporterXLSX.PROP_SPLIT_SQLTEXT, false);
        properties.put(DataExporterXLSX.PROP_SPLIT_BYROWCOUNT, EXCEL2007MAXROWS);
        properties.put(DataExporterXLSX.PROP_SPLIT_BYCOL, 0);
        return properties;
    }

    @Override
    public void init(IStreamDataExporterSite site) throws DBException {
        Object nullStringProp = site.getProperties().get(PROP_NULL_STRING);
        nullString = nullStringProp == null ? null : nullStringProp.toString();

        try {
            printHeader = (Boolean) site.getProperties().get(PROP_HEADER);
        } catch (Exception e) {
            printHeader = false;
        }

        try {
            rowNumber = (Boolean) site.getProperties().get(PROP_ROWNUMBER);
        } catch (Exception e) {
            rowNumber = false;
        }

        try {
            boolTrue = (String) site.getProperties().get(PROP_TRUESTRING);
        } catch (Exception e) {
            boolTrue = "true";
        }
        try {
            boolFalse = (String) site.getProperties().get(PROP_FALSESTRING);
        } catch (Exception e) {
            boolFalse = "false";
        }
        if (!"true".equals(boolTrue) || !"false".equals(boolFalse)) {
            booleRedefined = true;
        }

        try {
            exportSql = (Boolean) site.getProperties().get(PROP_EXPORT_SQL);
        } catch (Exception e) {
            exportSql = false;
        }

        try {
            splitSqlText = (Boolean) site.getProperties().get(PROP_SPLIT_SQLTEXT);
        } catch (Exception e) {
            splitSqlText = false;
        }

        try {
            splitByRowCount = (Integer) site.getProperties().get(PROP_SPLIT_BYROWCOUNT);
        } catch (Exception e) {
            splitByRowCount = EXCEL2007MAXROWS;
        }

        try {
            splitByCol = (Integer) site.getProperties().get(PROP_SPLIT_BYCOL);
        } catch (Exception e) {
            splitByCol = -1;
        }


        wb = new SXSSFWorkbook(ROW_WINDOW);

        worksheets = new HashMap<>(1);

        styleHeader = (XSSFCellStyle) wb.createCellStyle();


        BorderStyle border;

        try {

            border = BorderStyle.valueOf((String) site.getProperties().get(PROP_BORDER));

        } catch (Exception e) {

            border = BorderStyle.NONE;

        }

        FontStyleProp fontStyle;

        try {

            fontStyle = FontStyleProp.valueOf((String) site.getProperties().get(PROP_HEADER_FONT));

        } catch (Exception e) {

            fontStyle = FontStyleProp.NONE;

        }


        styleHeader.setBorderTop(border);
        styleHeader.setBorderBottom(border);
        styleHeader.setBorderLeft(border);
        styleHeader.setBorderRight(border);

        XSSFFont fontBold = (XSSFFont) wb.createFont();

        switch (fontStyle) {

            case BOLD:
                fontBold.setBold(true);
                break;

            case ITALIC:
                fontBold.setItalic(true);
                break;

            case STRIKEOUT:
                fontBold.setStrikeout(true);
                break;

            case UNDERLINE:
                fontBold.setUnderline((byte) 3);
                break;

            default:
                break;
        }

        styleHeader.setFont(fontBold);

        style = (XSSFCellStyle) wb.createCellStyle();
        style.setBorderTop(border);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
        this.rowCount = 0;

        super.init(site);
    }

    @Override
    public void dispose() {

        try {
            if (exportSql) {
                try {

                    Sheet sh = wb.createSheet();
                    if (splitSqlText) {
                        String[] sqlText = getSite().getSource().getName().split("\n", wb.getSpreadsheetVersion().getMaxRows());

                        int sqlRownum = 0;

                        for (String s : sqlText) {
                            Row row = sh.createRow(sqlRownum);
                            Cell newcell = row.createCell(0);
                            newcell.setCellValue(s);
                            sqlRownum++;
                        }

                    } else {
                        Row row = sh.createRow(0);
                        Cell newcell = row.createCell(0);
                        newcell.setCellValue(getSite().getSource().getName());
                    }
                    sh = null;
                } catch (Exception e) {
                    log.error("Dispose error", e);
                }
            }
            wb.write(getSite().getOutputStream());
            wb.dispose();

        } catch (IOException e) {
            log.error("Dispose error", e);
        }
        wb = null;
        for (Worksheet w : worksheets.values()) {
            w.dispose();
        }
        super.dispose();
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException {

        columns = getSite().getAttributes();
        showDescription = session.getDataSource().getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_SHOW_DESCRIPTION);
    }

    private void printHeader(DBCResultSet resultSet, Worksheet wsh) throws DBException {
        boolean hasDescription = false;
        if (showDescription) {
            // Read bindings to extract column descriptions
            boolean bindingsOk = true;
            DBDAttributeBindingMeta[] bindings = new DBDAttributeBindingMeta[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i) instanceof DBDAttributeBindingMeta) {
                    bindings[i] = (DBDAttributeBindingMeta) columns.get(i);
                } else {
                    bindingsOk = false;
                    break;
                }
            }
            if (bindingsOk) {
                DBSEntity sourceEntity = null;
                if (getSite().getSource() instanceof DBSEntity) {
                    sourceEntity = (DBSEntity) getSite().getSource();
                }
                ResultSetUtils.bindAttributes(resultSet.getSession(), sourceEntity, resultSet, bindings, null);
            }

            for (DBDAttributeBinding column : columns) {
                if (!CommonUtils.isEmpty(column.getDescription())) {
                    hasDescription = true;
                    break;
                }
            }
        }

        SXSSFSheet  sh = (SXSSFSheet)wsh.getSh();
        Row row = sh.createRow(wsh.getCurrentRow());

        int startCol = rowNumber ? 1 : 0;

        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            sh.trackColumnForAutoSizing(i);
            DBDAttributeBinding column = columns.get(i);

            String colName = column.getLabel();
            if (CommonUtils.isEmpty(colName)) {
                colName = column.getName();
            }
            Cell cell = row.createCell(i + startCol, CellType.STRING);
            cell.setCellValue(colName);
            cell.setCellStyle(styleHeader);
        }

        if (hasDescription) {
            wsh.incRow();
            Row descRow = sh.createRow(wsh.getCurrentRow());
            for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
                Cell descCell = descRow.createCell(i + startCol, CellType.STRING);
                String description = columns.get(i).getDescription();
                if (CommonUtils.isEmpty(description)) {
                    description = "";
                }
                descCell.setCellValue(description);
                descCell.setCellStyle(styleHeader);
            }
        }

        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            sh.autoSizeColumn(i);
        }

        wsh.incRow();
    }


    private void writeCellValue(Cell cell, Reader reader) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            char buffer[] = new char[2000];
            for (; ; ) {
                int count = reader.read(buffer);
                if (count <= 0) {
                    break;
                }
                sb.append(buffer, 0, count);
            }

            cell.setCellValue(sb.toString());

        } finally {
            ContentUtils.close(reader);
        }
    }


    private Worksheet createSheet(DBCResultSet resultSet, Object colValue) throws DBException {
        Worksheet w = new Worksheet(wb.createSheet(), colValue, 0);
        if (printHeader) {
            printHeader(resultSet, w);
        }
        return w;
    }

    private Worksheet getWsh(DBCResultSet resultSet, Object[] row) throws DBException {
        Object colValue = ((splitByCol <= 0) || (splitByCol >= columns.size())) ? "" : row[splitByCol];
        Worksheet w = worksheets.get(colValue);
        if (w == null) {
            w = createSheet(resultSet, colValue);
            worksheets.put(w.getColumnVal(), w);
        } else {
            if (w.getCurrentRow() >= splitByRowCount) {
                w = createSheet(resultSet, colValue);
                worksheets.put(w.getColumnVal(), w);
            }
        }
        return w;
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row)
        throws DBException, IOException {

        Worksheet wsh = getWsh(resultSet, row);

        Row rowX = wsh.getSh().createRow(wsh.getCurrentRow());

        int startCol = 0;

        if (rowNumber) {

            Cell cell = rowX.createCell(startCol, CellType.NUMERIC);
            cell.setCellStyle(style);
            cell.setCellValue(String.valueOf(wsh.getCurrentRow()));
            startCol++;
        }

        for (int i = 0; i < row.length; i++) {
            DBDAttributeBinding column = columns.get(i);
            Cell cell = rowX.createCell(i + startCol, getCellType(column));
            cell.setCellStyle(style);

            if (DBUtils.isNullValue(row[i])) {
                if (!CommonUtils.isEmpty(nullString)) {
                    cell.setCellValue(nullString);
                } else {
                    cell.setCellValue("");
                }
            } else if (row[i] instanceof DBDContent) {
                DBDContent content = (DBDContent) row[i];
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs == null) {
                        cell.setCellValue(DBConstants.NULL_VALUE_LABEL);
                    } else if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cell, cs.getContentReader());
                    } else {
                        cell.setCellValue(BINARY_FIXED);
                    }
                } finally {
                    content.release();
                }
            } else if (row[i] instanceof Boolean) {

                if (booleRedefined) {
                    cell.setCellValue((Boolean) row[i] ? boolTrue : boolFalse);
                } else {
                    cell.setCellValue((Boolean) row[i]);
                }

            } else if (row[i] instanceof Number) {

                cell.setCellValue(((Number) row[i]).doubleValue());

            } else {

                String stringValue = super.getValueDisplayString(column, row[i]);
                cell.setCellValue(stringValue);
            }

        }
        wsh.incRow();
        rowCount++;
    }

    private CellType getCellType(DBDAttributeBinding column) {
        switch (column.getDataKind()) {
            case NUMERIC:
                return CellType.NUMERIC;
            case BOOLEAN:
                return CellType.BOOLEAN;
            case STRING:
                return CellType.STRING;
            default:
                return CellType.BLANK;
        }
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor)
        throws DBException, IOException
    {
        if (rowCount == 0) {
            exportRow(null, null, new Object[columns.size()]);
        }
    }


}
