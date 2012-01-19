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
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DiagramCreateWizard extends Wizard implements INewWizard {

    private IFolder folder;
    private EntityDiagram diagram = new EntityDiagram(null, "");
    private DiagramCreateWizardPage pageContent;

    public DiagramCreateWizard() {
	}

    public DiagramCreateWizard(IFolder folder) {
        this.folder = folder;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(ERDMessages.wizard_diagram_create_title);
        setNeedsProgressMonitor(true);
    }

    public void addPages() {
        super.addPages();
        pageContent = new DiagramCreateWizardPage(diagram);
        addPage(pageContent);
    }

	@Override
	public boolean performFinish() {
        try {
            Collection<DBNNode> initialContent = pageContent.getInitialContent();
            List<DBSObject> rootObjects = new ArrayList<DBSObject>();
            for (DBNNode node : initialContent) {
                if (node instanceof DBNDatabaseNode) {
                    rootObjects.add(((DBNDatabaseNode) node).getObject());
                }
            }
            DiagramCreator creator = new DiagramCreator(rootObjects);
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
        Collection<DBSObject> roots;
        IFile diagramFile;

        private DiagramCreator(Collection<DBSObject> roots)
        {
            this.roots = roots;
        }

        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                Collection<DBSEntity> tables = DiagramObjectCollector.collectTables(
                    monitor,
                    roots);
                diagram.fillTables(monitor, tables, null);

                diagramFile = ERDResourceHandler.createDiagram(diagram, diagram.getName(), folder, monitor);
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
