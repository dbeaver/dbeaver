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
package org.jkiss.dbeaver.runtime.internal.ide.core;

import java.nio.file.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

public class CreateLinkedFoldersRunnable extends CreateLinkedResourcesRunnable {

    public CreateLinkedFoldersRunnable(IContainer container, Path... path)
    {
        super(container, IResource.NONE, path);
    }

    public String composeErrorMessage(IResource resource, Path... paths)
    {
        String message = NLS.bind(IdeCoreMessages.CreateLinkedFolderRunnable_e_unable_to_link, resource, paths);
        return message;
    }

    @Override
    public String composeCancelMessage(IResource resource, Path path)
    {
        String message = NLS.bind(IdeCoreMessages.CreateLinkedFolderRunnable_e_cancelled_link, resource, path);
        return message;
    }

    @Override
    protected void createLink(IContainer container, Path path, int flags, IProgressMonitor monitor)
            throws CoreException
    {
        String memberName = path.getFileName().toString();
        org.eclipse.core.runtime.Path memberPath = new org.eclipse.core.runtime.Path(memberName);
        final IFolder linked = container.getFolder(memberPath);
        linked.createLink(path.toUri(), IResource.NONE, monitor);
    }

}
