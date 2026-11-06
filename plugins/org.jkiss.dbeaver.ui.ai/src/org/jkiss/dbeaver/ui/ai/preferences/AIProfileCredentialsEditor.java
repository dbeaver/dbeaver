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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIUserProfileCredentials;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.internal.AIUIMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

final class AIProfileCredentialsEditor {
    private final Map<AIConfigurationProfile, Map<String, String>> savedCredentials = new LinkedHashMap<>();
    private final Map<AIConfigurationProfile, Map<String, String>> changes = new LinkedHashMap<>();
    private final Map<AIConfigurationProfile, DBException> loadErrors = new LinkedHashMap<>();
    private boolean profilesLoaded;

    void loadProfiles(@NotNull AIConfigurationProfile[] profiles, boolean resolveSecrets)
        throws InvocationTargetException, InterruptedException {
        Map<AIConfigurationProfile, Map<String, String>> loaded = new LinkedHashMap<>();
        Map<AIConfigurationProfile, DBException> errors = new LinkedHashMap<>();
        UIUtils.getDialogRunnableContext().run(true, true, monitor -> {
            for (AIConfigurationProfile profile : profiles) {
                try {
                    if (resolveSecrets) {
                        profile.resolveSecrets();
                    }
                    if (!profile.isGlobal()) {
                        loaded.put(profile, AIUserProfileCredentials.load(profile));
                    }
                } catch (DBException e) {
                    errors.put(profile, e);
                }
            }
        });
        savedCredentials.putAll(loaded);
        loadErrors.clear();
        loadErrors.putAll(errors);
        profilesLoaded = true;
    }

    void createControl(@NotNull Composite parent, @NotNull AIConfigurationProfile profile) throws DBException {
        checkLoaded(profile);
        Label description = new Label(parent, SWT.WRAP);
        description.setText(profile.isGlobal()
            ? AIUIMessages.ai_profiles_managed_description
            : AIUIMessages.ai_profiles_personal_description);
        GridData descriptionData = new GridData(GridData.FILL_HORIZONTAL);
        descriptionData.horizontalSpan = 2;
        descriptionData.widthHint = 450;
        description.setLayoutData(descriptionData);
        if (profile.isGlobal() || !savedCredentials.containsKey(profile)) {
            return;
        }
        Map<String, String> credentials = new LinkedHashMap<>(savedCredentials.get(profile));
        credentials.putAll(changes.getOrDefault(profile, Map.of()));
        for (var property : AIUserProfileCredentials.getCredentialProperties(profile.getConfiguration(), true)) {
            Text editor = UIUtils.createLabelText(parent, property.getDisplayName(),
                CommonUtils.notEmpty(credentials.get(property.getId())),
                SWT.BORDER | (property.isPassword() ? SWT.PASSWORD : SWT.NONE));
            editor.addModifyListener(event -> changes.computeIfAbsent(profile, key -> new LinkedHashMap<>())
                .put(property.getId(), editor.getText()));
        }
    }

    @NotNull
    String getStatus(@NotNull AIConfigurationProfile profile) {
        if (loadErrors.containsKey(profile)) {
            return AIUIMessages.ai_profiles_status_unavailable;
        }
        if (profile.isGlobal()) {
            return AIUIMessages.ai_profiles_status_managed;
        }
        Map<String, String> credentials = savedCredentials.get(profile);
        if (credentials == null) {
            return AIUIMessages.ai_profiles_status_loading;
        }
        if (credentials.isEmpty()) {
            return AIUIMessages.ai_profiles_status_not_required;
        }
        return credentials.values().stream().anyMatch(value -> !CommonUtils.isEmpty(value))
            ? AIUIMessages.ai_profiles_status_configured
            : AIUIMessages.ai_profiles_status_not_configured;
    }

    void saveChanges() throws DBException {
        for (var entry : changes.entrySet()) {
            AIUserProfileCredentials.save(entry.getKey(), entry.getValue());
            entry.getKey().resolveSecrets();
            savedCredentials.get(entry.getKey()).putAll(entry.getValue());
        }
        changes.clear();
    }

    void updateCredentials(@NotNull AIConfigurationProfile profile) throws DBException {
        if (profile.isGlobal()) {
            return;
        }
        Map<String, String> credentials = new LinkedHashMap<>();
        var properties = profile.getConfiguration();
        try {
            for (var property : AIUserProfileCredentials.getCredentialProperties(properties)) {
                credentials.put(property.getId(), CommonUtils.toString(property.readValue(properties, null, false)));
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new DBException("Error reading AI profile credentials", e);
        }
        savedCredentials.put(profile, credentials);
    }

    boolean isLoaded() {
        return profilesLoaded;
    }

    void checkLoaded(@NotNull AIConfigurationProfile profile) throws DBException {
        DBException error = loadErrors.get(profile);
        if (error != null) {
            throw error;
        }
    }
}
