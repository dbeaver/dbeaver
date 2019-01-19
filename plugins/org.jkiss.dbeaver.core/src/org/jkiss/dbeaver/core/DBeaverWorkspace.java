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
package org.jkiss.dbeaver.core;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

/**
 * DBeaver workspace.
 *
 * Basically just a wrapper around Eclipse workspace.
 * Additionally holds information about remote workspace.
 * Identified by unique ID (random UUID).
 */
public class DBeaverWorkspace implements DBPWorkspace {

    private static final String WORKSPACE_ID = "workspace-id";

    private DBPPlatform platform;
    private IWorkspace eclipseWorkspace;
    private String workspaceId;

    DBeaverWorkspace(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        this.platform = platform;
        this.eclipseWorkspace = eclipseWorkspace;

        // Check workspace ID
        Properties workspaceInfo = DBeaverCore.readWorkspaceInfo(GeneralUtils.getMetadataFolder());
        workspaceId = workspaceInfo.getProperty(WORKSPACE_ID);
        if (CommonUtils.isEmpty(workspaceId)) {
            // Generate new UUID
            workspaceId = "D" + Long.toString(
                Math.abs(SecurityUtils.generateRandomLong()),
                36).toUpperCase();
            workspaceInfo.setProperty(WORKSPACE_ID, workspaceId);
            DBeaverCore.writeWorkspaceInfo(GeneralUtils.getMetadataFolder(), workspaceInfo);
        }
    }

    @Override
    public IWorkspace getEclipseWorkspace() {
        return eclipseWorkspace;
    }

    @Override
    public DBPPlatform getPlatform() {
        return platform;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public File getAbsolutePath() {
        return eclipseWorkspace.getRoot().getLocation().toFile();
    }

    public void save(DBRProgressMonitor monitor) throws DBException {
        try {
            eclipseWorkspace.save(true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException("Error saving Eclipse workspace", e);
        }
    }

}