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
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PrefPageProjectResourceSettings
 */
public class PrefPageProjectNetworkProfiles extends PrefPageManagedNetworkProfiles
    implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.project.settings.networkProfiles"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageProjectNetworkProfiles.class);

    @Nullable
    private DBPProject projectMeta;

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
                UIUtils.showPreferencesFor(getShell(), null, PrefPageGlobalProjectNetworkProfiles.PAGE_ID);
                loadSettings();
            }
        );
        return composite;
    }

    @Override
    public void saveSettings(@NotNull DBWNetworkProfile profile) {
        super.saveSettings(profile);

        try {
            if (!DBWorkbench.isDistributed() && getProjectMeta().isUseSecretStorage()) {
                DBSSecretController secretController = DBSSecretController.getProjectSecretController(getProjectMeta());
                profile.persistSecrets(secretController);
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Save error", "Cannot save network profile credentials", e);
        }
    }

    @Nullable
    @Override
    protected DBSSecretController getSecretController() throws DBException {
        return !DBWorkbench.isDistributed() && getProjectMeta().isUseSecretStorage()
            ? DBSSecretController.getProjectSecretController(getProjectMeta()) : null;
    }

    @NotNull
    @Override
    protected DBWNetworkProfileManager getProfilesManager() {
        return getProjectMeta().getDataSourceRegistry().getNetworkProfiles();
    }

    @Override
    protected boolean deleteProfile(@NotNull DBWNetworkProfile profile) {
        List<? extends DBPDataSourceContainer> usedBy = getProjectMeta().getDataSourceRegistry().getDataSourcesByProfile(profile);
        if (!usedBy.isEmpty()) {
            UIUtils.showMessageBox(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                NLS.bind(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_info,
                    profile.getProfileName(), usedBy.size(), usedBy.stream()
                        .sorted(Comparator.comparing(DBPDataSourceContainer::getName))
                        .map(connection -> " - " + connection.getName())
                        .collect(Collectors.joining("\n"))
                ),
                SWT.ICON_ERROR
            );
            return false;
        }
        if (!super.confirmProfileDeletion(profile)) {
            return false;
        }
        super.removeProfile(profile);
        return true;
    }

    @NotNull
    @Override
    protected DBWNetworkProfile createProfile(@NotNull String profileName) {
        DBWNetworkProfile profile = new DBWNetworkProfile(getProjectMeta());
        profile.setProfileName(profileName);
        return profile;
    }

    @Override
    protected boolean isNameValid(@NotNull String profileName) {
        DBWNetworkProfile foundProfile = getProfilesManager().getProfile(null, profileName);
        if (foundProfile == null) {
            return true;
        }
        if (foundProfile.isGlobal()) {
            return confirmLocalCreation(profileName);
        }
        UIUtils.showMessageBox(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_title,
            NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_info,
                profileName,
                getProjectMeta().getName()
            ),
            SWT.ICON_ERROR
        );
        return false;
    }

    private boolean confirmLocalCreation(@NotNull String profileName) {
        return UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_local_name_used_in_global_label,
            NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_local_name_used_in_global_question,
                profileName,
                getProjectMeta().getName()
            )
        );
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

    void setProjectMeta(@NotNull DBPProject projectMeta) {
        this.projectMeta = projectMeta;
    }

    @NotNull
    DBPProject getProjectMeta() {
        return Objects.requireNonNull(projectMeta, "Project must be set before editing network profiles");
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
        return DBeaverIcons.getImage(DBIcon.CONNECTION_PROFILE);
    }
    @Override
    protected boolean hasAccessToPage() {
        return super.hasAccessToPage() ||
            (projectMeta != null && projectMeta.isPrivateProject() && DBWorkbench.getPlatform().getWorkspace()
                .hasRealmPermission(RMConstants.PERMISSION_DATABASE_DEVELOPER));
    }
}
