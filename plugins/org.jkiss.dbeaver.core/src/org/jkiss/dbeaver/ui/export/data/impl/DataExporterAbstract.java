/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.data.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandler2;
import org.jkiss.dbeaver.ui.export.data.IDataExporter;
import org.jkiss.dbeaver.ui.export.data.IDataExporterSite;

/**
 * Abstract Exporter
 */
public abstract class DataExporterAbstract implements IDataExporter {

    private IDataExporterSite site;

    public IDataExporterSite getSite()
    {
        return site;
    }

    @Override
    public void init(IDataExporterSite site) throws DBException
    {
        this.site = site;
    }

    @Override
    public void dispose()
    {
        // do nothing
    }

    protected String getValueDisplayString(
        DBDColumnBinding column,
        Object value)
    {
        final DBDValueHandler valueHandler = column.getValueHandler();
        if (valueHandler instanceof DBDValueHandler2) {
            return ((DBDValueHandler2)valueHandler).getValueDisplayString(column.getColumn(), getSite().getExportFormat(), value);
        } else {
            return valueHandler.getValueDisplayString(column.getColumn(), value);
        }
    }


}