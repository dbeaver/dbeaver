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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIAccountProperties;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.auth.AuthProperty;
import org.jkiss.dbeaver.model.auth.SMSession;
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
    private static final String ACCOUNT_AUTHENTICATION_PROPERTY = "accountAuthentication";
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
            if (!(ACCOUNT_AUTHENTICATION_PROPERTY.equals(propertyId) && profile.getConfiguration() instanceof AIAccountProperties)
                && properties.stream().noneMatch(property -> property.getId().equals(propertyId))) {
                throw new DBException("Invalid AI credential property " + propertyId);
            }
        }
        writeCredentials(profile, credentials, controller, null);
    }

    private static void writeCredentials(
        @NotNull AIConfigurationProfile profile,
        @NotNull Map<String, String> credentials,
        @NotNull DBSSecretController controller,
        @Nullable SMSession session
    ) throws DBException {
        for (var credential : credentials.entrySet()) {
            checkSession(session);
            controller.setPrivateSecretValue(
                new ProfileSecretObject(profile.getProfileId()),
                new DBSSecretValue(
                    getSecretId(profile, credential.getKey()),
                    profile.getProfileName() + ": " + credential.getKey(),
                    CommonUtils.isEmpty(credential.getValue()) ? null : credential.getValue()
                )
            );
        }
        checkSession(session);
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
        DBSSecretController controller = DBSSecretController.getGlobalSecretController();
        SMSession session = DBWorkbench.isPlatformStarted()
            ? DBWorkbench.getPlatform().getWorkspace().getWorkspaceSession() : null;
        Map<String, String> credentials = load(profile, controller);
        AIEngineProperties properties = profile.getConfiguration();
        apply(properties, credentials);
        if (properties instanceof AIAccountProperties accountProperties) {
            String mode = controller.getPrivateSecretValue(getSecretId(profile, ACCOUNT_AUTHENTICATION_PROPERTY));
            if (mode != null) {
                accountProperties.setAccountAuthentication(Boolean.parseBoolean(mode));
            }
            if (accountProperties.isAccountAuthentication() || mode != null) {
                credentials.put(ACCOUNT_AUTHENTICATION_PROPERTY, Boolean.toString(accountProperties.isAccountAuthentication()));
            }
            accountProperties.clearAccountTokens();
            accountProperties.setAccessToken(controller.getPrivateSecretValue(getSecretId(profile,
                AIAccountProperties.ACCOUNT_ACCESS_TOKEN_PROPERTY)));
            accountProperties.setRefreshToken(controller.getPrivateSecretValue(getSecretId(profile,
                AIAccountProperties.ACCOUNT_REFRESH_TOKEN_PROPERTY)));
            accountProperties.setStoredAccountId(controller.getPrivateSecretValue(getSecretId(profile,
                AIAccountProperties.ACCOUNT_ID_PROPERTY)));
            accountProperties.setStoredAccountEmail(controller.getPrivateSecretValue(getSecretId(profile,
                AIAccountProperties.ACCOUNT_EMAIL_PROPERTY)));
            accountProperties.setStoredExpiresAt(CommonUtils.toLong(controller.getPrivateSecretValue(getSecretId(profile,
                AIAccountProperties.ACCOUNT_EXPIRES_AT_PROPERTY))));
            accountProperties.setAccountTokenPersistence((previousRefreshToken, tokens) -> {
                checkSession(session);
                if (previousRefreshToken != null && !previousRefreshToken.equals(tokens.refreshToken())) {
                    String storedRefreshToken = controller.getPrivateSecretValue(getSecretId(profile,
                        AIAccountProperties.ACCOUNT_REFRESH_TOKEN_PROPERTY));
                    if (!previousRefreshToken.equals(storedRefreshToken)) {
                        throw new DBException("AI account credentials have changed while refreshing the token");
                    }
                }
                saveAccountCredentials(profile, accountProperties, tokens, controller, session);
            });
            accountProperties.setAccountTokenValidator(refreshToken -> {
                checkSession(session);
                String storedRefreshToken = controller.getPrivateSecretValue(getSecretId(profile,
                    AIAccountProperties.ACCOUNT_REFRESH_TOKEN_PROPERTY));
                if (!CommonUtils.equalObjects(refreshToken, storedRefreshToken)) {
                    throw new DBException("AI account credentials have changed. Reopen the profile settings to sign in again.");
                }
            });
        }
        checkSession(session);
        profile.setResolvedUserCredentials(credentials);
    }

    private static void checkSession(@Nullable SMSession session) throws DBException {
        if (session != null && session != DBWorkbench.getPlatform().getWorkspace().getWorkspaceSession()) {
            throw new DBException("AI account authorization session has changed");
        }
    }

    private static void saveAccountCredentials(
        @NotNull AIConfigurationProfile profile,
        @NotNull AIAccountProperties properties,
        @NotNull AIAccountAuthenticator.Tokens tokens,
        @NotNull DBSSecretController controller,
        @Nullable SMSession session
    ) throws DBException {
        checkUserProfile(profile);
        Map<String, String> credentials = new LinkedHashMap<>();
        // save the rotating refresh token first so a partial write can be recovered
        credentials.put(AIAccountProperties.ACCOUNT_REFRESH_TOKEN_PROPERTY, CommonUtils.notEmpty(tokens.refreshToken()));
        credentials.put(AIAccountProperties.ACCOUNT_ACCESS_TOKEN_PROPERTY, CommonUtils.notEmpty(tokens.accessToken()));
        credentials.put(AIAccountProperties.ACCOUNT_ID_PROPERTY, CommonUtils.notEmpty(tokens.accountId()));
        credentials.put(AIAccountProperties.ACCOUNT_EMAIL_PROPERTY, CommonUtils.notEmpty(tokens.email()));
        credentials.put(AIAccountProperties.ACCOUNT_EXPIRES_AT_PROPERTY, Long.toString(properties.getStoredExpiresAt()));
        credentials.put(ACCOUNT_AUTHENTICATION_PROPERTY, Boolean.toString(properties.isAccountAuthentication()));
        writeCredentials(profile, credentials, controller, session);
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
        if (properties instanceof AIAccountProperties accountProperties) {
            boolean mode = accountProperties.isAccountAuthentication();
            if (mode || resolvedCredentials.containsKey(ACCOUNT_AUTHENTICATION_PROPERTY)) {
                currentCredentials.put(ACCOUNT_AUTHENTICATION_PROPERTY, Boolean.toString(mode));
            }
            if (mode != Boolean.parseBoolean(resolvedCredentials.get(ACCOUNT_AUTHENTICATION_PROPERTY))) {
                updates.put(ACCOUNT_AUTHENTICATION_PROPERTY, Boolean.toString(mode));
            }
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
        return getSecretId(profile, property.getId());
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
