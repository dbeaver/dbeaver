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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PrefPageProjectResourceSettings
 */
public class PrefPageProjectNetworkProfiles extends PrefPageNetworkProfiles implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.project.settings.networkProfiles"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageProjectNetworkProfiles.class);

    @Nullable
    private DBPProject projectMeta;

    public PrefPageProjectNetworkProfiles() {
    }

    @Override
    public void saveSettings(@NotNull DBWNetworkProfile profile) {
        super.saveSettings(profile);

        try {
            if (!DBWorkbench.isDistributed() && projectMeta != null && projectMeta.isUseSecretStorage()) {
                DBSSecretController secretController = DBSSecretController.getProjectSecretController(projectMeta);
                profile.persistSecrets(secretController);
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Save error", "Cannot save network profile credentials", e);
        }
    }

    @Nullable
    @Override
    protected DBSSecretController getSecretController() throws DBException {
        DBSSecretController secretController = null;
        if (projectMeta == null) {
            return DBSSecretController.getGlobalSecretController();
        } else if (!DBWorkbench.isDistributed() && projectMeta.isUseSecretStorage()) {
            secretController = DBSSecretController.getProjectSecretController(projectMeta);
        }
        return secretController;
    }

    @NotNull
    protected DBWNetworkProfileManager getProfilesRegistry() {
        if (projectMeta == null) {
            return DBWorkbench.getPlatform().getNetworkProfiles();
        } else {
            return projectMeta.getDataSourceRegistry().getNetworkProfiles();
        }
    }

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
        String usedByNames = formatConnectionsUsingProfile(usedBy);
        if (!selectedProfile.isGlobal() && !usedBy.isEmpty()) {
            UIUtils.showMessageBox(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                NLS.bind(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_info,
                    selectedProfile.getProfileName(), usedBy.size(), usedByNames
                ),
                SWT.ICON_ERROR
            );
            return false;
        }
        if (!UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_title,
            getDeleteConfirmationQuestion(selectedProfile)
        )) {
            return false;
        }
        if (!usedBy.isEmpty() && !UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_title,
            NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_tool_delete_used_confirmation_question,
                selectedProfile.getProfileName(),
                usedBy.size(),
                usedByNames
            )
        )) {
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
        profilesRegistry.saveSettings();
    }

    @NotNull
    protected List<? extends DBPDataSourceContainer> connectionsUsingProfile(@NotNull DBWNetworkProfile selectedProfile) {
        return projectMeta != null
            ? projectMeta.getDataSourceRegistry().getDataSourcesByProfile(selectedProfile)
            : new ArrayList<>();
    }

    @Nullable
    @Override
    protected DBWNetworkProfile createNewProfile(@Nullable DBWNetworkProfile sourceProfile) {
        String profileName = sourceProfile == null ? "" : sourceProfile.getProfileName();

        DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
        boolean isCreatingGlobal = projectMeta == null;
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

            if (!checkName(profilesRegistry, profileName, isCreatingGlobal)) {
                continue;
            }

            break;
        }

        DBWNetworkProfile newProfile = isCreatingGlobal ? new DBWNetworkProfile() : new DBWNetworkProfile(projectMeta);
        newProfile.setProfileName(profileName);

        profilesRegistry.addOrUpdateProfile(newProfile);
        profilesRegistry.saveSettings();

        return newProfile;
    }

    protected boolean checkName(@NotNull DBWNetworkProfileManager profilesRegistry, @NotNull String profileName, boolean isCreatingGlobal) {
        DBWNetworkProfile foundProfile = profilesRegistry.getProfile(null, profileName);
        if (foundProfile != null) {
            if (isCreatingGlobal == foundProfile.isGlobal()) {
                UIUtils.showMessageBox(
                    getShell(),
                    UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_title,
                    projectMeta == null ?
                        NLS.bind(UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_global_info, profileName) :
                        NLS.bind(
                            UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_info,
                            profileName,
                            projectMeta.getName()
                        ),
                    SWT.ICON_ERROR
                );
                return false;
            } else if (!isCreatingGlobal) {
                return confirmLocalCreation(profileName);
            }
        } else if (isCreatingGlobal) {
            return confirmGlobalCreation(profileName);
        }
        return true;
    }

    private boolean confirmGlobalCreation(@NotNull String profileName) {
        List<String> projectsWithSameProfileName = getProjects()
            .stream()
            .filter(proj -> proj.getDataSourceRegistry().getNetworkProfiles().getProfile(null, profileName) != null)
            .map(DBPProject::getName)
            .map(n -> " - " + n)
            .toList();
        return projectsWithSameProfileName.isEmpty() || askGlobalNameConfirmation(projectsWithSameProfileName, profileName);
    }

    private boolean confirmLocalCreation(@NotNull String profileName) {
        return UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_local_name_used_in_global_label,
            NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_local_name_used_in_global_question,
                profileName,
                projectMeta != null ? projectMeta.getName() : ""
            )
        );
    }

    private boolean askGlobalNameConfirmation(@NotNull List<String> projectsWithSameProfile, @NotNull String profileName) {
        String projectsList = String.join("\n", projectsWithSameProfile);
        return UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_global_project_name_used_in_local_label,
            NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_global_project_name_used_in_local_question,
                profileName,
                projectsList
            )
        );
    }

    @NotNull
    protected List<? extends DBPProject> getProjects() {
        return DBWorkbench
            .getPlatform()
            .getWorkspace()
            .getProjects();
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public IAdaptable getElement() {
        return projectMeta instanceof RCPProject rcpProject ? rcpProject.getEclipseProject() : null;
    }

    @Override
    public void setElement(IAdaptable element) {
        IProject iProject;
        if (element instanceof DBNNode node && node.getOwnerProject() instanceof RCPProject rcpProject) {
            iProject = rcpProject.getEclipseProject();
        } else {
            iProject = GeneralUtils.adapt(element, IProject.class);
        }
        if (iProject != null) {
            this.projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(iProject);
        }
    }

    void setProjectMeta(@Nullable DBPProject projectMeta) {
        this.projectMeta = projectMeta;
    }

    @Nullable
    DBPProject getProjectMeta() {
        return projectMeta;
    }

    /**
     * Opens a property dialog for editing network profiles.
     *
     * @return {@code true} if the dialog was closed with OK, {@code false} otherwise or if an error occurred.
     */
    public static boolean open(@NotNull Shell shell, @NotNull RCPProject project, @Nullable DBWNetworkProfile profile) {
        PreferenceDialog dialog = getPropertyDialogOn(shell, project, profile);
        if (dialog == null) {
            log.error("Can't open network profiles preferences");
            return false;
        }
        return dialog.open() == IDialogConstants.OK_ID;
    }

    @Nullable
    private static PreferenceDialog getPropertyDialogOn(
        @NotNull Shell shell,
        @NotNull RCPProject project,
        @Nullable DBWNetworkProfile profile
    ) {
        return profile != null && profile.isGlobal()
            ? PreferencesUtil.createPreferenceDialogOn(
            shell,
            PrefPageGlobalProjectNetworkProfiles.PAGE_ID,
            null,
            profile.getProfileName()
        )
            : PreferencesUtil.createPropertyDialogOn(
                shell,
                project.getEclipseProject(),
                PAGE_ID,
                null,
                profile != null ? profile.getProfileName() : null
            );
    }

    @NotNull
    @Override
    protected Image getProfileImage(@NotNull DBWNetworkProfile profile) {
        return DBeaverIcons.getImage(profile.isGlobal() ? DBIcon.GLOBAL_PROFILE : DBIcon.CONNECTION_PROFILE);
    }
    @Override
    protected boolean hasAccessToPage() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN) ||
            (projectMeta != null && projectMeta.isPrivateProject() && DBWorkbench.getPlatform().getWorkspace()
                .hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER));
    }
}
