package org.jkiss.dbeaver.runtime.internal.ide.core;

import java.net.URI;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.runtime.ide.core.IdeCore;

public class CreateLinkedFolderRunnable implements ICoreRunnable {
	
	private final IFolder folder;
	private final URI location;
	private final int flags;

	public CreateLinkedFolderRunnable(IFolder folder, URI location)
	{
		this.folder = folder;
		this.location = location;
		this.flags = IResource.NONE;
	}



	@Override
	public void run(IProgressMonitor monitor) throws CoreException
	{
		if (folder == null || location == null) {
			String message = composeErrorMessage(folder, location);
			IStatus error = IdeCore.createError(message);
			throw new CoreException(error);
		}
		folder.createLink(location, flags, monitor);
	}



	public static String composeErrorMessage(IFolder folder, URI location)
	{
		String message = NLS.bind(IdeCoreMessages.CreateLinkedFolderRunnable_e_unable_to_link, folder, location);
		return message;
	}
	
}
