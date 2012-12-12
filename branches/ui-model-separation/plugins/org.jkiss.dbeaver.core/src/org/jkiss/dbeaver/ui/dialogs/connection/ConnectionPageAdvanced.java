/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
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

    @Override
    public void setSite(IDataSourceConnectionEditorSite site)
    {
        this.site = site;
    }

    protected boolean isCustomURL()
    {
        return false;
    }

    @Override
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

    @Override
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
            saveConnectionURL(connectionInfo);
        }
    }

    protected void saveConnectionURL(DBPConnectionInfo connectionInfo)
    {
        if (!isCustomURL()) {
            try {
                connectionInfo.setUrl(
                    site.getDriver().getDataSourceProvider().getConnectionURL(
                        site.getDriver(),
                        connectionInfo));
            } catch (DBException e) {
                setErrorMessage(e.getMessage());
            }
        }
    }

}
