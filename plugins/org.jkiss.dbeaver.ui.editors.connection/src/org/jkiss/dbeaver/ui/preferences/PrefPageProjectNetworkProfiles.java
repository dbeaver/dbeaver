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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileManager;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

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
            if (projectMeta != null && projectMeta.isUseSecretStorage()) {
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
        } else if (projectMeta.isUseSecretStorage()) {
            secretController = DBSSecretController.getProjectSecretController(projectMeta);
        }
        return secretController;
    }

    @NotNull
    private DBWNetworkProfileManager getProfilesRegistry() {
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
        if (projectMeta != null) {
            List<? extends DBPDataSourceContainer> usedBy = projectMeta
                .getDataSourceRegistry().getDataSourcesByProfile(selectedProfile);
            if (!usedBy.isEmpty()) {
                UIUtils.showMessageBox(
                    getShell(),
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                    NLS.bind(
                        UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_info, new Object[] {
                            selectedProfile.getProfileName(),
                            usedBy.size(),
                            usedBy.stream()
                                .sorted(Comparator.comparing(DBPNamedObject::getName))
                                .map(x -> " - " + x.getName())
                                .collect(Collectors.joining("\n"))
                        }
                    ),
                    SWT.ICON_ERROR
                );
                return false;
            }
        }
        if (UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_title,
            NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_question,
                selectedProfile.getProfileName()
            )
        )) {
            DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
            profilesRegistry.removeProfile(selectedProfile);
            profilesRegistry.saveSettings();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    protected DBWNetworkProfile createNewProfile(@Nullable DBWNetworkProfile sourceProfile) {
        String profileName = sourceProfile == null ? "" : sourceProfile.getProfileName();

        DBWNetworkProfileManager profilesRegistry = getProfilesRegistry();
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

            if (profilesRegistry.getProfile(null, profileName) != null) {
                UIUtils.showMessageBox(
                    getShell(),
                    UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_title,
                    projectMeta == null ?
                        NLS.bind(UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_global_info, profileName) :
                        NLS.bind(UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_info, profileName, projectMeta.getName()),
                    SWT.ICON_ERROR
                );

                continue;
            }

            break;
        }

        DBWNetworkProfile newProfile = projectMeta == null ? new DBWNetworkProfile() : new DBWNetworkProfile(projectMeta);
        newProfile.setProfileName(profileName);

        profilesRegistry.addOrUpdateProfile(newProfile);
        profilesRegistry.saveSettings();

        return newProfile;
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
        PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn(
            shell,
            project.getEclipseProject(),
            PAGE_ID,
            null,
            profile != null ? profile.getProfileName() : null
        );
        if (dialog == null) {
            log.error("Can't open network profiles preferences");
            return false;
        }
        return dialog.open() == IDialogConstants.OK_ID;
    }


}
