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
package org.jkiss.dbeaver.ui.net.ssh;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.net.ssh.SSHSession;
import org.jkiss.dbeaver.model.net.ssh.SSHSessionController;
import org.jkiss.dbeaver.model.net.ssh.SSHTunnelImpl;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.navigator.itemlist.ObjectListControl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class SSHTunnelView extends ViewPart {
    private ObjectListControl<SSHSession> viewer;
    private AbstractJob updateJob;

    @Override
    public void createPartControl(Composite parent) {
        viewer = new ObjectListControl<>(parent, SWT.SHEET, new ListContentProvider()) {
            @NotNull
            @Override
            protected String getListConfigId(List<Class<?>> classList) {
                return getClass().getName();
            }

            @Override
            protected LoadingJob<Collection<SSHSession>> createLoadService(boolean forUpdate) {
                return null;
            }
        };
        viewer.appendListData(List.of());
        viewer.getControl().addPaintListener(e -> {
            if (viewer.getListData().isEmpty()) {
                UIUtils.drawMessageOverControl(viewer.getControl(), e, "No active SSH tunnels", 0);
            }
        });

        updateJob = new AbstractJob("Refresh SSH tunnels") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                refreshTunnels();
                schedule(1000);
                return Status.OK_STATUS;
            }
        };
        updateJob.setUser(false);
        updateJob.setSystem(true);
        updateJob.schedule();
    }

    @Override
    public void setFocus() {
        viewer.setFocus();
    }

    @Override
    public void dispose() {
        if (updateJob != null) {
            updateJob.cancel();
            updateJob = null;
        }

        super.dispose();
    }

    private void refreshTunnels() {
        final DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();

        if (project == null) {
            UIUtils.asyncExec(() -> viewer.clearListData());
            return;
        }

        final List<SSHSession> sessions = project.getDataSourceRegistry().getDataSources().stream()
            .map(DBPDataSourceContainer::getActiveNetworkHandlers).flatMap(Stream::of)
            .filter(handler -> handler instanceof SSHTunnelImpl)
            .map(handler -> ((SSHTunnelImpl) handler).getController())
            .filter(Objects::nonNull)
            .distinct()
            .map(SSHSessionController::getSessions).flatMap(Stream::of)
            .toList();

        UIUtils.asyncExec(() -> {
            if (viewer.getControl().isDisposed()) {
                return;
            }

            viewer.getControl().setRedraw(false);
            viewer.clearListData();
            viewer.appendListData(sessions);
            viewer.getControl().setRedraw(true);
        });
    }
}
