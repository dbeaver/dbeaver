/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Settings connection page. Hosts particular drivers' connection pages
 */
class ConnectionPageSettings extends ActiveWizardPage<ConnectionWizard> implements IDataSourceConnectionEditorSite, ICompositeDialogPage, IDataSourceConnectionTester {
    private static final Log log = Log.getLog(DriverDescriptor.class);

    public static final String PAGE_NAME = ConnectionPageSettings.class.getSimpleName();

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
    private TabFolder tabFolder;

    /**
     * Constructor for ConnectionPageSettings
     */
    ConnectionPageSettings(
        @NotNull ConnectionWizard wizard,
        @NotNull DataSourceViewDescriptor viewDescriptor) {
        super(PAGE_NAME + "." + viewDescriptor.getId());
        this.wizard = wizard;
        this.viewDescriptor = viewDescriptor;

        setTitle(wizard.isNew() ? viewDescriptor.getLabel() : CoreMessages.dialog_setting_connection_wizard_title);
        setDescription(CoreMessages.dialog_connection_description);
    }

    /**
     * Constructor for ConnectionPageSettings
     */
    ConnectionPageSettings(
        @NotNull ConnectionWizard wizard,
        @NotNull DataSourceViewDescriptor viewDescriptor,
        @Nullable DataSourceDescriptor dataSource) {
        this(wizard, viewDescriptor);
        this.dataSource = dataSource;
    }

    IDataSourceConnectionEditor getConnectionEditor() {
        return connectionEditor;
    }

    @Override
    public void activatePage() {
        if (connectionEditor == null) {
            createProviderPage(getControl().getParent());
            //UIUtils.resizeShell(getWizard().getContainer().getShell());
        }

        //setMessage(NLS.bind(CoreMessages.dialog_connection_message, getDriver().getFullName()));
        DataSourceDescriptor connectionInfo = getActiveDataSource();
        if (!activated.contains(connectionInfo)) {
            if (this.connectionEditor != null) {
                this.connectionEditor.loadSettings();
            }
            if (subPages != null) {
                for (IDialogPage page : subPages) {
                    Control pageControl = page.getControl();
//                    if (pageControl == null) {
//                        page.createControl(getControl().getParent());
//                    }
                    if (pageControl != null && page instanceof IDataSourceConnectionEditor) {
                        ((IDataSourceConnectionEditor) page).loadSettings();
                    }
                }
            }
            activated.add(connectionInfo);
        } else if (connectionEditor != null) {
            connectionEditor.loadSettings();
        }
        activateCurrentItem();
        //getContainer().updateTitleBar();
    }

    @Override
    public void deactivatePage() {
        DataSourceDescriptor connectionInfo = getActiveDataSource();
        if (this.activated.contains(connectionInfo) && this.connectionEditor != null) {
            this.connectionEditor.saveSettings(connectionInfo);
        }
        super.deactivatePage();
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

    void saveSettings(DataSourceDescriptor dataSource) {
        if (subPages != null) {
            for (IDialogPage page : subPages) {
                if (ArrayUtils.contains(extraPages, page)) {
                    // Ignore extra pages
                    continue;
                }

                if (page.getControl() != null && page instanceof IDataSourceConnectionEditor) {
                    ((IDataSourceConnectionEditor) page).saveSettings(dataSource);
                } else if (page instanceof ConnectionWizardPage) {
                    ((ConnectionWizardPage) page).saveSettings(dataSource);
                }
            }
        }
        // Save connection settings AFTER extra pages.
        // Because it may contain some driver properties save which will be overwrited by driver props page otherwise
        if (connectionEditor != null) {
            connectionEditor.saveSettings(dataSource);
        }
    }

    @Override
    public void createControl(Composite parent) {
        if (wizard.isNew()) {
            setControl(new Composite(parent, SWT.BORDER));
        } else {
            createProviderPage(parent);
        }
    }

    private void createProviderPage(Composite parent) {
        if (this.connectionEditor != null && this.connectionEditor.getControl() != null) {
            return;
        }
        if (getControl() != null) {
            getControl().dispose();
        }

        try {
            if (this.connectionEditor == null) {
                this.connectionEditor = viewDescriptor.createView(IDataSourceConnectionEditor.class);
                this.connectionEditor.setSite(this);
            }
            // init sub pages (if any)
            IDialogPage[] allSubPages = getSubPages(false, true);

            if (!ArrayUtils.isEmpty(allSubPages)) {
                // Create tab folder
                List<IDialogPage> allPages = new ArrayList<>();
                allPages.add(connectionEditor);
                // Add sub pages
                Collections.addAll(allPages, allSubPages);

                tabFolder = new TabFolder(parent, SWT.TOP);
                tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
                setControl(tabFolder);

                for (IDialogPage page : allPages) {
                    if (ArrayUtils.contains(extraPages, page)) {
                        // Ignore extra pages
                        continue;
                    }
                    TabItem item = new TabItem(tabFolder, SWT.NONE);
                    page.createControl(tabFolder);
                    item.setData(page);
                    Control pageControl = page.getControl();
                    item.setControl(pageControl);
                    item.setText(CommonUtils.isEmpty(page.getTitle()) ? CoreMessages.dialog_setting_connection_general : page.getTitle());
                    item.setToolTipText(page.getDescription());
                }
                tabFolder.setSelection(0);
                tabFolder.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        activateCurrentItem();
                    }
                });
            } else {
                // Create single editor control
                this.connectionEditor.createControl(parent);
                setControl(this.connectionEditor.getControl());
            }

            UIUtils.setHelp(getControl(), IHelpContextIds.CTX_CON_WIZARD_SETTINGS);
        } catch (Exception ex) {
            log.warn(ex);
            setErrorMessage("Can't create settings dialog: " + ex.getMessage());
        }
        parent.layout();
    }

    private void activateCurrentItem() {
        if (tabFolder != null) {
            TabItem[] selection = tabFolder.getSelection();
            if (selection.length == 1) {
                IDialogPage page = (IDialogPage) selection[0].getData();
                page.setVisible(true);
            }
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

    @Override
    public boolean isPageComplete() {
        return wizard.getPageSettings() != this ||
            this.connectionEditor != null && this.connectionEditor.isComplete();
    }

    @Override
    public DBRRunnableContext getRunnableContext() {
        return wizard.getRunnableContext();
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
    public DBPDriver getDriver() {
        return wizard.getSelectedDriver();
    }

    @NotNull
    @Override
    public DataSourceDescriptor getActiveDataSource() {
        if (dataSource != null) {
            return dataSource;
        }
        return wizard.getActiveDataSource();
    }

    @Override
    public void updateButtons() {
        getWizard().getContainer().updateButtons();
    }

    @Override
    public boolean openDriverEditor() {
        DriverEditDialog dialog = new DriverEditDialog(wizard.getShell(), (DriverDescriptor) this.getDriver());
        return dialog.open() == IDialogConstants.OK_ID;
    }

    @Override
    public boolean openSettingsPage(String pageId) {
        return wizard.openSettingsPage(pageId);
    }

    @Override
    public void testConnection() {
        getWizard().testConnection();
    }

    @Override
    public DBPProject getProject() {
        return wizard.getDataSourceRegistry().getProject();
    }

    @Override
    public void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
        PropertyChangeEvent pcEvent = new PropertyChangeEvent(source, property, oldValue, newValue);
        for (TabItem item : tabFolder.getItems()) {
            IDialogPage page = (IDialogPage) item.getData();
            if (page instanceof IPropertyChangeListener) {
                ((IPropertyChangeListener) page).propertyChange(pcEvent);
            }
        }
        for (IWizardPage page : getWizard().getPages()) {
            if (page instanceof IPropertyChangeListener) {
                ((IPropertyChangeListener) page).propertyChange(pcEvent);
            }
        }
    }

    @Override
    public void dispose() {
        if (connectionEditor != null) {
            connectionEditor.dispose();
            connectionEditor = null;
        }
        super.dispose();
    }

    @Nullable
    @Override
    public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate) {
        if (extrasOnly) {
            return extraPages;
        }
        if (subPages != null) {
            return subPages;
        }
        if (!forceCreate) {
            return new IDialogPage[0];
        }
        if (this.connectionEditor == null) {
            this.connectionEditor = viewDescriptor.createView(IDataSourceConnectionEditor.class);
            this.connectionEditor.setSite(this);
        }

        if (connectionEditor instanceof ICompositeDialogPage) {
            subPages = ((ICompositeDialogPage) connectionEditor).getSubPages(extrasOnly, true);
            if (!ArrayUtils.isEmpty(subPages)) {
                for (IDialogPage page : subPages) {
                    if (page instanceof IDataSourceConnectionEditor) {
                        ((IDataSourceConnectionEditor) page).setSite(this);
                    }
                }
            }
            if (isNew() || !getDriver().isEmbedded()) {
                // Add network tabs (for new connections or non-embedded drivers)
                for (NetworkHandlerDescriptor descriptor : NetworkHandlerRegistry.getInstance().getDescriptors(getActiveDataSource())) {
                    subPages = ArrayUtils.add(IDialogPage.class, subPages, new ConnectionPageNetworkHandler(this, descriptor));
                }
            }

            if (extraPages != null) {
                subPages = ArrayUtils.concatArrays(subPages, extraPages);
            }

            return subPages;
        } else {
            return extraPages;
        }
    }

    public void addSubPage(IDialogPage page) {
        if (extraPages == null) {
            extraPages = new IDialogPage[]{page};
        } else {
            extraPages = ArrayUtils.concatArrays(extraPages, new IDialogPage[]{page});
        }
        if (page instanceof IWizardPage) {
            ((IWizardPage) page).setWizard(getWizard());
        }
    }

    @Override
    public void testConnection(DBCSession session) {
        if (connectionEditor instanceof IDataSourceConnectionTester) {
            ((IDataSourceConnectionTester) connectionEditor).testConnection(session);
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
