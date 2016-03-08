/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * ConnectionPageAbstract
 */

public abstract class ConnectionPageAbstract extends DialogPage implements IDataSourceConnectionEditor
{
    protected IDataSourceConnectionEditorSite site;
    // Driver name
    protected Text driverText;

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
    public void loadSettings() {
        DBPDriver driver = site.getDriver();
        if (driver != null) {
            driverText.setText(CommonUtils.toString(driver.getFullName()));
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        saveConnectionURL(dataSource.getConnectionConfiguration());
    }

    protected void saveConnectionURL(DBPConnectionConfiguration connectionInfo)
    {
        if (!isCustomURL()) {
            connectionInfo.setUrl(
                site.getDriver().getDataSourceProvider().getConnectionURL(
                    site.getDriver(),
                    connectionInfo));
        }
    }

    protected void createDriverPanel(Composite parent) {
        int numColumns = ((GridLayout) parent.getLayout()).numColumns;

        Composite panel = UIUtils.createPlaceholder(parent, 4, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = numColumns;
        panel.setLayoutData(gd);

        Composite placeholder = UIUtils.createPlaceholder(panel, 1);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_END);
        gd.horizontalSpan = 4;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        placeholder.setLayoutData(gd);

        if (!site.isNew() && !site.getDriver().isEmbedded()) {
            Link netConfigLink = new Link(panel, SWT.NONE);
            netConfigLink.setText("<a>Network settings (SSH, SSL, Proxy, ...)</a>");
            netConfigLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    site.openSettingsPage(ConnectionPageNetwork.PAGE_NAME);
                }
            });
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            gd.horizontalSpan = 4;
            netConfigLink.setLayoutData(gd);
        }

        Label divLabel = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 4;
        divLabel.setLayoutData(gd);

        Label driverLabel = new Label(panel, SWT.NONE);
        driverLabel.setText(CoreMessages.dialog_connection_driver);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        driverLabel.setLayoutData(gd);

        driverText = new Text(panel, SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        //gd.widthHint = 200;
        driverText.setLayoutData(gd);

        Button driverButton = new Button(panel, SWT.PUSH);
        driverButton.setText(CoreMessages.dialog_connection_edit_driver_button);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        driverButton.setLayoutData(gd);
        driverButton.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (site.openDriverEditor()) {
                    updateDriverInfo(site.getDriver());
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
    }

    protected void updateDriverInfo(DBPDriver driver) {

    }

}
