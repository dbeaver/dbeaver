/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.core.application;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.progress.ProgressManagerUtil;
import org.eclipse.ui.part.EditorInputTransfer;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.CreateConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor implements DBPProjectListener {
    private static final Log log = Log.getLog(ApplicationWorkbenchWindowAdvisor.class);

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
    {
        super(configurer);

        DBeaverCore.getInstance().getProjectRegistry().addProjectListener(this);
    }

    @Override
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer)
    {
        return new ApplicationActionBarAdvisor(configurer);
    }

    @Override
    public void preWindowOpen()
    {
        // Set timeout for short jobs (like SQL queries)
        // Jobs longer than this will show progress dialog
        ProgressManagerUtil.SHORT_OPERATION_TIME = 100;

        // Configure window
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(800, 600));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
        configurer.configureEditorAreaDropListener(new EditorAreaDropAdapter());
        configurer.addEditorAreaTransfer(EditorInputTransfer.getInstance());

        //PreferenceManager preferenceManager = PlatformUI.getWorkbench().getPreferenceManager();
        //preferenceManager.remove("org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Perspectives");
        //preferenceManager.remove("org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Workspace");

        // Show heap usage
        //PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, true);
    }

    @Override
    public void postWindowCreate()
    {
        final IWorkbenchWindow window = getWindowConfigurer().getWindow();
        UIUtils.updateMainWindowTitle(window);
/*
        try {
            ApplicationCSSManager.updateApplicationCSS(window.getShell().getDisplay());
        } catch (Throwable e) {
            log.warn(e);
        }
*/
    }

    @Override
    public void postWindowOpen() {
        super.postWindowOpen();

        if (DBeaverCore.isStandalone() && DataSourceDescriptor.getActiveDataSources().isEmpty()) {
            // Open New Connection wizard
            Display.getCurrent().asyncExec(new Runnable() {
                @Override
                public void run() {
                    IWorkbenchWindow window = getWindowConfigurer().getWindow();
                    CreateConnectionDialog dialog = new CreateConnectionDialog(window
                        , new NewConnectionWizard(DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry()));
                    dialog.open();
                }
            });
        }
    }

    @Override
    public boolean preWindowShellClose()
    {
        IWorkbenchWindow window = getWindowConfigurer().getWindow();

        try {
            if (!ConfirmationDialog.confirmAction(window.getShell(), DBeaverPreferences.CONFIRM_EXIT)) {
                return false;
            }
            // Close al content editors
            // They are locks resources which are shared between other editors
            // So we need to close em first
            IWorkbenchPage workbenchPage = window.getActivePage();
            IEditorReference[] editors = workbenchPage.getEditorReferences();
            for (IEditorReference editor : editors) {
                IEditorPart editorPart = editor.getEditor(false);
                if (editorPart != null && editorPart.getEditorInput() instanceof ContentEditorInput) {
                    workbenchPage.closeEditor(editorPart, false);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // Remove project listener
        ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
        if (projectRegistry != null) {
            projectRegistry.removeProjectListener(this);
        }

        // Do its job
        return super.preWindowShellClose();
    }

    @Override
    public void handleActiveProjectChange(IProject oldValue, IProject newValue)
    {
        UIUtils.updateMainWindowTitle(getWindowConfigurer().getWindow());
    }

    public class EditorAreaDropAdapter extends DropTargetAdapter
    {
    }

}

