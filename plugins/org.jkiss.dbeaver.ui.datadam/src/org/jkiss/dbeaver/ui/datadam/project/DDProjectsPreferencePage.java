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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.SyncResult;
import org.jkiss.dbeaver.model.datadam.sync.project.ProjectStatus;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DDDesktopTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.DDImportKeyDialog;
import org.jkiss.dbeaver.ui.datadam.DDUIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIIcons;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectConflictDialog.Choice;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ProjectJobBatchResult;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DDProjectsPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.synchronization.projects";
    private static final String PLUGIN_ID = "org.jkiss.dbeaver.ui.datadam";
    private static final String PREF_SHOW_REMOTE_PROJECTS = "datadam.projects.showRemoteProjects";
    private static final int MIN_VISIBLE_ROWS = 6;
    private static final int MAX_VISIBLE_ROWS = 12;

    private final DDProjectUIManager uiManager = DDProjectUIManager.getInstance();
    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Composite content;
    private Composite accountRow;
    private Text accountText;
    private Button importKeysButton;
    private Button forgetKeysButton;
    private Label projectsDescription;
    private Control projectsHelp;
    private AbstractJob accountViewJob;
    @Nullable
    private DDKeyBundle displayedBundle;
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
        content = UIUtils.createPlaceholder(parent, 1);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        projectsDescription = DDUIUtils.create(content, DDTrackingUIMessages.projects_page_description,
            DDTrackingUIMessages.projects_page_about_link, DDTrackingUIMessages.projects_page_about_url);
        Composite keysGroup = UIUtils.createTitledComposite(
            content, DDTrackingUIMessages.sync_preference_page_encryption_keys_group,
            2, GridData.FILL_HORIZONTAL, SWT.DEFAULT
        );
        GridData keysGroupLayout = (GridData) keysGroup.getParent().getLayoutData();
        keysGroupLayout.verticalIndent = 5;
        accountRow = UIUtils.createComposite(keysGroup, 2);
        GridData accountRowData = new GridData(GridData.FILL_HORIZONTAL);
        accountRowData.horizontalSpan = 2;
        accountRow.setLayoutData(accountRowData);
        GridData accountTextData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        accountTextData.widthHint = UIUtils.getFontHeight(accountRow.getDisplay().getSystemFont()) * 22;
        accountText = UIUtils.createLabelText(
            accountRow, DDTrackingUIMessages.sync_preference_page_account_label, "", SWT.READ_ONLY, accountTextData);
        Composite keyButtons = UIUtils.createComposite(keysGroup, 2);
        GridData keyButtonsData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        keyButtonsData.horizontalSpan = 2;
        keyButtons.setLayoutData(keyButtonsData);
        importKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.sync_preference_page_import_keys_button, null,
            SelectionListener.widgetSelectedAdapter(e -> importKeys())
        );
        GridData importKeysLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        importKeysLayout.horizontalIndent = 3;
        importKeysButton.setLayoutData(importKeysLayout);
        forgetKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.sync_preference_page_forget_keys_button, null,
            SelectionListener.widgetSelectedAdapter(e -> forgetKeys())
        );
        GridData forgetKeysLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        forgetKeysLayout.horizontalIndent = 3;
        forgetKeysButton.setLayoutData(forgetKeysLayout);
        projectsHelp = UIUtils.createWarningLabel(
            keysGroup, DDTrackingUIMessages.projects_page_sign_in_help, GridData.HORIZONTAL_ALIGN_BEGINNING, 2);
        ((GridData) projectsHelp.getLayoutData()).verticalIndent = 5;

        Composite projectsSection = UIUtils.createPlaceholder(content, 1);
        GridData projectsLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
        projectsLayout.verticalIndent = 3;
        projectsSection.setLayoutData(projectsLayout);
        showRemote = new Button(projectsSection, SWT.CHECK);
        showRemote.setText(DDTrackingUIMessages.projects_show_remote);
        showRemote.setSelection(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_SHOW_REMOTE_PROJECTS));
        showRemote.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            viewer.refresh();
            updateTableHeight();
            updateSelectionActions();
        }));

        Composite tableRow = new Composite(projectsSection, SWT.NONE);
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
            public boolean select(@NotNull Viewer viewer, @Nullable Object parentElement, @NotNull Object element) {
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
        syncButton = iconButton(
            actionButtons, DDTrackingUIIcons.PROJECT_SYNC,
            DDTrackingUIMessages.projects_sync, this::syncSelected
        );
        unbindButton = iconButton(
            actionButtons, DDTrackingUIIcons.PROJECT_DISCONNECT, DDTrackingUIMessages.projects_disconnect,
            this::unbindSelected
        );

        Composite endButtons = new Composite(projectsSection, SWT.NONE);
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

        refreshKeys();
        return content;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && viewer != null && !viewer.getControl().isDisposed()) {
            refreshKeys();
        }
    }

    @Override
    public void dispose() {
        if (accountViewJob != null) {
            accountViewJob.cancel();
        }
        super.dispose();
    }

    private void addNameColumn() {
        TableViewerColumn column = createColumn(DDTrackingUIMessages.projects_name, 220);
        column.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(@NotNull ViewerCell cell) {
                DDProjectView project = (DDProjectView) cell.getElement();
                StyledString name = new StyledString(project.name());
                if (project.remoteProject() != null && project.localProject() != null
                    && !project.localProject().getName().equals(project.remoteProject().name())) {
                    name.append(" (" + project.remoteProject().name() + ")", StyledString.QUALIFIER_STYLER);
                }
                cell.setText(name.getString());
                cell.setStyleRanges(name.getStyleRanges());
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
            @NotNull
            public String getText(@NotNull Object element) {
                return text.apply((DDProjectView) element);
            }

            @Override
            @Nullable
            public Image getImage(@NotNull Object element) {
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
        SelectionSyncAction syncAction = selectionSyncAction(selection);
        boolean download = syncAction == SelectionSyncAction.DOWNLOAD;
        boolean available = !operationRunning && displayedBundle != null && uiManager.isDDReady();
        editButton.setEnabled(available && selection.size() == 1 && selection.get(0).remoteProject() != null);
        syncButton.setEnabled(available && syncAction != SelectionSyncAction.NONE);
        syncButton.setImage(DBeaverIcons.getImage(download ? UIIcon.DOWNLOAD : DDTrackingUIIcons.PROJECT_SYNC));
        syncButton.setToolTipText(download ? DDTrackingUIMessages.projects_download : DDTrackingUIMessages.projects_sync);
        boolean selected = !selection.isEmpty();
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

    private void refreshKeys() {
        DDKeyBundle bundle = accessService.getKeyBundle();
        boolean present = bundle != null;
        accountRow.setVisible(present);
        ((GridData) accountRow.getLayoutData()).exclude = !present;
        boolean signedIn = accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN;
        importKeysButton.setEnabled(!present && signedIn);
        importKeysButton.setVisible(!present);
        ((GridData) importKeysButton.getLayoutData()).exclude = present;
        forgetKeysButton.setEnabled(present);
        forgetKeysButton.setVisible(present);
        ((GridData) forgetKeysButton.getLayoutData()).exclude = !present;
        boolean ready = signedIn && present;
        projectsDescription.setText(ready
            ? DDTrackingUIMessages.projects_page_sync_help : DDTrackingUIMessages.projects_page_description);
        projectsHelp.setVisible(!signedIn);
        ((GridData) projectsHelp.getLayoutData()).exclude = signedIn;
        showRemote.setEnabled(present);
        viewer.getTable().setEnabled(present);
        content.layout(true, true);

        if (!Objects.equals(displayedBundle, bundle)) {
            displayedBundle = bundle;
            accountText.setText(present ? bundle.accountId().toString() : DDTrackingUIMessages.sync_preference_page_unknown);
            if (accountViewJob != null) {
                accountViewJob.cancel();
            }
            if (present) {
                loadProjects();
                loadAccountName();
            } else {
                viewer.setInput(List.of());
                updateTableHeight();
            }
        }
        updateSelectionActions();
    }

    private void loadAccountName() {
        accountViewJob = new AbstractJob("Read DataDam account information") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                var user = accessService.getLoggedUserView();
                UIUtils.asyncExec(() -> {
                    if (!accountText.isDisposed() && accountViewJob == this && !monitor.isCanceled()) {
                        accountText.setText(user.accountName() != null ? user.accountName()
                            : user.accountId() == null ? DDTrackingUIMessages.sync_preference_page_unknown
                                : user.accountId().toString());
                    }
                });
                return Status.OK_STATUS;
            }
        };
        accountViewJob.setSystem(true);
        accountViewJob.schedule();
    }

    private void importKeys() {
        DDImportKeyDialog dialog = new DDImportKeyDialog(getShell());
        if (dialog.open() == Window.OK) {
            runKeyOperation(
                DDTrackingUIMessages.sync_preference_page_import_keys_failed,
                () -> accessService.importKeys(dialog.getPhrase())
            );
        }
    }

    private void forgetKeys() {
        if (UIUtils.confirmAction(
            getShell(), DDTrackingUIMessages.sync_preference_page_forget_keys_confirm_title,
            DDTrackingUIMessages.sync_preference_page_forget_keys_confirm_message
        )) {
            runKeyOperation(DDTrackingUIMessages.sync_preference_page_cannot_forget_keys, accessService::forgetKeys);
        }
    }

    private void runKeyOperation(@NotNull String errorMessage, @NotNull KeyOperation operation) {
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    operation.run();
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            showError(errorMessage, e.getTargetException());
        } finally {
            refreshKeys();
        }
    }

    @FunctionalInterface
    private interface KeyOperation {
        void run() throws DBException;
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
            case NOT_ON_DEVICE -> DDTrackingUIIcons.STATUS_NOT_ON_DEVICE;
            case MISSING_ON_SERVER -> DBIcon.STATUS_WARNING;
            case UP_TO_DATE -> UIIcon.OK_MARK;
            case LOCAL_CHANGES -> DDTrackingUIIcons.STATUS_LOCAL_CHANGES;
            case SERVER_CHANGES -> DDTrackingUIIcons.STATUS_SERVER_CHANGES;
            case CONFLICT -> DDTrackingUIIcons.STATUS_CONFLICT;
        };
    }

    private void connectProject() {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(
            getShell(),
            new LabelProvider() {
                @Override
                @NotNull
                public String getText(@NotNull Object element) {
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
        SelectionSyncAction syncAction = selectionSyncAction(selection);
        if (syncAction == SelectionSyncAction.DOWNLOAD) {
            importProjects(selection.stream().map(DDProjectView::remoteProject).toList());
        } else if (syncAction == SelectionSyncAction.SYNCHRONIZE) {
            syncProjects(selection.stream().map(DDProjectView::localProject).toList());
        }
    }

    private void syncProjects(@NotNull List<DBPProject> projects) {
        ProjectJobBatchResult<?> result = runOperation(() -> uiManager.synchronizeProjects(projects));
        if (result != null) {
            if (projects.size() == 1) {
                openSingleConflictDialog(result);
            } else {
                showConflictWarning(result);
            }
        }
    }

    private void importProjects(@NotNull List<DDSharedProject> projects) {
        runOperation(() -> uiManager.importProjects(projects));
    }

    private void resolveProjectConflict(@NotNull DBPProject project) {
        try {
            Choice choice = uiManager.openConflictDialog(getShell(), project);
            if (choice != null) {
                ProjectJobBatchResult<?> result = runOperation(() -> uiManager.applyConflictChoice(project, choice));
                if (result != null) {
                    showConflictWarning(result);
                }
            }
        } catch (DBException e) {
            showError(DDTrackingUIMessages.project_sync_revisions_failed, e);
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
        syncProjects(projects);
    }

    private void importAll() {
        List<DDSharedProject> projects = getProjectViews(this::canImport).stream()
            .map(DDProjectView::remoteProject).toList();
        importProjects(projects);
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

    @NotNull
    private SelectionSyncAction selectionSyncAction(@NotNull List<DDProjectView> selection) {
        if (!selection.isEmpty()) {
            if (selection.stream().allMatch(this::canImport)) {
                return SelectionSyncAction.DOWNLOAD;
            }
            if (selection.stream().allMatch(this::canSynchronize)) {
                return SelectionSyncAction.SYNCHRONIZE;
            }
        }
        return SelectionSyncAction.NONE;
    }

    private enum SelectionSyncAction {
        NONE,
        DOWNLOAD,
        SYNCHRONIZE
    }

    @Nullable
    private ProjectJobBatchResult<?> runOperation(@NotNull Supplier<ProjectJobBatchResult<?>> operation) {
        if (operationRunning) {
            return null;
        }
        operationRunning = true;
        updateSelectionActions();
        ProjectJobBatchResult<?> result;
        try {
            result = operation.get();
        } finally {
            operationRunning = false;
            if (!viewer.getControl().isDisposed()) {
                loadProjects();
            }
        }
        if (!result.failures().isEmpty() && !viewer.getControl().isDisposed()) {
            IStatus[] errors = result.failures().stream().map(failure -> {
                String message = failure.projectName() + ": " + failure.error().getMessage();
                return Status.error(message, new DBException(message, failure.error().getException()));
            }).toArray(IStatus[]::new);
            String message = NLS.bind(DDTrackingUIMessages.projects_errors,
                String.join("\n", Arrays.stream(errors).map(IStatus::getMessage).toList()));
            new StandardErrorDialog(getShell(), DDTrackingUIMessages.project_sync_title, message,
                new MultiStatus(PLUGIN_ID, 0, errors, message, null), IStatus.ERROR).open();
        }
        return result;
    }

    private void openSingleConflictDialog(@NotNull ProjectJobBatchResult<?> result) {
        if (viewer.getControl().isDisposed() || result.results().isEmpty()) {
            return;
        }
        DBPProject project = conflictedProject(result.results().get(0));
        if (project != null) {
            resolveProjectConflict(project);
        }
    }

    private void showConflictWarning(@NotNull ProjectJobBatchResult<?> result) {
        if (viewer.getControl().isDisposed()) {
            return;
        }
        List<DBPProject> conflictedProjects = getConflictedProjects(result);
        if (!conflictedProjects.isEmpty()) {
            MessageDialog.openWarning(getShell(), DDTrackingUIMessages.project_sync_title,
                NLS.bind(DDTrackingUIMessages.projects_conflicts,
                    String.join("\n", conflictedProjects.stream().map(DBPProject::getName).toList())));
        }
    }

    @NotNull
    private List<DBPProject> getConflictedProjects(@NotNull ProjectJobBatchResult<?> result) {
        List<DBPProject> conflictedProjects = new ArrayList<>();
        for (Object value : result.results()) {
            DBPProject project = conflictedProject(value);
            if (project != null) {
                conflictedProjects.add(project);
            }
        }
        return conflictedProjects;
    }

    @Nullable
    private DBPProject conflictedProject(@NotNull Object value) {
        return value instanceof SyncResult sync && sync.status() == ProjectStatus.CONFLICT ? sync.project() : null;
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
