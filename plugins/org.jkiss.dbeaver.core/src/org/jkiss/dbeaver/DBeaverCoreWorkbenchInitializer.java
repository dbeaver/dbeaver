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
package org.jkiss.dbeaver;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.themes.ColorDefinition;
import org.eclipse.ui.internal.themes.ThemeElementDefinition;
import org.eclipse.ui.internal.themes.WorkbenchThemeManager;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionTypes;
import org.jkiss.dbeaver.ui.preferences.PrefPageConnectionsGeneral;
import org.jkiss.dbeaver.ui.preferences.PrefPageTransactions;
import org.jkiss.dbeaver.ui.workbench.WorkbenchUtils;

import java.util.HashMap;
import java.util.Map;

public class DBeaverCoreWorkbenchInitializer implements IWorkbenchWindowInitializer {

    private static final String DBEAVER_COLOR_ID_PREFIX = "org.jkiss.dbeaver.";
    private static final String CUSTOM_COLOR_PREFERENCE_PREFIX = "org.jkiss.dbeaver.ui.customColorOverride.";

    private static final Map<String, String> pendingColorUpdates = new HashMap<>();
    private static boolean themePreferencesInitialized;

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        initializeThemePreferences();
        if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            WorkbenchUtils.removePreferencePages(PrefPageConnectionsGeneral.PAGE_ID + "/" + PrefPageConnectionTypes.PAGE_ID);
            WorkbenchUtils.removePreferencePages(PrefPageConnectionsGeneral.PAGE_ID + "/" + PrefPageTransactions.PAGE_ID);
        }
    }

    private static synchronized void initializeThemePreferences() {
        if (themePreferencesInitialized) {
            return;
        }
        themePreferencesInitialized = true;

        IPreferenceStore store = PrefUtil.getInternalPreferenceStore();
        store.addPropertyChangeListener(event -> recordColorChange(event.getProperty(), event.getNewValue()));
        IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
        eventBroker.subscribe(
            WorkbenchThemeManager.Events.THEME_REGISTRY_RESTYLED,
            event -> {
                clearPendingColorUpdates();
                UIUtils.asyncExec(DBeaverCoreWorkbenchInitializer::restoreUserColors);
            }
        );
        eventBroker.subscribe(WorkbenchThemeManager.Events.THEME_REGISTRY_MODIFIED, event -> synchronizeUserColors());
        UIUtils.asyncExec(DBeaverCoreWorkbenchInitializer::restoreUserColors);
    }

    private static synchronized void recordColorChange(@NotNull String preferenceKey, @NotNull Object value) {
        int colorIdOffset = preferenceKey.indexOf(DBEAVER_COLOR_ID_PREFIX);
        if (colorIdOffset < 0) {
            return;
        }
        String colorId = preferenceKey.substring(colorIdOffset);
        ColorDefinition definition = WorkbenchPlugin.getDefault().getThemeRegistry().findColor(colorId);
        if (definition != null && definition.isEditable()) {
            if (value instanceof String stringValue) {
                pendingColorUpdates.put(colorId, stringValue);
            } else if (value instanceof RGB rgb) {
                pendingColorUpdates.put(
                    colorId,
                    definition.isModifiedByUser() ? StringConverter.asString(rgb) : null
                );
            }
        }
    }

    private static synchronized void synchronizeUserColors() {
        IPreferenceStore store = PrefUtil.getInternalPreferenceStore();
        boolean changed = false;
        for (Map.Entry<String, String> entry : pendingColorUpdates.entrySet()) {
            ColorDefinition definition = WorkbenchPlugin.getDefault().getThemeRegistry().findColor(entry.getKey());
            if (definition == null || !definition.isEditable()) {
                continue;
            }
            String customKey = CUSTOM_COLOR_PREFERENCE_PREFIX + definition.getId();
            if (entry.getValue() == null || entry.getValue().equals(StringConverter.asString(definition.getValue()))) {
                if (!store.isDefault(customKey)) {
                    store.setToDefault(customKey);
                    changed = true;
                }
            } else if (!entry.getValue().equals(store.getString(customKey))) {
                store.setValue(customKey, entry.getValue());
                changed = true;
            }
        }
        for (ColorDefinition definition : WorkbenchPlugin.getDefault().getThemeRegistry().getColors()) {
            String customKey = CUSTOM_COLOR_PREFERENCE_PREFIX + definition.getId();
            if (!pendingColorUpdates.containsKey(definition.getId())
                && !store.isDefault(customKey)
                && !definition.isModifiedByUser()) {
                store.setToDefault(customKey);
                changed = true;
            }
        }
        pendingColorUpdates.clear();
        if (changed) {
            PrefUtil.saveInternalPrefs();
        }
    }

    private static synchronized void clearPendingColorUpdates() {
        pendingColorUpdates.clear();
    }

    private static void restoreUserColors() {
        IPreferenceStore store = PrefUtil.getInternalPreferenceStore();
        ITheme currentTheme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        ColorRegistry colorRegistry = currentTheme.getColorRegistry();

        for (ColorDefinition definition : WorkbenchPlugin.getDefault().getThemeRegistry().getColors()) {
            if (!definition.isEditable() || !definition.getId().startsWith(DBEAVER_COLOR_ID_PREFIX)) {
                continue;
            }
            String customKey = CUSTOM_COLOR_PREFERENCE_PREFIX + definition.getId();
            if (store.isDefault(customKey)) {
                continue;
            }
            definition.appendState(ThemeElementDefinition.State.MODIFIED_BY_USER);
            colorRegistry.put(definition.getId(), PreferenceConverter.getColor(store, customKey));
        }
    }
}
