/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.iotdb.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBGrant;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBPrivilege;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBRelationalUser;
import org.jkiss.dbeaver.ext.iotdb.ui.config.IoTDBCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.iotdb.ui.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.iotdb.ui.internal.IoTDBUiMessages;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class IoTDBUserEditorGeneral extends IoTDBUserEditorAbstract {

    private PageControl pageControl;
    private boolean isLoaded;
    private PrivilegeTableControl privilegesTable;

    @Override
    public void createPartControl(Composite parent) {
        pageControl = new PageControl(parent);

        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        // Login -> User Name
        {
            Composite loginGroup = UIUtils.createControlGroup(container,
                    IoTDBUiMessages.editors_user_editor_general_group_login, 2, GridData.FILL_HORIZONTAL, 0);
            Text userNameText = UIUtils.createLabelText(loginGroup,
                    IoTDBUiMessages.editors_user_editor_general_label_user_name, getDatabaseObject().getName());
            userNameText.setEditable(false);
        }

        // DBA Privileges
        {
            privilegesTable = new PrivilegeTableControl(container, IoTDBUiMessages.editors_user_editor_general_control_dba_privileges);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            privilegesTable.setLayoutData(gd);
            privilegesTable.addListener(SWT.Modify, event -> {
                final IoTDBPrivilege privilege = (IoTDBPrivilege) event.data;
                final int type = event.detail;
                addChangeCommand(
                    new IoTDBCommandGrantPrivilege(getDatabaseObject(), type, "", "", privilege),
                    new DBECommandReflector<IoTDBRelationalUser, IoTDBCommandGrantPrivilege>() {
                        @Override
                        public void redoCommand(@NotNull IoTDBCommandGrantPrivilege command) {
                            // no-op
                        }

                        @Override
                        public void undoCommand(@NotNull IoTDBCommandGrantPrivilege command) {
                            // no-op
                        }
                    });
            });
        }

        pageControl.createProgressPanel();
    }

    @Override
    public void activatePart() {
        if (isLoaded) {
            return;
        }
        DBCExecutionContext executionContext = getExecutionContext();
        if (executionContext == null) {
            return;
        }
        isLoaded = true;
        LoadingJob.createService(
            new DatabaseLoadService<>(
                IoTDBUiMessages.editors_user_editor_general_service_load_catalog_privileges,
                executionContext
            ) {
                @Override
                public List<IoTDBPrivilege> evaluate(@NotNull DBRProgressMonitor monitor) throws InvocationTargetException {
                    IoTDBRelationalUser user = getDatabaseObject();
                    if (user == null) {
                        isLoaded = false;
                        return null;
                    }
                    return user.getDataSource().getPrivilegesByKind(true).stream().toList();
                }
            },
            pageControl.createLoadVisualizer()
        ).schedule();
    }

    @Override
    protected PageControl getPageControl() {
        return pageControl;
    }

    @Override
    protected void processGrants(List<IoTDBGrant> grants) {
        privilegesTable.fillGrants(grants);
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        if (force || (source instanceof DBNEvent && ((DBNEvent) source).getSource() == DBNEvent.UPDATE_ON_SAVE) || !isLoaded) {
            isLoaded = false;
            activatePart();
            return RefreshResult.REFRESHED;
        }
        return RefreshResult.IGNORED;
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }

        public ProgressVisualizer<List<IoTDBPrivilege>> createLoadVisualizer() {
            return new ProgressVisualizer<List<IoTDBPrivilege>>() {
                @Override
                public void completeLoading(List<IoTDBPrivilege> privileges) {
                    super.completeLoading(privileges);
                    privilegesTable.fillPrivileges(privileges);
                    loadGrants();
                }
            };
        }

    }

}
