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
package org.jkiss.dbeaver.model.rcp;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.registry.ResourceHandlerDescriptor;
import org.jkiss.dbeaver.registry.ResourceTypeDescriptor;
import org.jkiss.dbeaver.registry.ResourceTypeRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * DBeaver desktop workspace.
 */
public class DesktopWorkspaceImpl extends EclipseWorkspaceImpl implements DBPWorkspaceDesktop, DBPExternalFileManager {

    private static final Log log = Log.getLog(DesktopWorkspaceImpl.class);

    private static final String EXT_FILES_PROPS_STORE = "dbeaver-external-files.data";

    private final Map<String, Map<String, Object>> externalFileProperties = new HashMap<>();

    private final AbstractJob externalFileSaver = new WorkspaceFilesMetadataJob();

    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<>();
    private DBPResourceHandler defaultHandler;

    public DesktopWorkspaceImpl(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        super(platform, eclipseWorkspace);

        loadExtensions(Platform.getExtensionRegistry());
        loadExternalFileProperties();
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

    private DBPResourceHandler getResourceHandler(DBPResourceTypeDescriptor resourceType) {
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            if (rhd.getTypeId().equals(resourceType.getId())) {
                return rhd.getHandler();
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
        if (resource.getParent() instanceof IProject && resource.getName().startsWith(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_PREFIX)) {
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
            DesktopProjectImpl project = projects.get(eclipseProject);
            IPath relativePath = resource.getFullPath().makeRelativeTo(project.getRootResource().getFullPath());
            while (relativePath.segmentCount() > 0) {
                String folderPath = relativePath.toString();
                ResourceTypeDescriptor resType = ResourceTypeRegistry.getInstance().getResourceTypeByRootPath(project, folderPath);
                if (resType != null) {
                    handler = getResourceHandler(resType);
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

    @NotNull
    @Override
    public DBPResourceHandlerDescriptor[] getAllResourceHandlers() {
        return handlerDescriptors.toArray(new DBPResourceHandlerDescriptor[0]);
    }


    @Override
    public DBPImage getResourceIcon(DBPAdaptable resourceAdapter) {
        IResource resource = resourceAdapter.getAdapter(IResource.class);
        if (resource != null) {
            return defaultHandler.getResourceIcon(resource);
        }
        return null;
    }

    @Override
    public IFolder getResourceDefaultRoot(@NotNull DBPProject project, @NotNull Class<? extends DBPResourceHandler> handlerType, boolean forceCreate) {
        if (!(project instanceof RCPProject rcpProject)) {
            return null;
        }
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            DBPResourceHandler handler = rhd.getHandler();
            if (handler != null && handler.getClass() == handlerType) {
                DBPResourceTypeDescriptor resourceType = rhd.getResourceType();
                if (resourceType == null) {
                    return null;
                }
                String defaultRoot = resourceType.getDefaultRoot(project);
                if (defaultRoot == null) {
                    // No root
                    return null;
                }
                org.eclipse.core.runtime.Path defaultRootPath = new org.eclipse.core.runtime.Path(defaultRoot);
                IContainer rootResource = rcpProject.getRootResource();
                if (rootResource == null) {
                    rootResource = rcpProject.getEclipseProject();
                }
                if (rootResource == null) {
                    throw new IllegalStateException("Project " + project.getName() + " doesn't have resource root");
                }
                final IFolder realFolder = rootResource.getFolder(defaultRootPath);

                if (forceCreate && !realFolder.exists()) {
                    try {
                        realFolder.create(true, true, new NullProgressMonitor());
                    } catch (CoreException e) {
                        log.error("Can not create '" + resourceType.getName() + "' root folder '" + realFolder.getName() + "'", e);
                        return realFolder;
                    }
                }
                return realFolder;
            }
        }
        return rcpProject.getEclipseProject().getFolder(BaseWorkspaceImpl.DEFAULT_RESOURCES_ROOT);
    }

    @Override
    public IFolder getResourceDefaultRoot(@NotNull DBPProject project, @NotNull DBPResourceHandlerDescriptor rhd, boolean forceCreate) {
        if (!(project instanceof RCPProject rcpProject) || rcpProject.getRootResource() == null) {
            return null;
        }
        DBPResourceTypeDescriptor resourceType = rhd.getResourceType();
        if (resourceType == null) {
            return null;
        }
        String defaultRoot = resourceType.getDefaultRoot(project);
        if (defaultRoot == null) {
            // No root
            return null;
        }
        final IFolder realFolder = rcpProject.getRootResource().getFolder(new org.eclipse.core.runtime.Path(defaultRoot));

        if (forceCreate && !realFolder.exists()) {
            try {
                realFolder.create(true, true, new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Can't create '" + resourceType.getName() + "' root folder '" + realFolder.getName() + "'", e);
                return realFolder;
            }
        }
        return realFolder;
    }

    @Override
    public void refreshWorkspaceContents(@NotNull DBRProgressMonitor monitor) {
        try {
            IWorkspaceRoot root = getEclipseWorkspace().getRoot();

            root.refreshLocal(IResource.DEPTH_ONE, monitor.getNestedMonitor());

            File workspaceLocation = root.getLocation().toFile();
            if (!workspaceLocation.exists()) {
                // Nothing to refresh
                return;
            }

            // Remove unexistent projects
            for (IProject project : root.getProjects()) {
                File projectDir = project.getLocation().toFile();
                if (!projectDir.exists()) {
                    monitor.subTask("Removing unexistent project '" + project.getName() + "'");
                    project.delete(false, true, monitor.getNestedMonitor());
                }
            }

            File[] wsFiles = workspaceLocation.listFiles();
            if (!ArrayUtils.isEmpty(wsFiles)) {
                // Add missing projects
                monitor.beginTask("Refreshing workspace contents", wsFiles.length);
                for (File wsFile : wsFiles) {
                    if (!wsFile.isDirectory() || wsFile.isHidden() || wsFile.getName().startsWith(".")) {
                        // skip regular files
                        continue;
                    }
                    File projectConfig = new File(wsFile, IProjectDescription.DESCRIPTION_FILE_NAME);
                    if (projectConfig.exists()) {
                        String projectName = wsFile.getName();
                        IProject project = root.getProject(projectName);
                        if (project.exists()) {
                            continue;
                        }
                        try {
                            monitor.subTask("Adding project '" + projectName + "'");
                            project.create(monitor.getNestedMonitor());
                        } catch (CoreException e) {
                            log.error("Error adding project '" + projectName + "' to workspace");
                        }
                    }
                }
            }

        } catch (Throwable e) {
            log.error("Error refreshing workspace contents", e);
        }
    }

    @NotNull
    @Override
    public DBPProject createProject(@NotNull String name, @Nullable String description) throws DBException {
        IProject project = null;
        try {
            project = getEclipseWorkspace().getRoot().getProject(name);
            NullProgressMonitor monitor = new NullProgressMonitor();
            if (project.exists()) {
                project.create(monitor);
            }
            final IProjectDescription pDescription = getEclipseWorkspace().newProjectDescription(project.getName());
            if (!CommonUtils.isEmpty(description)) {
                pDescription.setComment(description);
            }
            pDescription.setNatureIds(new String[]{DBeaverNature.NATURE_ID});
            project.setDescription(pDescription, monitor);
            project.open(monitor);
        } catch (Exception e) {
            throw new DBException("Error creating Eclipse project", e);
        }

        return getProject(project);
    }

    @Override
    public void deleteProject(@NotNull DBPProject project, boolean deleteContents) throws DBException {
        if (!(project instanceof RCPProject rcpProject)) {
            throw new DBException("Project '" + project.getName() + "' is not an RCP project");
        }
        if (project == activeProject) {
            throw new DBException("You cannot delete active project");
        }
        IProject eclipseProject = rcpProject.getEclipseProject();
        if (eclipseProject == null) {
            throw new DBException("Project '" + project.getName() + "' is not an Eclipse project");
        }
        if (project.isUseSecretStorage()) {
            var secretController = DBSSecretController.getProjectSecretController(project);
            secretController.deleteProjectSecrets(project.getId());
        }
        try {
            eclipseProject.delete(deleteContents, true, new NullProgressMonitor());
        } catch (CoreException e) {
            throw new DBException("Error deleting Eclipse project '" + project.getName() + "'", e);
        }
    }

    @Override
    protected void reloadWorkspace(DBRProgressMonitor monitor) {
        refreshWorkspaceContents(monitor);
    }

    @Override
    public Map<String, Object> getFileProperties(File file) {
        synchronized (externalFileProperties) {
            return externalFileProperties.get(file.getAbsolutePath());
        }
    }

    @Override
    public Object getFileProperty(File file, String property) {
        synchronized (externalFileProperties) {
            final Map<String, Object> fileProps = externalFileProperties.get(file.getAbsolutePath());
            return fileProps == null ? null : fileProps.get(property);
        }
    }

    @Override
    public void setFileProperty(File file, String property, Object value) {
        synchronized (externalFileProperties) {
            final String filePath = file.getAbsolutePath();
            Map<String, Object> fileProps = externalFileProperties.get(filePath);
            if (fileProps == null) {
                fileProps = new HashMap<>();
                externalFileProperties.put(filePath, fileProps);
            }
            if (value == null) {
                fileProps.remove(property);
            } else {
                fileProps.put(property, value);
            }
        }

        saveExternalFileProperties();
    }

    @Override
    public Map<String, Map<String, Object>> getAllFiles() {
        synchronized (externalFileProperties) {
            return new LinkedHashMap<>(externalFileProperties);
        }
    }

    private void loadExternalFileProperties() {
        synchronized (externalFileProperties) {
            externalFileProperties.clear();
            java.nio.file.Path propsFile = GeneralUtils.getMetadataFolder(getAbsolutePath())
                .resolve(EXT_FILES_PROPS_STORE);
            if (Files.exists(propsFile)) {
                try (InputStream is = Files.newInputStream(propsFile)) {
                    try (ObjectInputStream ois = new ObjectInputStream(is)) {
                        final Object object = ois.readObject();
                        if (object instanceof Map) {
                            externalFileProperties.putAll((Map) object);
                        } else {
                            log.error("Bad external files properties data format: " + object);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error saving external files properties", e);
                }
            }
        }
    }

    private void saveExternalFileProperties() {
        synchronized (externalFileProperties) {
            externalFileSaver.schedule(100);
        }
    }

    private class WorkspaceFilesMetadataJob extends AbstractJob {
        public WorkspaceFilesMetadataJob() {
            super("External files metadata saver");
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            synchronized (externalFileProperties) {
                java.nio.file.Path propsFile = GeneralUtils.getMetadataFolder(getAbsolutePath())
                    .resolve(EXT_FILES_PROPS_STORE);
                try (OutputStream os = Files.newOutputStream(propsFile)) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                        oos.writeObject(externalFileProperties);
                    }
                } catch (Exception e) {
                    log.error("Error saving external files properties", e);
                }
            }
            return Status.OK_STATUS;
        }
    }

}