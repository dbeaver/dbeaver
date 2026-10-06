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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * PrefPageProjectResourceSettings
 */
public class PrefPageProjectNetworkProfiles extends PrefPageNetworkProfiles implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.project.settings.networkProfiles"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageProjectNetworkProfiles.class);

    @Nullable
    private DBPProject projectMeta;

    private final Map<String, DBWNetworkProfile> originalProfiles = new LinkedHashMap<>();
    private final Map<String, DBWNetworkProfile> deletedProfiles = new LinkedHashMap<>();

    public PrefPageProjectNetworkProfiles() {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        // Embedded editors in global preferences have no preference container.
        if (getContainer() == null || projectMeta == null) {
            return super.createPreferenceContent(parent);
        }

        Composite composite = UIUtils.createComposite(parent, 1);
        Control content = super.createPreferenceContent(composite);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        UIUtils.createInfoLink(
            composite,
            UIConnectionMessages.pref_page_network_profiles_project_global_hint,
            () -> {
                boolean hasChanges = hasPendingChanges();
                UIUtils.showPreferencesFor(getShell(), null, PrefPageGlobalProjectNetworkProfiles.PAGE_ID);
                if (!hasChanges) {
                    loadSettings();
                }
            }
        );
        return composite;
    }

    private void persistProfileSecrets(@NotNull DBWNetworkProfile profile) {
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

    @NotNull
    @Override
    protected DBWNetworkProfile getEditableProfile(@NotNull DBWNetworkProfile profile) {
        // Credentials have already been resolved on the registered profile. Global saves also
        // persist credentials for unchanged registry entries, so those must retain their secrets.
        return new DBWNetworkProfile(profile);
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        rememberProfiles(getNetworkProfiles());
        deletedProfiles.clear();
    }

    private void rememberProfiles(@NotNull List<DBWNetworkProfile> profiles) {
        originalProfiles.clear();
        for (DBWNetworkProfile profile : profiles) {
            originalProfiles.put(profile.getProfileName(), new DBWNetworkProfile(profile));
        }
    }

    boolean hasPendingChanges() {
        saveHandlerSettings();
        for (DBWNetworkProfile profile : getNetworkProfiles()) {
            super.saveSettings(profile);
            if (!profile.equalConfigurations(originalProfiles.get(profile.getProfileName()))) {
                return true;
            }
        }
        return !deletedProfiles.isEmpty();
    }

    @Override
    public boolean performOk() {
        // A linked preferences dialog may have created a profile since this editor was loaded.
        for (DBWNetworkProfile profile : getNetworkProfiles()) {
            if (!originalProfiles.containsKey(profile.getProfileName()) && getProfilesRegistry().getProfiles().stream()
                .anyMatch(registered -> registered.getProfileName().equals(profile.getProfileName()))
            ) {
                showDuplicateNameError(profile.getProfileName());
                return false;
            }
        }
        for (DBWNetworkProfile profile : deletedProfiles.values()) {
            try {
                removeProfile(profile, connectionsUsingProfile(profile));
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                    NLS.bind(
                        UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_message,
                        profile.getProfileName()
                    ),
                    e
                );
                return false;
            }
        }
        return super.performOk();
    }

    @Override
    protected void updateNetworkProfiles(@NotNull List<DBWNetworkProfile> allProfiles) {
        DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
        boolean changed = !deletedProfiles.isEmpty();
        for (DBWNetworkProfile profile : allProfiles) {
            if (!profile.equalConfigurations(originalProfiles.get(profile.getProfileName()))
                || deletedProfiles.containsKey(profile.getProfileName())
            ) {
                // The registry must not share mutable configurations with the still-open preferences dialog.
                DBWNetworkProfile savedProfile = new DBWNetworkProfile(profile);
                persistProfileSecrets(savedProfile);
                profilesRegistry.addOrUpdateProfile(savedProfile);
                changed = true;
            }
        }
        if (changed) {
            profilesRegistry.saveSettings();
        }
        rememberProfiles(allProfiles);
        deletedProfiles.clear();
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
        DBWNetworkProfile originalProfile = originalProfiles.get(selectedProfile.getProfileName());
        if (originalProfile != null) {
            deletedProfiles.put(originalProfile.getProfileName(), originalProfile);
        }
        return true;
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
        // Connections may have started using the profile since its deletion was staged.
        if (!profile.isGlobal() && !usedBy.isEmpty()) {
            throw new DBException(NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_info,
                profile.getProfileName(), usedBy.size(), formatConnectionsUsingProfile(usedBy)
            ));
        }
        DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
        for (DBWNetworkProfile registeredProfile : new ArrayList<>(profilesRegistry.getProfiles())) {
            if (registeredProfile.getProfileName().equals(profile.getProfileName())) {
                profilesRegistry.removeProfile(registeredProfile);
                break;
            }
        }
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

        return newProfile;
    }

    protected boolean checkName(@NotNull DBWNetworkProfileManager profilesRegistry, @NotNull String profileName, boolean isCreatingGlobal) {
        DBWNetworkProfile foundProfile = getNetworkProfiles().stream()
            .filter(profile -> profile.getProfileName().equals(profileName))
            .findFirst().orElse(null);
        if (foundProfile == null && !deletedProfiles.containsKey(profileName)) {
            foundProfile = profilesRegistry.getProfiles().stream()
                .filter(profile -> profile.getProfileName().equals(profileName))
                .findFirst().orElse(null);
        }
        if (foundProfile == null && !isCreatingGlobal) {
            foundProfile = getProfilesForProject(null).stream()
                .filter(profile -> profile.getProfileName().equals(profileName))
                .findFirst().orElse(null);
        }
        if (foundProfile != null) {
            if (isCreatingGlobal == foundProfile.isGlobal()) {
                showDuplicateNameError(profileName);
                return false;
            } else if (!isCreatingGlobal) {
                return confirmLocalCreation(profileName);
            }
        } else if (isCreatingGlobal) {
            return confirmGlobalCreation(profileName);
        }
        return true;
    }

    private void showDuplicateNameError(@NotNull String profileName) {
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
    }

    private boolean confirmGlobalCreation(@NotNull String profileName) {
        List<String> projectsWithSameProfileName = getProjects()
            .stream()
            .filter(proj -> getProfilesForProject(proj).stream().anyMatch(profile -> profile.getProfileName().equals(profileName)))
            .map(DBPProject::getName)
            .map(n -> " - " + n)
            .toList();
        return projectsWithSameProfileName.isEmpty() || askGlobalNameConfirmation(projectsWithSameProfileName, profileName);
    }

    @NotNull
    protected List<DBWNetworkProfile> getProfilesForProject(@Nullable DBPProject project) {
        return project == null
            ? DBWorkbench.getPlatform().getNetworkProfiles().getProfiles()
            : project.getDataSourceRegistry().getNetworkProfiles().getProfiles();
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
