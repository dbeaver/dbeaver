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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.ui.ICompositeDialogPage;
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
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

class ConnectionPageSettings extends ActiveWizardPage<ConnectionWizard> implements IDataSourceConnectionEditorSite, ICompositeDialogPage
{
    static final Log log = LogFactory.getLog(DriverDescriptor.class);

    private ConnectionWizard wizard;
    private DataSourceViewDescriptor viewDescriptor;
    private IDataSourceConnectionEditor connectionEditor;
    private DataSourceDescriptor dataSource;
    private Map<DriverDescriptor, DBPConnectionInfo> infoMap = new HashMap<DriverDescriptor, DBPConnectionInfo>();
    private IDialogPage[] subPages;
    private boolean activated;

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

        setTitle(wizard.isNew() ? viewDescriptor.getLabel() : "Connection settings");
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
        if (!activated) {
            if (this.connectionEditor != null) {
                this.connectionEditor.loadSettings();
            }
            if (subPages != null) {
                for (IDialogPage page : subPages) {
                    if (page instanceof IDataSourceConnectionEditor) {
                        ((IDataSourceConnectionEditor) page).loadSettings();
                    }
                }
            }
            activated = true;
        }
    }

    @Override
    public void deactivatePage()
    {
        saveSettings();
    }

    void saveSettings()
    {
        getConnectionInfo().getProperties().clear();
        if (connectionEditor != null) {
            connectionEditor.saveSettings();
        }
        if (subPages != null) {
            for (IDialogPage page : subPages) {
                if (page instanceof IDataSourceConnectionEditor) {
                    ((IDataSourceConnectionEditor) page).saveSettings();
                }
            }
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        try {
            this.connectionEditor = viewDescriptor.createView(IDataSourceConnectionEditor.class);
            this.connectionEditor.setSite(this);
            // init sub pages (if any)
            getSubPages();

            if (wizard.isNew() && !CommonUtils.isEmpty(subPages)) {
                // Create tab folder
                List<IDialogPage> allPages = new ArrayList<IDialogPage>();
                allPages.add(connectionEditor);
                Collections.addAll(allPages, subPages);

                TabFolder tabFolder = new TabFolder(parent, SWT.TOP);
                tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

                for (IDialogPage page : allPages) {
                    TabItem item = new TabItem(tabFolder, SWT.NONE);
                    page.createControl(tabFolder);
                    Control pageControl = page.getControl();
                    item.setControl(pageControl);
                    item.setText(CommonUtils.isEmpty(page.getTitle()) ? "General" : page.getTitle());
                    item.setToolTipText(page.getDescription());
                }
                tabFolder.setSelection(0);
                setControl(tabFolder);
            } else {
                // Create single editor control
                this.connectionEditor.createControl(parent);
                setControl(this.connectionEditor.getControl());
            }

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
    public IRunnableContext getRunnableContext()
    {
        return wizard.getContainer();
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

    @Override
    public IDialogPage[] getSubPages()
    {
        if (subPages != null) {
            return subPages;
        }
        if (connectionEditor instanceof ICompositeDialogPage) {
            subPages = ((ICompositeDialogPage) connectionEditor).getSubPages();
            if (!CommonUtils.isEmpty(subPages)) {
                for (IDialogPage page : subPages) {
                    if (page instanceof IDataSourceConnectionEditor) {
                        ((IDataSourceConnectionEditor) page).setSite(this);
                    }
                }
            }
            return subPages;
        } else {
            return null;
        }
    }

}
