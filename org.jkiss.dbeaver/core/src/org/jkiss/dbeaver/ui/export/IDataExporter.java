/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export;

import org.jkiss.dbeaver.DBException;

import java.io.IOException;
import java.util.List;

/**
 * IDataExporter
 */
public interface IDataExporter {

    void init(IDataExporterSite site)
        throws DBException;

    void exportHeader()
        throws DBException, IOException;

    void exportRow(Object[] row)
        throws DBException, IOException;

    void exportFooter()
        throws DBException, IOException;

    void dispose();

}
