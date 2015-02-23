/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.edit.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.mysql.edit.UserPropertyHandler;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.edit.DBECommandReflector;
import org.jkiss.dbeaver.model.edit.prop.ControlPropertyCommandListener;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;

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
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        container.setLayoutData(gd);

        newUser = !getDatabaseObject().isPersisted();
        {
            Composite loginGroup = UIUtils.createControlGroup(container, MySQLMessages.editors_user_editor_general_group_login, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);

            userNameText = UIUtils.createLabelText(loginGroup, MySQLMessages.editors_user_editor_general_label_user_name, getDatabaseObject().getUserName());
            userNameText.setEditable(newUser);
            if (newUser) {
                ControlPropertyCommandListener.create(this, userNameText, UserPropertyHandler.NAME);
            }

            hostText = UIUtils.createLabelText(loginGroup, MySQLMessages.editors_user_editor_general_label_host, getDatabaseObject().getHost());
            hostText.setEditable(newUser);
            if (newUser) {
                ControlPropertyCommandListener.create(this, hostText, UserPropertyHandler.HOST);
            }

            String password = newUser ? "" : DEF_PASSWORD_VALUE; //$NON-NLS-1$
            Text passwordText = UIUtils.createLabelText(loginGroup, MySQLMessages.editors_user_editor_general_label_password, password, SWT.BORDER | SWT.PASSWORD);
            ControlPropertyCommandListener.create(this, passwordText, UserPropertyHandler.PASSWORD);

            Text confirmText = UIUtils.createLabelText(loginGroup, MySQLMessages.editors_user_editor_general_label_confirm, password, SWT.BORDER | SWT.PASSWORD);
            ControlPropertyCommandListener.create(this, confirmText, UserPropertyHandler.PASSWORD_CONFIRM);
        }

        {
            Composite limitsGroup = UIUtils.createControlGroup(container, MySQLMessages.editors_user_editor_general_group_limits, 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            Spinner maxQueriesText = UIUtils.createLabelSpinner(limitsGroup, MySQLMessages.editors_user_editor_general_spinner_max_queries, getDatabaseObject().getMaxQuestions(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxQueriesText, UserPropertyHandler.MAX_QUERIES);

            Spinner maxUpdatesText = UIUtils.createLabelSpinner(limitsGroup, MySQLMessages.editors_user_editor_general_spinner_max_updates,  getDatabaseObject().getMaxUpdates(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxUpdatesText, UserPropertyHandler.MAX_UPDATES);

            Spinner maxConnectionsText = UIUtils.createLabelSpinner(limitsGroup, MySQLMessages.editors_user_editor_general_spinner_max_connections, getDatabaseObject().getMaxConnections(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxConnectionsText, UserPropertyHandler.MAX_CONNECTIONS);

            Spinner maxUserConnectionsText = UIUtils.createLabelSpinner(limitsGroup, MySQLMessages.editors_user_editor_general_spinner_max_user_connections, getDatabaseObject().getMaxUserConnections(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxUserConnectionsText, UserPropertyHandler.MAX_USER_CONNECTIONS);
        }


        {
            privTable = new PrivilegeTableControl(container, MySQLMessages.editors_user_editor_general_control_dba_privileges);
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            privTable.setLayoutData(gd);

            privTable.addListener(SWT.Modify, new Listener() {
                @Override
                public void handleEvent(Event event)
                {
                    final MySQLPrivilege privilege = (MySQLPrivilege) event.data;
                    final boolean grant = event.detail == 1;
                    addChangeCommand(
                        new MySQLCommandGrantPrivilege(
                            getDatabaseObject(),
                            grant,
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
                }
            });

        }
        pageControl.createProgressPanel();

        commandlistener = new CommandListener();
        getEditorInput().getCommandContext().addCommandListener(commandlistener);
    }

    @Override
    public void dispose()
    {
        if (commandlistener != null) {
            getEditorInput().getCommandContext().removeCommandListener(commandlistener);
        }
        super.dispose();
    }

    @Override
    public void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingUtils.createService(
            new DatabaseLoadService<List<MySQLPrivilege>>(MySQLMessages.editors_user_editor_general_service_load_catalog_privileges, getDataSource()) {
                @Override
                public List<MySQLPrivilege> evaluate() throws InvocationTargetException, InterruptedException
                {
                    try {
                        return getDatabaseObject().getDataSource().getPrivilegesByKind(getProgressMonitor(), MySQLPrivilege.Kind.ADMIN);
                    }
                    catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            pageControl.createLoadVisualizer())
            .schedule();
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
    public void refreshPart(Object source, boolean force)
    {
        // do nothing
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
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run()
                    {
                        userNameText.setEditable(false);
                        hostText.setEditable(false);
                    }
                });
            }
        }
    }
}
