/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWUtils;
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
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.dbeaver.ui.preferences.PrefPageProjectNetworkProfiles;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Settings connection page. Hosts particular drivers' connection pages
 */
class ConnectionPageSettings extends ActiveWizardPage<ConnectionWizard> implements IDataSourceConnectionEditorSite, IDialogPageProvider, ICompositeDialogPageContainer, IDataSourceConnectionTester {
    public static final String PAGE_NAME = ConnectionPageSettings.class.getSimpleName();

    private static final Log log = Log.getLog(DriverDescriptor.class);

    private static final Reply REPLY_KEEP = new Reply("&Keep");
    private static final Reply REPLY_REMOVE = new Reply("&Remove");

    // Sort network handler pages to be last
    private static final Comparator<IDialogPage> PAGE_COMPARATOR = Comparator
        .comparing(ConnectionPageSettings::isHandlerPage);

    private static final int MAX_CHEVRON_ITEMS_TO_PREVIEW = 2;

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
    private ToolItem handlerItem;
    private ToolItem profileItem;

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

        String pageTitle = wizard.isNew() ? viewDescriptor.getLabel() : CoreMessages.dialog_setting_connection_wizard_title;
        if (isTemporaryConnection()) {
            pageTitle += " / TEMPORARY";
        }
        setTitle(pageTitle);
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
        UIUtils.asyncExec(() -> connectionEditor.activateEditor());
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

                // Create and populate top-right toolbar
                var toolBarComposite = new Composite(tabFolder, SWT.NONE);
                toolBarComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).create());

                var toolBar = new ToolBar(toolBarComposite, SWT.FLAT | SWT.RIGHT);
                handlerItem = createHandlerItem(toolBar, allPages);
                profileItem = createProfileItem(toolBar);
                tabFolder.setTopRight(toolBarComposite, SWT.RIGHT);
                UIStyles.fixToolBarForeground(toolBar);

                updateHandlerItem(allPages);
                updateProfileItem();

                tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
                    @Override
                    public void close(CTabFolderEvent event) {
                        CTabItem item = (CTabItem) event.item;
                        if (!closeTab(item)) {
                            event.doit = false;
                        }
                    }

                    //@Override
                    public void itemsCount(CTabFolderEvent event) {
                        updateHandlerItem(allPages);
                    }
                });
                tabFolder.addMouseListener(MouseListener.mouseUpAdapter(event -> {
                    if (event.button == 2) {
                        var folder = (CTabFolder) event.widget;
                        var item = folder.getItem(new Point(event.x, event.y));
                        if (item != null) {
                            closeTab(item);
                        }
                    }
                }));
                tabFolder.addKeyListener(KeyListener.keyPressedAdapter(event -> {
                    if (event.keyCode == SWT.DEL && event.stateMask == 0) {
                        var folder = (CTabFolder) event.widget;
                        var selection = folder.getSelection();
                        if (selection != null) {
                            closeTab(selection);
                        }
                    }
                }));

                setControl(tabFolder);

                for (IDialogPage page : allPages) {
                    if (ArrayUtils.contains(extraPages, page) || canAddHandler(page)) {
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
                // Set focus to the first tab
                // Otherwise focus foes into top right control which breaks traverse keys
                tabFolder.getSelection().getControl().setFocus();
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
    private ToolItem createHandlerItem(@NotNull ToolBar toolBar, @NotNull List<IDialogPage> pages) {
        var handlerManager = new MenuManager();
        handlerManager.setRemoveAllWhenShown(true);
        handlerManager.addMenuListener(manager -> {
            for (IDialogPage page : pages) {
                if (canAddHandler(page) && page instanceof ConnectionPageNetworkHandler handlerPage) {
                    manager.add(new AddHandlerAction(handlerPage.getHandlerDescriptor()));
                }
            }
        });

        var toolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
        toolItem.setText(CoreMessages.dialog_connection_network_add_tunnel_label);
        toolItem.setImage(DBeaverIcons.getImage(UIIcon.ADD));
        toolItem.addDisposeListener(e -> handlerManager.dispose());
        toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            var bounds = toolItem.getBounds();
            var location = toolBar.getDisplay().map(toolBar, null, bounds.x, bounds.height);
            var menu = handlerManager.createContextMenu(tabFolder);
            menu.setLocation(location.x, location.y);
            menu.setVisible(true);
        }));

        return toolItem;
    }

    @NotNull
    private ToolItem createProfileItem(@NotNull ToolBar toolBar) {
        var profileManager = new MenuManager();
        profileManager.setRemoveAllWhenShown(true);
        profileManager.addMenuListener(manager -> {
            manager.add(new ChooseNetworkProfileAction());
            manager.add(new Separator());

            var dataSource = getActiveDataSource();
            int index = 0;

            if (dataSource.getOrigin() instanceof DBPDataSourceOriginExternal origin) {
                for (DBWNetworkProfile profile : origin.getAvailableNetworkProfiles()) {
                    manager.add(new ChooseNetworkProfileAction(dataSource, profile, origin, index++));
                }
            }

            manager.add(new Separator());

            for (DBWNetworkProfile profile : getProject().getDataSourceRegistry().getNetworkProfiles()) {
                manager.add(new ChooseNetworkProfileAction(dataSource, profile, null, index++));
            }

            manager.add(new Separator());
            manager.add(new Action("Edit profiles...", DBeaverIcons.getImageDescriptor(UIIcon.RENAME)) {
                @Override
                public void run() {
                    DBWNetworkProfile profile = getActiveProfile();
                    PrefPageProjectNetworkProfiles.open(getShell(), getProject(), profile);
                    if (profile != null) {
                        selectProfile(profile);
                    }
                }
            });
        });

        var toolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
        toolItem.setText("N/A");
        toolItem.setToolTipText("Active profile");
        toolItem.setImage(DBeaverIcons.getImage(DBIcon.TYPE_DOCUMENT));
        toolItem.addDisposeListener(e -> profileManager.dispose());
        toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            var bounds = toolItem.getBounds();
            var location = toolBar.getDisplay().map(toolBar, null, bounds.x, bounds.height);
            var menu = profileManager.createContextMenu(tabFolder);
            menu.setLocation(location.x, location.y);
            menu.setVisible(true);
        }));

        return toolItem;
    }

    @NotNull
    private String computeChevronTitle(@NotNull List<IDialogPage> pages) {
        List<String> items = pages.stream()
            .filter(this::canAddHandler)
            .map(ConnectionPageNetworkHandler.class::cast)
            .map(x -> x.getHandlerDescriptor().getCodeName())
            .toList();
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < Math.min(items.size(), MAX_CHEVRON_ITEMS_TO_PREVIEW); i++) {
            joiner.add(items.get(i));
        }
        if (items.size() > MAX_CHEVRON_ITEMS_TO_PREVIEW) {
            joiner.add("...");
        }
        return joiner.toString();
    }

    private void updateHandlerItem(@NotNull List<IDialogPage> allPages) {
        if (hasHandlersToAdd(allPages)) {
            handlerItem.setText(computeChevronTitle(allPages));
            handlerItem.setImage(DBeaverIcons.getImage(UIIcon.ADD));
            handlerItem.setEnabled(true);
        } else {
            handlerItem.setText("");
            handlerItem.setImage(null);
            handlerItem.setEnabled(false);
        }
    }

    private void updateProfileItem() {
        String profileName = getActiveDataSource().getConnectionConfiguration().getConfigProfileName();
        if (CommonUtils.isNotEmpty(profileName)) {
            profileItem.setText(NLS.bind("Profile ''{0}''", profileName));
            profileItem.setToolTipText(NLS.bind("Active profile is ''{0}''", profileName));
        } else {
            profileItem.setText("No profile");
            profileItem.setToolTipText("No active profile is set");
        }
        updateFolderToolbar();
    }

    private void updateFolderToolbar() {
        try {
            Method method = CTabFolder.class.getDeclaredMethod("updateFolder", int.class);
            method.setAccessible(true);
            method.invoke(tabFolder, 8 /* UPDATE_TAB_HEIGHT | REDRAW */);
        } catch (ReflectiveOperationException e) {
            log.error("Can't update folder toolbar", e);
        }
    }

    private boolean unselectProfile(@Nullable DBWHandlerDescriptor handlerToKeep) {
        if (getActiveProfile() == null) {
            return true;
        }

        Set<DBWHandlerDescriptor> handlersToRemove = new HashSet<>();
        for (CTabItem item : tabFolder.getItems()) {
            if (item.getData() instanceof ConnectionPageNetworkHandler page) {
                handlersToRemove.add(page.getHandlerDescriptor());
            }
        }

        handlersToRemove.remove(handlerToKeep);

        if (!handlersToRemove.isEmpty()) {
            Reply reply = MessageBoxBuilder.builder()
                .setTitle("Change profile")
                .setMessage(NLS.bind(
                    "Do you want to keep {0} after unselecting the active profile?",
                    handlersToRemove.stream().map(DBWHandlerDescriptor::getCodeName).collect(Collectors.joining(", "))
                ))
                .setPrimaryImage(DBIcon.STATUS_QUESTION)
                .setReplies(REPLY_KEEP, REPLY_REMOVE, Reply.CANCEL)
                .setDefaultReply(Reply.CANCEL)
                .showMessageBox();

            if (reply == REPLY_KEEP) {
                handlersToRemove.clear();
            } else if (reply == REPLY_REMOVE) {
                // do nothing
            } else {
                return false;
            }
        }

        selectProfile0(null);

        for (DBWHandlerDescriptor descriptor : handlersToRemove) {
            removeHandler(descriptor);
        }

        refreshHandlers(null);
        updateProfileItem();

        return true;
    }

    private boolean selectProfile(@NotNull DBWNetworkProfile profile) {
        Set<DBWHandlerDescriptor> handlersToRemove = new HashSet<>();
        Set<DBWHandlerDescriptor> handlersToAdd = new HashSet<>();

        for (DBWHandlerConfiguration configuration : profile.getConfigurations()) {
            if (configuration.isEnabled()) {
                handlersToAdd.add(configuration.getHandlerDescriptor());
            }
        }

        for (CTabItem item : tabFolder.getItems()) {
            if (item.getData() instanceof ConnectionPageNetworkHandler page) {
                NetworkHandlerDescriptor descriptor = page.getHandlerDescriptor();
                if (handlersToAdd.contains(descriptor)) {
                    handlersToAdd.remove(descriptor);
                } else {
                    handlersToRemove.add(descriptor);
                }
            }
        }

        if (!handlersToRemove.isEmpty()) {
            String message = NLS.bind(
                "Changing the profile to ''{0}'' will remove {1}.\n\nDo you want to continue?",
                profile.getProfileName(),
                handlersToRemove.stream().map(DBWHandlerDescriptor::getCodeName).collect(Collectors.joining(", "))
            );
            if (!UIUtils.confirmAction(getShell(), "Change profile", message)) {
                return false;
            }
        }

        for (DBWHandlerDescriptor descriptor : handlersToRemove) {
            removeHandler(descriptor);
        }

        selectProfile0(profile);

        for (DBWHandlerDescriptor descriptor : handlersToAdd) {
            addHandler(descriptor, profile);
        }

        refreshHandlers(profile);
        updateProfileItem();

        return true;
    }

    private void selectProfile0(@Nullable DBWNetworkProfile profile) {
        getActiveDataSource().getConnectionConfiguration().setConfigProfile(profile);
    }

    private void addHandler(@NotNull DBWHandlerDescriptor descriptor, @Nullable DBWNetworkProfile profile) {
        if (findHandlerItem(descriptor) != null) {
            log.error("Handler " + descriptor + " is already enabled");
            return;
        }

        var page = findHandlerPage(descriptor);
        if (page == null) {
            log.error("Can't find page for handler " + descriptor);
            return;
        }

        page.loadConfiguration(profile);
        page.getHandlerConfiguration().setEnabled(true);

        var index = Math.min(tabFolder.getItemCount(), ArrayUtils.indexOf(subPages, page) + 1 /* main tab */);
        var item = createPageTab(page, index);

        // TODO: Stop activating pages
        activateItem(item);
    }

    private void removeHandler(@NotNull DBWHandlerDescriptor descriptor) {
        var item = findHandlerItem(descriptor);
        if (item == null) {
            log.error("Can't find page item for handler " + descriptor);
            return;
        }

        var page = (ConnectionPageNetworkHandler) item.getData();
        page.loadConfiguration(null);
        page.getHandlerConfiguration().setEnabled(false);

        // TODO: Stop activating pages
        activateItem(item);
        item.dispose();
    }

    private void refreshHandlers(@Nullable DBWNetworkProfile profile) {
        for (CTabItem item : tabFolder.getItems()) {
            if (item.getData() instanceof ConnectionPageNetworkHandler page) {
                refreshHandler(page.getHandlerDescriptor(), profile);
            }
        }
    }

    private void refreshHandler(@NotNull DBWHandlerDescriptor descriptor, @Nullable DBWNetworkProfile profile) {
        var item = findHandlerItem(descriptor);
        if (item == null) {
            log.error("Can't find page item for handler " + descriptor);
            return;
        }

        activateItem(item);

        var page = (ConnectionPageNetworkHandler) item.getData();
        page.refreshConfiguration(profile);
    }

    @Nullable
    private CTabItem findHandlerItem(@NotNull DBWHandlerDescriptor descriptor) {
        for (CTabItem it : tabFolder.getItems()) {
            if (it.getData() instanceof ConnectionPageNetworkHandler page && page.getHandlerDescriptor() == descriptor) {
                return it;
            }
        }
        return null;
    }

    @Nullable
    private ConnectionPageNetworkHandler findHandlerPage(@NotNull DBWHandlerDescriptor descriptor) {
        for (IDialogPage subPage : subPages) {
            if (subPage instanceof ConnectionPageNetworkHandler page && page.getHandlerDescriptor() == descriptor) {
                return page;
            }
        }
        return null;
    }

    private boolean closeTab(@NotNull CTabItem item) {
        // TODO: Don't require the page to be focused when closing it
        //       has something to do with page not being initialized
        //       and therefore not saving its _updated_ state
        if (item.getShowClose() && tabFolder.getSelection() == item && confirmTabClose(item)) {
            var page = (ConnectionPageNetworkHandler) item.getData();
            var descriptor = page.getHandlerDescriptor();
            if (unselectProfile(descriptor)) {
                removeHandler(descriptor);
                updateProfileItem();
                return true;
            }
        }
        return false;
    }

    private boolean confirmTabClose(@NotNull CTabItem item) {
        if (item.getData() instanceof ConnectionPageNetworkHandler page) {
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

    private boolean hasHandlersToAdd(@NotNull List<IDialogPage> pages) {
        for (IDialogPage page : pages) {
            if (canAddHandler(page)) {
                return true;
            }
        }

        return false;
    }

    private boolean canAddHandler(@NotNull IDialogPage page) {
        if (!isHandlerPage(page)) {
            return false;
        }

        NetworkHandlerDescriptor descriptor = ((ConnectionPageNetworkHandler) page).getHandlerDescriptor();
        for (DBWHandlerConfiguration handler : DBWUtils.getActualNetworkHandlers(getActiveDataSource())) {
            if (handler.getId().equals(descriptor.getId())) {
                return !handler.isEnabled();
            }
        }

        return true;
    }

    private static boolean isHandlerPage(@NotNull IDialogPage page) {
        return page instanceof ConnectionPageNetworkHandler;
    }

    @NotNull
    private CTabItem createPageTab(@NotNull IDialogPage page, int index) {
        final CTabItem item = new CTabItem(tabFolder, isHandlerPage(page) ? SWT.CLOSE : SWT.NONE, index);
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

    private void activateItem(@NotNull CTabItem item) {
        tabFolder.setSelection(item);
        activateCurrentItem();
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
                        panel.setMinSize(panel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
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
//        if (isTemporaryConnection()) {
//            return false;
//        }
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
        if (isTemporaryConnection()) {
            return "Temporary data source (changes won't be saved)";
        }
        final IDialogPage subPage = getCurrentSubPage();
        if (subPage != null && subPage.getErrorMessage() != null) {
            return subPage.getErrorMessage();
        }
        if (connectionEditor != null && connectionEditor.getErrorMessage() != null) {
            return connectionEditor.getErrorMessage();
        }
        return super.getErrorMessage();
    }

    private boolean isTemporaryConnection() {
        DataSourceDescriptor originalDataSource = wizard.getOriginalDataSource();
        return originalDataSource != null && originalDataSource.isTemporary();
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
                activateItem(pageTab);
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

    @Nullable
    private DBWNetworkProfile getActiveProfile() {
        DBPDataSourceContainer dataSource = getActiveDataSource();
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();
        if (CommonUtils.isEmpty(configuration.getConfigProfileName())) {
            return null;
        }
        return dataSource.getRegistry().getNetworkProfile(
            configuration.getConfigProfileSource(),
            configuration.getConfigProfileName()
        );
    }

    private class AddHandlerAction extends Action {
        private final DBWHandlerDescriptor descriptor;

        public AddHandlerAction(@NotNull DBWHandlerDescriptor descriptor) {
            super(descriptor.getCodeName(), AS_PUSH_BUTTON);
            this.descriptor = descriptor;
        }

        @Override
        public void run() {
            if (unselectProfile(descriptor)) {
                addHandler(descriptor, null);
                refreshHandler(descriptor, null);
                updateProfileItem();
            }
        }
    }

    private class ChooseNetworkProfileAction extends Action {
        private final DBWNetworkProfile profile;

        public ChooseNetworkProfileAction() {
            super("None", AS_RADIO_BUTTON);
            this.profile = null;
        }

        public ChooseNetworkProfileAction(
            @NotNull DBPDataSourceContainer container,
            @NotNull DBWNetworkProfile profile,
            @Nullable DBPDataSourceOrigin origin,
            int index
        ) {
            super(null, AS_RADIO_BUTTON);
            this.profile = profile;

            setText(ActionUtils.getLabelWithIndexMnemonic(getProfileName(profile, origin), index));
            setChecked(isProfileSelected(profile, container));
        }

        @Override
        public void run() {
            if (!isChecked()) {
                return;
            }
            if (profile != null) {
                selectProfile(profile);
            } else {
                unselectProfile(null);
            }
        }

        @NotNull
        private static String getProfileName(@NotNull DBWNetworkProfile profile, @Nullable DBPDataSourceOrigin origin) {
            if (origin != null) {
                return NLS.bind("{0} {1}", profile.getProfileName(), origin.getDisplayName());
            } else {
                return profile.getProfileName();
            }
        }

        private static boolean isProfileSelected(@NotNull DBWNetworkProfile profile, @NotNull DBPDataSourceContainer container) {
            DBPConnectionConfiguration config = container.getConnectionConfiguration();
            if (CommonUtils.isEmptyTrimmed(config.getConfigProfileName())) {
                return false;
            }
            return Objects.equals(profile.getProfileName(), config.getConfigProfileName())
                && Objects.equals(profile.getProfileSource(), config.getConfigProfileSource());
        }
    }
}
