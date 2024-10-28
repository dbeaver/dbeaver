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

package org.jkiss.dbeaver.ext.cubrid.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridPrivilage;
import org.jkiss.dbeaver.ext.cubrid.ui.config.CubridPrivilageHandler;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.prop.DBECommandProperty;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.utils.CommonUtils;

public class CubridPrivilageEditor extends AbstractDatabaseObjectEditor<CubridPrivilage>
{
    UserPageControl pageControl;
    private CubridPrivilage user;
    private Table table;
    private List<String> groups = new ArrayList<>();

    @Override
    public RefreshResult refreshPart(Object source, boolean force) {
        return RefreshResult.REFRESHED;
    }

    @Override
    public void createPartControl(Composite parent) {
        pageControl = new UserPageControl(parent, this);
        Composite container = UIUtils.createPlaceholder(pageControl, 2, 10);
        this.user = this.getDatabaseObject();
        GridData gds = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gds);
        {
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.heightHint = 30;
            Text t = UIUtils.createLabelText(container, "Name ", user.getName(), SWT.BORDER, new GridData(GridData.FILL_HORIZONTAL));
            GridData gd1 = new GridData();
            gd1.widthHint = 400;
            t.setLayoutData(gd1);
            t.setEditable(!this.getDatabaseObject().isPersisted());
            ControlPropertyCommandListener.create(this, t, CubridPrivilageHandler.NAME);
        }
        {
            String loginedUser = user.getDataSource().getContainer().getConnectionConfiguration().getUserName().toUpperCase();
            boolean allowEditPassword = new ArrayList<>(Arrays.asList("DBA", user.getName())).contains(loginedUser);
            Text t = UIUtils.createLabelText(container, "Password ", "", SWT.BORDER | SWT.PASSWORD);
            GridData gd1 = new GridData();
            gd1.widthHint = 400;
            t.setLayoutData(gd1);
            t.setEditable(allowEditPassword);
            ControlPropertyCommandListener.create(this, t, CubridPrivilageHandler.PASSWORD);
        }
        {
            UIUtils.createControlLabel(container, "Groups", 1);
            table = new Table(container, SWT.BORDER | SWT.CHECK);
            GridData gd = new GridData();
            gd.heightHint = 150;
            gd.widthHint = 390;
            table.setLayoutData(gd);
            loadGroups();
            new TableCommandListener(this, table, CubridPrivilageHandler.GROUPS, groups);
            if (getDatabaseObject().isPersisted()) {
                table.setEnabled(false);
            }
        }
        {
            Text t = UIUtils.createLabelText(container, "Description", user.getDescription(), SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
            GridData gd1 = new GridData(SWT.BEGINNING, SWT.CENTER, true, false);
            gd1.heightHint = 3 * t.getLineHeight();
            gd1.widthHint = 400;
            t.setLayoutData(gd1);
            ControlPropertyCommandListener.create(this, t, CubridPrivilageHandler.DESCRIPTION);
        }
        pageControl.createProgressPanel();
    }

    @Override
    public void setFocus() {
        if (pageControl != null) {
            this.pageControl.setFocus();
        }
    }

    private void loadGroups() {

        new AbstractJob("Load groups")
        {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                List<CubridPrivilage> cubridUsers;
                try {
                    cubridUsers = user.getDataSource().getCubridPrivilages(monitor);

                    UIUtils.syncExec(
                            () -> {
                                table.removeAll();
                                groups.clear();
                                for (CubridPrivilage privilage : cubridUsers) {
                                    if (!privilage.getName().equals(user.getName())) {
                                        TableItem item = new TableItem(table, SWT.BREAK);
                                        item.setImage(DBeaverIcons.getImage(DBIcon.TREE_USER_GROUP));
                                        item.setText(0, privilage.getName());

                                        if (user.getRoles().contains(privilage.getName())) {
                                            groups.add(privilage.getName());
                                            item.setChecked(true);


                                        }


                                    }

                                }

                            });
                } catch (DBException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    protected class UserPageControl extends ObjectEditorPageControl
    {

        public UserPageControl(Composite parent, CubridPrivilageEditor object) {
            super(parent, SWT.NONE, object);
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);

            contributionManager.add(new Separator());

            IWorkbenchSite workbenchSite = getSite();
            if (workbenchSite != null) {
                DatabaseEditorUtils.contributeStandardEditorActions(workbenchSite, contributionManager);
            }
        }
    }

    private class TableCommandListener
    {
        final private CubridPrivilageEditor editor;
        final private Table widget;
        final private CubridPrivilageHandler handler;
        private List<String> values;
        private DBECommandProperty<CubridPrivilage> command;
        private List<String> oldValue;

        public TableCommandListener(CubridPrivilageEditor editor, Table widget, CubridPrivilageHandler handler, List<String> oldValue) {
            this.editor = editor;
            this.widget = widget;
            this.handler = handler;
            this.oldValue = oldValue;
            addEventListener();
        }

        private void addEventListener() {
            widget.addListener(
                    SWT.Selection,
                    event -> {
                        TableItem item = (TableItem) event.item;
                        if (values == null) {
                            values = new ArrayList<>(oldValue);
                        }
                        if (item != null) {
                            if (item.getChecked()) {
                                values.add(item.getText());
                            } else {
                                values.removeIf(value -> value == item.getText());
                            }
                        }
                        DBECommandReflector<CubridPrivilage, DBECommandProperty<CubridPrivilage>> commandReflector = new DBECommandReflector<CubridPrivilage, DBECommandProperty<CubridPrivilage>>()
                        {

                            @Override
                            public void redoCommand(DBECommandProperty<CubridPrivilage> command) {
                                if (!table.isDisposed()) {
                                    editor.loadGroups();
                                    values = new ArrayList<String>(oldValue);
                                }
                            }

                            @Override
                            public void undoCommand(DBECommandProperty<CubridPrivilage> cp) {

                                if (!table.isDisposed()) {
                                    editor.loadGroups();
                                    values = new ArrayList<String>(oldValue);
                                }

                            }

                        };
                        if (command == null) {
                            if (!CommonUtils.equalObjects(values, oldValue)) {
                                command = new DBECommandProperty<CubridPrivilage>(editor.getDatabaseObject(), handler, oldValue, values);
                                editor.addChangeCommand(command, commandReflector);
                            }
                        } else {
                            if (CommonUtils.equalObjects(values, oldValue)) {
                                editor.removeChangeCommand(command);
                                command = null;
                            } else {
                                command.setNewValue(values);
                                editor.updateChangeCommand(command, commandReflector);
                            }
                        }
                    });
        }
    }
}