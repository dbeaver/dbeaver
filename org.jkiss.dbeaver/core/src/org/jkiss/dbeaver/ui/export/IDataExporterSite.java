/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export;

import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * IDataExporter
 */
public interface IDataExporterSite {

    DBPNamedObject getSource();

    Map<String, Object> getProperties();

    List<DBCColumnMetaData> getColumns();

    OutputStream getOutputStream();

    PrintWriter getWriter();

}