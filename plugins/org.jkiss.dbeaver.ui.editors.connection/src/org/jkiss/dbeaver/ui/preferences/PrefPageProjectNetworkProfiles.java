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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
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

    private DBPProject projectMeta;

    public PrefPageProjectNetworkProfiles() {
    }

    @Override
    public void saveSettings(DBWNetworkProfile profile) {
        super.saveSettings(profile);
        if (projectMeta.isUseSecretStorage()) {
            try {
                DBSSecretController secretController = DBSSecretController.getProjectSecretController(projectMeta);
                profile.persistSecrets(secretController);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Save error", "Cannot save network profile credentials", e);
            }
        }
    }

    @Override
    protected DBSSecretController getSecretController() throws DBException {
        DBSSecretController secretController = null;
        if (projectMeta.isUseSecretStorage()) {
            secretController = DBSSecretController.getProjectSecretController(projectMeta);
        }
        return secretController;
    }

    @Override
    protected List<DBWNetworkProfile> getDefaultNetworkProfiles() {
        return projectMeta.getDataSourceRegistry().getNetworkProfiles();
    }

    @Override
    protected void updateNetworkProfiles(List<DBWNetworkProfile> allProfiles) {
        for (DBWNetworkProfile profile : allProfiles) {
            saveSettings(profile);
            projectMeta.getDataSourceRegistry().updateNetworkProfile(profile);
        }
        projectMeta.getDataSourceRegistry().flushConfig();
    }

    @Override
    protected boolean deleteProfile(DBWNetworkProfile selectedProfile) {
        List<? extends DBPDataSourceContainer> usedBy = projectMeta
            .getDataSourceRegistry().getDataSourcesByProfile(selectedProfile);
        if (!usedBy.isEmpty()) {
            UIUtils.showMessageBox(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                NLS.bind(UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_info, new Object[]{
                    selectedProfile.getProfileName(),
                    usedBy.size(),
                    usedBy.stream()
                        .sorted(Comparator.comparing(DBPNamedObject::getName))
                        .map(x -> " - " + x.getName())
                        .collect(Collectors.joining("\n"))
                }),
                SWT.ICON_ERROR
            );
            return false;
        }
        if (UIUtils.confirmAction(
            getShell(),
            UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_title,
            NLS.bind(
                UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_question,
                selectedProfile.getProfileName()
            )
        )) {
            projectMeta.getDataSourceRegistry().removeNetworkProfile(selectedProfile);
            projectMeta.getDataSourceRegistry().flushConfig();

            return true;
        }
        return false;
    }

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

            if (projectMeta.getDataSourceRegistry().getNetworkProfile(null, profileName) != null) {
                UIUtils.showMessageBox(
                    getShell(),
                    UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_title,
                    NLS.bind(UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_info, profileName, projectMeta.getName()),
                    SWT.ICON_ERROR
                );

                continue;
            }

            break;
        }

        DBWNetworkProfile newProfile = new DBWNetworkProfile(projectMeta);
        newProfile.setProfileName(profileName);

        projectMeta.getDataSourceRegistry().updateNetworkProfile(newProfile);
        projectMeta.getDataSourceRegistry().flushConfig();

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

}
