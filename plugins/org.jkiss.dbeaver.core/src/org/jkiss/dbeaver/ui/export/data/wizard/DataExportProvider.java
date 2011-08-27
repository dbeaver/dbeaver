/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.data.wizard;

import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

/**
 * Data export source
 */
public class DataExportProvider {

    private DBSDataContainer dataContainer;
    private DBDDataFilter dataFilter;

    public DataExportProvider(DBSDataContainer dataContainer)
    {
        this.dataContainer = dataContainer;
    }

    public DataExportProvider(DBSDataContainer dataContainer, DBDDataFilter dataFilter)
    {
        this.dataContainer = dataContainer;
        this.dataFilter = dataFilter;
    }

    public DBSDataContainer getDataContainer()
    {
        return dataContainer;
    }

    public DBDDataFilter getDataFilter()
    {
        return dataFilter;
    }
}
