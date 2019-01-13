/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.gef.EditPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.model.DiagramLoader;
import org.jkiss.dbeaver.ext.erd.model.ERDObject;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.ext.erd.part.DiagramPart;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Standalone ERD editor
 */
public class ERDEditorStandalone extends ERDEditorPart implements DBPContextProvider, IResourceChangeListener {

    private static final Log log = Log.getLog(ERDEditorStandalone.class);

    /**
     * No-arg constructor
     */
    public ERDEditorStandalone()
    {
    }

    @Override
    public void dispose()
    {
        super.dispose();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException
    {
        super.init(site, input);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);

        loadDiagram(false);
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DiagramLoader.save(RuntimeUtils.makeMonitor(monitor), getDiagramPart(), getDiagram(), false, buffer);

            final IFile file = getEditorFile();
            file.setContents(new ByteArrayInputStream(buffer.toByteArray()), true, true, monitor);

            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Save diagram", null, e);
        }
    }

/*
    protected void createActions()
    {
        super.createActions();

        //addEditorAction(new SaveAction(this));
    }
*/

    @Override
    protected synchronized void loadDiagram(boolean refreshMetadata)
    {
        if (diagramLoadingJob != null) {
            // Do not start new one while old is running
            return;
        }
        diagramLoadingJob = LoadingJob.createService(
            new AbstractLoadService<EntityDiagram>("Load diagram '" + getEditorInput().getName() + "'") {
                @Override
                public EntityDiagram evaluate(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                    try {
                        return loadContentFromFile(monitor);
                    } catch (DBException e) {
                        log.error(e);
                    }

                    return null;
                }

                @Override
                public Object getFamily() {
                    return ERDEditorStandalone.this;
                }
            },
            progressControl.createLoadVisualizer());
        diagramLoadingJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event)
            {
                diagramLoadingJob = null;
            }
        });
        diagramLoadingJob.schedule();

        setPartName(getEditorInput().getName());
    }

    private EntityDiagram loadContentFromFile(DBRProgressMonitor progressMonitor)
        throws DBException
    {
        final IFile file = getEditorFile();

        final DiagramPart diagramPart = getDiagramPart();
        EntityDiagram entityDiagram = new EntityDiagram(getDecorator(), null, file.getName());
        entityDiagram.clear();
        entityDiagram.setLayoutManualAllowed(true);
        entityDiagram.setLayoutManualDesired(true);
        diagramPart.setModel(entityDiagram);

        try (final InputStream fileContent = file.getContents()) {
            DiagramLoader.load(progressMonitor, file.getProject(), diagramPart, fileContent);
        } catch (Exception e) {
            log.error("Error loading ER diagram from '" + file.getName() + "'", e);
        }

        return entityDiagram;
    }

    private IFile getEditorFile()
    {
        return EditorUtils.getFileFromInput(getEditorInput());
    }

    @Override
    public DBCExecutionContext getExecutionContext()
    {
        for (Object part : getViewer().getSelectedEditParts()) {
            EditPart editPart = (EditPart) part;
            if (editPart.getModel() instanceof ERDObject) {
                final ERDObject model = (ERDObject) editPart.getModel();
                Object object = model.getObject();
                if (object instanceof DBSObject) {
                    DBSObject dbObject = (DBSObject) object;
                    return DBUtils.getDefaultContext(dbObject, true);
                }
            }
        }
        return null;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        IResourceDelta delta= event.getDelta();
        if (delta == null) {
            return;
        }
        final IFile file = getEditorFile();
        delta = delta.findMember(file.getFullPath());
        if (delta == null) {
            return;
        }
        if (delta.getKind() == IResourceDelta.REMOVED) {
            if ((IResourceDelta.MOVED_TO & delta.getFlags()) != 0) {
                // Renamed
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IFile newFile = workspace.getRoot().getFile(delta.getMovedToPath());
                setInput(new FileEditorInput(newFile));
                setPartName(getEditorInput().getName());
            } else {
                // Deleted
                UIUtils.asyncExec(() ->
                    getSite().getWorkbenchWindow().getActivePage().closeEditor(ERDEditorStandalone.this, false));
            }
        }
    }

}
