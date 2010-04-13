/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.actions.sql.AbstractSQLAction;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.eclipse.jface.action.IAction;


public class CommitAction extends SessionAction
{
    @Override
    protected void updateAction(IAction action) {
        try {
            DBCSession session = isConnected() ? getSession() : null;
            action.setEnabled(session != null && !session.isAutoCommit());
        } catch (DBException e) {
            log.error(e);
            action.setEnabled(false);
        }
    }

    public void run(IAction action)
    {
        try {
            DBCSession session = getSession();
            if (session != null) {
                session.commit();
            } else {
                DBeaverUtils.showErrorDialog(getWindow().getShell(), "Commit", "No active database session found");
            }
        } catch (DBException e) {
            DBeaverUtils.showErrorDialog(getWindow().getShell(), "Commit", "Error commiting data", e);
        }
    }

}