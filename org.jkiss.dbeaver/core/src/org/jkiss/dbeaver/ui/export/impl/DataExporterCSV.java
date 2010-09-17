/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.ui.export.IDataExporterSite;

import java.io.IOException;
import java.util.List;

/**
 * CSV Exporter
 */
public class DataExporterCSV extends DataExporterAbstract {

    @Override
    public void init(IDataExporterSite site) throws DBException
    {
        super.init(site);
        //site.getProperties().get("")
    }

    public void exportHeader() throws DBException, IOException
    {
        List<DBDColumnBinding> columns = getSite().getColumns();
        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            DBDColumnBinding column = columns.get(i);
            writeCellValue(column.getColumn().getName());
            if (i < columnsSize - 1) {
                writeDelimiter();
            }
        }
    }

    public void exportRow(Object[] row) throws DBException, IOException
    {

    }

    public void exportFooter() throws DBException, IOException
    {

    }

    private void writeCellValue(String value)
    {

    }

    private void writeDelimiter()
    {
        //getSite().getWriter().print(getSite().get);
    }

}
