/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.lang.reflect.InvocationTargetException;


public class CommitAction extends SessionAction
{
    @Override
    protected void updateAction(IAction action) {
        DBPDataSource dataSource = getDataSource();
        if (dataSource != null) {
            DBCExecutionContext context = dataSource.openContext(VoidProgressMonitor.INSTANCE, "Check auto commit state");
            try {
                action.setEnabled(!context.getTransactionManager().isAutoCommit());
            }
            catch (DBCException e) {
                log.error(e);
                action.setEnabled(false);
            }
            finally {
                context.close();
            }
        } else {
            action.setEnabled(false);
        }
    }

    public void run(IAction action)
    {
        try {
            final DBPDataSource dataSource = getDataSource();
            if (dataSource != null) {
                DBeaverCore.getInstance().runAndWait2(true, true, new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        DBCExecutionContext context = dataSource.openContext(monitor, "Commit transaction");
                        try {
                            context.getTransactionManager().commit();
                        }
                        catch (DBCException e) {
                            throw new InvocationTargetException(e);
                        }
                        finally {
                            context.close();
                        }
                    }
                });
            } else {
                DBeaverUtils.showErrorDialog(getWindow().getShell(), "Commit", "No active database");
            }
        } catch (InvocationTargetException e) {
            DBeaverUtils.showErrorDialog(getWindow().getShell(), "Commit", "Error commiting data", e);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

}