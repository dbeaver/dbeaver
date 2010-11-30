/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegeTableControl;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
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

            UIUtils.createLabelText(loginGroup, "User Name", getUser().getName());
            UIUtils.createLabelText(loginGroup, "Host", getUser().getHost());

            UIUtils.createLabelText(loginGroup, "Password", "", SWT.BORDER | SWT.PASSWORD);
            UIUtils.createLabelText(loginGroup, "Confirm", "", SWT.BORDER | SWT.PASSWORD);
        }

        {
            Composite limitsGroup = UIUtils.createControlGroup(container, "Limits", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 200);

            UIUtils.createLabelText(limitsGroup, "Max Queries", "" + getUser().getMaxQuestions());
            UIUtils.createLabelText(limitsGroup, "Max Updates", "" + getUser().getMaxUpdates());
            UIUtils.createLabelText(limitsGroup, "Max Connections", "" + getUser().getMaxConnections());
            UIUtils.createLabelText(limitsGroup, "Max User Connections", "" + getUser().getMaxUserConnections());
        }


        {
            privTable = new PrivilegeTableControl(container, "DBA Privileges");
            gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            privTable.setLayoutData(gd);
/*
            final PrivilegesPairList privPair = new PrivilegesPairList(privsGroup);
            privileges = new TreeMap<String, Boolean>(getUser().getGlobalPrivileges());
            privPair.setModel(privileges);

            privPair.addListener(SWT.Modify, new Listener() {
                public void handleEvent(Event event)
                {
                    final String privilege = (String) event.data;
                    final boolean grant = event.detail == PairListControl.MOVE_RIGHT;
                    addChangeCommand(
                        new MySQLCommandGrantPrivilege(
                            grant,
                            getDatabaseObject(),
                            null,
                            privilege),
                        new IDatabaseObjectCommandReflector<MySQLCommandGrantPrivilege>() {
                            public void redoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                            {
                                privileges.put(privilege, grant);
                                if (!privPair.isDisposed()) {
                                    privPair.setModel(privileges);
                                }
                            }
                            public void undoCommand(MySQLCommandGrantPrivilege mySQLCommandGrantPrivilege)
                            {
                                privileges.put(privilege, !grant);
                                if (!privPair.isDisposed()) {
                                    privPair.setModel(privileges);
                                }
                            }
                        });
                }
            });

*/
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
