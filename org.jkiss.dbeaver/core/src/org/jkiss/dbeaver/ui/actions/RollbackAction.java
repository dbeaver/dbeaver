package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.utils.DBeaverUtils;

public class RollbackAction extends SessionAction
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
                session.rollback();
            } else {
                DBeaverUtils.showErrorDialog(getWindow().getShell(), "Rollback", "No active database session found");
            }
        } catch (DBException e) {
            DBeaverUtils.showErrorDialog(getWindow().getShell(), "Rollback", "Error rolling back changes", e);
        }
    }

}