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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class DBWNetworkProfileManagerTest {

    @Test
    public void loadedProfilesAreSortedByName() {
        TestNetworkProfileManager manager = new TestNetworkProfileManager(
            createProfile("Profile 10"),
            createProfile("Profile 2"),
            createProfile("Profile 1")
        );

        Assertions.assertEquals(
            List.of("Profile 1", "Profile 2", "Profile 10"),
            getSortedNames(manager.getProfiles())
        );
    }

    @Test
    public void addedProfilesKeepListSortedByName() {
        TestNetworkProfileManager manager = new TestNetworkProfileManager(
            createProfile("Profile 2"),
            createProfile("Profile 10")
        );

        manager.addOrUpdateProfile(createProfile("Profile 1"));

        Assertions.assertEquals(
            List.of("Profile 1", "Profile 2", "Profile 10"),
            getSortedNames(manager.getProfiles())
        );
    }

    @Test
    public void allProfilesKeepParentBeforeLocalGroups() {
        TestNetworkProfileManager parentManager = new TestNetworkProfileManager(
            createProfile("Global 10"),
            createProfile("Global 2")
        );
        TestNetworkProfileManager manager = new TestNetworkProfileManager(
            parentManager,
            createProfile("Project 10"),
            createProfile("Project 2")
        );

        Assertions.assertEquals(
            List.of("Global 2", "Global 10", "Project 2", "Project 10"),
            getSortedNames(manager.getAllProfiles())
        );
    }

    @NotNull
    private DBWNetworkProfile createProfile(@NotNull String name) {
        DBWNetworkProfile profile = new DBWNetworkProfile();
        profile.setProfileName(name);
        return profile;
    }

    @NotNull
    private List<String> getSortedNames(@NotNull List<DBWNetworkProfile> profiles) {
        return profiles.stream().map(DBWNetworkProfile::getProfileName).toList();
    }

    private static class TestNetworkProfileManager extends DBWNetworkProfileManager {
        @NotNull
        private final List<DBWNetworkProfile> profiles;
        @Nullable
        private final DBWNetworkProfileManager parentManager;

        private TestNetworkProfileManager(@NotNull DBWNetworkProfile... profiles) {
            this(null, profiles);
        }

        private TestNetworkProfileManager(
            @Nullable DBWNetworkProfileManager parentManager,
            @NotNull DBWNetworkProfile... profiles
        ) {
            this.profiles = new ArrayList<>(List.of(profiles));
            this.parentManager = parentManager;
        }

        @NotNull
        @Override
        protected List<DBWNetworkProfile> loadProfiles() {
            return profiles;
        }

        @Override
        public void saveSettings() {
        }

        @NotNull
        @Override
        protected DBSSecretController getSecretController() throws DBException {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        protected DBWNetworkProfileManager getParentManager() {
            return parentManager;
        }
    }
}
