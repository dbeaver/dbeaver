/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.tree;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.Iterator;

/**
 * RefreshTreeAction
 */
public class CopyTreeAction extends Action implements IObjectActionDelegate
{
    static final Log log = LogFactory.getLog(CopyTreeAction.class);

    private IWorkbenchPart targetPart;

    public CopyTreeAction()
    {
        setText("Copy");
        setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY); //$NON-NLS-1$
        setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
    }

    public CopyTreeAction(IWorkbenchPart view)
    {
        this();
        this.targetPart = view;
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart)
    {
        this.targetPart = targetPart;
    }

    @Override
    public void run()
    {
        ISelection selection = null;
        if (targetPart != null && targetPart.getSite().getSelectionProvider() != null) {
            selection = targetPart.getSite().getSelectionProvider().getSelection();
        }
        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;

            workbenchWindow.getShell().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    StringBuilder buf = new StringBuilder();
                    for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ){
                        if (buf.length() > 0) {
                            buf.append('\n');
                        }
                        buf.append(ViewUtils.convertObjectToString(iter.next()));
                    }

                    Clipboard clipboard = new Clipboard(workbenchWindow.getShell().getDisplay());
                    TextTransfer textTransfer = TextTransfer.getInstance();
                    clipboard.setContents(
                        new Object[]{buf.toString()},
                        new Transfer[]{textTransfer});
                }
            });
        }
    }

    public void run(IAction action)
    {
        this.run();
    }

    public void selectionChanged(IAction action, ISelection selection)
    {

    }

}