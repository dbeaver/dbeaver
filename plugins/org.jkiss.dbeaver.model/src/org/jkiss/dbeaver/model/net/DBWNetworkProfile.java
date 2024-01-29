/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBInfoUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConfigurationProfile;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.rm.RMProjectType;
import org.jkiss.dbeaver.model.secret.DBSSecret;
import org.jkiss.dbeaver.model.secret.DBSSecretBrowser;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretSubject;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.StringReader;
import java.util.*;

/**
 * Network configuration profile
 */
public class DBWNetworkProfile extends DBPConfigurationProfile {

    // Secret key prefix
    public static final String PROFILE_KEY_PREFIX = "/network-profile/";

    private transient DBSSecretSubject secretSubject;

    @NotNull
    private final List<DBWHandlerConfiguration> configurations = new ArrayList<>();

    @NotNull
    public List<DBWHandlerConfiguration> getConfigurations() {
        return configurations;
    }

    public DBWNetworkProfile() {
    }

    public DBWNetworkProfile(DBPProject project) {
        super(project);
    }

    @Override
    public String getProfileSource() {
        return secretSubject == null ? null : secretSubject.getSecretSubjectId();
    }

    public void setSecretSubject(DBSSecretSubject secretSubject) {
        this.secretSubject = secretSubject;
    }

    public void updateConfiguration(@NotNull DBWHandlerConfiguration cfg) {
        for (int i = 0; i < configurations.size(); i++) {
            DBWHandlerConfiguration c = configurations.get(i);
            if (Objects.equals(cfg.getId(), c.getId())) {
                configurations.set(i, cfg);
                return;
            }
        }
        configurations.add(cfg);
    }

    @Nullable
    public DBWHandlerConfiguration getConfiguration(DBWHandlerDescriptor handler) {
        for (DBWHandlerConfiguration  cfg : configurations) {
            if (Objects.equals(cfg.getId(), handler.getId())) {
                return cfg;
            }
        }
        return null;
    }

    @Nullable
    public DBWHandlerConfiguration getConfiguration(String configId) {
        for (DBWHandlerConfiguration  cfg : configurations) {
            if (Objects.equals(cfg.getId(), configId)) {
                return cfg;
            }
        }
        return null;
    }

    public String getSecretKeyId() {
        String prefix;
        DBPProject project = getProject();
        if (project != null) {
            prefix = RMProjectType.getPlainProjectId(project);
        } else if (secretSubject != null) {
            prefix = secretSubject.getSecretSubjectId();
        } else {
            prefix = "global";
        }
        return prefix + PROFILE_KEY_PREFIX + getProfileId();
    }

    @Override
    public void persistSecrets(DBSSecretController secretController) throws DBException {
        Map<String, Object> props = new LinkedHashMap<>();

        // Info fields (we don't use them anyhow)
        props.put("profile-id", getProfileId());
        props.put("profile-name", getProfileName());

        List<Map<String, Object>> handlersConfigs = new ArrayList<>();
        for (DBWHandlerConfiguration cfg : configurations) {
            Map<String, Object> hcProps = cfg.saveToMap();
            if (!hcProps.isEmpty()) {
                hcProps.put("id", cfg.getId());
                handlersConfigs.add(hcProps);
            }
        }
        if (!handlersConfigs.isEmpty()) {
            props.put("handlers", handlersConfigs);
        }

        String secretValue = DBInfoUtils.SECRET_GSON.toJson(props);

        secretController.setPrivateSecretValue(
            getSecretKeyId(),
            secretValue);
    }

    @Override
    public void resolveSecrets(DBSSecretController secretController) throws DBException {
        String secretValue = secretController.getPrivateSecretValue(getSecretKeyId());
        if (secretValue == null) {
            if (!DBWorkbench.isDistributed()) {
                // Backward compatibility
                loadFromLegacySecret(secretController);
            }
            return;
        }

        Map<String, Object> props = JSONUtils.parseMap(DBInfoUtils.SECRET_GSON, new StringReader(secretValue));

        List<Map<String, Object>> handlerConfigs = JSONUtils.getObjectList(props, "handlers");
        for (Map<String, Object> hc : handlerConfigs) {
            String configId = JSONUtils.getString(hc, "id");
            DBWHandlerConfiguration configuration = getConfiguration(configId);
            if (configuration != null) {
                configuration.loadFromMap(hc);
            }
        }
    }

    private void loadFromLegacySecret(DBSSecretController secretController) throws DBException {
        if (!(secretController instanceof DBSSecretBrowser) || getProject() == null) {
            return;
        }
        DBSSecretBrowser secretBrowser = (DBSSecretBrowser) secretController;
        for (DBWHandlerConfiguration cfg : configurations) {
            String prefix = "projects/" + getProject().getId() + "/network/" + cfg.getId() + "/profile/" + getProfileId();
            Map<String, String> secureProps = new LinkedHashMap<>();
            for (DBSSecret secret : secretBrowser.listSecrets(prefix)) {
                String secretId = secret.getId();
                switch (secret.getName()) {
                    case "user":
                        cfg.setUserName(secretController.getPrivateSecretValue(secretId));
                        break;
                    case "password":
                        cfg.setPassword(secretController.getPrivateSecretValue(secretId));
                        break;
                    case "name":
                        // Skip it
                        continue;
                    default:
                        secureProps.put(secret.getName(), secretController.getPrivateSecretValue(secretId));
                        break;
                }
            }
            if (!secureProps.isEmpty()) {
                cfg.setSecureProperties(secureProps);
            }
        }
    }

}
