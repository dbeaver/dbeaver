/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

/**
 * IDataExporter
 */
public interface IDataExporter {

    void init(IDataExporterSite site)
        throws DBException;

    void exportHeader(DBRProgressMonitor monitor)
        throws DBException, IOException;

    void exportRow(DBRProgressMonitor monitor, Object[] row)
        throws DBException, IOException;

    void exportFooter(DBRProgressMonitor monitor)
        throws DBException, IOException;

    void dispose();

}
