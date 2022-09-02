/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.List;

/**
 * DBeaver desktop workspace.
 */
public class DesktopWorkspaceImpl extends EclipseWorkspaceImpl implements DBPWorkspaceDesktop {

    private static final Log log = Log.getLog(DesktopWorkspaceImpl.class);

    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<>();
    private DBPResourceHandler defaultHandler;

    public DesktopWorkspaceImpl(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        super(platform, eclipseWorkspace);

        loadExtensions(Platform.getExtensionRegistry());
    }

    private void loadExtensions(IExtensionRegistry registry) {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ResourceHandlerDescriptor handlerDescriptor = new ResourceHandlerDescriptor(ext);
                handlerDescriptors.add(handlerDescriptor);

                if (handlerDescriptor.isDefault()) {
                    defaultHandler = handlerDescriptor.getHandler();
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        // Dispose resource handlers
        for (ResourceHandlerDescriptor handlerDescriptor : this.handlerDescriptors) {
            handlerDescriptor.dispose();
        }
        this.handlerDescriptors.clear();
    }

    private ResourceHandlerDescriptor getHandlerDescriptor(String id) {
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            if (rhd.getId().equals(id)) {
                return rhd;
            }
        }
        return null;
    }

    private ResourceHandlerDescriptor getHandlerDescriptorByRootPath(DBPProject project, String path) {
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            String defaultRoot = rhd.getDefaultRoot(project);
            if (defaultRoot != null && defaultRoot.equals(path)) {
                return rhd;
            }
        }
        return null;
    }

    @Override
    public DBPResourceHandler getResourceHandler(IResource resource) {
        if (DBWorkbench.getPlatform().getApplication().isExclusiveMode()) {
            // Resource handlers are disabled in exclusive mode
            return null;
        }
        if (resource == null || resource.isHidden() || resource.isPhantom()) {
            // Skip not accessible hidden and phantom resources
            return null;
        }
        if (resource.getParent() instanceof IProject && resource.getName().startsWith(DataSourceRegistry.LEGACY_CONFIG_FILE_PREFIX)) {
            // Skip connections settings file
            // TODO: remove in some older version
            return null;
        }
        // Check resource is synced
//        if (resource instanceof IFile && !resource.isSynchronized(IResource.DEPTH_ZERO)) {
//            ContentUtils.syncFile(new VoidProgressMonitor(), resource);
//        }

        // Find handler
        DBPResourceHandler handler = null;
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            if (rhd.canHandle(resource)) {
                handler = rhd.getHandler();
                break;
            }
        }
        if (handler == null && resource instanceof IFolder) {
            final IProject eclipseProject = resource.getProject();
            DBPProject project = projects.get(eclipseProject);
            IPath relativePath = resource.getFullPath().makeRelativeTo(project.getRootResource().getFullPath());
            while (relativePath.segmentCount() > 0) {
                String folderPath = relativePath.toString();
                ResourceHandlerDescriptor handlerDescriptor = getHandlerDescriptorByRootPath(project, folderPath);
                if (handlerDescriptor != null) {
                    handler = handlerDescriptor.getHandler();
                }
                relativePath = relativePath.removeLastSegments(1);
            }
        }
        if (handler == null) {
            handler = getDefaultResourceHandler();
        }
        return handler;
    }

    @Override
    @NotNull
    public DBPResourceHandler getDefaultResourceHandler() {
        return defaultHandler;
    }

    @Override
    public DBPResourceHandlerDescriptor[] getAllResourceHandlers() {
        return handlerDescriptors.toArray(new DBPResourceHandlerDescriptor[0]);
    }

    @Override
    public IFolder getResourceDefaultRoot(DBPProject project, Class<? extends DBPResourceHandler> handlerType, boolean forceCreate) {
        if (project == null) {
            return null;
        }
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            DBPResourceHandler handler = rhd.getHandler();
            if (handler != null && handler.getClass() == handlerType) {
                String defaultRoot = rhd.getDefaultRoot(project);
                if (defaultRoot == null) {
                    // No root
                    return null;
                }
                org.eclipse.core.runtime.Path defaultRootPath = new org.eclipse.core.runtime.Path(defaultRoot);
                IContainer rootResource = project.getRootResource();
                if (rootResource == null) {
                    rootResource = project.getEclipseProject();
                }
                if (rootResource == null) {
                    throw new IllegalStateException("Project " + project.getName() + " doesn't have resource root");
                }
                final IFolder realFolder = rootResource.getFolder(defaultRootPath);

                if (forceCreate && !realFolder.exists()) {
                    try {
                        realFolder.create(true, true, new NullProgressMonitor());
                    } catch (CoreException e) {
                        log.error("Can not create '" + rhd.getName() + "' root folder '" + realFolder.getName() + "'", e);
                        return realFolder;
                    }
                }
                return realFolder;
            }
        }
        return project.getEclipseProject().getFolder(DEFAULT_RESOURCES_ROOT);
    }

    @Override
    public DBPResourceHandlerDescriptor[] getResourceHandlerDescriptors() {
        DBPResourceHandlerDescriptor[] result = new DBPResourceHandlerDescriptor[handlerDescriptors.size()];
        for (int i = 0; i < handlerDescriptors.size(); i++) {
            result[i] = handlerDescriptors.get(i);
        }
        return result;
    }

    @Override
    public IFolder getResourceDefaultRoot(DBPProject project, DBPResourceHandlerDescriptor rhd, boolean forceCreate) {
        if (project == null || project.getRootResource() == null) {
            return null;
        }
        String defaultRoot = rhd.getDefaultRoot(project);
        if (defaultRoot == null) {
            // No root
            return null;
        }
        final IFolder realFolder = project.getRootResource().getFolder(new org.eclipse.core.runtime.Path(defaultRoot));

        if (forceCreate && !realFolder.exists()) {
            try {
                realFolder.create(true, true, new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Can't create '" + rhd.getName() + "' root folder '" + realFolder.getName() + "'", e);
                return realFolder;
            }
        }
        return realFolder;
    }

}