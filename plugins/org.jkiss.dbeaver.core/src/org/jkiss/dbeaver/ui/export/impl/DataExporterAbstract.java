/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.export.IDataExporter;
import org.jkiss.dbeaver.ui.export.IDataExporterSite;

/**
 * Abstract Exporter
 */
public abstract class DataExporterAbstract implements IDataExporter {

    private IDataExporterSite site;

    public IDataExporterSite getSite()
    {
        return site;
    }

    public void init(IDataExporterSite site) throws DBException
    {
        this.site = site;
    }

    public void dispose()
    {
        // do nothing
    }
}