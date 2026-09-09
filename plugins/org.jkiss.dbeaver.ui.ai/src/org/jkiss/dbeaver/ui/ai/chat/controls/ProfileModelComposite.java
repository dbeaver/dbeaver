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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.*;
import org.jkiss.dbeaver.model.ai.engine.AIEngine;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.registry.AISettingsEventListener;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.AIUIUtils;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class ProfileModelComposite extends Composite {
    private static final Log log = Log.getLog(ProfileModelComposite.class);
    private static final int MIN_MODEL_WIDTH = 80;
    private static final int MIN_CHAT_WIDTH = 240;

    private final AIChatControl chat;
    private final ToolBar profileBar;
    private final ToolItem profileItem;
    private final ToolBar modelBar;
    private final ToolItem modelItem;
    private final Map<ProfileKey, List<AIModel>> modelsByProfile = new HashMap<>();
    private String profileText;
    private String modelText;
    private Menu menu;
    private AbstractJob modelLoadJob;

    ProfileModelComposite(@NotNull AIChatControl chat, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.chat = chat;
        setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        profileBar = new ToolBar(this, SWT.FLAT | SWT.RIGHT);
        profileItem = new ToolItem(profileBar, SWT.DROP_DOWN);
        profileItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> showProfiles()));
        addAccessibleName(profileBar, AIChatMessagesUI.ai_chat_profile_label, profileItem);

        modelBar = new ToolBar(this, SWT.FLAT | SWT.RIGHT);
        modelItem = new ToolItem(modelBar, SWT.DROP_DOWN);
        modelItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> showModels(false)));
        addAccessibleName(modelBar, AIChatMessagesUI.ai_chat_model_label, modelItem);

        setLayout(new SelectorLayout());

        AIChatListener chatListener = new AIChatListener() {
            @Override
            public void conversationChanged(@NotNull AIChatConversation conversation) {
                refreshAsync(true);
            }

            @Override
            public void conversationProfileChanged(@NotNull AIChatConversation conversation) {
                refreshAsync(true);
            }

            @Override
            public void busyChanged(boolean busy) {
                refreshAsync(false);
            }
        };
        AISettingsEventListener settingsListener = registry -> UIUtils.asyncExec(() -> {
            if (!isDisposed()) {
                modelsByProfile.keySet().removeIf(key -> {
                    AIConfigurationProfile profile = registry.getSettings().getConfigurationOrNull(key.profileId());
                    return profile == null || !profile.getEngineId().equals(key.engineId());
                });
                refresh();
            }
        });
        chat.getChatSession().addListener(chatListener);
        AISettingsManager.getInstance().addChangedListener(settingsListener);
        addDisposeListener(event -> {
            cancelModelLoading();
            if (menu != null) {
                menu.dispose();
            }
            chat.getChatSession().removeListener(chatListener);
            AISettingsManager.getInstance().removeChangedListener(settingsListener);
        });
        refresh();
    }

    private void refreshAsync(boolean cancelLoading) {
        UIUtils.asyncExec(() -> {
            if (!isDisposed()) {
                if (cancelLoading) {
                    cancelModelLoading();
                }
                refresh();
            }
        });
    }

    private void refresh() {
        AISettings settings = AISettingsManager.getStaticSettings();
        AIConfigurationProfile profile = getProfile();
        if (profile != null && !chat.isBusy()) {
            AIConfigurationProfile current = settings.getConfigurationOrNull(profile.getProfileId());
            if (current == null) {
                current = settings.getDefaultConfigurationOrNull();
            }
            // preferences can replace or remove the profile object held by a conversation
            if (current != profile) {
                chat.setConversationProfile(chat.getActiveConversation(), current);
                profile = current;
            }
        }
        profileText = profile == null ? AIChatMessagesUI.ai_chat_profile_not_configured : profile.getProfileName();
        String profileTip = AIChatMessagesUI.ai_chat_profile_label + ": " + profileText;
        modelText = AIChatMessagesUI.ai_chat_model_not_configured;
        boolean modelSelectionSupported = false;
        profileItem.setImage(DBeaverIcons.getImage(AIIcons.AI));
        if (profile != null) {
            try {
                profileItem.setImage(DBeaverIcons.getImage(profile.getEngineDescriptor().getIcon()));
                profileTip += "\n" + profile.getEngineDescriptor().getLabel();
                AIEngineProperties configuration = profile.getConfiguration();
                modelSelectionSupported = configuration.isModelSelectionSupported();
                String model = configuration.getModelDisplayName();
                if (!CommonUtils.isEmpty(model)) {
                    modelText = model;
                } else if (!modelSelectionSupported) {
                    modelText = AIChatMessagesUI.ai_chat_model_unavailable;
                }
            } catch (DBException e) {
                log.debug("Error reading AI profile", e);
            }
        }
        profileItem.setToolTipText(profileTip);
        profileItem.setEnabled(!chat.isBusy());
        modelItem.setEnabled(modelSelectionSupported && !chat.isBusy() && modelLoadJob == null && canConfigure());
        if (modelLoadJob != null) {
            modelText = AIChatMessagesUI.ai_chat_model_loading;
        }
        modelItem.setToolTipText(AIChatMessagesUI.ai_chat_model_label + ": " + modelText);
        boolean visible = settings.getProperty(AIConstants.AI_CHAT_SHOW_PROFILE_AND_MODEL, true);
        GridData data = (GridData) getLayoutData();
        boolean visibilityChanged = data.exclude == visible;
        data.exclude = !visible;
        setVisible(visible);
        if (visibilityChanged) {
            getParent().layout(true, true);
        } else {
            layout(true);
        }
    }

    @Nullable
    private AIConfigurationProfile getProfile() {
        AIConfigurationProfile profile = chat.getActiveConversation().getProfile();
        return profile == null ? AISettingsManager.getStaticSettings().getDefaultConfigurationOrNull() : profile;
    }

    private boolean canConfigure() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER);
    }

    private void showProfiles() {
        if (chat.isBusy()) {
            return;
        }
        AIConfigurationProfile[] profiles = AISettingsManager.getStaticSettings().getConfigurations();
        if (profiles.length == 0) {
            AIUIUtils.showPreferences(getShell());
            return;
        }
        Menu profileMenu = createMenu(profileBar);
        Arrays.sort(profiles, Comparator.comparing(AIConfigurationProfile::getProfileName, String.CASE_INSENSITIVE_ORDER));
        for (AIConfigurationProfile profile : profiles) {
            MenuItem item = new MenuItem(profileMenu, SWT.RADIO);
            item.setText(profile.getProfileName().replace("&", "&&"));
            item.setSelection(profile == getProfile());
            try {
                item.setImage(DBeaverIcons.getImage(profile.getEngineDescriptor().getIcon()));
            } catch (DBException e) {
                log.debug(e);
            }
            item.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                if (item.getSelection() && !chat.isBusy()) {
                    cancelModelLoading();
                    chat.setConversationProfile(chat.getActiveConversation(), profile);
                }
            }));
        }
        showMenu(profileBar);
    }

    private void showModels(boolean forceRefresh) {
        AIConfigurationProfile profile = getProfile();
        if (profile == null || chat.isBusy() || modelLoadJob != null || !canConfigure()) {
            return;
        }
        try {
            if (!profile.getConfiguration().isModelSelectionSupported()) {
                return;
            }
        } catch (DBException e) {
            log.debug("Error reading AI profile", e);
            return;
        }
        ProfileKey key = new ProfileKey(profile.getProfileId(), profile.getEngineId());
        List<AIModel> cachedModels = modelsByProfile.get(key);
        if (!forceRefresh && cachedModels != null) {
            showModelMenu(profile, cachedModels);
            return;
        }
        AIChatConversation conversation = chat.getActiveConversation();
        modelLoadJob = new AbstractJob(AIChatMessagesUI.ai_chat_model_loading) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                List<AIModel> models = List.of();
                Exception error = null;
                try {
                    profile.resolveSecrets();
                    try (AIEngine<?> engine = profile.getEngineDescriptor().createEngineInstance(profile)) {
                        models = engine.getModels(monitor);
                    }
                } catch (Exception e) {
                    error = e;
                }
                List<AIModel> result = models;
                Exception failure = error;
                UIUtils.asyncExec(() -> {
                    if (isDisposed() || modelLoadJob != this) {
                        return;
                    }
                    modelLoadJob = null;
                    refresh();
                    if (monitor.isCanceled()) {
                        return;
                    }
                    AIConfigurationProfile current = AISettingsManager.getStaticSettings().getConfigurationOrNull(key.profileId());
                    if (failure == null && current != null && current.getEngineId().equals(key.engineId())) {
                        modelsByProfile.put(key, result);
                    }
                    if (conversation != chat.getActiveConversation() || profile != getProfile()
                        || chat.isBusy() || !isVisible()) {
                        return;
                    }
                    if (failure != null) {
                        DBWorkbench.getPlatformUI().showError(AIUIMessages.model_selector_refresh_error_title, null, failure);
                    } else {
                        showModelMenu(profile, result);
                    }
                });
                return Status.OK_STATUS;
            }
        };
        refresh();
        modelLoadJob.schedule();
    }

    private void showModelMenu(@NotNull AIConfigurationProfile profile, @NotNull List<AIModel> models) {
        Menu modelMenu = createMenu(modelBar);
        try {
            AIEngineProperties properties = profile.getConfiguration();
            if (!properties.isModelSelectionSupported()) {
                return;
            }
            String selected = properties.getModel();
            Map<String, AIModel> availableModels = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            models.stream()
                .filter(model -> model.features().contains(AIModelFeature.CHAT))
                .forEach(model -> availableModels.putIfAbsent(model.name(), model));
            if (!CommonUtils.isEmpty(selected)) {
                availableModels.putIfAbsent(
                    selected, new AIModel(selected, properties.getContextWindowSize(), Set.of(AIModelFeature.CHAT)));
            }
            for (AIModel model : availableModels.values()) {
                String name = model.name();
                MenuItem item = new MenuItem(modelMenu, SWT.RADIO);
                item.setText(name.replace("&", "&&"));
                item.setSelection(name.equals(selected));
                item.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                    if (item.getSelection() && !chat.isBusy() && profile == getProfile() && canConfigure()) {
                        try {
                            AIEngineProperties configuration = profile.getConfiguration();
                            if (configuration.isModelSelectionSupported() && !name.equals(configuration.getModel())) {
                                configuration.selectModel(model);
                                AISettingsManager.getInstance().saveSettings();
                            }
                        } catch (DBException e) {
                            DBWorkbench.getPlatformUI().showError(AIChatMessagesUI.ai_chat_model_change_error, null, e);
                        }
                    }
                }));
            }
        } catch (DBException e) {
            log.debug(e);
        }
        if (modelMenu.getItemCount() > 0) {
            new MenuItem(modelMenu, SWT.SEPARATOR);
        }
        MenuItem refreshItem = new MenuItem(modelMenu, SWT.PUSH);
        refreshItem.setText(AIUIMessages.gpt_preference_page_refresh_models);
        refreshItem.setImage(DBeaverIcons.getImage(UIIcon.REFRESH));
        refreshItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> showModels(true)));
        showMenu(modelBar);
    }

    private void cancelModelLoading() {
        if (modelLoadJob != null) {
            modelLoadJob.cancel();
            modelLoadJob = null;
        }
    }

    @NotNull
    private Menu createMenu(@NotNull Control control) {
        if (menu != null) {
            menu.dispose();
        }
        menu = new Menu(control);
        return menu;
    }

    private void showMenu(@NotNull Control control) {
        menu.setLocation(control.toDisplay(0, control.getSize().y));
        menu.setVisible(true);
    }

    private void addAccessibleName(@NotNull ToolBar bar, @NotNull String label, @NotNull ToolItem item) {
        bar.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(@NotNull AccessibleEvent event) {
                event.result = item.getToolTipText() == null ? label : item.getToolTipText();
            }
        });
    }

    private class SelectorLayout extends Layout {
        @NotNull
        @Override
        protected Point computeSize(@NotNull Composite composite, int widthHint, int heightHint, boolean flushCache) {
            restoreText();
            int height = Math.max(profileBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y, modelBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
            return new Point(widthHint == SWT.DEFAULT ? MIN_CHAT_WIDTH : widthHint, heightHint == SWT.DEFAULT ? height : heightHint);
        }

        @Override
        protected void layout(@NotNull Composite composite, boolean flushCache) {
            restoreText();
            Rectangle area = composite.getClientArea();
            int available = Math.max(0, area.width);
            int profileWidth = Math.min(profileBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, Math.max(0, available - MIN_MODEL_WIDTH));
            int modelWidth = Math.min(modelBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, available - profileWidth);
            fitText(profileItem, profileText, profileWidth);
            fitText(modelItem, modelText, modelWidth);
            profileBar.setBounds(area.x, area.y, profileWidth, area.height);
            modelBar.setBounds(area.x + profileWidth, area.y, modelWidth, area.height);
        }

        private void restoreText() {
            profileItem.setText(profileText.replace("&", "&&"));
            modelItem.setText(modelText.replace("&", "&&"));
        }

        private void fitText(@NotNull ToolItem item, @NotNull String text, int width) {
            GC gc = new GC(item.getParent());
            try {
                int available = width - (item.getBounds().width - gc.textExtent(text).x);
                if (gc.textExtent(text).x <= available) {
                    return;
                }
                int end = text.length();
                while (end > 0 && gc.textExtent(text.substring(0, end) + "...").x > available) {
                    end = text.offsetByCodePoints(end, -1);
                }
                item.setText((text.substring(0, end) + "...").replace("&", "&&"));
            } finally {
                gc.dispose();
            }
        }
    }

    private record ProfileKey(@NotNull String profileId, @NotNull String engineId) {
    }
}
