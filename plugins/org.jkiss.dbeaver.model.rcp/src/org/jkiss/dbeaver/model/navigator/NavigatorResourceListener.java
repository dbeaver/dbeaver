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

import org.eclipse.core.resources.*;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

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
                            handleResourceChange(projectNode, childDelta);
                        }
                    }
                }
            }
        }
    }

    public void refreshResourceState(DBNNode node, Object source) {
        if (node instanceof DBNResource resourceNode) {
            DBPResourceHandler newHandler = DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resourceNode.getResource());
            if (newHandler != resourceNode.getHandler()) {
                resourceNode.setHandler(newHandler);
            }
            model.fireNodeEvent(new DBNEvent(source, DBNEvent.Action.UPDATE, node));
        }
    }

    public void handleResourceChange(DBNNode node, IResourceDelta delta) {
        if (delta.getKind() == IResourceDelta.CHANGED) {
            // Update this node in navigator
            refreshResourceState(node, delta);
        }
        if (node instanceof DBNLazyNode lazyNode && lazyNode.needsInitialization()) {
            // Child nodes are not yet read so nothing to change here - just return
            return;
        }
        //delta.getAffectedChildren(IResourceDelta.ALL_WITH_PHANTOMS, IContainer.INCLUDE_HIDDEN)
        for (IResourceDelta childDelta : delta.getAffectedChildren(IResourceDelta.ALL_WITH_PHANTOMS, IContainer.INCLUDE_HIDDEN)) {
            handleChildResourceChange(node, childDelta);
        }
    }

    public void handleChildResourceChange(DBNNode parentNode, IResourceDelta delta) {
        if (parentNode instanceof DBNProject project) {
            if (handleProjectChanges(project, delta)) {
                return;
            }
        }
        if (!(parentNode instanceof DBNNodeWithCache nodeWithCache) || nodeWithCache.needsInitialization()) {
            return;
        }
        final IResource deltaResource = delta.getResource();
        DBNNode childResource = NavigatorResources.getChild(parentNode, deltaResource);
        if (childResource == null) {
            if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED) {
                // New child or new "grand-child"
                DBNNode newChild = NavigatorResources.getChild(parentNode, deltaResource);
                if (newChild == null) {
                    newChild = NavigatorResources.makeNode(parentNode, deltaResource);
                    if (newChild != null) {
                        DBNNode[] children = nodeWithCache.getCachedChildren();
                        children = ArrayUtils.add(DBNNode.class, children, newChild);
                        NavigatorResources.sortChildren(children);
                        nodeWithCache.setCachedChildren(children);
                        model.fireNodeEvent(new DBNEvent(delta, DBNEvent.Action.ADD, newChild));

                        if (delta.getKind() == IResourceDelta.CHANGED) {
                            // Notify just created resource
                            // This may happen (e.g.) when first script created in just created script folder
                            childResource = NavigatorResources.getChild(parentNode, deltaResource);
                            if (childResource != null) {
                                handleResourceChange(childResource, delta);
                            }
                        }
                    }
                }
            }
        } else {
            if (delta.getKind() == IResourceDelta.REMOVED) {
                // Node deleted
                DBNNode[] children = nodeWithCache.getCachedChildren();
                children = ArrayUtils.remove(DBNNode.class, children, childResource);
                nodeWithCache.setCachedChildren(children);
                DBNUtils.disposeNode(childResource, true);
            } else {
                // Node changed - handle it recursive
                handleResourceChange(childResource, delta);
            }
        }
    }

    private boolean handleProjectChanges(DBNProject projectNode, IResourceDelta delta) {
        if (projectNode.getProject() instanceof RCPProject rcpProject &&
            CommonUtils.equalObjects(delta.getResource(), rcpProject.getRootResource())) {
            // Go inside root resource
            for (IResourceDelta cChild : delta.getAffectedChildren()) {
                handleChildResourceChange(projectNode, cChild);
            }
            return true;
        }
        final String name = delta.getResource().getName();
        if (name.equals(DBPProject.METADATA_FOLDER)) {
            // Metadata configuration changed
            IResourceDelta[] configFiles = delta.getAffectedChildren();
            boolean dsChanged = false;
            if (configFiles != null) {
                for (IResourceDelta rd : configFiles) {
                    IResource childRes = rd.getResource();
                    if (childRes instanceof IFile && childRes.getName().startsWith(DBPDataSourceRegistry.MODERN_CONFIG_FILE_PREFIX)) {
                        dsChanged = true;
                    }
                }
            }
            if (dsChanged) {
                projectNode.getDatabases().getDataSourceRegistry().refreshConfig();
            }
            return true;
        } else {
            return false;
        }
    }

}
