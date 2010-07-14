/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.part.EditorInputTransfer;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;

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

        //configurer.set

        PreferenceManager preferenceManager = PlatformUI.getWorkbench().getPreferenceManager();
        preferenceManager.remove("org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Perspectives");
        preferenceManager.remove("org.eclipse.ui.preferencePages.Workbench/org.eclipse.ui.preferencePages.Workspace");

        // Show heap usage
        PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, true);
    }

    /*
    org.eclipse.ui.preferencePages.Editors
    org.eclipse.ui.preferencePages.Views
    org.eclipse.ui.preferencePages.Keys
    org.eclipse.ui.preferencePages.ContentTypes
    */
    public void postWindowCreate()
    {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setMaximized(true);
    }

    public boolean preWindowShellClose()
    {
        // Close al content editors
        // They are locks resources which are shared between other editors
        // So we need to close em first
        IWorkbenchPage workbenchPage = getWindowConfigurer().getWindow().getActivePage();
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

    public class EditorAreaDropAdapter extends DropTargetAdapter
    {
        public void handleDrop(IWorkbenchPage page, DropTargetEvent event)
        {
        }
    }

}

