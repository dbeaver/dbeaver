/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;


public class LinkEditorAction implements IWorkbenchWindowActionDelegate
{
    private DatabaseNavigatorView navigatorView;
    private IWorkbenchWindow window;

    public LinkEditorAction(DatabaseNavigatorView navigatorView) {
        this.navigatorView = navigatorView;
    }

    public void run(IAction action)
    {
        if (window != null) {
            IWorkbenchPage workbenchPage = window.getActivePage();
            if (workbenchPage != null) {
                IEditorPart activeEditor = workbenchPage.getActiveEditor();
                if (activeEditor instanceof IDatabaseEditor) {
                    IDatabaseEditorInput editorIinput = ((IDatabaseEditor) activeEditor).getEditorInput();
                    if (editorIinput != null) {
                        DBNNode dbnNode = editorIinput.getTreeNode();
                        if (dbnNode != null) {
                            navigatorView.showNode(dbnNode);
                        }
                    }
                }
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
    }

    public void dispose() {

    }

    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

}