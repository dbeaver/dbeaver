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

import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager.ConflictDetails;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DDProjectConflictDialog extends BaseDialog {
    static final int USE_LOCAL_ID = IDialogConstants.CLIENT_ID + 1;
    static final int USE_SERVER_ID = IDialogConstants.CLIENT_ID + 2;
    static final int STOP_SYNC_ID = IDialogConstants.CLIENT_ID + 3;

    @NotNull
    private final ConflictDetails details;

    public DDProjectConflictDialog(@NotNull Shell shell, @NotNull ConflictDetails details) {
        super(shell, DDTrackingUIMessages.project_sync_conflict_dialog_title, null);
        this.details = details;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite area = super.createDialogArea(parent);

        Composite heading = UIUtils.createComposite(area, 2);
        Label warning = new Label(heading, SWT.NONE);
        warning.setImage(DBeaverIcons.getImage(DBIcon.SMALL_WARNING));
        UIUtils.createLabel(heading, DDTrackingUIMessages.project_sync_conflict_heading);
        UIUtils.createLabel(
            area, NLS.bind(
                DDTrackingUIMessages.project_sync_conflict_project,
                details.project().getName()
            )
        );

        Table table = new Table(area, SWT.BORDER | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableData.widthHint = 660;
        tableData.heightHint = table.getHeaderHeight() + 3 * table.getItemHeight() + 2 * table.getBorderWidth();
        table.setLayoutData(tableData);

        TableColumn versionColumn = new TableColumn(table, SWT.LEFT);
        versionColumn.setText(DDTrackingUIMessages.project_sync_conflict_version);
        versionColumn.setWidth(270);
        TableColumn countColumn = new TableColumn(table, SWT.LEFT);
        countColumn.setText(DDTrackingUIMessages.project_sync_conflict_new_revisions);
        countColumn.setWidth(130);
        TableColumn dateColumn = new TableColumn(table, SWT.LEFT);
        dateColumn.setText(DDTrackingUIMessages.project_sync_conflict_last_revision);
        dateColumn.setWidth(240);

        DDSharedProjectRevision lastSynced = details.revisions().lastSyncedRevision();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        TableItem baseline = new TableItem(table, SWT.NONE);
        baseline.setText(new String[] {
            DDTrackingUIMessages.project_sync_conflict_last_sync,
            "—",
            formatter.format(lastSynced.updateTime())
        });
        List<DDSharedProjectRevision> newer = details.revisions().revisions().stream()
            .filter(revision -> revision.updateTime().isAfter(lastSynced.updateTime()))
            .toList();
        String latestDate = newer.stream().map(DDSharedProjectRevision::updateTime)
            .max(LocalDateTime::compareTo).map(formatter::format).orElse("—");
        TableItem remote = new TableItem(table, SWT.NONE);
        remote.setText(new String[] {
            DDTrackingUIMessages.project_sync_conflict_server_version,
            Integer.toString(newer.size()),
            latestDate
        });
        UIUtils.createLabel(area, DDTrackingUIMessages.project_sync_conflict_replaces);
        return area;
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, USE_LOCAL_ID, DDTrackingUIMessages.project_sync_conflict_use_local, false);
        createButton(parent, USE_SERVER_ID, DDTrackingUIMessages.project_sync_conflict_use_server, false);
        createButton(parent, STOP_SYNC_ID, DDTrackingUIMessages.project_sync_conflict_stop, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        setReturnCode(buttonId);
        close();
    }

    public enum Choice {
        USE_LOCAL,
        USE_SERVER,
        UNBIND
    }
}
