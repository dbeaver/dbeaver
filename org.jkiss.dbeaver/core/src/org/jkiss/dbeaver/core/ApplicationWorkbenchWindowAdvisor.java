/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.part.EditorInputTransfer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.NewConnectionWizard;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor
{
    static final Log log = LogFactory.getLog(ApplicationWorkbenchWindowAdvisor.class);

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
    {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer)
    {
        return new ApplicationActionBarAdvisor(configurer);
    }

    public void preWindowOpen()
    {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(600, 400));
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

    /*
    org.eclipse.ui.preferencePages.Editors
    org.eclipse.ui.preferencePages.Views
    org.eclipse.ui.preferencePages.Keys
    org.eclipse.ui.preferencePages.ContentTypes
    */
    public void postWindowCreate()
    {
        Shell activeShell = UIUtils.getActiveShell();
        if (activeShell != null) {
            activeShell.setMaximized(true);
        }
    }

    public boolean preWindowShellClose()
    {
        IWorkbenchWindow window = getWindowConfigurer().getWindow();

        if (!ConfirmationDialog.confirmAction(window.getShell(), PrefConstants.CONFIRM_EXIT)) {
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
        // Do its job
        return super.preWindowShellClose();
    }

    @Override
    public void postWindowOpen() {
        super.postWindowOpen();

        if (DBeaverCore.getInstance().getDataSourceRegistry().getDataSources().isEmpty()) {
            // Open New Connection wizard
            Display.getCurrent().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchWindow window = getWindowConfigurer().getWindow();
                    ConnectionDialog dialog = new ConnectionDialog(window, new NewConnectionWizard(window));
                    dialog.open();
                }
            });
        }
    }

    public class EditorAreaDropAdapter extends DropTargetAdapter
    {
        public void handleDrop(IWorkbenchPage page, DropTargetEvent event)
        {
        }
    }

}

