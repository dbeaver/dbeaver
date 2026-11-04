/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.datadam.project.handler;

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectListing;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class DDProjectImportHandler extends AbstractHandler {
    private final DDProjectUIManager uiManager = DDProjectUIManager.getInstance();

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        Shell shell = HandlerUtil.getActiveShell(event);
        List<DDSharedProject> projects = loadProjects();
        if (projects == null) {
            return null;
        }
        ImportProjectsDialog dialog = new ImportProjectsDialog(shell, projects);
        if (dialog.open() == Window.OK) {
            var result = uiManager.importProjects(dialog.getSelectedProjects());
            if (!result.failures().isEmpty()) {
                String errors = String.join(
                    "\n", result.failures().stream()
                        .map(failure -> failure.projectName() + ": " + failure.error().getMessage()).toList()
                );
                MessageDialog.openError(
                    shell, DDTrackingUIMessages.project_sync_title,
                    NLS.bind(DDTrackingUIMessages.projects_errors, errors)
                );
            }
        }
        return null;
    }

    @Nullable
    private List<DDSharedProject> loadProjects() {
        DBRRunnableWithResult<List<DDSharedProject>> loader = new DBRRunnableWithResult<>() {
            @Override
            public void run(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                monitor.beginTask(DDTrackingUIMessages.project_sync_import_list_job, IProgressMonitor.UNKNOWN);
                try {
                    List<ProjectListing> listings = uiManager.loadProjectListings(monitor);
                    if (listings != null && !monitor.isCanceled()) {
                        List<DDSharedProject> available = new ArrayList<>();
                        for (ProjectListing listing : listings) {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            if (listing.localProject() == null && listing.remoteProject() != null) {
                                available.add(listing.remoteProject());
                            }
                        }
                        result = available;
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            UIUtils.runInProgressDialog(loader);
            return loader.getResult();
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                DDTrackingUIMessages.project_sync_title,
                DDTrackingUIMessages.project_sync_import_list_failed, e.getTargetException()
            );
            return null;
        }
    }

    private static class ImportProjectsDialog extends BaseDialog {
        private final List<DDSharedProject> projects;
        private final Set<DDSharedProject> checkedProjects = new LinkedHashSet<>();
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private CheckboxTableViewer viewer;
        private Button selectAllButton;
        private Text filterText;

        private ImportProjectsDialog(@NotNull Shell shell, @NotNull List<DDSharedProject> projects) {
            super(shell, DDTrackingUIMessages.projects_import_dialog_title, null);
            this.projects = projects;
        }

        @NotNull
        @Override
        protected Composite createDialogArea(@NotNull Composite parent) {
            Composite area = super.createDialogArea(parent);
            Composite content = new Composite(area, SWT.NONE);
            content.setLayout(new GridLayout(1, false));
            content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

            UIUtils.createLabel(content, DDTrackingUIMessages.projects_import_prompt);
            filterText = new Text(content, SWT.BORDER | SWT.SEARCH);
            filterText.setMessage(DDTrackingUIMessages.projects_import_filter);
            filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            selectAllButton = new Button(content, SWT.CHECK);
            selectAllButton.setText(DDTrackingUIMessages.projects_import_select_all);
            selectAllButton.setEnabled(!projects.isEmpty());
            selectAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> checkAllProjects()));

            viewer = CheckboxTableViewer.newCheckList(content, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
            Table table = viewer.getTable();
            table.setHeaderVisible(true);
            table.setLinesVisible(true);
            GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
            tableData.widthHint = 780;
            tableData.heightHint = 240;
            table.setLayoutData(tableData);
            viewer.setContentProvider(ArrayContentProvider.getInstance());
            viewer.addFilter(new ViewerFilter() {
                @Override
                public boolean select(@NotNull Viewer viewer, @Nullable Object parentElement, @NotNull Object element) {
                    DDSharedProject row = (DDSharedProject) element;
                    String filter = filterText.getText().trim().toLowerCase(Locale.ROOT);
                    return row.name().toLowerCase(Locale.ROOT).contains(filter)
                        || row.description() != null
                        && row.description().toLowerCase(Locale.ROOT).contains(filter);
                }
            });

            addColumn("", 32, row -> "");
            addColumn(DDTrackingUIMessages.projects_import_project, 160, DDSharedProject::name);
            addColumn(DDTrackingUIMessages.projects_description, 352, DDSharedProject::description);
            addColumn(
                DDTrackingUIMessages.projects_import_last_sync, 232,
                row -> dateFormatter.format(row.updateTime())
            );

            viewer.addCheckStateListener(e -> {
                DDSharedProject project = (DDSharedProject) e.getElement();
                if (e.getChecked()) {
                    checkedProjects.add(project);
                } else {
                    checkedProjects.remove(project);
                }
                updateSelection();
            });
            filterText.addModifyListener(e -> {
                viewer.refresh();
                Set<DDSharedProject> visibleProjects = new HashSet<>();
                for (TableItem item : table.getItems()) {
                    visibleProjects.add((DDSharedProject) item.getData());
                }
                checkedProjects.retainAll(visibleProjects);
                for (TableItem item : table.getItems()) {
                    DDSharedProject project = (DDSharedProject) item.getData();
                    viewer.setChecked(project, checkedProjects.contains(project));
                }
                updateSelection();
            });
            viewer.setInput(projects);
            updateSelection();
            return area;
        }

        private void addColumn(@NotNull String title, int width, @NotNull Function<DDSharedProject, String> text) {
            TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
            column.getColumn().setText(title);
            column.getColumn().setWidth(width);
            column.setLabelProvider(new ColumnLabelProvider() {
                @Override
                @NotNull
                public String getText(@NotNull Object element) {
                    String value = text.apply((DDSharedProject) element);
                    return value == null ? "" : value;
                }
            });
        }

        private void checkAllProjects() {
            boolean checked = selectAllButton.getSelection();
            checkedProjects.clear();
            for (TableItem item : viewer.getTable().getItems()) {
                DDSharedProject project = (DDSharedProject) item.getData();
                viewer.setChecked(project, checked);
                if (checked) {
                    checkedProjects.add(project);
                }
            }
            updateSelection();
        }

        private void updateSelection() {
            TableItem[] visibleItems = viewer.getTable().getItems();
            selectAllButton.setEnabled(visibleItems.length > 0);
            selectAllButton.setSelection(visibleItems.length > 0 && Arrays.stream(visibleItems)
                .allMatch(item -> checkedProjects.contains(item.getData())));
            Button importButton = getButton(IDialogConstants.OK_ID);
            if (importButton != null) {
                importButton.setEnabled(!checkedProjects.isEmpty());
            }
        }

        @Override
        protected void createButtonsForButtonBar(@NotNull Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, DDTrackingUIMessages.projects_import_action, true)
                .setEnabled(false);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }

        @NotNull
        private List<DDSharedProject> getSelectedProjects() {
            return checkedProjects.stream().toList();
        }
    }
}
