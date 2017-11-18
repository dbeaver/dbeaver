/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.runtime.ide.core;

import java.nio.file.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.runtime.internal.ide.core.CreateLinkedFilesRunnable;
import org.jkiss.dbeaver.runtime.internal.ide.core.CreateLinkedFoldersRunnable;

public class WorkspaceResources {

    /**
     * Bulk operation to create several linked files
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
			String message = action.composeErrorMessage(container, paths);
			return IdeCore.createError(message, e);
		}
		return Status.OK_STATUS;
	}
	
	/**
     * Bulk operation to create several linked folders
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
			String message = action.composeErrorMessage(container, paths);
			return IdeCore.createError(message, e);
		}
		return Status.OK_STATUS;
	}
	
}
