package org.jkiss.dbeaver.ui.actions.sql;

import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.DBException;

public class RollbackAction extends AbstractSQLAction
{

    public RollbackAction()
    {
        setId(ICommandIds.CMD_ROLLBACK);
        setActionDefinitionId(ICommandIds.CMD_ROLLBACK);
        setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/sql/rollback.png"));
        setText("Rollback");
        setToolTipText("Rollback");
    }

    protected void execute(SQLEditor editor)
    {
        try {
            editor.getSession().rollback();
        } catch (DBException e) {
            DBeaverUtils.showErrorDialog(editor.getSite().getShell(), "Rollback", "Error rollback data", e);
        }
    }

}