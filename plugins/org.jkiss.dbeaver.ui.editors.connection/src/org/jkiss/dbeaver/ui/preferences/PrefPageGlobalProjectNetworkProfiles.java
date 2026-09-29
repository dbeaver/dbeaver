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
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.registry.GlobalNetworkProfileManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A preference page that shows network profiles for all projects.
 */
public final class PrefPageGlobalProjectNetworkProfiles extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.globalNetworkProfiles";

    private final ProfileUsageCache profileUsageCache = new ProfileUsageCache();
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

        profileUsageCache.load(projects);

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

        profileUsageCache.selectProject(project);

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

        @Override
        protected boolean confirmProfileDeletion(
            @NotNull DBWNetworkProfile profile,
            @NotNull List<? extends DBPDataSourceContainer> usedBy
        ) {
            if (usedBy.isEmpty()) {
                return super.confirmProfileDeletion(profile, usedBy);
            }
            return UIUtils.confirmAction(
                getShell(),
                UIConnectionMessages.pref_page_network_profiles_tool_delete_confirmation_title,
                withPrivateProjectsWarning(NLS.bind(
                    UIConnectionMessages.pref_page_network_profiles_tool_delete_used_confirmation_question,
                    profile.getProfileName(),
                    usedBy.size(),
                    formatConnectionsUsingProfile(usedBy)
                ))
            );
        }

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
        protected boolean checkName(@NotNull String profileName) {
            if (getProfilesRegistry().getProfile(null, profileName) != null) {
                UIUtils.showMessageBox(
                    getShell(),
                    UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_title,
                    NLS.bind(UIConnectionMessages.pref_page_network_profiles_tool_create_dialog_error_global_info, profileName),
                    SWT.ICON_ERROR
                );
                return false;
            }
            List<String> projectsWithSameProfileName = profileUsageCache.getProjectsWithLocalProfile(profileName).stream()
                .map(DBPProject::getName)
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
        protected GlobalNetworkProfileManager getProfilesRegistry() {
            if (DBWorkbench.getPlatform().getNetworkProfiles() instanceof GlobalNetworkProfileManager manager) {
                return manager;
            }
            throw new IllegalStateException("Global network profile manager expected");
        }

        @NotNull
        @Override
        protected List<? extends DBPDataSourceContainer> connectionsUsingProfile(@NotNull DBWNetworkProfile selectedProfile) {
            return profileUsageCache.getConnectionsUsingGlobalProfile(selectedProfile.getProfileName());
        }

        @NotNull
        @Override
        protected String formatConnectionsUsingProfile(@NotNull List<? extends DBPDataSourceContainer> dataSources) {
            return dataSources.stream()
                .collect(Collectors.groupingBy(DBPDataSourceContainer::getProject))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
                .map(entry -> " " + entry.getKey().getName() + "\n" + entry.getValue().stream()
                    .sorted(Comparator.comparing(DBPDataSourceContainer::getName))
                    .map(dataSource -> "   - " + dataSource.getName())
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

        @Override
        protected void removeProfile(
            @NotNull DBWNetworkProfile profile,
            @NotNull List<? extends DBPDataSourceContainer> usedBy
        ) throws DBException {
            try {
                getProfilesRegistry().detachProfile(profile, usedBy);
            } finally {
                usedBy.stream()
                    .map(DBPDataSourceContainer::getProject)
                    .distinct()
                    .forEach(profileUsageCache::refreshProject);
            }
            super.removeProfile(profile, usedBy);
        }
    }

    private static class ProfileUsageCache {
        private final Map<String, List<DBPProject>> projectsByLocalProfileName = new HashMap<>();
        private final Map<String, List<DBPDataSourceContainer>> connectionsByGlobalProfileName = new HashMap<>();
        private DBPProject activeProject;

        void load(@NotNull List<? extends DBPProject> projects) {
            projectsByLocalProfileName.clear();
            connectionsByGlobalProfileName.clear();
            projects.forEach(this::addProject);
        }

        void selectProject(@Nullable DBPProject project) {
            if (activeProject != null) {
                refreshProject(activeProject);
            }
            activeProject = project;
        }

        void refreshProject(@NotNull DBPProject project) {
            projectsByLocalProfileName.values().forEach(projects -> projects.remove(project));
            projectsByLocalProfileName.values().removeIf(List::isEmpty);
            connectionsByGlobalProfileName.values().forEach(connections ->
                connections.removeIf(connection -> connection.getProject() == project));
            connectionsByGlobalProfileName.values().removeIf(List::isEmpty);
            addProject(project);
        }

        @NotNull
        List<DBPProject> getProjectsWithLocalProfile(@NotNull String profileName) {
            return projectsByLocalProfileName.getOrDefault(profileName, List.of());
        }

        @NotNull
        List<DBPDataSourceContainer> getConnectionsUsingGlobalProfile(@NotNull String profileName) {
            return connectionsByGlobalProfileName.getOrDefault(profileName, List.of());
        }

        private void addProject(@NotNull DBPProject project) {
            var registry = project.getDataSourceRegistry();
            Set<String> localProfileNames = new HashSet<>();
            for (DBWNetworkProfile profile : registry.getNetworkProfiles().getProfiles()) {
                localProfileNames.add(profile.getProfileName());
                projectsByLocalProfileName.computeIfAbsent(profile.getProfileName(), name -> new ArrayList<>()).add(project);
            }
            for (DBPDataSourceContainer connection : registry.getDataSources()) {
                var configuration = connection.getConnectionConfiguration();
                String profileName = configuration.getConfigProfileName();
                if (configuration.getConfigProfileSource() == null && profileName != null &&
                    !localProfileNames.contains(profileName)) {
                    connectionsByGlobalProfileName.computeIfAbsent(profileName, name -> new ArrayList<>()).add(connection);
                }
            }
        }
    }
}
