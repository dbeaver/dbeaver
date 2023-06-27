/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.app.DBPWorkspaceEclipse;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.impl.auth.SessionContextImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.DBInterruptedException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/**
 * BaseWorkspaceImpl.
 */
public abstract class BaseWorkspaceImpl implements DBPWorkspaceEclipse {

    private static final Log log = Log.getLog(BaseWorkspaceImpl.class);

    public static final String DEFAULT_RESOURCES_ROOT = "Resources"; //$NON-NLS-1$

    protected static final String PROP_PROJECT_ACTIVE = "project.active";

    private static final String WORKSPACE_ID = "workspace-id";

    private final DBPPlatform platform;
    private final IWorkspace eclipseWorkspace;
    private final SessionContextImpl workspaceAuthContext;

    protected final Map<IProject, LocalProjectImpl> projects = new LinkedHashMap<>();
    protected DBPProject activeProject;
    private final List<DBPProjectListener> projectListeners = new ArrayList<>();

    protected BaseWorkspaceImpl(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        this.platform = platform;
        this.eclipseWorkspace = eclipseWorkspace;
        this.workspaceAuthContext = new SessionContextImpl(null);
    }

    @NotNull
    protected SMSession acquireWorkspaceSession(@NotNull DBRProgressMonitor monitor) throws DBException {
        return new LocalWorkspaceSession(this);
    }

    public abstract void initializeProjects();

    public void initializeWorkspaceSession() {
        // Acquire workspace session
        try {
            this.getAuthContext().addSession(acquireWorkspaceSession(new VoidProgressMonitor()));
        } catch (DBException e) {
            if (!(e instanceof DBInterruptedException)) {
                log.debug(e);
                DBWorkbench.getPlatformUI().showMessageBox(
                    "Authentication error",
                    "Error authenticating application user: " +
                        "\n" + e.getMessage(),
                    true);
            }
            dispose();
            System.exit(101);
        }
    }

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
        if (!projectListeners.isEmpty()) {
            log.warn("Some project listeners are still register: " + projectListeners);
            projectListeners.clear();
        }

        DBVModel.checkGlobalCacheIsEmpty();
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

    public DBPProject getProjectById(@NotNull String projectId) {
        for (DBPProject project : getProjects()) {
            if (projectId.equals(project.getId())) {
                return project;
            }
        }
        return null;
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
    public SMSessionContext getAuthContext() {
        return workspaceAuthContext;
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
        return workspaceId + "-" + getLocalHostId();
    }

    private static String getLocalHostId() {
        // Here we get local machine identifier. It is hashed and thus depersonalized
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            if (ni == null || ni.getHardwareAddress() == null) {
                Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
                while (niEnum.hasMoreElements()) {
                    ni = niEnum.nextElement();
                    if (ni.getHardwareAddress() != null) {
                        break;
                    }
                }
            }
            if (ni == null) {
                log.debug("Cannot detect local network interface");
                return "NOMACADDR";
            }
            byte[] hardwareAddress = ni.getHardwareAddress();

            // Use MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(hardwareAddress);

            long lValue = 0;
            for (int i = 0; i < messageDigest.length; i++) {
                lValue += (long)messageDigest[i] << (i * 8);
            }

            return Long.toString(Math.abs(lValue), 36).toUpperCase();
        } catch (Exception e) {
            log.debug(e);
            return "XXXXXXXXXX";
        }
    }

    ////////////////////////////////////////////////////////
    // Realm

    public boolean isAdmin() {
        return hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN);
    }

    @Override
    public boolean hasRealmPermission(String permission) {
        return true;
    }

    @Override
    public boolean supportsRealmFeature(String feature) {
        return true;
    }

}
