/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.editor;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfiguration;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

/**
 * Standalone dashboard editor
 */
public class DashboardEditorStandalone extends SinglePageDatabaseEditor<IEditorInput> implements IResourceChangeListener {

    private static final Log log = Log.getLog(DashboardEditorStandalone.class);

    private DashboardListViewer dashboardListViewer;
    private DashboardConfiguration dashboardConfig;

    public DashboardEditorStandalone() {
    }

    @Override
    public void dispose() {
        super.dispose();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    @Override
    public void createEditorControl(Composite parent) {
        dashboardListViewer = new DashboardListViewer(getSite(), this, null, dashboardConfig);
    }

    @Override
    public void setFocus() {
        dashboardListViewer.getControl().setFocus();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        try {
//            String diagramState = DiagramLoader.serializeDiagram(RuntimeUtils.makeMonitor(monitor), getDiagramPart(), getDiagram(), false, false);
//
//            final IFile file = EditorUtils.getFileFromInput(getEditorInput());
//            if (file == null) {
//                throw new DBException("Can't determine diagram file");
//            }
//            file.setContents(
//                new ByteArrayInputStream(diagramState.getBytes(StandardCharsets.UTF_8)),
//                true,
//                true,
//                monitor);
//            getCommandStack().markSaveLocation();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Save dashboard", null, e);
        }
    }

    private IFile getEditorFile() {
        return EditorUtils.getFileFromInput(getEditorInput());
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
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
                    getSite().getWorkbenchWindow().getActivePage().closeEditor(DashboardEditorStandalone.this, false));
            }
        }
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        return RefreshResult.REFRESHED;
    }
}
