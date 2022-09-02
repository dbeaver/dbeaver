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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.impl.auth.SessionContextImpl;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * BaseWorkspaceImpl.
 */
public abstract class BaseWorkspaceImpl implements DBPWorkspaceEclipse, DBPExternalFileManager {

    private static final Log log = Log.getLog(BaseWorkspaceImpl.class);

    public static final String DEFAULT_RESOURCES_ROOT = "Resources"; //$NON-NLS-1$

    protected static final String PROP_PROJECT_ACTIVE = "project.active";
    private static final String EXT_FILES_PROPS_STORE = "dbeaver-external-files.data";

    private static final String WORKSPACE_ID = "workspace-id";

    private final DBPPlatform platform;
    private final IWorkspace eclipseWorkspace;
    private final SessionContextImpl workspaceAuthContext;

    protected final Map<IProject, LocalProjectImpl> projects = new LinkedHashMap<>();
    protected DBPProject activeProject;

    private final List<DBPProjectListener> projectListeners = new ArrayList<>();
    private final Map<String, Map<String, Object>> externalFileProperties = new HashMap<>();

    private final AbstractJob externalFileSaver = new WorkspaceFilesMetadataJob();

    protected BaseWorkspaceImpl(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        this.platform = platform;
        this.eclipseWorkspace = eclipseWorkspace;
        this.workspaceAuthContext = new SessionContextImpl(null);

        loadExternalFileProperties();
    }

    @NotNull
    protected SMSession acquireWorkspaceSession(@NotNull DBRProgressMonitor monitor) throws DBException {
        return new BasicWorkspaceSession(this);
    }

    public abstract void initializeProjects();

    public static Properties readWorkspaceInfo(Path metadataFolder) {
        Properties props = new Properties();

        Path versionFile = metadataFolder.resolve(DBConstants.WORKSPACE_PROPS_FILE);
        if (Files.exists(versionFile)) {
            try (InputStream is = Files.newInputStream(versionFile)) {
                props.load(is);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return props;
    }

    public static void writeWorkspaceInfo(Path metadataFolder, Properties props) {
        Path versionFile = metadataFolder.resolve(DBConstants.WORKSPACE_PROPS_FILE);

        try (OutputStream os = Files.newOutputStream(versionFile)) {
            props.store(os, "DBeaver workspace version");
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public void dispose() {
        synchronized (projects) {
            // Dispose all DS registries
            for (LocalProjectImpl project : this.projects.values()) {
                project.dispose();
            }
            this.projects.clear();
        }
        DBVModel.checkGlobalCacheIsEmpty();

        if (!projectListeners.isEmpty()) {
            log.warn("Some project listeners are still register: " + projectListeners);
            projectListeners.clear();
        }
    }

    @NotNull
    @Override
    public IWorkspace getEclipseWorkspace() {
        return eclipseWorkspace;
    }

    @NotNull
    @Override
    public List<DBPProject> getProjects() {
        return new ArrayList<>(projects.values());
    }

    @Override
    public DBPProject getActiveProject() {
        return activeProject;
    }

    @Override
    public void setActiveProject(DBPProject project) {
        DBPProject oldActiveProject = this.activeProject;
        this.activeProject = project;

        if (!CommonUtils.equalObjects(oldActiveProject, project)) {
            platform.getPreferenceStore().setValue(
                PROP_PROJECT_ACTIVE, project == null ? "" : project.getName());

            fireActiveProjectChange(oldActiveProject, this.activeProject);
        }
    }

    @Override
    public DBPProject getProject(@NotNull IProject project) {
        return projects.get(project);
    }

    @Override
    public DBPProject getProject(@NotNull String projectName) {
        IProject eProject = eclipseWorkspace.getRoot().getProject(projectName);
        if (!eProject.exists()) {
            return null;
        }
        return getProject(eProject);
    }

    @NotNull
    @Override
    public SMSessionContext getAuthContext() {
        return workspaceAuthContext;
    }

    @Override
    public void refreshWorkspaceContents(DBRProgressMonitor monitor) throws DBException {
        try {
            IWorkspaceRoot root = eclipseWorkspace.getRoot();

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
            log.error("Error refreshing workspce contents", e);
        }
    }

    @Override
    public void addProjectListener(DBPProjectListener listener) {
        synchronized (projectListeners) {
            projectListeners.add(listener);
        }
    }

    @Override
    public void removeProjectListener(DBPProjectListener listener) {
        synchronized (projectListeners) {
            projectListeners.remove(listener);
        }
    }

    @NotNull
    @Override
    public DBPPlatform getPlatform() {
        return platform;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @NotNull
    @Override
    public Path getAbsolutePath() {
        return eclipseWorkspace.getRoot().getLocation().toFile().toPath();
    }

    @NotNull
    @Override
    public Path getMetadataFolder() {
        return getAbsolutePath().resolve(METADATA_FOLDER);
    }

    public void save(DBRProgressMonitor monitor) throws DBException {
        try {
            eclipseWorkspace.save(true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException("Error saving Eclipse workspace", e);
        }
    }

    @Override
    public DBPDataSourceRegistry getDefaultDataSourceRegistry() {
        return activeProject == null ? null : activeProject.getDataSourceRegistry();
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
            Path propsFile = GeneralUtils.getMetadataFolder().resolve(EXT_FILES_PROPS_STORE);
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

    protected void fireActiveProjectChange(DBPProject oldActiveProject, DBPProject activeProject) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleActiveProjectChange(oldActiveProject, activeProject);
        }
    }

    protected void fireProjectAdd(LocalProjectImpl project) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleProjectAdd(project);
        }
    }

    protected void fireProjectRemove(LocalProjectImpl project) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleProjectRemove(project);
        }
    }

    @NotNull
    private DBPProjectListener[] getListenersCopy() {
        DBPProjectListener[] listeners;
        synchronized (projectListeners) {
            listeners = projectListeners.toArray(new DBPProjectListener[0]);
        }
        return listeners;
    }

    public static String readWorkspaceId() {
        // Check workspace ID
        Properties workspaceInfo = BaseWorkspaceImpl.readWorkspaceInfo(GeneralUtils.getMetadataFolder());
        String workspaceId = workspaceInfo.getProperty(WORKSPACE_ID);
        if (CommonUtils.isEmpty(workspaceId)) {
            // Generate new UUID
            workspaceId = "D" + Long.toString(
                Math.abs(SecurityUtils.generateRandomLong()),
                36).toUpperCase();
            workspaceInfo.setProperty(WORKSPACE_ID, workspaceId);
            BaseWorkspaceImpl.writeWorkspaceInfo(GeneralUtils.getMetadataFolder(), workspaceInfo);
        }
        return workspaceId;
    }

    private class WorkspaceFilesMetadataJob extends AbstractJob {
        public WorkspaceFilesMetadataJob() {
            super("External files metadata saver");
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            synchronized (externalFileProperties) {
                Path propsFile = GeneralUtils.getMetadataFolder().resolve(EXT_FILES_PROPS_STORE);
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
