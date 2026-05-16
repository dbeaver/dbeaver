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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Network profile manager
 */
public abstract class DBWNetworkProfileManager {
    private static final Log log = Log.getLog(DBWNetworkProfileManager.class);

    @NotNull
    private final List<DBWNetworkProfile> profiles = new ArrayList<>();

    @NotNull
    public List<DBWNetworkProfile> getProfiles() {
        return profiles;
    }

    @Nullable
    public DBWNetworkProfile getProfile(@Nullable String source, @NotNull String name) {
        if (!CommonUtils.isEmpty(source)) {
            // Search in external sources
            DBWNetworkProfileProvider profileProvider = getProfileProvider();
            if (profileProvider != null) {
                return profileProvider.getNetworkProfile(source, name);
            }
            return null;
        }
        // Search in profiles
        synchronized (profiles) {
            for (DBWNetworkProfile profile : profiles) {
                if (CommonUtils.equalObjects(profile.getProfileName(), name)) {
                    return profile;
                }
            }
            DBWNetworkProfileManager parent = getParentManager();
            return parent == null ? null : parent.getProfile(source, name);
        }
    }

    public void addOrUpdateProfile(@NotNull DBWNetworkProfile profile) {
        for (int i = 0; i < profiles.size(); i++) {
            if (CommonUtils.equalObjects(profiles.get(i).getProfileName(), profile.getProfileName())) {
                profiles.set(i, profile);
                return;
            }
        }
        profiles.add(profile);
    }

    public void removeProfile(@NotNull DBWNetworkProfile profile) {
        profiles.remove(profile);
        try {
            DBSSecretController secretController = getSecretController();
            secretController.setPrivateSecretValue(
                profile.getSecretKeyId(),
                null);
            secretController.flushChanges();
        } catch (DBException e) {
            log.error("Error removing network profile secrets", e);
        }
    }

    @NotNull
    protected abstract DBSSecretController getSecretController() throws DBException;

    @Nullable
    protected DBWNetworkProfileProvider getProfileProvider() {
        return null;
    }

    @Nullable
    protected DBWNetworkProfileManager getParentManager() {
        return null;
    }
}
