package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.DBeaverUtils;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMModel;

import java.util.Iterator;

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

    public void run()
    {
        ISelection selection = null;
        if (targetPart != null && targetPart.getSite().getSelectionProvider() != null) {
            selection = targetPart.getSite().getSelectionProvider().getSelection();
        }
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Iterator iter = structSelection.iterator(); iter.hasNext(); ){
                Object object = iter.next();
                refreshObject(object);
            }
        }
    }

    private void refreshObject(Object object)
    {
        if (this.targetPart instanceof IMetaModelView) {
            IMetaModelView view = (IMetaModelView)this.targetPart;
            DBMModel model = view.getMetaModel();
            DBMNode node = model.findNode(object);
            if (node != null) {
                try {
                    node = node.refreshNode();
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
