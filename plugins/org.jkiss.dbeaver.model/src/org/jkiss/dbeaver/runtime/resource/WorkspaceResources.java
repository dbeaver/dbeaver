/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.runtime.resource;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.resource.links.CreateLinkedFilesRunnable;
import org.jkiss.dbeaver.runtime.resource.links.CreateLinkedFoldersRunnable;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.nio.file.Path;

public class WorkspaceResources {

    /**
     * Bulk operation to create several linked files
     * 
     * @param container
     * @param monitor
     * @param paths
     * @return
     */
    public static IStatus createLinkedFiles(IContainer container, IProgressMonitor monitor, Path... paths) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        CreateLinkedFilesRunnable action = new CreateLinkedFilesRunnable(container, paths);
        try {
            workspace.run(action, monitor);
        } catch (CoreException e) {
            return e.getStatus();
        } catch (Throwable e) {
            return GeneralUtils.makeErrorStatus(action.composeErrorMessage(container, paths), e);
        }
        return Status.OK_STATUS;
    }

    /**
     * Bulk operation to create several linked folders
     * 
     * @param container
     * @param monitor
     * @param paths
     * @return
     */
    public static IStatus createLinkedFolders(IContainer container, IProgressMonitor monitor, Path... paths) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        CreateLinkedFoldersRunnable action = new CreateLinkedFoldersRunnable(container, paths);
        try {
            workspace.run(action, monitor);
        } catch (CoreException e) {
            return e.getStatus();
        } catch (Throwable e) {
            return GeneralUtils.makeErrorStatus(action.composeErrorMessage(container, paths), e);
        }
        return Status.OK_STATUS;
    }

    public static IResource resolveWorkspaceResource(DBSObject dbsObject) {
        WorkspaceResourceResolver resolver = GeneralUtils.adapt(dbsObject, WorkspaceResourceResolver.class, true);
        if (resolver != null) {
            return resolver.resolveResource(dbsObject);
        }
        return null;
    }
}
