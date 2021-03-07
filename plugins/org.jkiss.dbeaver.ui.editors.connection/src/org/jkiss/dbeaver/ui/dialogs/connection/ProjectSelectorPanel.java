/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

package org.jkiss.dbeaver.ui.dialogs.connection;

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
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;

import java.util.List;

/**
 * ProjectSelectorPanel
 */
public class ProjectSelectorPanel {

    private DBPProject selectedProject;

    public ProjectSelectorPanel(@NotNull Composite parent, @Nullable DBPProject activeProject, int style) {
        this.selectedProject = activeProject;

        final List<DBPProject> projects = DBWorkbench.getPlatform().getWorkspace().getProjects();
        if (projects.size() == 1) {
            if (selectedProject == null) {
                selectedProject = projects.get(0);
            }
        } else if (projects.size() > 1) {

            boolean showIcon = (style & SWT.ICON) != 0;
            Composite projectGroup = UIUtils.createComposite(parent, showIcon ? 3 : 2);
            projectGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            if (showIcon) {
                new Label(projectGroup, SWT.NONE).setImage(DBeaverIcons.getImage(DBIcon.PROJECT));
            }
            UIUtils.createControlLabel(projectGroup, UIConnectionMessages.dialog_connection_driver_project);

            final Combo projectCombo = new Combo(projectGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            projectCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            for (DBPProject project : projects) {
                projectCombo.add(project.getName());
            }

            if (selectedProject == null) {
                selectedProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
                if (!projects.contains(selectedProject)) {
                    selectedProject = projects.get(0);
                }
            }
            projectCombo.setText(selectedProject.getName());
            projectCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    selectedProject = projects.get(projectCombo.getSelectionIndex());
                    onProjectChange();
                }
            });

            if (projects.size() < 2) {
                //projectCombo.setEnabled(false);
            }
        }
    }

    protected void onProjectChange() {

    }

    public DBPProject getSelectedProject() {
        return selectedProject;
    }
}