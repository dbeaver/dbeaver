/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ConnectionPageAbstract
 */
public abstract class ConnectionPageAbstract extends DialogPage implements IDataSourceConnectionEditor
{
    protected IDataSourceConnectionEditorSite site;

    private Font boldFont;
    private Button tunnelButton;
    private Button eventsButton;

    public IDataSourceConnectionEditorSite getSite() {
        return site;
    }

    protected void createAdvancedButtons(Composite parent, boolean makeDiv)
    {
        if (site.getDataSourceContainer() != null) {
            return;
        }
        boldFont = UIUtils.makeBoldFont(parent.getFont());
        if (makeDiv) {
            Label divLabel = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = ((GridLayout)parent.getLayout()).numColumns;
            divLabel.setLayoutData(gd);
        }
        {

            //Composite buttonsGroup = UIUtils.createPlaceholder(group, 3);
            Composite buttonsGroup = new Composite(parent, SWT.NONE);
            GridLayout gl = new GridLayout(2, true);
            gl.verticalSpacing = 0;
            gl.horizontalSpacing = 10;
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            buttonsGroup.setLayout(gl);

            //buttonsGroup.setLayout(new GridLayout(2, true));
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            if (makeDiv) {
                gd.horizontalSpan = ((GridLayout)parent.getLayout()).numColumns;
            }
            buttonsGroup.setLayoutData(gd);

            tunnelButton = new Button(buttonsGroup, SWT.PUSH);
            tunnelButton.setText("SSH Tunnel");
            tunnelButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            tunnelButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    configureTunnels();
                }
            });

            eventsButton = new Button(buttonsGroup, SWT.PUSH);
            eventsButton.setText("Connection Events");
            eventsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            eventsButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    configureEvents();
                }
            });
        }
    }

    @Override
    public void dispose() {
        if (boldFont != null) {
            UIUtils.dispose(boldFont);
        }
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
    public void loadSettings()
    {
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        if (tunnelButton != null) {
            tunnelButton.setFont(getFont());
            for (DBWHandlerConfiguration config : connectionInfo.getDeclaredHandlers()) {
                if (config.isEnabled()) {
                    tunnelButton.setFont(boldFont);
                    break;
                }
            }
        }
        if (eventsButton != null) {
            eventsButton.setFont(getFont());
            for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                if (connectionInfo.getEvent(eventType).isEnabled()) {
                    eventsButton.setFont(boldFont);
                    break;
                }
            }
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

    private void configureEvents()
    {
        DBPConnectionInfo connectionInfo = site.getConnectionInfo();
        EditShellEventsDialog dialog = new EditShellEventsDialog(
                getShell(),
                connectionInfo);
        if (dialog.open() == IDialogConstants.OK_ID) {
            eventsButton.setFont(getFont());
            for (DBPConnectionEventType eventType : connectionInfo.getDeclaredEvents()) {
                if (connectionInfo.getEvent(eventType).isEnabled()) {
                    eventsButton.setFont(boldFont);
                    break;
                }
            }
        }
    }

    private void configureTunnels()
    {
        EditNetworkDialog dialog = new EditNetworkDialog(
                getShell(),
                site.getDriver(),
                site.getConnectionInfo());
        if (dialog.open() == IDialogConstants.OK_ID) {
            tunnelButton.setFont(getFont());
            for (DBWHandlerConfiguration config : site.getConnectionInfo().getDeclaredHandlers()) {
                if (config.isEnabled()) {
                    tunnelButton.setFont(boldFont);
                    break;
                }
            }
        }
    }

}
