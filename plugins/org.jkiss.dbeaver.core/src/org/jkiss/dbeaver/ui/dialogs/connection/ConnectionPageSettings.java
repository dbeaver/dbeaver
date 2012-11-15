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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;

import java.util.HashMap;
import java.util.Map;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

class ConnectionPageSettings extends ActiveWizardPage implements IDataSourceConnectionEditorSite
{
    static final Log log = LogFactory.getLog(DriverDescriptor.class);

    private ConnectionWizard wizard;
    private DataSourceViewDescriptor viewDescriptor;
    private IDataSourceConnectionEditor connectionEditor;
    private DataSourceDescriptor dataSource;
    private Map<DriverDescriptor, DBPConnectionInfo> infoMap = new HashMap<DriverDescriptor, DBPConnectionInfo>();

    /**
     * Constructor for ConnectionPageSettings
     */
    ConnectionPageSettings(
        ConnectionWizard wizard,
        DataSourceViewDescriptor viewDescriptor)
    {
        super("newConnectionSettings");
        this.wizard = wizard;
        this.viewDescriptor = viewDescriptor;

        setTitle(viewDescriptor.getLabel());
        setDescription(CoreMessages.dialog_connection_description);
    }

    /**
     * Constructor for ConnectionPageSettings
     */
    ConnectionPageSettings(
        ConnectionWizard wizard,
        DataSourceViewDescriptor viewDescriptor,
        DataSourceDescriptor dataSource)
    {
        this(wizard, viewDescriptor);
        this.dataSource = dataSource;
    }

    @Override
    public void activatePage()
    {
        setMessage(NLS.bind(CoreMessages.dialog_connection_message, getDriver().getName()));

        if (this.connectionEditor != null) {
            this.connectionEditor.loadSettings();
        }
        //this.editor.loadSettings();
    }

    @Override
    public void deactivatePage()
    {
        if (this.connectionEditor != null) {
            this.connectionEditor.saveSettings();
        }
    }

    void saveSettings()
    {
        if (connectionEditor != null) {
            connectionEditor.saveSettings();
        }
    }
    @Override
    public void createControl(Composite parent)
    {
        try {
            this.connectionEditor = viewDescriptor.createView(IDataSourceConnectionEditor.class);
            this.connectionEditor.setSite(this);
            this.connectionEditor.createControl(parent);

            setControl(this.connectionEditor.getControl());
            final Image editorImage = this.connectionEditor.getImage();
            if (editorImage != null) {
                setImageDescriptor(ImageDescriptor.createFromImage(editorImage));
            }

            UIUtils.setHelp(getControl(), IHelpContextIds.CTX_CON_WIZARD_SETTINGS);
        }
        catch (Exception ex) {
            log.warn(ex);
            setErrorMessage("Can't create settings dialog: " + ex.getMessage());
            setControl(new Composite(parent, SWT.BORDER));
        }
    }

    @Override
    public boolean canFlipToNextPage()
    {
        return true;
    }

    @Override
    public boolean isPageComplete()
    {
        if (wizard.getPageSettings() != this) {
            return true;
        }
        return this.connectionEditor != null && this.connectionEditor.isComplete();
    }

    @Override
    public DriverDescriptor getDriver()
    {
        return wizard.getSelectedDriver();
    }

    @Override
    public DBPConnectionInfo getConnectionInfo()
    {
        if (dataSource != null) {
            return dataSource.getConnectionInfo();
        }
        DriverDescriptor driver = getDriver();
        DBPConnectionInfo info = infoMap.get(driver);
        if (info == null) {
            info = new DBPConnectionInfo();
            info.setClientHomeId(driver.getDefaultClientHomeId());
            infoMap.put(driver, info);
        }
        return info;
    }

    @Override
    public void updateButtons()
    {
        getWizard().getContainer().updateButtons();
    }

    @Override
    public void updateMessage()
    {
        getWizard().getContainer().updateMessage();
        getWizard().getContainer().updateTitleBar();
    }

    @Override
    public void testConnection()
    {
        if (this.connectionEditor != null) {
            this.connectionEditor.saveSettings();
            this.wizard.testConnection(getConnectionInfo());
        }
    }

    @Override
    public boolean openDriverEditor()
    {
        DriverEditDialog dialog = new DriverEditDialog(wizard.getShell(), this.getDriver());
        return dialog.open() == IDialogConstants.OK_ID;
    }

    @Override
    public void dispose()
    {
        if (connectionEditor != null) {
            connectionEditor.dispose();
            connectionEditor = null;
        }
        super.dispose();
    }

}
