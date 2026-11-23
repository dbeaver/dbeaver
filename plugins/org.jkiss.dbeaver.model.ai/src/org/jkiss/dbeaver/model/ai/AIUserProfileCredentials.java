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

/**
 * Credentials of non-global AI profiles in Team Edition.
 * Each user keeps them in private secrets, with the same ids as CloudBeaver.
 */
public final class AIUserProfileCredentials {
    private static final String SECRET_ID_PREFIX = "ai.profile.";
    private static final String SECRET_OBJECT_TYPE = "aiProfile";
    private static final String ACCOUNT_AUTHENTICATION_PROPERTY = "accountAuthentication";

    private AIUserProfileCredentials() {
    }

    public static boolean isAvailable() {
        return DBWorkbench.isDistributed() && !DBWorkbench.getPlatform().getApplication().isHeadlessMode();
    }

    public static boolean isUserProfile(@NotNull AIConfigurationProfile profile) {
        return isAvailable() && !profile.isGlobal();
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
    public static Map<String, String> getCredentials(@NotNull AIEngineProperties properties) throws DBException {
        Map<String, String> credentials = new LinkedHashMap<>();
        for (ObjectPropertyDescriptor property : getCredentialProperties(properties, false)) {
            try {
                credentials.put(property.getId(), CommonUtils.toString(property.readValue(properties, null, false)));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new DBException("Error reading AI credential " + property.getId(), e);
            }
        }
        if (properties instanceof AIAccountProperties account) {
            credentials.put(ACCOUNT_AUTHENTICATION_PROPERTY, Boolean.toString(account.isAccountAuthentication()));
            credentials.put(AIAccountProperties.ACCOUNT_ACCESS_TOKEN_PROPERTY, CommonUtils.notEmpty(account.getAccessToken()));
            credentials.put(AIAccountProperties.ACCOUNT_REFRESH_TOKEN_PROPERTY, CommonUtils.notEmpty(account.getRefreshToken()));
            credentials.put(AIAccountProperties.ACCOUNT_ID_PROPERTY, CommonUtils.notEmpty(account.getAccountId()));
            credentials.put(AIAccountProperties.ACCOUNT_EMAIL_PROPERTY, CommonUtils.notEmpty(account.getAccountEmail()));
            credentials.put(AIAccountProperties.ACCOUNT_EXPIRES_AT_PROPERTY, Long.toString(account.getStoredExpiresAt()));
        }
        return credentials;
    }

    /**
     * Replaces all credentials of the properties, missing values are cleared
     */
    public static void setCredentials(
        @NotNull AIEngineProperties properties,
        @NotNull Map<String, String> credentials
    ) throws DBException {
        for (ObjectPropertyDescriptor property : getCredentialProperties(properties, false)) {
            try {
                property.writeValue(properties, CommonUtils.notEmpty(credentials.get(property.getId())));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new DBException("Error setting AI credential " + property.getId(), e);
            }
        }
        if (properties instanceof AIAccountProperties account) {
            String accountAuthentication = credentials.get(ACCOUNT_AUTHENTICATION_PROPERTY);
            // the shared profile method stays the default until the user picks one
            account.setUserAccountAuthentication(accountAuthentication == null
                ? account.isAccountAuthentication()
                : Boolean.parseBoolean(accountAuthentication));
            account.setAccessToken(credentials.get(AIAccountProperties.ACCOUNT_ACCESS_TOKEN_PROPERTY));
            account.setRefreshToken(credentials.get(AIAccountProperties.ACCOUNT_REFRESH_TOKEN_PROPERTY));
            account.setStoredAccountId(credentials.get(AIAccountProperties.ACCOUNT_ID_PROPERTY));
            account.setStoredAccountEmail(credentials.get(AIAccountProperties.ACCOUNT_EMAIL_PROPERTY));
            account.setStoredExpiresAt(CommonUtils.toLong(credentials.get(AIAccountProperties.ACCOUNT_EXPIRES_AT_PROPERTY)));
        }
    }

    public static void load(@NotNull AIConfigurationProfile profile) throws DBException {
        DBSSecretController controller = DBSSecretController.getGlobalSecretController();
        AIEngineProperties properties = profile.getConfiguration();
        Map<String, String> credentials = new LinkedHashMap<>();
        for (String id : getCredentials(properties).keySet()) {
            credentials.put(id, controller.getPrivateSecretValue(getSecretId(profile, id)));
        }
        setCredentials(properties, credentials);
        if (properties instanceof AIAccountProperties account) {
            account.setAccountTokenPersistence((previousRefreshToken, tokens) -> save(profile));
        }
    }

    public static void save(@NotNull AIConfigurationProfile profile) throws DBException {
        DBSSecretController controller = DBSSecretController.getGlobalSecretController();
        for (var credential : getCredentials(profile.getConfiguration()).entrySet()) {
            controller.setPrivateSecretValue(
                new ProfileSecretObject(profile.getProfileId()),
                new DBSSecretValue(
                    getSecretId(profile, credential.getKey()),
                    profile.getProfileName() + ": " + credential.getKey(),
                    CommonUtils.nullIfEmpty(credential.getValue())
                )
            );
        }
        controller.flushChanges();
    }

    /**
     * Deletes the credentials of all users
     */
    public static void delete(@NotNull AIConfigurationProfile profile) throws DBException {
        DBSSecretController.getGlobalSecretController().deleteObjectSecrets(new ProfileSecretObject(profile.getProfileId()));
    }

    /**
     * Personal credentials must never get into the shared configuration
     */
    @NotNull
    public static AIEngineProperties copyWithoutCredentials(@NotNull AIEngineProperties properties) throws DBException {
        AIEngineProperties copy = AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.READ_PROPS_GSON.toJson(properties), properties.getClass());
        setCredentials(copy, Map.of());
        copy.clearUserCredentials();
        return copy;
    }

    @NotNull
    private static String getSecretId(@NotNull AIConfigurationProfile profile, @NotNull String propertyId) {
        return SECRET_ID_PREFIX + profile.getProfileId() + "." + propertyId;
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
