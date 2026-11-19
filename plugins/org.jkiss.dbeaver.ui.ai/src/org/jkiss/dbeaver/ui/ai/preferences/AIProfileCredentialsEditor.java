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
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

final class AIProfileCredentialsEditor {
    private final Map<AIConfigurationProfile, AIConfigurationProfile> personalProfiles = new LinkedHashMap<>();
    private final Map<AIConfigurationProfile, Map<String, String>> changes = new LinkedHashMap<>();
    private final Map<AIConfigurationProfile, DBException> loadErrors = new LinkedHashMap<>();
    private boolean profilesLoaded;

    void loadProfiles(@NotNull AIConfigurationProfile[] profiles, boolean canManageConfiguration)
        throws InvocationTargetException, InterruptedException {
        Map<AIConfigurationProfile, DBException> errors = new LinkedHashMap<>();
        UIUtils.getDialogRunnableContext().run(true, true, monitor -> {
            for (AIConfigurationProfile profile : profiles) {
                if (!profile.isAvailable()) {
                    continue;
                }
                try {
                    if (canManageConfiguration) {
                        profile.resolveSecrets();
                    } else {
                        AIConfigurationProfile personalProfile = new AIConfigurationProfile();
                        personalProfile.setProfileId(profile.getProfileId());
                        personalProfile.setProfileName(profile.getProfileName());
                        personalProfile.setEngineId(profile.getEngineId());
                        AIEngineProperties configuration = profile.getConfiguration();
                        personalProfile.setConfiguration(AISettingsManager.READ_PROPS_GSON.fromJson(
                            AISettingsManager.READ_PROPS_GSON.toJson(configuration), configuration.getClass()));
                        personalProfile.useUserCredentialScope(profile);
                        AIUserProfileCredentials.resolve(personalProfile);
                        personalProfiles.put(profile, personalProfile);
                    }
                } catch (DBException e) {
                    errors.put(profile, e);
                }
            }
        });
        loadErrors.clear();
        loadErrors.putAll(errors);
        profilesLoaded = true;
    }

    void createControl(@NotNull Composite parent, @NotNull AIConfigurationProfile profile) throws DBException {
        checkLoaded(profile);
        AIConfigurationProfile personal = personalProfiles.get(profile);
        if (personal == null) {
            return;
        }
        Map<String, String> saved = personal.getResolvedUserCredentials();
        if (saved == null) {
            return;
        }
        Map<String, String> edits = changes.getOrDefault(profile, Map.of());
        for (var property : AIUserProfileCredentials.getCredentialProperties(personal.getConfiguration(), true)) {
            String id = property.getId();
            Text editor = UIUtils.createLabelText(parent, property.getDisplayName(),
                CommonUtils.notEmpty(edits.getOrDefault(id, saved.get(id))),
                SWT.BORDER | (property.isPassword() ? SWT.PASSWORD : SWT.NONE));
            editor.addModifyListener(event -> changes.computeIfAbsent(profile, key -> new LinkedHashMap<>())
                .put(id, editor.getText()));
        }
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

    void saveChanges() throws DBException {
        for (var entry : personalProfiles.entrySet()) {
            AIConfigurationProfile profile = entry.getKey();
            AIConfigurationProfile personal = entry.getValue();
            if (changes.containsKey(profile)) {
                AIUserProfileCredentials.save(profile, changes.get(profile));
                personal.resolveSecrets();
            } else {
                AIUserProfileCredentials.saveChanges(personal);
            }
            profile.resolveSecrets();
        }
        changes.clear();
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
