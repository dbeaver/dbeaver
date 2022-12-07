/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.model.DiagramLoader;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.erd.ui.part.DiagramPart;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

/**
 * Standalone ERD editor
 */
public class ERDEditorStandalone extends ERDEditorPart implements IResourceChangeListener {

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
    public boolean isReadOnly() {
        return !this.isProjectResourceEditable();
    }
    
    @Override
    public boolean isModelEditEnabled() {
        return super.isModelEditEnabled() && this.isProjectResourceEditable();
    }
    
    private boolean isProjectResourceEditable() {
        DBPProject project = this.getDiagramProject();
        return project == null || project.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT);
    }

    @Override
    public void refreshDiagram(boolean force, boolean refreshMetadata) {
        if (isDirty()) {
            if (ConfirmationDialog.confirmAction(
                null,
                NavigatorPreferences.CONFIRM_ENTITY_REVERT,
                ConfirmationDialog.QUESTION,
                getTitle()) != IDialogConstants.YES_ID)
            {
                return;
            }

        }
        super.refreshDiagram(force, refreshMetadata);
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
            String diagramState = DiagramLoader.serializeDiagram(RuntimeUtils.makeMonitor(monitor), getDiagramPart(), getDiagram(), false, false);

            final IFile file = EditorUtils.getFileFromInput(getEditorInput());
            if (file == null) {
                throw new DBException("Can't determine diagram file");
            }
            file.setContents(
                new ByteArrayInputStream(diagramState.getBytes(StandardCharsets.UTF_8)),
                true,
                true,
                monitor);
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
            new AbstractLoadService<>("Load diagram '" + getEditorInput().getName() + "'") {
                @Override
                public EntityDiagram evaluate(DBRProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        return loadContentFromFile(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
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

    @NotNull
    @Override
    public DBPProject getDiagramProject() {
        final IFile resource = getEditorFile();
        if (resource != null) {
            return DBPPlatformDesktop.getInstance().getWorkspace().getProject(resource.getProject());
        }
        return DBWorkbench.getPlatform().getWorkspace().getActiveProject();
    }

    private EntityDiagram loadContentFromFile(DBRProgressMonitor progressMonitor)
        throws DBException
    {
        final IFile localFile = EditorUtils.getFileFromInput(getEditorInput());
        if (localFile == null) {
            throw new DBException("Can not get file from editor input " + getEditorInput());
        }

        final DiagramPart diagramPart = getDiagramPart();
        EntityDiagram entityDiagram = new EntityDiagram(null, localFile.getName(), getContentProvider(), getDecorator());
        entityDiagram.clear();
        entityDiagram.setLayoutManualAllowed(true);
        entityDiagram.setLayoutManualDesired(true);
        diagramPart.setModel(entityDiagram);

        try (final InputStreamReader isr = new InputStreamReader(localFile.getContents(), GeneralUtils.UTF8_CHARSET)) {
            DiagramLoader.load(progressMonitor, getDiagramProject(), diagramPart, isr);
        } catch (Exception e) {
            throw new DBException("Error loading ER diagram from '" + localFile.getName() + "'", e);
        }

        return entityDiagram;
    }

    private IFile getEditorFile()
    {
        return EditorUtils.getFileFromInput(getEditorInput());
    }

    /*@Override
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
    }*/

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        IResourceDelta delta= event.getDelta();
        if (delta == null) {
            return;
        }
        final IFile file = getEditorFile();
        if (file == null) {
            return;
        }
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
