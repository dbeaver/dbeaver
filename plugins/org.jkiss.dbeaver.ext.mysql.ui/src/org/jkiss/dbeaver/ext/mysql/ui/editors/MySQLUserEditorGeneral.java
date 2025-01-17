/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.ui.config.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.mysql.ui.config.MySQLUserManager;
import org.jkiss.dbeaver.ext.mysql.ui.config.UserPropertyHandler;
import org.jkiss.dbeaver.ext.mysql.ui.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * MySQLUserEditorGeneral
 */
public class MySQLUserEditorGeneral extends MySQLUserEditorAbstract
{
    //static final Log log = Log.getLog(MySQLUserEditorGeneral.class);
    public static final String DEF_PASSWORD_VALUE = "**********"; //$NON-NLS-1$

    private PageControl pageControl;
    private boolean isLoaded;
    private PrivilegeTableControl privTable;
    private boolean newUser;
    private Text userNameText;
    private Text hostText;
    private CommandListener commandlistener;

    @Override
    public void createPartControl(Composite parent)
    {
        pageControl = new PageControl(parent);

        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        newUser = !getDatabaseObject().isPersisted();
        {
            Composite loginGroup = UIUtils.createControlGroup(container, MySQLUIMessages.editors_user_editor_general_group_login, 2, GridData.FILL_HORIZONTAL, 0);

            userNameText = UIUtils.createLabelText(loginGroup, MySQLUIMessages.editors_user_editor_general_label_user_name, getDatabaseObject().getUserName());
            userNameText.setEditable(newUser);
            if (newUser) {
                ControlPropertyCommandListener.create(this, userNameText, UserPropertyHandler.NAME);
            }

            hostText = UIUtils.createLabelText(loginGroup, MySQLUIMessages.editors_user_editor_general_label_host, getDatabaseObject().getHost());
            hostText.setEditable(newUser);
            if (newUser) {
                ControlPropertyCommandListener.create(this, hostText, UserPropertyHandler.HOST);
            }

            String password = newUser ? "" : DEF_PASSWORD_VALUE; //$NON-NLS-1$
            Text passwordText = UIUtils.createLabelText(loginGroup, MySQLUIMessages.editors_user_editor_general_label_password, password, SWT.BORDER | SWT.PASSWORD);
            ControlPropertyCommandListener.create(this, passwordText, UserPropertyHandler.PASSWORD);

            Text confirmText = UIUtils.createLabelText(loginGroup, MySQLUIMessages.editors_user_editor_general_label_confirm, password, SWT.BORDER | SWT.PASSWORD);
            ControlPropertyCommandListener.create(this, confirmText, UserPropertyHandler.PASSWORD_CONFIRM);
        }

        {
            Composite limitsGroup = UIUtils.createControlGroup(container, MySQLUIMessages.editors_user_editor_general_group_limits, 2, GridData.FILL_HORIZONTAL, 0);

            Spinner maxQueriesText = UIUtils.createLabelSpinner(limitsGroup, MySQLUIMessages.editors_user_editor_general_spinner_max_queries, getDatabaseObject().getMaxQuestions(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxQueriesText, UserPropertyHandler.MAX_QUERIES);

            Spinner maxUpdatesText = UIUtils.createLabelSpinner(limitsGroup, MySQLUIMessages.editors_user_editor_general_spinner_max_updates,  getDatabaseObject().getMaxUpdates(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxUpdatesText, UserPropertyHandler.MAX_UPDATES);

            Spinner maxConnectionsText = UIUtils.createLabelSpinner(limitsGroup, MySQLUIMessages.editors_user_editor_general_spinner_max_connections, getDatabaseObject().getMaxConnections(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxConnectionsText, UserPropertyHandler.MAX_CONNECTIONS);

            Spinner maxUserConnectionsText = UIUtils.createLabelSpinner(limitsGroup, MySQLUIMessages.editors_user_editor_general_spinner_max_user_connections, getDatabaseObject().getMaxUserConnections(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxUserConnectionsText, UserPropertyHandler.MAX_USER_CONNECTIONS);
        }


        {
            privTable = new PrivilegeTableControl(container, MySQLUIMessages.editors_user_editor_general_control_dba_privileges, true);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            privTable.setLayoutData(gd);

            privTable.addListener(SWT.Modify, event -> {
                final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
                final boolean grant = event.detail >= 1;
                final boolean withGrantOption = event.detail == 2;
                addChangeCommand(
                    new MySQLCommandGrantPrivilege(
                        getDatabaseObject(),
                        grant,
                        withGrantOption,
                        null,
                        null,
                        privilege),
                    new DBECommandReflector<MySQLUser, MySQLCommandGrantPrivilege>() {
                        @Override
                        public void redoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                        {
                            if (!privTable.isDisposed()) {
                                privTable.checkPrivilege(privilege, grant);
                            }
                        }
                        @Override
                        public void undoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                        {
                            if (!privTable.isDisposed()) {
                                privTable.checkPrivilege(privilege, !grant);
                            }
                        }
                    });
            });

        }
        pageControl.createProgressPanel();

        commandlistener = new CommandListener();
        DBECommandContext context = getEditorInput().getCommandContext();
        if (context != null) {
            context.addCommandListener(commandlistener);
        }
    }

    @Override
    public void dispose()
    {
        DBECommandContext commandContext = getEditorInput().getCommandContext();
        if (commandlistener != null && commandContext != null) {
            commandContext.removeCommandListener(commandlistener);
        }
        super.dispose();
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
                MySQLUIMessages.editors_user_editor_general_service_load_catalog_privileges,
                executionContext
            ) {
                @Override
                public List<MySQLPrivilege> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException {
                    try {
                        MySQLUser user = getDatabaseObject();
                        if (user == null) {
                            isLoaded = false;
                            return null;
                        }
                        return user.getDataSource().getPrivilegesByKind(monitor, MySQLPrivilege.Kind.ADMIN)
                            .stream()
                            .filter(p -> !p.getName().equalsIgnoreCase("proxy"))
                            .toList();
                    } catch (DBException e) {
                        isLoaded = false;
                        throw new InvocationTargetException(e);
                    }
                }
            },
            pageControl.createLoadVisualizer()
        ).schedule();
    }

    @Override
    protected PageControl getPageControl()
    {
        return pageControl;
    }

    @Override
    protected void processGrants(List<MySQLGrant> grants)
    {
        privTable.fillGrants(grants);
    }

    @Override
    public RefreshResult refreshPart(Object source, boolean force)
    {
        // do nothing
        return RefreshResult.IGNORED;
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }
        public ProgressVisualizer<List<MySQLPrivilege>> createLoadVisualizer() {
            return new ProgressVisualizer<List<MySQLPrivilege>>() {
                @Override
                public void completeLoading(List<MySQLPrivilege> privs) {
                    super.completeLoading(privs);
                    privTable.fillPrivileges(privs);
                    loadGrants();
                }
            };
        }

    }

    private class CommandListener extends DBECommandAdapter {
        @Override
        public void onSave()
        {
            if (newUser && getDatabaseObject().isPersisted()) {
                newUser = false;
                UIUtils.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        userNameText.setEditable(false);
                        hostText.setEditable(false);
                    }
                });
            }
        }

        @Override
        public void onCommandChange(DBECommand<?> command) {
            if (command instanceof MySQLUserManager.CommandRenameUser) {
                MySQLUserManager.CommandRenameUser mysqlCommand = (MySQLUserManager.CommandRenameUser) command;
                setUsernameAndHost(mysqlCommand.getNewUserName(), mysqlCommand.getNewHost());
            }
        }

        @Override
        public void onReset() {
            MySQLUser user = getDatabaseObject();
            setUsernameAndHost(user.getUserName(), user.getHost());
        }

        private void setUsernameAndHost(@NotNull String username, @NotNull String host) {
            UIUtils.asyncExec(() -> {
                if (!privTable.isDisposed()) {
                    userNameText.setText(username);
                    hostText.setText(host);
                }
            });
        }
    }
}
