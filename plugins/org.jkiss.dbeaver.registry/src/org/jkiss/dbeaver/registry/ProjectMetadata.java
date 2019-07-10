/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;

import java.io.File;
import java.util.Map;

public class ProjectMetadata implements DBPProject {
    private static final Log log = Log.getLog(ProjectMetadata.class);

    public static final String METADATA_FOLDER = ".dbeaver";

    private final DBPWorkspace workspace;
    private final IProject project;
    private volatile DataSourceRegistry dataSourceRegistry;
    private Map<String, Object> resourceProperties;
    private final Object metadataSync = new Object();

    public ProjectMetadata(DBPWorkspace workspace, IProject project) {
        this.workspace = workspace;
        this.project = project;
    }

    @NotNull
    @Override
    public DBPWorkspace getWorkspace() {
        return workspace;
    }

    @NotNull
    @Override
    public String getName() {
        return project.getName();
    }

    @NotNull
    @Override
    public File getAbsolutePath() {
        return project.getLocation().toFile();
    }

    @NotNull
    @Override
    public IProject getEclipseProject() {
        return project;
    }

    @Override
    public boolean isOpen() {
        return project.isOpen();
    }

    @Override
    public void ensureOpen() {
        if (!project.isOpen()) {
            try {
                NullProgressMonitor monitor = new NullProgressMonitor();
                project.open(monitor);
                project.refreshLocal(IFile.DEPTH_ONE, monitor);
            } catch (CoreException e) {
                log.error(e);
            }
        }
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        if (dataSourceRegistry == null) {
            dataSourceRegistry = new DataSourceRegistry(workspace.getPlatform(), this);
        }
        return dataSourceRegistry;
    }

    @Override
    public Object getResourceProperty(IResource resource, String propName) {
        loadMetadata();
        Object resProps = resourceProperties.get(resource.getLocation().toString());
        if (resProps instanceof Map) {
            return ((Map) resProps).get(propName);
        }
        return null;
    }

    @Override
    public Map<String, Object> getResourceProperties(IResource resource) {
        loadMetadata();
        return (Map<String, Object>) resourceProperties.get(resource.getLocation().toString());
    }

    @Override
    public void setResourceProperties(IResource resource, Map<String, Object> properties) {
        flushMetadata();
    }

    public void dispose() {
        dataSourceRegistry.dispose();
    }

    private void loadMetadata() {
        synchronized (metadataSync) {
            if (resourceProperties != null) {
                return;
            }

        }
    }

    private void flushMetadata() {
        synchronized (metadataSync) {

        }
    }

}
