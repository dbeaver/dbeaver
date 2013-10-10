/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * TODO DF: Work In Progress !!!!
 * 
 * Actions on Tables
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableToolsHandler extends AbstractHandler {

    private static final Log LOG = LogFactory.getLog(DB2TableToolsHandler.class);

    private static final String CMD_REORG_ID = "org.jkiss.dbeaver.ext.db2.table.reorg";
    private static final String CMD_REORGIX_ID = "org.jkiss.dbeaver.ext.db2.table.reorgix";
    private static final String CMD_RUNSTATS_ID = "org.jkiss.dbeaver.ext.db2.table.runstats";
    private static final String CMD_SETINTEGRITY_ID = "org.jkiss.dbeaver.ext.db2.table.setintegrity";

    private static final String DB2_REORG = "REORG TABLE %s";
    private static final String DB2_REORGIX = "REORG INDEXES ALL FOR TABLE %s";
    private static final String DB2_RUNSTATS = "RUNSTATS ON TABLE %s WITH DISTRIBUTION AND DETAILED INDEXES ALL";
    private static final String SQL_SETINTEGRITY = "SET INTEGRITY FOR %s ALLOW NO ACCESS IMMEDIATE CHECKED;";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {

        // TODO DF: check everything
        IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);

        DBNDatabaseItem node = (DBNDatabaseItem) selection.getFirstElement();
        final DB2Table db2Table = (DB2Table) node.getObject();

        if (db2Table != null) {

            Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            //
            // DB2ReorgInfoDialog dialog = new DB2ReorgInfoDialog(activeShell);
            // if (dialog.open() != IDialogConstants.OK_ID) {
            // return null;
            // }

            try {
                if (event.getCommand().getId().equals(CMD_REORG_ID)) {
                    performReorg(activeShell, db2Table);
                }
                if (event.getCommand().getId().equals(CMD_REORGIX_ID)) {
                    performReorgIx(activeShell, db2Table);
                }
                if (event.getCommand().getId().equals(CMD_RUNSTATS_ID)) {
                    performRunstats(activeShell, db2Table);
                }
                if (event.getCommand().getId().equals(CMD_SETINTEGRITY_ID)) {
                    performSetIntegrity(activeShell, db2Table);
                }

                // TOOD DF: refresh DB2Table
                // db2Table.refreshObject(VoidProgressMonitor.INSTANCE);
                // DBNModel.getInstance().refreshNodeContent(sourceSchema, this, DBNEvent.NodeChange.REFRESH);
                // node.refreshNode(monitor, source);
            } catch (InvocationTargetException e) {
                LOG.debug("InvocationTargetException : " + e.getTargetException().getMessage());
                UIUtils.showErrorDialog(activeShell, "Error", e.getTargetException().getMessage());
            } catch (InterruptedException e) {
                LOG.debug("InvocationTargetException : " + e.getMessage());
                UIUtils.showErrorDialog(activeShell, "Error", e.getMessage());
            }
        }

        return null;
    }

    // -------
    // Helpers
    // -------
    private void performReorg(Shell shell, final DB2Table db2Table) throws InvocationTargetException, InterruptedException
    {
        final String sql = String.format(DB2_REORG, db2Table.getFullQualifiedName());

        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                try {
                    DB2Utils.callAdminCmd(monitor, db2Table.getDataSource(), sql);
                } catch (SQLException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
        UIUtils.showMessageBox(shell, "OK", "REORG OK..", SWT.ICON_INFORMATION);
    }

    private void performReorgIx(Shell shell, final DB2Table db2Table) throws InvocationTargetException, InterruptedException
    {
        final String sql = String.format(DB2_REORG, db2Table.getFullQualifiedName());

        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                try {
                    DB2Utils.callAdminCmd(monitor, db2Table.getDataSource(), sql);
                } catch (SQLException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
        UIUtils.showMessageBox(shell, "OK", "REORG OK..", SWT.ICON_INFORMATION);
    }

    private void performRunstats(Shell shell, final DB2Table db2Table) throws InvocationTargetException, InterruptedException
    {
        final String sql = String.format(DB2_RUNSTATS, db2Table.getFullQualifiedName());

        // TODO DF: how to get a nice waiting clock?
        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                try {
                    DB2Utils.callAdminCmd(monitor, db2Table.getDataSource(), sql);
                } catch (SQLException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
        UIUtils.showMessageBox(shell, "OK", "Runstats OK..", SWT.ICON_INFORMATION);
    }

    private void performSetIntegrity(Shell shell, final DB2Table db2Table) throws InvocationTargetException, InterruptedException
    {
        final String sql = String.format(DB2_RUNSTATS, db2Table.getFullQualifiedName());

        // TODO DF: how to get a nice waiting clock?
        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                try {
                    // JDBCUtils.executeSQL(session, sql);
                    DB2Utils.callAdminCmd(monitor, db2Table.getDataSource(), sql);
                } catch (SQLException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
        UIUtils.showMessageBox(shell, "OK", "Runstats OK..", SWT.ICON_INFORMATION);
    }

    // -------
    // Dialogs
    // -------

    private class DB2ReorgInfoDialog extends Dialog {

        private String errorSchemaName;
        private String errorTableName;

        public String getErrorSchemaName()
        {
            return errorSchemaName;
        }

        public String getErrorTableName()
        {
            return errorTableName;
        }

        // Dialog managment
        private Text errorSchmaNameText;
        private Text errorTableNameText;

        public DB2ReorgInfoDialog(Shell parentShell)
        {
            super(parentShell);
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Name for Error Table?");
            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            errorSchmaNameText = UIUtils.createLabelText(composite, "Schema", null);
            errorSchmaNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            errorTableNameText = UIUtils.createLabelText(composite, "Name", null);
            errorTableNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }

        @Override
        protected void okPressed()
        {
            this.errorSchemaName = errorSchmaNameText.getText().trim().toUpperCase();
            this.errorTableName = errorTableNameText.getText().trim().toUpperCase();
            super.okPressed();
        }
    }

    private class DB2RunstatsInfoDialog extends Dialog {

        private String errorSchemaName;
        private String errorTableName;

        public String getErrorSchemaName()
        {
            return errorSchemaName;
        }

        public String getErrorTableName()
        {
            return errorTableName;
        }

        // Dialog managment
        private Text errorSchmaNameText;
        private Text errorTableNameText;

        public DB2RunstatsInfoDialog(Shell parentShell)
        {
            super(parentShell);
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Name for Error Table?");
            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            errorSchmaNameText = UIUtils.createLabelText(composite, "Schema", null);
            errorSchmaNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            errorTableNameText = UIUtils.createLabelText(composite, "Name", null);
            errorTableNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }

        @Override
        protected void okPressed()
        {
            this.errorSchemaName = errorSchmaNameText.getText().trim().toUpperCase();
            this.errorTableName = errorTableNameText.getText().trim().toUpperCase();
            super.okPressed();
        }
    }

}