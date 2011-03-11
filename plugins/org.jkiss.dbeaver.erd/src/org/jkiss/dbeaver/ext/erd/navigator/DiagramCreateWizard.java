/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;

import java.lang.reflect.InvocationTargetException;

public class DiagramCreateWizard extends Wizard implements INewWizard {

    private IFolder folder;
    private EntityDiagram diagram = new EntityDiagram(null, "");

    public DiagramCreateWizard() {
	}

    public DiagramCreateWizard(IFolder folder) {
        this.folder = folder;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Diagram Create Wizard");
        setNeedsProgressMonitor(true);
    }

    public void addPages() {
        super.addPages();
        addPage(new DiagramCreateWizardPage(diagram));
    }

	@Override
	public boolean performFinish() {
        try {
            DiagramCreator creator = new DiagramCreator();
            RuntimeUtils.run(getContainer(), true, true, creator);

            NavigatorHandlerObjectOpen.openResource(creator.diagramFile, DBeaverCore.getActiveWorkbenchWindow());
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            UIUtils.showErrorDialog(
                getShell(),
                "Create error",
                "Cannot create diagram",
                ex.getTargetException());
            return false;
        }
        return true;
	}

    private class DiagramCreator implements DBRRunnableWithProgress {
        IFile diagramFile;
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                diagramFile = ERDResourceHandler.createDiagram(diagram, diagram.getName(), folder, monitor);
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
