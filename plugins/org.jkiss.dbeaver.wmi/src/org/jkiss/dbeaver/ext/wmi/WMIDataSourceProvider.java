/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.wmi.model.WMIDataSource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.Collection;

public class WMIDataSourceProvider implements DBPDataSourceProvider {

    static final Log log = LogFactory.getLog(WMIDataSourceProvider.class);

    public void init(DBPApplication application)
    {

    }

    public long getFeatures()
    {
        return 0;
    }

    public Collection<IPropertyDescriptor> getConnectionProperties(DBPDriver driver, DBPConnectionInfo connectionInfo) throws DBException
    {
        return null;
    }

    public DBPDataSource openDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container) throws DBException
    {
        return new WMIDataSource(monitor, container);
    }

    public void close()
    {

    }

}
