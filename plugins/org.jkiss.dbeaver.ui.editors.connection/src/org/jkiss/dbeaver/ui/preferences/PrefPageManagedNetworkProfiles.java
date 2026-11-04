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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Common editing flow for project and global network profiles.
 */
abstract class PrefPageManagedNetworkProfiles extends PrefPageNetworkProfiles {

    protected final Set<String> deletedProfileNames = new HashSet<>();

    @Override
    protected boolean hasAccessToPage() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN);
    }

    @NotNull
    protected abstract DBWNetworkProfileManager getProfilesManager();

    @NotNull
    protected abstract DBWNetworkProfile createNewProfile(@NotNull String profileName);

    protected abstract boolean isNameValid(@NotNull String profileName) throws DBException;

    @NotNull
    @Override
    protected List<DBWNetworkProfile> getDefaultNetworkProfiles() {
        return getProfilesManager().getProfiles();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        deletedProfileNames.clear();
    }

    @Override
    protected boolean deleteProfile(@NotNull DBWNetworkProfile profile) {
        if (!confirmProfileDeletion(profile)) {
            return false;
        }
        if (getProfilesManager().getProfiles().contains(profile)) {
            deletedProfileNames.add(profile.getProfileName());
        }
        return true;
    }

    @Override
    public boolean performOk() {
        List<DBWNetworkProfile> profilesToRemove = getProfilesManager().getProfiles().stream()
            .filter(profile -> deletedProfileNames.contains(profile.getProfileName()))
            .toList();
        for (DBWNetworkProfile profile : profilesToRemove) {
            if (!removeProfile(profile)) {
                return false;
            }
        }
        if (!super.performOk()) {
            return false;
        }
        deletedProfileNames.clear();
        return true;
    }

    @Override
    protected void updateNetworkProfiles(@NotNull List<DBWNetworkProfile> allProfiles) {
        DBWNetworkProfileManager profilesRegistry = getProfilesManager();
        for (DBWNetworkProfile profile : allProfiles) {
            profilesRegistry.addOrUpdateProfile(profile);
        }
        profilesRegistry.saveSettings();
    }

    protected boolean confirmProfileDeletion(@NotNull DBWNetworkProfile profile) {
        return UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_title,
            getDeleteConfirmationQuestion(profile)
        );
    }

    @NotNull
    protected String getDeleteConfirmationQuestion(@NotNull DBWNetworkProfile profile) {
        return NLS.bind(
            UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_question,
            profile.getProfileName()
        );
    }

    protected boolean removeProfile(@NotNull DBWNetworkProfile profile) {
        getProfilesManager().removeProfile(profile);
        return true;
    }

    @Nullable
    @Override
    protected DBWNetworkProfile createNewProfile(@Nullable DBWNetworkProfile sourceProfile) {
        String profileName = sourceProfile == null ? "" : sourceProfile.getProfileName();

        while (true) {
            profileName = EnterNameDialog.chooseName(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_profile_name,
                profileName
            );

            if (CommonUtils.isEmptyTrimmed(profileName)) {
                return null;
            }

            profileName = profileName.trim();

            try {
                if (isNameValid(profileName)) {
                    break;
                }
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_title, null, e);
                return null;
            }
        }

        return createNewProfile(profileName);
    }
}
