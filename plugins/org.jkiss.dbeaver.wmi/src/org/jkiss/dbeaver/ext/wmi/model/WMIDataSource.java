/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSEntitySelector;

/**
 * GenericDataSource
 */
public class WMIDataSource extends WMINamespace implements DBPDataSource,
    DBSEntityContainer, DBSEntitySelector, IAdaptable
{
    static final Log log = LogFactory.getLog(WMIDataSource.class);

    private DBSDataSourceContainer container;

    public WMIDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        this.container = container;
    }

}
