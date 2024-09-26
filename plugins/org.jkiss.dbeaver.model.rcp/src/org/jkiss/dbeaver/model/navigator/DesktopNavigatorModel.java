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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;

public class DesktopNavigatorModel extends DBNModel {

    private NavigatorResourceListener resourceListener;

    public DesktopNavigatorModel(DBPPlatform platform, @Nullable List<? extends DBPProject> modelProjects) {
        super(platform, modelProjects);
    }

    public void initialize() {
        super.initialize();
        if (isGlobal()) {
            DBPPlatform platform = DBWorkbench.getPlatform();
            if (platform instanceof DBPPlatformDesktop platformDesktop) {
                resourceListener = new NavigatorResourceListener(this);
                platformDesktop.getWorkspace().getEclipseWorkspace().addResourceChangeListener(
                    resourceListener
                );
            }
        }
    }

    public void dispose() {
        if (isGlobal()) {
            DBPPlatform platform = DBWorkbench.getPlatform();
            if (platform instanceof DBPPlatformDesktop platformDesktop) {
                platformDesktop.getWorkspace().getEclipseWorkspace().removeResourceChangeListener(
                    resourceListener);
                resourceListener = null;
            }
        }
        super.dispose();
    }

    @Override
    protected DBNProject createProjectNode(DBNRoot parent, DBPProject project) {
        DBPPlatform platform = DBWorkbench.getPlatform();
        if (platform instanceof DBPPlatformDesktop platformDesktop && project instanceof RCPProject rcpProject) {
            return new DBNProjectDesktop(
                parent,
                rcpProject,
                platformDesktop.getWorkspace().getResourceHandler(rcpProject.getEclipseProject()));
        } else {
            throw new IllegalStateException("Desktop navigator model can be used only in RCP applications");
        }
    }

}
