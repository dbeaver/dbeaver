/*
 * Copyright (C) 2010-2015 Serge Rieder
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;

/**
 * ConnectionPageAbstract
 */
public abstract class ConnectionPageAbstract extends DialogPage implements IDataSourceConnectionEditor
{
    protected IDataSourceConnectionEditorSite site;

    public IDataSourceConnectionEditorSite getSite() {
        return site;
    }

    @Override
    public void dispose() {
        super.dispose();
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
    public void saveSettings(DataSourceDescriptor dataSource)
    {
        saveConnectionURL(dataSource.getConnectionInfo());
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
