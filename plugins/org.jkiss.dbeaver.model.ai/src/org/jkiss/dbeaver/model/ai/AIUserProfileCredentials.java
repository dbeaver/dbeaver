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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIAccountProperties;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.auth.AuthProperty;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretObject;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AIUserProfileCredentials {
    private static final String SECRET_ID_PREFIX = "ai.profile.";
    private static final String SECRET_OBJECT_TYPE = "aiProfile";

    private AIUserProfileCredentials() {
    }

    public static boolean isAvailable() {
        return DBWorkbench.isDistributed() && !DBWorkbench.getPlatform().getApplication().isHeadlessMode();
    }

    public static boolean isUserProfile(@NotNull AIConfigurationProfile profile) {
        return isAvailable() && !profile.isGlobal();
    }

    @NotNull
    public static List<ObjectPropertyDescriptor> getCredentialProperties(@NotNull AIEngineProperties properties) {
        return getCredentialProperties(properties, false);
    }

    @NotNull
    public static List<ObjectPropertyDescriptor> getCredentialProperties(
        @NotNull AIEngineProperties properties,
        boolean localizedNames
    ) {
        return ObjectAttributeDescriptor.extractAnnotations(null, properties.getClass(), null, null, localizedNames).stream()
            .filter(property -> property.isPassword() || property.getAnnotation(AuthProperty.class) != null)
            .filter(property -> !AIAccountProperties.ACCOUNT_CREDENTIAL_PROPERTY_IDS.contains(property.getId()))
            .filter(property -> properties.isUserCredentialProperty(property.getId()))
            .toList();
    }

    @NotNull
    public static Map<String, String> load(@NotNull AIConfigurationProfile profile) throws DBException {
        return load(profile, DBSSecretController.getGlobalSecretController());
    }

    @NotNull
    static Map<String, String> load(
        @NotNull AIConfigurationProfile profile,
        @NotNull DBSSecretController controller
    ) throws DBException {
        checkUserProfile(profile);
        Map<String, String> credentials = new LinkedHashMap<>();
        for (ObjectPropertyDescriptor property : getCredentialProperties(profile.getConfiguration())) {
            credentials.put(property.getId(), CommonUtils.notEmpty(controller.getPrivateSecretValue(getSecretId(profile, property))));
        }
        return credentials;
    }

    public static void save(@NotNull AIConfigurationProfile profile, @NotNull Map<String, String> credentials) throws DBException {
        save(profile, credentials, DBSSecretController.getGlobalSecretController());
    }

    static void save(
        @NotNull AIConfigurationProfile profile,
        @NotNull Map<String, String> credentials,
        @NotNull DBSSecretController controller
    ) throws DBException {
        checkUserProfile(profile);
        List<ObjectPropertyDescriptor> properties = getCredentialProperties(profile.getConfiguration());
        for (String propertyId : credentials.keySet()) {
            if (properties.stream().noneMatch(property -> property.getId().equals(propertyId))) {
                throw new DBException("Invalid AI credential property " + propertyId);
            }
        }
        for (ObjectPropertyDescriptor property : properties) {
            if (credentials.containsKey(property.getId())) {
                String value = credentials.get(property.getId());
                controller.setPrivateSecretValue(
                    new ProfileSecretObject(profile.getProfileId()),
                    new DBSSecretValue(
                        getSecretId(profile, property),
                        profile.getProfileName() + ": " + property.getDisplayName(),
                        CommonUtils.isEmpty(value) ? null : value
                    )
                );
            }
        }
        controller.flushChanges();
    }

    @NotNull
    public static AIEngineProperties copyWithoutCredentials(@NotNull AIEngineProperties properties) throws DBException {
        AIEngineProperties copy = AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.READ_PROPS_GSON.toJson(properties), properties.getClass());
        apply(copy, Map.of());
        copy.clearUserCredentials();
        if (copy instanceof AIAccountProperties accountProperties) {
            accountProperties.clearAccountTokens();
        }
        return copy;
    }

    public static void resolve(@NotNull AIConfigurationProfile profile) throws DBException {
        Map<String, String> credentials = load(profile);
        apply(profile.getConfiguration(), credentials);
        profile.setResolvedUserCredentials(credentials);
    }

    public static void saveChanges(@NotNull AIConfigurationProfile profile) throws DBException {
        Map<String, String> resolvedCredentials = profile.getResolvedUserCredentials();
        if (resolvedCredentials == null) {
            return;
        }
        AIEngineProperties properties = profile.getConfiguration();
        Map<String, String> currentCredentials = new LinkedHashMap<>();
        Map<String, String> updates = new LinkedHashMap<>();
        try {
            for (ObjectPropertyDescriptor property : getCredentialProperties(properties)) {
                String value = CommonUtils.toString(property.readValue(properties, null, false));
                currentCredentials.put(property.getId(), value);
                if (!value.equals(resolvedCredentials.get(property.getId()))) {
                    updates.put(property.getId(), value);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new DBException("Error reading AI profile credentials", e);
        }
        if (!updates.isEmpty()) {
            save(profile, updates);
            profile.setResolvedUserCredentials(currentCredentials);
        }
    }

    static void apply(@NotNull AIEngineProperties properties, @NotNull Map<String, String> credentials) throws DBException {
        try {
            for (ObjectPropertyDescriptor property : getCredentialProperties(properties)) {
                property.writeValue(properties, CommonUtils.notEmpty(credentials.get(property.getId())));
            }
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new DBException("Error applying AI profile credentials", e);
        }
    }

    private static void checkUserProfile(@NotNull AIConfigurationProfile profile) throws DBException {
        if (profile.isGlobal()) {
            throw new DBException("AI profile does not use user credentials");
        }
    }

    @NotNull
    private static String getSecretId(@NotNull AIConfigurationProfile profile, @NotNull ObjectPropertyDescriptor property) {
        return SECRET_ID_PREFIX + profile.getProfileId() + "." + property.getId();
    }

    private record ProfileSecretObject(@NotNull String secretObjectId) implements DBSSecretObject {
        @NotNull
        @Override
        public String getProjectId() {
            return "";
        }

        @NotNull
        @Override
        public String getSecretObjectId() {
            return secretObjectId;
        }

        @NotNull
        @Override
        public String getSecretObjectType() {
            return SECRET_OBJECT_TYPE;
        }
    }
}
