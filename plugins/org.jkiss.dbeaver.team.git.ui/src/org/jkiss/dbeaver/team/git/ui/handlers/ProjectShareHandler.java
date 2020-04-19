/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.egit.ui.internal.sharing.SharingWizard;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.team.git.ui.internal.GITMessages;
import org.jkiss.dbeaver.team.git.ui.utils.GitUIUtils;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectShareHandler extends AbstractHandler implements IElementUpdater {

    private static final Log log = Log.getLog(ProjectShareHandler.class);

    @Override
    public Object execute(ExecutionEvent event) {
        IProject project = GitUIUtils.extractActiveProject(event);

        if (project == null) {
            DBWorkbench.getPlatformUI().showError(
                "Nothing to share - no active project",
                "Select a project or resource to share");
            return null;
        }

        IWorkbench workbench = HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench();
        SharingWizard wizard = new SharingWizard();
        wizard.init(workbench, project);
        Shell shell = HandlerUtil.getActiveShell(event);
        WizardDialog wizardDialog = new WizardDialog(shell, wizard);
        wizardDialog.setHelpAvailable(false);
        if (wizardDialog.open() == IDialogConstants.OK_ID) {
            // Add content
            addProjectContentsToRepository(event, project);

            DBeaverNotifications.showNotification(
                "git",
                GITMessages.project_share_handler_notifications_title_project_added,
                NLS.bind(GITMessages.project_share_handler_notifications_text_project_added, project.getName()),
                DBPMessageType.INFORMATION,
                () -> ActionUtils.runCommand(GITCommandIds.CMD_COMMIT, workbench));
        }

        return null;
    }

    private void addProjectContentsToRepository(ExecutionEvent event, IProject project) {
        List<IResource> resources = new ArrayList<>();
        try {
            addFolderToIndex(project, resources);
        } catch (CoreException e) {
            log.error(e);
        }

        IStructuredSelection selection = new StructuredSelection(resources);
        ActionUtils.runCommand(GITCommandIds.EGIT_CMD_ADD_TO_INDEX, selection, UIUtils.getActiveWorkbenchWindow());
    }

    private void addFolderToIndex(IContainer container, List<IResource> resources) throws CoreException {
        for (IResource resource : container.members(IContainer.INCLUDE_HIDDEN)) {
            if (container instanceof IProject && resource instanceof IFolder && resource.getName().equals(DBPProject.METADATA_FOLDER)) {
                // Add dbeaver configs
                for (IResource cfgResource : ((IFolder) resource).members(IContainer.INCLUDE_HIDDEN)) {
                    if (cfgResource instanceof IFile) {
                        if (cfgResource.getFileExtension().equals("bak")) {
                            continue;
                        }
                        resources.add(cfgResource);
                    }
                }
                continue;
            } else if (resource.isHidden() || resource.isTeamPrivateMember() || resource.isLinked() || resource.isVirtual() || !resource.exists()) {
                continue;
            }
            if (container instanceof IProject) {
                if (resource instanceof IFolder && resource.getName().equals(".settings")) {
                    continue;
                }
                if (resource instanceof IFile) {
                    if (resource.getName().startsWith(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_PREFIX) && resource.getName().endsWith(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_EXT)) {
                        continue;
                    }
                }
            }
            if (resource instanceof IFolder) {
                addFolderToIndex((IFolder) resource, resources);
            } else {
                resources.add(resource);
            }
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        IProject project = GitUIUtils.extractActiveProject(element.getServiceLocator());
        if (project != null) {
            element.setText(NLS.bind(GITMessages.project_share_handler_menu_element_text_add, project.getName()));
        }
    }
}
