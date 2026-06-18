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
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Network profile manager
 */
public abstract class DBWNetworkProfileManager {

    @Nullable
    private volatile List<DBWNetworkProfile> profiles;

    public DBWNetworkProfileManager() {
    }

    @NotNull
    public List<DBWNetworkProfile> getProfiles() {
        return getProfilesSafe();
    }

    @NotNull
    public List<DBWNetworkProfile> getAllProfiles() {
        List<DBWNetworkProfile> profileList = getProfiles();
        DBWNetworkProfileManager parentManager = getParentManager();
        if (parentManager != null) {
            List<DBWNetworkProfile> pp = parentManager.getProfiles();
            if (!pp.isEmpty()) {
                if (profileList.isEmpty()) {
                    return pp;
                }
                List<DBWNetworkProfile> cl = new ArrayList<>(profileList.size() + pp.size());
                cl.addAll(pp);
                cl.addAll(profileList);
                return cl;
            }
        }
        return profileList;
    }

    private List<DBWNetworkProfile> getProfilesSafe() {
        if (profiles != null) {
            return profiles;
        }
        List<DBWNetworkProfile> pl = loadProfiles();
        synchronized (this) {
            if (profiles != null) {
                return profiles;
            }
            profiles = new ArrayList<>(pl);
        }

        return profiles;
    }

    @NotNull
    protected List<DBWNetworkProfile> loadProfiles() {
        // Do nothing by default
        return new ArrayList<>();
    }

    @Nullable
    public DBWNetworkProfile getProfile(@Nullable String source, @NotNull String name) {
        if (!CommonUtils.isEmpty(source)) {
            // Search in external sources
            DBWNetworkProfileProvider profileProvider = getProfileProvider();
            if (profileProvider != null) {
                return profileProvider.getNetworkProfile(source, name);
            }
            DBWNetworkProfileManager parent = getParentManager();
            return parent == null ? null : parent.getProfile(source, name);
        }
        // Search in profiles
        List<DBWNetworkProfile> profilesSafe = getProfilesSafe();
        synchronized (profilesSafe) {
            for (DBWNetworkProfile profile : profilesSafe) {
                if (CommonUtils.equalObjects(profile.getProfileName(), name)) {
                    return profile;
                }
            }
            DBWNetworkProfileManager parent = getParentManager();
            return parent == null ? null : parent.getProfile(source, name);
        }
    }

    public void addOrUpdateProfile(@NotNull DBWNetworkProfile profile) {
        List<DBWNetworkProfile> profilesSafe = getProfilesSafe();
        synchronized (profilesSafe) {
            for (int i = 0; i < profilesSafe.size(); i++) {
                if (CommonUtils.equalObjects(profilesSafe.get(i).getProfileName(), profile.getProfileName())) {
                    profilesSafe.set(i, profile);
                    return;
                }
            }
            profilesSafe.add(profile);
        }
    }

    public void removeProfile(@NotNull DBWNetworkProfile profile) {
        List<DBWNetworkProfile> profilesSafe = getProfilesSafe();
        synchronized (profilesSafe) {
            profilesSafe.remove(profile);
        }
    }

    // Saves profiles in persistent configuration
    // Doesn't throw errors, any errors will be in the log
    public abstract void saveSettings();

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
