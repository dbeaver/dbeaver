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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIPromptGeneratorRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.ai.AIUIUtils;
import org.jkiss.dbeaver.ui.ai.chat.AIChatUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessages;
import org.jkiss.utils.ArrayUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ContextComposite extends Composite {

    public static final DateTimeFormatter CONV_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm");
    public static final DateTimeFormatter CONV_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm (MMM dd)");
    private static final Log log = Log.getLog(ContextComposite.class);

    private static final String CSS_ID = "ai-chat-context";

    private final AIChatControl chat;

    private final Composite contextComposite;
    private final Label contextIcon;
    private final Label contextName;
    private final MenuManager scopeDropDown;
    private final MenuManager contextDropDown;
    private final MenuManager settingsDropDown;
    private final MenuManager conversationDropDown;
    private final ToolBarManager toolBarManager;
    private final Composite conversationComposite;
    private final Label conversationIcon;
    private final Text conversationNameText;
    private final List<AIChatConversation> contextConversations = new ArrayList<>();

    private IAction changeScopeAction;
    private IAction addConversationAction;
    @Nullable
    private IAction deleteConversationAction;

    public ContextComposite(@NotNull AIChatControl chat, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.chat = chat;
        this.chat.getChatSession().addListener(new AIChatListener() {
            @Override
            public void messageAdded(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
                if (chat.isDisposed()) {
                    return;
                }
                UIUtils.asyncExec(() -> {
                    updateActions();
                    conversationNameText.setText(conversation.getCaption());
                    chat.renameConversation(conversation, conversation.getCaption());
                });
            }

            @Override
            public void messageRemoved(@NotNull AIChatConversation conversation, @NotNull AIChatMessage message) {
                updateActions();
            }

            @Override
            public void settingsChanged(@Nullable AIContextSettings settings) {
                updateContext(settings);
            }

            @Override
            public void conversationChanged(@NotNull AIChatConversation conversation) {
                try {
                    updateConversation(conversation);
                } catch (DBException e) {
                    DBWorkbench.getPlatformUI()
                        .showError("Error updating conversation list", "Can't update conversation list", e);
                }
            }

            @Override
            public void busyChanged(boolean busy) {
                UIUtils.asyncExec(() -> {
                    updateActions();
                    UIUtils.enableWithChildren(contextComposite, !busy);
                    UIUtils.enableWithChildren(conversationComposite, !busy);
                });
            }
        });

        setLayout(new GridLayout(2, false));
        WidgetElement.setID(this, CSS_ID);

        { // context drop-down
            contextComposite = new Composite(this, SWT.NONE);
            contextComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).numColumns(3).create());
            GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
            contextComposite.setLayoutData(gd);
            new CompositeBorderPainter(contextComposite);

            contextIcon = new Label(contextComposite, SWT.NONE);
            contextIcon.setImage(DBeaverIcons.getImage(DBIcon.TREE_DATABASE));

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

            settingsDropDown = new MenuManager();
            settingsDropDown.setRemoveAllWhenShown(true);
            settingsDropDown.addMenuListener(this::contributeSettingActions);
        }

        toolBarManager = new ToolBarManager(SWT.FLAT);
        contributeContextActions(toolBarManager);
        toolBarManager.createControl(this);

        scopeDropDown = new MenuManager();
        scopeDropDown.setRemoveAllWhenShown(true);
        scopeDropDown.addMenuListener(this::fillScopeDropDown);

        { // conversation picker
            conversationComposite = new Composite(this, SWT.NO);
            conversationComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).numColumns(3).create());
            conversationComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            new CompositeBorderPainter(conversationComposite);

            conversationIcon = new Label(conversationComposite, SWT.NONE);
            conversationIcon.setImage(DBeaverIcons.getImage(AIIcons.AI));

            conversationNameText = new Text(conversationComposite, SWT.NONE);
            conversationNameText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            conversationNameText.addMouseListener(MouseListener.mouseDoubleClickAdapter(mouseEvent -> showConversationDropDown()));
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

            conversationDropDown = new MenuManager();
            conversationDropDown.setRemoveAllWhenShown(true);
            conversationDropDown.addMenuListener(this::fillConversationDropDown);

            ToolBarManager conversationToolBarManager = new ToolBarManager(SWT.FLAT);
            contributeConversationActions(conversationToolBarManager);
            conversationToolBarManager.createControl(this);
        }

        updateContext(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        toolBarManager.dispose();
        contextDropDown.dispose();
        settingsDropDown.dispose();
        scopeDropDown.dispose();
    }

    private void updateContext(@Nullable AIContextSettings completionSettings) {
        if (completionSettings != null) {
            this.contextName.setText(completionSettings.getDataSourceContainer().getName());
            this.contextIcon.setImage(DBeaverIcons.getImage(completionSettings.getDataSourceContainer().getDriver().getIcon()));
        } else {
            this.contextName.setText("No connection");
            this.contextIcon.setImage(DBeaverIcons.getImage(DBIcon.DATABASE_DEFAULT));
        }

        updateActions();
    }

    @Nullable
    private AIChatConversation findConversation(@NotNull String name) {
        for (AIChatConversation conversation : contextConversations) {
            if (conversation.getCaption().equals(name)) {
                return conversation;
            }
        }
        return null;
    }

    private void updateConversation(@NotNull AIChatConversation conversation) throws DBException {
        contextConversations.clear();
        contextConversations.addAll(chat.listConversations());

        DBPImage convIcon = getConversationIcon(conversation);
        conversationIcon.setImage(DBeaverIcons.getImage(convIcon));
        conversationNameText.setText(conversation.getCaption());
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
        Control[] ccc = contextComposite.getChildren();
        showDropDown(ccc[ccc.length - 1], 0, contextDropDown);
    }

    private void showConversationDropDown() {
        Control[] ccc = conversationComposite.getChildren();
        showDropDown(ccc[ccc.length - 1], 0, conversationDropDown);
    }

    private void showScopeDropDown() {
        ToolBar toolBar = toolBarManager.getControl();
        showDropDown(toolBar, toolBar.getItem(0).getBounds().width / 2, scopeDropDown);
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
            manager.add(new EmptyAction("No active connections"));
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
            manager.add(new Action("No connection", DBeaverIcons.getImageDescriptor(DBIcon.DATABASE_DEFAULT)) {
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

    private void fillConversationDropDown(@NotNull IMenuManager manager) {
        LocalDate today = LocalDate.now();

        for (AIChatConversation conversion : contextConversations.stream()
            .sorted((o1, o2) -> o2.getTime().compareTo(o1.getTime())).toList()
        ) {
            LocalDateTime msgTime = conversion.getTime();
            manager.add(new Action(
                conversion.getCaption() + " - " + msgTime.format(
                        msgTime.toLocalDate().equals(today) ? CONV_TIME_FORMAT : CONV_DATE_TIME_FORMAT),
                DBeaverIcons.getImageDescriptor(getConversationIcon(conversion))
            ) {
                @Override
                public void run() {
                    try {
                        chat.selectConversation(conversion);
                    } catch (Exception e) {
                        DBWorkbench.getPlatformUI().showError(
                            "Error selecting conversation",
                            "Could not select conversation: " + e.getMessage(),
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
            return;
        }
        manager.add(new EmptyAction("Configure AI context"));
        manager.add(new Separator());

        manager.add(new EmptyAction("Applies to"));
        manager.add(new ChangeContextLevelAction(true));
        manager.add(new ChangeContextLevelAction(false));

        manager.add(new Separator());
        DBPDataSourceContainer dsContainer = chat.getDataSourceContainer();
        DBCExecutionContext executionContext = dsContainer == null ? null : chat.getExecutionContext(dsContainer);
        if (executionContext == null) {
            manager.add(new EmptyAction(
                dsContainer == null ?
                    "No database connection selected" :
                    (dsContainer.isConnecting() ?
                    "Database is being connected..." :
                    "Database is not connected")
                    ));
            return;
        }

        manager.add(new EmptyAction("Metadata sent to AI"));
        DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();
        boolean showSchemas = false;
        boolean showCatalogs = false;
        if (contextDefaults != null) {
            showSchemas = contextDefaults.getDefaultSchema() != null || contextDefaults.supportsSchemaChange();
            showCatalogs = contextDefaults.getDefaultCatalog() != null || contextDefaults.supportsCatalogChange();
        }

        for (AIDatabaseScope scope : AIDatabaseScope.values()) {
            if ((scope == AIDatabaseScope.CURRENT_SCHEMA && !showSchemas) ||
                scope == AIDatabaseScope.CURRENT_DATABASE && !showCatalogs
            ) {
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

    private void updateActions() {
        boolean busy = chat.isBusy();
        changeScopeAction.setEnabled(!busy && chat.getCompletionSettings() != null);
        addConversationAction.setEnabled(!busy);
        if (deleteConversationAction != null) {
            deleteConversationAction.setEnabled(!busy);
        }
    }

    private void contributeContextActions(@NotNull IContributionManager manager) {
        changeScopeAction = new Action(AIChatMessages.ai_chat_change_scope_label, DBeaverIcons.getImageDescriptor(UIIcon.FILTER_CONFIG)) {
            @Override
            public void run() {
                showScopeDropDown();
            }
        };

        manager.add(changeScopeAction);
        manager.add(new Action(AIChatMessages.ai_chat_settings_label, IAction.AS_DROP_DOWN_MENU) {
            {
                setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
            }

            @Override
            public void run() {
                if (DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                    AIUIUtils.showPreferences(getShell());
                } else {
                    showDropDown(toolBarManager.getControl(), toolBarManager.getControl().getSize().x / 2, settingsDropDown);
                }
            }

            @NotNull
            @Override
            public IMenuCreator getMenuCreator() {
                return new MenuCreator(widget -> settingsDropDown);
            }
        });
    }

    private void contributeSettingActions(@NotNull IContributionManager manager) {
        manager.add(new EmptyAction("Messages"));
        manager.add(new SettingsToggleAction("Show message time", AIConstants.AI_CHAT_SHOW_MESSAGE_TIME));
        manager.add(new SettingsToggleAction("Show time spent", AIConstants.AI_CHAT_SHOW_TIME_SPENT));
        manager.add(new SettingsToggleAction("Show tokens spent", AIConstants.AI_CHAT_SHOW_TOKENS_SPENT));
        manager.add(new Separator());
        manager.add(new EmptyAction("Chat"));
        manager.add(new SettingsToggleAction("Show total tokens spent", AIConstants.AI_CHAT_SHOW_TOTAL_TOKENS_SPENT));
    }

    private void contributeConversationActions(@NotNull IContributionManager manager) {
        addConversationAction = new Action("New conversation", DBeaverIcons.getImageDescriptor(UIIcon.ADD)) {
            @Override
            public void run() {
                String name = createNewConversationName();
                try {
                    chat.selectConversation(chat.createEmptyConversation(name, chat.getDataSourceContainer()));
                    chat.setFocusOnPrompt();
                } catch (Exception e) {
                    DBWorkbench.getPlatformUI().showError(
                        "Error creating conversation",
                        "Could not create new conversation: " + e.getMessage(),
                        e
                    );
                }
            }
        };
        manager.add(addConversationAction);
        if (chat.getChatSession().getStorage().canPersist()) {
            deleteConversationAction = new Action("Delete conversation", DBeaverIcons.getImageDescriptor(UIIcon.DELETE)) {
                @Override
                public void run() {
                    if (DBWorkbench.getPlatformUI().confirmAction(
                        "Delete conversation",
                        "Are you sure you want to delete conversation '" + chat.getActiveConversation().getCaption() + "'?"
                    )) {
                        try {
                            chat.removeActiveConversation();
                        } catch (Exception e) {
                            DBWorkbench.getPlatformUI().showError(
                                "Error deleting conversation",
                                "Could not delete conversation: " + e.getMessage(),
                                e
                            );
                        }
                    }
                }
            };
            manager.add(deleteConversationAction);
        }
    }

    @NotNull
    private String createNewConversationName() {
        String baseName = AIChatMessages.ai_chat_default_conversation_name;
        String name = baseName;
        for (int i = 1; ; i++) {
            if (findConversation(name) == null) {
                break;
            }
            name = baseName + " " + i;
        }
        return name;
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
            super(dataSourceContext ? "This connection" : "This conversation", Action.AS_RADIO_BUTTON);
            this.dataSourceContext = dataSourceContext;

            boolean isDataSourceSettings = chat.getCompletionSettings() instanceof AICompletionSettings;
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
                newSettings = new AICompletionSettings(dataSourceContainer);
            } else {
                newSettings = new AIChatConversationSettings(chat.getChatSession(), chat.getActiveConversation());
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
            super(getScopeTitle(settings, scope, dsContainer, contextDefaults), AS_RADIO_BUTTON);
            this.settings = settings;
            this.scope = scope;
            setChecked(settings.getScope() == scope);
        }

        @NotNull
        private static String getScopeTitle(
            @NotNull AIContextSettings settings,
            @NotNull AIDatabaseScope scope,
            @NotNull DBPDataSourceContainer dsContainer,
            @Nullable DBCExecutionContextDefaults<?, ?> contextDefaults
        ) {
            String title = scope.getTitle();
            return switch (scope) {
                case CURRENT_SCHEMA -> {
                    DBSSchema defaultSchema = contextDefaults == null ? null : contextDefaults.getDefaultSchema();
                    yield title + " (" + (defaultSchema == null ?
                        "No schema selected" :
                        DBUtils.getObjectFullName(defaultSchema, DBPEvaluationContext.UI)) + ")";
                }
                case CURRENT_DATABASE -> {
                    DBSCatalog defaultDatabase = contextDefaults == null ? null : contextDefaults.getDefaultCatalog();
                    yield title + " (" + (defaultDatabase == null ?
                        "No database selected" :
                        DBUtils.getObjectFullName(defaultDatabase, DBPEvaluationContext.UI)) + ")";
                }
                case CURRENT_DATASOURCE -> "All objects (" + dsContainer.getName() + ")";
                case CUSTOM -> title + " ..." + (settings.getScope() == scope ?
                    "(" + (ArrayUtils.isEmpty(settings.getCustomObjectIds()) ? "Empty" : settings.getCustomObjectIds().length) + ")" : "");
                default -> title;
            };
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
            AIChatUtils.chooseCustomScope(
                getShell(),
                settings,
                container,
                chat::getExecutionContext,
                chat.getActiveConversation()
            );
        }
    }

}
