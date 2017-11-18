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

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.jkiss.dbeaver.runtime.ide.core.IdeCore;

public abstract class CreateLinkedResourceRunnable<R extends IResource> implements ICoreRunnable {

    private final R resource;
    private final URI[] locations;
    private final int flags;

    public CreateLinkedResourceRunnable(R resource, int flags, URI... locations)
    {
        this.resource = resource;
        this.flags = flags;
        this.locations = locations;
    }

    public abstract String composeErrorMessage(R resource, URI... locations);

    public abstract String composeCancelMessage(R resource, URI location);

    @Override
    public void run(IProgressMonitor monitor) throws CoreException
    {
        if (resource == null) {
            String message = composeErrorMessage(resource, locations);
            IStatus error = IdeCore.createError(message);
            throw new CoreException(error);
        }
        SubMonitor subMonitor = SubMonitor.convert(monitor, locations.length);
        for (URI uri : locations) {
            if (subMonitor.isCanceled()) {
                String message = composeCancelMessage(resource, uri);
                IStatus cancel = IdeCore.createCancel(message);
                throw new CoreException(cancel);
            }
            if (uri == null) {
                String message = composeErrorMessage(resource, uri);
                IStatus error = IdeCore.createError(message);
                throw new CoreException(error);
            }
            createLink(resource, uri, flags, monitor);
            subMonitor.worked(1);
        }
    }

    protected abstract void createLink(R resource, URI location, int flags, IProgressMonitor monitor)
            throws CoreException;

}
