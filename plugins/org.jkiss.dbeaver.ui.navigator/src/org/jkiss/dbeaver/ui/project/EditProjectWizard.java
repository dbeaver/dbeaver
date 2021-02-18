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
package org.jkiss.dbeaver.ui.project;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizard;

/**
 * EditProjectWizard
 */
public class EditProjectWizard extends ActiveWizard {

    private static final Log log = Log.getLog(EditProjectWizard.class);

    private final DBPProject project;

    public EditProjectWizard(DBPProject project) {
        this.project = project;
    }

    @Override
    public String getWindowTitle() {
        return "Project " + project.getName() + " settings";
    }

    @Override
    public Image getDefaultPageImage() {
        return DBeaverIcons.getImage(DBIcon.PROJECT);
    }

    @Override
    public void addPages() {
        IPreferenceNode[] preferenceNodes = PreferencesUtil.propertiesContributorsFor(project.getEclipseProject());
        createPreferencePages(preferenceNodes);
        //addPreferencePage(new PrefPageProjectNetworkProfiles(), "Network profiles", "Connections' network profiles");
        //addPreferencePage(new PrefPageProjectResourceSettings(), "Resource settings", "Project resource folders/locations");
    }

    @Override
    protected IAdaptable getActiveElement() {
        return project.getEclipseProject();
    }

    @Override
    public boolean performFinish() {
        super.savePrefPageSettings();
        project.getDataSourceRegistry().flushConfig();
        return true;
    }
}
