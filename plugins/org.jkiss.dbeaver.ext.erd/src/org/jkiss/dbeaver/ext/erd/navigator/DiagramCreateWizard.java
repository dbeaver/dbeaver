/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
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
	private String errorMessage;

    public DiagramCreateWizard() {
	}

	@Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(ERDMessages.wizard_diagram_create_title);
        setNeedsProgressMonitor(true);
        IFolder diagramFolder = null;
        if (selection != null) {
            Object element = selection.getFirstElement();
            if (element != null) {
                diagramFolder = Platform.getAdapterManager().getAdapter(element, IFolder.class);
			}
        }
        if (diagramFolder == null) {
        	IProject activeProject = DBeaverCore.getInstance().getProjectRegistry().getActiveProject();
        	if (activeProject == null) {
				errorMessage = "Can't create diagram without active project";
			} else {
	        	try {
					diagramFolder = ERDResourceHandler.getDiagramsFolder(activeProject, true);
				} catch (CoreException e) {
					errorMessage = e.getMessage();
				}
			}
        }
        this.folder = diagramFolder;
    }

    @Override
    public void addPages() {
        super.addPages();
        pageContent = new DiagramCreateWizardPage(diagram);
        addPage(pageContent);
        if (getContainer() != null) {
            //WizardDialog call
            pageContent.setErrorMessage(errorMessage);
		}
    }
    
    @Override
    public void setContainer(IWizardContainer wizardContainer) {
    	super.setContainer(wizardContainer);
    	if (pageContent != null) {
    		//New Wizard call
            pageContent.setErrorMessage(errorMessage);
		}
    }
    
	@Override
	public boolean performFinish() {
        try {
            Collection<DBNNode> initialContent = pageContent.getInitialContent();
            List<DBSObject> rootObjects = new ArrayList<>();
            for (DBNNode node : initialContent) {
                if (node instanceof DBNDatabaseNode) {
                    rootObjects.add(((DBNDatabaseNode) node).getObject());
                }
            }
            DiagramCreator creator = new DiagramCreator(rootObjects);
            RuntimeUtils.run(getContainer(), true, true, creator);

            NavigatorHandlerObjectOpen.openResource(creator.diagramFile, DBeaverUI.getActiveWorkbenchWindow());
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

        @Override
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
