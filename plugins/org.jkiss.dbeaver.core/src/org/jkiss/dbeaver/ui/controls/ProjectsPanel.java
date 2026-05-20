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

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerProjectSetActive;

import java.util.ArrayList;
import java.util.List;

/**
 * ProjectsPanel
 */
class ProjectsPanel implements DBPProjectListener {

    private DBPProject selectedProject;
    private final Combo projectCombo;
    private final List<DBPProject> projects;

    public ProjectsPanel(@NotNull Composite parent) {
        DBPPlatformDesktop.getInstance().getWorkspace().addProjectListener(this);
        parent.addDisposeListener(e ->
            DBPPlatformDesktop.getInstance().getWorkspace().removeProjectListener(this));

        projects = new ArrayList<>(
            DBWorkbench.getPlatform().getWorkspace().getProjects());
        projects.sort((o1, o2) ->
            o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName()));

        Composite projectGroup = UIUtils.createComposite(parent, 3);
        projectGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        new Label(projectGroup, SWT.NONE).setImage(DBeaverIcons.getImage(DBIcon.PROJECT));

        projectCombo = new Combo(projectGroup, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY | SWT.FLAT);
        projectCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        refillCombo();

        projectCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedProject = projects.get(projectCombo.getSelectionIndex());

                NavigatorHandlerProjectSetActive.setActiveProject(selectedProject);
            }
        });
    }

    @Nullable
    public DBPProject getSelectedProject() {
        return selectedProject;
    }

    public void setSelectedProject(@Nullable DBPProject selectedProject) {
        this.selectedProject = selectedProject;
        if (selectedProject == null) {
            projectCombo.setText("");
        } else {
            projectCombo.setText(selectedProject.getName());
        }
    }

    public void addProject(@NotNull DBPProject project) {
        projects.add(project);
        projects.sort((o1, o2) ->
            o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName()));
        UIUtils.asyncExec(this::refillCombo);
    }

    public void removeProject(@NotNull DBPProject project) {
        projects.remove(project);
        UIUtils.asyncExec(this::refillCombo);
    }

    private void refillCombo() {
        projectCombo.removeAll();
        for (DBPProject project : projects) {
            projectCombo.add(project.getName());
        }

        selectedProject = NavigatorUtils.getSelectedProject();
        if (selectedProject == null  || !projects.contains(selectedProject)) {
            selectedProject = projects.getFirst();
        }

        projectCombo.setText(selectedProject == null ? "" : selectedProject.getName());
    }

    @Override
    public void handleProjectAdd(@NotNull DBPProject project) {
        addProject(project);
    }

    @Override
    public void handleProjectRemove(@NotNull DBPProject project) {
        removeProject(project);
    }

    @Override
    public void handleActiveProjectChange(@NotNull DBPProject oldValue, @NotNull DBPProject newValue) {
        setSelectedProject(newValue);
    }

}