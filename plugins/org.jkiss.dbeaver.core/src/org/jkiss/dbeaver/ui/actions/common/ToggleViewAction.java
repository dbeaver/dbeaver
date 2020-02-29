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
package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.*;
import org.eclipse.ui.views.IViewDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;


public class ToggleViewAction extends Action implements IPartListener
{

    private String viewId;
    private boolean listenerRegistered = false;
    private IViewDescriptor viewDescriptor;

    public ToggleViewAction(String viewId)
    {
        this.viewId = viewId;
        viewDescriptor = PlatformUI.getWorkbench().getViewRegistry().find(viewId);
    }

    @Override
    public String getText()
    {
        if (viewDescriptor != null) {
            return viewDescriptor.getLabel();
        }
        return super.getText();
    }

    @Override
    public String getToolTipText()
    {
        if (viewDescriptor != null) {
            return viewDescriptor.getDescription();
        }
        return super.getToolTipText();
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        if (viewDescriptor != null) {
            return viewDescriptor.getImageDescriptor();
        }
        return super.getImageDescriptor();
    }

    @Override
    public int getStyle()
    {
        return AS_CHECK_BOX;
    }

    @Override
    public boolean isChecked()
    {
        if (!listenerRegistered) {
            IWorkbenchPage activePage = getActivePage();
            if (activePage == null) {
                return false;
            }
            activePage.addPartListener(this);
            listenerRegistered = true;
            IViewReference viewReference = activePage.findViewReference(viewId);
            setChecked(viewReference != null && viewReference.getView(false) != null);
        }

        return super.isChecked();
    }

    @Override
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
            DBWorkbench.getPlatformUI().showError(viewId, "Can't open view " + viewId, ex);
        }
    }

    private static IWorkbenchPage getActivePage()
    {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        return workbenchWindow.getActivePage();
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part)
    {
    }

    @Override
    public void partOpened(IWorkbenchPart part)
    {
        if (part.getSite().getId().equals(viewId)) {
            setChecked(true);
        }
    }

    @Override
    public void partClosed(IWorkbenchPart part)
    {
        if (part.getSite().getId().equals(viewId)) {
            setChecked(false);
        }
    }

    @Override
    public void partActivated(IWorkbenchPart part)
    {
    }

    @Override
    public void partDeactivated(IWorkbenchPart part)
    {
    }

}