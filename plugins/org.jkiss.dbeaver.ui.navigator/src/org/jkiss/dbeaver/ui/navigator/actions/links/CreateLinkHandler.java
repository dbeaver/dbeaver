/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.navigator.actions.links;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

public abstract class CreateLinkHandler extends AbstractHandler {

    static final Path[] NO_TARGETS = new Path[0];

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection structured = HandlerUtil.getCurrentSelection(event);
        if (structured.isEmpty() || !(structured instanceof IStructuredSelection)) {
            return null;
        }
        Object first = ((IStructuredSelection)structured).getFirstElement();
        IResource resource = GeneralUtils.adapt(first, IResource.class);
        IContainer container = extractContainer(resource);
        if (container == null) {
            IStatus error = GeneralUtils.makeErrorStatus(NLS.bind(UINavigatorMessages.CreateLinkHandler_e_create_link_validation, resource));
            StatusAdapter statusAdapter = new StatusAdapter(error);
            statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
                UINavigatorMessages.CreateLinkHandler_e_create_link_title);
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
                    throws CoreException {
                if (container instanceof IFolder && !container.exists()) {
                    // Create parent folder
                    ((IFolder) container).create(true, true, monitor);
                }
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
            IStatus error = GeneralUtils.makeErrorStatus(UINavigatorMessages.CreateLinkHandler_e_create_link_message,
                    e.getTargetException());
            StatusAdapter statusAdapter = new StatusAdapter(error);
            statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
                UINavigatorMessages.CreateLinkHandler_e_create_link_title);
            StatusManager.getManager().handle(statusAdapter, StatusManager.LOG | StatusManager.SHOW);
        } catch (InterruptedException e) {
            // skip
        }
        return null;
    }

    private IContainer extractContainer(IResource resource) {
        if (resource instanceof IContainer) {
            return (IContainer) resource;
        }
        return null;
    }

    protected abstract Path[] selectTargets(ExecutionEvent event);

    private IRunnableContext getRunnableContext(ExecutionEvent event) {
        final IWorkbenchWindow activeWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        if (activeWindow != null) {
            return activeWindow;
        }
        return PlatformUI.getWorkbench().getProgressService();
    }

    protected abstract IStatus createLink(IContainer container, IProgressMonitor monitor, Path... targets);

}