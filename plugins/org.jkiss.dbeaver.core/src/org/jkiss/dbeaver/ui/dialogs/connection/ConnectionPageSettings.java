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

import org.jkiss.dbeaver.core.Log;
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditor;
import org.jkiss.dbeaver.ext.ui.IDataSourceConnectionEditorSite;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Settings connection page. Hosts particular drivers' connection pages
 */
class ConnectionPageSettings extends ActiveWizardPage<ConnectionWizard> implements IDataSourceConnectionEditorSite, ICompositeDialogPage
{
    static final Log log = Log.getLog(DriverDescriptor.class);

    @NotNull
    private final ConnectionWizard wizard;
    @NotNull
    private DataSourceViewDescriptor viewDescriptor;
    @Nullable
    private IDataSourceConnectionEditor connectionEditor;
    @Nullable
    private DataSourceDescriptor dataSource;
    private final Map<DriverDescriptor, DataSourceDescriptor> infoMap = new HashMap<DriverDescriptor, DataSourceDescriptor>();
    private final Set<DataSourceDescriptor> activated = new HashSet<DataSourceDescriptor>();
    private IDialogPage[] subPages;

    /**
     * Constructor for ConnectionPageSettings
     */
    ConnectionPageSettings(
        @NotNull ConnectionWizard wizard,
        @NotNull DataSourceViewDescriptor viewDescriptor)
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
        @NotNull ConnectionWizard wizard,
        @NotNull DataSourceViewDescriptor viewDescriptor,
        @Nullable DataSourceDescriptor dataSource)
    {
        this(wizard, viewDescriptor);
        this.dataSource = dataSource;
    }

    @Override
    public void activatePage()
    {
        setMessage(NLS.bind(CoreMessages.dialog_connection_message, getDriver().getFullName()));
        DataSourceDescriptor connectionInfo = getActiveDataSource();
        if (!activated.contains(connectionInfo)) {
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
            activated.add(connectionInfo);
        }
        getContainer().updateTitleBar();
    }

    @Override
    public Image getImage() {
        if (this.connectionEditor != null) {
            Image image = this.connectionEditor.getImage();
            if (image != null) {
                return image;
            }
        }
        return super.getImage();
    }

    void saveSettings(DataSourceDescriptor dataSource)
    {
        getActiveDataSource().getConnectionInfo().getProperties().clear();
        if (connectionEditor != null) {
            connectionEditor.saveSettings(dataSource);
        }
        if (subPages != null) {
            for (IDialogPage page : subPages) {
                if (page instanceof IDataSourceConnectionEditor) {
                    ((IDataSourceConnectionEditor) page).saveSettings(dataSource);
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

            if (wizard.isNew() && !ArrayUtils.isEmpty(subPages)) {
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
        return wizard.getPageSettings() != this ||
            this.connectionEditor != null && this.connectionEditor.isComplete();
    }

    @Override
    public IRunnableContext getRunnableContext()
    {
        return wizard.getContainer();
    }

    @Override
    public DataSourceRegistry getDataSourceRegistry() {
        return wizard.getDataSourceRegistry();
    }

    @Override
    public boolean isNew() {
        return wizard.isNew();
    }

    @Override
    public DriverDescriptor getDriver()
    {
        return wizard.getSelectedDriver();
    }

    @NotNull
    @Override
    public DataSourceDescriptor getActiveDataSource()
    {
        if (dataSource != null) {
            return dataSource;
        }
        DriverDescriptor driver = getDriver();
        DataSourceDescriptor info = infoMap.get(driver);
        if (info == null) {
            DBPConnectionInfo connectionInfo = new DBPConnectionInfo();
            info = new DataSourceDescriptor(
                wizard.getDataSourceRegistry(),
                DataSourceDescriptor.generateNewId(wizard.getSelectedDriver()),
                wizard.getSelectedDriver(),
                connectionInfo);
            info.getConnectionInfo().setClientHomeId(driver.getDefaultClientHomeId());
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
        // Dispose all temp data sources
        for (DataSourceDescriptor dataSource : infoMap.values()) {
            dataSource.dispose();
        }
        super.dispose();
    }

    @Nullable
    @Override
    public IDialogPage[] getSubPages()
    {
        if (subPages != null) {
            return subPages;
        }
        if (connectionEditor instanceof ICompositeDialogPage) {
            subPages = ((ICompositeDialogPage) connectionEditor).getSubPages();
            if (!ArrayUtils.isEmpty(subPages)) {
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
