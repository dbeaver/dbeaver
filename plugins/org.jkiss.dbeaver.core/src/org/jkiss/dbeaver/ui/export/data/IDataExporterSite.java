/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.data;

import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * IDataExporter
 */
public interface IDataExporterSite {

    DBPNamedObject getSource();

    Map<String, String> getProperties();

    List<DBDColumnBinding> getColumns();

    OutputStream getOutputStream();

    PrintWriter getWriter();

    void flush() throws IOException;

    void writeBinaryData(InputStream stream, long streamLength) throws IOException;

}