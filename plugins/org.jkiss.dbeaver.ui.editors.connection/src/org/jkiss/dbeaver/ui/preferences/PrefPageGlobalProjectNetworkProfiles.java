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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.util.List;

/**
 * A preference page that shows network profiles for all projects.
 */
public final class PrefPageGlobalProjectNetworkProfiles extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.globalNetworkProfiles";

    private PrefPageProjectNetworkProfiles networkProfilesPage;
    private Composite networkProfilesPageHolder;
    private int lastProjectIndex = -1;

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
            DBPProject project = projects.get(projectCombo.getSelectionIndex());
            if (!refreshActiveProject(project)) {
                // Failed to load another project, let's fall back to the old one...
                projectCombo.select(lastProjectIndex);
                return;
            }
            lastProjectIndex = projectCombo.getSelectionIndex();
        }));

        UIUtils.createInfoLink(
            composite,
            UIConnectionMessages.pref_page_network_profiles_global_project_hint,
            () -> {
                if (projects.get(projectCombo.getSelectionIndex()) instanceof RCPProject project) {
                    PrefPageProjectNetworkProfiles.open(getShell(), project, null);
                    refreshActiveProject(project);
                }
            }
        );

        networkProfilesPageHolder = new Composite(composite, SWT.NONE);
        networkProfilesPageHolder.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(3, 1).create());
        networkProfilesPageHolder.setLayout(new FillLayout());

        // Populate and select active project
        DBPProject activeProject = workspace.getActiveProject();
        for (DBPProject project : projects) {
            projectCombo.add(project.getDisplayName());
            if (project == activeProject) {
                projectCombo.select(projectCombo.getItemCount() - 1);
            }
        }
        if (projectCombo.getSelectionIndex() < 0) {
            projectCombo.select(0);
        }
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

    private boolean refreshActiveProject(@NotNull DBPProject project) {
        if (project.getDataSourceRegistry().hasError()) {
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

        networkProfilesPage = new PrefPageProjectNetworkProfiles();
        networkProfilesPage.setProjectMeta(project);
        networkProfilesPage.createControl(networkProfilesPageHolder);
        networkProfilesPage.loadSettings();

        return true;
    }
}
