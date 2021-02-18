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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

/**
 * PrefPageProjectResourceSettings
 */
public class PrefPageProjectUserProfiles extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.proejct.settings.userProfiles"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageProjectUserProfiles.class);

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(final Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
/*

        {
            UIUtils.createControlLabel(composite, UINavigatorMessages.pref_page_projects_settings_label_resource_location);

            resourceTable = new Table(composite, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
            resourceTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            resourceTable.setHeaderVisible(true);
            resourceTable.setLinesVisible(true);
            UIUtils.createTableColumn(resourceTable, SWT.LEFT, UINavigatorMessages.pref_page_projects_settings_label_resource);
            UIUtils.createTableColumn(resourceTable, SWT.LEFT, UINavigatorMessages.pref_page_projects_settings_label_folder);
            resourceTable.setHeaderVisible(true);
            resourceTable.setLayoutData(new GridData(GridData.FILL_BOTH));

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
                                    final File file = ((IPath) selection).toFile();
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
                                    final IPath plainPath = ((IPath) result[0]).removeFirstSegments(1).removeTrailingSeparator();
                                    item.setText(1, plainPath.toString());
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
*/

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults() {
/*
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
*/

        super.performDefaults();
    }

    @Override
    public boolean performOk() {
/*
        java.util.List<IResource> refreshedResources = new ArrayList<>();

        // Save roots
        DBPProject projectMeta = getProjectMeta();
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

*/
        return super.performOk();
    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable element) {
    }

}
