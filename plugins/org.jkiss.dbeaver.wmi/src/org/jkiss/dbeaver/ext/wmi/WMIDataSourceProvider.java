/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.wmi.model.WMIDataSource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.Collection;

public class WMIDataSourceProvider implements DBPDataSourceProvider {


    private boolean libLoaded = false;

    public void init(DBPApplication application)
    {
    }

    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    public Collection<IPropertyDescriptor> getConnectionProperties(DBPDriver driver, DBPConnectionInfo connectionInfo) throws DBException
    {
        return null;
    }

    public DBPDataSource openDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container) throws DBException
    {
        if (!libLoaded) {
            loadNativeLib(container.getDriver());
            libLoaded = true;
        }
        return new WMIDataSource(container);
    }

    private void loadNativeLib(DBPDriver driver) throws DBException
    {
/*
        String arch = System.getProperty("os.arch");
        String libName = (arch != null && arch.indexOf("64") != -1) ?
            "jkiss_wmi_x86_64" : "jkiss_wmi_x86";
        try {
            System.loadLibrary(libName);
        } catch (UnsatisfiedLinkError e) {
            throw new DBException("Can't load native library '" + libName + "'", e);
        }
*/
    }

    public void close()
    {
        //WMIService.unInitializeThread();
    }

}
