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
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.utils.CommonUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Common editing flow for project and global network profiles.
 */
abstract class PrefPageManagedNetworkProfiles extends PrefPageNetworkProfiles {

    @Override
    protected boolean hasAccessToPage() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN);
    }

    @NotNull
    protected abstract DBWNetworkProfileManager getProfilesRegistry();

    @NotNull
    protected abstract DBWNetworkProfile createProfile(@NotNull String profileName);

    protected abstract boolean checkName(@NotNull String profileName);

    @NotNull
    protected abstract List<? extends DBPDataSourceContainer> connectionsUsingProfile(@NotNull DBWNetworkProfile profile);

    @NotNull
    @Override
    protected List<DBWNetworkProfile> getDefaultNetworkProfiles() {
        return getProfilesRegistry().getProfiles();
    }

    @Override
    protected void updateNetworkProfiles(@NotNull List<DBWNetworkProfile> allProfiles) {
        DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
        for (DBWNetworkProfile profile : allProfiles) {
            saveSettings(profile);
            profilesRegistry.addOrUpdateProfile(profile);
        }
        profilesRegistry.saveSettings();
    }

    @Override
    protected boolean deleteProfile(@NotNull DBWNetworkProfile selectedProfile) {
        List<? extends DBPDataSourceContainer> usedBy = connectionsUsingProfile(selectedProfile);
        if (!confirmProfileDeletion(selectedProfile, usedBy)) {
            return false;
        }
        try {
            removeProfile(selectedProfile, usedBy);
            return true;
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(
                UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                NLS.bind(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_message,
                    selectedProfile.getProfileName()
                ),
                e
            );
            return false;
        }
    }

    protected boolean confirmProfileDeletion(
        @NotNull DBWNetworkProfile profile,
        @NotNull List<? extends DBPDataSourceContainer> usedBy
    ) {
        if (!usedBy.isEmpty()) {
            UIUtils.showMessageBox(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                NLS.bind(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_info,
                    profile.getProfileName(), usedBy.size(), formatConnectionsUsingProfile(usedBy)
                ),
                SWT.ICON_ERROR
            );
            return false;
        }
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

    @NotNull
    protected String formatConnectionsUsingProfile(@NotNull List<? extends DBPDataSourceContainer> dataSources) {
        return dataSources.stream()
            .sorted(Comparator.comparing(DBPNamedObject::getName))
            .map(dataSource -> " - " + dataSource.getName())
            .collect(Collectors.joining("\n"));
    }

    protected void removeProfile(
        @NotNull DBWNetworkProfile profile,
        @NotNull List<? extends DBPDataSourceContainer> usedBy
    ) throws DBException {
        DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
        profilesRegistry.removeProfile(profile);
        if (!DBWorkbench.isDistributed()) {
            profilesRegistry.saveSettings();
        }
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

            if (checkName(profileName)) {
                break;
            }
        }

        DBWNetworkProfile newProfile = createProfile(profileName);
        DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
        profilesRegistry.addOrUpdateProfile(newProfile);
        if (!DBWorkbench.isDistributed()) {
            profilesRegistry.saveSettings();
        }

        return newProfile;
    }
}
