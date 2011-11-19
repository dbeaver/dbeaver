/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
import org.jkiss.dbeaver.ui.help.IHelpContextIds;
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
        setDescription("Driver specific settings.");
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

    public void activatePage()
    {
        setMessage(NLS.bind(CoreMessages.dialog_connection_message, getDriver().getName()));

        if (this.connectionEditor != null) {
            this.connectionEditor.loadSettings();
        }
        //this.editor.loadSettings();
    }

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

    public boolean canFlipToNextPage()
    {
        return true;
    }

    public boolean isPageComplete()
    {
        if (wizard.getPageSettings() != this) {
            return true;
        }
        return this.connectionEditor != null && this.connectionEditor.isComplete();
    }

    public DriverDescriptor getDriver()
    {
        return wizard.getSelectedDriver();
    }

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

    public void updateButtons()
    {
        getWizard().getContainer().updateButtons();
    }

    public void updateMessage()
    {
        getWizard().getContainer().updateMessage();
        getWizard().getContainer().updateTitleBar();
    }

    public void testConnection()
    {
        if (this.connectionEditor != null) {
            this.connectionEditor.saveSettings();
            this.wizard.testConnection(getConnectionInfo());
        }
    }

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
        super.dispose();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
