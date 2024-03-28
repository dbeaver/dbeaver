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
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.dashboard.DashboardIcons;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfiguration;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConfigurationList;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewer;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.SinglePageDatabaseEditor;

/**
 * Standalone dashboard editor
 */
public class DashboardEditorStandalone extends SinglePageDatabaseEditor<IEditorInput> implements DashboardViewer, IResourceChangeListener {

    private static final Log log = Log.getLog(DashboardEditorStandalone.class);

    private DashboardListViewer dashboardListViewer;
    private DashboardConfiguration dashboardConfig;
    private DashboardConfigurationList configurationList;

    public DashboardEditorStandalone() {
    }

    @Override
    public void dispose() {
        super.dispose();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    @Override
    public void createEditorControl(Composite parent) {
        dashboardListViewer = new DashboardListViewer(getSite(), this, configurationList, dashboardConfig);
        dashboardListViewer.createControl(parent);
        dashboardListViewer.createDashboardsFromConfiguration();

        getSite().setSelectionProvider(dashboardListViewer);
    }

    @Override
    public void setFocus() {
        Control control = dashboardListViewer.getControl();
        if (control != null) {
            control.setFocus();
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);

        IFile file = EditorUtils.getFileFromInput(input);
        if (file == null) {
            throw new PartInitException("Cannot get file from editor input " + input);
        }

        DBPProject project = DBPPlatformDesktop.getInstance().getWorkspace().getProject(file.getProject());
        if (project == null) {
            throw new PartInitException("Cannot get project from file " + file);
        }
        configurationList = new DashboardConfigurationList(project, file);
        configurationList.checkDefaultDashboardExistence();
        dashboardConfig = configurationList.getDashboards().get(0);

        String fileName = getEditorInput().getName();
        if (fileName.endsWith("." + DashboardConstants.DASHBOARD_EXT)) {
            fileName = fileName.substring(0, fileName.length() - 1 - DashboardConstants.DASHBOARD_EXT.length());
        }
        setTitleImage(DBeaverIcons.getImage(DashboardIcons.DASHBOARD));
        setPartName(fileName);
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

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        return dashboardConfig.getDataSourceContainer();
    }

    @Override
    public DashboardConfigurationList getConfigurationList() {
        return configurationList;
    }

    @Override
    public DashboardConfiguration getConfiguration() {
        return dashboardConfig;
    }

    @Override
    public DashboardListViewer getDashboardListViewer() {
        return dashboardListViewer;
    }

    @Override
    public void updateStatus() {

    }
}
