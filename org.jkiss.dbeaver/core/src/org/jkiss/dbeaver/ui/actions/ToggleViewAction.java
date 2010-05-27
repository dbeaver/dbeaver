/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.*;


public class ToggleViewAction extends Action implements IPartListener
{
    static Log log = LogFactory.getLog(ToggleViewAction.class);

    private String viewId;
    private boolean listenerRegistered = false;

    public ToggleViewAction(String text, String viewId)
    {
        setText(text);
        this.viewId = viewId;
    }

    public int getStyle()
    {
        return AS_CHECK_BOX;
    }

    public boolean isChecked()
    {
        if (!listenerRegistered) {
            IWorkbenchPage activePage = getActivePage();
            if (activePage == null) {
                return false;
            }
            activePage.addPartListener(this);
            listenerRegistered = true;
            return activePage.findView(viewId) != null;
        }

        return super.isChecked();
    }

    public void run()
    {
        IWorkbenchPage activePage = getActivePage();
        if (activePage == null) {
            return;
        }
        try {
            IViewPart view = activePage.findView(viewId);
            if (view == null) {
                activePage.showView(viewId);
            } else {
                activePage.hideView(view);
            }
        } catch (PartInitException ex) {
            log.error("Can't open view " + viewId, ex);
        }
    }

    private static IWorkbenchPage getActivePage()
    {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        return workbenchWindow.getActivePage();
    }

    public void partBroughtToTop(IWorkbenchPart part)
    {
    }

    public void partClosed(IWorkbenchPart part)
    {
        if (part.getSite().getId().equals(viewId)) {
            setChecked(false);
        }
    }

    public void partActivated(IWorkbenchPart part)
    {
    }

    public void partDeactivated(IWorkbenchPart part)
    {
    }

    public void partOpened(IWorkbenchPart part)
    {
        if (part.getSite().getId().equals(viewId)) {
            setChecked(true);
        }
    }
}