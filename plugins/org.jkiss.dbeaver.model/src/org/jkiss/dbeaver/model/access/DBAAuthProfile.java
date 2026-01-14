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

package org.jkiss.dbeaver.model.access;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBInfoUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPAuthModelDescriptor;
import org.jkiss.dbeaver.model.connection.DBPConfigurationProfile;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.rm.RMProjectType;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Auth profile.
 * Authentication properties.
 */
public class DBAAuthProfile extends DBPConfigurationProfile {

    // Secret key prefix
    public static final String PROFILE_KEY_PREFIX = "/auth-profile/";

    private String authModelId;
    private String userName;
    private String userPassword;
    private boolean savePassword;

    public DBAAuthProfile(DBPProject project) {
        super(project);
    }

    public DBAAuthProfile(DBAAuthProfile source) {
        super(source);
        this.authModelId = source.authModelId;
        this.userName = source.userName;
        this.userPassword = source.userPassword;
        this.savePassword = source.savePassword;
    }

    public String getSecretKeyId() {
        return RMProjectType.getPlainProjectId(getProject()) + PROFILE_KEY_PREFIX + getProfileId();
    }

    public String getAuthModelId() {
        return authModelId;
    }

    public void setAuthModelId(String authModelId) {
        this.authModelId = authModelId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public boolean isSavePassword() {
        return savePassword;
    }

    public void setSavePassword(boolean savePassword) {
        this.savePassword = savePassword;
    }

    public DBPAuthModelDescriptor getAuthModel() {
        return DBWorkbench.getPlatform().getDataSourceProviderRegistry().getAuthModel(authModelId);
    }

    @Override
    public void persistSecrets(DBSSecretController secretController) throws DBException {
        Map<String, Object> props = new LinkedHashMap<>();

        // Info fields (we don't use them anyhow)
        props.put("profile-id", getProfileId());
        props.put("profile-name", getProfileName());

        // Primary props
        if (getUserName() != null) {
            props.put("user", getUserName());
        }
        if (getUserPassword() != null) {
            props.put("password", getUserPassword());
        }
        // Additional auth props
        if (!CommonUtils.isEmpty(getProperties())) {
            props.put("properties", getProperties());
        }
        String secretValue = DBInfoUtils.SECRET_GSON.toJson(props);

        secretController.setPrivateSecretValue(
            getSecretKeyId(),
            secretValue);
        secretController.flushChanges();
    }

    @Override
    public void resolveSecrets(DBSSecretController secretController) throws DBException {
        String secretValue = secretController.getPrivateSecretValue(
            getSecretKeyId());
        if (secretValue == null) {
            // Backward compatibility
            loadFromLegacySecret(secretController);
            return;
        }

        Map<String, Object> props = JSONUtils.parseMap(DBInfoUtils.SECRET_GSON, new StringReader(secretValue));
        userName = JSONUtils.getString(props, "user");
        userPassword = JSONUtils.getString(props, "password");
        setProperties(JSONUtils.deserializeStringMap(props, "properties"));
    }

    private void loadFromLegacySecret(DBSSecretController secretController) throws DBException {
        // Auth profiles were not supported in legacy versions
    }

}
