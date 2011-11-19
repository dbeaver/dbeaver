/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;

/**
 * ConnectionPageAdvanced
 */
public abstract class ConnectionPageAdvanced extends DialogPage implements IDataSourceConnectionEditor
{
    protected IDataSourceConnectionEditorSite site;

    private boolean driverPropsLoaded;
    private ConnectionPropertiesControl propsControl;
    private PropertySourceCustom propertySource;

    protected Composite createPropertiesTab(Composite parent)
    {
        final Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        propsControl = new ConnectionPropertiesControl(placeholder, SWT.NONE);
        return placeholder;
    }

    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    public void loadSettings()
    {
        // Load values from new connection info
        driverPropsLoaded = false;

        // Set props model
        if (propsControl != null) {
            refreshDriverProperties();
        }
    }

    protected void refreshDriverProperties()
    {
        if (!driverPropsLoaded) {
            DBPConnectionInfo tmpConnectionInfo = new DBPConnectionInfo();
            saveSettings(tmpConnectionInfo);
            tmpConnectionInfo.setProperties(site.getConnectionInfo().getProperties());
            propertySource = propsControl.makeProperties(site.getDriver(), tmpConnectionInfo/*.getUrl(), site.getConnectionInfo().getProperties()*/);
            propsControl.loadProperties(propertySource);
            driverPropsLoaded = true;
        }
    }

    public void saveSettings()
    {
        saveSettings(site.getConnectionInfo());
    }

    protected void saveSettings(DBPConnectionInfo connectionInfo)
    {
        if (connectionInfo != null) {
            if (propertySource != null) {
                connectionInfo.setProperties(propertySource.getProperties());
            }
        }
    }

}
