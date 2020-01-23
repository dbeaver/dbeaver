/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.model.DiagramObjectCollector;
import org.jkiss.dbeaver.ext.erd.model.ERDDecoratorDefault;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DiagramCreateWizard extends Wizard implements INewWizard {

    private IFolder folder;
    private EntityDiagram diagram = new EntityDiagram(new ERDDecoratorDefault(), null, "");
    private DiagramCreateWizardPage pageContent;
	private String errorMessage;
    private IStructuredSelection entitySelection;

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
            DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        	if (activeProject == null) {
				errorMessage = "Can't create diagram without active project";
			} else {
	        	try {
					diagramFolder = ERDResourceHandler.getDiagramsFolder(activeProject, true);
				} catch (CoreException e) {
					errorMessage = e.getMessage();
				}
			}

			// Check for entity selection
            if (selection != null && !selection.isEmpty()) {
        	    if (Platform.getAdapterManager().getAdapter(selection.getFirstElement(), DBSEntity.class) != null) {
        	        entitySelection = selection;
                }
            }
        }
        this.folder = diagramFolder;
    }

    @Override
    public void addPages() {
        super.addPages();
        pageContent = new DiagramCreateWizardPage(diagram, entitySelection);
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
            UIUtils.run(getContainer(), true, true, creator);

            SQLEditorHandlerOpenEditor.openResource(creator.diagramFile);
        }
        catch (InterruptedException ex) {
            return false;
        }
        catch (InvocationTargetException ex) {
            DBWorkbench.getPlatformUI().showError(
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
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
            try {
                Collection<DBSEntity> tables = DiagramObjectCollector.collectTables(
                    monitor,
                    roots);
                diagram.fillEntities(monitor, tables, null);

                diagramFile = ERDResourceHandler.createDiagram(diagram, diagram.getName(), folder, monitor);
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
