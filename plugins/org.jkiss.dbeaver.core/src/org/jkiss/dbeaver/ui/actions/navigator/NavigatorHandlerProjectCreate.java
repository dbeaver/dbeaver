/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

public class NavigatorHandlerProjectCreate extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell activeShell = HandlerUtil.getActiveShell(event);
        IWorkspace workspace = DBeaverCore.getInstance().getWorkspace();
        String projectName = EnterNameDialog.chooseName(activeShell, "Project name");
        if (!CommonUtils.isEmpty(projectName)) {
            try {
                IProject project = workspace.getRoot().getProject(projectName);
                if (project.exists()) {
                    throw new DBException("Project '" + projectName + "' already exists");
                }
                project.create(VoidProgressMonitor.INSTANCE.getNestedMonitor());

                DBeaverCore.getInstance().getProjectRegistry().addProject(project);
            } catch (Exception e) {
                UIUtils.showErrorDialog(activeShell, "Create project", null, e);
            }
        }
        return null;
    }

}