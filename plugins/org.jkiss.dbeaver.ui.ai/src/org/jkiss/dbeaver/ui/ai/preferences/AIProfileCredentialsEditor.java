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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIUserProfileCredentials;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.LinkedHashMap;
import java.util.Map;

final class AIProfileCredentialsEditor {
    // users edit copies, so cancelled changes do not reach the profiles used by the chat
    private final Map<AIConfigurationProfile, AIConfigurationProfile> personalProfiles = new LinkedHashMap<>();
    private final Map<AIConfigurationProfile, Map<String, String>> loadedCredentials = new LinkedHashMap<>();
    private final Map<AIConfigurationProfile, Map<String, String>> editedCredentials = new LinkedHashMap<>();
    private final Map<AIConfigurationProfile, DBException> loadErrors = new LinkedHashMap<>();

    void loadProfile(@NotNull AIConfigurationProfile profile, boolean canManageConfiguration) {
        try {
            AIConfigurationProfile target = canManageConfiguration ? profile : copyProfile(profile);
            if (AIUserProfileCredentials.isUserProfile(target)) {
                AIUserProfileCredentials.load(target);
                loadedCredentials.put(profile, AIUserProfileCredentials.getCredentials(target.getConfiguration()));
            } else {
                target.resolveSecrets();
            }
            if (target != profile) {
                personalProfiles.put(profile, target);
            }
        } catch (DBException e) {
            loadErrors.put(profile, e);
        }
    }

    void createControl(@NotNull Composite parent, @NotNull AIConfigurationProfile profile) throws DBException {
        checkLoaded(profile);
        AIEngineProperties properties = getConfiguration(profile);
        Map<String, String> credentials = getEditedCredentials(profile, properties);
        for (var property : AIUserProfileCredentials.getCredentialProperties(properties, true)) {
            String id = property.getId();
            Text editor = UIUtils.createLabelText(parent, property.getDisplayName(), credentials.get(id),
                SWT.BORDER | (property.isPassword() ? SWT.PASSWORD : SWT.NONE));
            editor.addModifyListener(event -> credentials.put(id, editor.getText()));
        }
    }

    @NotNull
    private Map<String, String> getEditedCredentials(
        @NotNull AIConfigurationProfile profile,
        @NotNull AIEngineProperties properties
    ) throws DBException {
        Map<String, String> credentials = editedCredentials.get(profile);
        if (credentials == null) {
            credentials = AIUserProfileCredentials.getCredentials(properties);
            editedCredentials.put(profile, credentials);
        }
        return credentials;
    }

    @NotNull
    AIEngineProperties getConfiguration(@NotNull AIConfigurationProfile profile) throws DBException {
        checkLoaded(profile);
        AIConfigurationProfile personal = personalProfiles.get(profile);
        if (personal == null) {
            throw new DBException("Personal AI profile credentials are not loaded");
        }
        return personal.getConfiguration();
    }

    void saveChanges(@NotNull AIConfigurationProfile[] profiles) throws DBException {
        for (AIConfigurationProfile profile : profiles) {
            // only loaded personal profiles have saved values to compare with
            Map<String, String> loaded = loadedCredentials.get(profile);
            if (loaded == null) {
                continue;
            }
            AIConfigurationProfile target = personalProfiles.getOrDefault(profile, profile);
            Map<String, String> edited = editedCredentials.get(profile);
            if (edited != null) {
                AIUserProfileCredentials.setCredentials(target.getConfiguration(), edited);
            }
            Map<String, String> credentials = AIUserProfileCredentials.getCredentials(target.getConfiguration());
            // unchanged keys may have been updated meanwhile, e.g. a refreshed account token
            Map<String, String> changes = new LinkedHashMap<>(credentials);
            changes.entrySet().removeIf(credential -> credential.getValue().equals(loaded.get(credential.getKey())));
            if (!changes.isEmpty()) {
                AIUserProfileCredentials.save(target, changes);
                loadedCredentials.put(profile, credentials);
            }
            if (target != profile) {
                // the chat uses the settings profile, it may still keep older values
                AIUserProfileCredentials.load(profile);
            }
        }
    }

    void checkLoaded(@NotNull AIConfigurationProfile profile) throws DBException {
        DBException error = loadErrors.get(profile);
        if (error != null) {
            throw error;
        }
    }

    @NotNull
    private static AIConfigurationProfile copyProfile(@NotNull AIConfigurationProfile profile) throws DBException {
        AIConfigurationProfile copy = new AIConfigurationProfile();
        copy.setProfileId(profile.getProfileId());
        copy.setProfileName(profile.getProfileName());
        copy.setEngineId(profile.getEngineId());
        AIEngineProperties configuration = profile.getConfiguration();
        copy.setConfiguration(AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.READ_PROPS_GSON.toJson(configuration), configuration.getClass()));
        return copy;
    }
}
