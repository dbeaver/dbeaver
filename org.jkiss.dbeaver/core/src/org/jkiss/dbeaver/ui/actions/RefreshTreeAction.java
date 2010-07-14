/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.model.meta.DBMEvent;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

/**
 * RefreshTreeAction
 */
public class RefreshTreeAction extends Action implements IObjectActionDelegate
{
    static final Log log = LogFactory.getLog(RefreshTreeAction.class);

    private IWorkbenchPart targetPart;

    public RefreshTreeAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        //setId(ActionFactory.REFRESH.getId());
        // Associate the action with a pre-defined command, to allow key bindings.
        //setActionDefinitionId(ActionFactory.REFRESH.getId());
        setText("Refresh");
        setActionDefinitionId("org.eclipse.ui.file.refresh"); //$NON-NLS-1$
        setImageDescriptor(DBIcon.REFRESH.getImageDescriptor());
    }

    public RefreshTreeAction(IWorkbenchPart view)
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

            try {
                DBeaverUtils.run(workbenchWindow, true, true, new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        int count = 1;
                        for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ){
                            Object object = iter.next();
                            monitor.beginTask("Refresh selected object (" + (count++) + ")", 5);
                            refreshObject(monitor, object);
                            monitor.done();
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                DBeaverUtils.showErrorDialog(
                    workbenchWindow.getShell(),
                    "Refresh", "Could not refresh tree object", e.getTargetException());
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    private void refreshObject(DBRProgressMonitor monitor, Object object)
    {
        if (this.targetPart instanceof IMetaModelView) {
            IMetaModelView view = (IMetaModelView)this.targetPart;
            final DBMModel model = view.getMetaModel();
            DBMNode node = model.findNode(object);
            if (node != null) {
                try {
                    node = node.refreshNode(monitor);
                }
                catch (DBException ex) {
                    log.error("Could not refresh tree node", ex);
                }
            }
            if (node != null) {
                final DBMNode refNode = node;
                targetPart.getSite().getShell().getDisplay().asyncExec(new Runnable() {
                    public void run()
                    {
                        model.fireNodeRefresh(targetPart, refNode, DBMEvent.NodeChange.REFRESH);
                    }
                });
            }
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
