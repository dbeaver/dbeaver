/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommandReflector;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.runtime.MySQLCommandGrantPrivilege;
import org.jkiss.dbeaver.ext.mysql.runtime.UserPropertyHandler;
import org.jkiss.dbeaver.model.impl.edit.ControlCommandListener;
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

    private PageControl pageControl;
    private boolean isLoaded;
    private PrivilegeTableControl privTable;

    public void createPartControl(Composite parent)
    {
        pageControl = new PageControl(parent);

        Composite container = UIUtils.createPlaceholder(pageControl, 2, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        {
            Composite loginGroup = UIUtils.createControlGroup(container, "Login", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);

            Text userNameText = UIUtils.createLabelText(loginGroup, "User Name", getUser().getUserName());
            userNameText.setEditable(false);
            Text hostText = UIUtils.createLabelText(loginGroup, "Host", getUser().getHost());
            hostText.setEditable(false);

            Text passwordText = UIUtils.createLabelText(loginGroup, "Password", "**********", SWT.BORDER | SWT.PASSWORD);
            ControlCommandListener.create(this, passwordText, UserPropertyHandler.PASSWORD);

            Text confirmText = UIUtils.createLabelText(loginGroup, "Confirm", "**********", SWT.BORDER | SWT.PASSWORD);
            ControlCommandListener.create(this, confirmText, UserPropertyHandler.PASSWORD_CONFIRM);
        }

        {
            Composite limitsGroup = UIUtils.createControlGroup(container, "Limits", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            Spinner maxQueriesText = UIUtils.createLabelSpinner(limitsGroup, "Max Queries", getUser().getMaxQuestions(), 0, Integer.MAX_VALUE);
            ControlCommandListener.create(this, maxQueriesText, UserPropertyHandler.MAX_QUERIES);

            Spinner maxUpdatesText = UIUtils.createLabelSpinner(limitsGroup, "Max Updates",  getUser().getMaxUpdates(), 0, Integer.MAX_VALUE);
            ControlCommandListener.create(this, maxUpdatesText, UserPropertyHandler.MAX_UPDATES);

            Spinner maxConnectionsText = UIUtils.createLabelSpinner(limitsGroup, "Max Connections", getUser().getMaxConnections(), 0, Integer.MAX_VALUE);
            ControlCommandListener.create(this, maxConnectionsText, UserPropertyHandler.MAX_CONNECTIONS);

            Spinner maxUserConnectionsText = UIUtils.createLabelSpinner(limitsGroup, "Max User Connections", getUser().getMaxUserConnections(), 0, Integer.MAX_VALUE);
            ControlCommandListener.create(this, maxUserConnectionsText, UserPropertyHandler.MAX_USER_CONNECTIONS);
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
                            grant,
                            null,
                            null,
                            privilege),
                        new IDatabaseObjectCommandReflector<MySQLCommandGrantPrivilege>() {
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
    }

    public void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingUtils.executeService(
            new DatabaseLoadService<List<MySQLPrivilege>>("Load catalog privileges", getUser().getDataSource()) {
                public List<MySQLPrivilege> evaluate() throws InvocationTargetException, InterruptedException
                {
                    try {
                        return getUser().getDataSource().getPrivileges(getProgressMonitor(), MySQLPrivilege.Kind.ADMIN);
                    }
                    catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            pageControl.createLoadVisualizer());
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

}
