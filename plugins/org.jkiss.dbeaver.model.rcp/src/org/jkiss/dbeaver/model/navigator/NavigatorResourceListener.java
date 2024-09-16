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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;

public class NavigatorResourceListener implements IResourceChangeListener {

    private static final Log log = Log.getLog(NavigatorResourceListener.class);

    private final DesktopNavigatorModel model;

    public NavigatorResourceListener(DesktopNavigatorModel model) {
        this.model = model;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
            IResourceDelta delta = event.getDelta();
            for (IResourceDelta childDelta : delta.getAffectedChildren()) {
                if (childDelta.getResource() instanceof IProject project) {
                    DBNProject projectNode = NavigatorResources.getProjectNode(model.getRoot(), project);
                    if (projectNode == null) {
                        if (childDelta.getKind() == IResourceDelta.ADDED) {
                            // New projectNode
                            DBPProject projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(project);
                            if (projectMeta == null) {
                                log.error("Can't find project '" + project.getName() + "' metadata");
                            } else {
                                model.getRoot().addProject(projectMeta, true);
                            }
                        } else if (childDelta.getKind() != IResourceDelta.REMOVED) {
                            // Project not found - report an error
                            log.error("Project '" + childDelta.getResource().getName() + "' not found in navigator");
                        }
                    } else {
                        if (childDelta.getKind() == IResourceDelta.REMOVED) {
                            // Project deleted
                            DBPProject projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(project);
                            if (projectMeta == null) {
                                log.error("Can't find project '" + project.getName() + "' metadata");
                            } else {
                                model.getRoot().removeProject(projectMeta);
                            }
                        } else {
                            // Some resource changed within the projectNode
                            // Let it handle this event itself
                            projectNode.handleResourceChange(childDelta);
                        }
                    }
                }
            }
        }
    }


}
