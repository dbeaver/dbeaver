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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.net.DBWNetworkProfile;
import org.jkiss.dbeaver.model.net.DBWNetworkProfileUsageProvider.ProjectConnections;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.registry.GlobalNetworkProfileManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A preference page that shows network profiles for all projects.
 */
public final class PrefPageGlobalProjectNetworkProfiles extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.globalNetworkProfiles";

    private PrefPageManagedNetworkProfiles networkProfilesPage;
    private Composite networkProfilesPageHolder;
    private int lastProjectIndex = -1;
    private Link projectInfoLink;

    @Override
    public void init(@NotNull IWorkbench workbench) {
        // do nothing
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());

        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        List<? extends DBPProject> projects = workspace.getProjects();

        Combo projectCombo = UIUtils.createLabelCombo(
            composite,
            UIConnectionMessages.pref_page_network_profiles_global_project_label,
            SWT.DROP_DOWN | SWT.READ_ONLY
        );
        projectCombo.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        projectCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            int selectionIndex = projectCombo.getSelectionIndex();
            if (selectionIndex == 0) {
                lastProjectIndex = -1;
                refreshActiveProject(null);
                projectInfoLink.setVisible(false);
                return;
            }
            DBPProject project = projects.get(selectionIndex - 1);
            if (!refreshActiveProject(project)) {
                // Failed to load another project, let's fall back to the old one...
                projectCombo.select(lastProjectIndex);
                return;
            }
            lastProjectIndex = selectionIndex - 1;
            projectInfoLink.setVisible(true);
        }));

        projectInfoLink = UIUtils.createInfoLink(
            composite,
            UIConnectionMessages.pref_page_network_profiles_global_project_hint,
            () -> {
                int selectionIndex = projectCombo.getSelectionIndex();
                if (selectionIndex < 1) {
                    refreshActiveProject(null);
                } else if (projects.get(selectionIndex - 1) instanceof RCPProject project) {
                    PrefPageProjectNetworkProfiles.open(getShell(), project, null);
                    refreshActiveProject(project);
                }
            }
        );

        networkProfilesPageHolder = new Composite(composite, SWT.NONE);
        networkProfilesPageHolder.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(3, 1).create());
        networkProfilesPageHolder.setLayout(new FillLayout());

        // Populate and select active project
        projectCombo.add("<Global>");
        for (DBPProject project : projects) {
            projectCombo.add(project.getDisplayName());
        }
        projectCombo.select(0);
        projectCombo.notifyListeners(SWT.Selection, new Event());

        return composite;
    }

    @Override
    public boolean performOk() {
        if (!super.performOk()) {
            return false;
        }
        if (networkProfilesPage != null) {
            networkProfilesPage.performOk();
        }
        return true;
    }

    @Override
    public void dispose() {
        if (networkProfilesPage != null) {
            networkProfilesPage.dispose();
        }
    }

    private boolean refreshActiveProject(@Nullable DBPProject project) {
        if (project != null && project.getDataSourceRegistry().hasError()) {
            DBWorkbench.getPlatformUI().showError(
                "Error opening project",
                NLS.bind(
                    "Can''t show network profiles for project ''{0}'' because it can''t be loaded",
                    project.getDisplayName()
                )
            );
            return false;
        }

        // It's easier to recreate the whole page... not ideal
        if (networkProfilesPage != null) {
            networkProfilesPage.getControl().dispose();
            networkProfilesPage.dispose();
            networkProfilesPage = null;
        }

        networkProfilesPage = createPrefPageNetworkProfiles(project);
        networkProfilesPage.createControl(networkProfilesPageHolder);
        networkProfilesPage.loadSettings();
        networkProfilesPageHolder.layout(true, true);

        return true;
    }

    @NotNull
    private PrefPageManagedNetworkProfiles createPrefPageNetworkProfiles(@Nullable DBPProject project) {
        if (project == null) {
            return new PrefPageGlobalNetworkProfiles();
        }
        PrefPageProjectNetworkProfiles projectPage = new PrefPageProjectNetworkProfiles();
        projectPage.setProjectMeta(project);
        return projectPage;
    }

    @NotNull
    private List<? extends DBPProject> getProjects() {
        return DBWorkbench.getPlatform().getWorkspace().getProjects();
    }

    private class PrefPageGlobalNetworkProfiles extends PrefPageManagedNetworkProfiles {

        @Nullable
        @Override
        protected DBSSecretController getSecretController() throws DBException {
            return DBSSecretController.getGlobalSecretController();
        }

        @NotNull
        @Override
        protected DBWNetworkProfile createProfile(@NotNull String profileName) {
            DBWNetworkProfile profile = new DBWNetworkProfile();
            profile.setProfileName(profileName);
            return profile;
        }

        @Override
        protected boolean isNameValid(@NotNull String profileName) throws DBException {
            if (getProfilesManager().getProfile(null, profileName) != null) {
                UIUtils.showMessageBox(
                    getShell(),
                    UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_title,
                    NLS.bind(UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_global_info, profileName),
                    SWT.ICON_ERROR
                );
                return false;
            }
            List<String> projectsWithSameProfileName = getProfilesManager().findLocalProfileConflicts(profileName).values().stream()
                .map(name -> " - " + name)
                .toList();
            return projectsWithSameProfileName.isEmpty() || UIUtils.confirmAction(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_global_project_name_used_in_local_label,
                NLS.bind(
                    UIConnectionMessages.pref_page_network_profiles_global_project_name_used_in_local_question,
                    profileName,
                    String.join("\n", projectsWithSameProfileName)
                )
            );
        }

        @NotNull
        @Override
        protected Image getProfileImage(@NotNull DBWNetworkProfile profile) {
            return DBeaverIcons.getImage(DBIcon.GLOBAL_PROFILE);
        }

        @NotNull
        @Override
        protected GlobalNetworkProfileManager getProfilesManager() {
            if (DBWorkbench.getPlatform().getNetworkProfiles() instanceof GlobalNetworkProfileManager manager) {
                return manager;
            }
            throw new IllegalStateException("Global network profile manager expected");
        }

        @Override
        protected boolean deleteProfile(@NotNull DBWNetworkProfile profile) {
            try {
                List<ProjectConnections> usedBy = getProfilesManager().findGlobalProfileConnections(profile.getProfileName());
                boolean confirmed = usedBy.isEmpty() ? super.confirmProfileDeletion(profile) : confirmUsedProfileDeletion(profile, usedBy);
                if (!confirmed) {
                    return false;
                }
                List<DBPDataSourceContainer> connections = resolveConnections(usedBy);
                getProfilesManager().detachProfile(profile, connections);
                super.removeProfile(profile);
                return true;
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_title,
                    NLS.bind(
                        UIConnectionMessages.pref_page_network_profiles_tool_delete_dialog_error_message,
                        profile.getProfileName()
                    ), e
                );
                return false;
            }
        }

        @NotNull
        private static List<DBPDataSourceContainer> resolveConnections(@NotNull List<ProjectConnections> usedBy) throws DBException {
            List<DBPDataSourceContainer> connections = new ArrayList<>();
            for (ProjectConnections projectUsage : usedBy) {
                DBPProject project = DBWorkbench.getPlatform().getWorkspace().getProjectById(projectUsage.projectId());
                if (project == null) {
                    throw new DBException("Project no longer exists: " + projectUsage.projectId());
                }
                var registry = project.getDataSourceRegistry();
                registry.checkForErrors();
                for (String id : projectUsage.connections().keySet()) {
                    DBPDataSourceContainer connection = registry.getDataSource(id);
                    if (connection == null) {
                        throw new DBException("Cannot resolve connection " + id + " in project " + project.getName());
                    }
                    connections.add(connection);
                }
            }
            return connections;
        }

        private boolean confirmUsedProfileDeletion(@NotNull DBWNetworkProfile profile, @NotNull List<ProjectConnections> usedBy) {
            return UIUtils.confirmAction(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_title,
                withPrivateProjectsWarning(NLS.bind(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_used_confirmation_question,
                    profile.getProfileName(), usedBy.stream().mapToInt(project -> project.connections().size()).sum(),
                    formatProjectConnections(usedBy)
                ))
            );
        }

        @NotNull
        private String formatProjectConnections(@NotNull List<ProjectConnections> usedBy) {
            return usedBy.stream()
                .sorted(Comparator.comparing(ProjectConnections::projectName))
                .map(project -> " " + project.projectName() + "\n" + project.connections().values().stream()
                    .sorted()
                    .map(name -> "   - " + name)
                    .collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n"));
        }

        @NotNull
        @Override
        protected String getDeleteConfirmationQuestion(@NotNull DBWNetworkProfile profile) {
            return withPrivateProjectsWarning(super.getDeleteConfirmationQuestion(profile));
        }

        @NotNull
        private String withPrivateProjectsWarning(@NotNull String question) {
            return DBWorkbench.isDistributed() && isPrivateProjectsEnabled()
                ? question + "\n" + UIConnectionMessages.pref_page_network_profiles_tool_delete_private_projects_warning
                : question;
        }

        private boolean isPrivateProjectsEnabled() {
            return getProjects().stream().anyMatch(DBPProject::isPrivateProject);
        }
    }
}
