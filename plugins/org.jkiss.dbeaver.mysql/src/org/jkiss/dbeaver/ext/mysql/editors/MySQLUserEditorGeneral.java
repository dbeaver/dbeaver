/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.runtime.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.mysql.runtime.UserPropertyHandler;
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
    //static final Log log = LogFactory.getLog(MySQLUserEditorGeneral.class);
    public static final String DEF_PASSWORD_VALUE = "**********";

    private PageControl pageControl;
    private boolean isLoaded;
    private PrivilegeTableControl privTable;
    private boolean newUser;
    private Text userNameText;
    private Text hostText;
    private CommandListener commandlistener;

    public void createPartControl(Composite parent)
    {
        pageControl = new PageControl(parent);

        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        container.setLayoutData(gd);

        newUser = !getDatabaseObject().isPersisted();
        {
            Composite loginGroup = UIUtils.createControlGroup(container, "Login", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);

            userNameText = UIUtils.createLabelText(loginGroup, "User Name", getDatabaseObject().getUserName());
            userNameText.setEditable(newUser);
            if (newUser) {
                ControlPropertyCommandListener.create(this, userNameText, UserPropertyHandler.NAME);
            }

            hostText = UIUtils.createLabelText(loginGroup, "Host", getDatabaseObject().getHost());
            hostText.setEditable(newUser);
            if (newUser) {
                ControlPropertyCommandListener.create(this, hostText, UserPropertyHandler.HOST);
            }

            String password = newUser ? "" : DEF_PASSWORD_VALUE;
            Text passwordText = UIUtils.createLabelText(loginGroup, "Password", password, SWT.BORDER | SWT.PASSWORD);
            ControlPropertyCommandListener.create(this, passwordText, UserPropertyHandler.PASSWORD);

            Text confirmText = UIUtils.createLabelText(loginGroup, "Confirm", password, SWT.BORDER | SWT.PASSWORD);
            ControlPropertyCommandListener.create(this, confirmText, UserPropertyHandler.PASSWORD_CONFIRM);
        }

        {
            Composite limitsGroup = UIUtils.createControlGroup(container, "Limits", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            Spinner maxQueriesText = UIUtils.createLabelSpinner(limitsGroup, "Max Queries", getDatabaseObject().getMaxQuestions(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxQueriesText, UserPropertyHandler.MAX_QUERIES);

            Spinner maxUpdatesText = UIUtils.createLabelSpinner(limitsGroup, "Max Updates",  getDatabaseObject().getMaxUpdates(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxUpdatesText, UserPropertyHandler.MAX_UPDATES);

            Spinner maxConnectionsText = UIUtils.createLabelSpinner(limitsGroup, "Max Connections", getDatabaseObject().getMaxConnections(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxConnectionsText, UserPropertyHandler.MAX_CONNECTIONS);

            Spinner maxUserConnectionsText = UIUtils.createLabelSpinner(limitsGroup, "Max User Connections", getDatabaseObject().getMaxUserConnections(), 0, Integer.MAX_VALUE);
            ControlPropertyCommandListener.create(this, maxUserConnectionsText, UserPropertyHandler.MAX_USER_CONNECTIONS);
        }


        {
            privTable = new PrivilegeTableControl(container, "DBA Privileges");
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            privTable.setLayoutData(gd);

            privTable.addListener(SWT.Modify, new Listener() {
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
                            public void redoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                            {
                                if (!privTable.isDisposed()) {
                                    privTable.checkPrivilege(privilege, grant);
                                }
                            }
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

    public void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingUtils.createService(
            new DatabaseLoadService<List<MySQLPrivilege>>("Load catalog privileges", getDataSource()) {
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

    public void refreshPart(Object source)
    {
        // do nothing
    }

    private class PageControl extends UserPageControl {
        public PageControl(Composite parent) {
            super(parent);
        }
        public ProgressVisualizer<List<MySQLPrivilege>> createLoadVisualizer() {
            return new ProgressVisualizer<List<MySQLPrivilege>>() {
                public void completeLoading(List<MySQLPrivilege> privs) {
                    super.completeLoading(privs);
                    privTable.fillPrivileges(privs);
                    loadGrants();
                }
            };
        }

    }

    private class CommandListener extends DBECommandAdapter {
        public void onSave()
        {
            if (newUser && getDatabaseObject().isPersisted()) {
                newUser = false;
                Display.getDefault().asyncExec(new Runnable() {
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
