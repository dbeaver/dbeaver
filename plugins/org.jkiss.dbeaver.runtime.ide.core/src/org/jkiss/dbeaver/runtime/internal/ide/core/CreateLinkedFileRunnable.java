/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
