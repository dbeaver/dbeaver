/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.ui.export.IDataExporterSite;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * CSV Exporter
 */
public class DataExporterCSV extends DataExporterAbstract {

    private static final String PROP_DELIMITER = "delimiter";

    private String delimiter;
    private String rowDelimiter;
    private PrintWriter out;
    private List<DBDColumnBinding> columns;

    @Override
    public void init(IDataExporterSite site) throws DBException
    {
        super.init(site);
        delimiter = String.valueOf(site.getProperties().get(PROP_DELIMITER));
        out = site.getWriter();
        rowDelimiter = System.getProperty("line.separator");
    }

    @Override
    public void dispose()
    {
        delimiter = null;
        out = null;
        super.dispose();
    }

    public void exportHeader() throws DBException, IOException
    {
        columns = getSite().getColumns();
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            DBDColumnBinding column = columns.get(i);
            writeCellValue(column.getColumn().getName());
            if (i < columnsSize - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

    public void exportRow(Object[] row) throws DBException, IOException
    {
        for (int i = 0; i < row.length; i++) {
            DBDColumnBinding column = columns.get(i);
            String stringValue = column.getValueHandler().getValueDisplayString(column.getColumn(), row[i]);
            writeCellValue(stringValue);
            if (i < row.length - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

    public void exportFooter() throws DBException, IOException
    {

    }

    private void writeCellValue(String value)
    {
        out.write(value);
    }

    private void writeDelimiter()
    {
        out.write(delimiter);
    }

    private void writeRowLimit()
    {
        out.write(rowDelimiter);
    }

}
