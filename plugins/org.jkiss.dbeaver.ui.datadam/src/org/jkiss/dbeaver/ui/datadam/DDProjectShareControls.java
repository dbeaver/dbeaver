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
package org.jkiss.dbeaver.ui.datadam;

import com.dbeaver.datadam.share.api.model.DDSharedProject;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.datadam.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyStore;
import org.jkiss.dbeaver.model.datadam.sync.DDProjectShareService;
import org.jkiss.dbeaver.model.datadam.sync.core.DDShareClient;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class DDProjectShareControls {
    private DDProjectShareControls() {
    }

    static void create(@NotNull Composite parent, @NotNull Supplier<String> gatewayUrlSupplier) {
        Composite group = UIUtils.createTitledComposite(parent, DDTrackingUIMessages.share_projects_title,
            2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);
        UIUtils.createPushButton(group, DDTrackingUIMessages.share_project_upload, null,
            SelectionListener.widgetSelectedAdapter(event -> synchronize(group, gatewayUrlSupplier.get(), false)));
        UIUtils.createPushButton(group, DDTrackingUIMessages.share_project_download, null,
            SelectionListener.widgetSelectedAdapter(event -> synchronize(group, gatewayUrlSupplier.get(), true)));
    }

    private static void synchronize(@NotNull Composite parent, @NotNull String url, boolean download) {
        DDKeyBundle bundle = DDKeyStore.load();
        if (bundle == null || url.isBlank()) {
            DBWorkbench.getPlatformUI().showMessageBox(DDTrackingUIMessages.share_projects_title,
                bundle == null ? DDTrackingUIMessages.sync_preference_page_log_in_first
                    : DDTrackingUIMessages.sync_preference_page_url_not_configured, true);
            return;
        }
        Object selected = select(parent, DBWorkbench.getPlatform().getWorkspace().getProjects().toArray(),
            DDTrackingUIMessages.share_select_local_project);
        if (!(selected instanceof DBPProject project)) {
            return;
        }
        DDProjectShareService service = new DDProjectShareService(
            new DDShareClient(url, new DDBundleCredentials(bundle)), bundle.accountId());
        try {
            if (download) {
                List<DDSharedProject> projects = run(service::listProjects);
                Object remote = select(parent, projects.toArray(), DDTrackingUIMessages.share_select_remote_project);
                if (!(remote instanceof DDSharedProject remoteProject)) {
                    return;
                }
                if (!UIUtils.confirmAction(parent.getShell(), DDTrackingUIMessages.share_projects_title,
                    DDTrackingUIMessages.share_replace_local_project)) {
                    return;
                }
                UUID id = remoteProject.id();
                run(monitor -> {
                    service.download(monitor, project, id, true);
                    return true;
                });
            } else {
                run(monitor -> {
                    service.upload(monitor, project);
                    return true;
                });
            }
            DBWorkbench.getPlatformUI().showMessageBox(DDTrackingUIMessages.share_projects_title,
                DDTrackingUIMessages.share_project_complete, false);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(DDTrackingUIMessages.share_projects_title,
                DDTrackingUIMessages.share_project_failed, e);
        }
    }

    @Nullable
    private static Object select(@NotNull Composite parent, @NotNull Object[] elements, @NotNull String message) {
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(parent.getShell(), new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return element instanceof DBPProject project ? project.getName() : ((DDSharedProject) element).name();
            }
        });
        dialog.setTitle(DDTrackingUIMessages.share_projects_title);
        dialog.setMessage(message);
        dialog.setElements(elements);
        return dialog.open() == Window.OK ? dialog.getFirstResult() : null;
    }

    private static <T> T run(@NotNull Operation<T> operation) throws DBException {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    result.set(operation.run(monitor));
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            throw new DBException("Project sharing failed", e.getTargetException());
        }
        return result.get();
    }

    private interface Operation<T> {
        T run(@NotNull DBRProgressMonitor monitor) throws DBException;
    }
}
