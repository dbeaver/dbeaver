package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.RefreshJob;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.DBeaverUtils;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMModel;

import java.util.Iterator;
import java.lang.reflect.InvocationTargetException;

/**
 * RefreshTreeAction
 */
public class RefreshTreeAction extends Action implements IObjectActionDelegate
{
    private IWorkbenchPart targetPart;

    public RefreshTreeAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        //setId(ActionFactory.REFRESH.getId());
        // Associate the action with a pre-defined command, to allow key bindings.
        //setActionDefinitionId(ActionFactory.REFRESH.getId());
        setText("Refresh");
        setActionDefinitionId("org.eclipse.ui.file.refresh"); //$NON-NLS-1$
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/refresh.png"));
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
            RefreshJob job = new RefreshJob(targetPart, structSelection);
            job.schedule();
/*
            try {
                workbenchWindow.run(true, true, new IRunnableWithProgress() {
                   public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                       int count = 1;
                       for (Iterator iter = structSelection.iterator(); iter.hasNext(); ){
                           Object object = iter.next();
                           monitor.beginTask("Refresh selected object", count++);
                           refreshObject(monitor, object);
                           monitor.done();
                       }
                   }
                });
            } catch (InvocationTargetException e) {
                DBeaverUtils.showErrorDialog(
                    workbenchWindow.getShell(),
                    "Connect", "Can't connect to ", e.getTargetException());
            } catch (InterruptedException e) {
                // Do nothing
            }
*/
        }
    }

    private void refreshObject(IProgressMonitor monitor, Object object)
    {
        if (this.targetPart instanceof IMetaModelView) {
            IMetaModelView view = (IMetaModelView)this.targetPart;
            DBMModel model = view.getMetaModel();
            DBMNode node = model.findNode(object);
            if (node != null) {
                try {
                    node = node.refreshNode(monitor);
                }
                catch (DBException ex) {
                    DBeaverUtils.showErrorDialog(
                        this.targetPart.getSite().getShell(),
                        "Refresh error",
                        "Can't refresh tree node",
                        ex);
                }
            }
            if (node != null) {
                model.fireNodeRefresh(this.targetPart, node);
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
