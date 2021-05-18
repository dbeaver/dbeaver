/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.project;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceHandlerDescriptor;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * PrefPageProjectResourceSettings
 */
public class PrefPageProjectResourceSettings extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.projectSettings"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageProjectResourceSettings.class);

    private IProject project;
    private Table resourceTable;
    private TableEditor handlerTableEditor;

    public PrefPageProjectResourceSettings() {
        setDescription("DBeaver project resources/folders settings");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(final Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            UIUtils.createControlLabel(composite, UINavigatorMessages.pref_page_projects_settings_label_resource_location);

            resourceTable = new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 400;
            gd.heightHint = 300;
            resourceTable.setLayoutData(gd);
            resourceTable.setHeaderVisible(true);
            resourceTable.setLinesVisible(true);
            UIUtils.createTableColumn(resourceTable, SWT.LEFT, UINavigatorMessages.pref_page_projects_settings_label_resource);
            UIUtils.createTableColumn(resourceTable, SWT.LEFT, UINavigatorMessages.pref_page_projects_settings_label_folder);
            resourceTable.setHeaderVisible(true);

            handlerTableEditor = new TableEditor(resourceTable);
            handlerTableEditor.verticalAlignment = SWT.TOP;
            handlerTableEditor.horizontalAlignment = SWT.RIGHT;
            handlerTableEditor.grabHorizontal = true;
            handlerTableEditor.grabVertical = true;
            resourceTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseUp(MouseEvent e) {
                    disposeOldEditor();

                    final TableItem item = resourceTable.getItem(new Point(0, e.y));
                    if (item == null) {
                        return;
                    }
                    int columnIndex = UIUtils.getColumnAtPos(item, e.x, e.y);
                    if (columnIndex <= 0) {
                        return;
                    }
                    if (columnIndex == 1) {
                        final String resourcePath = item.getText(1);
                        if (project != null) {
                            final IFolder folder = project.getFolder(resourcePath);
                            ContainerSelectionDialog dialog = new ContainerSelectionDialog(resourceTable.getShell(), folder, true, UINavigatorMessages.pref_page_projects_settings_label_select + item.getText(0) + UINavigatorMessages.pref_page_projects_settings_label_root_folder);
                            dialog.showClosedProjects(false);
                            dialog.setValidator(selection -> {
                                if (selection instanceof IPath) {
                                    IPath path = (IPath) selection;
                                    if (CommonUtils.isEmptyTrimmed(convertToString(path))) {
                                        return UINavigatorMessages.pref_page_projects_settings_label_not_use_project_root;
                                    }
                                    final File file = path.toFile();
                                    if (file.isHidden() || file.getName().startsWith(".")) {
                                        return UINavigatorMessages.pref_page_projects_settings_label_not_use_hidden_folders;
                                    }
                                    final String[] segments = ((IPath) selection).segments();
                                    if (!project.getName().equals(segments[0])) {
                                        return UINavigatorMessages.pref_page_projects_settings_label_not_store_resources_in_another_project;
                                    }
                                }
                                return null;
                            });
                            if (dialog.open() == IDialogConstants.OK_ID) {
                                final Object[] result = dialog.getResult();
                                if (result.length == 1 && result[0] instanceof IPath) {
                                    item.setText(1, convertToString((IPath) result[0]));
                                }
                            }
                        } else {
                            final Text editor = new Text(resourceTable, SWT.NONE);
                            editor.setText(resourcePath);
                            editor.selectAll();
                            handlerTableEditor.setEditor(editor, item, 1);
                            editor.setFocus();
                            editor.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    item.setText(1, editor.getText());
                                }
                            });
                        }
                    }
                }
            });

            UIUtils.createInfoLabel(composite, UINavigatorMessages.pref_page_projects_settings_label_restart_require_refresh_global_settings);
        }

        performDefaults();

        return composite;
    }

    @NotNull
    private static String convertToString(@NotNull IPath path) {
        return path.removeFirstSegments(1).removeTrailingSeparator().toString();
    }

    private void disposeOldEditor() {
        Control oldEditor = handlerTableEditor.getEditor();
        if (oldEditor != null) oldEditor.dispose();
    }

    @Override
    protected void performDefaults() {
        resourceTable.removeAll();
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        for (DBPResourceHandlerDescriptor descriptor : workspace.getResourceHandlerDescriptors()) {
            if (!descriptor.isManagable()) {
                continue;
            }
            TableItem item = new TableItem(resourceTable, SWT.NONE);
            item.setData(descriptor);
            final DBPImage icon = descriptor.getIcon();
            if (icon != null) {
                item.setImage(DBeaverIcons.getImage(icon));
            }
            item.setText(0, descriptor.getName());

            DBPProject projectMeta = getProjectMeta();
            String defaultRoot = projectMeta == null ? null : descriptor.getDefaultRoot(projectMeta);
            if (defaultRoot != null) {
                item.setText(1, defaultRoot);
            }
        }
        UIUtils.packColumns(resourceTable, true);

        super.performDefaults();
    }

    private DBPProject getProjectMeta() {
        return DBWorkbench.getPlatform().getWorkspace().getProject(this.project);
    }

    @Override
    public boolean performOk() {
        java.util.List<IResource> refreshedResources = new ArrayList<>();

        // Save roots
        DBPProject projectMeta = getProjectMeta();
        if (projectMeta != null) {
            for (TableItem item : resourceTable.getItems()) {
                DBPResourceHandlerDescriptor descriptor = (DBPResourceHandlerDescriptor) item.getData();
                String rootPath = item.getText(1);
                if (!CommonUtils.equalObjects(descriptor.getDefaultRoot(projectMeta), rootPath)) {
                    IResource oldResource = project.findMember(descriptor.getDefaultRoot(projectMeta));
                    if (oldResource != null) {
                        refreshedResources.add(oldResource);
                    }

                    IResource newResource = project.findMember(rootPath);
                    if (newResource != null) {
                        refreshedResources.add(newResource);
                    }
                    descriptor.setDefaultRoot(projectMeta, rootPath);
                }
            }
            if (!refreshedResources.isEmpty()) {
                for (IResource resource : refreshedResources) {
                    DBNUtils.refreshNavigatorResource(resource, this);
                }
            }
        }

        return super.performOk();
    }

    @Override
    public IAdaptable getElement() {
        return project;
    }

    @Override
    public void setElement(IAdaptable element) {
        if (element instanceof IProject) {
            this.project = (IProject) element;
        } else {
            this.project = DBUtils.getAdapter(IProject.class, element);
        }
    }

}
