package org.jkiss.dbeaver.launch.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

public abstract class LaunchShortcut implements ILaunchShortcut2 {

    @Override
    public void launch(ISelection selection, String mode)
    {
        if (selection instanceof IStructuredSelection) {
            searchAndLaunch(((IStructuredSelection) selection).toArray(), mode, getSelectionEmptyMessage());
        }
    }

    @Override
    public void launch(IEditorPart editor, String mode)
    {
        ISelection selection = editor.getEditorSite().getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection) {
            searchAndLaunch(((IStructuredSelection) selection).toArray(), mode, getEditorEmptyMessage());
        }
    }

    protected abstract String getSelectionEmptyMessage();

    protected abstract String getEditorEmptyMessage();

    protected abstract void searchAndLaunch(Object[] scope, String mode, String emptyMessage);

    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection)
    {
        // let the framework resolve configurations based on resource mapping
        return null;
    }

    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart)
    {
        // let the framework resolve configurations based on resource mapping
        return null;
    }

    @Override
    public IResource getLaunchableResource(ISelection selection)
    {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;
            if (ss.size() == 1) {
                Object element = ss.getFirstElement();
                if (element instanceof IAdaptable) {
                    return getLaunchableResource((IAdaptable)element);
                }
            }
        }
        return null;
    }

    @Override
    public IResource getLaunchableResource(IEditorPart editorpart)
    {
        return getLaunchableResource(editorpart.getEditorInput());
    }

    protected IResource getLaunchableResource(IAdaptable adaptable) {
        return Adapters.adapt(adaptable, IResource.class);
    }

}
