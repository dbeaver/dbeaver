/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.team.git.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.operations.GitScopeUtil;
import org.eclipse.egit.ui.internal.selection.SelectionUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.*;

public abstract class GITAbstractHandler extends AbstractHandler {

    private static final Log log = Log.getLog(GITAbstractHandler.class);

    protected IResource[] getResourcesInScope(ExecutionEvent event)
        throws ExecutionException {
        try {
            IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
            if (activePart instanceof IEditorPart) {
                IFile editorFile = EditorUtils.getFileFromInput(((IEditorPart) activePart).getEditorInput());
                if (editorFile != null) {
                    return new IResource[]{editorFile};
                } else {
                    return new IResource[0];
                }
            } else {
                IResource[] selectedResources = getSelectedResources(event);
                if (selectedResources.length > 0) {
                    IWorkbenchPart part = HandlerUtil.getActivePart(event);
                    return GitScopeUtil.getRelatedChanges(part, selectedResources);
                } else {
                    return new IResource[0];
                }
            }
        } catch (Exception e) {
            // ignore, we will not show the commit dialog in case the user
            // cancels the scope operation
            log.error(e);
            return null;
        }
    }

    protected Repository getRepository(boolean warn, ExecutionEvent event) {
        Shell shell = HandlerUtil.getActiveShell(event);
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof IEditorPart) {
            IFile editorFile = EditorUtils.getFileFromInput(((IEditorPart) activePart).getEditorInput());
            if (editorFile != null) {
                return SelectionUtils.getRepositoryOrWarn(new StructuredSelection(editorFile), shell);
            } else {
                return null;
            }
        } else {
            IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
            if (warn) {
                return SelectionUtils.getRepositoryOrWarn(selection, shell);
            } else {
                return SelectionUtils.getRepository(selection);
            }
        }
    }

    protected IResource[] getSelectedResources(ExecutionEvent event) {
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        return SelectionUtils.getSelectedResources(selection);
    }

    protected Repository[] getRepositories(ExecutionEvent event) {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof IEditorPart) {
            IFile editorFile = EditorUtils.getFileFromInput(((IEditorPart) activePart).getEditorInput());
            if (editorFile != null) {
                Repository repository = SelectionUtils.getRepository(new StructuredSelection(editorFile));
                if (repository != null) {
                    return new Repository[]{repository};
                }
            }
        }
        {
            INavigatorModelView navigatorModelView = GeneralUtils.adapt(activePart, INavigatorModelView.class);
            if (navigatorModelView != null) {
                DBNNode rootNode = navigatorModelView.getRootNode();
                if (rootNode != null) {
                    DBPProject ownerProject = rootNode.getOwnerProject();
                    if (ownerProject instanceof RCPProject rcpProject) {
                        return getRepositories(new StructuredSelection(rcpProject.getEclipseProject()));
                    }
                }
            }
        }
        IStructuredSelection selection = HandlerUtil.getCurrentStructuredSelection(event);
        return getRepositories(selection);
    }

    /////////////////////////////////////////////////////////////
    // copied from EGist source to provide backward compatibility with older versions

    private static Repository[] getRepositories(IStructuredSelection selection) {

        IProject[] selectedProjects = getSelectedProjects(selection);

        if (selectedProjects.length > 0)
            return getRepositoriesFor(selectedProjects);

        if (selection.isEmpty()) {
            return new Repository[0];
        }

        Set<Repository> repos = new LinkedHashSet<>();
        for (Object o : selection.toArray()) {
            Repository repo = Adapters.adapt(o, Repository.class);
            if (repo != null) {
                repos.add(repo);
            } else {
                // no repository found for one of the objects!
                return new Repository[0];
            }
        }
        return repos.toArray(new Repository[0]);
    }

    private static IProject[] getSelectedProjects(
        IStructuredSelection selection) {
        Set<IProject> ret = new LinkedHashSet<>();
        for (IResource resource : getSelectedAdaptables(selection, IResource.class)) {
            RepositoryMapping mapping = RepositoryMapping.getMapping(resource);
            if (mapping != null && (mapping.getContainer() instanceof IProject))
                ret.add((IProject) mapping.getContainer());
            else
                return new IProject[0];
        }
        ret.addAll(extractProjectsFromMappings(selection));

        return ret.toArray(new IProject[0]);
    }

    private static <T> List<T> getSelectedAdaptables(ISelection selection,
                                                     Class<T> c) {
        List<T> result;
        if (selection != null && !selection.isEmpty()) {
            result = new ArrayList<>();
            Iterator elements = ((IStructuredSelection) selection).iterator();
            while (elements.hasNext()) {
                T adapter = Adapters.adapt(elements.next(), c);
                if (adapter != null) {
                    result.add(adapter);
                }
            }
        } else {
            result = Collections.emptyList();
        }
        return result;
    }

    private static Set<IProject> extractProjectsFromMappings(
        IStructuredSelection selection) {
        Set<IProject> ret = new LinkedHashSet<>();
        for (ResourceMapping mapping : getSelectedAdaptables(selection,
            ResourceMapping.class)) {
            IProject[] mappedProjects = mapping.getProjects();
            if (mappedProjects != null && mappedProjects.length != 0) {
                // Some mappings (WorkingSetResourceMapping) return the projects
                // in unpredictable order. Sort them like the navigator to
                // correspond to the order the user usually sees.
                List<IProject> projects = new ArrayList<>(Arrays.asList(mappedProjects));
                projects.sort(CommonUtils.RESOURCE_NAME_COMPARATOR);
                ret.addAll(projects);
            }
        }
        return ret;
    }

    private static Repository[] getRepositoriesFor(final IProject[] projects) {
        Set<Repository> ret = new LinkedHashSet<>();
        for (IProject project : projects) {
            RepositoryMapping repositoryMapping = RepositoryMapping
                .getMapping(project);
            if (repositoryMapping == null) {
                return new Repository[0];
            }
            ret.add(repositoryMapping.getRepository());
        }
        return ret.toArray(new Repository[0]);
    }

}
