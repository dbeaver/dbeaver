/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;

/**
 * DBD Value Controller
 */
public interface DBDColumnController extends DBDValueController
{

    /**
     * Row controller
     * @return row controller
     */
    DBDRowController getRow();

    /**
     * Column meta data
     * @return meta data
     */
    DBCColumnMetaData getColumnMetaData();

    /**
     * Column unique ID string
     * @return string
     */
    String getColumnId();

    DBDValueLocator getValueLocator();

}
