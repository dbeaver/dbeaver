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
package org.jkiss.dbeaver.ui.datadam.project;

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectJobBatchResult;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DDProjectsPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.synchronization.projects";
    private static final String PREF_SHOW_REMOTE_PROJECTS = "datadam.projects.showRemoteProjects";
    private static final int MIN_VISIBLE_ROWS = 6;
    private static final int MAX_VISIBLE_ROWS = 12;

    private final DDProjectUIManager uiManager = DDProjectUIManager.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TableViewer viewer;
    private Button showRemote;
    private Composite actionButtons;
    private Button editButton;
    private Button syncButton;
    private Button unbindButton;
    private Button connectButton;
    private Button syncAllButton;
    private Button importAllButton;
    private boolean operationRunning;
    @Nullable
    private ProjectJobBatchResult<?> lastOperationResult;

    public DDProjectsPreferencePage() {
        noDefaultAndApplyButton();
    }

    @Override
    public void init(@NotNull IWorkbench workbench) {
        // No workbench state to initialize.
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createPlaceholder(parent, 1);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        showRemote = new Button(content, SWT.CHECK);
        showRemote.setText(DDTrackingUIMessages.projects_show_remote);
        showRemote.setSelection(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_SHOW_REMOTE_PROJECTS));
        showRemote.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            viewer.refresh();
            updateTableHeight();
            updateSelectionActions();
        }));

        Composite tableRow = new Composite(content, SWT.NONE);
        GridLayout tableRowLayout = new GridLayout(2, false);
        tableRowLayout.marginWidth = 0;
        tableRowLayout.marginHeight = 0;
        tableRow.setLayout(tableRowLayout);
        tableRow.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        viewer = new TableViewer(tableRow, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        GridData tableData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        tableData.heightHint = table.getHeaderHeight() + MIN_VISIBLE_ROWS * table.getItemHeight()
            + 2 * table.getBorderWidth();
        table.setLayoutData(tableData);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return showRemote.getSelection() || ((DDProjectView) element).localProject() != null;
            }
        });
        addNameColumn();
        addColumn(
            DDTrackingUIMessages.projects_description, 180,
            row -> row.description() == null ? "" : row.description(), null
        );
        addColumn(DDTrackingUIMessages.projects_status, 175, this::statusText, this::statusIcon);
        addColumn(
            DDTrackingUIMessages.projects_last_revision, 170,
            row -> row.lastSyncedRevision() == null ? "—" : dateFormatter.format(row.lastSyncedRevision()), null
        );
        viewer.addSelectionChangedListener(e -> updateSelectionActions());

        actionButtons = new Composite(tableRow, SWT.NONE);
        GridLayout actionLayout = new GridLayout(1, false);
        actionLayout.marginWidth = 0;
        actionLayout.marginHeight = 0;
        actionButtons.setLayout(actionLayout);
        actionButtons.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
        editButton = iconButton(actionButtons, UIIcon.EDIT, DDTrackingUIMessages.projects_edit, this::editProject);
        syncButton = iconButton(actionButtons, UIIcon.REFRESH, DDTrackingUIMessages.projects_sync, this::syncSelected);
        unbindButton = iconButton(
            actionButtons, UIIcon.SQL_DISCONNECT, DDTrackingUIMessages.projects_disconnect,
            this::unbindSelected
        );

        Composite endButtons = new Composite(content, SWT.NONE);
        endButtons.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        GridLayout endLayout = new GridLayout(3, false);
        endLayout.marginWidth = 0;
        endButtons.setLayout(endLayout);
        connectButton = new Button(endButtons, SWT.PUSH);
        connectButton.setText(DDTrackingUIMessages.projects_connect);
        connectButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> connectProject()));
        syncAllButton = new Button(endButtons, SWT.PUSH);
        syncAllButton.setText(DDTrackingUIMessages.projects_sync_all);
        syncAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> syncAll()));
        importAllButton = new Button(endButtons, SWT.PUSH);
        importAllButton.setText(DDTrackingUIMessages.projects_import_all);
        importAllButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        importAllButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> importAll()));

        updateSelectionActions();
        loadProjects();
        return content;
    }

    private void addNameColumn() {
        TableViewerColumn column = createColumn(DDTrackingUIMessages.projects_name, 220);
        column.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                DDProjectView project = (DDProjectView) cell.getElement();
                StyledString name = new StyledString(project.name());
                if (project.remoteProject() != null && project.localProject() != null
                    && !project.localProject().getName().equals(project.remoteProject().name())) {
                    name.append(" (" + project.remoteProject().name() + ")", StyledString.QUALIFIER_STYLER);
                }
                cell.setText(name.getString());
                cell.setStyleRanges(name.getStyleRanges());
                cell.setImage(DBeaverIcons.getImage(DBIcon.PROJECT));
                super.update(cell);
            }
        });
    }

    private void addColumn(
        @NotNull String title,
        int width,
        @NotNull Function<DDProjectView, String> text,
        @Nullable Function<DDProjectView, DBPImage> icon
    ) {
        TableViewerColumn column = createColumn(title, width);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return text.apply((DDProjectView) element);
            }

            @Override
            public Image getImage(Object element) {
                DBPImage image = icon == null ? null : icon.apply((DDProjectView) element);
                return image == null ? null : DBeaverIcons.getImage(image);
            }
        });
    }

    @NotNull
    private TableViewerColumn createColumn(@NotNull String title, int width) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        return column;
    }

    @NotNull
    private Button iconButton(
        @NotNull Composite parent,
        @NotNull DBPImage icon,
        @NotNull String tooltip,
        @NotNull Runnable action
    ) {
        Button button = new Button(parent, SWT.PUSH);
        button.setImage(DBeaverIcons.getImage(icon));
        button.setToolTipText(tooltip);
        button.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));
        button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> action.run()));
        return button;
    }

    private void updateSelectionActions() {
        List<DDProjectView> selection = selectedRows();
        boolean selected = !selection.isEmpty();
        boolean download = selected && selection.stream().allMatch(this::canImport);
        boolean synchronize = selected && selection.stream().allMatch(this::canSynchronize);
        boolean available = !operationRunning && uiManager.isDDReady();
        editButton.setEnabled(available && selection.size() == 1 && selection.get(0).remoteProject() != null);
        syncButton.setEnabled(available && (download || synchronize));
        syncButton.setToolTipText(download ? DDTrackingUIMessages.projects_download : DDTrackingUIMessages.projects_sync);
        unbindButton.setEnabled(available && selected && selection.stream().allMatch(this::canUnbind));
        boolean showUnbind = !download;
        unbindButton.setVisible(showUnbind);
        ((GridData) unbindButton.getLayoutData()).exclude = !showUnbind;
        actionButtons.layout(true, true);
        connectButton.setEnabled(available);
        boolean hasLocal = false;
        boolean hasRemote = false;
        for (var item : viewer.getTable().getItems()) {
            DDProjectView view = (DDProjectView) item.getData();
            hasLocal |= canSynchronize(view);
            hasRemote |= canImport(view);
        }
        syncAllButton.setEnabled(available && hasLocal);
        importAllButton.setEnabled(available && hasRemote);
        boolean showImportAll = showRemote.getSelection();
        if (importAllButton.getVisible() != showImportAll) {
            importAllButton.setVisible(showImportAll);
            ((GridData) importAllButton.getLayoutData()).exclude = !showImportAll;
            importAllButton.getParent().getParent().layout(true, true);
        }
    }

    private void loadProjects() {
        if (!uiManager.isDDReady()) {
            viewer.setInput(List.of());
            updateTableHeight();
            updateSelectionActions();
            return;
        }
        DBRRunnableWithResult<List<DDProjectView>> loader = new DBRRunnableWithResult<>() {
            @Override
            public void run(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                monitor.beginTask(DDTrackingUIMessages.project_sync_import_list_job, IProgressMonitor.UNKNOWN);
                try {
                    if (!DDDesktopTokenUIManager.getInstance().ensureTokenReady(monitor)) {
                        return;
                    }
                    List<DDProjectView> views = uiManager.loadProjectViews(monitor);
                    if (!monitor.isCanceled()) {
                        result = views;
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
            if (!viewer.getControl().isDisposed() && loader.getResult() != null) {
                viewer.setInput(loader.getResult());
            }
        } catch (InvocationTargetException e) {
            showError(DDTrackingUIMessages.project_sync_import_list_failed, e.getTargetException());
        } finally {
            if (!viewer.getControl().isDisposed()) {
                updateTableHeight();
                updateSelectionActions();
            }
        }
    }

    private void updateTableHeight() {
        Table table = viewer.getTable();
        int rows = Math.max(MIN_VISIBLE_ROWS, Math.min(table.getItemCount(), MAX_VISIBLE_ROWS));
        GridData tableData = (GridData) table.getLayoutData();
        int height = table.getHeaderHeight() + rows * table.getItemHeight() + 2 * table.getBorderWidth();
        if (tableData.heightHint != height) {
            tableData.heightHint = height;
            table.getParent().getParent().layout(true, true);
        }
    }

    @NotNull
    private List<DDProjectView> selectedRows() {
        List<DDProjectView> selection = new ArrayList<>();
        for (Object element : viewer.getStructuredSelection()) {
            selection.add((DDProjectView) element);
        }
        return selection;
    }

    @NotNull
    private String statusText(@NotNull DDProjectView row) {
        return switch (row.status()) {
            case NOT_SYNC -> DDTrackingUIMessages.projects_not_sync;
            case NOT_ON_DEVICE -> DDTrackingUIMessages.projects_not_on_device;
            case MISSING_ON_SERVER -> DDTrackingUIMessages.projects_missing_on_server;
            case UP_TO_DATE -> DDTrackingUIMessages.projects_up_to_date;
            case LOCAL_CHANGES -> DDTrackingUIMessages.projects_local_changes;
            case SERVER_CHANGES -> DDTrackingUIMessages.projects_server_changes;
            case CONFLICT -> DDTrackingUIMessages.projects_both_changes;
        };
    }

    @NotNull
    private DBPImage statusIcon(@NotNull DDProjectView row) {
        return switch (row.status()) {
            case NOT_SYNC -> DBIcon.PROJECT;
            case NOT_ON_DEVICE -> UIIcon.PROJECTS;
            case MISSING_ON_SERVER -> DBIcon.STATUS_WARNING;
            case UP_TO_DATE -> UIIcon.OK_MARK;
            case LOCAL_CHANGES -> UIIcon.EXPORT;
            case SERVER_CHANGES -> UIIcon.IMPORT;
            case CONFLICT -> DBIcon.STATUS_WARNING;
        };
    }

    private void connectProject() {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(
            getShell(),
            new LabelProvider() {
                @Override
                public String getText(Object element) {
                    return ((DBPProject) element).getName();
                }
            }
        );
        dialog.setTitle(DDTrackingUIMessages.projects_connect);
        dialog.setMessage(DDTrackingUIMessages.projects_choose_local);
        try {
            dialog.setElements(uiManager.getNotDatadamLocalProjects().toArray());
        } catch (DBException e) {
            showError(DDTrackingUIMessages.projects_connect_failed, e);
            return;
        }
        if (dialog.open() != Window.OK) {
            return;
        }
        DBPProject project = (DBPProject) dialog.getFirstResult();
        DDProjectEditDialog<DBPProject> editor = new DDProjectEditDialog<>(
            getShell(), DDTrackingUIMessages.projects_connect, project, project.getName(), project.getDescription());
        if (editor.open() == Window.OK) {
            var details = editor.getDetails();
            runOperation(() -> uiManager.shareProject(details.project(), details.name(), details.description()));
        }
    }

    private void editProject() {
        List<DDProjectView> selection = selectedRows();
        if (selection.size() != 1 || selection.get(0).remoteProject() == null) {
            return;
        }
        DDSharedProject project = selection.get(0).remoteProject();
        DDProjectEditDialog<DDSharedProject> editor = new DDProjectEditDialog<>(
            getShell(), DDTrackingUIMessages.projects_edit, project, project.name(), project.description());
        if (editor.open() == Window.OK) {
            var details = editor.getDetails();
            runOperation(() -> uiManager.updateProject(details.project(), details.name(), details.description()));
        }
    }

    private void syncSelected() {
        List<DDProjectView> selection = selectedRows();
        if (selection.isEmpty()) {
            return;
        }
        if (selection.stream().allMatch(this::canImport)) {
            List<DDSharedProject> projects = selection.stream().map(DDProjectView::remoteProject).toList();
            runOperation(() -> uiManager.importProjects(projects));
        } else if (selection.stream().allMatch(this::canSynchronize)) {
            List<DBPProject> projects = selection.stream().map(DDProjectView::localProject).toList();
            runOperation(() -> uiManager.synchronizeProjects(projects));
        }
    }

    private void unbindSelected() {
        List<DDProjectView> selection = selectedRows();
        if (selection.isEmpty() || !selection.stream().allMatch(this::canUnbind)) {
            return;
        }
        List<DBPProject> projects = selection.stream().map(DDProjectView::localProject).toList();
        String message = projects.size() == 1
            ? NLS.bind(DDTrackingUIMessages.project_sync_unbind_confirm_message, projects.get(0).getName())
            : DDTrackingUIMessages.projects_unbind_selected_confirm_message;
        if (UIUtils.confirmAction(getShell(), DDTrackingUIMessages.project_sync_unbind_confirm_title, message)) {
            runOperation(() -> uiManager.unbindProjects(projects));
        }
    }

    private void syncAll() {
        List<DBPProject> projects = getProjectViews(this::canSynchronize).stream()
            .map(DDProjectView::localProject).toList();
        runOperation(() -> uiManager.synchronizeProjects(projects));
    }

    private void importAll() {
        List<DDSharedProject> projects = getProjectViews(this::canImport).stream()
            .map(DDProjectView::remoteProject).toList();
        runOperation(() -> uiManager.importProjects(projects));
    }

    @NotNull
    private List<DDProjectView> getProjectViews(@NotNull Predicate<DDProjectView> filter) {
        List<DDProjectView> projects = new ArrayList<>();
        for (var item : viewer.getTable().getItems()) {
            DDProjectView view = (DDProjectView) item.getData();
            if (filter.test(view)) {
                projects.add(view);
            }
        }
        return projects;
    }

    private boolean canSynchronize(@NotNull DDProjectView view) {
        return view.localProject() != null && view.status() != ProjectStatus.NOT_SYNC
            && view.status() != ProjectStatus.MISSING_ON_SERVER;
    }

    private boolean canImport(@NotNull DDProjectView view) {
        return view.status() == ProjectStatus.NOT_ON_DEVICE && view.remoteProject() != null;
    }

    private boolean canUnbind(@NotNull DDProjectView view) {
        return view.localProject() != null && view.status() != ProjectStatus.NOT_SYNC;
    }

    private void runOperation(@NotNull Supplier<ProjectJobBatchResult<?>> operation) {
        if (operationRunning) {
            return;
        }
        operationRunning = true;
        lastOperationResult = null;
        updateSelectionActions();
        try {
            lastOperationResult = operation.get();
        } finally {
            operationRunning = false;
            if (!viewer.getControl().isDisposed()) {
                loadProjects();
            }
        }
    }

    private void showError(@NotNull String message, @NotNull Throwable error) {
        UIUtils.asyncExec(() ->
            DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.project_sync_title, message, error));
    }

    @Override
    public boolean performOk() {
        if (!super.performOk()) {
            return false;
        }
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        store.setValue(PREF_SHOW_REMOTE_PROJECTS, showRemote.getSelection());
        PrefUtils.savePreferenceStore(store);
        return true;
    }

}
