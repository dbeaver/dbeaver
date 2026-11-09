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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.action.*;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.ai.chat.AIChatController;
import org.jkiss.dbeaver.ui.ai.chat.AIChatUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatIcons;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.dbeaver.ui.ai.preferences.AIPreferencePageChat;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class ContextComposite extends Composite {

    private static final Log log = Log.getLog(ContextComposite.class);

    private static final String CSS_ID = "ai-chat-context";

    private final AIChatControl chat;

    private final Composite contextComposite;
    private final Label contextIcon;
    private final Label contextName;
    private final MenuManager scopeDropDown;
    private final MenuManager contextDropDown;
    private final ToolBarManager toolBarManager;
    private final Composite conversationComposite;
    private final Label conversationIcon;
    private final Text conversationNameText;
    private final AdditionalConnectionsComposite additionalConnections;
    private final ToolBarManager conversationToolBarManager;

    private IAction changeScopeAction;
    private IAction addConversationAction;
    private IAction addConnectionsAction;
    @Nullable
    private IAction deleteConversationAction;

    public ContextComposite(@NotNull AIChatControl chat, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.chat = chat;
        GridLayout headerLayout = new GridLayout(2, false);
        headerLayout.marginWidth = 5;
        headerLayout.marginHeight = 5;
        headerLayout.horizontalSpacing = 5;
        headerLayout.verticalSpacing = 5;
        setLayout(headerLayout);
        WidgetElement.setID(this, CSS_ID);

        { // conversation picker
            conversationComposite = new Composite(this, SWT.NO);
            conversationComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).numColumns(3).create());
            conversationComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            new CompositeBorderPainter(conversationComposite);

            conversationIcon = new Label(conversationComposite, SWT.NONE);
            conversationIcon.setImage(DBeaverIcons.getImage(AIIcons.AI));
            conversationIcon.addMouseListener(MouseListener.mouseDownAdapter(event -> showConversationDropDown()));

            conversationNameText = new Text(conversationComposite, SWT.NONE);
            conversationNameText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            conversationNameText.addMouseListener(MouseListener.mouseDoubleClickAdapter(mouseEvent -> showConversationDropDown()));
            conversationNameText.addKeyListener(KeyListener.keyPressedAdapter(e -> {
                if (e.keyCode == SWT.ARROW_DOWN) {
                    showConversationDropDown();
                } else if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    chat.renameConversation(chat.getActiveConversation(), conversationNameText.getText());
                    chat.setFocusOnPrompt();
                } else if (e.keyCode == SWT.ESC) {
                    conversationNameText.setText(chat.getActiveConversation().getCaption());
                    chat.setFocusOnPrompt();
                }
            }));
            conversationNameText.getAccessible().addAccessibleListener(new AccessibleAdapter() {
                @Override
                public void getName(@NotNull AccessibleEvent e) {
                    e.result = AIChatMessagesUI.ai_chat_a11y_conversation_name;
                }
            });
            conversationNameText.addFocusListener(FocusListener.focusLostAdapter(e -> {
                AIChatConversation activeConversation = chat.getActiveConversation();
                chat.renameConversation(
                    activeConversation,
                    conversationNameText.getText()
                );
            }));

            Label conversationArrow = new Label(conversationComposite, SWT.NONE);
            conversationArrow.setImage(DBeaverIcons.getImage(UIIcon.TREE_COLLAPSE));
            conversationArrow.addMouseListener(MouseListener.mouseDownAdapter(e -> showConversationDropDown()));


            conversationToolBarManager = new ToolBarManager(SWT.FLAT);
            contributeConversationActions(conversationToolBarManager);
            contributeContextActions(conversationToolBarManager);
            conversationToolBarManager.createControl(this);
        }


        Composite databaseRow = new Composite(this, SWT.NONE);
        databaseRow.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(5, 5).create());
        databaseRow.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
        { // context drop-down
            contextComposite = new Composite(databaseRow, SWT.NONE);
            contextComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).numColumns(3).create());
            GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            contextComposite.setLayoutData(gd);
            new CompositeBorderPainter(contextComposite);
            contextComposite.addKeyListener(KeyListener.keyPressedAdapter(e -> {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR || e.keyCode == SWT.ARROW_DOWN || e.character == ' ') {
                    showContextDropDown();
                } else if (e.keyCode == SWT.ESC) {
                    chat.setFocusOnPrompt();
                }
            }));
            contextComposite.addTraverseListener(e -> {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            });
            contextComposite.addFocusListener(FocusListener.focusGainedAdapter(e -> contextComposite.redraw()));
            contextComposite.addFocusListener(FocusListener.focusLostAdapter(e -> contextComposite.redraw()));
            contextComposite.addPaintListener(e -> {
                if (contextComposite.isFocusControl()) {
                    Rectangle bounds = contextComposite.getBounds();
                    e.gc.drawFocus(1, 1, bounds.width - 2, bounds.height - 2);
                }
            });
            contextComposite.getAccessible().addAccessibleListener(new AccessibleAdapter() {
                @Override
                public void getName(@NotNull AccessibleEvent e) {
                    e.result = AIChatMessagesUI.ai_chat_a11y_connection_name + ": " + contextName.getText();
                }
            });

            contextIcon = new Label(contextComposite, SWT.NONE);
            contextIcon.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));
            contextIcon.addMouseListener(MouseListener.mouseDownAdapter(event -> showContextDropDown()));

            contextName = new Label(contextComposite, SWT.NONE);
            contextName.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            contextName.addMouseListener(MouseListener.mouseDownAdapter(e -> showContextDropDown()));

            Label contextArrow = new Label(contextComposite, SWT.NONE);
            contextArrow.setImage(DBeaverIcons.getImage(UIIcon.TREE_COLLAPSE));
            contextArrow.addMouseListener(MouseListener.mouseDownAdapter(e -> showContextDropDown()));

            contextDropDown = new MenuManager();
            contextDropDown.setRemoveAllWhenShown(true);
            contextDropDown.addMenuListener(manager -> {
                try {
                    fillContextDropDown(manager);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI().showError("Error filling context drop-down", "Can't fill context drop-down", e);
                }
            });
        }

        toolBarManager = new ToolBarManager(SWT.FLAT);
        addConnectionsAction = new Action(
            AIChatMessagesUI.ai_chat_add_connections, DBeaverIcons.getImageDescriptor(AIChatIcons.CONNECTION_ADD)
        ) {
            @Override
            public void run() {
                AddConnectionsDialog dialog = new AddConnectionsDialog(getShell(), chat.getActiveConversation());
                if (dialog.open() == org.eclipse.jface.window.Window.OK) {
                    chat.addConnections(dialog.getConnections());
                }
            }
        };
        toolBarManager.add(addConnectionsAction);
        ToolBar connectionToolbar = toolBarManager.createControl(databaseRow);
        connectionToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        scopeDropDown = new MenuManager();
        scopeDropDown.setRemoveAllWhenShown(true);
        scopeDropDown.addMenuListener(this::fillScopeDropDown);

        additionalConnections = new AdditionalConnectionsComposite(chat, this);
        additionalConnections.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
        chat.enableConnectionDropTargets(this);

        AIChatListener chatListener = new AIChatListener() {
            @Override
            public void messageAdded(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
                if (chat.isDisposed()) {
                    return;
                }
                UIUtils.asyncExec(() -> {
                    if (!isDisposed() && conversation == chat.getActiveConversation()) {
                        updateActions();
                        conversationNameText.setText(conversation.getCaption());
                    }
                });
            }

            @Override
            public void messageRemoved(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
                UIUtils.asyncExec(() -> updateActions());
            }

            @Override
            public void settingsChanged(@Nullable AIContextSettings settings) {
                UIUtils.asyncExec(() -> {
                    if (!isDisposed()) {
                        updateContext(settings);
                    }
                });
            }

            @Override
            public void conversationChanged(@NotNull AIChatConversation conversation) {
                UIUtils.asyncExec(() -> {
                    if (!isDisposed() && conversation == chat.getActiveConversation()) {
                        updateConversation(conversation);
                    }
                });
            }

            @Override
            public void conversationRenamed(@NotNull AIChatConversation conversation, @NotNull String newName) {
                UIUtils.asyncExec(() -> {
                    if (!isDisposed() && conversation == chat.getActiveConversation()) {
                        conversationNameText.setText(conversation.getCaption());
                        conversationNameText.setToolTipText(conversation.getCaption());
                    }
                });
            }

            @Override
            public void conversationConnectionsChanged(@NotNull AIChatConversation conversation) {
                UIUtils.asyncExec(() -> {
                    if (!isDisposed() && conversation == chat.getActiveConversation()) {
                        additionalConnections.setConversation(conversation);
                    }
                });
            }

            @Override
            public void busyChanged(boolean busy) {
                UIUtils.asyncExec(() -> {
                    if (isDisposed()) {
                        return;
                    }
                    updateActions();
                    additionalConnections.setEnabled(!busy);
                    UIUtils.enableWithChildren(contextComposite, !busy);
                    UIUtils.enableWithChildren(conversationComposite, !busy);
                });
            }
        };
        chat.getChatSession().addListener(chatListener);
        addDisposeListener(event -> chat.getChatSession().removeListener(chatListener));

        updateConversation(chat.getActiveConversation());
        updateContext(chat.getCompletionSettings());
    }

    @Override
    public void dispose() {
        super.dispose();
        toolBarManager.dispose();
        conversationToolBarManager.dispose();
        contextDropDown.dispose();
        scopeDropDown.dispose();
    }

    private void updateContext(@Nullable AIContextSettings completionSettings) {
        if (completionSettings != null) {
            this.contextName.setText(completionSettings.getDataSourceContainer().getName());
            this.contextIcon.setImage(DBeaverIcons.getImage(completionSettings.getDataSourceContainer().getDriver().getIcon()));
        } else {
            this.contextName.setText(AIChatMessagesUI.ai_chat_context_menu_no_connection);
            this.contextIcon.setImage(DBeaverIcons.getImage(DBIcon.DATABASE_DEFAULT));
        }

        contextName.setToolTipText(contextName.getText());
        layout(true, true);
        updateActions();
    }

    private void updateConversation(@NotNull AIChatConversation conversation) {
        DBPImage convIcon = getConversationIcon(conversation);
        conversationIcon.setImage(DBeaverIcons.getImage(convIcon));
        conversationNameText.setText(conversation.getCaption());
        conversationNameText.setToolTipText(conversation.getCaption());
        additionalConnections.setConversation(conversation);
        updateActions();
    }

    @NotNull
    private static DBPImage getConversationIcon(@NotNull AIChatConversation conversation) {
        DBPImage convIcon = AIIcons.AI;
        {
            AIPromptGenerator promptGenerator = conversation.getPromptGenerator();
            AIPromptGeneratorDescriptor gd = AIPromptGeneratorRegistry.getInstance()
                .getPromptGenerator(promptGenerator.generatorId());
            if (gd != null && gd.getIcon() != null) {
                convIcon = gd.getIcon();
            }
        }
        return convIcon;
    }

    private void showContextDropDown() {
        showDropDown(contextComposite, 0, contextDropDown);
    }

    private void showConversationDropDown() {
        try {
            AIChatConversation active = chat.getActiveConversation();
            DBPDataSourceContainer primary = active.getDataSource();
            Map<DBPDataSourceContainer, List<AIChatConversation>> groups = new LinkedHashMap<>();
            groups.put(primary, new ArrayList<>());
            for (DBPDataSourceContainer container : active.getDataSources()) {
                groups.putIfAbsent(container, new ArrayList<>());
            }
            for (var entry : groups.entrySet()) {
                entry.getValue().addAll(chat.getChatSession().getAllConversations(entry.getKey()).stream()
                    .sorted(Comparator.comparing(AIChatConversation::getTime).reversed()).toList());
            }
            List<AIChatConversation> primaryConversations = groups.get(primary);
            if (!primaryConversations.contains(active)) {
                primaryConversations.addFirst(active);
            }
            if (active == chat.getActiveConversation()) {
                new ConversationSelectorPopup(conversationComposite, chat, groups).open();
            }
        } catch (DBException exception) {
            DBWorkbench.getPlatformUI().showError(AIChatMessagesUI.ai_chat_conversation_error, null, exception);
        }
    }

    public void showScopeDropDown() {
        if (chat.isBusy() || chat.isWaitingForResponse()) {
            return;
        }
        ToolBar toolBar = conversationToolBarManager.getControl();
        showDropDown(toolBar, toolBar.getItem(1).getBounds().x, scopeDropDown);
    }

    public void openSettings() {
        if (DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            UIUtils.showPreferencesFor(
                getShell(), AISettingsManager.getInstance().getSettings(), AIPreferencePageChat.PAGE_ID);
        }
    }

    private void showDropDown(@NotNull Control origin, int shift, @NotNull MenuManager manager) {
        Menu menu = manager.createContextMenu(origin);
        menu.setLocation(origin.toDisplay(shift, origin.getSize().y));
        menu.setVisible(true);
    }

    private void fillContextDropDown(@NotNull IMenuManager manager) throws DBException {
        var projects = DBWorkbench.getPlatform().getWorkspace().getProjects();
        // Add all datasources with chats (even disconnected)
        Set<DBPDataSourceContainer> containers = new LinkedHashSet<>(chat.getChatSession().getAllDataSources());
        // Get all connected datasources
        containers.addAll(projects.stream()
            .filter(DBPProject::isRegistryLoaded)
            .map(DBPProject::getDataSourceRegistry)
            .flatMap(ds -> ds.getDataSources().stream())
            .filter(DBPDataSourceContainer::isConnected)
            .toList());
        if (containers.isEmpty()) {
            manager.add(new EmptyAction(AIChatMessagesUI.ai_chat_context_menu_no_active_connections));
        }
        for (DBPDataSourceContainer container : containers) {
            manager.add(new Action(container.getName(), DBeaverIcons.getImageDescriptor(container.getDriver().getIcon())) {
                @Override
                public void run() {
                    try {
                        chat.setDataSource(container);
                    } catch (Exception e) {
                        DBWorkbench.getPlatformUI().showError(
                            "Error setting data source",
                            "Could not set data source: " + e.getMessage(),
                            e
                        );
                    }
                }
            });
        }
        if (chat.getCompletionSettings() != null) {
            manager.add(new Separator());
            manager.add(new Action(
                AIChatMessagesUI.ai_chat_context_menu_no_connection,
                DBeaverIcons.getImageDescriptor(DBIcon.DATABASE_DEFAULT)
            ) {
                @Override
                public void run() {
                    try {
                        chat.setDataSource(null);
                    } catch (Exception e) {
                        DBWorkbench.getPlatformUI().showError(
                            "Error clearing data source",
                            "Could not clear data source: " + e.getMessage(),
                            e
                        );
                    }
                }
            });
        }
    }

    private void fillScopeDropDown(@NotNull IMenuManager manager) {
        AIContextSettings settings = chat.getCompletionSettings();
        if (settings == null) {
            manager.add(new EmptyAction(AIChatMessagesUI.ai_chat_context_menu_no_database_connection));
            manager.add(new Action(AIChatMessagesUI.ai_chat_context_menu_select_connection) {
                @Override
                public void run() {
                    showContextDropDown();
                }
            });
        } else {
            manager.add(new EmptyAction(AIChatMessagesUI.ai_chat_primary_context_applies_to));
            manager.add(new ChangeContextLevelAction(true));
            manager.add(new ChangeContextLevelAction(false));
            manager.add(new Separator());
            DBPDataSourceContainer dsContainer = chat.getDataSourceContainer();
            DBCExecutionContext executionContext = dsContainer == null ? null : chat.getExecutionContext(dsContainer);
            if (executionContext == null) {
                manager.add(new EmptyAction(
                    dsContainer == null ?
                        AIChatMessagesUI.ai_chat_context_menu_no_database_connection :
                        (dsContainer.isConnecting() ?
                            AIChatMessagesUI.ai_chat_context_menu_database_connecting :
                            AIChatMessagesUI.ai_chat_context_menu_database_not_connected)
                ));
            } else {
                manager.add(new EmptyAction(AIChatMessagesUI.ai_chat_primary_objects_sent));
                DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
                List<AIDatabaseScope> availableScopes = AIDatabaseScope.getAvailableScopes(contextDefaults);

                for (AIDatabaseScope scope : AIDatabaseScope.values()) {
                    if (!availableScopes.contains(scope)) {
                        if (settings.getScope() == scope) {
                            AIDatabaseScope newScope = scope == AIDatabaseScope.CURRENT_SCHEMA ?
                                AIDatabaseScope.CURRENT_DATABASE : AIDatabaseScope.CURRENT_DATASOURCE;
                            log.trace("AI scope fallback to " + newScope);
                            settings.setScope(newScope);
                        }
                        continue;
                    }
                    manager.add(new ChangeScopeAction(settings, scope, dsContainer, contextDefaults));
                }
            }
        }

        manager.add(new Separator());
        Action configureContext = new Action(AIChatMessagesUI.ai_chat_configure_database_context) {
            @Override
            public void run() {
                new DatabaseContextDialog(getShell(), chat).open();
            }
        };
        configureContext.setEnabled(chat.getActiveConversation().getDataSource() != null);
        manager.add(configureContext);
        manager.add(new Separator());
        MenuManager profiles = new MenuManager(AIChatMessagesUI.ai_chat_context_menu_active_configuration);
        Stream.of(AISettingsManager.getInstance().getSettings().getConfigurations())
            .sorted(Comparator.comparing(AIConfigurationProfile::getProfileName, String.CASE_INSENSITIVE_ORDER))
            .map(ChangeProfileAction::new)
            .forEach(profiles::add);
        manager.add(profiles);

        manager.add(new Separator());
        contributeSettingActions(manager);

        manager.add(new Separator());
        manager.add(new Action(AIChatMessagesUI.ai_chat_conversation_same_databases) {
            @Override
            public void run() {
                chat.createNewConversation(true);
            }
        });
        if (deleteConversationAction != null) {
            manager.add(new Separator());
            manager.add(deleteConversationAction);
        }
    }

    private void updateActions() {
        if (isDisposed()) {
            return;
        }
        boolean busy = chat.isBusy() || chat.isWaitingForResponse();
        changeScopeAction.setEnabled(!busy);
        addConversationAction.setEnabled(!busy);
        addConnectionsAction.setEnabled(!busy && chat.getActiveConversation().getDataSource() != null);
        if (deleteConversationAction != null) {
            deleteConversationAction.setEnabled(!busy);
        }
    }

    private void contributeContextActions(@NotNull IContributionManager manager) {
        changeScopeAction = new Action(AIChatMessagesUI.ai_chat_more, DBeaverIcons.getImageDescriptor(UIIcon.DOTS_BUTTON)) {
            @Override
            public void run() {
                showScopeDropDown();
            }
        };
        setToolTipWithShortcut(changeScopeAction, AIChatController.CMD_OPEN_FILTERS);

        manager.add(changeScopeAction);
        Action settingsAction = new Action(
            AIChatMessagesUI.ai_chat_settings_label,
            DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)
        ) {
            @Override
            public void run() {
                openSettings();
            }
        };
        setToolTipWithShortcut(settingsAction, AIChatController.CMD_OPEN_SETTINGS);
        settingsAction.setEnabled(
            DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER));
        manager.add(settingsAction);
    }

    private void setToolTipWithShortcut(@NotNull IAction action, @NotNull String commandId) {
        String tip = action.getText();
        String shortcut = ActionUtils.findCommandDescription(commandId, chat.getController().getSite(), true);
        action.setToolTipText(CommonUtils.isEmpty(shortcut) ? tip : tip + " (" + shortcut + ")");
    }

    private void contributeSettingActions(@NotNull IContributionManager manager) {
        manager.add(new EmptyAction(AIChatMessagesUI.ai_chat_context_menu_messages));
        manager.add(new SettingsToggleAction(
            AIChatMessagesUI.ai_chat_context_menu_show_message_time, AIConstants.AI_CHAT_SHOW_MESSAGE_TIME));
        manager.add(new SettingsToggleAction(
            AIChatMessagesUI.ai_chat_context_menu_show_time_spent, AIConstants.AI_CHAT_SHOW_TIME_SPENT));
        manager.add(new SettingsToggleAction(
            AIChatMessagesUI.ai_chat_context_menu_show_tokens_spent, AIConstants.AI_CHAT_SHOW_TOKENS_SPENT));
        manager.add(new Separator());
        manager.add(new EmptyAction(AIChatMessagesUI.ai_chat_context_menu_chat));
        manager.add(new SettingsToggleAction(
            AIChatMessagesUI.ai_chat_context_menu_show_total_tokens_spent, AIConstants.AI_CHAT_SHOW_TOTAL_TOKENS_SPENT));
        manager.add(new SettingsToggleAction(
            AIChatMessagesUI.ai_chat_show_profile_and_model, AIConstants.AI_CHAT_SHOW_PROFILE_AND_MODEL, true));
    }

    private void contributeConversationActions(@NotNull IContributionManager manager) {
        addConversationAction = new Action(AIChatMessagesUI.ai_chat_conversation_new_label, DBeaverIcons.getImageDescriptor(UIIcon.ADD)) {
            @Override
            public void run() {
                chat.createNewConversation();
            }
        };
        setToolTipWithShortcut(addConversationAction, AIChatController.CMD_NEW_CONVERSATION);
        manager.add(addConversationAction);
        if (chat.getChatSession().getStorage().canPersist()) {
            deleteConversationAction = new Action(
                AIChatMessagesUI.ai_chat_conversation_delete_label, DBeaverIcons.getImageDescriptor(UIIcon.CLOSE)
            ) {
                @Override
                public void run() {
                    chat.deleteActiveConversationWithConfirmation();
                }
            };
            setToolTipWithShortcut(deleteConversationAction, AIChatController.CMD_DELETE_CONVERSATION);
        }
    }

    private static class SettingsToggleAction extends Action {
        private final String id;
        private final boolean defaultValue;

        SettingsToggleAction(@NotNull String name, @NotNull String id) {
            this(name, id, false);
        }

        SettingsToggleAction(@NotNull String name, @NotNull String id, boolean defaultValue) {
            super(name, IAction.AS_CHECK_BOX);
            this.id = id;
            this.defaultValue = defaultValue;
        }

        @Override
        public void run() {
            AISettingsManager.getInstance()
                .modifySettings(s -> s.setProperty(id, !s.getProperty(id, defaultValue)));
        }

        @Override
        public boolean isChecked() {
            return AISettingsManager.getInstance()
                .getSettings().getProperty(id, defaultValue);
        }
    }

    private class ChangeContextLevelAction extends Action {
        private final boolean dataSourceContext;

        public ChangeContextLevelAction(boolean dataSourceContext) {
            super(
                dataSourceContext ?
                    AIChatMessagesUI.ai_chat_context_menu_this_connection :
                    AIChatMessagesUI.ai_chat_context_menu_this_conversation,
                Action.AS_RADIO_BUTTON
            );
            this.dataSourceContext = dataSourceContext;

            boolean isDataSourceSettings = chat.getCompletionSettings() instanceof AIContextSettingsDataSource;
            setChecked(dataSourceContext == isDataSourceSettings);
        }

        @Override
        public void run() {
            if (!isChecked()) {
                return;
            }
            DBPDataSourceContainer dataSourceContainer = chat.getDataSourceContainer();
            if (dataSourceContainer == null) {
                DBWorkbench.getPlatformUI().showError(
                    "No connection", "Cannot change context level when no connection is selected");
                return;
            }
            AIContextSettings currentSettings = chat.getChatSession().getConversationSettings(chat.getActiveConversation());
            AIContextSettings newSettings = createContextSettings(dataSourceContainer, currentSettings);
            chat.setCompletionSettings(newSettings);
        }

        @NotNull
        private AIContextSettings createContextSettings(
            @NotNull DBPDataSourceContainer dataSourceContainer,
            @Nullable AIContextSettings currentSettings
        ) {
            AIContextSettings newSettings;
            // change scope
            if (dataSourceContext) {
                newSettings = new AIContextSettingsDataSource(dataSourceContainer);
            } else {
                newSettings = new AIContextSettingsChatConversation(chat.getChatSession(), chat.getActiveConversation());
            }
            if (currentSettings != null && !dataSourceContext) {
                newSettings.setScope(currentSettings.getScope());
                if (newSettings.getScope() == AIDatabaseScope.CUSTOM) {
                    newSettings.setCustomObjectIds(currentSettings.getCustomObjectIds());
                }
            } else {
                newSettings.setScope(AIDatabaseScope.CURRENT_SCHEMA);
            }
            return newSettings;
        }
    }

    private class ChangeScopeAction extends Action {
        private final AIContextSettings settings;
        private final AIDatabaseScope scope;

        public ChangeScopeAction(
            @NotNull AIContextSettings settings,
            @NotNull AIDatabaseScope scope,
            @NotNull DBPDataSourceContainer dsContainer,
            @Nullable DBCExecutionContextDefaults<?, ?> contextDefaults
        ) {
            super(AIChatScopeUtils.getTitle(settings, scope, dsContainer, contextDefaults), AS_RADIO_BUTTON);
            this.settings = settings;
            this.scope = scope;
            setChecked(settings.getScope() == scope);
        }


        @Override
        public void run() {
            if (!isChecked()) {
                return;
            }
            if (scope == AIDatabaseScope.CUSTOM) {
                chooseCustomScope();
            } else {
                settings.setScope(scope);
                AIChatUtils.saveContextSettings(chat.getActiveConversation(), settings);
            }
        }

        private void chooseCustomScope() {
            DBPDataSourceContainer container = settings.getDataSourceContainer();
            if (container == null) {
                return;
            }
            DBCExecutionContext executionContext = chat.getExecutionContext(container);
            if (executionContext == null) {
                return;
            }
            AIChatUtils.chooseCustomScope(
                getShell(),
                settings,
                container,
                ds -> executionContext,
                chat.getActiveConversation()
            );
        }
    }

    private class ChangeProfileAction extends Action {

        private final AIConfigurationProfile profile;

        public ChangeProfileAction(
            @NotNull AIConfigurationProfile profile
        ) {
            super(genProfileName(profile), AS_RADIO_BUTTON);
            this.profile = profile;
            setEnabled(profile.isAvailable());
            if (profile.isAvailable()) {
                try {
                    AIEngineDescriptor engine = profile.getEngineDescriptor();
                    setImageDescriptor(DBeaverIcons.getImageDescriptor(engine.getIcon()));
                } catch (DBException e) {
                    log.debug(e);
                }
            }
            AIConfigurationProfile chatProfile = chat.getActiveConversation().getProfile();
            if (chatProfile == null) {
                chatProfile = AISettingsManager.getStaticSettings().getDefaultConfigurationOrNull();
            }
            setChecked(chatProfile == profile);
        }

        @NotNull
        private static String genProfileName(@NotNull AIConfigurationProfile profile) {
            if (!profile.isAvailable()) {
                return profile.getProfileName() + " - " + AIUIMessages.ai_engines_page_provider_unavailable;
            }
            String model = null;
            try {
                model = profile.getConfiguration().getModel();
            } catch (DBException e) {
                log.debug(e);
            }
            return profile.getProfileName() + (model == null ? "" : " / " + model);
        }

        @Override
        public void run() {
            if (!isChecked() || !profile.isAvailable()) {
                return;
            }
            chat.selectConversationProfile(profile);
        }
    }

}
