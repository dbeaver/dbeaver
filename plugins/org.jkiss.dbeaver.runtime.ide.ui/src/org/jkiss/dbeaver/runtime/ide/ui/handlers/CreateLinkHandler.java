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
package org.jkiss.dbeaver.runtime.ide.ui.handlers;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jkiss.dbeaver.runtime.ide.ui.IdeUi;
import org.jkiss.dbeaver.runtime.internal.ide.ui.IdeMessages;

public abstract class CreateLinkHandler extends AbstractHandler {
    
    protected static final Path[] NO_TARGETS = new Path[0];

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IStructuredSelection structured = HandlerUtil.getCurrentStructuredSelection(event);
        if (structured.isEmpty()) {
            return null;
        }
        Object first = structured.getFirstElement();
        IResource resource = Adapters.adapt(first, IResource.class);
        IContainer container = extractContainer(resource);
        if (container == null) {
            String message = NLS.bind(IdeMessages.CreateLinkHandler_e_create_link_validation, resource);
            IStatus error = IdeUi.createError(message);
            StatusAdapter statusAdapter = new StatusAdapter(error);
            statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY, IdeMessages.CreateLinkHandler_e_create_link_title);
            StatusManager.getManager().handle(statusAdapter, StatusManager.SHOW);
            return null;
        }

        Path[] locations = selectTargets(event);
        if (locations == null || locations.length == 0) {
            return null;
        }
        WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {

            @Override
            protected void execute(IProgressMonitor monitor)
                    throws CoreException, InvocationTargetException, InterruptedException
            {
                IStatus linked = createLink(container, monitor, locations);
                int severity = linked.getSeverity();
                switch (severity) {
                case IStatus.CANCEL:
                    throw new OperationCanceledException(linked.getMessage());
                case IStatus.ERROR:
                    throw new CoreException(linked);
                default:
                    break;
                }
            }
        };
        IRunnableContext context = getRunnableContext(event);
        try {
            context.run(true, true, operation);
        } catch (InvocationTargetException e) {
            IStatus error = IdeUi.createError(IdeMessages.CreateLinkHandler_e_create_link_message, e.getTargetException());
            StatusAdapter statusAdapter = new StatusAdapter(error);
            statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY, IdeMessages.CreateLinkHandler_e_create_link_title);
            StatusManager.getManager().handle(statusAdapter, StatusManager.LOG | StatusManager.SHOW);
        } catch (InterruptedException e) {
            // skip
        }
        return null;
    }

    protected IContainer extractContainer(IResource resource)
    {
        if (resource instanceof IContainer) {
            IContainer container = (IContainer) resource;
            return container;
        }
        return null;
    }

    protected abstract Path[] selectTargets(ExecutionEvent event);

    protected IRunnableContext getRunnableContext(ExecutionEvent event)
    {
        final IWorkbenchWindow activeWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        if (activeWindow != null) {
            return activeWindow;
        }
        return PlatformUI.getWorkbench().getProgressService();
    }

    protected abstract IStatus createLink(IContainer container, IProgressMonitor monitor, Path...targets);

}