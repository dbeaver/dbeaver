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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.ui.controls.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.properties.PropertySourceCustom;

/**
 * DriverPropertiesDialogPage
 */
public class DriverPropertiesDialogPage extends ConnectionPageAbstract
{

    private ConnectionPageAbstract hostPage;
    private ConnectionPropertiesControl propsControl;
    private PropertySourceCustom propertySource;

    private DBPConnectionInfo prevConnectionInfo = null;

    public DriverPropertiesDialogPage(ConnectionPageAbstract hostPage)
    {
        this.hostPage = hostPage;
        setTitle("Driver properties");
        setDescription("JDBC driver properties");
    }

    @Override
    public boolean isComplete()
    {
        return true;
    }

    @Override
    public void loadSettings()
    {
        // Set props model
        if (propsControl != null) {
            refreshDriverProperties();
        }
    }

    protected void refreshDriverProperties()
    {
        if (prevConnectionInfo == site.getConnectionInfo()) {
            return;
        }
        DBPConnectionInfo tmpConnectionInfo = new DBPConnectionInfo();
        hostPage.saveSettings(tmpConnectionInfo);
        tmpConnectionInfo.getProperties().putAll(site.getConnectionInfo().getProperties());
        propertySource = propsControl.makeProperties(
            site.getRunnableContext(),
            site.getDriver(),
            tmpConnectionInfo/*.getUrl(), site.getConnectionInfo().getProperties()*/);
        propsControl.loadProperties(propertySource);
        prevConnectionInfo = site.getConnectionInfo();
    }

    @Override
    protected void saveSettings(DBPConnectionInfo connectionInfo)
    {
        if (connectionInfo != null) {
            if (propertySource != null) {
                connectionInfo.getProperties().putAll(propertySource.getProperties());
            }
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        propsControl = new ConnectionPropertiesControl(parent, SWT.NONE);
        setControl(propsControl.getControl());
    }

}
