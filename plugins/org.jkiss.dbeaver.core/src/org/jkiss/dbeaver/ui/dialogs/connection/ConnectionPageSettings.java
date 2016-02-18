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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.ui.*;
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
    private final Set<DataSourceDescriptor> activated = new HashSet<>();
    private IDialogPage[] subPages, extraPages;

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
        if (connectionEditor == null) {
            createProviderPage(getControl().getParent());
        }
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
        } else if (connectionEditor != null) {
            connectionEditor.loadSettings();
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
        getActiveDataSource().getConnectionConfiguration().getProperties().clear();
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
        if (wizard.isNew()) {
            setControl(new Composite(parent, SWT.BORDER));
        } else {
            createProviderPage(parent);
        }
    }

    private void createProviderPage(Composite parent) {
        if (this.connectionEditor != null) {
            return;
        }
        if (getControl() != null) {
            getControl().dispose();
        }

        try {
            this.connectionEditor = viewDescriptor.createView(IDataSourceConnectionEditor.class);
            this.connectionEditor.setSite(this);
            // init sub pages (if any)
            getSubPages();

            if (wizard.isNew() && !ArrayUtils.isEmpty(subPages)) {
                // Create tab folder
                List<IDialogPage> allPages = new ArrayList<>();
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
        }
        parent.layout();
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
    public DBRRunnableContext getRunnableContext()
    {
        return new RunnableContextDelegate(wizard.getContainer());
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
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
        return wizard.getActiveDataSource();
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
            if (extraPages != null) {
                subPages = ArrayUtils.concatArrays(subPages, extraPages);
            }
            return subPages;
        } else {
            return null;
        }
    }

    public void addSubPage(IDialogPage page) {
        if (extraPages == null) {
            extraPages = new IDialogPage[] { page };
        } else {
            extraPages = ArrayUtils.concatArrays(extraPages, new IDialogPage[] { page });
        }
    }
}
