package org.jkiss.dbeaver.runtime.ide.core;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.runtime.internal.ide.core.CreateLinkedFileRunnable;
import org.jkiss.dbeaver.runtime.internal.ide.core.CreateLinkedFolderRunnable;

public class WorkspaceResources {

	public static IStatus linkFile(IFile file, URI location, IProgressMonitor monitor) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try {
			workspace.run(new CreateLinkedFileRunnable(file, location), monitor);
		} catch (CoreException e) {
			return e.getStatus();
		} catch (Throwable e) {
			String message = CreateLinkedFileRunnable.composeErrorMessage(file, location);
			return IdeCore.createError(message, e);
		}
		return Status.OK_STATUS;
	}
	
	public static IStatus linkFolder(IFolder folder, URI location, IProgressMonitor monitor) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		try {
			workspace.run(new CreateLinkedFolderRunnable(folder, location), monitor);
		} catch (CoreException e) {
			return e.getStatus();
		} catch (Throwable e) {
			String message = CreateLinkedFolderRunnable.composeErrorMessage(folder, location);
			return IdeCore.createError(message, e);
		}
		return Status.OK_STATUS;
	}
	
}
