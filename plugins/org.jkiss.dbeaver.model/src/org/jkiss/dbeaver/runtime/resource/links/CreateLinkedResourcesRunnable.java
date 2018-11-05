/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.runtime.resource.links;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.nio.file.Path;

public abstract class CreateLinkedResourcesRunnable implements ICoreRunnable {

    private final IContainer container;
    private final Path[] paths;
    private final int flags;

    public CreateLinkedResourcesRunnable(IContainer container, int flags, Path... paths) {
        this.container = container;
        this.flags = flags;
        this.paths = paths;
    }

    public abstract String composeErrorMessage(IResource resource, Path... paths);

    public abstract String composeCancelMessage(IResource resource, Path path);

    @Override
    public void run(IProgressMonitor monitor) throws CoreException {
        if (container == null) {
            throw new CoreException(GeneralUtils.makeErrorStatus(composeErrorMessage(container, paths)));
        }
        SubMonitor subMonitor = SubMonitor.convert(monitor, paths.length);
        for (Path path : paths) {
            if (subMonitor.isCanceled()) {
                throw new CoreException(GeneralUtils.makeErrorStatus(composeCancelMessage(container, path)));
            }
            if (path == null) {
                throw new CoreException(GeneralUtils.makeErrorStatus(composeErrorMessage(container, path)));
            }
            createLink(container, path, flags, monitor);
            subMonitor.worked(1);
        }
    }

    protected abstract void createLink(IContainer container, Path path, int flags, IProgressMonitor monitor)
            throws CoreException;

}
