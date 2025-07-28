/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class NavigatorViewPropertyPage extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    private static final String KEY_NAV_VIEW = "navigator.default.view";
    private static final String VIEW_TYPE_SIMPLE = "Simple";
    private static final String VIEW_TYPE_ADVANCED = "Advanced";

    private Combo combo;
    private DBPProject projectMeta;

    public NavigatorViewPropertyPage() {
        setTitle("Navigator Default View");
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite container = UIUtils.createPlaceholder(parent, 1);

        Group group = UIUtils.createControlGroup(container, "Navigator Settings", 2, GridData.FILL_HORIZONTAL, 0);

        UIUtils.createControlLabel(group, "Connection view");
        combo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        combo.add("Simple");
        combo.add("Advanced");

        loadValues();

        if (!currentUserIsAdmin()) {
            combo.setEnabled(false);
            setMessage("Only admin can change connection view", WARNING);
        }

        return container;
    }

    private boolean currentUserIsAdmin() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN);
    }

    private void loadValues() {
        if (projectMeta == null) {
            return;
        }

        Object viewTypeObj = projectMeta.getProjectProperty(KEY_NAV_VIEW);
        String viewType = viewTypeObj == null ? VIEW_TYPE_SIMPLE : viewTypeObj.toString();

        combo.select(VIEW_TYPE_ADVANCED.equals(viewType) ? 1 : 0);
    }

    @Override
    public boolean performOk() {
        projectMeta.setProjectProperty(KEY_NAV_VIEW, combo.getSelectionIndex() == 1 ? VIEW_TYPE_ADVANCED : VIEW_TYPE_SIMPLE);
        return super.performOk();
    }

        @Override
    public IAdaptable getElement() {
        return projectMeta instanceof RCPProject rcpProject ? rcpProject.getEclipseProject() : null;
    }

    @Override
    public void setElement(IAdaptable element) {
        IProject project;
        if (element instanceof DBNNode node && node.getOwnerProject() instanceof RCPProject rcpProject) {
            project = rcpProject.getEclipseProject();
        } else {
            project = GeneralUtils.adapt(element, IProject.class);
        }
        if (project != null) {
            this.projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(project);
        }
    }
}
