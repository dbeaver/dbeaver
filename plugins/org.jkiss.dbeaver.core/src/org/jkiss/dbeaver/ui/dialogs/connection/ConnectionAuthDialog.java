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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.utils.CommonUtils;

/**
 * Connection auth dialog
 */
public class ConnectionAuthDialog extends BaseAuthDialog
{
    private DataSourceDescriptor dataSource;
    private DBWHandlerConfiguration networkHandler;

    public ConnectionAuthDialog(@NotNull Shell parentShell, @NotNull DataSourceDescriptor dataSource, @Nullable DBWHandlerConfiguration networkHandler)
    {
        super(parentShell,
            networkHandler != null ?
                    NLS.bind(CoreMessages.dialog_connection_auth_title_for_handler, networkHandler.getTitle()) :
                    "'" + dataSource.getName() + CoreMessages.dialog_connection_auth_title, //$NON-NLS-1$
            dataSource.getDriver().getIcon());

        this.dataSource = dataSource;
        this.networkHandler = networkHandler;

        if (networkHandler != null) {
            setUserName(CommonUtils.notEmpty(networkHandler.getUserName()));
            setUserPassword(CommonUtils.notEmpty(networkHandler.getPassword()));
        } else {
            setUserName(CommonUtils.notEmpty(dataSource.getConnectionInfo().getUserName()));
            setUserPassword(CommonUtils.notEmpty(dataSource.getConnectionInfo().getUserPassword()));
        }
    }

    @Override
    protected void okPressed() {
        super.okPressed();

        if (networkHandler != null) {
            networkHandler.setUserName(getUserName());
            networkHandler.setPassword(getUserPassword());
            networkHandler.setSavePassword(isSavePassword());
        } else {
            dataSource.getConnectionInfo().setUserName(getUserName());
            dataSource.getConnectionInfo().setUserPassword(getUserPassword());
            dataSource.setSavePassword(isSavePassword());
        }
    }

}
