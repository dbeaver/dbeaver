/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewRegistry;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.*;

/**
 * Settings connection page. Hosts particular drivers' connection pages
 */
class ConnectionPageSettings extends ActiveWizardPage<ConnectionWizard> implements IDataSourceConnectionEditorSite, IDialogPageProvider, ICompositeDialogPageContainer, IDataSourceConnectionTester {
    private static final Log log = Log.getLog(DriverDescriptor.class);

    public static final String PAGE_NAME = ConnectionPageSettings.class.getSimpleName();

    // Sort network handler pages to be last, with pinned pages first among them
    private static final Comparator<IDialogPage> PAGE_COMPARATOR = Comparator
        .comparing((IDialogPage page) -> page instanceof ConnectionPageNetworkHandler)
        .thenComparing(page -> !isPagePinned(page));

    @NotNull
    private final ConnectionWizard wizard;
    @NotNull
    private final DataSourceViewDescriptor viewDescriptor;
    private final DataSourceViewDescriptor substitutedViewDescriptor;
    private final DBPDriverSubstitutionDescriptor driverSubstitution;
    @Nullable
    private IDataSourceConnectionEditor connectionEditor;
    private IDataSourceConnectionEditor originalConnectionEditor;
    private final Set<DataSourceDescriptor> activated = new HashSet<>();
    private IDialogPage[] subPages, extraPages;
    private CTabFolder tabFolder;

    /**
     * Constructor for ConnectionPageSettings
     */
    ConnectionPageSettings(
        @NotNull ConnectionWizard wizard,
        @NotNull DataSourceViewDescriptor viewDescriptor,
        @Nullable DBPDriverSubstitutionDescriptor driverSubstitution
    ) {
        super(PAGE_NAME + "." + viewDescriptor.getId());

        this.wizard = wizard;
        this.viewDescriptor = viewDescriptor;
        this.driverSubstitution = driverSubstitution;

        if (driverSubstitution != null) {
            this.substitutedViewDescriptor = DataSourceViewRegistry.getInstance().findView(
                DataSourceProviderRegistry.getInstance().getDataSourceProvider(driverSubstitution.getProviderId()),
                IActionConstants.EDIT_CONNECTION_POINT
            );
        } else {
            this.substitutedViewDescriptor = null;
        }

        setTitle(wizard.isNew() ? viewDescriptor.getLabel() : CoreMessages.dialog_setting_connection_wizard_title);
        setDescription(CoreMessages.dialog_connection_description);
    }

    @NotNull
    private IDataSourceConnectionEditor getConnectionEditor() {
        if (connectionEditor == null) {
            if (substitutedViewDescriptor == null) {
                connectionEditor = getOriginalConnectionEditor();
            } else {
                connectionEditor = substitutedViewDescriptor.createView(IDataSourceConnectionEditor.class);
                connectionEditor.setSite(this);
            }
        }

        return connectionEditor;
    }

    @NotNull
    private IDataSourceConnectionEditor getOriginalConnectionEditor() {
        if (originalConnectionEditor == null) {
            originalConnectionEditor = viewDescriptor.createView(IDataSourceConnectionEditor.class);
            originalConnectionEditor.setSite(this);
        }

        return originalConnectionEditor;
    }

    @Override
    public void activatePage() {
        if (connectionEditor == null) {
            createProviderPage(getControl().getParent());
            //UIUtils.resizeShell(getWizard().getContainer().getShell());
        }

        Control control = getControl();
        control.setRedraw(false);
        try {
            setDescription(NLS.bind(CoreMessages.dialog_connection_message, getDriver().getFullName()));
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
        } finally {
            control.setRedraw(true);
        }
        //getContainer().updateTitleBar();
        UIUtils.asyncExec(() -> control.setFocus());
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
        // Because it may contain some driver properties save which will be overwritten by driver props page otherwise
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
            // init main page
            getConnectionEditor();

            // init sub pages (if any)
            IDialogPage[] allSubPages = getDialogPages(false, true);

            {
                // Create tab folder
                List<IDialogPage> allPages = new ArrayList<>();
                allPages.add(connectionEditor);
                if (!ArrayUtils.isEmpty(allSubPages)) {
                    // Add sub pages
                    Collections.addAll(allPages, allSubPages);
                }
                allPages.sort(PAGE_COMPARATOR);

                tabFolder = new CTabFolder(parent, SWT.TOP);
                tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
                tabFolder.setUnselectedCloseVisible(false);

                final ToolBar tabFolderChevron = createChevron(allPages);
                tabFolder.setTopRight(tabFolderChevron, SWT.RIGHT);

                tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
                    @Override
                    public void close(CTabFolderEvent event) {
                        if (confirmTabClose((CTabItem) event.item)) {
                            final ConnectionPageNetworkHandler page = (ConnectionPageNetworkHandler) event.item.getData();
                            final NetworkHandlerDescriptor descriptor = page.getHandlerDescriptor();
                            final DBPConnectionConfiguration configuration = getActiveDataSource().getConnectionConfiguration();
                            final DBWHandlerConfiguration handler = configuration.getHandler(descriptor.getId());

                            if (handler != null) {
                                handler.setEnabled(false);
                            }
                        } else {
                            event.doit = false;
                        }
                    }

                    //@Override
                    public void itemsCount(CTabFolderEvent event) {
                        tabFolderChevron.setVisible(canShowChevron(allPages));
                    }
                });
                tabFolder.addKeyListener(KeyListener.keyPressedAdapter(event -> {
                    if (event.keyCode == SWT.DEL && event.stateMask == 0) {
                        final CTabFolder folder = (CTabFolder) event.widget;
                        final CTabItem selection = folder.getSelection();

                        if (selection != null && selection.getShowClose() && confirmTabClose(selection)) {
                            selection.dispose();
                        }
                    }
                }));

                setControl(tabFolder);

                for (IDialogPage page : allPages) {
                    if (ArrayUtils.contains(extraPages, page) || canShowInChevron(page)) {
                        // Ignore extra pages
                        continue;
                    }
                    createPageTab(page, tabFolder.getItemCount());
                }
                tabFolder.setSelection(0);
                tabFolder.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        activateCurrentItem();
                    }
                });
            }

            activateCurrentItem();
            Dialog.applyDialogFont(tabFolder);
            UIUtils.setHelp(getControl(), IHelpContextIds.CTX_CON_WIZARD_SETTINGS);
        } catch (Exception ex) {
            log.warn(ex);
            setErrorMessage("Can't create settings dialog: " + ex.getMessage());
        }
        parent.layout();
    }

    @NotNull
    private ToolBar createChevron(@NotNull List<IDialogPage> pages) {
        final MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(m -> {
            for (int i = 0; i < pages.size(); i++) {
                final IDialogPage page = pages.get(i);

                if (canShowInChevron(page)) {
                    manager.add(new AddNetworkHandlerAction(getActiveDataSource(), (ConnectionPageNetworkHandler) page, i));
                }
            }
        });

        final ToolBar toolBar = new ToolBar(tabFolder, SWT.FLAT | SWT.RIGHT);

        final ToolItem toolItem = UIUtils
            .createToolItem(toolBar, CoreMessages.dialog_connection_network_add_tunnel_label, null, UIIcon.ADD, null);
        toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            final Rectangle bounds = toolItem.getBounds();
            final Point location = toolBar.getDisplay().map(toolBar, null, 0, bounds.height);
            final Menu menu = manager.createContextMenu(tabFolder);
            menu.setLocation(location.x, location.y);
            menu.setVisible(true);
        }));
        toolItem.addDisposeListener(e -> manager.dispose());

        return toolBar;
    }

    private boolean confirmTabClose(@NotNull CTabItem item) {
        if (item.getData() instanceof ConnectionPageNetworkHandler) {
            final ConnectionPageNetworkHandler page = (ConnectionPageNetworkHandler) item.getData();
            final NetworkHandlerDescriptor descriptor = page.getHandlerDescriptor();

            final int decision = ConfirmationDialog.confirmAction(
                getShell(),
                ConfirmationDialog.INFORMATION,
                DBeaverPreferences.CONFIRM_DISABLE_NETWORK_HANDLER,
                ConfirmationDialog.CONFIRM,
                descriptor.getCodeName()
            );

            return decision == IDialogConstants.OK_ID;
        }

        return false;
    }

    private boolean canShowChevron(@NotNull List<IDialogPage> pages) {
        for (IDialogPage page : pages) {
            if (canShowInChevron(page)) {
                return true;
            }
        }

        return false;
    }

    private boolean canShowInChevron(@NotNull IDialogPage page) {
        if (isPagePinned(page) || !(page instanceof ConnectionPageNetworkHandler)) {
            return false;
        }

        final NetworkHandlerDescriptor descriptor = ((ConnectionPageNetworkHandler) page).getHandlerDescriptor();
        final DBPConnectionConfiguration configuration = getActiveDataSource().getConnectionConfiguration();
        final DBWHandlerConfiguration handler = configuration.getHandler(descriptor.getId());

        return handler == null || !handler.isEnabled();
    }

    private static boolean isPagePinned(@NotNull IDialogPage page) {
        if (page instanceof ConnectionPageNetworkHandler) {
            return ((ConnectionPageNetworkHandler) page).getHandlerDescriptor().isPinned();
        } else {
            return true;
        }
    }

    @NotNull
    private CTabItem createPageTab(@NotNull IDialogPage page, int index) {
        final CTabItem item = new CTabItem(tabFolder, isPagePinned(page) ? SWT.NONE : SWT.CLOSE, index);
        item.setData(page);
        item.setText(CommonUtils.isEmpty(page.getTitle()) ? CoreMessages.dialog_setting_connection_general : page.getTitle());
        item.setToolTipText(page.getDescription());

        if (page.getControl() == null) {
            // TODO: We should respect pages that might not want to be scrollable (e.g. if they have their own scrollable controls)
            item.setControl(UIUtils.createScrolledComposite(tabFolder, SWT.H_SCROLL | SWT.V_SCROLL));
        } else {
            item.setControl(page.getControl().getParent());
        }

        return item;
    }

    private void activateCurrentItem() {
        if (tabFolder != null) {
            CTabItem selection = tabFolder.getSelection();
            if (selection != null) {
                IDialogPage page = (IDialogPage) selection.getData();
                if (page.getControl() == null) {
                    // Create page
                    ScrolledComposite panel = (ScrolledComposite) selection.getControl();
                    panel.setRedraw(false);
                    try {
                        page.createControl(panel);
                        Dialog.applyDialogFont(panel);
                        UIUtils.configureScrolledComposite(panel, page.getControl());
                        panel.layout(true, true);
                    } catch (Throwable e) {
                        DBWorkbench.getPlatformUI().showError("Error creating configuration page", null, e);
                    } finally {
                        panel.setRedraw(true);
                    }
                }
                page.setVisible(true);
                updatePageCompletion();
            }
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }

    @Override
    protected void updatePageCompletion() {
        for (CTabItem item : tabFolder.getItems()) {
            final IDialogPage page = (IDialogPage) item.getData();
            final boolean complete;

            if (item.getData() instanceof IWizardPage p) {
                complete = p.isPageComplete();
            } else if (item.getData() instanceof IDataSourceConnectionEditor p) {
                complete = p.isComplete();
            } else {
                continue;
            }

            if (complete || tabFolder.getSelection() == item) {
                item.setImage(null);
                item.setToolTipText(page.getDescription());
            } else {
                item.setImage(DBeaverIcons.getImage(DBIcon.SMALL_ERROR));
                item.setToolTipText(Objects.requireNonNullElse(page.getErrorMessage(), "Page is incomplete"));
            }
        }

        super.updatePageCompletion();
    }

    @Override
    public boolean isPageComplete() {
        if (subPages != null) {
            for (IDialogPage page : subPages) {
                if (page instanceof IWizardPage wizardPage && !wizardPage.isPageComplete()) {
                    return false;
                }
                if (page instanceof IDataSourceConnectionEditor editor && !editor.isComplete()) {
                    return false;
                }
            }
        }
        return wizard.getPageSettings() != this ||
            this.connectionEditor != null &&
                (this.connectionEditor.isExternalConfigurationProvided() || this.connectionEditor.isComplete());
    }

    @Override
    public String getErrorMessage() {
        final IDialogPage subPage = getCurrentSubPage();
        if (subPage != null && subPage.getErrorMessage() != null) {
            return subPage.getErrorMessage();
        }
        if (connectionEditor != null && connectionEditor.getErrorMessage() != null) {
            return connectionEditor.getErrorMessage();
        }
        return super.getErrorMessage();
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
        return wizard.getActiveDataSource();
    }

    @Override
    public void updateButtons() {
        updatePageCompletion();
        // getWizard().getContainer().updateButtons();
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
    public RCPProject getProject() {
        DBPDataSourceRegistry registry = wizard.getDataSourceRegistry();
        return registry == null ? null : (RCPProject) registry.getProject();
    }

    @Override
    public void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
        PropertyChangeEvent pcEvent = new PropertyChangeEvent(source, property, oldValue, newValue);
        for (CTabItem item : tabFolder.getItems()) {
            IDialogPage page = (IDialogPage) item.getData();
            if (page instanceof IPropertyChangeListener && page.getControl() != null) {
                ((IPropertyChangeListener) page).propertyChange(pcEvent);
            }
        }
        for (IWizardPage page : getWizard().getPages()) {
            if (page instanceof IPropertyChangeListener && page.getControl() != null) {
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
        if (extraPages != null) {
            for (IDialogPage ep : extraPages) {
                ep.dispose();
            }
            extraPages = null;
        }
        super.dispose();
    }

    @Nullable
    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        if (extrasOnly) {
            return extraPages;
        }
        if (subPages != null) {
            return subPages;
        }
        if (!forceCreate) {
            return new IDialogPage[0];
        }

        final IDataSourceConnectionEditor originalConnectionEditor = getOriginalConnectionEditor();

        if (originalConnectionEditor instanceof IDialogPageProvider) {
            subPages = ((IDialogPageProvider) originalConnectionEditor).getDialogPages(extrasOnly, true);

            if ((!getDriver().isEmbedded() || CommonUtils.toBoolean(getDriver().getDriverParameter(DBConstants.DRIVER_PARAM_ENABLE_NETWORK_PARAMETERS)))
                && !CommonUtils.toBoolean(getDriver().getDriverParameter(DBConstants.DRIVER_PARAM_DISABLE_NETWORK_PARAMETERS))
            ) {
                // Add network tabs (for non-embedded drivers)
                for (NetworkHandlerDescriptor descriptor : NetworkHandlerRegistry.getInstance().getDescriptors(getActiveDataSource())) {
                    if (driverSubstitution != null && !driverSubstitution.getInstance().isNetworkHandlerSupported(descriptor)) {
                        continue;
                    }
                    subPages = ArrayUtils.add(IDialogPage.class, subPages, new ConnectionPageNetworkHandler(this, descriptor));
                }
            }

            if (extraPages != null) {
                subPages = ArrayUtils.concatArrays(subPages, extraPages);
            }

            try {
                // Externally provided sub-pages
                IDialogPageProvider externalPagesProvider = GeneralUtils.adapt(
                    getActiveDataSource(),
                    IDialogPageProvider.class);
                if (externalPagesProvider != null) {
                    IDialogPage[] dialogPages = externalPagesProvider.getDialogPages(false, true);
                    if (dialogPages != null) {
                        for (IDialogPage page : dialogPages) {
                            if (page != null) {
                                subPages = ArrayUtils.add(IDialogPage.class, subPages, page);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e);
            }


            if (!ArrayUtils.isEmpty(subPages)) {
                for (IDialogPage page : subPages) {
                    if (page instanceof IDataSourceConnectionEditor p) {
                        p.setSite(this);
                    }
                    if (page instanceof IWizardPage p) {
                        p.setWizard(getWizard());
                    }
                }
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

    @Override
    public void showSubPage(IDialogPage subPage) {
        CTabItem selection = tabFolder.getSelection();
        for (CTabItem pageTab : tabFolder.getItems()) {
            if (pageTab.getData() == subPage) {
                tabFolder.setSelection(pageTab);
                activateCurrentItem();
                if (selection != null && selection.getData() != subPage && selection.getData() instanceof ActiveWizardPage) {
                    ((ActiveWizardPage<?>) selection.getData()).deactivatePage();
                }
                if (subPage instanceof ActiveWizardPage) {
                    ((ActiveWizardPage<?>) subPage).activatePage();
                }
                break;
            }
        }
    }

    @Override
    @Nullable
    public IDialogPage getCurrentSubPage() {
        final CTabItem selection = tabFolder.getSelection();
        return selection != null ? (IDialogPage) selection.getData() : null;
    }

    private class AddNetworkHandlerAction extends Action {
        private final DBPDataSourceContainer container;
        private final ConnectionPageNetworkHandler page;
        private final int index;

        public AddNetworkHandlerAction(@NotNull DBPDataSourceContainer container, @NotNull ConnectionPageNetworkHandler page, int index) {
            super(page.getHandlerDescriptor().getCodeName(), AS_PUSH_BUTTON);

            this.container = container;
            this.page = page;
            this.index = index;
        }

        @Override
        public void run() {
            final NetworkHandlerDescriptor descriptor = page.getHandlerDescriptor();
            final DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
            DBWHandlerConfiguration handler = configuration.getHandler(descriptor.getId());

            if (handler == null) {
                handler = new DBWHandlerConfiguration(descriptor, container);
                configuration.updateHandler(handler);
            }

            handler.setEnabled(true);
            tabFolder.setSelection(createPageTab(page, Math.min(tabFolder.getItemCount(), index)));
            activateCurrentItem();
        }
    }
}
