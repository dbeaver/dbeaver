/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export;

import org.jkiss.dbeaver.DBException;

import java.util.List;

/**
 * IDataExporter
 */
public interface IDataExporter {

    void init(IDataExporterSite site)
        throws DBException;

    void exportHeader()
        throws DBException;

    void exportRow(List<String> rows)
        throws DBException;

    void exportFooter()
        throws DBException;

    void dispose();

}
