package org.jkiss.dbeaver.runtime.internal.ide.core;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.runtime.ide.core.IdeCore;

public class CreateLinkedFileRunnable implements ICoreRunnable {
	
	private final IFile file;
	private final URI location;
	private final int flags;

	public CreateLinkedFileRunnable(IFile file, URI location)
	{
		this.file = file;
		this.location = location;
		this.flags = IResource.NONE;
	}



	@Override
	public void run(IProgressMonitor monitor) throws CoreException
	{
		if (file == null || location == null) {
			String message = composeErrorMessage(file, location);
			IStatus error = IdeCore.createError(message);
			throw new CoreException(error);
		}
		file.createLink(location, flags, monitor);
	}

	public static String composeErrorMessage(IFile file, URI location)
	{
		String message = NLS.bind(IdeCoreMessages.CreateLinkedFileRunnable_e_unable_to_link, file, location);
		return message;
	}

}
